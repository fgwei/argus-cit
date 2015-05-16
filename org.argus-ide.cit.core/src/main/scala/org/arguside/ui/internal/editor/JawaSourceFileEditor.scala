package org.arguside.ui.internal.editor

import java.util.ResourceBundle

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.SynchronizedBuffer

import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.jdt.core.dom.CompilationUnit
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.SelectionHistory
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.StructureSelectHistoryAction
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.StructureSelectionAction
import org.eclipse.jdt.internal.ui.text.java.IJavaReconcilingListener
import org.eclipse.jdt.ui.PreferenceConstants
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds
import org.eclipse.jface.action.Action
import org.eclipse.jface.action.IContributionItem
import org.eclipse.jface.action.MenuManager
import org.eclipse.jface.action.Separator
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.IDocumentExtension4
import org.eclipse.jface.text.ITextOperationTarget
import org.eclipse.jface.text.ITextSelection
import org.eclipse.jface.text.ITextViewerExtension
import org.eclipse.jface.text.Position
import org.eclipse.jface.text.information.InformationPresenter
import org.eclipse.jface.text.source.Annotation
import org.eclipse.jface.text.source.IAnnotationModel
import org.eclipse.jface.util.PropertyChangeEvent
import org.eclipse.jface.viewers.ISelection
import org.eclipse.ui.ISelectionListener
import org.eclipse.ui.IWorkbenchCommandConstants
import org.eclipse.ui.IWorkbenchPart
import org.eclipse.ui.texteditor.IAbstractTextEditorHelpContextIds
import org.eclipse.ui.texteditor.ITextEditorActionConstants
import org.eclipse.ui.texteditor.TextOperationAction
import org.arguside.core.internal.ArgusPlugin
import org.arguside.core.internal.extensions.SemanticHighlightingParticipants
import org.arguside.core.internal.jdt.model.JawaCompilationUnit
import org.arguside.ui.internal.actions
import org.arguside.ui.internal.editor.decorators.semantichighlighting.TextPresentationEditorHighlighter
import org.arguside.ui.internal.editor.decorators.semantichighlighting.TextPresentationHighlighter
import org.arguside.ui.internal.preferences.EditorPreferencePage
import org.arguside.util.Utils
import org.arguside.util.eclipse.EclipseUtils
import org.arguside.util.eclipse.EditorUtils
import org.arguside.util.internal.eclipse.AnnotationUtils.RichModel
import org.arguside.util.ui.DisplayThread

class JawaSourceFileEditor extends CompilationUnitEditor with JawaCompilationUnitEditor { self =>
  import JawaSourceFileEditor._

  private val reconcilingListeners: ReconcilingListeners = new JawaSourceFileEditor.ReconcilingListeners

  /**
   * Contains references to all extensions provided by the extension point
   * `org.argus-ide.sdt.core.semanticHighlightingParticipants`.
   *
   * These extensions are mainly provided by users, therefore any accesss need
   * to be wrapped in a safe runner.
   */
  private lazy val semanticHighlightingParticipants = new IJavaReconcilingListener {

    def nameOf[A](a: A) = a.getClass().getName()

    val exts = getSourceViewer() match {
      case jsv: JavaSourceViewer => SemanticHighlightingParticipants.extensions flatMap { ext =>
        EclipseUtils.withSafeRunner(s"Error occurred while creating semantic action of '${nameOf(ext)}'.") {
          ext.participant(jsv)
        }
      }
    }

    override def aboutToBeReconciled() = ()
    override def reconciled(ast: CompilationUnit, forced: Boolean, progressMonitor: IProgressMonitor) = {
      self.getInteractiveCompilationUnit() match {
        case scu: JawaCompilationUnit => exts foreach { ext =>
          EclipseUtils.withSafeRunner(s"Error occurred while executing '${nameOf(ext)}'.") {
            ext(scu)
          }
        }
        case _ =>
      }
    }
  }

  setPartName("Jawa Editor")
  setDocumentProvider(ArgusPlugin().documentProvider)

  override protected def createActions() {
    super.createActions()

    val cutAction = new TextOperationAction(bundleForConstructedKeys, "Editor.Cut.", this, ITextOperationTarget.CUT) //$NON-NLS-1$
    cutAction.setHelpContextId(IAbstractTextEditorHelpContextIds.CUT_ACTION)
    cutAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_CUT)
    setAction(ITextEditorActionConstants.CUT, cutAction)

    val copyAction = new TextOperationAction(bundleForConstructedKeys, "Editor.Copy.", this, ITextOperationTarget.COPY, true) //$NON-NLS-1$
    copyAction.setHelpContextId(IAbstractTextEditorHelpContextIds.COPY_ACTION)
    copyAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY)
    setAction(ITextEditorActionConstants.COPY, copyAction)

    val pasteAction = new TextOperationAction(bundleForConstructedKeys, "Editor.Paste.", this, ITextOperationTarget.PASTE) //$NON-NLS-1$
    pasteAction.setHelpContextId(IAbstractTextEditorHelpContextIds.PASTE_ACTION)
    pasteAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_PASTE)
    setAction(ITextEditorActionConstants.PASTE, pasteAction)

    val selectionHistory = new SelectionHistory(this)

    val historyAction = new StructureSelectHistoryAction(this, selectionHistory)
    historyAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_LAST)
    setAction(StructureSelectionAction.HISTORY, historyAction)
    selectionHistory.setHistoryAction(historyAction)

    // disable Java indent logic, which is otherwise invoked when the tab key is entered
    setAction("IndentOnTab", null)

