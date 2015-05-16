package org.arguside.util.eclipse

import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IResource
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility
import org.eclipse.jdt.ui.JavaUI
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.ITextSelection
import org.eclipse.jface.text.Position
import org.eclipse.jface.text.Region
import org.eclipse.jface.text.link.LinkedModeModel
import org.eclipse.jface.text.link.LinkedModeUI
import org.eclipse.jface.text.link.LinkedPosition
import org.eclipse.jface.text.link.LinkedPositionGroup
import org.eclipse.jface.text.source.Annotation
import org.eclipse.jface.text.source.IAnnotationModelExtension2
import org.eclipse.jface.text.source.ISourceViewer
import org.eclipse.ui.IEditorPart
import org.eclipse.ui.IFileEditorInput
import org.eclipse.ui.IWorkbenchPage
import org.eclipse.ui.IWorkbenchWindow
import org.eclipse.ui.PlatformUI
import org.eclipse.ui.part.FileEditorInput
import org.eclipse.ui.texteditor.ITextEditor
import org.arguside.core.compiler.InteractiveCompilationUnit
import org.arguside.core.internal.ArgusPlugin
import org.arguside.core.internal.jdt.model.JawaSourceFile
import org.arguside.ui.editor.ISourceViewerEditor
import org.arguside.ui.editor.InteractiveCompilationUnitEditor
import org.arguside.util.Utils.WithAsInstanceOfOpt

/**
 * Provides helper methods for the text editor of Eclipse, which is a GUI aware
 * class. This means that most methods of this object can only be called when an
 * UI environment is loaded - if this is not the case these methods will fail at
 * runtime.
 */
object EditorUtils {

  /** Opens the element passed as an argument in an Editor and applies the given method to it.
   *  @see [[org.eclipse.jdt.ui.JavaUI.openInEditor]]
   */
  def openEditorAndApply[T](element: IJavaElement)(editor: IEditorPart => T): T =
    editor(JavaUI.openInEditor(element))

  /** Return the compilation unit open in the given editor.*/
  def getEditorCompilationUnit(editor: ITextEditor): Option[InteractiveCompilationUnit] = {
    editor match {
      case icuEditor: InteractiveCompilationUnitEditor =>
        Some(icuEditor.getInteractiveCompilationUnit)
      case _ =>
        None
    }
  }

  /** Returns a region matching the given text selection.
   */
  def textSelection2region(selection: ITextSelection): IRegion =
    new Region(selection.getOffset, selection.getLength)

  /** Returns the current selection for a given editor
   */
  def getTextSelection(editor: ITextEditor): Option[ITextSelection] = {
    for {
      workbenchSite <- Option(editor.getSite)
      provider <- Option(workbenchSite.getSelectionProvider)
      selection <- Option(provider.getSelection)
      textSelection <- selection.asInstanceOfOpt[ITextSelection]
    } yield textSelection
  }

  private def activeWorkbenchWindow: Option[IWorkbenchWindow] =
    Option(PlatformUI.getWorkbench.getActiveWorkbenchWindow)

  private def activePage(w: IWorkbenchWindow): Option[IWorkbenchPage] =
    Option(w.getActivePage)

  private def activeEditor(p: IWorkbenchPage): Option[IEditorPart] =
    if (p.isEditorAreaVisible) Some(p.getActiveEditor) else None

  /** Returns the resource of the active editor if it exists.
   *
   *  This method returns `None` in the following cases:
   *  - It is not executed on the UI thread
   *  - The active selection is not an editor
   *  - The active editor doesn't provide a resource (which is the case if an
   *   [[IClassFile]] is opened)
   */
  def resourceOfActiveEditor: Option[IResource] = for {
    w <- activeWorkbenchWindow
    p <- activePage(w)
    e <- activeEditor(p)
    r <- Option(e.getEditorInput().getAdapter(classOf[IResource]))
  } yield r.asInstanceOf[IResource]

  /** Type-safe downcast of an [[IEditorPart]] to a [[ISourceViewerEditor]].
   */
  def textEditor(e: IEditorPart): Option[ISourceViewerEditor] =
    PartialFunction.condOpt(e) {
      case t: ISourceViewerEditor => t
    }

  private def file(e: ITextEditor): Option[IFile] =
    PartialFunction.condOpt(e.getEditorInput()) {
      case f: IFileEditorInput => f.getFile
    }

  private def selection(e: ITextEditor): Option[ITextSelection] =
    PartialFunction.condOpt(e.getSelectionProvider.getSelection) {
      case s: ITextSelection => s
    }

