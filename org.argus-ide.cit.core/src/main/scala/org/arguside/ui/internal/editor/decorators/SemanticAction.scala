package org.arguside.ui.internal.editor.decorators

import org.arguside.core.internal.jdt.model.JawaCompilationUnit

trait SemanticAction extends (JawaCompilationUnit => Unit) {
  def apply(scu: JawaCompilationUnit): Unit
}