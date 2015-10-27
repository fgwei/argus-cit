package org.arguside.core.internal.lexical

import org.arguside.ui.syntax.JawaSyntaxClass
import org.arguside.ui.syntax.JawaSyntaxClasses
import org.arguside.ui.syntax.JawaTokenToSyntaxClass
import scala.annotation.tailrec
import org.eclipse.jface.text.IDocument
import org.sireum.jawa.sjc.lexer.{ Token => JawaToken }
import org.sireum.jawa.sjc.lexer.Tokens._
import org.arguside.core.lexical.JawaCodeTokenizer
import org.sireum.jawa.sjc.lexer.JawaLexer
import org.sireum.jawa.DefaultReporter

class JawaCodeTokenizerJawaCompilerBased extends JawaCodeTokenizer {

  import JawaCodeTokenizer.Token

  def tokenize(contents: String, offset: Int = 0): IndexedSeq[Token] = {
    val tokens = JawaLexer.createRawLexer(Left(contents), new DefaultReporter).toIndexedSeq.init

    /**
     * Scan forwards or backwards for nearest token that is neither whitespace nor comment
     */
    @tailrec
    def findMeaningfulToken(pos: Int, shift: Int): Option[JawaToken] =
      if (pos <= 0 || pos >= tokens.length)
        None
      else {
        val tok = tokens(pos)
        tok.tokenType match {
          case WS | LINE_COMMENT | MULTILINE_COMMENT | DOC_COMMENT =>
            findMeaningfulToken(pos + shift, shift)
          case _ =>
            Some(tok)
        }
      }
    
    /* Denotes the class of a token. */
    def tokenClass(token: JawaToken, pos: Int) =
      JawaTokenToSyntaxClass(token)

    tokens.zipWithIndex map {
      case (tok, i) =>
        Token(tok.offset + offset, tok.length, tokenClass(tok, i))
    }
  }

}