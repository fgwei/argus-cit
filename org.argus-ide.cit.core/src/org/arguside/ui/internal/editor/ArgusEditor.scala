package org.arguside.ui.internal.editor

import argus.tools.eclipse.contribution.weaving.jdt.ui.javaeditor.IArgusEditor
import org.eclipse.jdt.ui.text.IJavaPartitions
import org.eclipse.ui.IEditorReference
import org.eclipse.ui.IFileEditorInput
import org.eclipse.ui.IWorkbenchPage
import org.arguside.core.IArgusProject
import org.arguside.core.lexical.ArgusPartitions
import org.arguside.util.Utils.WithAsInstanceOfOpt
import org.arguside.util.eclipse.EclipseUtils
import org.arguside.core.lexical.ArgusCodePartitioner
import org.arguside.ui.editor.ISourceViewerEditor
import org.arguside.ui.editor.InteractiveCompilationUnitEditor

trait ArgusEditor extends IArgusEditor with ISourceViewerEditor with InteractiveCompilationUnitEditor {

  override def createDocumentPartitioner = ArgusCodePartitioner.documentPartitioner()

}

object ArgusEditor {

  val LEGAL_CONTENT_TYPES = Array[String](
    IJavaPartitions.JAVA_DOC,
    IJavaPartitions.JAVA_MULTI_LINE_COMMENT,
    IJavaPartitions.JAVA_SINGLE_LINE_COMMENT,
    IJavaPartitions.JAVA_STRING,
    IJavaPartitions.JAVA_CHARACTER,
    ArgusPartitions.SCALA_MULTI_LINE_STRING)

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
