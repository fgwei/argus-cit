package org.arguside.ui.internal.editor

import org.eclipse.core.runtime.IAdapterFactory
import org.eclipse.debug.ui.actions.IRunToLineTarget
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget
import org.eclipse.jdt.internal.debug.ui.actions.RunToLineAdapter

// Zw: what does adapter means in elcipse IDE???
class JawaRetargettableActionAdapterFactory extends IAdapterFactory {
  override def getAdapter[T](adaptableObject : AnyRef, adapterType : Class[T]): T =
    if (adapterType == classOf[IRunToLineTarget])
      {new RunToLineAdapter}.asInstanceOf[T]
    else if (adapterType == classOf[IToggleBreakpointsTarget])
      {new JawaToggleBreakpointAdapter}.asInstanceOf[T]
    else
      null.asInstanceOf[T]

  override def getAdapterList : Array[Class[_]] =
    Array(classOf[IRunToLineTarget], classOf[IToggleBreakpointsTarget])
}
