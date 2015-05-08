package org.arguside.core.internal.jdt.model

import argus.tools.eclipse.contribution.weaving.jdt.cuprovider.ICompilationUnitProvider
import org.eclipse.jdt.core.WorkingCopyOwner
import org.eclipse.jdt.internal.core.PackageFragment

class JawaCompilationUnitProvider extends ICompilationUnitProvider {
  def create(parent : PackageFragment, name : String, owner : WorkingCopyOwner) =
    new JawaSourceFile(parent, name, owner)
}
