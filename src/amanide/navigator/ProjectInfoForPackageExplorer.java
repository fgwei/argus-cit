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

import amanide.natures.PilarNature;
import amanide.natures.PilarNatureWithoutProjectException;
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
	public ProjectInfoForPackageExplorer(IProject project) {
		this.recreateInfo(project);
	}

	/**
	 * Recreates the information about the project.
	 */
	public void recreateInfo(IProject project) {
		configErrors.clear();
		List<ProjectConfigError> configErrorsAndInfo = getConfigErrorsAndInfo(project);
		configErrors.addAll(configErrorsAndInfo);
	}

	/**
	 * Do the update of the markers in a separate job so that we don't do that
	 * in the ui-thread.
	 * 
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

	/**
	 * Never returns null.
	 * 
	 * This method should only be called through recreateInfo.
	 */
	private List<ProjectConfigError> getConfigErrorsAndInfo(IProject project) {
		if (project == null || !project.isOpen()) {
			return new ArrayList<ProjectConfigError>();
		}
		PilarNature nature = PilarNature.getPilarNature(project);
		if (nature == null) {
			return new ArrayList<ProjectConfigError>();
		}

		// If the info is not readily available, we try to get some more
		// times... after that, if still not available,
		// we just return as if it's all OK.
		List<ProjectConfigError> configErrorsAndInfo = null;
		boolean goodToGo = false;
		for (int i = 0; i < 10 && !goodToGo; i++) {
			try {
				configErrorsAndInfo = nature.getConfigErrorsAndInfo(project);
				goodToGo = true;
			} catch (PilarNatureWithoutProjectException e1) {
				goodToGo = false;
				synchronized (this) {
					try {
						wait(100);
					} catch (InterruptedException e) {
					}
				}
			}
		}
		if (configErrorsAndInfo == null) {
			return new ArrayList<ProjectConfigError>();
		}

		if (nature != null) {
			synchronized (lock) {
				UpdateAmanIDEPackageExplorerProblemMarkers job = projectToJob
						.get(project);
				if (job == null) {
					job = new UpdateAmanIDEPackageExplorerProblemMarkers(
							"Update pydev package explorer markers for: "
									+ project);
					projectToJob.put(project, job);
				}
				job.setInfo(project, configErrorsAndInfo
						.toArray(new ProjectConfigError[configErrorsAndInfo
								.size()]));
				job.schedule();
			}
		}

		return configErrorsAndInfo;
	}

}