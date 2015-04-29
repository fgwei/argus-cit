package org.arguside.core.internal.lexical

import org.eclipse.jface.text._
import org.eclipse.jface.text.rules._
import org.arguside.ui.syntax.JawaSyntaxClass
import org.eclipse.jface.preference.IPreferenceStore
import org.arguside.core.lexical.AbstractJawaScanner

class SingleTokenScanner(
  val preferenceStore: IPreferenceStore, syntaxClass: JawaSyntaxClass)
    extends AbstractJawaScanner {

  private var offset: Int = _
  private var length: Int = _
  private var consumed = false

  def setRange(document: IDocument, offset: Int, length: Int) {
    this.offset = offset
    this.length = length
    this.consumed = false
  }

  def nextToken(): IToken =
    if (consumed)
      Token.EOF
    else {
      consumed = true
      getToken(syntaxClass)
    }

  def getTokenOffset = offset

  def getTokenLength = length

}
