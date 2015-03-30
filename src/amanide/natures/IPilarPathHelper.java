package amanide.natures;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

import amanide.editors.codecompletion.ModulesFoundStructure;

/**
 * @author Fengguo Wei
 */
public interface IPilarPathHelper {

	/**
	 * Given the absolute file system location of a module, returns the
	 * qualified module name.
	 *
	 * @param absoluteModuleLocation
	 *            this is the location of the module. Only for directories, or
	 *            .pilar, .plr files.
	 * @return the dot-separated qualified name of the module that the file or
	 *         folder should represent. E.g.: compiler.ast
	 */
	public String resolveModule(String absoluteModuleLocation, IProject project);

	/**
	 * Sets the pilar path to operate on.
	 * 
	 * @param string
	 *            with paths separated by {@code |}
	 */
	public void setPilarPath(String string);

	/**
	 * Getter for Pilar path.
	 *
	 * @return list of Pilar path entries.
	 */
	public List<String> getPilarpath();

	/**
	 * This method should traverse the pilarpath passed and return a structure
	 * with the info that could be collected about the files that are related to
	 * pilar modules.
	 */
	public ModulesFoundStructure getModulesFoundStructure(
			IProgressMonitor monitor);
}
