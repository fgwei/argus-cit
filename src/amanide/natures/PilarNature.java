package amanide.natures;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.internal.resources.Project;
import org.eclipse.core.internal.resources.ProjectInfo;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;

import amanide.AmanIDEPlugin;
import amanide.navigator.elements.ProjectConfigError;
import amanide.utils.JobProgressComunicator;
import amanide.utils.Log;
import amanide.utils.MisconfigurationException;
import amanide.utils.StringUtils;
import amanide.utils.Tuple;

/**
 * PilarNature is used as a marker class.
 * 
 * When pilar nature is presct gets extra properties. Project gets assigned
 * pilar nature when: - a pilar file is edited - a pilar project wizard is
 * created
 * 
 * @author <a href="mailto:wfg611004900521@gmail.com">Fengguo Wei</a>
 */
public class PilarNature extends AbstractPilarNature implements IProjectNature {
	private IProject project;
	private final Object initLock = new Object();
	/**
	 * This is the nature ID
	 */
	public static final String PILAR_NATURE_ID = "amanide.pilarNature";

	/**
	 * This is the nature name
	 */
	public static final String PILAR_NATURE_NAME = "pilarNature";

	/**
	 * Builder id for amanide (code completion todo and others)
	 */
	public static final String BUILDER_ID = "amanide.AmanIDEBuilder";

	/**
	 * Contains a list with the natures created.
	 */
	private final static List<WeakReference<PilarNature>> createdNatures = new ArrayList<WeakReference<PilarNature>>();

	/**
	 * This is the completions cache for the nature represented by this object
	 * (it is associated with a project).
	 */
	// private ICodeCompletionASTManager astManager;

	/**
	 * We have to know if it has already been initialized.
	 */
	private boolean initialized;

	/**
	 * Manages pythonpath things
	 */
	private final IPilarPathNature pilarPathNature = new PilarPathNature();

	/**
	 * Used to actually store settings for the pythonpath
	 */
	private final IPilarNatureStore pilarNatureStore = new PilarNatureStore();

	/**
	 * constant that stores the name of the python version we are using for the
	 * project with this nature
	 */
	private static QualifiedName pilarProjectVersion = null;

	/**
	 * Constructor
	 * 
	 * Adds the nature to the list of created natures.
	 */
	public PilarNature() {
		synchronized (createdNatures) {
			createdNatures.add(new WeakReference<PilarNature>(this));
		}
	}

	/**
	 * This is the job that is used to rebuild the pilar nature modules.
	 */
	protected class RebuildPilarNatureModules extends Job {

		protected RebuildPilarNatureModules() {
			super("Pilar Nature: rebuilding modules");
		}

		@Override
		@SuppressWarnings("unchecked")
		protected IStatus run(IProgressMonitor monitor) {

			String paths;
			try {
				paths = pilarPathNature.getOnlyProjectPilarPathStr();
			} catch (CoreException e1) {
				Log.log(e1);
				return Status.OK_STATUS;
			}

			try {
				if (monitor.isCanceled()) {
					return Status.OK_STATUS;
				}
				final JobProgressComunicator jobProgressComunicator = new JobProgressComunicator(
						monitor, "Rebuilding modules",
						IProgressMonitor.UNKNOWN, this);
				final PilarNature nature = PilarNature.this;
				try {
					// ICodeCompletionASTManager tempAstManager = astManager;
					// if (tempAstManager == null) {
					// tempAstManager = new ASTManager();
					// }
					if (monitor.isCanceled()) {
						return Status.OK_STATUS;
					}
					// synchronized (tempAstManager.getLock()) {
					// astManager = tempAstManager;
					// tempAstManager.setProject(getProject(), nature, false);
					// // it
					// is
					// a
					// new
					// manager,
					// so,
					// remove
					// all
					// deltas

					// begins task automatically
					// tempAstManager.changePilarPath(paths, project,
					// jobProgressComunicator);
					// if (monitor.isCanceled()) {
					// return Status.OK_STATUS;
					// }
					// saveAstManager();
					// }
				} catch (Throwable e) {
					Log.log(e);
				}

				if (monitor.isCanceled()) {
					return Status.OK_STATUS;
				}
				PilarNatureListenersManager.notifyPilarPathRebuilt(project,
						nature);
				// end task
				jobProgressComunicator.done();
			} catch (Exception e) {
				Log.log(e);
			}
			return Status.OK_STATUS;
		}
	}

