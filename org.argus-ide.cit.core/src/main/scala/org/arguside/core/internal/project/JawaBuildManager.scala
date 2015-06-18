package org.arguside.core.internal.project

import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IResource
import org.eclipse.core.runtime.SubMonitor
import org.arguside.core.IArgusPlugin
import org.arguside.core.IArgusProject
import org.arguside.core.CitConstants
import org.arguside.core.internal.builder.BuildProblemMarker
import org.arguside.util.internal.SettingConverterUtil
import org.arguside.core.internal.builder.CachedAnalysisBuildManager
import org.arguside.logging.HasLogger
import org.eclipse.core.runtime.IProgressMonitor
import org.sireum.util._
import java.util.concurrent.atomic.AtomicReference
import java.lang.ref.SoftReference
import org.eclipse.core.resources.IContainer
import org.eclipse.core.runtime.IPath
import org.arguside.core.internal.builder.jawa.EclipseJawaBuildManager

class JawaBuildManager(project: IArgusProject, analysisCache: Option[IFile] = None,
  addToClasspath: Seq[IPath] = Seq.empty, srcOutputs: Seq[(IContainer, IContainer)] = Seq.empty) 
  extends EclipseJawaBuildManager(project, analysisCache, addToClasspath, srcOutputs) with HasLogger {
  
  private def areTransitiveDependenciesBuilt = {
    val projectsInError =
      project.transitiveDependencies.filter(p => IArgusPlugin().getArgusProject(p).buildManager.hasErrors)

//    val stopBuildOnErrorsProperty = SettingConverterUtil.convertNameToProperty(ScalaPluginSettings.stopBuildOnErrors.name)
    val stopBuildOnErrors = true//project.storage.getBoolean(stopBuildOnErrorsProperty)

    if (stopBuildOnErrors && projectsInError.nonEmpty) {
      project.underlying.deleteMarkers(CitConstants.ProblemMarkerId, true, IResource.DEPTH_INFINITE)
      val errorText = projectsInError.map(_.getName).toSet.mkString(", ")
      BuildProblemMarker.create(project.underlying,
        s"Project ${project.underlying.getName} not built due to errors in dependent project(s) ${errorText}")
      false
    } else true
  }

  override def build(addedOrUpdated: Set[IFile], removed: Set[IFile], pm: SubMonitor): Unit = {
    if (areTransitiveDependenciesBuilt) {
      project.underlying.deleteMarkers(CitConstants.ProblemMarkerId, true, IResource.DEPTH_INFINITE)
      super.build(addedOrUpdated, removed, pm)
    }
  }
}