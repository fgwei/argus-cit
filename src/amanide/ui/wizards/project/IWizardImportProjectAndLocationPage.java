package amanide.ui.wizards.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.IWorkingSet;

import amanide.natures.IPilarNature;

/**
 * The first page in the New Project wizard must implement this interface.
 * 
 * @author <a href="mailto:wfg611004900521@gmail.com">Fengguo Wei</a>
 */
public interface IWizardImportProjectAndLocationPage extends IWizardPage {

	public static final String AMANDROID_IMPORT_PROJECT_CREATE_PREFERENCES = "AMANDROID_IMPORT_PROJECT_CREATE_PREFERENCES";

	/**
	 * @return a string as specified in the constants in IPilarNature
	 * @see IPilarNature#PILAR_VERSION_XXX
	 */
	public String getProjectType();

	/**
	 * Returns a handle to the new project.
	 */
	public IProject getProjectHandle();

	/**
	 * Gets the imported file path for the new project.
	 */
	public IPath getImportedFilePath();

	/**
	 * Gets the location path for the new project.
	 */
	public IPath getLocationPath();

	/**
	 * @return "Default" to mean that the default interpreter should be used or
	 *         the complete path to an interpreter configured.
	 * 
	 *         Note that this changes from the python nature, where only the
	 *         path is returned (because at this point, we want to give the user
	 *         a visual indication that it's the Default interpreter if that's
	 *         the one selected)
	 */
	// public String getProjectInterpreter();

	/**
	 * Returns the working sets to which the new project should be added.
	 *
	 * @return the selected working sets to which the new project should be
	 *         added
	 */
	public IWorkingSet[] getWorkingSets();

}
