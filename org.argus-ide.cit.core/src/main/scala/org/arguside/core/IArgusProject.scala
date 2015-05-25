package org.arguside.core

import org.eclipse.core.resources.IProject
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.core.resources.IContainer
import org.eclipse.core.resources.IFile
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.core.runtime.SubMonitor
import org.eclipse.core.runtime.IProgressMonitor
import scala.collection.mutable.Publisher
import java.io.File
import org.eclipse.core.runtime.IPath
import org.eclipse.jdt.core.WorkingCopyOwner
import org.eclipse.jdt.internal.core.DefaultWorkingCopyOwner
import org.eclipse.jdt.internal.core.SearchableEnvironment
import org.arguside.core.compiler.IPresentationCompilerProxy
import org.arguside.core.internal.builder.EclipseBuildManager

/**
 * A message class to signal various project-related statuses, such as a argus Installation change, or a successful Build.
 * Immutable.
 */
trait IArgusProjectEvent
case class BuildSuccess() extends IArgusProjectEvent
case class ArgusInstallationChange() extends IArgusProjectEvent

/** The Argus classpath broken down in the JDK, android library and user library.
 *
 *  The Jawa compiler needs these entries to be separated for proper setup.
 *  Immutable.
 *
 *  @note All paths are file-system absolute paths. Any path variables or
 *        linked resources are resolved.
 */
trait IArgusClasspath {
  /**
   * The JDK elements that should figure on classpath.
   */
  val jdkPaths: Seq[IPath]
  /**
   * The scala standard library.
   */
  val androidLibrary: Option[IPath]
  /**
   * User libraries that should figure on classpath.
   */
  val userCp: Seq[IPath]
  /**
   * An optional android version string for diagnostics.
   * If present, should match the content of the library.properties in the android Library.
   */
  val androidVersionString: Option[String]
  /**
   * The concatenation of the full classpath.
   */
  val fullClasspath: Seq[File]
}

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
   * The instance of the presentation compiler that runs on this project's source elements.
   */
  val presentationCompiler: IPresentationCompilerProxy
  
  /**
   *  Does this project have the platform's level of a Argus-corresponding Nature ?
   */
  def hasArgusNature: Boolean

  /** The direct dependencies of this project. It only returns opened projects. */
  def directDependencies: Seq[IProject]

  /** All direct and indirect dependencies of this project.
   *
   *  Indirect dependencies are considered only if that dependency is exported by the dependent project.
   *  Consider the following dependency graph:
   *     A -> B -> C
   *
   *  transitiveDependencies(C) = {A, B} iff B *exports* the A project in its classpath
   */
  def transitiveDependencies: Seq[IProject]

  /** Return the exported dependencies of this project. An exported dependency is
   *  another project this project depends on, and which is exported to downstream
   *  dependencies.
   */
  def exportedDependencies(): Seq[IProject]
  
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

  /**
   * Initialization for the build manager associated to this project
   * @return an initialized EclipseBuildManager
   */
  def buildManager(): EclipseBuildManager

  /**
   * It true, it means all source Files have to be reloaded
   */
  def prepareBuild(): Boolean

  /**
   * Builds the project.
   */
  def build(addedOrUpdated: Set[IFile], removed: Set[IFile], monitor: SubMonitor): Unit

  /** Reset the presentation compiler of projects that depend on this one.
   *  This should be done after a successful build, since the output directory
   *  now contains an up-to-date version of this project.
   */
  def resetDependentProjects(): Unit
  
  /**
   *  Cleans metadata on the project, such as error markers and classpath validation status
   */
  def clean(implicit monitor: IProgressMonitor): Unit
  
  /* Classpath Management */

  /** The ScalaClasspath Instance valid for tihs project */
  def argusClasspath: IArgusClasspath

  /** The result of validation checks performed on classpath */
  def isClasspathValid(): Boolean

  /** Inform this project the classpath was just changed. Triggers validation
   *
   *  @param queue If true, a classpath validation run will be triggered eventually.
   *    If false, the validation will yield to any ongoing validation of the classpath.
   */
  def classpathHasChanged(queue: Boolean = true): Unit

  /** Returns a new search name environment for this Scala project.
   *
   *  @param workingCopyOwner A working copy owner who's copies are searched first.
   */
  def newSearchableEnvironment(workingCopyOwner: WorkingCopyOwner = DefaultWorkingCopyOwner.PRIMARY): SearchableEnvironment
}

object IArgusProject {

  def apply(underlying: IProject): IArgusProject = org.arguside.core.internal.project.ArgusProject(underlying)

}
