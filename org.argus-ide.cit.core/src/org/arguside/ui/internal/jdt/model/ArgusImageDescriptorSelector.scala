package org.arguside.ui.internal.jdt.model

import argus.tools.eclipse.contribution.weaving.jdt.imagedescriptor.IImageDescriptorSelector
import org.eclipse.jdt.core.JavaModelException
import org.eclipse.jdt.internal.ui.text.java.LazyJavaCompletionProposal
import org.eclipse.jface.resource.ImageDescriptor
import org.arguside.core.internal.jdt.model.ArgusElement

class ArgusImageDescriptorSelector extends IImageDescriptorSelector {

  def getTypeImageDescriptor(isInner : Boolean, isInInterfaceOrAnnotation : Boolean, flags : Int, useLightIcons : Boolean, element : AnyRef) : ImageDescriptor =
  try {
    element match {
      case se : ArgusElement => se.getImageDescriptor
      case _ => null
    }
  } catch {
    case _ : JavaModelException => null
  }

  def createCompletionProposalImageDescriptor(proposal : LazyJavaCompletionProposal) : ImageDescriptor = {
    proposal.getJavaElement match {
      case se : ArgusElement => se.getImageDescriptor
      case _ => null
    }
  }
}
