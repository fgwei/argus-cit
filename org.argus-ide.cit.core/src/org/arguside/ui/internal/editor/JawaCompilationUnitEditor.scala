package org.arguside.ui.internal.editor

import org.eclipse.jdt.internal.ui.javaeditor.IJavaEditorActionConstants
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer
import org.eclipse.jface.text.source.SourceViewerConfiguration
import org.eclipse.jface.util.IPropertyChangeListener
import org.eclipse.jface.util.PropertyChangeEvent
import org.eclipse.swt.widgets.Composite
import org.arguside.core.IArgusPlugin
import org.arguside.core.compiler.InteractiveCompilationUnit
import org.arguside.ui.internal.editor.decorators.semantichighlighting
import org.arguside.util.eclipse.SWTUtils.fnToPropertyChangeListener
import org.arguside.util.ui.DisplayThread
import org.arguside.ui.syntax.JawaSyntaxClasses

/** Trait containing common logic used by both the `ArgusSourceFileEditor` and `ArgusClassFileEditor`.*/
trait JawaCompilationUnitEditor extends JavaEditor with JawaEditor {
  /**@note Current implementation assumes that all accesses to this member should be confined to the UI Thread */
  private var semanticHighlightingPresenter: semantichighlighting.Presenter = _
  protected def semanticHighlightingPreferences = semantichighlighting.Preferences(argusPrefStore)

  private val preferenceListener: IPropertyChangeListener = handlePreferenceStoreChanged _

  argusPrefStore.addPropertyChangeListener(preferenceListener)

  protected def argusPrefStore = IArgusPlugin().getPreferenceStore()
  def javaPrefStore = super.getPreferenceStore

  override def setSourceViewerConfiguration(configuration: SourceViewerConfiguration) {
    super.setSourceViewerConfiguration(
      configuration match {
        case svc: JawaSourceViewerConfiguration => svc
        case _ => new JawaSourceViewerConfiguration(javaPrefStore, argusPrefStore, this)
      })
  }

  override def createPartControl(parent: Composite) {
    super.createPartControl(parent)

    val sv = sourceViewer
    if (isArgusSemanticHighlightingEnabled)
      installArgusSemanticHighlighting(forceSemanticHighlightingOnInstallment)
  }

  /** Should semantic highlighting be triggered at initialization. */
  def forceSemanticHighlightingOnInstallment: Boolean

  protected def installArgusSemanticHighlighting(forceRefresh: Boolean): Unit = {
    if (semanticHighlightingPresenter == null) {
      val presentationHighlighter = createSemanticHighlighter
      semanticHighlightingPresenter = new semantichighlighting.Presenter(JawaCompilationUnitEditor.this, presentationHighlighter, semanticHighlightingPreferences, DisplayThread)
      semanticHighlightingPresenter.initialize(forceRefresh)
    }
  }

  def createSemanticHighlighter: semantichighlighting.TextPresentationHighlighter

  protected def uninstallArgusSemanticHighlighting(removesHighlights: Boolean): Unit = {
    if (semanticHighlightingPresenter != null) {
      semanticHighlightingPresenter.dispose(removesHighlights)
      semanticHighlightingPresenter = null
    }
  }

  def sourceViewer: JavaSourceViewer = super.getSourceViewer.asInstanceOf[JavaSourceViewer]

  override protected final def installSemanticHighlighting(): Unit = { /* Never install the Java semantic highlighting engine on a Argus Editor*/ }

  private def isArgusSemanticHighlightingEnabled: Boolean = semanticHighlightingPreferences.isEnabled

  override def dispose() {
    super.dispose()
    argusPrefStore.removePropertyChangeListener(preferenceListener)
    uninstallArgusSemanticHighlighting(removesHighlights = false)
  }

  override protected def handlePreferenceStoreChanged(event: PropertyChangeEvent) = {
    event.getProperty match {
      case JawaSyntaxClasses.ENABLE_SEMANTIC_HIGHLIGHTING =>
        // This preference can be changed only via the preference dialog, hence the below block
        // is ensured to be always run within the UI Thread. Check the JavaDoc of `handlePreferenceStoreChanged`
        if (isArgusSemanticHighlightingEnabled) installArgusSemanticHighlighting(forceRefresh = true)
        else uninstallArgusSemanticHighlighting(removesHighlights = true)

      case _ =>
        super.handlePreferenceStoreChanged(event)
    }
  }

  override final def createJavaSourceViewerConfiguration: JawaSourceViewerConfiguration =
    new JawaSourceViewerConfiguration(javaPrefStore, argusPrefStore, this)

  override final def getInteractiveCompilationUnit(): InteractiveCompilationUnit = {
    IArgusPlugin().jawaCompilationUnit(getEditorInput()).orNull
  }
}
