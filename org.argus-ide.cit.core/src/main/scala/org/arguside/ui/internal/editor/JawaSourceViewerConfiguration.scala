package org.arguside.ui.internal.editor

import org.eclipse.core.runtime.NullProgressMonitor
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.ITypeRoot
import org.eclipse.jdt.internal.ui.JavaPlugin
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput
import org.eclipse.jdt.internal.ui.javaeditor.ICompilationUnitDocumentProvider
import org.eclipse.jdt.internal.ui.text.CompositeReconcilingStrategy
import org.eclipse.jdt.internal.ui.text.java.SmartSemicolonAutoEditStrategy
import org.eclipse.jdt.ui.text.IJavaPartitions
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration
import org.eclipse.jface.internal.text.html.BrowserInformationControl
import org.eclipse.jface.internal.text.html.HTMLPrinter
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.text.AbstractReusableInformationControlCreator
import org.eclipse.jface.text.DefaultInformationControl
import org.eclipse.jface.text.DefaultTextHover
import org.eclipse.jface.text.IAutoEditStrategy
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.IInformationControl
import org.eclipse.jface.text.IInformationControlCreator
import org.eclipse.jface.text.ITextHover
import org.eclipse.jface.text.formatter.IContentFormatter
import org.eclipse.jface.text.formatter.MultiPassContentFormatter
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector
import org.eclipse.jface.text.hyperlink.URLHyperlinkDetector
import org.eclipse.jface.text.information.InformationPresenter
import org.eclipse.jface.text.reconciler.IReconciler
import org.eclipse.jface.text.rules.DefaultDamagerRepairer
import org.eclipse.jface.text.source.Annotation
import org.eclipse.jface.text.source.DefaultAnnotationHover
import org.eclipse.jface.text.source.IAnnotationHoverExtension
import org.eclipse.jface.text.source.ILineRange
import org.eclipse.jface.text.source.ISourceViewer
import org.eclipse.jface.text.source.LineRange
import org.eclipse.jface.util.PropertyChangeEvent
import org.eclipse.swt.widgets.Shell
import org.eclipse.ui.editors.text.EditorsUI
import org.eclipse.ui.texteditor.ChainedPreferenceStore
import org.arguside.core.IArgusPlugin
import org.arguside.core.internal.hyperlink._
import org.arguside.core.internal.ArgusPlugin
import org.eclipse.jface.util.IPropertyChangeListener
import org.arguside.core.internal.jdt.model.JawaCompilationUnit
import org.arguside.ui.internal.reconciliation.JawaReconcilingStrategy
import org.arguside.ui.internal.editor.spelling.SpellingReconcileStrategy
import org.arguside.ui.internal.editor.spelling.SpellingService
import org.arguside.ui.internal.editor.spelling.JawaSpellingEngine
import org.arguside.ui.internal.reconciliation.JawaReconciler
import org.arguside.core.lexical.JawaCodeScanners
import org.arguside.core.lexical.JawaPartitions
import org.eclipse.jface.text.ITextDoubleClickStrategy

class JawaSourceViewerConfiguration(
  javaPreferenceStore: IPreferenceStore,
  argusPreferenceStore: IPreferenceStore,
  editor: JawaEditor)
    extends JavaSourceViewerConfiguration(
      JavaPlugin.getDefault.getJavaTextTools.getColorManager,
      javaPreferenceStore,
      editor,
      IJavaPartitions.JAVA_PARTITIONING) with IPropertyChangeListener {

  private val combinedPrefStore = new ChainedPreferenceStore(
      Array(argusPreferenceStore, javaPreferenceStore))

  private val codeHighlightingScanners = JawaCodeScanners.codeHighlightingScanners(argusPreferenceStore, javaPreferenceStore)

  /**
   * Creates a reconciler with a delay of 500ms.
   */
  override def getReconciler(sourceViewer: ISourceViewer): IReconciler =
    // the editor is null for the Syntax coloring previewer pane (so no reconciliation)
    Option(editor).map { editor =>
      val s = new CompositeReconcilingStrategy
      s.setReconcilingStrategies(Array(
          new JawaReconcilingStrategy(editor),
          new SpellingReconcileStrategy(
              editor,
              editor.getViewer(),
              new SpellingService(EditorsUI.getPreferenceStore(), new JawaSpellingEngine),
              ArgusPlugin().jawaSourceFileContentType,
              EditorsUI.getPreferenceStore())))

      val reconciler = new JawaReconciler(editor, s, isIncremental = false)
      reconciler.setDelay(500)
      reconciler.setProgressMonitor(new NullProgressMonitor())
      reconciler
    }.orNull

  override def getPresentationReconciler(sourceViewer: ISourceViewer): JawaPresentationReconciler = {
    val reconciler = new JawaPresentationReconciler()
    reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer))

    for ((partitionType, tokenScanner) <- codeHighlightingScanners) {
      val dr = new DefaultDamagerRepairer(tokenScanner)
      reconciler.setDamager(dr, partitionType)
      reconciler.setRepairer(dr, partitionType)
    }
    reconciler
  }
  
  override def getHyperlinkDetectors(sv: ISourceViewer): Array[IHyperlinkDetector] = {
    val detectors = List(DeclarationHyperlinkDetector(), new URLHyperlinkDetector())
    if (editor != null)
      detectors.foreach { d => d.setContext(editor) }

    detectors.toArray
  }

  private def getTypeRoot: Option[ITypeRoot] = Option(editor) map { editor =>
    val input = editor.getEditorInput
    val provider = editor.getDocumentProvider

    (provider, input) match {
      case (icudp: ICompilationUnitDocumentProvider, _) => icudp getWorkingCopy input
      case (_, icfei: IClassFileEditorInput)            => icfei.getClassFile
      case _                                            => null
    }
  }

  private def compilationUnit: Option[JawaCompilationUnit] =
    getTypeRoot collect { case scu: JawaCompilationUnit => scu }

  private def getProject: IJavaProject =
    getTypeRoot.map(_.asInstanceOf[IJavaElement].getJavaProject).orNull

  override def handlePropertyChangeEvent(event: PropertyChangeEvent) {
    super.handlePropertyChangeEvent(event)
    codeHighlightingScanners.values foreach (_ adaptToPreferenceChange event)
  }

  override def propertyChange(event: PropertyChangeEvent): Unit = {
    handlePropertyChangeEvent(event)
  }
  
  override def getDoubleClickStrategy(sourceViewer: ISourceViewer, contentType: String): ITextDoubleClickStrategy = {
    new JawaDoubleClickStrategy
  }

  /**
   * Adds Argus related partition types to the list of configured content types,
   * in order that they are available for several features of the IDE.
   */
  override def getConfiguredContentTypes(sourceViewer: ISourceViewer): Array[String] =
    super.getConfiguredContentTypes(sourceViewer) ++
      Seq(JawaPartitions.JAWA_MULTI_LINE_STRING)

  override def affectsTextPresentation(event: PropertyChangeEvent) = true

}
