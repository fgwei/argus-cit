package org.arguside.ui.editor

import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.Region
import org.sireum.jawa.sjc.lexer.Chars

object WordFinder {
  /** Returns the region of the word enclosing the given offset in the document.
    * A word is defined as a set of characters surrounded by whitespace characters.
    * If the offset is immediately surrounded by whitespace characters, an empty region is returned.
    *
    * @param document The document that will be used to find the word
    * @param offset   The caret position in the `document`
    * @return The region of the word that contains the passed `offset` in the `document`
    */
  def findWord(document: IDocument, offset: Int): IRegion = {
    val docLenght = document.getLength()
    var end = offset
    while (end < docLenght && !Chars.isWhitespace(document.getChar(end)) && document.getChar(end) != '`') end += 1
    val isGraveAccent = if(document.getChar(end-1) != '.' && document.getChar(end) == '`') true else false
    end = offset
    while (end < docLenght && Chars.isIdentifierPart(document.getChar(end), isGraveAccent)) end += 1

    var start = offset
    while (start > 0 && Chars.isIdentifierPart(document.getChar(start - 1), isGraveAccent)) start -= 1

    start = Math.max(0, start)
    end = Math.min(docLenght, end)

    new Region(start, end - start)
  }
}
