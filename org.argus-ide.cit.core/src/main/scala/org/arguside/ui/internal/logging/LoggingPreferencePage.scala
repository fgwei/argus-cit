package org.arguside.ui.internal.logging

import org.arguside.logging.Level
import org.arguside.core.internal.logging.LogManager
import org.arguside.core.IArgusPlugin
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer
import org.eclipse.jface.preference.BooleanFieldEditor
import org.eclipse.jface.preference.ComboFieldEditor
import org.eclipse.jface.preference.FieldEditorPreferencePage
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Link
import org.eclipse.swt.SWT
import org.eclipse.ui.IWorkbench
import org.eclipse.ui.IWorkbenchPreferencePage
import org.arguside.core.internal.logging.LoggingPreferenceConstants

class LoggingPreferencePage extends FieldEditorPreferencePage with IWorkbenchPreferencePage {

  setPreferenceStore(IArgusPlugin().getPreferenceStore)

  setDescription("General settings for managing logging information in the plugin.")

  override def createFieldEditors() {
    val sortedLevels = Level.values.toArray.sortBy(_.id)
    val namesAndValues = sortedLevels.map(v => Array(v.toString, v.toString))

    addField(new ComboFieldEditor(LoggingPreferenceConstants.LogLevel, "Log Level", namesAndValues, getFieldEditorParent))
    addField(new BooleanFieldEditor(LoggingPreferenceConstants.IsConsoleAppenderEnabled, "Output log in terminal", getFieldEditorParent))
    addField(new BooleanFieldEditor(LoggingPreferenceConstants.RedirectStdErrOut, "Redirect standard out/err to log file", getFieldEditorParent))
  }

  override def createContents(parent: Composite): Control = {
    val control = super.createContents(parent)

    val link = new Link(parent, SWT.NONE)
    link.setText("Click <a>here</a> to open the log file")

    control
  }

  def init(workbench: IWorkbench) {}
}

class LoggingPreferencePageInitializer extends AbstractPreferenceInitializer {
  override def initializeDefaultPreferences() {
    val store = IArgusPlugin().getPreferenceStore
    store.setDefault(LoggingPreferenceConstants.LogLevel, LogManager.defaultLogLevel.toString)
    store.setDefault(LoggingPreferenceConstants.IsConsoleAppenderEnabled, false)
    store.setDefault(LoggingPreferenceConstants.RedirectStdErrOut, true)
  }
}
