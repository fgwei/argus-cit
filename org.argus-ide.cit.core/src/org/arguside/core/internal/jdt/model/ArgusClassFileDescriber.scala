package org.arguside.core.internal.jdt.model

import java.io.DataInputStream
import java.io.InputStream
import java.io.IOException
import scala.annotation.switch
import scala.collection.mutable.HashMap
import org.eclipse.core.runtime.QualifiedName
import org.eclipse.core.runtime.content.IContentDescriber
import org.eclipse.core.runtime.content.IContentDescription
import org.arguside.logging.HasLogger

object ArgusClassFileDescriber extends HasLogger {
  final val JAVA_MAGIC = 0xCAFEBABE
  final val CONSTANT_UTF8 = 1
  final val CONSTANT_UNICODE = 2
  final val CONSTANT_INTEGER = 3
  final val CONSTANT_FLOAT = 4
  final val CONSTANT_LONG = 5
  final val CONSTANT_DOUBLE = 6
  final val CONSTANT_CLASS = 7
  final val CONSTANT_STRING = 8
  final val CONSTANT_FIELDREF = 9
  final val CONSTANT_METHODREF = 10
  final val CONSTANT_INTFMETHODREF = 11
  final val CONSTANT_NAMEANDTYPE = 12

  def isArgus(contents : InputStream) : Option[String] = {
    try {
      val in = new DataInputStream(contents)

      if (in.readInt() != JAVA_MAGIC)
        return None
      if (in.skipBytes(4) != 4)
        return None

      var sourceFile : String = null
      var isArgus = false

      val pool = new HashMap[Int, String]

      val poolSize = in.readUnsignedShort
      var argusSigIndex = -1
      var argusIndex = -1
      var sourceFileIndex = -1
      var i = 1
      while (i < poolSize) {
        (in.readByte().toInt: @switch) match {
          case CONSTANT_UTF8 =>
            val str = in.readUTF()
            pool(i) = str
            if (argusIndex == -1 || argusSigIndex == -1 || sourceFileIndex == -1) {
              if (argusIndex == -1 && str == "Pilar")
                argusIndex = i
              else if (argusSigIndex == -1 && str == "PilarSig")
                argusSigIndex = i
              else if (sourceFileIndex == -1 && str == "SourceFile")
                sourceFileIndex = i
            }
          case CONSTANT_UNICODE =>
            val toSkip = in.readUnsignedShort()
            if (in.skipBytes(toSkip) != toSkip) return None
          case CONSTANT_CLASS | CONSTANT_STRING =>
            if (in.skipBytes(2) != 2) return None
          case CONSTANT_FIELDREF | CONSTANT_METHODREF | CONSTANT_INTFMETHODREF
             | CONSTANT_NAMEANDTYPE | CONSTANT_INTEGER | CONSTANT_FLOAT =>
            if (in.skipBytes(4) != 4) return None
          case CONSTANT_LONG | CONSTANT_DOUBLE =>
            if (in.skipBytes(8) != 8) return None
            i += 1
          case other =>
            logger.debug("Unknown constant pool id: " + other)
            return None
        }
        i += 1
      }

      if (argusIndex == -1 && argusSigIndex == -1)
        return None

      if (in.skipBytes(6) != 6)
        return None

      val numInterfaces = in.readUnsignedShort()
      val iToSkip = numInterfaces*2
      if (in.skipBytes(iToSkip) != iToSkip)
        return None

      def skipFieldsOrMethods() : Boolean = {
        val num = in.readUnsignedShort()
        var i = 0
        while (i < num) {
          i += 1
          if (in.skipBytes(6) != 6)
            return false

          val numAttributes = in.readUnsignedShort()
          var j = 0
          while (j < numAttributes) {
            j += 1
            val attrNameIndex = in.readUnsignedShort()
            isArgus ||= (attrNameIndex == argusIndex || attrNameIndex == argusSigIndex)
            val numToSkip = in.readInt()
            if (in.skipBytes(numToSkip) != numToSkip)
              return false
          }
        }
        true
      }

      // In this binary parser, skipFieldsOrMethods moves the read pointer
      // by skipping fields/methods definitions in the classfile.
      // This (at-first-glance) repetition is thus important.
      if (!skipFieldsOrMethods())
        return None
      if (!skipFieldsOrMethods())
        return None

      val numAttributes = in.readUnsignedShort()
      var j = 0
      while (j < numAttributes) {
        j += 1
        val attrNameIndex = in.readUnsignedShort()
        if (attrNameIndex == sourceFileIndex) {
          in.readInt()
          val index = in.readUnsignedShort()
          sourceFile = pool(index)
          if (isArgus)
            return Some(sourceFile)
        } else {
          isArgus ||= (attrNameIndex == argusIndex || attrNameIndex == argusSigIndex)
          if (isArgus && sourceFile != null)
            return Some(sourceFile)
          val numToSkip = in.readInt()
          if (in.skipBytes(numToSkip) != numToSkip)
            return None
        }
      }
      None
    } catch {
      case ex : IOException => None
    }
  }
}

class ArgusClassFileDescriber extends IContentDescriber {
  import IContentDescriber.INVALID
  import IContentDescriber.VALID
  import ArgusClassFileDescriber._

  override def describe(contents : InputStream, description : IContentDescription) : Int =
    if (isArgus(contents).isDefined) VALID else INVALID

  override def getSupportedOptions : Array[QualifiedName] = new Array[QualifiedName](0)
}
