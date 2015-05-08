package org.arguside.core.internal.project

import java.io.File.pathSeparator
import scala.Right
import scala.collection.immutable
import scala.collection.mutable
import scala.collection.mutable.Publisher
import scala.reflect.internal.util.SourceFile
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import org.eclipse.core.resources.IContainer
import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IMarker
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IResource
import org.eclipse.core.resources.IResourceProxy
import org.eclipse.core.resources.IResourceProxyVisitor
import org.eclipse.core.resources.ProjectScope
import org.eclipse.core.runtime.IPath
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.Path
import org.eclipse.core.runtime.SubMonitor
import org.eclipse.jdt.core.IClasspathEntry
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.JavaModelException
import org.eclipse.jdt.internal.core.util.Util
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.util.IPropertyChangeListener
import org.eclipse.jface.util.PropertyChangeEvent
import org.eclipse.ui.IEditorPart
import org.eclipse.ui.IPartListener
import org.eclipse.ui.IWorkbenchPart
import org.eclipse.ui.part.FileEditorInput
import org.arguside.core.IArgusProject
import org.arguside.core.IArgusProjectEvent
import org.arguside.core.ScalaInstallationChange
import org.arguside.core.BuildSuccess
import org.arguside.core.IArgusPlugin
import org.arguside.core.internal.ArgusPlugin.plugin
import org.arguside.core.internal.jdt.model.JawaSourceFile
import org.arguside.core.resources.EclipseResource
import org.arguside.core.resources.MarkerFactory
import org.arguside.logging.HasLogger
import org.arguside.ui.internal.actions.PartAdapter
import org.arguside.util.internal.SettingConverterUtil
import org.arguside.util.Utils.WithAsInstanceOfOpt
import org.arguside.util.eclipse.SWTUtils.fnToPropertyChangeListener
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jface.preference.IPersistentPreferenceStore
import org.eclipse.core.runtime.CoreException
import org.arguside.core.CitConstants
import org.arguside.util.eclipse.SWTUtils
import org.arguside.util.eclipse.EclipseUtils
import org.arguside.util.eclipse.FileUtils
import org.eclipse.jdt.core.WorkingCopyOwner
import org.eclipse.jdt.internal.core.DefaultWorkingCopyOwner
import org.eclipse.jdt.internal.core.SearchableEnvironment
import org.eclipse.jdt.internal.core.JavaProject
import org.arguside.ui.internal.editor.JawaEditor
import java.io.IOException
import org.arguside.ui.internal.preferences.PropertyStore

object ArgusProject {
  def apply(underlying: IProject): ArgusProject = {
    val project = new ArgusProject(underlying)
    project.init()
    project
  }

  /** Listen for [[IWorkbenchPart]] event and takes care of loading/discarding jawa compilation units.*/
  private class ProjectPartListener(project: ArgusProject) extends PartAdapter with HasLogger {
    override def partOpened(part: IWorkbenchPart) {
//      doWithCompilerAndFile(part) { (compiler, ssf) =>
//        logger.debug("open " + part.getTitle)
//        ssf.forceReload()
//      }
    }

    override def partClosed(part: IWorkbenchPart) {
//      doWithCompilerAndFile(part) { (compiler, ssf) =>
//        logger.debug("close " + part.getTitle)
//        ssf.discard()
//      }
    }

//    private def doWithCompilerAndFile(part: IWorkbenchPart)(op: (IScalaPresentationCompiler, JawaSourceFile) => Unit) {
//      part match {
//        case editor: IEditorPart =>
//          editor.getEditorInput match {
//            case fei: FileEditorInput =>
//              val f = fei.getFile
//              if (f.getProject == project.underlying && (f.getName.endsWith(CitConstants.PilarFileExtn) || f.getName.endsWith(CitConstants.PilarFileExtn))) {
//                for (ssf <- JawaSourceFile.createFromPath(f.getFullPath.toString)) {
//                  if (project.underlying.isOpen)
//                    project.presentationCompiler(op(_, ssf))
//                }
//              }
//            case _ =>
//          }
//        case _ =>
//      }
//    }
  }

