package argus.tools.eclipse.contribution.weaving.jdt.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;

public aspect SourceLookupAspect {
  pointcut findSourceElements(Object object) :
    execution(Object[] AbstractSourceLookupDirector.findSourceElements(Object)) &&
    args(object);
  
  Object[] around(Object object) throws CoreException :
    findSourceElements(object) {
    if (object instanceof String) {
      String sourceFile = (String)object;
      if (sourceFile.endsWith(".java")) {
        String pilarSourceFile1 = sourceFile.substring(0, sourceFile.length()-5)+".pilar";
        String pilarSourceFile2 = sourceFile.substring(0, sourceFile.length()-5)+".plr";
        List result = new ArrayList();
        Object[] pilarResults1 = proceed(pilarSourceFile1);
        Object[] pilarResults2 = proceed(pilarSourceFile2);
        Object[] javaResults = proceed(sourceFile);
        result.addAll(Arrays.asList(pilarResults1));
        result.addAll(Arrays.asList(pilarResults2));
        result.addAll(Arrays.asList(javaResults));
        return result.toArray();
      }
    }
    
    return proceed(object);
  }
}
