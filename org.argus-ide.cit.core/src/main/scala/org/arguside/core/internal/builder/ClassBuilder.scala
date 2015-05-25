package org.arguside.core.internal.builder

import org.arguside.core.internal.jdt.builder.GeneralJawaJavaBuilder
import org.arguside.core.internal.jdt.builder.JawaJavaBuilderUtils
import org.arguside.core.internal.jdt.util.JDTUtils
import org.eclipse.core.resources.IProject
import org.eclipse.jdt.internal.core.JavaModelManager
import org.eclipse.jdt.internal.core.builder.State

/** Holds common behavior for a builder that has to interop with SDT. */
trait JDTBuilderFacade {

  protected val jawaJavaBuilder = new GeneralJawaJavaBuilder

  /** The underlying project. */
  protected def project: IProject

  protected def refresh() {
    val modelManager = JavaModelManager.getJavaModelManager
    val state = modelManager.getLastBuiltState(project, null).asInstanceOf[State]
    val newState =
      if (state ne null)
        state
      else {
        JawaJavaBuilderUtils.initializeBuilder(jawaJavaBuilder, 0, false)
        StateUtils.newState(jawaJavaBuilder)
      }
    StateUtils.tagAsStructurallyChanged(newState)
    StateUtils.resetStructurallyChangedTypes(newState)
    modelManager.setLastBuiltState(project, newState)
    JDTUtils.refreshPackageExplorer
  }

  protected def ensureProject() {
    if (jawaJavaBuilder.getProject == null)
      jawaJavaBuilder.setProject0(project)
  }
}
