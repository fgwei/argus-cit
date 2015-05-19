package org.arguside.ui.internal.actions

import org.eclipse.core.resources.IProject
import org.eclipse.core.runtime.Platform
import org.eclipse.ui.IObjectActionDelegate
import org.eclipse.ui.IWorkbenchPart
import org.arguside.core.CitConstants
import org.arguside.util.eclipse.EclipseUtils

object ToggleArgusNatureAction {
  val PDE_PLUGIN_NATURE = "org.eclipse.pde.PluginNature" /* == org.eclipse.pde.internal.core.natures.PDE.PLUGIN_NATURE */
  val PDE_BUNDLE_NAME = "org.eclipse.pde.ui"
}

class ToggleArgusNatureAction extends AbstractPopupAction {
  import ToggleArgusNatureAction._

  override def performAction(project: IProject) {
    toggleArgusNature(project)
  }

  private def toggleArgusNature(project: IProject): Unit =
    EclipseUtils.withSafeRunner("Couldn't toggle Argus nature.") {
      if (project.hasNature(CitConstants.NatureId)) {
//        doIfPdePresent(project) { ScalaLibraryPluginDependencyUtils.removeScalaLibraryRequirement(project) }
        updateNatureIds(project) { _ filterNot (_ == CitConstants.NatureId) }
      } else {
//        doIfPdePresent(project) { ScalaLibraryPluginDependencyUtils.addScalaLibraryRequirement(project) }
        updateNatureIds(project) { CitConstants.NatureId +: _ }
      }
    }

  private def doIfPdePresent(project: IProject)(proc: => Unit) =
    if (project.hasNature(PDE_PLUGIN_NATURE) && Platform.getBundle(PDE_BUNDLE_NAME) != null)
      proc

  private def updateNatureIds(project: IProject)(natureIdUpdater: Array[String] => Array[String]) {
    val projectDescription = project.getDescription
    val currentNatureIds = projectDescription.getNatureIds
    val updatedNatureIds = natureIdUpdater(currentNatureIds)
    projectDescription.setNatureIds(updatedNatureIds)
    project.setDescription(projectDescription, null)
    project.touch(null)
  }
}
