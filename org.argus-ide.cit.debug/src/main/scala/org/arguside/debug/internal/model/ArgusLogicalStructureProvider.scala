/*
 * Copyright (c) 2014 Contributor. All rights reserved.
 */
package org.arguside.debug.internal.model

import scala.collection.JavaConverters.asScalaBufferConverter

import org.eclipse.debug.core.ILogicalStructureProvider
import org.eclipse.debug.core.ILogicalStructureType
import org.eclipse.debug.core.model.IValue
import org.arguside.debug.internal.ArgusDebugPlugin
import org.arguside.debug.internal.ArgusDebugger
import org.arguside.logging.HasLogger

import com.sun.jdi.BooleanValue
import com.sun.jdi.ClassType
import com.sun.jdi.ReferenceType

class ArgusLogicalStructureProvider extends ILogicalStructureProvider {

  override def getLogicalStructureTypes(value: IValue): Array[ILogicalStructureType] = {
    value match {
      case objectReference: ArgusObjectReference if ArgusLogicalStructureProvider.isArgusCollection(objectReference) =>
        Array(ArgusCollectionLogicalStructureType)
      case _ =>
        ArgusLogicalStructureProvider.emptyLogicalStructureTypes
    }
  }

}

object ArgusLogicalStructureProvider extends HasLogger {

  private lazy val emptyLogicalStructureTypes: Array[ILogicalStructureType] = Array.empty

  def isArgusCollection(objectReference: ArgusObjectReference): Boolean =
    objectReference.wrapJDIException("Exception while checking if passed object reference is a Argus collection type") {
      checkIfImplements(objectReference.referenceType(), "scala.collection.TraversableOnce")
    }

  def isTraversableLike(objectReference: ArgusObjectReference): Boolean =
    objectReference.wrapJDIException("Exception while checking if passed object reference is TraversableLike") {
      checkIfImplements(objectReference.referenceType(), "scala.collection.TraversableLike")
    }

  /**
   * Checks 'implements' with Java meaning
   */
  def implements(classType: ClassType, interfaceName: String): Boolean = {
    import scala.collection.JavaConverters._
    classType.allInterfaces.asScala.exists(_.name == interfaceName)
  }

  private def checkIfImplements(refType: ReferenceType, interfaceName: String) = refType match {
    case classType: ClassType =>
      implements(classType, interfaceName)
    case _ => // TODO: ArgusObjectReference should always reference objects of class type, never of array type. Can we just cast?
      false
  }

  // All these methods, when using default ArgusDebugger.currentThread, won't work in ExpressionEvaluator's tree view,
  // if user will switch debugged thread. It's problem that thread cannot be just cached (JDI limitation).
  // Right now we only inform user that there's problem with chosen thread and he has to change it to this proper one
  // to be able to call these operations correctly.
  def hasDefiniteSize(collectionRef: ArgusObjectReference, thread: ArgusThread = ArgusDebugger.currentThread): Boolean =
    collectionRef.wrapJDIException("Exception while checking if collection has definite size") {
      collectionRef.invokeMethod("hasDefiniteSize", "()Z", thread)
        .asInstanceOf[ArgusPrimitiveValue].underlying
        .asInstanceOf[BooleanValue]
        .value()
    }

  def callIsEmpty(collectionRef: ArgusObjectReference, thread: ArgusThread = ArgusDebugger.currentThread): Boolean =
    collectionRef.wrapJDIException("Exception while checking if collection is empty") {
      collectionRef.invokeMethod("isEmpty", "()Z", thread)
        .asInstanceOf[ArgusPrimitiveValue].underlying
        .asInstanceOf[BooleanValue]
        .value()
    }

  def callToArray(collectionRef: ArgusObjectReference, thread: ArgusThread = ArgusDebugger.currentThread): ArgusArrayReference =
    collectionRef.wrapJDIException("Exception while converting collection to Array") {
      val (manifestObject, toArraySignature) = 
        (collectionRef.getDebugTarget().objectByName("scala.reflect.Manifest", false, null), "(Lscala/reflect/ClassManifest;)Ljava/lang/Object;")


      // get Manifest.Any, needed to call toArray(..)
      val anyManifestObject = manifestObject.invokeMethod("Any", thread) match {
        case o: ArgusObjectReference =>
          o
        case _ =>
          // in case something changes in the next versions of Argus
          throw new Exception("Unexpected return value for Manifest.Any()")
      }

      collectionRef.invokeMethod("toArray", toArraySignature, thread, anyManifestObject)
        .asInstanceOf[ArgusArrayReference]
    }

  def splitCollection(traversableLikeRef: ArgusObjectReference, splitAtIndex: Int, thread: ArgusThread = ArgusDebugger.currentThread): (ArgusObjectReference, ArgusObjectReference) =
    traversableLikeRef.wrapJDIException("Exception while splitting collection at index $index") {
      val arg = ArgusValue(splitAtIndex, traversableLikeRef.getDebugTarget())
      val tupleWithParts = traversableLikeRef.invokeMethod("splitAt", "(I)Lscala/Tuple2;", thread, arg)
        .asInstanceOf[ArgusObjectReference]

      val firstPart = getElementOfTuple(tupleWithParts, 1, thread)
      val secondPart = getElementOfTuple(tupleWithParts, 2, thread)
      (firstPart, secondPart)
    }

  private def getElementOfTuple(tupleRef: ArgusObjectReference, elementNumber: Int, thread: ArgusThread) = {
    require(elementNumber > 0, s"Tuple element number must be positive")

    tupleRef.invokeMethod(s"_$elementNumber", "()Ljava/lang/Object;", thread)
      .asInstanceOf[ArgusObjectReference]
  }
}

object ArgusCollectionLogicalStructureType extends ILogicalStructureType with HasLogger {

  // Members declared in org.eclipse.debug.core.ILogicalStructureType

  override def getDescription(): String = "Flat the Argus collections"

  override val getId: String = ArgusDebugPlugin.id + ".logicalstructure.collection"

  // Members declared in org.eclipse.debug.core.model.ILogicalStructureTypeDelegate

  override def getLogicalStructure(value: IValue): IValue =
    callToArray(value).getOrElse(value)

  override def providesLogicalStructure(value: IValue): Boolean = true // TODO: check that as it is created by the provider, it is never used with other values

  // Members declared in org.eclipse.debug.core.model.ILogicalStructureTypeDelegate2

  override def getDescription(value: IValue): String = getDescription

  // other methods

  /**
   * Tries to call toArray on given value.
   */
  private def callToArray(value: IValue): Option[IValue] = {
    val scalaValue = value.asInstanceOf[ArgusObjectReference]

    try {
      Some(ArgusLogicalStructureProvider.callToArray(scalaValue))
    } catch {
      case e: Exception =>
        // fail gracefully in case of problem
        logger.debug("Failed to compute logical structure for '%s'".format(scalaValue), e)
        None
    }
  }
}
