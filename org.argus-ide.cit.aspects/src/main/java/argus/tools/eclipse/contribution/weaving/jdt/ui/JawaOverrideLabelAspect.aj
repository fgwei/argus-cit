package argus.tools.eclipse.contribution.weaving.jdt.ui;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.OverrideIndicatorLabelDecorator;

@SuppressWarnings("restriction")
public privileged aspect JawaOverrideLabelAspect {
  pointcut getOverrideIndicators(IMethod method) :
    args (method) &&
	  (execution(int OverrideIndicatorLabelDecorator.getOverrideIndicators(IMethod)));

  int around(IMethod method) throws JavaModelException : getOverrideIndicators(method) {
    if (method instanceof IMethodOverrideInfo) {
      return ((IMethodOverrideInfo)method).getOverrideInfo();
    } 
    else
      return proceed(method);
  }
}
