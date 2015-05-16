package org.arguside.ui.internal.editor

import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditor
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds
import org.eclipse.jface.action.Action
import org.eclipse.jface.text.ITextSelection
import org.arguside.core.internal.jdt.model.JawaClassFile
import org.arguside.core.internal.jdt.model.JawaCompilationUnit

class JawaClassFileEditor extends ClassFileEditor with JawaCompilationUnitEditor {

  override def getElementAt(offset : Int) : IJavaElement = {
    getInputJavaElement match {
      case scf : JawaClassFile => scf.getElementAt(offset)
      case _ => null
    }
  }

  override def getCorrespondingElement(element : IJavaElement) : IJavaElement = {
      getInputJavaElement match {
        case scf : JawaClassFile => scf.getCorrespondingElement(element).getOrElse(super.getCorrespondingElement(element))
        case _ => super.getCorrespondingElement(element)
    }
  }

//  override protected def createActions() {
//    super.createActions()
//    val openAction = new Action {
//      override def run {
//        Option(getInputJavaElement) map (_.asInstanceOf[JawaCompilationUnit]) foreach { scu =>
//         scu.followDeclaration(JawaClassFileEditor.this, getSelectionProvider.getSelection.asInstanceOf[ITextSelection])
//        }
//      }
//    }
//    openAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_EDITOR)
//    setAction("OpenEditor", openAction)
//  }

//  override def createSemanticHighlighter: TextPresentationHighlighter =
//    TextPresentationEditorHighlighter(this, semanticHighlightingPreferences, _ => (), _ => ())
//
//  override def forceSemanticHighlightingOnInstallment: Boolean = true
}
