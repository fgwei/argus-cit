package argus.tools.eclipse.contribution.weaving.jdt.ui.javaeditor;

import org.eclipse.jdt.internal.ui.text.correction.QuickAssistProcessor;
import org.eclipse.jdt.internal.ui.text.correction.AdvancedQuickAssistProcessor;
import org.eclipse.jdt.internal.ui.text.correction.QuickFixProcessor;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import argus.tools.eclipse.contribution.weaving.jdt.ArgusJDTWeavingPlugin;
import argus.tools.eclipse.contribution.weaving.jdt.IJawaCompilationUnit;

@SuppressWarnings("restriction")
public privileged aspect JawaEditorPreferencesAspect {

  pointcut getAssists(IInvocationContext context, IProblemLocation[] locations):
    execution(IJavaCompletionProposal[] QuickAssistProcessor.getAssists(IInvocationContext, IProblemLocation[])) && args(context, locations) ||
    execution(IJavaCompletionProposal[] AdvancedQuickAssistProcessor.getAssists(IInvocationContext, IProblemLocation[])) && args(context, locations) ||
    execution(IJavaCompletionProposal[] QuickFixProcessor.getCorrections(IInvocationContext, IProblemLocation[])) && args(context, locations);

  /**
   * Disable Java quick fixes/assists on Jawa sources. They can be very slow, and totally useless.
   */
  IJavaCompletionProposal[] around(IInvocationContext context, IProblemLocation[] locations):
    getAssists(context, locations) {
      if (context.getCompilationUnit() instanceof IJawaCompilationUnit)
        return null;
      else
        return proceed(context, locations);
  }
}
