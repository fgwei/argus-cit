package org.arguside.core

import org.eclipse.core.resources.IProject
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.core.resources.IContainer
import org.eclipse.core.resources.IFile
//import org.arguside.core.internal.builder.EclipseBuildManager
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.core.runtime.SubMonitor
import org.eclipse.core.runtime.IProgressMonitor
import scala.collection.mutable.Publisher
import java.io.File
import org.eclipse.core.runtime.IPath
import org.eclipse.jdt.core.WorkingCopyOwner
import org.eclipse.jdt.internal.core.DefaultWorkingCopyOwner
import org.eclipse.jdt.internal.core.SearchableEnvironment

/**
 * A message class to signal various project-related statuses, such as a pilar Installation change, or a successful Build.
 * Immutable.
 */
trait IArgusProjectEvent
case class BuildSuccess() extends IArgusProjectEvent
case class ScalaInstallationChange() extends IArgusProjectEvent

/** The pilar classpath broken down in the JDK, Scala library and user library.
 *
 *  The Scala compiler needs these entries to be separated for proper setup.
 *  Immutable.
 *
 *  @note All paths are file-system absolute paths. Any path variables or
 *        linked resources are resolved.
 */
//trait IPilarClasspath {
//  /**
//   * The JDK elements that should figure on classpath.
//   */
//  val jdkPaths: Seq[IPath]
//  /**
//   * The scala standard library.
//   */
//  val scalaLibrary: Option[IPath]
//  /**
//   * User libraries that should figure on classpath.
//   */
//  val userCp: Seq[IPath]
//  /**
//   * An optional Scala version string for diagnostics.
//   * If present, should match the content of the library.properties in the scala Library.
//   */
//  val scalaVersionString: Option[String]
//  /**
//   * The concatenation of the full classpath.
//   */
//  val fullClasspath: Seq[File]
//}

/**
 * This class represents a Scala Project and associated tools necessary to build it.
 *
 * This class is not thread-safe.
 */
trait IArgusProject extends Publisher[IArgusProjectEvent] {

  /**
   * An IProject which is the project object at the Eclipse platform's level.
   */
  val underlying: IProject

  /**
   *  Does this project have the platform's level of a Argus-corresponding Nature ?
   */
  def hasArgusNature: Boolean

  /** The JDT-level project corresponding to this (Argus) project. */
  val javaProject: IJavaProject

  /** The Sequence of source folders used by this project */
  def sourceFolders: Seq[IPath]

  /** Return the output folders of this project. Paths are relative to the workspace root,
   *  and they are handles only (may not exist).
   */
  def outputFolders: Seq[IPath]

  /** The output folder file-system absolute paths. */
  def outputFolderLocations: Seq[IPath]

  /** Return the source folders and their corresponding output locations
   *  without relying on NameEnvironment. Does not create folders if they
   *  don't exist already.
   *
   *  @return A sequence of pairs of source folders with their corresponding
   *          output folder.
   */
  def sourceOutputFolders(): Seq[(IContainer, IContainer)]

  /** Return all source files in the source path. It only returns buildable files (meaning
   *  Java or pilar sources).
   */
  def allSourceFiles(): Set[IFile]

  /** Return all the files in the current project. It walks all source entries in the classpath
   *  and respects inclusion and exclusion filters. It returns both buildable files (java or pilar)
   *  and all other files in the source path.
   */
  def allFilesInSourceDirs(): Set[IFile]

  /** Return the current project's preference store.
   *  @return A project-specific store if the project is set to use project-specific settings,
   *  a scoped preference store otherwise.
   */
  def storage: IPreferenceStore


  /** Returns a new search name environment for this Scala project.
   *
   *  @param workingCopyOwner A working copy owner who's copies are searched first.
   */
  def newSearchableEnvironment(workingCopyOwner: WorkingCopyOwner = DefaultWorkingCopyOwner.PRIMARY): SearchableEnvironment
}

object IArgusProject {

  def apply(underlying: IProject): IArgusProject = org.arguside.core.internal.project.ArgusProject(underlying)

}
