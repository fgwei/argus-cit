package org.arguside.ui.editor

import org.arguside.core.internal.hyperlink.DeclarationHyperlinkDetector
import org.arguside.core.internal.hyperlink.ArgusHyperlink
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.hyperlink.IHyperlink
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector

/** Factory object for creating argus-specific editor goodies, like auto-edits or
 *  hyperlink detectors.
 */
object SourceConfiguration {

  /** A hyperlink detector that navigates to the declaration of the symbol under cursor.
   *
   *  @note For proper functionality, this detector needs an `editor` as part of the context. Make
   *        sure to configure this before returning it to the platform:
   *        {{{
   *          val detector = SourceConfiguration.scalaDeclarationDetector
   *          detector.setContext(editor)
   *        }}}
   */
  def argusDeclarationDetector: AbstractHyperlinkDetector = DeclarationHyperlinkDetector()

  /** Create a hyperlink that can open a Scala editor.
   *
   * @param openableOrUnit The unit to open (either an Openable, or an `InteractiveCompilationUnit`)
   * @param pos            The position at which the editor should be open
   * @param len            The length of the selection in the open editor
   * @param label          A hyperlink label (additional information)
   * @param text           The name of the hyperlink, to be shown in a menu if there's more than one hyperlink
   * @param wordRegion     The region to underline in the start editor
   */
  def argusHyperlink(openableOrUnit: AnyRef, region: IRegion, label: String, text: String, wordRegion: IRegion): IHyperlink =
    new ArgusHyperlink(openableOrUnit, region, label, text, wordRegion)
}
