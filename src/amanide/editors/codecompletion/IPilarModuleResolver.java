package amanide.editors.codecompletion;

import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Allows third-party plugins to provide an alternative method for pilar module
 * discovery and resolution.
 */
public interface IPilarModuleResolver {
	/**
	 * Resolves a module's absolute file location to its dot-separated qualified
	 * name. The file at the given location does not have to exist.
	 * <p>
	 * The returned dot-separated qualified name should be interpretable as a
	 * relative path to one of the given base locations. Implementations might
	 * deviate from this rule to simulate a Pilar import hook.
	 *
	 * @param project
	 *            the project context in which the module resolution is being
	 *            performed. Can be {@code null} if resolution should be done
	 *            for the workspace instead of a particular project.
	 * @param moduleLocation
	 *            the absolute file system location of the module. Only for
	 *            directories, or .pilar, .plr files.
	 * @param baseLocations
	 *            the locations relative to which to resolve the Pilar module.
	 * @return the qualified name of the module. e.g. {@code compiler.ast}. If
	 *         the module can not be resolved, returns an empty string. A
	 *         {@code null} returned from the method means that the module
	 *         resolution has to be delegated to the next module resolver or to
	 *         the default PyDev module resolution.
	 */
	String resolveModule(IProject project, IPath moduleLocation,
			List<IPath> baseLocations);

	/**
	 * Collects all the Pilar modules, including directories, files and zip
	 * packages, in a project or the entire workspace.
	 *
	 * @param project
	 *            the project to collect Pilar modules for, or {@code null} to
	 *            indicate an interpreter-wide collection.
	 * @param monitor
	 *            the progress monitor. Can be {@code null}.
	 * @return a collection of the found Pilar modules and zip files as absolute
	 *         file locations. Each module is expected to be resolvable with
	 *         {@link #resolveModule(IProject, IPath, List)}. A {@code null}
	 *         returned from the method means that the module discovery has to
	 *         be delegated to the next module resolver or to the default PyDev
	 *         module resolution.
	 */
	Collection<IPath> findAllModules(IProject project, IProgressMonitor monitor);
}