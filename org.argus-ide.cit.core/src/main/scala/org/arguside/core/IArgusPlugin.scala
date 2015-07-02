package org.arguside.core

import org.eclipse.core.resources.IProject
import org.eclipse.ui.IEditorInput
import org.eclipse.ui.plugin.AbstractUIPlugin
import org.arguside.core.internal.jdt.model.JawaCompilationUnit
import org.arguside.logging.HasLogger

object IArgusPlugin {

  /** The runtime instance of ArgusPlugin
   */
  def apply(): IArgusPlugin = org.arguside.core.internal.ArgusPlugin()

}

/** The public interface of the plugin runtime class of the CIT plugin.
 *
 *  All methods defined inside this trait are thread-safe.
 *  For the inherited methods, check their own documentation.
 */
trait IArgusPlugin extends AbstractUIPlugin with HasLogger {

  /* W.Zhou: added to let argus-debug's building pass */
  lazy val noTimeoutMode: Boolean = false
  lazy val headlessMode: Boolean = false
  
  /** Always returns the ArgusProject for the given project, creating a
   *  new instance if needed.
   *
   *  The given project has to have the argus nature,
   *  otherwise it might lead to errors later on.
   *
   *  If it is not known if the project has the argus nature or not,
   *  use [[org.arguside.core.IArgusPlugin.asArgusProject]] instead.
   */
  def getArgusProject(project: IProject): IArgusProject

  /**
   * Return Some(ArgusProject) if the project has the argus nature, None otherwise.
   */
  def asArgusProject(project: IProject): Option[IArgusProject]

  /**
   * Finds the `JawaCompilationUnit` of a given `IEditorInput`. Returns `None`
   * if no compilation unit is found.
   */
  def jawaCompilationUnit(input: IEditorInput): Option[JawaCompilationUnit]
}
