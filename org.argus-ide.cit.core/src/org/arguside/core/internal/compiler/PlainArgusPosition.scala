package org.arguside.core.internal.compiler

import org.arguside.core.compiler.IPositionInformation
import scala.reflect.internal.util.SourceFile
import scala.reflect.internal.util.BatchSourceFile
import scala.reflect.io.AbstractFile
import org.arguside.core.compiler.ISourceMap

/** An implementation of position information that is based on a Jawa SourceFile implementation
 */
class PlainArgusPosition(sourceFile: SourceFile) extends IPositionInformation {
  def apply(pos: Int): Int = pos

  def offsetToLine(offset: Int): Int = sourceFile.offsetToLine(offset)

  def lineToOffset(line: Int): Int = sourceFile.lineToOffset(line)
}

/** An implementation of `ISourceMap` that is the identity transformation. */
class PlainArgusInfo(file: AbstractFile, override val originalSource: Array[Char]) extends ISourceMap {
  override lazy val sourceFile = new BatchSourceFile(file, argusSource)
  override val argusPos = IPositionInformation.plainJawa(sourceFile)
  override val originalPos = IPositionInformation.plainJawa(sourceFile)

  override def argusLine(line: Int): Int = line
  override def originalLine(line: Int): Int = line
}
