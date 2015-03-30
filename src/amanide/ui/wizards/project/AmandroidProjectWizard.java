package amanide.ui.wizards.project;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.sireum.amandroid.decompile.AmDecoder$;
import org.sireum.amandroid.decompile.Dex2PilarConverter$;
import org.sireum.util.FileUtil$;

import amanide.AmanIDEPlugin;
import amanide.AmanIDEStructureConfigHelpers;
import amanide.callbacks.ICallback2;
import amanide.navigator.ui.AmanIDEPackageExplorer;
import amanide.navigator.ui.AmanIDEPackageExplorer.AmanIDECommonViewer;
import amanide.utils.Log;

/**
 * Amandroid Project creation wizard
 *
 * <ul>
 * <li>Asks users information about Amandroid project
 * <li>Launches another thread to create Amandroid project. A progress monitor
 * is shown in UI thread
 * </ul>
 *
 * Adapted from Middo Ohtamaa & Fabio Zadrozny
 * 
 * @author <a href="mailto:wfg611004900521@gmail.com">Fengguo Wei</a>
 */
public class AmandroidProjectWizard extends AbstractNewProjectWizard implements
		IExecutableExtension {

	/**
	 * The current selection.
	 */
	protected IStructuredSelection selection;

	public static final String WIZARD_ID = "amanide.ui.wizards.project.AmandroidProjectWizard";

	protected IWizardImportProjectAndLocationPage projectPage;

	Shell shell;

	/** Target project created by this wizard */
	IProject generatedProject;

	/** Exception throw by generator thread */
	Exception creationThreadException;

	private IProject createdProject;

	private IConfigurationElement fConfigElement;

	protected IWorkbench workbench;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
		this.selection = currentSelection;
		this.workbench = workbench;
		initializeDefaultPageImageDescriptor();
		projectPage = createProjectPage();
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	/**
	 * Creates the project page.
	 */
	protected IWizardImportProjectAndLocationPage createProjectPage() {
		return new ImportProjectAndLocationWizardPage(
				"Setting project properties", "Amandroid", "apk");
	}

	/**
	 * Returns the page that should appear after the sources page, or null if no
	 * page comes after it.
	 */
	protected WizardPage getPageAfterSourcesPage() {
		return referencePage;
	}

	/**
	 * Add wizard pages to the instance
	 *
	 * @see org.eclipse.jface.wizard.IWizard#addPages()
	 */
	@Override
	public void addPages() {
		addPage(projectPage);
		addProjectReferencePage();
	}

	/**
	 * Creates a new project resource with the entered name.
	 *
	 * @return the created project resource, or <code>null</code> if the project
	 *         was not created
	 */
	protected IProject createNewProject(
			final Object... additionalArgsToConfigProject) {
		// get a project handle
		final IProject newProjectHandle = projectPage.getProjectHandle();

		// get a project descriptor
		IPath defaultPath = Platform.getLocation();
		IPath newPath = projectPage.getLocationPath();
		if (defaultPath.equals(newPath)) {
			newPath = null;
		} else {
			// The user entered the path and it's the same as it'd be if he
			// chose the default path.
			IPath withName = defaultPath.append(newProjectHandle.getName());
			if (newPath.toFile().equals(withName.toFile())) {
				newPath = null;
			}
		}

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IProjectDescription description = workspace
				.newProjectDescription(newProjectHandle.getName());
		description.setLocation(newPath);

		// update the referenced project if provided
		if (referencePage != null) {
			IProject[] refProjects = referencePage.getReferencedProjects();
			if (refProjects.length > 0) {
				description.setReferencedProjects(refProjects);
			}
		}

		final String projectType = projectPage.getProjectType();
		final IPath importedFilePath = projectPage.getImportedFilePath();
		// define the operation to create a new project
		WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
			@Override
			protected void execute(IProgressMonitor monitor)
					throws CoreException {

				createAndConfigProject(newProjectHandle, description,
						projectType, importedFilePath, monitor,
						additionalArgsToConfigProject);
			}
		};

		// run the operation to create a new project
		try {
			getContainer().run(true, true, op);
		} catch (InterruptedException e) {
			return null;
		} catch (InvocationTargetException e) {
			Throwable t = e.getTargetException();
			if (t instanceof CoreException) {
				if (((CoreException) t).getStatus().getCode() == IResourceStatus.CASE_VARIANT_EXISTS) {
					MessageDialog
							.openError(getShell(), "Unable to create project",
									"Another project with the same name (and different case) already exists.");
				} else {
					ErrorDialog.openError(getShell(),
							"Unable to create project", null,
							((CoreException) t).getStatus());
				}
			} else {
				// Unexpected runtime exceptions and errors may still occur.
				Log.log(IStatus.ERROR, t.toString(), t);
				MessageDialog.openError(getShell(), "Unable to create project",
						t.getMessage());
			}
			return null;
		}

		return newProjectHandle;
	}

	protected ICallback2<Boolean, IPath, IPath> getCreateProjectViaAPKHandlesCallback = new ICallback2<Boolean, IPath, IPath>() {
		@Override
		public Boolean call(IPath importedFilePath, IPath projectFilePath) {
			FileUtil$ fu = FileUtil$.MODULE$;
			AmDecoder$ de = AmDecoder$.MODULE$;
			Dex2PilarConverter$ dpc = Dex2PilarConverter$.MODULE$;
			String out = de.decode(fu.toUri(importedFilePath.toFile()),
					fu.toUri(projectFilePath.toFile()));
			String dexFile = out + "/classes.dex";
			if (fu.toFile(dexFile).exists()) {
				dpc.convert(dexFile);
			}
			Path from = fu.toFile(out).toPath();
			Path to = projectFilePath.toFile().toPath();

			try (DirectoryStream<Path> directoryStream = Files
					.newDirectoryStream(from)) {
				for (Path path : directoryStream) {
					Path d2 = to.resolve(path.getFileName());
					Files.move(path, d2, StandardCopyOption.REPLACE_EXISTING);
				}
				Files.delete(from);
			} catch (IOException ex) {
				Log.log(ex.getMessage());
				return false;
			}
			return true;
		}
	};

	/**
	 * This method can be overridden to provide a custom creation of the
	 * project.
	 *
	 * It should create the project, configure the folders in the pilarpath
	 * (source folders and external folders if applicable), set the project type
	 * and project interpreter.
	 */
	protected void createAndConfigProject(final IProject newProjectHandle,
			final IProjectDescription description, final String projectType,
			final IPath importedFilePath, IProgressMonitor monitor,
			Object... additionalArgsToConfigProject) throws CoreException {
		ICallback2<Boolean, IPath, IPath> getCreateProjectViaAPKHandlesCallback = this.getCreateProjectViaAPKHandlesCallback;
		AmanIDEStructureConfigHelpers.createAmandroidProject(description,
				newProjectHandle, monitor, projectType, importedFilePath,
				getCreateProjectViaAPKHandlesCallback);
	}

	/**
	 * The user clicked Finish button
	 *
	 * Launches another thread to create Amandroid project. A progress monitor
	 * is shown in the UI thread.
	 */
	@Override
	public boolean performFinish() {
		createdProject = createNewProject();

		IWorkingSet[] workingSets = projectPage.getWorkingSets();
		if (workingSets.length > 0) {
			PlatformUI.getWorkbench().getWorkingSetManager()
					.addToWorkingSets(createdProject, workingSets);

			// Workaround to properly show project in Package Explorer: if Top
			// Level Elements are
			// working sets, and the destination working set of the new project
			// is selected, that set
			// must be reselected in order to display the project.
			AmanIDEPackageExplorer pView = (AmanIDEPackageExplorer) PlatformUI
					.getWorkbench().getActiveWorkbenchWindow().getActivePage()
					.findView("amanide.navigator.view");
			if (pView != null) {
				IWorkingSet[] inputSets = ((AmanIDECommonViewer) pView
						.getCommonViewer()).getSelectedWorkingSets();
				if (inputSets != null && inputSets.length == 1) {
					IWorkingSet inputSet = inputSets[0];
					if (inputSet != null) {
						for (IWorkingSet destinationSet : workingSets) {
							if (inputSet.equals(destinationSet)) {
								pView.getCommonViewer().setInput(inputSet);
								break;
							}
						}
					}
				}
			}
		}

		// Switch to default perspective (will ask before changing)
		BasicNewProjectResourceWizard.updatePerspective(fConfigElement);
		BasicNewResourceWizard.selectAndReveal(createdProject,
				workbench.getActiveWorkbenchWindow());

		return true;
	}

	public IProject getCreatedProject() {
		return createdProject;
	}

	/**
	 * Set Amandroid logo to top bar
	 */
	protected void initializeDefaultPageImageDescriptor() {
		ImageDescriptor desc = AmanIDEPlugin.imageDescriptorFromPlugin(
				AmanIDEPlugin.getPluginID(), "icons/amandroid_logo.png");//$NON-NLS-1$
		setDefaultPageImageDescriptor(desc);
	}

	@Override
	public void setInitializationData(IConfigurationElement config,
			String propertyName, Object data) throws CoreException {
		this.fConfigElement = config;
	}
}
