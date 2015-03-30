/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package amanide;

import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import amanide.callbacks.ICallback;
import amanide.callbacks.ICallback2;
import amanide.natures.IPilarNature;
import amanide.natures.PilarNature;

public class AmanIDEStructureConfigHelpers {

	/**
	 * Creates a project resource handle for the current project name field
	 * value.
	 * <p>
	 * This method does not create the project resource; this is the
	 * responsibility of <code>IProject::create</code> invoked by the new
	 * project resource wizard.
	 * </p>
	 *
	 * @return the new project resource handle
	 */
	public static IProject getProjectHandle(String projectName) {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
	}

	/**
	 * @see #createAmandroidProject(IProjectDescription, IProject,
	 *      IProgressMonitor, String, String, ICallback, ICallback, ICallback)
	 */
	public static void createAmandroidProject(IProjectDescription description,
			IProject projectHandle, IProgressMonitor monitor,
			String projectType, IPath importedFilePath,
			ICallback2<Boolean, IPath, IPath> getCreateProjectHandlesCallback)
			throws OperationCanceledException, CoreException {
		createAmandroidProject(description, projectHandle, monitor,
				projectType, importedFilePath, getCreateProjectHandlesCallback,
				null);
	}

	/**
	 * Creates a project resource given the project handle and description.
	 * 
	 * @param description
	 *            the project description to create a project resource for
	 * 
	 * @param projectHandle
	 *            the project handle to create a project resource for
	 * 
	 * @param monitor
	 *            the progress monitor to show visual progress with
	 * 
	 * @param projectType
	 *            one of the PILAR_VERSION_XXX or JYTHON_VERSION_XXX constants
	 *            from {@link IPilarNature}
	 * 
	 * @param projectInterpreter
	 *            This is the interpreter to be added. It's one of the
	 *            interpreters available from
	 *            IInterpreterManager.getInterpreters.
	 * 
	 * 
	 * @param getCreateProjectHandlesCallback
	 *            This is a callback that's called with the project and it
	 *            should return a list of handles with the folders that should
	 *            be created and added to the project as source folders. (if
	 *            null, no source folders should be created)
	 * 
	 *            E.g.: To create a 'src' source folder, the callback should be:
	 * 
	 *            ICallback<List<IContainer>, IProject>
	 *            getCreateProjectHandlesCallback = new
	 *            ICallback<List<IContainer>, IProject>(){
	 * 
	 *            public List<IContainer> call(IProject projectHandle) {
	 *            IContainer folder = projectHandle.getFolder("src");
	 *            List<IContainer> ret = new ArrayList<IContainer>();
	 *            ret.add(folder); return ret; } };
	 *
	 *
	 * @param getExternalSourceFolderHandlesCallback
	 *            Same as the getCreateProjectHandlesCallback, but returns a
	 *            list of Strings to the actual paths in the filesystem that
	 *            should be added as external source folders. (if null, no
	 *            external source folders should be created)
	 * 
	 * @param getExistingSourceFolderHandlesCallback
	 *            Same as the getExternalSourceFolderHandlesCallback, but the
	 *            external folders listed will be treated as source folders
	 *            rather than external libraries. No folders will be created.
	 *            (if null, no external source folders will be referenced)
	 * 
	 * @param getVariableSubstitutionCallback
	 *            Same as getCreateProjectHandlesCallback, but returns a map of
	 *            String, String, so that the keys in the map can be used to
	 *            resolve the source folders paths (project and external).
	 * 
	 * @exception CoreException
	 *                if the operation fails
	 * @exception OperationCanceledException
	 *                if the operation is canceled
	 */
	public static void createAmandroidProject(
			IProjectDescription description,
			IProject projectHandle,
			IProgressMonitor monitor,
			String projectType,
			IPath importedFilePath,
			ICallback2<Boolean, IPath, IPath> getCreateProjectHandlesCallback,
			ICallback<Map<String, String>, IProject> getVariableSubstitutionCallback)
			throws CoreException, OperationCanceledException {

		try {
			monitor.beginTask("", 2000); //$NON-NLS-1$

			projectHandle.create(description, new SubProgressMonitor(monitor,
					1000));

			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}

			projectHandle.open(IResource.BACKGROUND_REFRESH,
					new SubProgressMonitor(monitor, 1000));

			String projectPilarpath = null;
			if (getCreateProjectHandlesCallback != null) {
				IPath path = (IPath) projectHandle.getLocation().clone();
				Boolean success = getCreateProjectHandlesCallback.call(
						importedFilePath, path);

				if (success) {
					StringBuffer buf = new StringBuffer();
					String containerPath = "/${PROJECT_DIR_NAME}/classes";
					buf.append(containerPath);

					projectPilarpath = buf.toString();
				}
			}

			Map<String, String> variableSubstitution = null;
			if (getVariableSubstitutionCallback != null) {
				variableSubstitution = getVariableSubstitutionCallback
						.call(projectHandle);
			}

			// we should rebuild the path even if there's no source-folder (this
			// way we will re-create the astmanager)
			PilarNature.addNature(projectHandle, null, projectType,
					projectPilarpath, variableSubstitution);
		} finally {
			monitor.done();
		}
	}

	public static String convertToProjectRelativePath(IProject project,
			IContainer container) {
		String projectHandleName = project.getFullPath().toString();
		return convertToProjectRelativePath(projectHandleName, container
				.getFullPath().toString());
	}

	public static String convertToProjectRelativePath(String projectHandleName,
			String containerPath) {
		if (containerPath.startsWith(projectHandleName)) {
			containerPath = containerPath.substring(projectHandleName.length());
			containerPath = "/${PROJECT_DIR_NAME}" + containerPath;
		}
		return containerPath;
	}

	/**
	 * Creates a new project resource with the entered name.
	 * 
	 * @param projectName
	 *            The name of the project
	 * @param projectLocationPath
	 *            the location for the project. If null, the default location
	 *            (in the workspace) will be used to create the project.
	 * @param references
	 *            The projects that should be referenced from the newly created
	 *            project
	 * 
	 * @return the created project resource, or <code>null</code> if the project
	 *         was not created
	 * @throws CoreException
	 * @throws OperationCanceledException
	 */
	public static IProject createAmandroidProject(
			String projectName,
			IPath projectLocationPath,
			IProject[] references,

			IProgressMonitor monitor,
			String projectType,
			IPath importedFilePath,
			ICallback2<Boolean, IPath, IPath> getCreateProjectHandlesCallback,
			ICallback<Map<String, String>, IProject> getVariableSubstitutionCallback)
			throws OperationCanceledException, CoreException {

		// get a project handle
		final IProject projectHandle = getProjectHandle(projectName);

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IProjectDescription description = workspace
				.newProjectDescription(projectHandle.getName());
		description.setLocation(projectLocationPath);

		// update the referenced project if provided
		if (references != null && references.length > 0) {
			description.setReferencedProjects(references);
		}

		createAmandroidProject(description, projectHandle, monitor,
				projectType, importedFilePath, getCreateProjectHandlesCallback,
				getVariableSubstitutionCallback);
		return projectHandle;
	}
}
