package org.arguside.core.internal.lexical

import org.eclipse.jface.text.ITypedRegion
import scala.collection.mutable.ListBuffer
import org.sireum.jawa.lexer.JawaLexer
import org.eclipse.jface.text.TypedRegion

object JawaPartitionTokenizer {
  def tokenise(text: String): List[ITypedRegion] = {
    val tokens = new ListBuffer[ITypedRegion]
    val jawatokens = JawaLexer.tokenise(Left(text))
    tokens ++= jawatokens map {
      jtoken =>
        val tokenStart = jtoken.offset
        val tokenLength = jtoken.length
        val contentType = jtoken.tokenType.toString
        new TypedRegion(tokenStart, tokenLength, contentType)
    }
    tokens.toList
  }
}