	/**
	 * @return the natures that were created.
	 */
	public static List<PilarNature> getInitializedPilarNatures() {
		ArrayList<PilarNature> ret = new ArrayList<PilarNature>();
		synchronized (createdNatures) {
			for (Iterator<WeakReference<PilarNature>> it = createdNatures
					.iterator(); it.hasNext();) {
				PilarNature pilarNature = it.next().get();
				if (pilarNature == null) {
					it.remove();
				} else if (pilarNature.getProject() != null) {
					ret.add(pilarNature);
				}
			}
		}
		return ret;
	}

	static QualifiedName getPilarProjectVersionQualifiedName() {
		if (pilarProjectVersion == null) {
			// we need to do this because the plugin ID may not be known on
			// 'static' time
			pilarProjectVersion = new QualifiedName(
					AmanIDEPlugin.getPluginID(), "PILAR_PROJECT_VERSION");
		}
		return pilarProjectVersion;
	}

	/**
	 * constant that stores the name of the python version we are using for the
	 * project with this nature
	 */
	private static QualifiedName pilarProjectInterpreter = null;

	// static QualifiedName getPilarProjectInterpreterQualifiedName() {
	// if (pilarProjectInterpreter == null) {
	// // we need to do this because the plugin ID may not be known on
	// // 'static' time
	// pilarProjectInterpreter = new QualifiedName(
	// AmanIDEPlugin.getPluginID(), "PILAR_PROJECT_INTERPRETER");
	// }
	// return pilarProjectInterpreter;
	// }

	@Override
	public boolean isResourceInPilarpathProjectSources(IResource resource,
			boolean addExternal) throws MisconfigurationException,
			CoreException {
		String resourceOSString = AmanIDEPlugin.getIResourceOSString(resource);
		if (resourceOSString == null) {
			return false;
		}
		return isResourceInPilarpathProjectSources(resourceOSString,
				addExternal);

	}

	@Override
	public boolean isResourceInPilarpathProjectSources(String absPath,
			boolean addExternal) throws MisconfigurationException,
			CoreException {
		return resolveModuleOnlyInProjectSources(absPath, addExternal) != null;
	}

	@Override
	public String resolveModuleOnlyInProjectSources(IResource fileAbsolutePath,
			boolean addExternal) throws CoreException,
			MisconfigurationException {

		String resourceOSString = AmanIDEPlugin
				.getIResourceOSString(fileAbsolutePath);
		if (resourceOSString == null) {
			return null;
		}
		return resolveModuleOnlyInProjectSources(resourceOSString, addExternal);
	}

	/**
	 * This method is called only when the project has the nature added..
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#configure()
	 */
	@Override
	public void configure() throws CoreException {
	}

	/**
	 * @see org.eclipse.core.resources.IProjectNature#deconfigure()
	 */
	@Override
	public void deconfigure() throws CoreException {
	}

	/**
	 * Returns the project
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#getProject()
	 */
	@Override
	public IProject getProject() {
		return project;
	}

	/**
	 * Sets this nature's project - called from the eclipse platform.
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#setProject(org.eclipse.core.resources.IProject)
	 */
	@Override
	public synchronized void setProject(final IProject project) {
		getStore().setProject(project);
		this.project = project;
		this.pilarPathNature.setProject(project, this);

		if (project != null) {
			// call initialize always - let it do the control.
			init(null, null, new NullProgressMonitor(), null);
		} else {
			this.clearCaches(false);
		}

	}

	public static IPilarNature addNature(IEditorInput element) {
		if (element instanceof FileEditorInput) {
			IFile file = (IFile) ((FileEditorInput) element)
					.getAdapter(IFile.class);
			if (file != null) {
				try {
					return PilarNature.addNature(file.getProject(), null, null,
							null, null);
				} catch (CoreException e) {
					Log.log(e);
				}
			}
		}
		return null;
	}

