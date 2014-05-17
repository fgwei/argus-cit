/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package iamandroid.ui.wizards.project;

import iamandroid.AmandroidPlugin;
import iamandroid.AmandroidStructureConfigHelpers;
import iamandroid.callbacks.ICallback;
import iamandroid.utils.Log;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
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

/**
 * Python Project creation wizard
 *
 * <ul>
 * <li>Asks users information about Python project
 * <li>Launches another thread to create Python project. A progress monitor is shown in UI thread
 * </ul>
 *
 * TODO: Add a checkbox asking should a skeleton of a Python program generated
 *
 * @author Mikko Ohtamaa
 * @author Fabio Zadrozny
 */
public class AmandroidProjectWizard extends AbstractNewProjectWizard implements IExecutableExtension {

    /**
     * The current selection.
     */
    protected IStructuredSelection selection;

    public static final String WIZARD_ID = "iamandroid.ui.wizards.project.AmandroidProjectWizard";

    protected IWizardNewProjectNameAndLocationPage projectPage;
    protected IWizardNewProjectExistingSourcesPage sourcesPage;

    Shell shell;

    /** Target project created by this wizard */
    IProject generatedProject;

    /** Exception throw by generator thread */
    Exception creationThreadException;

    private IProject createdProject;

    private IConfigurationElement fConfigElement;

    protected IWorkbench workbench;

