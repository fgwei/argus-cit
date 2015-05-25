package org.arguside.core.internal.project

import scala.collection.mutable
import org.eclipse.jdt.core.IPackageFragmentRoot
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.core.resources.IMarker
import org.eclipse.jdt.core.IJarEntryResource
import java.util.Properties
import org.eclipse.core.resources.IProject
import org.eclipse.jdt.core.IJavaModelMarker
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.core.resources.IResource
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.IPath
import org.eclipse.jdt.core.IClasspathEntry
import org.eclipse.jdt.launching.JavaRuntime
import org.eclipse.jdt.core.JavaCore
import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IStorage
import java.io.IOException
import org.eclipse.core.resources.IFolder
import org.eclipse.jdt.internal.core.JavaModelManager
import org.eclipse.core.runtime.Path
import org.eclipse.jdt.core.JavaModelException
import org.arguside.logging.HasLogger
import org.arguside.core.CitConstants
import org.arguside.core.internal.ArgusPlugin
import java.io.File
import org.eclipse.jdt.internal.core.JavaProject
import org.arguside.util.eclipse.EclipseUtils
import org.osgi.framework.Version
import org.arguside.util.internal.SettingConverterUtil
import org.eclipse.jface.util.StatusHandler
import org.eclipse.debug.core.DebugPlugin
import scala.collection.immutable.HashMap
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import org.eclipse.jface.preference.IPersistentPreferenceStore
import org.arguside.core.IArgusClasspath
import org.sireum.util._
import org.arguside.core.resources.MarkerFactory

/** The Argus classpath broken down in the JDK, android library and user library.
 *
 *  The Jawa compiler needs these entries to be separated for proper setup.
 *
 *  @note All paths are file-system absolute paths. Any path variables or
 *        linked resources are resolved.
 */
case class ArgusClasspath(val jdkPaths: Seq[IPath], // JDK classpath
  val androidLibrary: Option[IPath], // android library
  val userCp: Seq[IPath], // user classpath, excluding the android library and JDK
  val androidVersionString: Option[String]) extends IArgusClasspath {
  override def toString =
    """
    jdkPaths: %s
    androidLib: %s
    usercp: %s
    androidVersion: %s

    """.format(jdkPaths, androidLibrary, userCp, androidVersionString)

  lazy val androidLibraryFile: Option[File] =
    androidLibrary.map(_.toFile.getAbsoluteFile)

  private def toPath(ps: Seq[IPath]): Seq[File] = ps map (_.toFile.getAbsoluteFile)

  /** Return the full classpath of this project.
   *
   *  It puts the JDK and the android library in front of the user classpath.
   */
  lazy val fullClasspath: Seq[File] =
    toPath(jdkPaths) ++ androidLibraryFile.toSeq ++ toPath(userCp)
}

/** A android library definition.
 *
 *  @param location  The file-system absolute path to the root of the Scala library
 *  @param version   An option version, retrieved from library.properties, if present
 *  @param isProject Whether the library is provided by a project inside the workspace
 *
 */
private case class AndroidLibrary(location: IPath, version: Option[String], isProject: Boolean)

/** Extractor which returns the android version of a jar,
 */
private object VersionInFile {

  /**
   * TODO for android
   * Regex accepting filename of the format: name_2.xx.xx-version.jar.
   * It is used to extract the `2.xx.xx` section.
   */
  private val CrossCompiledRegex = """.*_(2\.\d+(?:\.\d*)?)(?:-.*)?.jar""".r

  def unapply(fileName: String): Option[String] = {
    fileName match {
      case CrossCompiledRegex(version) =>
        Some(version)
      case _ =>
        None
    }
  }
}

/** Argus project classpath management. This class is responsible for breaking down the classpath in
 *  JDK entries, android library entries, and user entries. It also validates the classpath and
 *  manages the classpath error markers for the given Argus project.
 */
trait ClasspathManagement extends HasLogger { self: ArgusProject =>

  /** Return the argus classpath breakdown for the managed project. */
  def argusClasspath: ArgusClasspath = {
    val jdkEntries = jdkPaths
    val cp = javaClasspath.filterNot(jdkEntries.toSet)

    androidLibraries match {
      case Seq(AndroidLibrary(pf, version, _), _*) =>
        new ArgusClasspath(jdkEntries, Some(pf), cp.filterNot(_ == pf), version)
      case _ =>
        new ArgusClasspath(jdkEntries, None, cp, None)
    }
  }

