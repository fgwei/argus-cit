package iamandroid.builders;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

public class AmandroidBuilder extends IncrementalProjectBuilder {

	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor)
      throws CoreException {
		if (kind == IncrementalProjectBuilder.FULL_BUILD) {
			fullBuild(monitor);
		} else {
			IResourceDelta delta = getDelta(getProject());
			if (delta == null) {
			   fullBuild(monitor);
			} else {
			   incrementalBuild(delta, monitor);
			}
		}
		return null;
	}
	
	class AmandroidBuildVisitor implements IResourceVisitor {
		@Override
		public boolean visit(IResource resource) throws CoreException {
			// TODO Auto-generated method stub
			return false;
		}
	}
	
	class AmandroidBuildDeltaVisitor implements IResourceDeltaVisitor {
		@Override
		public boolean visit(IResourceDelta delta) throws CoreException {
			// TODO Auto-generated method stub
			return false;
		}
	}
	
	protected void fullBuild(final IProgressMonitor monitor) throws CoreException {
		try {
			getProject().accept(new AmandroidBuildVisitor());
	  } catch (CoreException e) { }
	}
	
	protected void incrementalBuild(IResourceDelta delta,
			IProgressMonitor monitor) throws CoreException {
		try {
			// the visitor does the work.
	    delta.accept(new AmandroidBuildDeltaVisitor());
		} catch (CoreException e) { }
	}
	
	protected void startupOnInitialize() {
	   // add builder init logic here
	}
	protected void clean(IProgressMonitor monitor) {
	   // add builder clean logic here
	}

	@Override
	public ISchedulingRule getRule(int kind, Map<String, String> args) {
		// TODO Auto-generated method stub
		return super.getRule(kind, args);
	}
	
}
