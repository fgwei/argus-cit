package org.arguside.debug.internal.launching

import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate
import org.eclipse.jdt.launching.IVMRunner
import org.eclipse.pde.internal.launching.launcher.VMHelper

trait ArgusDebuggerForLaunchDelegate extends AbstractJavaLaunchConfigurationDelegate {
  override def getVMRunner(configuration: ILaunchConfiguration, mode: String): IVMRunner = {
    val vm = VMHelper.createLauncher(configuration)
    new StandardVMArgusDebugger(vm)
  }
}