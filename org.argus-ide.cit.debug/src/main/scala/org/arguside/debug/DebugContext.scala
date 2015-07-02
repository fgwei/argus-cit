package org.arguside.debug

import org.eclipse.debug.core.model.IBreakpoint
import org.arguside.debug.internal.model.ArgusDebugTarget

trait DebugContext

case class BreakpointContext(breakpoint: IBreakpoint, debugTarget: ArgusDebugTarget) extends DebugContext
