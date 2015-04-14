package argus.tools.eclipse.contribution.weaving.jdt.core;

import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.SourceElementParser;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.CompilationUnitProblemFinder;

import argus.tools.eclipse.contribution.weaving.jdt.IArgusElement;
import argus.tools.eclipse.contribution.weaving.jdt.IArgusSourceFile;;

@SuppressWarnings("restriction")
public privileged aspect CompilationUnitProblemFinderAspect {
  pointcut process(
    CompilationUnit unitElement,
    SourceElementParser parser,
    WorkingCopyOwner workingCopyOwner,
    HashMap problems,
    boolean creatingAST,
    int reconcileFlags,
    IProgressMonitor monitor) : 
    args(unitElement, parser, workingCopyOwner, problems, creatingAST, reconcileFlags, monitor) &&
    execution(public static CompilationUnitDeclaration CompilationUnitProblemFinder.process(
      CompilationUnit, SourceElementParser, WorkingCopyOwner, HashMap, boolean, int, IProgressMonitor));

  CompilationUnitDeclaration around(
    CompilationUnit unitElement,
    SourceElementParser parser,
    WorkingCopyOwner workingCopyOwner,
    HashMap problems,
    boolean creatingAST,
    int reconcileFlags,
    IProgressMonitor monitor) :
    process(unitElement, parser, workingCopyOwner, problems, creatingAST, reconcileFlags, monitor) {
    CompilationUnit original = unitElement.originalFromClone(); 
    if (!(original instanceof IArgusElement))
      return proceed(unitElement, parser, workingCopyOwner, problems, creatingAST, reconcileFlags, monitor);
    
    if (original instanceof IArgusSourceFile) {
      IProblem[] unitProblems = ((IArgusSourceFile)original).getProblems();
      int length = unitProblems == null ? 0 : unitProblems.length;
      if (length > 0) {
        CategorizedProblem[] categorizedProblems = new CategorizedProblem[length];
        System.arraycopy(unitProblems, 0, categorizedProblems, 0, length);
        problems.put(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, categorizedProblems);
      }
    }

    return null;
  }
}
