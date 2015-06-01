package org.arguside.ui.internal.actions.hyperlinks

import argus.tools.eclipse.contribution.weaving.jdt.ui.actions.IOpenActionProvider
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor
import org.eclipse.jdt.ui.actions.OpenAction

/**
 * @author fgwei
 */
class OpenActionProvider extends IOpenActionProvider {
  override def getOpenAction(editor: JavaEditor): OpenAction = new HyperlinkOpenAction(editor)
}