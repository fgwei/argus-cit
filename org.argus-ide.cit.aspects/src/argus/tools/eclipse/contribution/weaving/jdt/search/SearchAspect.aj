package argus.tools.eclipse.contribution.weaving.jdt.search;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.core.search.matching.MatchLocator;
import org.eclipse.jdt.internal.core.search.matching.PossibleMatch;

import argus.tools.eclipse.contribution.weaving.jdt.IArgusClassFile;
import argus.tools.eclipse.contribution.weaving.jdt.IArgusCompilationUnit;

@SuppressWarnings("restriction")
public privileged aspect SearchAspect {
  pointcut parseAndBuildBindings(MatchLocator ml, PossibleMatch possibleMatch, boolean mustResolve) :
    execution(boolean MatchLocator.parseAndBuildBindings(PossibleMatch, boolean)) &&
    target(ml) &&
    args(possibleMatch, mustResolve);
  
  pointcut process(MatchLocator ml, PossibleMatch possibleMatch, boolean bindingsWereCreated) :
    execution(void MatchLocator.process(PossibleMatch, boolean)) &&
    target(ml) &&
    args(possibleMatch, bindingsWereCreated);
  
  pointcut getSourceFileName(PossibleMatch pm) :
    execution(String getSourceFileName()) &&
    target(pm);
    
  boolean around(MatchLocator ml, PossibleMatch possibleMatch, boolean mustResolve) throws CoreException :
    parseAndBuildBindings(ml, possibleMatch, mustResolve) {
    if (!(possibleMatch.openable instanceof IArgusCompilationUnit))
      return proceed(ml, possibleMatch, mustResolve);

    possibleMatch.parsedUnit = null;
    int size = ml.matchesToProcess.length;
    if (ml.numberOfMatches == size)
      System.arraycopy(ml.matchesToProcess, 0, ml.matchesToProcess = new PossibleMatch[size == 0 ? 1 : size * 2], 0, ml.numberOfMatches);
    ml.matchesToProcess[ml.numberOfMatches++] = possibleMatch;
    return true;
  }
  
  void around(MatchLocator ml, PossibleMatch possibleMatch, boolean bindingsWereCreated) throws CoreException :
    process(ml, possibleMatch, bindingsWereCreated) {
    if (possibleMatch.openable instanceof IArgusCompilationUnit)
      ((IArgusCompilationUnit)possibleMatch.openable).reportMatches(ml, possibleMatch);
    else
      proceed(ml, possibleMatch, bindingsWereCreated);
  }

  String around(PossibleMatch pm) :
    getSourceFileName(pm) {
    if (pm.sourceFileName != null || !(pm.openable instanceof IArgusClassFile))
      return proceed(pm);

    pm.sourceFileName = PossibleMatch.NO_SOURCE_FILE_NAME;
    String fileName = ((IArgusClassFile)pm.openable).getSourceFileName();
    if (fileName != null)
      pm.sourceFileName = fileName;
    return pm.sourceFileName;
  }
}
