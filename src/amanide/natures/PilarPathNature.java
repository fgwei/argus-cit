package amanide.natures;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;

import amanide.AmanIDEPlugin;
import amanide.utils.FastStringBuffer;
import amanide.utils.FileUtils;
import amanide.utils.Log;
import amanide.utils.MisconfigurationException;
import amanide.utils.OrderedMap;
import amanide.utils.StringSubstitution;
import amanide.utils.StringUtils;

/**
 * @author <a href="mailto:fgwei@k-state.edu">Fengguo Wei</a>
 */
public class PilarPathNature implements IPilarPathNature {

	private volatile IProject fProject;
	private volatile PilarNature fNature;

	/**
	 * This is the property that has the pilar path - associated with the
	 * project.
	 */
	private static QualifiedName projectSourcePathQualifiedName = null;

	static QualifiedName getProjectSourcePathQualifiedName() {
		if (projectSourcePathQualifiedName == null) {
			projectSourcePathQualifiedName = new QualifiedName(
					AmanIDEPlugin.getPluginID(), "PROJECT_SOURCE_PATH");
		}
		return projectSourcePathQualifiedName;
	}

	/**
	 * This is the property that has the external pilar path - associated with
	 * the project.
	 */
	private static QualifiedName projectVariableSubstitutionQualifiedName = null;

	static QualifiedName getProjectVariableSubstitutionQualifiedName() {
		if (projectVariableSubstitutionQualifiedName == null) {
			projectVariableSubstitutionQualifiedName = new QualifiedName(
					AmanIDEPlugin.getPluginID(),
					"PROJECT_VARIABLE_SUBSTITUTION");
		}
		return projectVariableSubstitutionQualifiedName;
	}

	@Override
	public void setProject(IProject project, IPilarNature nature) {
		this.fProject = project;
		this.fNature = (PilarNature) nature;
	}

	@Override
	public IPilarNature getNature() {
		return this.fNature;
	}

	/**
	 * @return the project pilarpath with complete paths in the filesystem.
	 */
	@Override
	public String getOnlyProjectPilarPathStr() throws CoreException {
		String source = null;
		// String contributed = null;
		IProject project = fProject;
		PilarNature nature = fNature;

		if (project == null || nature == null) {
			return "";
		}

		// Substitute with variables!
		StringSubstitution stringSubstitution = new StringSubstitution(nature);

		source = (String) getProjectSourcePath(true, stringSubstitution,
				RETURN_STRING_WITH_SEPARATOR);

		if (source == null) {
			source = "";
		}
		// we have to work on this one to resolve to full files, as what is
		// stored is the position
		// relative to the project location
		List<String> strings = StringUtils.splitAndRemoveEmptyTrimmed(source,
				'|');
		FastStringBuffer buf = new FastStringBuffer();

		for (String currentPath : strings) {
			if (currentPath.trim().length() > 0) {
				IPath p = new Path(currentPath);

				boolean found = false;
				p = p.removeFirstSegments(1); // The first segment should always
												// be the project (historically
												// it's this way, but having it
												// relative would be nicer!?!).
				IResource r = project.findMember(p);
				if (r == null) {
					r = project.getFolder(p);
				}
				if (r != null) {
					IPath location = r.getLocation();
					if (location != null) {
						found = true;
						buf.append(FileUtils.getFileAbsolutePath(location
								.toFile()));
						buf.append("|");
					}
				}
				if (!found) {
					Log.log(IStatus.WARNING, "Unable to find the path "
							+ currentPath + " in the project were it's \n"
							+ "added as a source folder for AmanIDE (project: "
							+ project.getName() + ") member:" + r, null);
				}
			}
		}
		Log.log("getOnlyProjectPilarPathStr: " + buf.toString());
		return buf.toString();
	}

