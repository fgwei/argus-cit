package argus.tools.eclipse.contribution.weaving.jdt.ui.javaeditor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabelComposer;
import org.eclipse.jdt.ui.JavaElementLabels;

import argus.tools.eclipse.contribution.weaving.jdt.ArgusJDTWeavingPlugin;
import argus.tools.eclipse.contribution.weaving.jdt.IJawaElement;

/** Tweaks the labels in the outline, to display jawa native syntax
 * 
 */
@SuppressWarnings("restriction")
public privileged aspect OutlineAspect {
  pointcut appendElementLabel(JavaElementLabelComposer jelc, IJavaElement element, long flags) :
    execution(void JavaElementLabelComposer.appendElementLabel(IJavaElement, long)) &&
    args(element, flags) &&
    target(jelc);
  
  void around(JavaElementLabelComposer jelc, IJavaElement element, long flags) :
    appendElementLabel(jelc, element, flags) {
    if (!(element instanceof IJawaElement))
      proceed(jelc, element, flags);
    else {
      IPackageFragmentRoot root = JavaModelUtil.getPackageFragmentRoot(element);
      if (JavaElementLabelComposer.getFlag(flags, JavaElementLabels.PREPEND_ROOT_PATH)) {
        jelc.appendPackageFragmentRootLabel(root, JavaElementLabels.ROOT_QUALIFIED);
        jelc.fBuffer.append(JavaElementLabels.CONCAT_STRING);
      }
      
      IJawaElement jawaElement = (IJawaElement)element;
      jelc.fBuffer.append(jawaElement.getLabelText(flags));
      if (JavaElementLabelComposer.getFlag(flags, JavaElementLabels.APPEND_ROOT_PATH)) {
        int offset= jelc.fBuffer.length();
        jelc.fBuffer.append(JavaElementLabels.CONCAT_STRING);
        jelc.appendPackageFragmentRootLabel(root, JavaElementLabels.ROOT_QUALIFIED);

        if (JavaElementLabelComposer.getFlag(flags, JavaElementLabels.COLORIZE)) {
          jelc.fBuffer.setStyle(offset, jelc.fBuffer.length() - offset, JavaElementLabelComposer.QUALIFIER_STYLE);
        }
      } else {
    	  	ArgusJDTWeavingPlugin.logErrorMessage(" " + element.getElementType());
      }
    }
  }
}