package org.arguside.debug.internal.launching

import org.arguside.core.internal.launching.ArgusLaunchDelegate

/**
 * Launch configuration delegate starting Scala applications with the Scala debugger.
 */
class ArgusApplicationLaunchConfigurationDelegate extends ArgusLaunchDelegate
  with ArgusDebuggerForLaunchDelegate