	/**
	 * Similar to the getOnlyProjectPilarPathStr method above but only for
	 * source files (not contributed nor external) and return IResources (zip
	 * files or folders).
	 */
	@Override
	public Set<IResource> getProjectSourcePathFolderSet() throws CoreException {
		String source = null;
		IProject project = fProject;
		PilarNature nature = fNature;

		Set<IResource> ret = new HashSet<>();
		if (project == null || nature == null) {
			return ret;
		}

		// Substitute with variables!
		StringSubstitution stringSubstitution = new StringSubstitution(nature);

		source = (String) getProjectSourcePath(true, stringSubstitution,
				RETURN_STRING_WITH_SEPARATOR);

		if (source == null) {
			return ret;
		}
		// we have to work on this one to resolve to full files, as what is
		// stored is the position
		// relative to the project location
		List<String> strings = StringUtils.splitAndRemoveEmptyTrimmed(source,
				'|');

		for (String currentPath : strings) {
			if (currentPath.trim().length() > 0) {
				IPath p = new Path(currentPath);
				p = p.removeFirstSegments(1);
				IResource r = project.findMember(p);
				if (r == null) {
					r = project.getFolder(p);
				}
				if (r != null && r.exists()) {
					ret.add(r);
				}
			}
		}
		return ret;
	}

	@Override
	public void setProjectSourcePath(String newSourcePath) throws CoreException {
		PilarNature nature = fNature;
		if (nature != null) {
			nature.getStore().setPathProperty(
					PilarPathNature.getProjectSourcePathQualifiedName(),
					newSourcePath);
		}
	}

	@Override
	public void setVariableSubstitution(Map<String, String> variableSubstitution)
			throws CoreException {
		PilarNature nature = fNature;
		if (nature != null) {
			nature.getStore()
					.setMapProperty(
							PilarPathNature
									.getProjectVariableSubstitutionQualifiedName(),
							variableSubstitution);
		}
	}

	@Override
	public void clearCaches() {
	}

	@Override
	public Set<String> getProjectSourcePathSet(boolean replace)
			throws CoreException {
		String projectSourcePath;
		PilarNature nature = fNature;
		if (nature == null) {
			return new HashSet<String>();
		}
		projectSourcePath = getProjectSourcePath(replace);
		Log.log("projectSourcePath:::" + projectSourcePath);
		return new HashSet<String>(StringUtils.splitAndRemoveEmptyTrimmed(
				projectSourcePath, '|'));
	}

	@Override
	public String getProjectSourcePath(boolean replace) throws CoreException {
		return (String) getProjectSourcePath(replace, null,
				RETURN_STRING_WITH_SEPARATOR);
	}

	@Override
	@SuppressWarnings("unchecked")
	public OrderedMap<String, String> getProjectSourcePathResolvedToUnresolvedMap()
			throws CoreException {
		return (OrderedMap<String, String>) getProjectSourcePath(true, null,
				RETURN_MAP_RESOLVED_TO_UNRESOLVED);
	}

	private static final int RETURN_STRING_WITH_SEPARATOR = 1;
	private static final int RETURN_MAP_RESOLVED_TO_UNRESOLVED = 2;

