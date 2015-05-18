package org.arguside.core.internal.lexical

import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.rules.IToken
import org.eclipse.jface.text.rules.Token
import org.arguside.core.lexical.AbstractJawaScanner

/**
 * Scans Jawa source code snippets and divides them into their corresponding
 * token.
 */
class JawaCodeScanner(
  val preferenceStore: IPreferenceStore)
    extends AbstractJawaScanner {

  import org.arguside.core.lexical.JawaCodeTokenizer.{Token => JToken}

  private val tokenizer = new JawaCodeTokenizerJawaCompilerBased

  private var ranges: IndexedSeq[JToken] = _
  private var index: Int = _
  private var length: Int = _
  private var offset: Int = _

  def setRange(document: IDocument, offset: Int, length: Int) {
    ranges = tokenizer.tokenize(document.get(offset, length), offset)
    index = 0

    if (!ranges.isEmpty) {
      val JToken(start, len, _) = ranges(index)
      this.offset = start
      this.length = len
    }
  }

  def nextToken(): IToken =
    if (index >= ranges.size)
      Token.EOF
    else {
      val JToken(start, len, syntaxClass) = ranges(index)
      val tok = getToken(syntaxClass)
      index += 1
      offset = start
      length = len
      tok
    }

  def getTokenOffset(): Int = offset

  def getTokenLength(): Int = length

}