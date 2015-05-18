package org.arguside.ui.internal.preferences

import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.ui.IWorkbenchPreferencePage
import org.eclipse.ui.IWorkbench
import org.eclipse.ui.dialogs.PropertyPage
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Group
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.SWT
import org.arguside.logging.HasLogger
import org.arguside.core.IArgusPlugin

class ArgusPreferences extends PropertyPage with IWorkbenchPreferencePage with HasLogger {

  /** Pulls the preference store associated with this plugin */
  override def doGetPreferenceStore(): IPreferenceStore = {
    IArgusPlugin().getPreferenceStore()
  }

  def init(x$1: IWorkbench): Unit = {
  }

  def createContents(parent: Composite): Control = {
    val composite = {
      //No Outer Composite
      val tmp = new Composite(parent, SWT.NONE)
      val layout = new GridLayout(1, false)
      tmp.setLayout(layout)
      val data = new GridData(GridData.FILL)
      data.grabExcessHorizontalSpace = true
      data.horizontalAlignment = GridData.FILL
      tmp.setLayoutData(data)
      tmp
    }
    composite
  }
}
