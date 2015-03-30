package amanide.ui.actions.container;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import amanide.natures.IPilarPathNature;
import amanide.natures.PilarNature;
import amanide.utils.Log;
import amanide.utils.OrderedMap;
import amanide.utils.StringUtils;

/**
 * Action used to remove a file folder from its project's PILARPATH.
 * 
 * adapted from org.python.pydev.ui.actions.container.PyRemSrcFolder.java
 * 
 * @author Fengguo Wei
 */
public class RemSrcFolder extends ContainerAction {

	@Override
	protected boolean confirmRun() {
		return true;
	}

	@Override
	protected void afterRun(int resourcesAffected) {

	}

	@Override
	protected int doActionOnContainer(IContainer container,
			IProgressMonitor monitor) {
		try {
			IProject project = container.getProject();
			IPilarPathNature pythonPathNature = PilarNature
					.getPilarPathNature(project);
			if (pythonPathNature == null) {
				Log.log("Unable to get PilarNature on project: " + project);
				return 0;
			}
			OrderedMap<String, String> projectSourcePathMap = pythonPathNature
					.getProjectSourcePathResolvedToUnresolvedMap();
			String pathToRemove = container.getFullPath().toString();

			if (projectSourcePathMap.remove(pathToRemove) == null) {
				return 0;
			}
			// Set back the map with the variables, not the one with resolved
			// vars.
			pythonPathNature.setProjectSourcePath(StringUtils.join("|",
					projectSourcePathMap.values()));
			PilarNature.getPilarNature(project).rebuildPath();
			return 1;
		} catch (CoreException e) {
			Log.log(IStatus.ERROR,
					"Unexpected error setting project properties", e);
		}
		return 0;
	}

}
