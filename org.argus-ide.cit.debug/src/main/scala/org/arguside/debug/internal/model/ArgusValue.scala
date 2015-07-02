/*
 * Copyright (c) 2014 Contributor. All rights reserved.
 */
package org.arguside.debug.internal.model

import scala.collection.JavaConverters.asScalaBufferConverter

import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import org.eclipse.debug.core.DebugException
import org.eclipse.debug.core.model.IIndexedValue
import org.eclipse.debug.core.model.IValue
import org.eclipse.debug.core.model.IVariable
import org.arguside.debug.internal.ArgusDebugPlugin

import com.sun.jdi.ArrayReference
import com.sun.jdi.BooleanValue
import com.sun.jdi.ByteValue
import com.sun.jdi.CharValue
import com.sun.jdi.ClassType
import com.sun.jdi.DoubleValue
import com.sun.jdi.Field
import com.sun.jdi.FloatValue
import com.sun.jdi.IntegerValue
import com.sun.jdi.LongValue
import com.sun.jdi.Method
import com.sun.jdi.ObjectReference
import com.sun.jdi.ReferenceType
import com.sun.jdi.ShortValue
import com.sun.jdi.StringReference
import com.sun.jdi.Value
import com.sun.jdi.VoidValue

object ArgusValue {

  /**
   * Returns the given JDI value wrapped
   */
  def apply(value: Value, target: ArgusDebugTarget): ArgusValue = {
    value match {
      case arrayReference: ArrayReference =>
        new ArgusArrayReference(arrayReference, target)
      case booleanValue: BooleanValue =>
        // TODO: cache the values?
        new ArgusPrimitiveValue("scala.Boolean", booleanValue.value.toString, booleanValue, target)
      case byteValue: ByteValue =>
        new ArgusPrimitiveValue("scala.Byte", byteValue.value.toString, byteValue, target)
      case charValue: CharValue =>
        new ArgusPrimitiveValue("scala.Char", charValue.value.toString, charValue, target)
      case doubleValue: DoubleValue =>
        new ArgusPrimitiveValue("scala.Double", doubleValue.value.toString, doubleValue, target)
      case floatValue: FloatValue =>
        new ArgusPrimitiveValue("scala.Float", floatValue.value.toString, floatValue, target)
      case integerValue: IntegerValue =>
        new ArgusPrimitiveValue("scala.Int", integerValue.value.toString, integerValue, target)
      case longValue: LongValue =>
        new ArgusPrimitiveValue("scala.Long", longValue.value.toString, longValue, target)
      case shortValue: ShortValue =>
        new ArgusPrimitiveValue("scala.Short", shortValue.value.toString, shortValue, target)
      case stringReference: StringReference =>
        new ArgusStringReference(stringReference, target)
      case objectReference: ObjectReference => // include ClassLoaderReference, ClassObjectReference, ThreadGroupReference, ThreadReference
        new ArgusObjectReference(objectReference, target)
      case null =>
        // TODO : cache one per target
        new ArgusNullValue(target)
      case voidValue: VoidValue =>
        ??? // TODO: in what cases do we get this value ?
      case _ =>
        ???
    }
  }

  /** Mirroring 'normal' values into wrapped JDI ones
   */
  def apply(value: Any, target: ArgusDebugTarget): ArgusValue = {
    value match {
      case s: String =>
        new ArgusStringReference(target.virtualMachine.mirrorOf(s), target)
      case int: Int =>
        val mirror = target.virtualMachine.mirrorOf(int)
        new ArgusPrimitiveValue("scala.Int", int.toString(), mirror, target)
      case _ =>
        ???
    }
  }

  final val BOXED_PRIMITIVE_TYPES = List("Ljava/lang/Integer;", "Ljava/lang/Long;", "Ljava/lang/Boolean;", "Ljava/lang/Byte;", "Ljava/lang/Double;", "Ljava/lang/Float;", "Ljava/lang/Short;")
  final val BOXED_CHAR_TYPE = "Ljava/lang/Character;"

}

// TODO: cache values?

abstract class ArgusValue(val underlying: Value, target: ArgusDebugTarget) extends ArgusDebugElement(target) with IValue {

  // Members declared in org.eclipse.debug.core.model.IValue

  override def isAllocated(): Boolean = true // TODO: should always be true with a JVM, to check. ObjectReference#isCollected ?

  final override def getReferenceTypeName(): String =
    wrapJDIException("Exception while retrieving reference type name") { doGetReferenceTypeName() }

  final override def getValueString(): String =
    wrapJDIException("Exception while retrieving value string") { doGetValueString() }

  final override def getVariables(): Array[IVariable] =
    wrapJDIException("Exception while retrieving variables") { doGetVariables() }

  final override def hasVariables(): Boolean =
    wrapJDIException("Exception while checking if debug element has variables") { doHasVariables() }

