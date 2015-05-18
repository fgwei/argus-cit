package org.arguside.ui.internal.actions

import org.eclipse.core.resources.IProject
import org.arguside.core.IArgusPlugin

class RestartPresentationCompilerAction extends AbstractPopupAction {
  override def performAction(project: IProject): Unit = {
    val argusProject = IArgusPlugin().asArgusProject(project)
    argusProject foreach (_.presentationCompiler.askRestart())
  }
}
