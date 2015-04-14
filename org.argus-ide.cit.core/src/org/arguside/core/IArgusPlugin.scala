package org.arguside.core

import org.eclipse.core.resources.IProject
import org.eclipse.ui.IEditorInput
import org.eclipse.ui.plugin.AbstractUIPlugin
import org.arguside.core.internal.jdt.model.ArgusCompilationUnit
import org.arguside.logging.HasLogger
import org.arguside.util.internal.CompilerUtils

object IArgusPlugin {

  /** The runtime instance of ScalaPlugin
   */
  def apply(): IArgusPlugin = org.arguside.core.internal.ArgusPlugin()

}

/** The public interface of the plugin runtime class of the SDT plugin.
 *
 *  All methods defined inside this trait are thread-safe.
 *  For the inherited methods, check their own documentation.
 */
trait IArgusPlugin extends AbstractUIPlugin with HasLogger {

  import CitConstants._

  /** Indicates if the `citcore.notimeouts` flag is set.
   */
  lazy val noTimeoutMode: Boolean = System.getProperty(NoTimeoutsProperty) ne null

  /** Indicates if the `citcore.headless` flag is set.
   */
  lazy val headlessMode: Boolean = System.getProperty(HeadlessProperty) ne null

  /** The Scala version the CIT plugin is running on.
   */
//  lazy val argusVersion: ScalaVersion = ScalaVersion.current

  /** The `major.minor` string for the Scala version the CIT plugin is running on.
   */
//  lazy val shortScalaVersion: String = CompilerUtils.shortString(argusVersion)

  /** Always returns the ArgusProject for the given project, creating a
   *  new instance if needed.
   *
   *  The given project has to have the Argus nature,
   *  otherwise it might lead to errors later on.
   *
   *  If it is not known if the project has the Argus nature or not,
   *  use [[org.arguside.core.IArgusPlugin!.asArgusProject]] instead.
   */
  def getArgusProject(project: IProject): IArgusProject

  /**
   * Return Some(ScalaProject) if the project has the Scala nature, None otherwise.
   */
  def asScalaProject(project: IProject): Option[IArgusProject]

  /**
   * Finds the `ScalaCompilationUnit` of a given `IEditorInput`. Returns `None`
   * if no compilation unit is found.
   */
  def argusCompilationUnit(input: IEditorInput): Option[ArgusCompilationUnit]
}