//    val openAction = new Action {
//      private def jawaCompilationUnit: Option[JawaCompilationUnit] =
//        Option(getInteractiveCompilationUnit) map (_.asInstanceOf[JawaCompilationUnit])
//
//      override def run {
//        jawaCompilationUnit foreach { scu =>
//          scu.followDeclaration(JawaSourceFileEditor.this, getSelectionProvider.getSelection.asInstanceOf[ITextSelection])
//        }
//      }
//    }
//    openAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_EDITOR)
//    setAction("OpenEditor", openAction)
  }

  /**
   * The tabs to spaces converter of the editor is not partition aware,
   * therefore we disable it here. There is an auto edit strategy configured in
   * the [[ArgusSourceViewerConfiguration]] that handles the conversion for each
   * partition separately.
   */
  override def isTabsToSpacesConversionEnabled(): Boolean =
    false

  override protected def initializeKeyBindingScopes() {
    setKeyBindingScopes(Array(JAWA_EDITOR_SCOPE))
  }

  /** Returns the annotation model of the current document provider.
   */
  private def getAnnotationModelOpt: Option[IAnnotationModel] = {
    for {
      documentProvider <- Option(getDocumentProvider)
      annotationModel <- Option(documentProvider.getAnnotationModel(getEditorInput))
    } yield annotationModel
  }

  override def editorContextMenuAboutToShow(menu: org.eclipse.jface.action.IMenuManager): Unit = {
    super.editorContextMenuAboutToShow(menu)

    def groupMenuItemsByGroupId(items: Seq[IContributionItem]) = {
      // the different groups (as indicated by separators) and
      // contributions in a menu are originally just a flat list
      val groups = items.foldLeft(Nil: List[(String, List[IContributionItem])]) {

        // start a new group
        case (others, group: Separator) => (group.getId, Nil) :: others

        // append contribution to the current group
        case ((group, others) :: rest, element) => (group, element :: others) :: rest

        // the menu does not start with a group, this shouldn't happen, but if
        // it does we just skip this element, so it will stay in the menu.
        case (others, _) => others
      }
      groups.toMap
    }

    def findJdtSourceMenuManager(items: Seq[IContributionItem]) = {
      items.collect {
        case mm: MenuManager if mm.getId == "org.eclipse.jdt.ui.source.menu" => mm
      }
    }

    findJdtSourceMenuManager(menu.getItems) foreach { mm =>

      val groups = groupMenuItemsByGroupId(mm.getItems)

      // these contributions won't work on Jawa files, so we remove them
      val blacklist = List("codeGroup", "importGroup", "generateGroup", "externalizeGroup")
      blacklist.flatMap(groups.get).flatten.foreach(mm.remove)
    }
  }

  override def isMarkingOccurrences =
    argusPrefStore.getBoolean(EditorPreferencePage.P_ENABLE_MARK_OCCURRENCES)

  override def createSemanticHighlighter: TextPresentationHighlighter =
    TextPresentationEditorHighlighter(this, semanticHighlightingPreferences, addReconcilingListener _, removeReconcilingListener _)

  override def forceSemanticHighlightingOnInstallment: Boolean = false // relies on the Java reconciler to refresh the highlights

  def addReconcilingListener(listener: IJavaReconcilingListener): Unit =
    reconcilingListeners.addReconcileListener(listener)

  def removeReconcilingListener(listener: IJavaReconcilingListener): Unit =
    reconcilingListeners.removeReconcileListener(listener)

  override def aboutToBeReconciled(): Unit = {
    super.aboutToBeReconciled()
    reconcilingListeners.aboutToBeReconciled()
  }

  override def reconciled(ast: CompilationUnit, forced: Boolean, progressMonitor: IProgressMonitor): Unit = {
    super.reconciled(ast, forced, progressMonitor)
    reconcilingListeners.reconciled(ast, forced, progressMonitor)
  }
}

object JawaSourceFileEditor {
  private val EDITOR_BUNDLE_FOR_CONSTRUCTED_KEYS = "org.eclipse.ui.texteditor.ConstructedEditorMessages"
  private val bundleForConstructedKeys = ResourceBundle.getBundle(EDITOR_BUNDLE_FOR_CONSTRUCTED_KEYS)

  private val JAWA_EDITOR_SCOPE = "argus.tools.eclipse.jawaEditorScope"

  private val OCCURRENCE_ANNOTATION = "org.eclipse.jdt.ui.occurrences"

  /** A thread-safe object for keeping track of Java reconciling listeners.*/
  private class ReconcilingListeners extends IJavaReconcilingListener {
    private val reconcilingListeners = new ArrayBuffer[IJavaReconcilingListener] with SynchronizedBuffer[IJavaReconcilingListener]

    /** Return a snapshot of the currently registered `reconcilingListeners`. This is useful to avoid concurrency hazards when iterating on the `reconcilingListeners`. */
    private def currentReconcilingListeners: List[IJavaReconcilingListener] = reconcilingListeners.toList

    override def aboutToBeReconciled(): Unit =
      for (listener <- currentReconcilingListeners) listener.aboutToBeReconciled()

    override def reconciled(ast: CompilationUnit, forced: Boolean, progressMonitor: IProgressMonitor): Unit =
      for (listener <- currentReconcilingListeners) listener.reconciled(ast, forced, progressMonitor)

    def addReconcileListener(listener: IJavaReconcilingListener): Unit = reconcilingListeners += listener

    def removeReconcileListener(listener: IJavaReconcilingListener): Unit = reconcilingListeners -= listener
  }
}
