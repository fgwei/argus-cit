package argus.tools.eclipse.contribution.weaving.jdt.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.containers.CompositeSourceContainer;

public aspect SourceLookupAspect {
  pointcut findSourceElements1(Object object) :
    execution(Object[] AbstractSourceLookupDirector.findSourceElements(Object)) &&
    args(object);
  
  pointcut findSourceElements2(String name) :
	execution(Object[] CompositeSourceContainer.findSourceElements(String)) &&
	args(name);
  
  Object[] around(Object object) throws CoreException :
    findSourceElements1(object) {
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
  
  Object[] around(String name) throws CoreException :
	  findSourceElements2(name) {
	  if(name != null && name.endsWith(".java")) {
		  String pilarName1 = name.substring(0, name.length()-5)+".pilar";
	      String pilarName2 = name.substring(0, name.length()-5)+".plr";
	      List result = new ArrayList();
	      Object[] pilarResults1 = proceed(pilarName1);
	      Object[] pilarResults2 = proceed(pilarName2);
	      Object[] javaResults = proceed(name);
	      result.addAll(Arrays.asList(pilarResults1));
	      result.addAll(Arrays.asList(pilarResults2));
	      result.addAll(Arrays.asList(javaResults));
	      return result.toArray();
	  }
	  return proceed(name);
  }
}
