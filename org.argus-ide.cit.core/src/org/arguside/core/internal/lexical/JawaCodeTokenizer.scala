package org.arguside.core.internal.lexical

import org.arguside.ui.syntax.JawaSyntaxClass
import org.sireum.jawa.lexer.JawaLexer
import org.arguside.ui.syntax.JawaToSyntaxClass
import scala.annotation.tailrec
import org.eclipse.jface.text.IDocument
import org.sireum.jawa.lexer.{ Token => JawaToken }
import org.sireum.jawa.lexer.Tokens._
import org.arguside.core.lexical.IJawaCodeTokenizer
import org.arguside.ui.syntax.JawaSyntaxClasses

class JawaCodeTokenizer() extends IJawaCodeTokenizer {

  import IJawaCodeTokenizer.Token

  def tokenize(contents: String, offset: Int = 0): IndexedSeq[Token] = {
    val token = JawaLexer.createRawLexer(Left(contents)).toIndexedSeq.init

    /**
     * Heuristic to distinguish the macro keyword from uses as an identifier. To be 100% accurate requires a full parse,
     * which would be too slow, but this is hopefully adequate.
     */
    def isMacro(token: JawaToken, pos: Int): Boolean =
      token.tokenType.isId &&
      findMeaningfulToken(pos + 1, shift = 1).exists(token => token.tokenType.isId) &&
      findMeaningfulToken(pos - 1, shift = -1).exists(_.tokenType == EQUALS)

    /**
     * Scan forwards or backwards for nearest token that is neither whitespace nor comment
     */
    @tailrec
    def findMeaningfulToken(pos: Int, shift: Int): Option[JawaToken] =
      if (pos <= 0 || pos >= token.length)
        None
      else {
        val tok = token(pos)
        tok.tokenType match {
          case WS | LINE_COMMENT | MULTILINE_COMMENT =>
            findMeaningfulToken(pos + shift, shift)
          case _ =>
            Some(tok)
        }
      }

    /* Denotes the class of a token. */
    def tokenClass(token: JawaToken, pos: Int) =
      if (isMacro(token, pos)) JawaSyntaxClasses.KEYWORD
      else JawaToSyntaxClass(token)

    token.zipWithIndex map {
      case (tok, i) =>
        Token(tok.offset + offset, tok.length, tokenClass(tok, i))
    }
  }

}