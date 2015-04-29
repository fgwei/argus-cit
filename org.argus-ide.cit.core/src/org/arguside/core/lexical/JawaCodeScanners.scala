package org.arguside.core.lexical

import org.eclipse.jface.preference.IPreferenceStore
import org.arguside.core.internal.lexical.SingleTokenScanner
import org.arguside.core.internal.lexical.StringTokenScanner
import org.arguside.ui.syntax.{ JawaSyntaxClasses => SSC }
import org.eclipse.jface.text.IDocument
import org.eclipse.jdt.ui.text.IJavaPartitions
import org.arguside.ui.syntax.JawaSyntaxClass
import org.arguside.core.internal.lexical.JawaCodeScanner
import org.arguside.core.internal.lexical.JawaCodeTokenizer
import org.arguside.core.internal.lexical.JawaCommentScanner

/** Entry point to the Jawa code scanners.
 *
 *  Code scanners are made to work on partitions (see [[ScalaCodePartition]]). They parse the content of partitions, and
 *  return a lists of 'token'. A token represent anything remarkable in the code: keyword, braces, id, symbols, ...
 *
 *  The type of the tokens returned by the Scala scanners is defined in [[org.arguside.ui.syntax.ScalaSyntaxClasses]].
 *
 *  @see org.eclipse.jface.text.rules.ITokenScanner
 *  @see org.arguside.core.lexical.ScalaCodePartitioner
 */
object JawaCodeScanners {

  /** Returns a map of all code scanners for Jawa code, associated to the partition id.
   */
  def codeHighlightingScanners(jawaPreferenceStore: IPreferenceStore, javaPreferenceStore: IPreferenceStore): Map[String, AbstractJawaScanner] =
    Map(
      IDocument.DEFAULT_CONTENT_TYPE -> jawaCodeScanner(jawaPreferenceStore),
      IJavaPartitions.JAVA_SINGLE_LINE_COMMENT -> jawaSingleLineCommentScanner(jawaPreferenceStore, javaPreferenceStore),
      IJavaPartitions.JAVA_MULTI_LINE_COMMENT -> jawaMultiLineCommentScanner(jawaPreferenceStore, javaPreferenceStore),
      IJavaPartitions.JAVA_STRING -> stringScanner(jawaPreferenceStore),
      IJavaPartitions.JAVA_CHARACTER -> characterScanner(jawaPreferenceStore),
      JawaPartitions.JAWA_MULTI_LINE_STRING -> multiLineStringScanner(jawaPreferenceStore))

  /** Creates a code scanner which returns a single token for the configured region.
   */
  def singleTokenScanner(preferenceStore: IPreferenceStore, syntaxClass: JawaSyntaxClass): AbstractJawaScanner =
    new SingleTokenScanner(preferenceStore, syntaxClass)

  /** Creates a code scanner for plain Jawa code.
   */
  def jawaCodeScanner(preferenceStore: IPreferenceStore): AbstractJawaScanner =
    new JawaCodeScanner(preferenceStore)

  /** Creates a code tokenizer for plain Jawa code.
   *
   *  This tokenizer has a richer interface, but is not compatible with the Eclipse [[org.eclipse.jface.text.rules.ITokenScanner]].
   *
   *  @see PilarCodeTokenizer
   */
  def jawaCodeTokenizer(): IJawaCodeTokenizer =
    new JawaCodeTokenizer()

  private def jawaSingleLineCommentScanner(
    preferenceStore: IPreferenceStore,
    javaPreferenceStore: IPreferenceStore): AbstractJawaScanner =
    new JawaCommentScanner(preferenceStore, javaPreferenceStore, SSC.SINGLE_LINE_COMMENT, SSC.TASK_TAG)

  private def jawaMultiLineCommentScanner(
    preferenceStore: IPreferenceStore,
    javaPreferenceStore: IPreferenceStore): AbstractJawaScanner =
    new JawaCommentScanner(preferenceStore, javaPreferenceStore, SSC.MULTI_LINE_COMMENT, SSC.TASK_TAG)

  private def multiLineStringScanner(preferenceStore: IPreferenceStore): AbstractJawaScanner =
    new SingleTokenScanner(preferenceStore, SSC.MULTI_LINE_STRING)

  private def stringScanner(preferenceStore: IPreferenceStore): AbstractJawaScanner =
    new StringTokenScanner(preferenceStore, SSC.ESCAPE_SEQUENCE, SSC.STRING)

  private def characterScanner(preferenceStore: IPreferenceStore): AbstractJawaScanner =
    new StringTokenScanner(preferenceStore, SSC.ESCAPE_SEQUENCE, SSC.CHARACTER)

}

/** A tokenizer for plain Jawa code. Like the code scanners in [[JawaCodeScanners]], but returns tokens with a richer interface.
 */
trait IJawaCodeTokenizer {

  import IJawaCodeTokenizer.Token

  /** Tokenizes a string of Jawa code.
   *
   *  @param contents the string to tokenize
   *  @param offset If `contents` is a snippet within a larger document, use `offset` to indicate it `contents` offset within the larger document so that resultant tokens are properly positioned with respect to the larger document.
   *  @return an sequence of the tokens for the given string
   */
  def tokenize(contents: String, offset: Int = 0): IndexedSeq[Token]

}

object IJawaCodeTokenizer {

  /** A Jawa token.
   *
   *  @param offset the position of the first character of the token
   *  @param length the length of the token
   *  @param syntaxClass the class of the token
   */
  case class Token(offset: Int, length: Int, syntaxClass: JawaSyntaxClass)
}
