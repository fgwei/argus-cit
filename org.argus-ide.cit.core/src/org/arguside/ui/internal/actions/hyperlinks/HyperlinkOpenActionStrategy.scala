package org.arguside.ui.internal.actions.hyperlinks

import org.arguside.core.internal.hyperlink.BaseHyperlinkDetector
import org.arguside.core.internal.jdt.model.JawaCompilationUnit
import org.arguside.util.eclipse.EditorUtils
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor
import org.eclipse.jface.text.ITextSelection

trait HyperlinkOpenActionStrategy {
//  protected def detectionStrategy: BaseHyperlinkDetector

//  protected def openHyperlink(editor: JavaEditor) {
//    getTextSelection(editor) map { selection =>
//      withArgusCompilatioUnit(editor) { scu =>
//        scu.followReference(detectionStrategy, editor, selection)
//      }
//    }
//  }

  private def withJawaCompilatioUnit[T](editor: JavaEditor)(f: JawaCompilationUnit => T): Option[T] = {
    val inputJavaElement = EditorUtility.getEditorInputJavaElement(editor, false)
    Option(inputJavaElement) map (_.asInstanceOf[JawaCompilationUnit]) map (f)
  }

//  protected def isEnabled(editor: JavaEditor): Boolean = getTextSelection(editor) map { textSelection =>
//    val region = EditorUtils.textSelection2region(textSelection)
//    detectionStrategy.detectHyperlinks(editor, region, canShowMultipleHyperlinks = false) != null
//  } getOrElse(false)

  private def getTextSelection(editor: JavaEditor): Option[ITextSelection] = EditorUtils.getTextSelection(editor)
}
