package org.arguside.ui.internal.editor

import argus.tools.eclipse.contribution.weaving.jdt.ui.javaeditor.IJawaEditor
import org.eclipse.jdt.ui.text.IJavaPartitions
import org.eclipse.ui.IEditorReference
import org.eclipse.ui.IFileEditorInput
import org.eclipse.ui.IWorkbenchPage
import org.arguside.core.IArgusProject
import org.arguside.core.lexical.JawaPartitions
import org.arguside.util.Utils.WithAsInstanceOfOpt
import org.arguside.util.eclipse.EclipseUtils
import org.arguside.core.lexical.JawaCodePartitioner
import org.arguside.ui.editor.ISourceViewerEditor
import org.arguside.ui.editor.InteractiveCompilationUnitEditor

trait JawaEditor extends IJawaEditor with ISourceViewerEditor with InteractiveCompilationUnitEditor {

  override def createDocumentPartitioner = JawaCodePartitioner.documentPartitioner()

}

object JawaEditor {

  val LEGAL_CONTENT_TYPES = Array[String](
    IJavaPartitions.JAVA_DOC,
    IJavaPartitions.JAVA_MULTI_LINE_COMMENT,
    IJavaPartitions.JAVA_SINGLE_LINE_COMMENT,
    IJavaPartitions.JAVA_STRING,
    IJavaPartitions.JAVA_CHARACTER,
    JawaPartitions.JAWA_MULTI_LINE_STRING)

  import org.arguside.util.Utils.WithAsInstanceOfOpt

  /**
   * Checks whether there's at least one open editor related to given project
   */
  def projectHasOpenEditors(project: IArgusProject): Boolean = {
    def hasOpenEditorForThisProject(page: IWorkbenchPage) = {
      val editorRefs = page.getEditorReferences
      editorRefs exists hasEqualProject
    }

    def hasEqualProject(editorRef: IEditorReference) = {
      val isEqual = for {
        editor <- Option(editorRef.getEditor( /*restore =*/ false))
        input <- editor.getEditorInput.asInstanceOfOpt[IFileEditorInput]
      } yield {
        val file = input.getFile
        project.underlying equals file.getProject
      }
      isEqual.getOrElse(false)
    }

    EclipseUtils.getWorkbenchPages.exists(hasOpenEditorForThisProject)
  }
}
