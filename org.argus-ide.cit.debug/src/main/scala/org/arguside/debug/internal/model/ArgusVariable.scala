package org.arguside.debug.internal.model

import org.eclipse.debug.core.model.IVariable
import com.sun.jdi.LocalVariable
import com.sun.jdi.Field
import com.sun.jdi.ArrayType
import com.sun.jdi.ObjectReference
import org.eclipse.debug.core.model.IValue

abstract class ArgusVariable(target: ArgusDebugTarget) extends ArgusDebugElement(target) with IVariable {

  // Members declared in org.eclipse.debug.core.model.IValueModification

  override def setValue(value: IValue): Unit = ???
  override def setValue(value: String): Unit = ???
  override def supportsValueModification: Boolean = false // TODO: need real logic
  override def verifyValue(value: IValue): Boolean = ???
  override def verifyValue(value: String): Boolean = ???

  // Members declared in org.eclipse.debug.core.model.IVariable

  final override def getValue(): IValue =
    wrapJDIException("Exception while retrieving variable's value") { doGetValue() }

  final override def getName(): String =
    wrapJDIException("Exception while retrieving variable's name") { doGetName() }

  final override def getReferenceTypeName(): String =
    wrapJDIException("Exception while retrieving variable's reference type name") { doGetReferenceTypeName() }

  override def hasValueChanged: Boolean = false // TODO: need real logic

  protected def doGetValue(): IValue
  protected def doGetName(): String
  protected def doGetReferenceTypeName(): String
}

class ArgusThisVariable(underlying: ObjectReference, stackFrame: ArgusStackFrame) extends ArgusVariable(stackFrame.getDebugTarget) {

  // Members declared in org.eclipse.debug.core.model.IVariable

  override protected def doGetName: String = "this"
  override protected def doGetReferenceTypeName: String = underlying.referenceType.name
  override protected def doGetValue: IValue = new ArgusObjectReference(underlying, getDebugTarget)
}

class ArgusLocalVariable(underlying: LocalVariable, stackFrame: ArgusStackFrame) extends ArgusVariable(stackFrame.getDebugTarget) {

  // Members declared in org.eclipse.debug.core.model.IVariable

  override protected def doGetName(): String = underlying.name
  override protected def doGetReferenceTypeName(): String = underlying.typeName

  // fetching the value for local variables cannot be delayed because the underlying stackframe element may become invalid at any time
  override protected def doGetValue: IValue = ArgusValue(stackFrame.stackFrame.getValue(underlying), getDebugTarget)
}

class ArgusArrayElementVariable(index: Int, arrayReference: ArgusArrayReference) extends ArgusVariable(arrayReference. getDebugTarget) {

  // Members declared in org.eclipse.debug.core.model.IVariable

  override protected def doGetName(): String = "(%s)".format(index)
  override protected def doGetReferenceTypeName(): String = arrayReference.underlying.referenceType.asInstanceOf[ArrayType].componentTypeName
  override protected def doGetValue(): IValue = ArgusValue(arrayReference.underlying.getValue(index), getDebugTarget)

}

class ArgusFieldVariable(field: Field, objectReference: ArgusObjectReference) extends ArgusVariable(objectReference.getDebugTarget) {

  // Members declared in org.eclipse.debug.core.model.IVariable

  override protected def doGetName(): String = field.name
  override protected def doGetReferenceTypeName(): String = field.typeName
  override protected def doGetValue(): IValue = ArgusValue(objectReference.underlying.getValue(field), getDebugTarget)
}