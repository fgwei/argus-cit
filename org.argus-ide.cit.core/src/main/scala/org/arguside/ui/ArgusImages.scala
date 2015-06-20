package org.arguside.ui

import org.eclipse.jdt.internal.ui.JavaPluginImages
import org.eclipse.jface.resource.ImageDescriptor
import org.eclipse.swt.graphics.Image
import org.arguside.util.eclipse.OSGiUtils

object ArgusImages {
  val MISSING_ICON: ImageDescriptor = ImageDescriptor.getMissingImageDescriptor

  val JAWA_FILE: ImageDescriptor = OSGiUtils.getImageDescriptorFromCoreBundle("/icons/full/obj16/scu_obj.gif")
  val JAWA_CLASS_FILE: ImageDescriptor = OSGiUtils.getImageDescriptorFromCoreBundle("/icons/full/obj16/sclassf_obj.gif")
  val EXCLUDED_JAWA_FILE: ImageDescriptor = OSGiUtils.getImageDescriptorFromCoreBundle("/icons/full/obj16/scu_resource_obj.gif")

  val JAWA_CLASS: ImageDescriptor = OSGiUtils.getImageDescriptorFromCoreBundle("/icons/full/obj16/class_obj.gif")
  val JAWA_INTERFACE: ImageDescriptor = OSGiUtils.getImageDescriptorFromCoreBundle("/icons/full/obj16/trait_obj.gif")
  
  val PUBLIC_METHOD: ImageDescriptor = OSGiUtils.getImageDescriptorFromCoreBundle("/icons/full/obj16/defpub_obj.gif")
  val PRIVATE_METHOD: ImageDescriptor = OSGiUtils.getImageDescriptorFromCoreBundle("/icons/full/obj16/defpri_obj.gif")
  val PROTECTED_METHOD: ImageDescriptor = OSGiUtils.getImageDescriptorFromCoreBundle("/icons/full/obj16/defpro_obj.gif")

  val PUBLIC_FIELD: ImageDescriptor = OSGiUtils.getImageDescriptorFromCoreBundle("/icons/full/obj16/valpub_obj.gif")
  val PROTECTED_FIELD: ImageDescriptor = OSGiUtils.getImageDescriptorFromCoreBundle("/icons/full/obj16/valpro_obj.gif")
  val PRIVATE_FIELD: ImageDescriptor = OSGiUtils.getImageDescriptorFromCoreBundle("/icons/full/obj16/valpri_obj.gif")

  val ARGUS_PROJECT_WIZARD: ImageDescriptor = OSGiUtils.getImageDescriptorFromCoreBundle("/icons/full/wizban/newsprj_wiz.png")

  val REFRESH_REPL_TOOLBAR: ImageDescriptor = OSGiUtils.getImageDescriptorFromCoreBundle("/icons/full/etool16/refresh_interpreter.gif")

  val NEW_CLASS: ImageDescriptor = OSGiUtils.getImageDescriptorFromCoreBundle("/icons/full/etool16/newclass_wiz.gif")
  val CORRECTION_RENAME: ImageDescriptor = OSGiUtils.getImageDescriptorFromCoreBundle("/icons/full/obj16/correction_rename.gif")

  val ADD_METHOD_PROPOSAL: Image = JavaPluginImages.DESC_MISC_PUBLIC.createImage()
}
