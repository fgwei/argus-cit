package org.arguside.ui.internal.actions

import org.eclipse.jface.action.IAction
import org.eclipse.jface.viewers.ISelection
import org.eclipse.ui.IObjectActionDelegate
import org.eclipse.ui.IWorkbenchPart
import org.eclipse.ui.IWorkbenchWindow
import org.arguside.core.internal.logging.LogManager
import org.eclipse.ui.IWorkbenchWindowActionDelegate
import org.arguside.ui.internal.diagnostic
import org.arguside.util.eclipse.SWTUtils
import org.arguside.util.eclipse.EclipseUtils

class RunDiagnosticAction extends IObjectActionDelegate with IWorkbenchWindowActionDelegate {
  private var parentWindow: IWorkbenchWindow = null

  val RUN_DIAGNOSTICS = "org.argus-ide.cit.ui.runDiag.action"
  val REPORT_BUG      = "org.argus-ide.cit.ui.reportBug.action"
  val OPEN_LOG_FILE   = "org.argus-ide.cit.ui.openLogFile.action"

  override def init(window: IWorkbenchWindow) {
    parentWindow = window
  }

  override def dispose = { }

  override def selectionChanged(action: IAction, selection: ISelection) {  }

  override def run(action: IAction) {
    EclipseUtils.withSafeRunner("Error occurred while trying to create diagnostic dialog.") {
      action.getId match {
        case RUN_DIAGNOSTICS =>
          val shell = if (parentWindow == null) SWTUtils.getShell else parentWindow.getShell
          new diagnostic.DiagnosticDialog(shell).open
        case REPORT_BUG =>
          val shell = if (parentWindow == null) SWTUtils.getShell else parentWindow.getShell
          new diagnostic.ReportBugDialog(shell).open
        case OPEN_LOG_FILE =>
          OpenExternalFile(LogManager.logFile).open()
        case _ =>
      }
    }
  }

  override def setActivePart(action: IAction, targetPart: IWorkbenchPart) { }
}