  protected def doGetReferenceTypeName(): String
  protected def doGetValueString(): String
  protected def doGetVariables(): Array[IVariable]
  protected def doHasVariables(): Boolean
}

class ArgusArrayReference(override val underlying: ArrayReference, target: ArgusDebugTarget) extends ArgusValue(underlying, target) with IIndexedValue {

  // Members declared in org.eclipse.debug.core.model.IValue

  protected override def doGetReferenceTypeName(): String = "scala.Array"
  protected override def doGetValueString(): String = "%s(%d) (id=%d)".format(ArgusStackFrame.getSimpleName(underlying.referenceType.signature), getSize, underlying.uniqueID)
  protected override def doGetVariables(): Array[IVariable] = getVariables(0, getSize)
  protected override def doHasVariables(): Boolean = getSize > 0

  // Members declared in org.eclipse.debug.core.model.IIndexedValue

  override def getVariable(offset: Int) : IVariable = new ArgusArrayElementVariable(offset, this)

  override def getVariables(offset: Int, length: Int) : Array[IVariable] = (offset until offset + length).map(new ArgusArrayElementVariable(_, this)).toArray

  override def getSize(): Int =
    wrapJDIException("Exception while retrieving size") { underlying.length }

  override def getInitialOffset(): Int = 0

}

class ArgusPrimitiveValue(typeName: String, value: String, override val underlying: Value, target: ArgusDebugTarget) extends ArgusValue(underlying, target) {

  // Members declared in org.eclipse.debug.core.model.IValue

  protected override def doGetReferenceTypeName(): String = typeName
  protected override def doGetValueString(): String = value
  protected override def doGetVariables(): Array[IVariable] = Array()
  protected override def doHasVariables(): Boolean = false

}

class ArgusStringReference(override val underlying: StringReference, target: ArgusDebugTarget) extends ArgusObjectReference(underlying, target) {

  protected override def doGetReferenceTypeName() = "java.lang.String"
  protected override def doGetValueString(): String = """"%s" (id=%d)""".format(underlying.value, underlying.uniqueID)

}

class ArgusObjectReference(override val underlying: ObjectReference, target: ArgusDebugTarget) extends ArgusValue(underlying, target) with HasFieldValue with HasMethodInvocation {
  import ArgusValue._

  // Members declared in org.eclipse.debug.core.model.IValue

  protected override def doGetReferenceTypeName(): String = underlying.referenceType.name

  protected override def doGetValueString(): String = {
    // TODO: move to string builder?
    val refTypeSignature = getReferenceType.signature
    if (BOXED_PRIMITIVE_TYPES.contains(refTypeSignature)) {
      "%s %s (id=%d)".format(ArgusStackFrame.getSimpleName(refTypeSignature), getBoxedPrimitiveValue(), underlying.uniqueID)
    } else if (refTypeSignature == BOXED_CHAR_TYPE) {
      "%s '%s' (id=%d)".format(ArgusStackFrame.getSimpleName(refTypeSignature), getBoxedPrimitiveValue(), underlying.uniqueID)
    } else {
      "%s (id=%d)".format(ArgusStackFrame.getSimpleName(refTypeSignature), underlying.uniqueID)
    }
  }

  protected override def doGetVariables(): Array[IVariable] = {
    import scala.collection.JavaConverters._
    referenceType.allFields.asScala.map(new ArgusFieldVariable(_, this)).sortBy(_.getName).toArray
  }
  protected override def doHasVariables(): Boolean = !referenceType.allFields.isEmpty

  protected override def getReferenceType: ReferenceType = underlying.referenceType()

  protected override def getJdiFieldValue(field: Field): Value = underlying.getValue(field)

  protected[model] override def classType: ClassType = referenceType.asInstanceOf[ClassType]

  protected[model] def jdiInvokeMethod(method: Method, thread: ArgusThread, args: Value*): Value = Option(thread) match {
    case None =>
      logger.debug(s"Cannot invoke method $method on $this because no thread is selected or debugged thread is no longer available")
      val status = new Status(IStatus.ERROR, ArgusDebugPlugin.id, "No thread is selected or debugged thread is no longer available")
      throw new DebugException(status)
    case Some(t) => t.invokeMethod(underlying, method, args: _*)
  }

  // -----

  /** Return the string representation of the boxed primitive value.
   *  Should be called only when this is a boxing instance.
   */
  private def getBoxedPrimitiveValue(): String = {
    ArgusDebugModelPresentation.computeDetail(fieldValue("value"))
  }
}

class ArgusNullValue(target: ArgusDebugTarget) extends ArgusValue(null, target) {

  // Members declared in org.eclipse.debug.core.model.IValue

  protected override def doGetReferenceTypeName(): String = "null"
  protected override def doGetValueString(): String = "null"
  protected override def doGetVariables(): Array[IVariable] = Array() // TODO: cached empty array?
  protected override def doHasVariables(): Boolean = false

}