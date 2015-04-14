package org.arguside.core.extensions

import org.eclipse.core.runtime.IPath
import org.arguside.core.compiler.InteractiveCompilationUnit

trait SourceFileProvider {
  /** Create a compilation unit for the passed workspace `path`.*/
  def createFrom(path: IPath): Option[InteractiveCompilationUnit]
}
