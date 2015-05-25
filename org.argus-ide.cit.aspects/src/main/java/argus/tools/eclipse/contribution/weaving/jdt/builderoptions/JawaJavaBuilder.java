package argus.tools.eclipse.contribution.weaving.jdt.builderoptions;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.internal.core.builder.JavaBuilder;

@SuppressWarnings("restriction")
public abstract class JawaJavaBuilder extends JavaBuilder {

  
  @Override
  public void clean(IProgressMonitor monitor) throws CoreException {
    super.clean(monitor);
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public IProject[] build(int kind, Map ignored, IProgressMonitor monitor) throws CoreException {
    return super.build(kind, ignored, monitor);
  }
  
  abstract public void setProject0(IProject project);
}
