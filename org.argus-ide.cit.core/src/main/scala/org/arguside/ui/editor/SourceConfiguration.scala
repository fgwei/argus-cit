package org.arguside.ui.editor

import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.hyperlink.IHyperlink
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector
import org.arguside.core.internal.hyperlink.DeclarationHyperlinkDetector
import org.arguside.core.internal.hyperlink.JawaHyperlink
import org.sireum.jawa.sjc.parser.JawaSymbol
import org.eclipse.jdt.core.IJavaElement
import org.sireum.jawa.sjc.util.Position

/** Factory object for creating jawa-specific editor goodies, like auto-edits or
 *  hyperlink detectors.
 */
object SourceConfiguration {

  /** A hyperlink detector that navigates to the declaration of the symbol under cursor.
   *
   *  @note For proper functionality, this detector needs an `editor` as part of the context. Make
   *        sure to configure this before returning it to the platform:
   *        {{{
   *          val detector = SourceConfiguration.jawaDeclarationDetector
   *          detector.setContext(editor)
   *        }}}
   */
  def jawaDeclarationDetector: AbstractHyperlinkDetector = DeclarationHyperlinkDetector()

  /** Create a hyperlink that can open a Jawa editor.
   *
   * @param sym            The jawa symbol
   * @param pos            The position at which the editor should be open
   * @param len            The length of the selection in the open editor
   * @param label          A hyperlink label (additional information)
   * @param text           The name of the hyperlink, to be shown in a menu if there's more than one hyperlink
   * @param wordRegion     The region to underline in the start editor
   */
  def jawaHyperlink(elementOrPos: Either[IJavaElement, Position], label: String, text: String, wordRegion: IRegion): IHyperlink =
    new JawaHyperlink(elementOrPos, label, text, wordRegion)
}
