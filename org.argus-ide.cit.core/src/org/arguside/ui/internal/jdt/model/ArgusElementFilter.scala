package org.arguside.ui.internal.jdt.model

import org.eclipse.jface.viewers.Viewer
import org.eclipse.jface.viewers.ViewerFilter
import org.arguside.core.internal.jdt.model.ArgusElement

class ArgusElementFilter extends ViewerFilter {
  def select(viewer : Viewer, parentElement : AnyRef, element : AnyRef) : Boolean =
    !element.isInstanceOf[ArgusElement] || element.asInstanceOf[ArgusElement].isVisible
}
