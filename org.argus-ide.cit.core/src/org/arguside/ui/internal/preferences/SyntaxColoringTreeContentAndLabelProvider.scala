package org.arguside.ui.internal.preferences

import org.arguside.ui.syntax.ArgusSyntaxClass.Category
import org.eclipse.jface.viewers._
import org.arguside.ui.syntax.ArgusSyntaxClass

/** Content and label provider for the tree of syntax element in the syntax coloring
 *  preference pages.
 */
class SyntaxColoringTreeContentAndLabelProvider(categories: List[Category]) extends LabelProvider with ITreeContentProvider {

  def getElements(inputElement: AnyRef) = categories.toArray

  def getChildren(parentElement: AnyRef) = parentElement match {
    case Category(_, children) => children.toArray
    case _ => Array()
  }

  def getParent(element: AnyRef): Category = categories.find(_.children contains element).orNull

  def hasChildren(element: AnyRef) = getChildren(element).nonEmpty

  def inputChanged(viewer: Viewer, oldInput: AnyRef, newInput: AnyRef) {}

  override def getText(element: AnyRef) = element match {
    case Category(name, _) => name
    case ArgusSyntaxClass(displayName, _, _, _) => displayName
  }
}
