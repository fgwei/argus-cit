package amanide.natures;

import org.eclipse.core.resources.IProject;

/**
 * Interface that should be used for clients that want to know:
 * 
 * - when the project pythonpath has been rebuilt
 * 
 * @author <a href="mailto:fgwei@k-state.edu">Fengguo Wei</a>
 */
public interface IPilarNatureListener {

	/**
	 * Notification that the pilarpath has been rebuilt.
	 * 
	 * @param project
	 *            is the project that had the pythonpath rebuilt
	 * @param nature
	 *            the project pythonpath used when rebuilding
	 *            {@link IPythonPathNature#getCompleteProjectPythonPath()}
	 */
	void notifyPilarPathRebuilt(IProject project, IPilarNature nature);

}
