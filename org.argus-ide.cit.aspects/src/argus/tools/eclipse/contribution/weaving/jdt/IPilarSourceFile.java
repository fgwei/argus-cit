/*
 * Copyright 2005-2010 LAMP/EPFL
 */
// $Id$

package argus.tools.eclipse.contribution.weaving.jdt;

import org.eclipse.jdt.core.compiler.IProblem;

public interface IPilarSourceFile extends IPilarCompilationUnit {
  public IProblem[] getProblems();
}
