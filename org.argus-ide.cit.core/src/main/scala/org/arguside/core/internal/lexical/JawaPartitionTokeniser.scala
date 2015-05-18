package org.arguside.core.internal.lexical

import org.eclipse.jdt.ui.text.IJavaPartitions._
import org.eclipse.jface.text._
import org.eclipse.jface.text.IDocument.DEFAULT_CONTENT_TYPE
import scala.annotation.switch
import scala.annotation.tailrec
import scala.collection.mutable.Stack
import scala.collection.mutable.ListBuffer
import org.arguside.core.lexical.JawaPartitions._
import org.sireum.util._

object JawaPartitionTokeniser {

  def tokenise(text: String): IList[ITypedRegion] = {
    val tokeniser = new JawaPartitionTokeniser(text)
    tokeniser.tokenise(text)
  }

}

/** @see org.arguside.core.lexical.JawaCodePartitioner
 */
class JawaPartitionTokeniser(text: String) {
  import org.arguside.ui.syntax.{ JawaSyntaxClasses => ssc }
  private val tokenizer = new JawaCodeTokenizerJawaCompilerBased
    
  def tokenise(text: String): IList[ITypedRegion] = {
    val tokens = tokenizer.tokenize(text)
    val regions: IList[ITypedRegion] = tokens.map{
      token =>
        val tokenStart: Int = token.offset
        val tokenLength: Int = token.length
        val contentType: String = token.syntaxClass match {
          case ssc.CHARACTER => JAVA_CHARACTER
          case ssc.STRING => JAVA_STRING
          case ssc.MULTI_LINE_STRING => JAWA_MULTI_LINE_STRING
          case ssc.SINGLE_LINE_COMMENT => JAVA_SINGLE_LINE_COMMENT
          case ssc.MULTI_LINE_COMMENT => JAVA_MULTI_LINE_COMMENT
          case ssc.DOC_COMMENT => JAVA_DOC
          case _ => DEFAULT_CONTENT_TYPE
        }
        new TypedRegion(tokenStart, tokenLength, contentType)
    }.toList    
    regions
  }

}
