package org.arguside.core.internal.extensions

import org.arguside.core.extensions.SemanticHighlightingParticipant
import org.arguside.util.eclipse.EclipseUtils

/**
 * Gives access to the semanticHighlightingParticipants extension point. The
 * extensions of this extension point are used by the `ScalaSourceFileEditor`
 * to participate in the semantic highlighting process.
 */
object SemanticHighlightingParticipants {

  /**
   * The ID of the extension point.
   */
  final val ExtensionPointId = "org.argus-ide.cit.core.semanticHighlightingParticipants"

  /**
   * Returns all existing implementations of
   * [[org.scalaide.core.extensions.SemanticHighlightingParticipant]], which
   * could successfully be instantiated.
   */
  def extensions: Seq[SemanticHighlightingParticipant] = {
    val elems = EclipseUtils.configElementsForExtension(ExtensionPointId)
    elems flatMap { e =>
      EclipseUtils.withSafeRunner("Error occurred while trying to create semantic highlighting participant.") {
        e.createExecutableExtension("class").asInstanceOf[SemanticHighlightingParticipant]
      }
    }
  }
}