    public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        this.selection = currentSelection;
        this.workbench = workbench;
        initializeDefaultPageImageDescriptor();
        projectPage = createProjectPage();
        sourcesPage = createSourcesPage();
//        PyDialogHelpers.enableAskInterpreterStep(false);
    }

    @Override
    public void dispose() {
        super.dispose();
//        PyDialogHelpers.enableAskInterpreterStep(true);
    }

    /**
     * Creates the project page.
     */
    protected IWizardNewProjectNameAndLocationPage createProjectPage() {
        return new NewProjectNameAndLocationWizardPage("Setting project properties");
    }

    /**
     * Creates the sources page.
     */
    protected IWizardNewProjectExistingSourcesPage createSourcesPage() {
        return new NewProjectExistingSourcesWizardPage("Setting project sources");
    }

    /**
     * Returns the sources page.
     */
    protected IWizardNewProjectExistingSourcesPage getSourcesPage() {
        return sourcesPage;
    }

    /**
     * Returns the page that should appear after the sources page, or null if no page comes after it.
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
//        addPage(sourcesPage);
        addProjectReferencePage();
    }

    /**
     * Creates a new project resource with the entered name.
     *
     * @return the created project resource, or <code>null</code> if the project was not created
     */
    protected IProject createNewProject(final Object... additionalArgsToConfigProject) {
        // get a project handle
        final IProject newProjectHandle = projectPage.getProjectHandle();

        // get a project descriptor
        IPath defaultPath = Platform.getLocation();
        IPath newPath = projectPage.getLocationPath();
        if (defaultPath.equals(newPath)) {
            newPath = null;
        } else {
            //The user entered the path and it's the same as it'd be if he chose the default path.
            IPath withName = defaultPath.append(newProjectHandle.getName());
            if (newPath.toFile().equals(withName.toFile())) {
                newPath = null;
            }
        }

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IProjectDescription description = workspace.newProjectDescription(newProjectHandle.getName());
        description.setLocation(newPath);

        // update the referenced project if provided
        if (referencePage != null) {
            IProject[] refProjects = referencePage.getReferencedProjects();
            if (refProjects.length > 0) {
                description.setReferencedProjects(refProjects);
            }
        }

        final String projectType = projectPage.getProjectType();
//        String projectInterpreter = projectPage.getProjectInterpreter();
//        if (projectInterpreter == null || projectInterpreter.trim().isEmpty()) {
//            projectInterpreter = IPythonNature.DEFAULT_INTERPRETER;
//        }
//        final String projectInterpreterFinal = projectInterpreter;

        // define the operation to create a new project
        WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
            @Override
            protected void execute(IProgressMonitor monitor) throws CoreException {

                createAndConfigProject(newProjectHandle, description, projectType, monitor,
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
                    MessageDialog.openError(getShell(), "Unable to create project",
                            "Another project with the same name (and different case) already exists.");
                } else {
                    ErrorDialog
                            .openError(getShell(), "Unable to create project", null, ((CoreException) t).getStatus());
                }
            } else {
                // Unexpected runtime exceptions and errors may still occur.
                Log.log(IStatus.ERROR, t.toString(), t);
                MessageDialog.openError(getShell(), "Unable to create project", t.getMessage());
            }
            return null;
        }

        return newProjectHandle;
    }

    protected ICallback<List<IContainer>, IProject> getSourceFolderHandlesCallback = new ICallback<List<IContainer>, IProject>() {
        public List<IContainer> call(IProject projectHandle) {
            final int sourceFolderConfigurationStyle = projectPage.getSourceFolderConfigurationStyle();
            List<IContainer> ret = new ArrayList<IContainer>();
            switch (sourceFolderConfigurationStyle) {

                case IWizardNewProjectNameAndLocationPage.AMANDROID_NEW_PROJECT_CREATE_PROJECT_AS_SRC_FOLDER:
                    //if the user hasn't selected to create a source folder, use the project itself for that.
                    ret = new ArrayList<IContainer>();
                    ret.add(projectHandle);
                    return ret;

                case IWizardNewProjectNameAndLocationPage.AMANDROID_NEW_PROJECT_EXISTING_SOURCES:
                    return new ArrayList<IContainer>();

                case IWizardNewProjectNameAndLocationPage.AMANDROID_NEW_PROJECT_NO_PATH:
                    return new ArrayList<IContainer>();

                default:
                    IContainer folder = projectHandle.getFolder("src");
                    ret = new ArrayList<IContainer>();
                    ret.add(folder);
                    return ret;
            }
        }
    };

    protected ICallback<List<IPath>, IProject> getExistingSourceFolderHandlesCallback = new ICallback<List<IPath>, IProject>() {
        public List<IPath> call(IProject projectHandle) {
            if (projectPage.getSourceFolderConfigurationStyle() == IWizardNewProjectNameAndLocationPage.AMANDROID_NEW_PROJECT_EXISTING_SOURCES) {
                List<IPath> eSources = sourcesPage.getExistingSourceFolders();
                if (eSources.size() > 0) {
                    return eSources;
                }
            }
            return null;
        }
    };

    /**
     * This method can be overridden to provide a custom creation of the project.
     *
     * It should create the project, configure the folders in the pythonpath (source folders and external folders
     * if applicable), set the project type and project interpreter.
     */
    protected void createAndConfigProject(final IProject newProjectHandle, final IProjectDescription description,
            final String projectType, IProgressMonitor monitor,
            Object... additionalArgsToConfigProject) throws CoreException {
        ICallback<List<IContainer>, IProject> getSourceFolderHandlesCallback = this.getSourceFolderHandlesCallback;
        ICallback<List<IPath>, IProject> getExistingSourceFolderHandlesCallback = this.getExistingSourceFolderHandlesCallback;

        AmandroidStructureConfigHelpers.createAmandroidProject(description, newProjectHandle, monitor, projectType,
                getSourceFolderHandlesCallback, null, getExistingSourceFolderHandlesCallback);
    }

    /**
     * The user clicked Finish button
     *
     * Launches another thread to create Amandroid project. A progress monitor is shown in the UI thread.
     */
    @Override
    public boolean performFinish() {
        createdProject = createNewProject();

        IWorkingSet[] workingSets = projectPage.getWorkingSets();
        if (workingSets.length > 0) {
            PlatformUI.getWorkbench().getWorkingSetManager().addToWorkingSets(createdProject, workingSets);

            //Workaround to properly show project in Package Explorer: if Top Level Elements are
            //working sets, and the destination working set of the new project is selected, that set
            //must be reselected in order to display the project.
//            PydevPackageExplorer pView = (PydevPackageExplorer) PlatformUI.getWorkbench()
//                    .getActiveWorkbenchWindow().getActivePage()
//                    .findView("org.python.pydev.navigator.view");
//            if (pView != null) {
//                IWorkingSet[] inputSets = ((PydevCommonViewer) pView.getCommonViewer()).getSelectedWorkingSets();
//                if (inputSets != null && inputSets.length == 1) {
//                    IWorkingSet inputSet = inputSets[0];
//                    if (inputSet != null) {
//                        for (IWorkingSet destinationSet : workingSets) {
//                            if (inputSet.equals(destinationSet)) {
//                                pView.getCommonViewer().setInput(inputSet);
//                                break;
//                            }
//                        }
//                    }
//                }
//            }
        }

        // Switch to default perspective (will ask before changing)
        BasicNewProjectResourceWizard.updatePerspective(fConfigElement);
        BasicNewResourceWizard.selectAndReveal(createdProject, workbench.getActiveWorkbenchWindow());

        return true;
    }

    public IProject getCreatedProject() {
        return createdProject;
    }

    /**
     * Set Amandroid logo to top bar
     */
    protected void initializeDefaultPageImageDescriptor() {
        ImageDescriptor desc = AmandroidPlugin
                .imageDescriptorFromPlugin(AmandroidPlugin.getPluginID(), "icons/amandroid_logo.png");//$NON-NLS-1$
        setDefaultPageImageDescriptor(desc);
    }

    public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
            throws CoreException {
        this.fConfigElement = config;
    }
}
