package org.arguside.ui.internal.editor.decorators

import org.arguside.core.internal.jdt.model.ArgusCompilationUnit

trait SemanticAction extends (ArgusCompilationUnit => Unit) {
  def apply(scu: ArgusCompilationUnit): Unit
}
