package org.arguside.ui.internal.preferences

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer
import org.eclipse.core.runtime.preferences.DefaultScope
import org.arguside.ui.internal.diagnostic.StartupDiagnostics

class PreferenceInitializer extends AbstractPreferenceInitializer {

  def initializeDefaultPreferences(): Unit = {
    val node = DefaultScope.INSTANCE.getNode("org.argus-ide.cit.core");
    node.putBoolean(StartupDiagnostics.ASK_DIAGNOSTICS, true);
  }

}
