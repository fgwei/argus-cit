package org.arguside.ui.internal.preferences

import org.eclipse.jdt.ui.text.IJavaPartitions
import org.eclipse.jface.text.IDocumentPartitioner
import org.arguside.ui.internal.editor.ArgusSourceViewerConfiguration
import org.arguside.ui.syntax.preferences.PreviewerFactoryConfiguration
import org.arguside.core.lexical.ArgusCodePartitioner
import org.eclipse.jface.text.source.ISourceViewer
import org.arguside.ui.syntax.ArgusSyntaxClasses
import org.eclipse.jface.preference.IPreferenceStore
import org.arguside.ui.internal.editor.decorators.semantichighlighting.HighlightingStyle
import org.arguside.ui.internal.editor.decorators.semantichighlighting.Preferences

class StandardPreviewerFactoryConfiguration extends PreviewerFactoryConfiguration {

  def getConfiguration(preferenceStore: org.eclipse.jface.preference.IPreferenceStore): PreviewerFactoryConfiguration.PreviewerConfiguration = {
    new ArgusSourceViewerConfiguration(preferenceStore, preferenceStore, null)
  }

  def getDocumentPartitioners(): Map[String, IDocumentPartitioner] =
    Map((IJavaPartitions.JAVA_PARTITIONING, ArgusCodePartitioner.documentPartitioner(conservative = true)))
}

object ArgusPreviewerFactoryConfiguration extends StandardPreviewerFactoryConfiguration

object SemanticPreviewerFactoryConfiguration extends StandardPreviewerFactoryConfiguration {
  override def additionalStyling(viewer: ISourceViewer, store: IPreferenceStore) {
    val textWidgetOpt = Option(viewer.getTextWidget)
    for {
      textWidget <- textWidgetOpt
      position <- SyntaxColoringPreferencePage.semanticLocations
    } if (store.getBoolean(ArgusSyntaxClasses.ENABLE_SEMANTIC_HIGHLIGHTING)
      && (store.getBoolean(HighlightingStyle.symbolTypeToSyntaxClass(position.kind).enabledKey)
        || position.shouldStyle)) {
      val styleRange = HighlightingStyle(Preferences(store), position.kind).style(position)
      textWidget.setStyleRange(styleRange)
    }
  }
}
