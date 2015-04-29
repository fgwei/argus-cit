package org.arguside.core.lexical

import org.eclipse.jface.text.rules.Token
import org.eclipse.jface.text.rules.ITokenScanner
import org.arguside.ui.syntax.JawaSyntaxClass
import org.eclipse.jface.util.PropertyChangeEvent
import org.eclipse.jface.preference.IPreferenceStore

/** Base class for Argus specific token scanners.
 *
 *  @see org.scalaide.core.lexical.ArgusCodeScanners.
 */
trait AbstractJawaScanner extends ITokenScanner {

  /** Updates the UI configuration for the tokens managed by this scanner,
   *  according to the new preferences.
   */
  def adaptToPreferenceChange(event: PropertyChangeEvent) =
    for ((syntaxClass, token) <- tokens)
      token.setData(getTextAttribute(syntaxClass))

  /** Returns the preference store used to configure the tokens managed by
   *  this scanner.
   */
  protected def preferenceStore: IPreferenceStore

  /** Returns the token corresponding to the given [[JawaSyntaxClass]].
   */
  protected def getToken(syntaxClass: JawaSyntaxClass): Token =
    tokens.getOrElse(syntaxClass, createToken(syntaxClass))

  private var tokens: Map[JawaSyntaxClass, Token] = Map()

  private def createToken(syntaxClass: JawaSyntaxClass) = {
    val token = new Token(getTextAttribute(syntaxClass))
    tokens = tokens + (syntaxClass -> token)
    token
  }

  private def getTextAttribute(syntaxClass: JawaSyntaxClass) = syntaxClass.getTextAttribute(preferenceStore)

}
