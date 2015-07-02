package org.arguside.debug.internal.model

import com.sun.jdi.ClassType
import com.sun.jdi.ReferenceType
import com.sun.jdi.Type
import com.sun.jdi.Field
import com.sun.jdi.Value
import com.sun.jdi.Method

/** A Reference type in the Argus debug model. Represente an array, an interface or a class type.
 */
class ArgusReferenceType(underlying: ReferenceType, debugTarget: ArgusDebugTarget) extends ArgusDebugElement(debugTarget) with HasFieldValue {

  override protected def getReferenceType(): ReferenceType = underlying

  override protected def getJdiFieldValue(field: Field): Value = underlying.getValue(field)

}

/** A Class type in the Argus debug model
 */
class ArgusClassType(underlying: ClassType, debugTarget: ArgusDebugTarget) extends ArgusReferenceType(underlying, debugTarget) with HasMethodInvocation {

  override protected[model] def classType(): ClassType = underlying

  override protected[model] def jdiInvokeMethod(method: Method, thread: ArgusThread, args: Value*): Value = thread.invokeStaticMethod(underlying, method, args:_*)

}

object ArgusType {

  /** Return the given JDI Type wrapped inside a Argus debug model type.
   */
  def apply(t: Type, debugTarget: ArgusDebugTarget): ArgusReferenceType = {
    t match {
      case c: ClassType =>
        new ArgusClassType(c, debugTarget)
      case r: ReferenceType =>
        new ArgusReferenceType(r, debugTarget)
    }
  }
}
