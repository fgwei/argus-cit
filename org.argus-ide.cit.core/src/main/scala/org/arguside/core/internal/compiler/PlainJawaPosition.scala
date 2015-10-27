package org.arguside.core.internal.compiler

import org.arguside.core.compiler.IPositionInformation
import org.arguside.core.compiler.ISourceMap
import org.sireum.jawa.io.SourceFile
import org.sireum.jawa.io.AbstractFile
import org.sireum.jawa.io.FgSourceFile

/** An implementation of position information that is based on a Jawa SourceFile implementation
 */
class PlainJawaPosition(sourceFile: SourceFile) extends IPositionInformation {
  def apply(pos: Int): Int = pos

  def offsetToLine(offset: Int): Int = sourceFile.offsetToLine(offset)

  def lineToOffset(line: Int): Int = sourceFile.lineToOffset(line)
}

/** An implementation of `ISourceMap` that is the identity transformation. */
class PlainJawaInfo(file: AbstractFile, override val originalSource: Array[Char]) extends ISourceMap {
  override lazy val sourceFile = new FgSourceFile(file, jawaSource)
  override val jawaPos = IPositionInformation.plainJawa(sourceFile)
  override val originalPos = IPositionInformation.plainJawa(sourceFile)

  override def jawaLine(line: Int): Int = line
  override def originalLine(line: Int): Int = line
}
