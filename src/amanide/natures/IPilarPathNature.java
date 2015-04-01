package amanide.natures;

import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import amanide.utils.MisconfigurationException;
import amanide.utils.OrderedMap;

/**
 * @author <a href="mailto:wfg611004900521@gmail.com">Fengguo Wei</a>
 */
public interface IPilarPathNature {

	/**
	 * Sets the project this pilar path nature is associated with
	 * 
	 * @param project
	 */
	public void setProject(IProject project, IPilarNature nature);

	/**
	 * @return the pilarpath (source and externals) as a string (paths separated
	 *         by | ), and always as complete paths in the filesystem.
	 * @throws CoreException
	 */
	public String getOnlyProjectPilarPathStr() throws CoreException;

	/**
	 * Sets the project source path (paths are relative to the project location
	 * and are separated by | ) It can contain variables to be substituted.
	 * 
	 * @param newSourcePath
	 * @throws CoreException
	 */
	public void setProjectSourcePath(String newSourcePath) throws CoreException;

	/**
	 * @param replaceVariables
	 *            if true, any variables must be substituted (note that the
	 *            return should still be always interpreted relative to the
	 *            project location)
	 * @return only the project source paths (paths are relative to the project
	 *         location and are separated by | )
	 * @throws CoreException
	 */
	public String getProjectSourcePath(boolean replaceVariables)
			throws CoreException;

	/**
	 * @param replaceVariables
	 *            if true, any variables must be substituted (note that the
	 *            return should still be always interpreted relative to the
	 *            project location)
	 * @return only the project source paths as a list of strings (paths are
	 *         relative to the project location)
	 * @throws CoreException
	 */
	public Set<String> getProjectSourcePathSet(boolean replaceVariables)
			throws CoreException;

	/**
	 * This is a method akin to getProjectSourcePathSet, but it will return an
	 * ordered map where we map the version with variables resolved to the
	 * version without variables resolved.
	 * 
	 * It should be used when doing some PYTHONPATH manipulation based on the
	 * current values, so, we can keep the values with the variables when doing
	 * some operation while being able to check for the resolved paths to check
	 * if some item should be actually added or not.
	 */
	public OrderedMap<String, String> getProjectSourcePathResolvedToUnresolvedMap()
			throws CoreException;

	/**
	 * Can be called to force the cleaning of the caches (needed when the nature
	 * is rebuilt)
	 */
	public void clearCaches();

	/**
	 * This method sets a variable substitution so that the source folders
	 * (project and external) can be set based on those variables.
	 * 
	 * E.g.: If a variable PLATFORM maps to win32, setting a source folder as
	 * /libs/${PLATFORM}/dlls, it will be resolved in the project as
	 * /libs/win32/dlls.
	 * 
	 * Another example would be creating a varible MY_APP that maps to
	 * d:\bin\my_app, so, ${MY_APP}/libs would point to d:\bin\my_app/libs
	 * 
	 * Note that this variables are set at the project level and are resolved
	 * later than at the system level, so, when performing the substitution, it
	 * should get the variables from the interpreter and override those with the
	 * project variables before actually resolving anything.
	 */
	public void setVariableSubstitution(Map<String, String> variableSubstitution)
			throws CoreException;

	/**
	 * Same as getVariableSubstitution(true);
	 */
	public Map<String, String> getVariableSubstitution() throws CoreException,
			MisconfigurationException, PilarNatureWithoutProjectException;

	/**
	 * @param addInterpreterInfoSubstitutions
	 *            if true the substitutions in the interpreter will also be
	 *            added. Otherwise, only the substitutions from this nature will
	 *            be returned.
	 */
	public Map<String, String> getVariableSubstitution(
			boolean addInterpreterInfoSubstitutions) throws CoreException,
			MisconfigurationException, PilarNatureWithoutProjectException;

	/**
	 * The nature that contains this pilarpath nature.
	 * 
	 * @return
	 */
	public IPilarNature getNature();

	/**
	 * Gets the folders files which are added to the pilarpath relative to the
	 * project..
	 */
	public Set<IResource> getProjectSourcePathFolderSet() throws CoreException;

}