	/**
	 * Utility routine to remove a PilarNature from a project.
	 */
	public static synchronized void removeNature(IProject project,
			IProgressMonitor monitor) throws CoreException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}

		PilarNature nature = PilarNature.getPilarNature(project);
		if (nature == null) {
			return;
		}

		try {
			// we have to set the nature store to stop listening changes to
			// .amanideproject
			nature.pilarNatureStore.setProject(null);
		} catch (Exception e) {
			Log.log(e);
		}

		try {
			// we have to remove the project from the pilarpath nature too...
			nature.pilarPathNature.setProject(null, null);
		} catch (Exception e) {
			Log.log(e);
		}

		// notify listeners that the pilarpath nature is now empty for this
		// project
		try {
			PilarNatureListenersManager.notifyPilarPathRebuilt(project, null);
		} catch (Exception e) {
			Log.log(e);
		}

		try {
			// actually remove the amanide configurations
			IResource member = project.findMember(".amanideproject");
			if (member != null) {
				member.delete(true, null);
			}
		} catch (CoreException e) {
			Log.log(e);
		}

		// and finally... remove the nature

		IProjectDescription description = project.getDescription();
		List<String> natures = new ArrayList<String>(Arrays.asList(description
				.getNatureIds()));
		natures.remove(PILAR_NATURE_ID);
		description.setNatureIds(natures.toArray(new String[natures.size()]));
		project.setDescription(description, monitor);
	}

	/**
	 * Lock to access the map below.
	 */
	private final static Object mapLock = new Object();

	/**
	 * If some project has a value here, we're already in the process of adding
	 * a nature to it.
	 */
	private final static Map<IProject, Object> mapLockAddNature = new HashMap<IProject, Object>();

	/**
	 * Utility routine to add PilarNature to the project
	 * 
	 * @param projectPilarpath
	 *            : @see {@link IPilarPathNature#setProjectSourcePath(String)}
	 */
	public static IPilarNature addNature(
			// Only synchronized internally!
			IProject project, IProgressMonitor monitor, String version,
			String projectPilarpath, Map<String, String> variableSubstitution)
			throws CoreException {

		if (project == null || !project.isOpen()) {
			return null;
		}

		if (project.hasNature(PILAR_NATURE_ID)) {
			// Return if it already has the nature configured.
			return getPilarNature(project);
		}
		boolean alreadyLocked = false;
		synchronized (mapLock) {
			if (mapLockAddNature.get(project) == null) {
				mapLockAddNature.put(project, new Object());
			} else {
				alreadyLocked = true;
			}
		}
		if (alreadyLocked) {
			// Ok, there's some execution path already adding the nature. Let's
			// simply wait a bit here and return
			// the nature that's there (this way we avoid any possible deadlock)
			// -- in the worse case, null
			// will be returned here, but this is a part of the protocol
			// anyways.
			// Done because of: Deadlock acquiring PilarNature -- at
			// setDescription()
			// https://sourceforge.net/tracker/?func=detail&aid=3478567&group_id=85796&atid=577329
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// ignore
			}
			return getPilarNature(project);
		} else {
			IProjectDescription desc = project.getDescription();
			if (monitor == null) {
				monitor = new NullProgressMonitor();
			}
			// if (projectInterpreter == null) {
			// projectInterpreter = IPilarNature.DEFAULT_INTERPRETER;
			// }
			try {
				// Lock only for the project and add the nature (at this point
				// we know it hasn't been added).
				String[] natures = desc.getNatureIds();
				String[] newNatures = new String[natures.length + 1];
				System.arraycopy(natures, 0, newNatures, 0, natures.length);
				newNatures[natures.length] = PILAR_NATURE_ID;
				desc.setNatureIds(newNatures);

				// add the builder. It is used for pylint, pychecker, code
				// completion, etc.
				ICommand[] commands = desc.getBuildSpec();

				// now, add the builder if it still hasn't been added.
				// if (hasBuilder(commands) == false
				// && AmanIDEBuilderPrefPage.usePydevBuilders()) {
				//
				// ICommand command = desc.newCommand();
				// command.setBuilderName(BUILDER_ID);
				// ICommand[] newCommands = new ICommand[commands.length + 1];
				//
				// System.arraycopy(commands, 0, newCommands, 1,
				// commands.length);
				// newCommands[0] = command;
				// desc.setBuildSpec(newCommands);
				// }
				project.setDescription(desc, monitor);

				IProjectNature n = getPilarNature(project);
				if (n instanceof PilarNature) {
					PilarNature nature = (PilarNature) n;
					// call initialize always - let it do the control.
					nature.init(version, projectPilarpath, monitor,
					// projectInterpreter,
							variableSubstitution);
					return nature;
				}
			} finally {
				synchronized (mapLock) {
					mapLockAddNature.remove(project);
				}
			}
		}

		return null;
	}

	/**
	 * Utility to know if the pydev builder is in one of the commands passed.
	 * 
	 * @param commands
	 */
	private static boolean hasBuilder(ICommand[] commands) {
		for (int i = 0; i < commands.length; i++) {
			if (commands[i].getBuilderName().equals(BUILDER_ID)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Initializes the pilar nature if it still has not been for this session.
	 * 
	 * Actions includes restoring the dump from the code completion cache
	 * 
	 * @param projectPilarpath
	 *            this is the project python path to be used (may be null) -- if
	 *            not null, this nature is being created
	 * @param version
	 *            this is the version (project type) to be used (may be null) --
	 *            if not null, this nature is being created
	 * @param monitor
	 */
	@SuppressWarnings("unchecked")
	private void init(String version, String projectPilarpath,
			IProgressMonitor monitor,
			// String interpreter,
			Map<String, String> variableSubstitution) {

		// if some information is passed, restore it (even if it was already
		// initialized)
		boolean updatePaths = version != null || projectPilarpath != null
				|| variableSubstitution != null;

		if (updatePaths) {
			this.getStore().startInit();
			try {
				if (variableSubstitution != null) {
					this.getPilarPathNature().setVariableSubstitution(
							variableSubstitution);
				}
				if (projectPilarpath != null) {
					this.getPilarPathNature().setProjectSourcePath(
							projectPilarpath);
				}
				if (version != null) {
					this.setVersion(version);
				}
			} catch (CoreException e) {
				Log.log(e);
			} finally {
				this.getStore().endInit();
			}
		} else {
			// Change: 1.3.10: it could be reloaded more than once... (when it
			// shouldn't)
			// if (astManager != null) {
			// return; // already initialized...
			// }
		}

		synchronized (initLock) {
			if (initialized && !updatePaths) {
				return;
			}
			initialized = true;
		}

		if (updatePaths) {
			// If updating the paths, rebuild and return (don't try to load an
			// existing ast manager
			// and restore anything already there)
			rebuildPath();
			return;
		}

		// if (monitor.isCanceled()) {
		// checkPilarPathHelperPathsJob.schedule(500);
		// return;
		// }

		// Change: 1.3.10: no longer in a Job... should already be called in a
		// job if that's needed.

		// try {
		// File astOutputFile = getAstOutputFile();
		// if (astOutputFile == null) {
		// Log.log(IStatus.INFO, "Not saving ast manager for: "
		// + this.project + ". No write area available.", null);
		// return; // The project was deleted
		// }
		// astManager = ASTManager.loadFromFile(astOutputFile);
		// if (astManager != null) {
		// synchronized (astManager.getLock()) {
		// astManager.setProject(getProject(), this, true); // this is
		// // the
		// // project
		// // related
		// // to
		// // it,
		// // restore
		// // the
		// // deltas
		// // (we
		// // may
		// // have
		// // some
		// // crash)
		//
		// // just a little validation so that we restore the needed
		// // info if we did not get the modules
		// if (astManager.getModulesManager().getOnlyDirectModules().length <
		// 15) {
		// astManager = null;
		// }
		//
		// if (astManager != null) {
		// List<IInterpreterObserver> participants = ExtensionHelper
		// .getParticipants(ExtensionHelper.PYDEV_INTERPRETER_OBSERVER);
		// for (IInterpreterObserver observer : participants) {
		// try {
		// observer.notifyNatureRecreated(this, monitor);
		// } catch (Exception e) {
		// // let's not fail because of other plugins
		// Log.log(e);
		// }
		// }
		// }
		// }
		// }
		// } catch (Exception e) {
		// // Log.logInfo("Info: Rebuilding internal caches for: "+this.project,
		// // e);
		// astManager = null;
		// }

		// errors can happen when restoring it
		// if (astManager == null) {
		// try {
		// rebuildPath();
		// } catch (Exception e) {
		// Log.log(e);
		// }
		// } else {
		// checkPilarPathHelperPathsJob.schedule(500);
		// }
	}

	private final Job checkPilarPathHelperPathsJob = new Job(
			"Check restored pythonpath") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			// try {
			// if (astManager != null) {
			// String pythonpath = pilarPathNature
			// .getOnlyProjectPilarPathStr(true);
			// PilarPathHelper pythonPathHelper = (PilarPathHelper) astManager
			// .getModulesManager().getPilarPathHelper();
			// // If it doesn't match, rebuid the pythonpath!
			// if (!new HashSet<String>(
			// PilarPathHelper.parsePilarPathFromStr(pythonpath,
			// null)).equals(new HashSet<String>(
			// pythonPathHelper.getPilarpath()))) {
			// rebuildPath();
			// }
			// }
			// } catch (CoreException e) {
			// Log.log(e);
			// }
			return Status.OK_STATUS;
		}

	};

	/**
	 * Returns the directory that should store completions.
	 * 
	 * @param p
	 * @return
	 */
	private File getCompletionsCacheDir(IProject p) {
		IPath path = p.getWorkingLocation(AmanIDEPlugin.getPluginID());

		if (path == null) {
			// this can happen if the project was removed.
			return null;
		}
		File file = new File(path.toOSString());
		return file;
	}

	@Override
	public File getCompletionsCacheDir() {
		return getCompletionsCacheDir(getProject());
	}

	/**
	 * @return the file where the python path helper should be saved.
	 */
	private File getAstOutputFile() {
		File completionsCacheDir = getCompletionsCacheDir();
		if (completionsCacheDir == null) {
			return null;
		}
		return new File(completionsCacheDir, "v1_astmanager");
	}

	/**
	 * Can be called to refresh internal info (or after changing the path in the
	 * preferences).
	 * 
	 * @throws CoreException
	 */
	@Override
	public void rebuildPath() {
		clearCaches(true);
		this.rebuildJob.cancel();
		this.rebuildJob.schedule(20L);
	}

	private RebuildPilarNatureModules rebuildJob = new RebuildPilarNatureModules();

	/**
	 * @return Returns the completionsCache. Note that it can be null.
	 */
	// @Override
	// public ICodeCompletionASTManager getAstManager() {
	// return astManager; // Change: don't wait if it's still not initialized.
	// }

	@Override
	public boolean isOkToUse() {
		return // this.astManager != null &&
		this.pilarPathNature != null;
	}

	// public void setAstManager(ICodeCompletionASTManager astManager) {
	// this.astManager = astManager;
	// }

	@Override
	public IPilarPathNature getPilarPathNature() {
		return pilarPathNature;
	}

	public static IPilarPathNature getPilarPathNature(IProject project) {
		PilarNature pythonNature = getPilarNature(project);
		if (pythonNature != null) {
			return pythonNature.pilarPathNature;
		}
		return null;
	}

	/**
	 * @return all the python natures available in the workspace (for opened and
	 *         existing projects)
	 */
	public static List<IPilarNature> getAllPilarNatures() {
		List<IPilarNature> natures = new ArrayList<IPilarNature>();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = root.getProjects();
		for (IProject project : projects) {
			PilarNature nature = getPilarNature(project);
			if (nature != null) {
				natures.add(nature);
			}
		}
		return natures;
	}

	public static PilarNature getPilarNature(IResource resource) {
		if (resource == null) {
			return null;
		}
		return getPilarNature(resource.getProject());
	}

	private static final Object lockGetNature = new Object();

	/**
	 * @param project
	 *            the project we want to know about (if it is null, null is
	 *            returned)
	 * @return the python nature for a project (or null if it does not exist for
	 *         the project)
	 * 
	 * @note: it's synchronized because more than 1 place could call
	 *        getPilarNature at the same time and more than one nature ended up
	 *        being created from project.getNature().
	 */
	public static PilarNature getPilarNature(IProject project) {
		if (project != null && project.isOpen()) {
			try {
				// Speedup: as this method is called a lot, we just check if the
				// nature is available internally without
				// any locks, and just lock if it's not (which is needed to
				// avoid a racing condition creating more
				// than 1 nature).
				try {
					if (project instanceof Project) {
						Project p = (Project) project;
						ProjectInfo info = (ProjectInfo) p.getResourceInfo(
								false, false);
						IProjectNature nature = info.getNature(PILAR_NATURE_ID);
						if (nature instanceof PilarNature) {
							return (PilarNature) nature;
						}
					}
				} catch (Throwable e) {
					// Shouldn't really happen, but as using internal methods of
					// project, who knows if it may change
					// from one version to another.
					Log.log(e);
				}

				synchronized (lockGetNature) {
					IProjectNature n = project.getNature(PILAR_NATURE_ID);
					if (n instanceof PilarNature) {
						return (PilarNature) n;
					}
				}
			} catch (CoreException e) {
				Log.logInfo(e);
			}
		}
		return null;
	}

	/**
	 * Stores the version as a cache (the actual version is set in the xml
	 * file). This is so that we don't have a runtime penalty for it.
	 */
	private String versionPropertyCache = null;

	/**
	 * Returns the Pilar version of the Project.
	 * 
	 * It's a String in the format "pilar 4.0", as defined by the constants
	 * PYTHON_VERSION_XX and JYTHON_VERSION_XX in IPilarNature.
	 * 
	 * @note it might have changed on disk (e.g. a repository update).
	 * @return the python version for the project
	 * @throws CoreException
	 */
	@Override
	public String getVersion() throws CoreException {
		return getVersionAndError().o1;
	}

	private Tuple<String, String> getVersionAndError() throws CoreException {
		if (project != null) {
			if (versionPropertyCache == null) {
				String storeVersion = getStore().getPropertyFromXml(
						getPilarProjectVersionQualifiedName());
				if (storeVersion == null) { // there is no such property set
					// (let's set it to the default)
					setVersion(getDefaultVersion()); // will set the
					// versionPropertyCache
					// too
				} else {
					// now, before returning and setting in the cache, let's
					// make sure it's a valid version.
					if (!IPilarNature.Versions.ALL_VERSIONS_ANY_FLAVOR
							.contains(storeVersion)) {
						Log.log("The stored version is invalid ("
								+ storeVersion + "). Setting default.");
						setVersion(getDefaultVersion()); // will set the
						// versionPropertyCache
						// too
					} else {
						// Ok, it's correct.
						versionPropertyCache = storeVersion;
					}
				}
			}
		} else {
			String msg = "Trying to get version without project set. Returning default.";
			Log.log(msg);
			return new Tuple<String, String>(getDefaultVersion(), msg);
		}

		if (versionPropertyCache == null) {
			String msg = "The cached version is null. Returning default.";
			Log.log(msg);
			return new Tuple<String, String>(getDefaultVersion(), msg);

		} else if (!IPilarNature.Versions.ALL_VERSIONS_ANY_FLAVOR
				.contains(versionPropertyCache)) {
			String msg = "The cached version (" + versionPropertyCache
					+ ") is invalid. Returning default.";
			Log.log(msg);
			return new Tuple<String, String>(getDefaultVersion(), msg);
		}
		return new Tuple<String, String>(versionPropertyCache, null);
	}

	/**
	 * @param version
	 *            : the project version given the constants PYTHON_VERSION_XX
	 *            and JYTHON_VERSION_XX in IPilarNature. If null, nothing is
	 *            done for the version.
	 * 
	 * @param interpreter
	 *            the interpreter to be set if null, nothing is done to the
	 *            interpreter.
	 * 
	 * @throws CoreException
	 */
	@Override
	public void setVersion(String version) throws CoreException {
		clearCaches(false);

		if (version != null) {
			this.versionPropertyCache = version;
		}

		// if (interpreter != null) {
		// this.interpreterPropertyCache = interpreter;
		// }

		if (project != null) {
			boolean notify = false;
			if (version != null) {
				// IPilarNatureStore store = getStore();
				// QualifiedName pythonProjectVersionQualifiedName =
				// getPilarProjectVersionQualifiedName();
				// String current = store
				// .getPropertyFromXml(pythonProjectVersionQualifiedName);
				//
				// if (current == null || !current.equals(version)) {
				// store.setPropertyToXml(pythonProjectVersionQualifiedName,
				// version, true);
				// notify = true;
				// }
			}
			// if (interpreter != null) {
			// IPilarNatureStore store = getStore();
			// QualifiedName pythonProjectInterpreterQualifiedName =
			// getPilarProjectInterpreterQualifiedName();
			// String current = store
			// .getPropertyFromXml(pythonProjectInterpreterQualifiedName);
			//
			// if (current == null || !current.equals(interpreter)) {
			// store.setPropertyToXml(
			// pythonProjectInterpreterQualifiedName, interpreter,
			// true);
			// notify = true;
			// }
			// }
			if (notify) {
				PilarNatureListenersManager.notifyPilarPathRebuilt(project,
						this);
			}
		}
	}

	@Override
	public String getDefaultVersion() {
		return PILAR_VERSION_LATEST;
	}

	// @Override
	// public void saveAstManager() {
	// File astOutputFile = getAstOutputFile();
	// if (astOutputFile == null) {
	// // The project was removed. Nothing to save here.
	// Log.log(IStatus.INFO, "Not saving ast manager for: " + this.project
	// + ". No write area available.", null);
	// return;
	// }
	//
	// if (astManager == null) {
	// return;
	//
	// } else {
	// synchronized (astManager.getLock()) {
	// astManager.saveToFile(astOutputFile);
	// }
	// }
	// }
	// public int getInterpreterType() throws CoreException {
	// if (interpreterType == null) {
	// String version = getVersion();
	// interpreterType = getInterpreterTypeFromVersion(version);
	// }
	//
	// return interpreterType;
	//
	// }

	// public static int getInterpreterTypeFromVersion(String version)
	// throws CoreException {
	// int interpreterType;
	// if (IPilarNature.Versions.ALL_PILAR_VERSIONS.contains(version)) {
	// interpreterType = INTERPRETER_TYPE_PILAR;
	// } else {
	// // if others fail, consider it python
	// interpreterType = INTERPRETER_TYPE_PYTHON;
	// }
	//
	// return interpreterType;
	// }

	/**
	 * Resolve the module given the absolute path of the file in the filesystem.
	 * 
	 * @param fileAbsolutePath
	 *            the absolute file path
	 * @return the module name
	 */
	@Override
	public String resolveModule(String fileAbsolutePath) {
		String moduleName = null;

		// if (astManager != null) {
		// moduleName = astManager.getModulesManager().resolveModule(
		// fileAbsolutePath);
		// }
		return moduleName;
	}

	/**
	 * Resolve the module given the absolute path of the file in the filesystem.
	 * 
	 * @param fileAbsolutePath
	 *            the absolute file path
	 * @return the module name
	 * @throws CoreException
	 */
	@Override
	public String resolveModuleOnlyInProjectSources(String fileAbsolutePath,
			boolean addExternal) throws CoreException {
		String moduleName = null;

		// if (astManager != null) {
		// IModulesManager modulesManager = astManager.getModulesManager();
		// if (modulesManager instanceof ProjectModulesManager) {
		// moduleName = ((ProjectModulesManager) modulesManager)
		// .resolveModuleOnlyInProjectSources(fileAbsolutePath,
		// addExternal);
		// }
		// }
		return moduleName;
	}

	public static String[] getStrAsStrItems(String str) {
		return str.split("\\|");
	}

	// public IInterpreterManager getRelatedInterpreterManager() {
	// try {
	// int interpreterType = getInterpreterType();
	// switch (interpreterType) {
	// case IInterpreterManager.INTERPRETER_TYPE_PYTHON:
	// return PydevPlugin.getPilarInterpreterManager();
	//
	// case IInterpreterManager.INTERPRETER_TYPE_JYTHON:
	// return PydevPlugin.getJythonInterpreterManager();
	//
	// case IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON:
	// return PydevPlugin.getIronpythonInterpreterManager();
	//
	// default:
	// throw new RuntimeException(
	// "Unable to find the related interpreter manager for type: "
	// + interpreterType);
	// }
	// } catch (Exception e) {
	// throw new RuntimeException(e);
	// }
	//
	// }

	// ------------------------------------------------------------------------------------------
	// LOCAL CACHES
	public void clearCaches(boolean clearGlobalModulesCache) {
		this.versionPropertyCache = null;
		this.pilarPathNature.clearCaches();
		if (clearGlobalModulesCache) {
			// ModulesManager.clearCache();
		}
	}

	/**
	 * @return the version of the grammar as defined in
	 *         IPilarNature.GRAMMAR_PYTHON...
	 */
	@Override
	public int getGrammarVersion() {
		try {
			String version = getVersion();
			if (version == null) {
				Log.log("Found null version. Returning default.");
				return LATEST_GRAMMAR_VERSION;
			}

			List<String> splitted = StringUtils.split(version, ' ');
			if (splitted.size() != 2) {
				String storeVersion;
				try {
					storeVersion = getStore().getPropertyFromXml(
							getPilarProjectVersionQualifiedName());
				} catch (Exception e) {
					storeVersion = "Unable to get storeVersion. Reason: "
							+ e.getMessage();
				}

				Log.log("Found invalid version: " + version + "\n"
						+ "Returning default\n" + "Project: " + this.project
						+ "\n" + "versionPropertyCache: "
						+ versionPropertyCache + "\n" + "storeVersion:"
						+ storeVersion);

				return LATEST_GRAMMAR_VERSION;
			}

			String grammarVersion = splitted.get(1);
			return getGrammarVersionFromStr(grammarVersion);

		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param grammarVersion
	 *            a string in the format 2.x or 3.x
	 * @return the grammar version as given in
	 *         IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION
	 */
	public static int getGrammarVersionFromStr(String grammarVersion) {
		// Note that we don't have the grammar for all versions, so, we use the
		// one closer to it (which is
		// fine as they're backward compatible).
		if ("4.0".equals(grammarVersion)) {
			return GRAMMAR_PILAR_VERSION_4_0;

		}

		if (grammarVersion != null) {
			return LATEST_GRAMMAR_VERSION;
		}

		Log.log("Unable to recognize version: " + grammarVersion
				+ " returning default.");
		return LATEST_GRAMMAR_VERSION;
	}

	protected IPilarNatureStore getStore() {
		return pilarNatureStore;
	}

	@Override
	public String toString() {
		return "PilarNature: " + this.project;
	}

	// @Override
	// public boolean startRequests() {
	// // TODO Auto-generated method stub
	// return false;
	// }

	/**
	 * @return a list of configuration errors and the interpreter info for the
	 *         project (the interpreter info can be null)
	 * @throws PythonNatureWithoutProjectException
	 */
	public List<ProjectConfigError> getConfigErrorsAndInfo(
			final IProject relatedToProject)
			throws PilarNatureWithoutProjectException {
		ArrayList<ProjectConfigError> lst = new ArrayList<ProjectConfigError>();
		if (this.project == null) {
			lst.add(new ProjectConfigError(relatedToProject,
					"The configured nature has no associated project."));
		}
		try {

			List<String> projectSourcePathSet = new ArrayList<String>(this
					.getPilarPathNature().getProjectSourcePathSet(true));
			Collections.sort(projectSourcePathSet);
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

			for (String path : projectSourcePathSet) {
				if (path.trim().length() > 0) {
					IPath p = new Path(path);
					IResource resource = root.findMember(p);
					if (resource == null) {
						relatedToProject.refreshLocal(p.segmentCount(), null);
						resource = root.findMember(p); // 2nd attempt (after
														// refresh)
					}
					if (resource == null || !resource.exists()) {
						lst.add(new ProjectConfigError(relatedToProject,
								"Source folder: " + path + " not found"));
					}
				}
			}

			Tuple<String, String> versionAndError = getVersionAndError();
			if (versionAndError.o2 != null) {
				lst.add(new ProjectConfigError(relatedToProject, StringUtils
						.replaceNewLines(versionAndError.o2, " ")));
			}

		} catch (Throwable e) {
			lst.add(new ProjectConfigError(relatedToProject, StringUtils
					.replaceNewLines("Unexpected error:" + e.getMessage(), " ")));
		}
		return lst;
	}

}
