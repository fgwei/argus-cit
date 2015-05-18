package org.arguside.ui.internal.handlers

import org.eclipse.core.runtime.IStatus
import org.eclipse.debug.core.IStatusHandler
import org.arguside.util.ui.DisplayThread
import org.eclipse.ui.PlatformUI

trait RichStatusHandler extends IStatusHandler {

  final def handleStatus(status: IStatus, source: Object): Object = {
    val display = PlatformUI.getWorkbench().getDisplay();
    if (PlatformUI.isWorkbenchRunning() && display != null && !display.isDisposed()) {
      // the correct display thread and spawn to it if not.
      if (Thread.currentThread().equals(display.getThread())) {
        // The current thread is the display thread, execute synchronously
        doHandleStatus(status, source);
      } else {
        DisplayThread.syncExec(doHandleStatus(status, source))
      }
    }
    null
  }

  protected def doHandleStatus(status: IStatus, source: Object): Unit

}