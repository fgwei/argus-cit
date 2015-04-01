package amanide.ui.actions.container;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import amanide.AmanIDEStructureConfigHelpers;
import amanide.natures.IPilarPathNature;
import amanide.natures.PilarNature;
import amanide.utils.Log;
import amanide.utils.OrderedMap;
import amanide.utils.StringUtils;

/**
 * Action used to add a file folder to its project's PILARPATH
 * 
 * @author Fengguo Wei
 */
public class AddSrcFolder extends ContainerAction {

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
			IPilarPathNature pilarPathNature = PilarNature
					.getPilarPathNature(project);
			if (pilarPathNature == null) {
				Log.log("Unable to get PilarNature on project: " + project);
				return 0;
			}
			OrderedMap<String, String> projectSourcePathMap = pilarPathNature
					.getProjectSourcePathResolvedToUnresolvedMap();
			String pathToAdd = container.getFullPath().toString();
			if (projectSourcePathMap.containsKey(pathToAdd)) {
				return 0;
			}

			pathToAdd = AmanIDEStructureConfigHelpers
					.convertToProjectRelativePath(project.getFullPath()
							.toString(), pathToAdd);

			Collection<String> values = new ArrayList<String>(
					projectSourcePathMap.values());
			values.add(pathToAdd);
			pilarPathNature.setProjectSourcePath(StringUtils.join("|", values));
			PilarNature.getPilarNature(project).rebuildPath();
			return 1;
		} catch (CoreException e) {
			Log.log(IStatus.ERROR,
					"Unexpected error setting project properties", e);
		}
		return 0;
	}

}
