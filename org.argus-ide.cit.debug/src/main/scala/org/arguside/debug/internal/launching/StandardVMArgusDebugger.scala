package org.arguside.debug.internal.launching

import org.eclipse.debug.core.ILaunch
import org.eclipse.debug.core.model.IDebugTarget
import org.eclipse.debug.core.model.IProcess
import org.eclipse.jdt.internal.launching.StandardVMDebugger
import org.eclipse.jdt.launching.IVMInstall
import org.eclipse.jdt.launching.VMRunnerConfiguration
import org.arguside.debug.internal.model.ArgusDebugTarget

import com.sun.jdi.VirtualMachine

/**
 * Launcher for debug Scala applications using the Scala debugger.
 * Extends the Java debugger launcher, but use the Scala debug model instead of the Java one.
 */
class StandardVMArgusDebugger(vm: IVMInstall) extends StandardVMDebugger(vm) {

  override def createDebugTarget(unusedConfiguration: VMRunnerConfiguration, launch: ILaunch, unusedPort: Int, process: IProcess, virtualMachine: VirtualMachine): IDebugTarget = {
    ArgusDebugTarget(virtualMachine, launch, process,
      allowDisconnect = false, allowTerminate = true,
      classPath = Some(unusedConfiguration.getClassPath))
  }

}