package org.arguside.util

import org.eclipse.jdt.core.IBuffer
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.Region
import org.sireum.jawa.sjc.lexer.Chars._
import org.sireum.jawa.sjc.lexer.Chars

object JawaWordFinder {

  private def docToSeq(doc: IDocument) = new IndexedSeq[Char] {
    override def apply(i: Int) = doc.getChar(i)
    override def length = doc.getLength
  }

  private def bufferToSeq(buf: IBuffer) = new IndexedSeq[Char] {
    override def apply(i: Int) = buf.getChar(i)
    override def length = buf.getLength
  }

  /** See [[findWord(IndexedSeq[Char],Int):IRegion]]. */
  def findWord(document: IDocument, offset: Int): IRegion =
    findWord(docToSeq(document), offset)

  /** See [[findWord(IndexedSeq[Char],Int):IRegion]]. */
  def findWord(buffer: IBuffer, offset: Int): IRegion =
    findWord(bufferToSeq(buffer), offset)

  def findWord(document: IndexedSeq[Char], offset: Int): IRegion = {
    if (offset < 0 || offset > document.length) throw new IndexOutOfBoundsException("Received an invalid offset for word finding.")
    val docLenght = document.size
    var end = offset
    while (end < docLenght && !Chars.isWhitespace(document(end)) && document(end) != '`') end += 1
    val isGraveAccent = if(document(end-1) != '.' && document(end) == '`') true else false
    end = offset
    while (end < docLenght && Chars.isIdentifierPart(document(end), isGraveAccent)) end += 1

    var start = offset
    while (start > 0 && Chars.isIdentifierPart(document(start - 1), isGraveAccent)) start -= 1

    start = Math.max(0, start)
    end = Math.min(docLenght, end)

    new Region(start, end - start)
  }

  /** See [[findCompletionPoint(IndexedSeq[Char],Int):IRegion]]. */
  def findCompletionPoint(document: IDocument, offset: Int): IRegion =
    findCompletionPoint(docToSeq(document), offset)

  /** See [[findCompletionPoint(IndexedSeq[Char],Int):IRegion]]. */
  def findCompletionPoint(buffer: IBuffer, offset: Int): IRegion =
    findCompletionPoint(bufferToSeq(buffer), offset)

  /**
   * Find the point after which a completion should be inserted in the document.
   */
  def findCompletionPoint(document: IndexedSeq[Char], offset0: Int): IRegion = {
    def isWordPart(ch: Char) = isIdentifierPart(ch, true) || isOperatorPart(ch)

    val offset = if (offset0 >= document.length) (document.length - 1) else offset0
    val ch = document(offset)
    if (isWordPart(ch))
      findWord(document, offset)
    else if (offset > 0 && isWordPart(document(offset - 1)))
      findWord(document, offset - 1)
    else
      new Region(offset, 0)
  }

  /** Returns the length of the identifier which is located at the offset position. */
  def identLenAtOffset(doc: IDocument, offset: Int): Int =
    JawaWordFinder.findWord(doc, offset).getLength()
}