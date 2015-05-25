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

class ProjectsDependentSbtBuildManager(project: IArgusProject, analysisCache: Option[IFile] = None,
  addToClasspath: Seq[IPath] = Seq.empty, srcOutputs: Seq[(IContainer, IContainer)] = Seq.empty) extends CachedAnalysisBuildManager with HasLogger {
  
  /** Initialized in `build`, used by the SbtProgress. */
  private var monitor: SubMonitor = _

  private val sources: MSet[IFile] = msetEmpty
  // TODO need to change Object to the Analysis
  private val cached = new AtomicReference[SoftReference[Object]]

  def analysisStore = analysisCache.getOrElse(project.underlying.getFile(".cache"))
  private def cacheFile = analysisStore.getLocation.toFile

  // this directory is used by Sbt to store classfiles between
  // compilation runs to implement all-or-nothing compilation
  // sementics. Original files are copied over to tempDir and
  // moved back in case of compilation errors.
  private val tempDir = project.underlying.getFolder(".tmpBin")
  private def tempDirFile = tempDir.getLocation().toFile()
  
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
  
  private def setCached(a: Object): Object = {
    cached set new SoftReference[Object](a); a
  }
  
  private def clearCached(): Unit = {
    Option(cached.get) foreach (ref => ref.clear)
  }
  
  override def clean(implicit monitor: IProgressMonitor) {
    analysisStore.refreshLocal(IResource.DEPTH_ZERO, null)
    analysisStore.delete(true, false, monitor)
    clearCached()
  }

  override def invalidateAfterLoad: Boolean = true

  override def canTrackDependencies: Boolean = true

  override def build(addedOrUpdated: Set[IFile], removed: Set[IFile], pm: SubMonitor): Unit = {
    if (areTransitiveDependenciesBuilt) {
      project.underlying.deleteMarkers(CitConstants.ProblemMarkerId, true, IResource.DEPTH_INFINITE)
      // TODO the real build
//      super.build(addedOrUpdated, removed, pm)
    }
  }
}