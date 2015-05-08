package org.arguside.ui.internal.editor.decorators

import scala.reflect.internal.util.SourceFile
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.preference.PreferenceConverter
import org.eclipse.jface.text.IPainter
import org.eclipse.jface.text.{ Position => JFacePosition }
import org.eclipse.jface.text.TextViewer
import org.eclipse.jface.text.source.Annotation
import org.eclipse.jface.text.source.AnnotationPainter
import org.eclipse.jface.text.source.IAnnotationAccess
import org.eclipse.jface.text.source.ISourceViewer
import org.eclipse.jface.util.IPropertyChangeListener
import org.eclipse.jface.util.PropertyChangeEvent
import org.eclipse.swt.SWT
import org.eclipse.ui.editors.text.EditorsUI
import org.arguside.core.IArgusPlugin
import org.arguside.core.internal.jdt.model.JawaCompilationUnit
import org.arguside.logging.HasLogger
import org.arguside.util.internal.eclipse.AnnotationUtils
import org.arguside.util.eclipse.EclipseUtils

/**
 * Represents basic properties - enabled, bold an italic.
 *
 * Format of properties:
 *
 * argus.tools.eclipse.ui.preferences.$preferencePageId.(enabled|text.italic|text.bold)
 */
private[decorators] class Properties(preferencePageId: String) {
  private val base = s"argus.tools.eclipse.ui.preferences.$preferencePageId."

  val active = base + "enabled"

  val bold = base + "text.bold"

  val italic = base + "text.italic"
}

/**
 * Base for creating custom semantic actions.
 *
 * If you provide a `preferencePageId`, you have to create a properties page for it
 * with properties matching those from [[org.arguside.ui.internal.editor.decorators.Properties]] class.
 *
 * @param sourceViewer
 * @param annotationId id of annotation (must match id from plugin.xml)
 * @param preferencePageId id of preference page, optional
 */
abstract class BaseSemanticAction(
  sourceViewer: ISourceViewer,
  annotationId: String,
  preferencePageId: Option[String])
  extends SemanticAction
  with HasLogger {

  private val propertiesOpt = preferencePageId.map(id => new Properties(id))

  protected val annotationAccess = new IAnnotationAccess {
    override def getType(annotation: Annotation) = annotation.getType
    override def isMultiLine(annotation: Annotation) = true
    override def isTemporary(annotation: Annotation) = true
  }

  protected def pluginStore: IPreferenceStore = IArgusPlugin().getPreferenceStore

  protected def isFontStyleBold = propertiesOpt match {
    case Some(properties) if pluginStore.getBoolean(properties.bold) => SWT.BOLD
    case _ => SWT.NORMAL
  }

  protected def isFontStyleItalic = propertiesOpt match {
    case Some(properties) if pluginStore.getBoolean(properties.italic) => SWT.ITALIC
    case _ => SWT.NORMAL
  }

  protected lazy val P_COLOR = {
    val lookup = new org.eclipse.ui.texteditor.AnnotationPreferenceLookup()
    val pref = lookup.getAnnotationPreference(annotationId)
    pref.getColorPreferenceKey()
  }

  protected def colorValue = {
    val rgb = PreferenceConverter.getColor(EditorsUI.getPreferenceStore, P_COLOR)
    ColorManager.colorManager.getColor(rgb)
  }

  protected val textStyleStrategy = new HighlightingTextStyleStrategy(isFontStyleBold | isFontStyleItalic)

  protected val painter: AnnotationPainter = {
    val p = new AnnotationPainter(sourceViewer, annotationAccess)
    p.addAnnotationType(annotationId, annotationId)
    p.addTextStyleStrategy(annotationId, textStyleStrategy)
    //FIXME settings color of the underline is required to active TextStyle (bug ??, better way ??)
    p.setAnnotationTypeColor(annotationId, colorValue)
    val textViewer = sourceViewer.asInstanceOf[TextViewer]
    textViewer.addPainter(p)
    textViewer.addTextPresentationListener(p)
    p
  }

  protected def findAll(scu: JawaCompilationUnit, sourceFile: SourceFile): Map[Annotation, JFacePosition]

  private val _listener = new IPropertyChangeListener {
    override def propertyChange(event: PropertyChangeEvent) {
      propertiesOpt.foreach { properties =>
        val changed = event.getProperty() match {
          case properties.bold | properties.italic | P_COLOR => true
          case properties.active => {
            refresh()
            false
          }
          case _ => false
        }
        if (changed) {
          textStyleStrategy.fontStyle = isFontStyleBold | isFontStyleItalic
          painter.setAnnotationTypeColor(annotationId, colorValue)
          painter.paint(IPainter.CONFIGURATION)
        }
      }
    }
  }

  private def refresh() = {
    for {
      page <- EclipseUtils.getWorkbenchPages
      editorReference <- page.getEditorReferences
      editorInput <- Option(editorReference.getEditorInput)
      scu <- IArgusPlugin().jawaCompilationUnit(editorInput)
    } apply(scu)
  }

  PropertyChangeListenerProxy(_listener, pluginStore, EditorsUI.getPreferenceStore).autoRegister()

}
