package org.arguside.ui.internal.editor.decorators.semantichighlighting

import org.arguside.ui.syntax.ArgusSyntaxClasses
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.text.TextAttribute

class Preferences(val store: IPreferenceStore) {
  def isEnabled(): Boolean =
    store.getBoolean(ArgusSyntaxClasses.ENABLE_SEMANTIC_HIGHLIGHTING)

  def isStrikethroughDeprecatedDecorationEnabled(): Boolean =
    store.getBoolean(ArgusSyntaxClasses.STRIKETHROUGH_DEPRECATED)

  def isUseSyntacticHintsEnabled(): Boolean =
    store.getBoolean(ArgusSyntaxClasses.USE_SYNTACTIC_HINTS)

  def isInterpolatedStringCodeDecorationEnabled(): Boolean =
    ArgusSyntaxClasses.IDENTIFIER_IN_INTERPOLATED_STRING.enabled(store)

  def interpolatedStringTextAttribute(): TextAttribute =
    ArgusSyntaxClasses.IDENTIFIER_IN_INTERPOLATED_STRING.getTextAttribute(store)
}

object Preferences {
  def apply(store: IPreferenceStore): Preferences = new Preferences(store)
}