  /** Return the classpath entries coming from the JDK.  */
  def jdkPaths: Seq[IPath] = {
    val rawClasspath = javaProject.getRawClasspath()

    rawClasspath.toSeq.flatMap(cp =>
      cp.getEntryKind match {
        case IClasspathEntry.CPE_CONTAINER =>
          val path0 = cp.getPath
          if (!path0.isEmpty && path0.segment(0) == JavaRuntime.JRE_CONTAINER) {
            val container = JavaCore.getClasspathContainer(path0, javaProject)
            Option(container).map(_.getClasspathEntries.toSeq.map(_.getPath))
          } else None

        case _ => None

      }).flatten
  }

  /** Return the fully resolved classpath of this project, including the
   *  Android library and the JDK entries, in the *project-defined order*.
   *
   *  The Jawa compiler needs the JDK and Android library on the bootclasspath,
   *  meaning that during compilation the effective order is with these two
   *  components at the head of the list. This method *does not* move them
   *  in front.
   */
  private def javaClasspath: Seq[IPath] = {
    val path = new mutable.LinkedHashSet[IPath]

    val computedClasspaths = mutable.HashSet[IJavaProject]()

    def computeClasspath(project: IJavaProject, followedPath: List[IJavaProject]): Unit = {
      // have we seen he project, or does is it part of a cyclic dependency
      if (!computedClasspaths.contains(project) && !followedPath.contains(project)) {
      val cpes = project.getResolvedClasspath(true)

      for (
        // we take only exported dependencies on classPath, except for the initial project for which we take all
        cpe <- cpes if project == javaProject || cpe.isExported || cpe.getEntryKind == IClasspathEntry.CPE_SOURCE
      ) cpe.getEntryKind match {
        case IClasspathEntry.CPE_PROJECT =>
          val depProject = EclipseUtils.workspaceRoot.getProject(cpe.getPath.lastSegment)
          if (JavaProject.hasJavaNature(depProject)) {
            computeClasspath(JavaCore.create(depProject), project :: followedPath)
          }
        case IClasspathEntry.CPE_LIBRARY =>
          if (cpe.getPath != null) {
            val absPath = EclipseUtils.workspaceRoot.findMember(cpe.getPath)
            if (absPath != null)
              path += absPath.getLocation
            else {
              path += cpe.getPath
            }
          } else
            logger.error("Classpath computation encountered a null path for " + cpe, null)
        case IClasspathEntry.CPE_SOURCE =>
          val cpeOutput = cpe.getOutputLocation
          val outputLocation = if (cpeOutput != null) cpeOutput else project.getOutputLocation

          if (outputLocation != null) {
            val absPath = EclipseUtils.workspaceRoot.findMember(outputLocation)
            if (absPath != null)
              path += absPath.getLocation
          }

        case _ =>
          logger.warn("Classpath computation encountered unknown entry: " + cpe)
      }
        computedClasspaths += project
      }
    }
    computeClasspath(javaProject, List())
    path.toList
  }

  private val classpathCheckLock = new Object
  @volatile
  private var classpathHasBeenChecked = false
  @volatile
  private var classpathValid = false;

  private def isCheckingClasspath(): Boolean = java.lang.Thread.holdsLock(classpathCheckLock)

  /** Return <code>true</code> if the classpath is deemed valid.
   *  Check the classpath if it has not been checked yet.
   */
  def isClasspathValid(): Boolean = {
    classpathCheckLock.synchronized {
      if (!classpathHasBeenChecked)
        checkClasspath()
      classpathValid
    }
  }

  /** Check if the classpath is valid for argus.
   *  It is said valid if it contains one and only android library jar, with a version compatible
   *  with the one from the argus-ide plug-in
   *  @param queue Do not trust an ongoing check to deal with the classPath
   */
  def classpathHasChanged(queue: Boolean = true) = {
    if (queue || !isCheckingClasspath()){
      classpathCheckLock.synchronized {
        // mark as in progress
        classpathHasBeenChecked = false
            checkClasspath()
        if (classpathValid) {
          // no point in resetting compilers on an invalid classpath,
          // it would not work anyway. But we need to reset them if the classpath
          // was (and still is) valid, because the contents might have changed.
          logger.info("Resetting compilers due to classpath change.")
          resetCompilers()
        }
      }
    }
  }

  protected def resetClasspathCheck() {
    // mark the classpath as not checked
    classpathCheckLock.synchronized {
      classpathHasBeenChecked = false
    }
  }

