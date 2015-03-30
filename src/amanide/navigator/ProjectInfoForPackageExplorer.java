package amanide.navigator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import amanide.navigator.elements.PilarSourceFolder;
import amanide.navigator.elements.ProjectConfigError;
import amanide.utils.Log;
import amanide.utils.PilarMarkerUtils;

/**
 * This class contains information about the project (info we need to show in
 * the tree).
 */
public class ProjectInfoForPackageExplorer {

	/**
	 * These are the source folders that can be found in this file provider. The
	 * way we see things in this provider, the python model starts only after
	 * some source folder is found.
	 */
	private static final Map<IProject, ProjectInfoForPackageExplorer> projectToSourceFolders = new HashMap<IProject, ProjectInfoForPackageExplorer>();
	private static final Object lockProjectToSourceFolders = new Object();

	/**
	 * @return the information on a project. Can create it if it's not
	 *         available.
	 */
	public static ProjectInfoForPackageExplorer getProjectInfo(
			final IProject project) {
		if (project == null) {
			return null;
		}
		synchronized (lockProjectToSourceFolders) {
			ProjectInfoForPackageExplorer projectInfo = projectToSourceFolders
					.get(project);
			if (projectInfo == null) {
				if (!project.isOpen()) {
					return null;
				}
				// No project info: create it
				projectInfo = projectToSourceFolders.get(project);
				if (projectInfo == null) {
					projectInfo = new ProjectInfoForPackageExplorer(project);
					projectToSourceFolders.put(project, projectInfo);
				}
			} else {
				if (!project.isOpen()) {
					projectToSourceFolders.remove(project);
					projectInfo = null;
				}
			}
			return projectInfo;
		}
	}

	/**
	 * Note that the source folders are added/removed lazily (not when the info
	 * is recreated)
	 */
	public final Set<PilarSourceFolder> sourceFolders = new HashSet<PilarSourceFolder>();

	/**
	 * Whenever the info is recreated this is also recreated.
	 */
	public final List<ProjectConfigError> configErrors = new ArrayList<ProjectConfigError>();

	/**
	 * Creates the info for the passed project.
	 */
	private ProjectInfoForPackageExplorer(IProject project) {
		this.recreateInfo(project);
	}

	/**
	 * Recreates the information about the project.
	 */
	public void recreateInfo(IProject project) {
		configErrors.clear();
		// configErrors.addAll(configErrorsAndInfo.o1);
	}

	/**
	 * Do the update of the markers in a separate job so that we don't do that
	 * in the ui-thread.
	 * 
	 * See: #PyDev-88: Eclipse freeze on project import and project creation
	 * (presumable cause: virtualenvs as custom interpreters)
	 */
	private static final class UpdateAmanIDEPackageExplorerProblemMarkers
			extends Job {
		private IProject fProject;
		private ProjectConfigError[] fProjectConfigErrors;
		private final Object lockJob = new Object();

		private UpdateAmanIDEPackageExplorerProblemMarkers(String name) {
			super(name);
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final IProject project;
			final ProjectConfigError[] projectConfigErrors;
			synchronized (lockJob) {
				project = this.fProject;
				projectConfigErrors = this.fProjectConfigErrors;
				this.fProject = null;
				this.fProjectConfigErrors = null;

				// In a racing condition it's possible that it was scheduled
				// again when the projectConfigErrors was already
				// set to null.
				if (projectConfigErrors == null) {
					return Status.OK_STATUS;
				}
			}

			ArrayList lst = new ArrayList(projectConfigErrors.length);
			for (ProjectConfigError error : projectConfigErrors) {
				try {
					Map attributes = new HashMap();
					attributes.put(IMarker.MESSAGE, error.getLabel());
					attributes.put(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
					lst.add(attributes);
				} catch (Exception e) {
					Log.log(e);
				}
			}
			PilarMarkerUtils
					.replaceMarkers(
							(Map<String, Object>[]) lst.toArray(new Map[lst
									.size()]),
							project,
							PilarBaseModelProvider.AMANIDE_PACKAGE_EXPORER_PROBLEM_MARKER,
							true, monitor);

			synchronized (ProjectInfoForPackageExplorer.lock) {
				// Only need to lock at the outer lock as it's the same one
				// needed for the job creation.
				if (this.fProject == null) {
					// if it's not null, it means it was rescheduled!
					ProjectInfoForPackageExplorer.projectToJob.remove(project);
				}
			}
			return Status.OK_STATUS;
		}

		public void setInfo(IProject project,
				ProjectConfigError[] projectConfigErrors) {
			synchronized (lockJob) {
				this.fProject = project;
				this.fProjectConfigErrors = projectConfigErrors;
			}
		}
	}

	private static final Map<IProject, UpdateAmanIDEPackageExplorerProblemMarkers> projectToJob = new HashMap<IProject, UpdateAmanIDEPackageExplorerProblemMarkers>();
	private static final Object lock = new Object();

}