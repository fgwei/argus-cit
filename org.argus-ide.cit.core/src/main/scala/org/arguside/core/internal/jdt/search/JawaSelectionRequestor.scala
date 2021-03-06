package org.arguside.core.internal.jdt.search

import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.internal.core.NameLookup
import org.eclipse.jdt.internal.core.Openable
import org.eclipse.jdt.internal.core.SelectionRequestor

class JawaSelectionRequestor(nameLookup : NameLookup, openable : Openable) extends SelectionRequestor(nameLookup, openable) {
  override def addElement(elem : IJavaElement) =
    if (elem != null) super.addElement(elem)

  override def findLocalElement(pos : Int) =
    super.findLocalElement(pos)

  def hasSelection() = elementIndex >= 0
}
