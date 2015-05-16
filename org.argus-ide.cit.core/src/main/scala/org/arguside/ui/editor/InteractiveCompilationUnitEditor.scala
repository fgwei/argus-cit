package org.arguside.ui.editor

import org.arguside.core.compiler.InteractiveCompilationUnit

trait InteractiveCompilationUnitEditor extends DecoratedInteractiveEditor {
  /** Returns `null` if the editor is closed. */
  def getInteractiveCompilationUnit(): InteractiveCompilationUnit
}
