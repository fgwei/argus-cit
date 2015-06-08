package org.arguside.ui.internal.editor

import org.eclipse.jface.text.ITextDoubleClickStrategy
import org.eclipse.jface.text.ITextViewer
import org.arguside.ui.editor.WordFinder


/**
 * @author fgwei
 */
class JawaDoubleClickStrategy extends ITextDoubleClickStrategy {
  override def doubleClicked(part: ITextViewer) = {
    val offset = part.getSelectedRange().x
    val region = WordFinder.findWord(part.getDocument, offset)
    part.setSelectedRange(region.getOffset, region.getLength)
  }
}