package org.arguside.ui.internal.editor.decorators.semantichighlighting

import org.arguside.ui.syntax.JawaSyntaxClasses
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.text.TextAttribute

class Preferences(val store: IPreferenceStore) {
  def isEnabled(): Boolean =
    store.getBoolean(JawaSyntaxClasses.ENABLE_SEMANTIC_HIGHLIGHTING)

  def isUseSyntacticHintsEnabled(): Boolean =
    store.getBoolean(JawaSyntaxClasses.USE_SYNTACTIC_HINTS)
}

object Preferences {
  def apply(store: IPreferenceStore): Preferences = new Preferences(store)
}