  /** Applies the side-effecting function passed as an argument to the current editor.
   */
  def doWithCurrentEditor(block: ISourceViewerEditor => Unit): Unit = {
    withCurrentEditor { editor =>
      block(editor)
      None
    }
  }

  /** Applies the function passed as an argument monadically to the current editor.
   */
  def withCurrentEditor[T](block: ISourceViewerEditor => Option[T]): Option[T] = for {
    w <- activeWorkbenchWindow
    p <- activePage(w)
    e <- activeEditor(p)
    t <- textEditor(e)
    b <- block(t)
  } yield b

  /** Applies the function passed as an argument monadically to the given Jawa source file.
   */
  def withCurrentJawaSourceFile[T](block: JawaSourceFile => T): Option[T] = {
    withCurrentEditor { textEditor =>
      file(textEditor) flatMap { file =>
        JawaSourceFile.createFromPath(file.getFullPath.toString) map block
      }
    }
  }

  /** @see [[ withJawaSourceFileAndSelection(JawaSourceFile, ITextSelection): Option[T] ]]
   */
  def withJawaFileAndSelection[T](block: (InteractiveCompilationUnit, ITextSelection) => Option[T]): Option[T] = {
    withCurrentEditor { textEditor =>
      EditorUtils.getEditorCompilationUnit(textEditor) flatMap { icu =>
        selection(textEditor) flatMap { selection =>
          block(icu, selection)
        }
      }
    }
  }

  /** Applies the function passed as an argument monadically to the given Jawa source file and current selection.
   */
  def withJawaSourceFileAndSelection[T](block: (JawaSourceFile, ITextSelection) => Option[T]): Option[T] = {
    withJawaFileAndSelection { (icu, selection) =>
      icu match {
        case ssf: JawaSourceFile => block(ssf, selection)
        case _ => None
      }
    }
  }

  /** Given an `ISourceViewer` it applies `f` on the underlying document's model.
   *  If one of the involved components is `null`, even if `f` returns `null`, this
   *  method returns `None`, otherwise the result of `f`.
   *
   *  This method is UI independent.
   */
  def withDocument[A](sourceViewer: ISourceViewer)(f: IDocument => A): Option[A] =
    for {
      s <- Option(sourceViewer)
      d <- Option(s.getDocument)
      r <- Option(f(d))
    } yield r

  /** Enters the editor in the LinkedModeUI with the given list of position groups.
   *  Each position group is a list of positions with identical strings.
   *  A position is given as an offset and the length.
   */
  def enterMultiLinkedModeUi(positionGroups: List[List[(Int, Int)]], selectFirst: Boolean): Unit =
    EditorUtils.doWithCurrentEditor { editor =>

      val model = new LinkedModeModel {
        positionGroups foreach { ps =>
          this addGroup new LinkedPositionGroup {
            val document = editor.getDocumentProvider.getDocument(editor.getEditorInput)
            ps foreach (p => addPosition(new LinkedPosition(document, p._1, p._2, 0)))
          }
        }
        forceInstall
      }

      val viewer = editor.getViewer

      // by default, an entire symbol is selected when entering linked mode; a nicer
      // behavior when renaming is to leave the cursor/selection as it was, so...

      // save the current selection
      val priorSelection = viewer.getSelectedRange()

      (new LinkedModeUI(model, viewer)).enter

      // restore the selection unless selecting the first instance of the symbol was desired
      if (!selectFirst)
        viewer.setSelectedRange(priorSelection.x, priorSelection.y)
    }

  /** Enters the editor in the LinkedModeUI with the given list of positions.
   *  A position is given as an offset and the length.
   */
  def enterLinkedModeUi(ps: List[(Int, Int)], selectFirst: Boolean): Unit =
    enterMultiLinkedModeUi(ps :: Nil, selectFirst)

  /** Returns the document attached to an editor of the file passed as an argument,
   *  opening the editor if the file is not already opened, just bringing the editor
   *  toplevel otherwise.
   */
  def findOrOpen(file: IFile): Option[IDocument] = {
    for (window <- activeWorkbenchWindow) yield {
      val page = window.getActivePage()
      val part = Option(page.findEditor(new FileEditorInput(file))).getOrElse(EditorUtility.openInEditor(file, true))
      page.bringToTop(part)
      ArgusPlugin().documentProvider.getDocument(part.getEditorInput)
    }
  }
}
