package org.arguside.ui.internal.preferences

import org.eclipse.jdt.ui.text.IJavaPartitions
import org.eclipse.jface.text.IDocumentPartitioner
import org.arguside.ui.syntax.preferences.PreviewerFactoryConfiguration
import org.eclipse.jface.text.source.ISourceViewer
import org.eclipse.jface.preference.IPreferenceStore
import org.arguside.ui.internal.editor.decorators.semantichighlighting.HighlightingStyle
import org.arguside.ui.internal.editor.decorators.semantichighlighting.Preferences
import org.arguside.core.lexical.JawaCodePartitioner
import org.arguside.ui.syntax.JawaSyntaxClasses
import org.arguside.ui.internal.editor.JawaSourceViewerConfiguration

class StandardPreviewerFactoryConfiguration extends PreviewerFactoryConfiguration {

  def getConfiguration(preferenceStore: org.eclipse.jface.preference.IPreferenceStore): PreviewerFactoryConfiguration.PreviewerConfiguration = {
    new JawaSourceViewerConfiguration(preferenceStore, preferenceStore, null)
  }

  def getDocumentPartitioners(): Map[String, IDocumentPartitioner] =
    Map((IJavaPartitions.JAVA_PARTITIONING, JawaCodePartitioner.documentPartitioner(conservative = true)))
}

object ArgusPreviewerFactoryConfiguration extends StandardPreviewerFactoryConfiguration

object SemanticPreviewerFactoryConfiguration extends StandardPreviewerFactoryConfiguration {
  override def additionalStyling(viewer: ISourceViewer, store: IPreferenceStore) {
    val textWidgetOpt = Option(viewer.getTextWidget)
    for {
      textWidget <- textWidgetOpt
      position <- SyntaxColoringPreferencePage.semanticLocations
    } if (store.getBoolean(JawaSyntaxClasses.ENABLE_SEMANTIC_HIGHLIGHTING)
      && (store.getBoolean(HighlightingStyle.symbolTypeToSyntaxClass(position.kind).enabledKey)
        || position.shouldStyle)) {
      val styleRange = HighlightingStyle(Preferences(store), position.kind).style(position)
      textWidget.setStyleRange(styleRange)
    }
  }
}