  /** Return all package fragments on the classpath that might be a Android library, with their version.
   *
   *  @return the absolute file-system path to package fragments that define `scala.Predef`.
   *          If it contains path variables or is a linked resources, the path is resolved.
   */
  private def androidLibraries: IList[AndroidLibrary] = {
//    val pathToPredef = new Path("scala/Predef.class")
//    fragmentRoots.toSeq
    ilistEmpty
  }
  
  /**
   * Checks the classpath for invalid/several scala library references, wrong versions, etc.
   *
   * Beware: this code path  is not watched by the compilerSettingListener
   * normally a preference change such as below would trigger a cascade of changes[2].
   * To make sure they are consistent, they finish with a classpath check.
   * That final classpath check (right here) can't be watched on, otherwise we may risk a recursion.
   *
   * Hence, changes to the Scala Installation made here need to replicate that cascade manually:
   *   - setting the SCALA_DESIRED_INSTALLATION to a ScalaInstallationChoice
   *   - calling setDesiredScalaInstallation with that choice
   *
   * @param canFixInstallationFromScalaLib whether to configure the project to use a specific
   *                                       Scala Installation in reaction to a versioned, unknown[1]
   *                                       scala-library found in the classpath's contents
   *
   * @note
   * [1] said library can't be a scala container
   * [2] see ScalaProject's `setDesiredInstallation` and `compilerSettingsListener`
   */
  private[internal] def checkClasspath(canFixInstallationFromScalaLib: Boolean = false): Unit = {
    // check the version of Scala library used, and if enabled, the Scala compatibility of the other jars.
//    val withVersionClasspathValidator =
//      storage.getBoolean(SettingConverterUtil.convertNameToProperty(ScalaPluginSettings.withVersionClasspathValidator.name))
    val errors = Seq[(Int, String, String)]()
    updateClasspathMarkers(errors)
    classpathHasBeenChecked = true
  }

  private def moreThanOneLibraryError(libs: Seq[IPath], compatible: Boolean): String = {
    val first =  "More than one scala library found in the build path (%s).".format(libs.mkString(", "))
    if (compatible) first + "This is not an optimal configuration, try to limit to one Scala library in the build path."
    else first + "At least one has an incompatible version. Please update the project build path so it contains only one compatible scala library."
  }

  /** Manage the possible classpath error/warning reported on the project.
   */
  private def updateClasspathMarkers(errors: Seq[(Int, String, String)]) {
    // set the state
    classpathValid = errors.forall(_._1 != IMarker.SEVERITY_ERROR)

    // the marker manipulation needs to be done in a Job, because it requires
    // a change on the IProject, which is locked for modification during
    // the classpath change notification
    EclipseUtils.scheduleJob("Update classpath error markers", underlying, Job.BUILD) { monitor =>
      if (underlying.isOpen()) { // cannot change markers on closed project
        // clean the classpath markers
        underlying.deleteMarkers(CitConstants.ClasspathProblemMarkerId, true, IResource.DEPTH_ZERO)

        if (!classpathValid) {
          // delete all other Scala and Java error markers
          underlying.deleteMarkers(CitConstants.ProblemMarkerId, true, IResource.DEPTH_INFINITE)
          underlying.deleteMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE)
        }

        // create the classpath problem markers
        errors foreach {
          case (severity, message, markerId) => (new cpMarkerFactory(markerId)).create(underlying, severity, message)
        }
      }
      Status.OK_STATUS
    }
  }

  private def validateBinaryVersionsOnClasspath(): Seq[(Int, String, String)] = {
    val entries = argusClasspath.userCp
    val errors = mutable.ListBuffer[(Int, String, String)]()
//    val badEntries = mutable.ListBuffer[(IPath, AndroidVersion)]()
//
//    for (entry <- entries if entry ne null) {
//      entry.lastSegment() match {
//        case VersionInFile(version) =>
//          if (!ArgusPlugin().isCompatibleVersion(version, this)) {
//            badEntries += ((entry,version))
//            val msg = s"${entry.lastSegment()} of ${this.underlying.getName()} build path is cross-compiled with an incompatible version of Scala (${version.unparse}). In case this report is mistaken, this check can be disabled in the compiler preference page."
//            errors += ((IMarker.SEVERITY_ERROR, msg, CitConstants.ArgusVersionProblemMarkerId))
//          }
//        case _ =>
//          // ignore libraries that aren't cross compiled/are compatible
//      }
//    }
    errors.toSeq
  }

  private class cpMarkerFactory(key:String) extends MarkerFactory(key)
}