  /**
   * Return true if the given Java project is also a Scala project, false otherwise.
   */
  def isArgusProject(project: IJavaProject): Boolean =
    (project ne null) && isArgusProject(project.getProject)

  /**
   * Return true if the given project is a Scala project, false othrerwise.
   */
  def isArgusProject(project: IProject): Boolean =
    try {
      project != null && project.isOpen && project.hasNature(CitConstants.NatureId)
    } catch {
      case _:
      CoreException => false
    }
}

class ArgusProject private (val underlying: IProject) extends HasLogger with IArgusProject {

  private val worbenchPartListener: IPartListener = new ArgusProject.ProjectPartListener(this)
  
  /** To avoid letting 'this' reference escape during initialization, this method is called right after a
   *  [[ArgusPlugin]] instance has been fully initialized.
   */
  private def init(): Unit = {
    if (!IArgusPlugin().headlessMode)
      SWTUtils.getWorkbenchWindow map (_.getPartService().addPartListener(worbenchPartListener))
  }

  /** Does this project have the Argus nature? */
  def hasArgusNature: Boolean = ArgusProject.isArgusProject(underlying)

//  private def settingsError(severity: Int, msg: String, monitor: IProgressMonitor): Unit = {
//    val mrk = underlying.createMarker(CitConstants.SettingProblemMarkerId)
//    mrk.setAttribute(IMarker.SEVERITY, severity)
//    mrk.setAttribute(IMarker.MESSAGE, msg)
//  }
//
//  private def clearSettingsErrors(): Unit =
//    if (isUnderlyingValid) {
//      underlying.deleteMarkers(CitConstants.SettingProblemMarkerId, true, IResource.DEPTH_ZERO)
//    }

//  def directDependencies: Seq[IProject] =
//    underlying.getReferencedProjects.filter(_.isOpen)
//
//  def transitiveDependencies: Seq[IProject] =
//    directDependencies ++ (directDependencies flatMap (p => IArgusPlugin().getArgusProject(p).exportedDependencies))

//  def exportedDependencies: Seq[IProject] = {
//    for {
//      entry <- resolvedClasspath
//      if entry.getEntryKind == IClasspathEntry.CPE_PROJECT && entry.isExported
//    } yield EclipseUtils.workspaceRoot.getProject(entry.getPath().toString)
//  }

  lazy val javaProject: IJavaProject = JavaCore.create(underlying)

  def sourceFolders: Seq[IPath] = {
    for {
      cpe <- resolvedClasspath if cpe.getEntryKind == IClasspathEntry.CPE_SOURCE
      resource <- Option(EclipseUtils.workspaceRoot.findMember(cpe.getPath)) if resource.exists
    } yield resource.getLocation
  }

  def outputFolders: Seq[IPath] =
    sourceOutputFolders.map(_._2.getFullPath()).toSeq

  def outputFolderLocations: Seq[IPath] =
    sourceOutputFolders.map(_._2.getLocation()).toSeq

  def sourceOutputFolders: Seq[(IContainer, IContainer)] = {
    val cpes = resolvedClasspath
    for {
      cpe <- cpes if cpe.getEntryKind == IClasspathEntry.CPE_SOURCE
      source <- Option(EclipseUtils.workspaceRoot.findMember(cpe.getPath)) if source.exists
    } yield {
      val cpeOutput = cpe.getOutputLocation
      val outputLocation = if (cpeOutput != null) cpeOutput else javaProject.getOutputLocation

      val wsroot = EclipseUtils.workspaceRoot
      if (source.getProject.getFullPath == outputLocation)
        (source.asInstanceOf[IContainer], source.asInstanceOf[IContainer])
      else {
        val binPath = wsroot.getFolder(outputLocation)
        (source.asInstanceOf[IContainer], binPath)
      }
    }
  }

  protected def isUnderlyingValid = (underlying.exists() && underlying.isOpen)

