package org.arguside.ui.internal.explorer

import org.arguside.ui.ArgusImages
import org.eclipse.core.internal.resources.File
import org.eclipse.jface.viewers.DecorationContext
import org.eclipse.jface.viewers.IDecoration
import org.eclipse.jface.viewers.ILabelProviderListener
import org.eclipse.jface.viewers.ILightweightLabelDecorator

class ArgusDecorator extends ILightweightLabelDecorator {

  def decorate(elem: Any, decoration: IDecoration): Unit = elem match {
    case file: File if file.getName().endsWith(".pilar") || file.getName().endsWith(".plr") =>
      decoration.getDecorationContext() match {
        case dc: DecorationContext =>
          dc.putProperty(IDecoration.ENABLE_REPLACE, true)
          decoration.addOverlay(ArgusImages.EXCLUDED_JAWA_FILE, IDecoration.REPLACE)
        case _ =>
      }
    case _ =>
  }

  def dispose(): Unit = {}

  def isLabelProperty(elem: Any, property: String): Boolean = false

  def addListener(listener: ILabelProviderListener): Unit = {}

  def removeListener(listener: ILabelProviderListener): Unit = {}

}