	/**
	 * Function which can take care of getting the paths just for the project
	 * (i.e.: without external source folders).
	 * 
	 * @param replace
	 *            used only if returnType == RETURN_STRING_WITH_SEPARATOR.
	 * 
	 * @param substitution
	 *            the object which will do the string substitutions (only
	 *            internally used as an optimization as creating the instance
	 *            may be expensive, so, if some other place already creates it,
	 *            it can be passed along).
	 * 
	 * @param returnType
	 *            if RETURN_STRING_WITH_SEPARATOR returns a string using '|' as
	 *            the separator. If RETURN_MAP_RESOLVED_TO_UNRESOLVED returns a
	 *            map which points from the paths resolved to the maps
	 *            unresolved.
	 */
	private Object getProjectSourcePath(boolean replace,
			StringSubstitution substitution, int returnType)
			throws CoreException {
		String projectSourcePath;
		boolean restore = false;
		IProject project = fProject;
		PilarNature nature = fNature;

		if (project == null || nature == null) {
			if (returnType == RETURN_STRING_WITH_SEPARATOR) {
				return "";
			} else if (returnType == RETURN_MAP_RESOLVED_TO_UNRESOLVED) {
				return new OrderedMap<String, String>();
			} else {
				throw new AssertionError("Unexpected return: " + returnType);
			}
		}
		projectSourcePath = nature.getStore().getPathProperty(
				PilarPathNature.getProjectSourcePathQualifiedName());
		if (projectSourcePath == null) {
			// has not been set
			if (returnType == RETURN_STRING_WITH_SEPARATOR) {
				return "";
			} else if (returnType == RETURN_MAP_RESOLVED_TO_UNRESOLVED) {
				return new OrderedMap<String, String>();
			} else {
				throw new AssertionError("Unexpected return: " + returnType);
			}
		}

		if (replace && substitution == null) {
			substitution = new StringSubstitution(fNature);
		}

		// we have to validate it, because as we store the values relative to
		// the workspace, and not to the
		// project, the path may become invalid (in which case we have to make
		// it compatible again).
		StringBuffer buffer = new StringBuffer();
		List<String> paths = StringUtils.splitAndRemoveEmptyTrimmed(
				projectSourcePath, '|');
		IPath projectPath = project.getFullPath();
		for (String path : paths) {
			if (path.trim().length() > 0) {
				if (path.indexOf("${") != -1) { // Account for the string
												// substitution.
					buffer.append(path);
				} else {
					IPath p = new Path(path);
					if (p.isEmpty()) {
						continue; // go to the next...
					}
					if (projectPath != null && !projectPath.isPrefixOf(p)) {
						p = p.removeFirstSegments(1);
						p = projectPath.append(p);
						restore = true;
					}
					buffer.append(p.toString());
				}
				buffer.append("|");
			}
		}

		// it was wrong and has just been fixed
		if (restore) {
			projectSourcePath = buffer.toString();
			setProjectSourcePath(projectSourcePath);
			if (nature != null) {
				// yeap, everything has to be done from scratch, as all the
				// filesystem paths have just
				// been turned to dust!
				nature.rebuildPath();
			}
		}

		if (returnType == RETURN_STRING_WITH_SEPARATOR) {
			return trimAndReplaceVariablesIfNeeded(replace, projectSourcePath,
					nature, substitution);

		} else if (returnType == RETURN_MAP_RESOLVED_TO_UNRESOLVED) {
			String ret = StringUtils.leftAndRightTrim(projectSourcePath, '|');
			OrderedMap<String, String> map = new OrderedMap<String, String>();

			List<String> unresolvedVars = StringUtils
					.splitAndRemoveEmptyTrimmed(ret, '|');

			// Always resolves here!
			List<String> resolved = StringUtils.splitAndRemoveEmptyTrimmed(
					substitution.performPilarpathStringSubstitution(ret), '|');

			int size = unresolvedVars.size();
			if (size != resolved.size()) {
				throw new AssertionError("Error: expected same size from:\n"
						+ unresolvedVars + "\nand\n" + resolved);
			}
			for (int i = 0; i < size; i++) {
				String un = unresolvedVars.get(i);
				String res = resolved.get(i);
				map.put(res, un);
			}

			return map;
		} else {
			throw new AssertionError("Unexpected return: " + returnType);
		}

	}

	/**
	 * Replaces the variables if needed.
	 */
	private String trimAndReplaceVariablesIfNeeded(boolean replace,
			String projectSourcePath, PilarNature nature,
			StringSubstitution substitution) throws CoreException {
		String ret = StringUtils.leftAndRightTrim(projectSourcePath, '|');
		if (replace) {
			ret = substitution.performPilarpathStringSubstitution(ret);
		}
		return ret;
	}

	@Override
	public Map<String, String> getVariableSubstitution() throws CoreException,
			MisconfigurationException, PilarNatureWithoutProjectException {
		return getVariableSubstitution(true);
	}

	/**
	 * Returns the variables in the pilar nature.
	 */
	@Override
	public Map<String, String> getVariableSubstitution(
			boolean addInterpreterInfoSubstitutions) throws CoreException,
			MisconfigurationException, PilarNatureWithoutProjectException {
		PilarNature nature = this.fNature;
		if (nature == null) {
			return new HashMap<String, String>();
		}

		Map<String, String> variableSubstitution;
		variableSubstitution = new HashMap<String, String>();

		// no need to validate because those are always 'file-system' related
		Map<String, String> variableSubstitution2 = nature.getStore()
				.getMapProperty(
						PilarPathNature
								.getProjectVariableSubstitutionQualifiedName());
		if (variableSubstitution2 != null) {
			if (variableSubstitution != null) {
				variableSubstitution.putAll(variableSubstitution2);
			} else {
				variableSubstitution = variableSubstitution2;
			}
		}

		// never return null!
		if (variableSubstitution == null) {
			variableSubstitution = new HashMap<String, String>();
		}
		return variableSubstitution;
	}

}