  /** This function checks that the underlying project is closed, if not, return the classpath, otherwise return Nil,
   *  so avoids throwing an exceptions.
   *  @return the classpath or Nil, if the underlying project is closed.
   */
  private def resolvedClasspath =
    try {
      if (isUnderlyingValid) {
        javaProject.getResolvedClasspath(true).toList
      } else {
        Nil
      }
    } catch {
      case e: JavaModelException => logger.error(e); Nil
    }
  
  def allSourceFiles(): Set[IFile] = {
    allFilesInSourceDirs() filter (f => FileUtils.isBuildable(f.getName))
  }

  def allFilesInSourceDirs(): Set[IFile] = {
    /** Cache it for the duration of this call */
    lazy val currentSourceOutputFolders = sourceOutputFolders

    /** Return the inclusion patterns of `entry` as an Array[Array[Char]], ready for consumption
     *  by the JDT.
     *
     *  @see org.eclipse.jdt.internal.core.ClassPathEntry.fullInclusionPatternChars()
     */
    def fullPatternChars(entry: IClasspathEntry, patterns: Array[IPath]): Array[Array[Char]] = {
      if (patterns.isEmpty)
        null
      else {
        val prefixPath = entry.getPath().removeTrailingSeparator();
        for (pattern <- patterns)
          yield prefixPath.append(pattern).toString().toCharArray();
      }
    }

    /** Logic is copied from existing code ('isExcludedFromProject'). Code is trying to
     *  see if the given path is a source or output folder for any source entry in the
     *  classpath of this project.
     */
    def sourceOrBinaryFolder(path: IPath): Boolean = {
      if (path.segmentCount() > 2) return false // is a subfolder of a package

      currentSourceOutputFolders exists {
        case (srcFolder, binFolder) =>
          (srcFolder.getFullPath() == path || binFolder.getFullPath() == path)
      }
    }

    var sourceFiles = new immutable.HashSet[IFile]

    for {
      srcEntry <- javaProject.getResolvedClasspath(true)
      if srcEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE
      srcFolder = EclipseUtils.workspaceRoot.findMember(srcEntry.getPath())
      if srcFolder ne null
    } {
      val inclusionPatterns = fullPatternChars(srcEntry, srcEntry.getInclusionPatterns())
      val exclusionPatterns = fullPatternChars(srcEntry, srcEntry.getExclusionPatterns())
      val isAlsoProject = srcFolder == underlying // source folder is the project itself

      srcFolder.accept(
        new IResourceProxyVisitor {
          def visit(proxy: IResourceProxy): Boolean = {
            proxy.getType match {
              case IResource.FILE =>
                if (!Util.isExcluded(proxy.requestFullPath(), inclusionPatterns, exclusionPatterns, false))
                  sourceFiles += proxy.requestResource().asInstanceOf[IFile] // must be an IFile, otherwise we wouldn't be here

                false // don't recurse, it's a file anyway

              case IResource.FOLDER =>
                if (isAlsoProject) {
                  !sourceOrBinaryFolder(proxy.requestFullPath) // recurse if not on a source or binary folder path
                } else if (exclusionPatterns != null) {
                  if (Util.isExcluded(proxy.requestFullPath, inclusionPatterns, exclusionPatterns, true)) {
                    // must walk children if inclusionPatterns != null, can skip them if == null
                    // but folder is excluded so do not create it in the output folder
                    inclusionPatterns != null
                  } else true
                } else true // recurse into subfolders

              case _ =>
                true
            }
          }
        }, IResource.NONE)
    }

    sourceFiles
  }

  private def cleanOutputFolders(implicit monitor: IProgressMonitor) = {
    def delete(container: IContainer, deleteDirs: Boolean)(f: String => Boolean): Unit =
      if (container.exists()) {
        container.members.foreach {
          case cntnr: IContainer =>
            if (deleteDirs) {
              try {
                cntnr.delete(true, monitor) // might not work.
              } catch {
                case _: Exception =>
                  delete(cntnr, deleteDirs)(f)
                  if (deleteDirs)
                    try {
                      cntnr.delete(true, monitor) // try again
                    } catch {
                      case t: Exception => eclipseLog.error(t)
                    }
              }
            } else
              delete(cntnr, deleteDirs)(f)
          case file: IFile if f(file.getName) =>
            try {
              file.delete(true, monitor)
            } catch {
              case t: Exception => eclipseLog.error(t)
            }
          case _ =>
        }
      }

    val outputLocation = javaProject.getOutputLocation
    val resource = EclipseUtils.workspaceRoot.findMember(outputLocation)
    resource match {
      case container: IContainer => delete(container, container != javaProject.getProject)(_.endsWith(".class"))
      case _ =>
    }
  }

  private def refreshOutputFolders(): Unit = {
    sourceOutputFolders foreach {
      case (_, binFolder) =>
        binFolder.refreshLocal(IResource.DEPTH_INFINITE, null)
        // make sure the folder is marked as Derived, so we don't see classfiles in Open Resource
        // but don't set it unless necessary (this might be an expensive operation)
        if (!binFolder.isDerived && binFolder.exists)
          binFolder.setDerived(true, null)
    }
  }

  // TODO Per-file encodings
  private def encoding: Option[String] =
    sourceFolders.headOption flatMap { path =>
      EclipseUtils.workspaceRoot.findContainersForLocation(path) match {
        case Array(container) => Some(container.getDefaultCharset)
        case _ => None
      }
    }

//  protected def shownSettings(settings: Settings, filter: Settings#Setting => Boolean): Seq[(Settings#Setting, String)] = {
//    // save the current preferences state, so we don't go through the logic of the workspace
//    // or project-specific settings for each setting in turn.
//    val currentStorage = storage
//    for {
//      box <- IDESettings.shownSettings(settings)
//      setting <- box.userSettings if filter(setting)
//      value = currentStorage.getString(SettingConverterUtil.convertNameToProperty(setting.name))
//      if (value.nonEmpty)
//    } yield (setting, value)
//  }
//
//  private def initializeSetting(setting: Settings#Setting, propValue: String) {
//    try {
//      setting.tryToSetFromPropertyValue(propValue)
//      logger.debug("[%s] initializing %s to %s (%s)".format(underlying.getName(), setting.name, setting.value.toString, storage.getString(SettingConverterUtil.convertNameToProperty(setting.name))))
//    } catch {
//      case t: Throwable => eclipseLog.error("Unable to set setting '" + setting.name + "' to '" + propValue + "'", t)
//    }
//  }

  /** Return a the project-specific preference store. This does not take into account the
   *  user-preference whether to use project-specific compiler settings or not.
   */
  lazy val projectSpecificStorage: IPersistentPreferenceStore = {
    val p = new PropertyStore(new ProjectScope(underlying), CitConstants.PluginId) {
      override def save() {
        try {
          super.save()
        } catch {
          case e:IOException =>
            logger.error(s"An Exception occured saving the project-specific preferences for ${underlying.getName()} ! Your settings will not be persisted. Please report !")
            throw(e)
          }
        }

      }
//    p.addPropertyChangeListener(compilerSettingsListener)
    p
  }

  /** 
   *  Return the current project preference store.
   */
  def storage: IPreferenceStore = {
//    if (usesProjectSettings) projectSpecificStorage else 
      IArgusPlugin().getPreferenceStore()
  }

  override def newSearchableEnvironment(workingCopyOwner: WorkingCopyOwner = DefaultWorkingCopyOwner.PRIMARY): SearchableEnvironment = {
    val jProject = javaProject.asInstanceOf[JavaProject]
    jProject.newSearchableNameEnvironment(workingCopyOwner)
  }

  override def toString: String = underlying.getName

  override def equals(other: Any): Boolean = other match {
    case otherSP: IArgusProject => underlying == otherSP.underlying
    case _ => false
  }

  override def hashCode(): Int = underlying.hashCode()
}
