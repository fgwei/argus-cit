package amanide.editors.codecompletion;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.ide.IDE;

import amanide.AmanIDEStructureConfigHelpers;
import amanide.core.ExtensionHelper;
import amanide.core.ModulesKey;
import amanide.editors.PilarEditor;
import amanide.natures.IPilarPathHelper;
import amanide.natures.IPilarPathNature;
import amanide.natures.PilarNature;
import amanide.preferences.FileTypesPreferencesPage;
import amanide.utils.FastStringBuffer;
import amanide.utils.FileUtils;
import amanide.utils.FullRepIterable;
import amanide.utils.Log;
import amanide.utils.OrderedMap;
import amanide.utils.PilarFileListing;
import amanide.utils.PilarFileListing.PilarFileInfo;
import amanide.utils.StringUtils;

/**
 * This is not a singleton because we may have a different pilarpath for each
 * project (even though we have a default one as the original pilarpath).
 *
 * @author Fengguo Wei
 */
public final class PilarPathHelper implements IPilarPathHelper {

	/**
	 * This is a list of Files containing the pilarpath. It's always an
	 * immutable list. The instance must be changed to change the pilarpath.
	 */
	private volatile List<String> pilarpath = Collections
			.unmodifiableList(new ArrayList<String>());
	private List<IPath> searchPaths = Collections
			.unmodifiableList(new ArrayList<IPath>());

	/**
	 * The array of module resolvers from all
	 * org.pilar.pilardev.pilardev_pilar_module_resolver extensions. Initialized
	 * lazily by {@link getPilarModuleResolvers}.
	 */
	private transient IPilarModuleResolver[] pilarModuleResolvers;
	private final Object pilarModuleResolversLock = new Object();

	/**
	 * Returns the default path given from the string.
	 * 
	 * @param str
	 * @return a trimmed string with all the '\' converted to '/'
	 */
	public static String getDefaultPathStr(String str) {
		// this check is no longer done... could result in other problems
		// if(acceptPoint == false && str.indexOf(".") == 0){ //cannot start
		// with a dot
		// throw new
		// RuntimeException("The pilarpath can only have absolute paths (cannot start with '.', therefore, the path: '"+str+"' is not valid.");
		// }
		return StringUtils.replaceAllSlashes(str.trim());
	}

	public PilarPathHelper() {
	}

	/**
	 * This method returns all modules that can be obtained from a root File.
	 * 
	 * @param monitor
	 *            keep track of progress (and cancel)
	 * @return the listing with valid module files considering that root is a
	 *         root path in the pilarpath. May return null if the passed file
	 *         does not exist or is not a directory (e.g.: zip file)
	 */
	public static PilarFileListing getModulesBelow(File root,
			IProgressMonitor monitor) {
		if (!root.exists()) {
			return null;
		}

		if (root.isDirectory()) {
			FileFilter filter = new FileFilter() {

				@Override
				public boolean accept(File pathname) {
					if (pathname.isFile()) {
						return isValidFileMod(FileUtils
								.getFileAbsolutePath(pathname));
					} else {
						return false;
					}
				}

			};
			return PilarFileListing.getPilarFilesBelow(root, filter, monitor);

		}
		return null;
	}

	/**
	 * @return if the path passed belongs to a valid pilar source file (checks
	 *         for the extension)
	 */
	public static boolean isValidSourceFile(String path) {
		return isValidSourceFile(path,
				FileTypesPreferencesPage.getDottedValidSourceFiles());
	}

	public static boolean isValidSourceFile(String path,
			String[] dottedValidSourceFiles) {
		int len = dottedValidSourceFiles.length;
		for (int i = 0; i < len; i++) {
			if (path.endsWith(dottedValidSourceFiles[i])) {
				return true;
			}
		}
		if (path.endsWith(".pilarpredef")) {
			return true;
		}
		return false;
	}

	/**
	 * @return whether an IFile is a valid source file given its extension
	 */
	public static boolean isValidSourceFile(IFile file) {
		String ext = file.getFileExtension();
		if (ext == null) { // no extension
			return false;
		}
		ext = ext.toLowerCase();
		String[] validSourceFiles = FileTypesPreferencesPage
				.getValidSourceFiles();
		int len = validSourceFiles.length;
		for (int i = 0; i < len; i++) {
			String end = validSourceFiles[i];
			if (ext.equals(end)) {
				return true;
			}
		}
		if (ext.equals(".pilarpredef")) {
			return true;
		}
		return false;
	}

	/**
	 * @return if the paths maps to a valid pilar module (depending on its
	 *         extension).
	 */
	public static boolean isValidFileMod(String path) {

		boolean ret = false;
		if (isValidSourceFile(path)) {
			ret = true;
		}

		return ret;
	}

	/**
	 * Resolves an absolute file system location of a module to its name, scoped
	 * to the paths in {@link #getPilarpath()}.
	 *
	 * @param absoluteModuleLocation
	 *            the location of the module. Only for directories, or .pilar,
	 *            .pilard, .dll, .so, .pilaro files.
	 * @return a dot-separated qualified name of the Pilar module that the file
	 *         or folder should represent. E.g.: {@code compiler.ast}.
	 */
	@Override
	public String resolveModule(String absoluteModuleLocation, IProject project) {
		return resolveModule(absoluteModuleLocation, false, getPilarpath(),
				project);
	}

	/**
	 * Resolves an absolute file system location of a a module to its name,
	 * scoped to the paths in {@link #getPilarpath()} and in context to a given
	 * project.
	 *
	 * @param absoluteModuleLocation
	 *            the location of the module. Only for directories, or .pilar,
	 *            .plr files.
	 * @param requireFileToExist
	 *            if {@code true}, requires the path to exist on the filesystem.
	 * @param project
	 *            the project context in which the module resolution is being
	 *            performed. If resolution is to be done without respect to a
	 *            project, then {@code null}.
	 * @return a dot-separated qualified name of the Pilar module that the file
	 *         or folder should represent. E.g.: {@code compiler.ast}.
	 */
	public String resolveModule(String absoluteModuleLocation,
			final boolean requireFileToExist, IProject project) {
		return resolveModule(absoluteModuleLocation, requireFileToExist,
				getPilarpath(), project);
	}

	/**
	 * Resolves an absolute file system location of a module to its name, scoped
	 * to the paths in the search locations and in context to a given project.
	 *
	 * @param absoluteModuleLocation
	 *            the location of the module. Only for directories, or .pilar,
	 *            .plr files.
	 * @param requireFileToExist
	 *            if {@code true}, requires the path to exist on the filesystem.
	 * @param baseLocations
	 *            the locations relative to which to resolve the Pilar module.
	 * @param project
	 *            the project context in which the module resolution is being
	 *            performed. Can be {@code null} if resolution should to be done
	 *            without respect to a project.
	 * @return a dot-separated qualified name of the Pilar module that the file
	 *         or folder should represent. E.g.: {@code compiler.ast}.
	 */
	public String resolveModule(String absoluteModuleLocation,
			final boolean requireFileToExist, List<String> baseLocations,
			IProject project) {
		IPath modulePath = Path.fromOSString(absoluteModuleLocation);

		if (requireFileToExist && !modulePath.toFile().exists()) {
			return null;
		}

		// Try to consult each of the resolvers:
		IPilarModuleResolver[] pilarModuleResolvers = getPilarModuleResolvers();
		if (pilarModuleResolvers.length > 0) {
			List<IPath> convertedBasePaths = new ArrayList<>();
			for (String searchPath : baseLocations) {
				convertedBasePaths.add(Path.fromOSString(searchPath));
			}

			for (IPilarModuleResolver resolver : pilarModuleResolvers) {
				String resolved = resolver.resolveModule(project, modulePath,
						convertedBasePaths);
				if (resolved == null) {
					// The null string represents delegation to the next
					// resolver.
					continue;
				}
				if (resolved.isEmpty()) {
					// The empty string represents resolution failure.
					return null;
				}
				return resolved;
			}
		}

		// If all of the resolvers have delegated, then go forward with the
		// default behavior.
		absoluteModuleLocation = FileUtils
				.getFileAbsolutePath(absoluteModuleLocation);
		absoluteModuleLocation = getDefaultPathStr(absoluteModuleLocation);
		String fullPathWithoutExtension;

		if (isValidSourceFile(absoluteModuleLocation)) {
			fullPathWithoutExtension = FullRepIterable
					.headAndTail(absoluteModuleLocation)[0];
		} else {
			fullPathWithoutExtension = absoluteModuleLocation;
		}

		final File moduleFile = new File(absoluteModuleLocation);
		boolean isFile = moduleFile.isFile();

		// go through our pilarpath and check the beginning
		for (String pathEntry : baseLocations) {

			String element = getDefaultPathStr(pathEntry);
			if (absoluteModuleLocation.startsWith(element)) {
				int len = element.length();
				String s = absoluteModuleLocation.substring(len);
				String sWithoutExtension = fullPathWithoutExtension
						.substring(len);

				if (s.startsWith("/")) {
					s = s.substring(1);
				}
				if (sWithoutExtension.startsWith("/")) {
					sWithoutExtension = sWithoutExtension.substring(1);
				}

				if (!isValidModuleLastPart(sWithoutExtension)) {
					continue;
				}

				s = s.replaceAll("/", ".");
				if (s.indexOf(".") != -1) {
					File root = new File(element);
					if (root.exists() == false) {
						continue;
					}

					final List<String> temp = StringUtils.dotSplit(s);
					String[] modulesParts = temp
							.toArray(new String[temp.size()]);

					// this means that more than 1 module is specified, so, in
					// order to get it,
					// we have to go and see if all the folders to that module
					// have __init__.pilar in it...
					if (modulesParts.length > 1 && isFile) {
						String[] t = new String[modulesParts.length - 1];

						for (int i = 0; i < modulesParts.length - 1; i++) {
							t[i] = modulesParts[i];
						}
						t[t.length - 1] = t[t.length - 1] + "."
								+ modulesParts[modulesParts.length - 1];
						modulesParts = t;
					}

					// here, in modulesParts, we have something like
					// ["compiler", "ast.pilar"] - if file
					// ["pilarwin","debugger"] - if folder
					//
					// root starts with the pilarpath folder that starts with
					// the same
					// chars as the full path passed in.
					boolean isValid = true;
					for (int i = 0; i < modulesParts.length && root != null; i++) {
						root = new File(FileUtils.getFileAbsolutePath(root)
								+ "/" + modulesParts[i]);

						// check if file is in root...
						if (isValidFileMod(modulesParts[i])) {
							if (root.exists() && root.isFile()) {
								break;
							}

						}
					}
					if (isValid) {
						if (isFile) {
							s = stripExtension(s);
						} else if (moduleFile.exists() == false) {
							// ok, it does not exist, so isFile will not work,
							// let's just check if it is
							// a valid module (ends with .pilar or .plr) and
							// if it
							// is, strip the extension
							if (isValidFileMod(s)) {
								s = stripExtension(s);
							}
						}
						return s;
					}
				} else {
					return s;
				}
			}

		}
		// ok, it was not found in any existing way, so, if we don't require the
		// file to exist, let's just do some simpler search and get the
		// first match (if any)... this is useful if the file we are looking for
		// has just been deleted
		if (!requireFileToExist) {
			// we have to remove the last part (.pilar, .plr)
			for (String element : baseLocations) {
				element = getDefaultPathStr(element);
				if (fullPathWithoutExtension.startsWith(element)) {
					String s = fullPathWithoutExtension.substring(element
							.length());
					if (s.startsWith("/")) {
						s = s.substring(1);
					}
					if (!isValidModuleLastPart(s)) {
						continue;
					}
					s = s.replaceAll("/", ".");
					return s;
				}
			}
		}
		return null;
	}

	/**
	 * Note that this function is not completely safe...beware when using it.
	 * 
	 * @param s
	 * @return
	 */
	public static String stripExtension(String s) {
		if (s != null) {
			return StringUtils.stripExtension(s);
		}
		return null;
	}

	/**
	 * @param s
	 * @return
	 */
	public static boolean isValidModuleLastPart(String s) {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '-' || c == ' ' || c == '.' || c == '+') {
				return false;
			}
		}
		return true;
	}

	public void setPilarPath(List<String> newPilarpath) {
		this.pilarpath = Collections.unmodifiableList(new ArrayList<String>(
				newPilarpath));
		this.fixSearchPaths();
	}

	/**
	 * Sets searchPaths (a list of the pilarpath search directories as
	 * {@link IPath}s).
	 */
	private void fixSearchPaths() {
		List<String> pathStrings = getPilarpath();
		ArrayList<IPath> searchPaths = new ArrayList<>(pathStrings.size());
		for (String searchPath : pathStrings) {
			searchPaths.add(Path.fromOSString(searchPath));
		}
		this.searchPaths = Collections.unmodifiableList(searchPaths);
	}

	/**
	 * @param string
	 *            with paths separated by |
	 * @return
	 */
	@Override
	public void setPilarPath(String string) {
		setPilarPath(parsePilarPathFromStr(string, new ArrayList<String>()));
	}

	/**
	 * @param string
	 *            this is the string that has the pilarpath (separated by |)
	 * @param lPath
	 *            OUT: this list is filled with the pilarpath (if null an
	 *            ArrayList is created to fill the pilarpath).
	 * @return
	 */
	public static List<String> parsePilarPathFromStr(String string,
			List<String> lPath) {
		if (lPath == null) {
			lPath = new ArrayList<String>();
		}
		String[] strings = string.split("\\|");
		for (int i = 0; i < strings.length; i++) {
			String defaultPathStr = getDefaultPathStr(strings[i]);
			if (defaultPathStr != null && defaultPathStr.trim().length() > 0) {
				File file = new File(defaultPathStr);
				if (file.exists()) {
					// we have to get it with the appropriate cases and in a
					// canonical form
					String path = FileUtils.getFileAbsolutePath(file);
					lPath.add(path);
				} else {
					lPath.add(defaultPathStr);
				}
			}
		}
		return lPath;
	}

	/**
	 * @return a list with the pilarpath, such that each element of the list is
	 *         a part of the pilarpath
	 * @note returns a list that's not modifiable!
	 */
	@Override
	public List<String> getPilarpath() {
		return pilarpath;
	}

	/**
	 * Collects the Pilar modules.
	 */
	@Override
	public ModulesFoundStructure getModulesFoundStructure(
			IProgressMonitor monitor) {
		return getModulesFoundStructure(null, monitor);
	}

	/**
	 * Collects the Pilar modules.
	 * <p>
	 * Plugins that extend the
	 * {@code org.pilar.pilardev.pilardev_pilar_module_resolver} extension point
	 * can extend the behavior of this method. If no such extension exists, the
	 * default behavior is to recursively traverse the directories in the
	 * PILARPATH.
	 *
	 * @param project
	 *            the project scope, can be {@code null} to represent a
	 *            system-wide collection.
	 * @param monitor
	 *            a project monitor, can be {@code null}.
	 * @return a {@link ModulesFoundStructure} containing the encountered
	 *         modules.
	 */
	public ModulesFoundStructure getModulesFoundStructure(IProject project,
			IProgressMonitor monitor) {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		IPilarModuleResolver[] pilarModuleResolvers = getPilarModuleResolvers();
		if (pilarModuleResolvers.length > 0) {
			List<IPath> searchPaths = this.searchPaths;
			for (IPilarModuleResolver finder : pilarModuleResolvers) {
				Collection<IPath> modulesAndZips = finder.findAllModules(
						project, monitor);
				if (modulesAndZips == null) {
					continue;
				}
				ModulesFoundStructure modulesFoundStructure = new ModulesFoundStructure();
				for (IPath moduleOrZip : modulesAndZips) {
					File moduleOrZipFile = moduleOrZip.toFile();
					String qualifiedName = finder.resolveModule(project,
							moduleOrZip, searchPaths);
					if (qualifiedName != null && !qualifiedName.isEmpty()) {
						modulesFoundStructure.regularModules.put(
								moduleOrZipFile, qualifiedName);
					}
				}
				return modulesFoundStructure;
			}
		}

		// The default behavior is to recursively traverse the directories in
		// the PILARPATH to
		// collect all encountered Pilar modules.
		ModulesFoundStructure ret = new ModulesFoundStructure();

		List<String> pilarpathList = getPilarpath();
		FastStringBuffer tempBuf = new FastStringBuffer();
		for (Iterator<String> iter = pilarpathList.iterator(); iter.hasNext();) {
			String element = iter.next();

			if (monitor.isCanceled()) {
				break;
			}

			// the slow part is getting the files... not much we can do (I
			// think).
			File root = new File(element);
			PilarFileListing below = getModulesBelow(root, monitor);
			if (below != null) {

				Iterator<PilarFileInfo> e1 = below.getFoundPilarFileInfos()
						.iterator();
				while (e1.hasNext()) {
					PilarFileInfo pilarFileInfo = e1.next();
					File file = pilarFileInfo.getFile();
					String modName = pilarFileInfo.getModuleName(tempBuf);
					if (isValidModuleLastPart(FullRepIterable
							.getLastPart(modName))) {
						ret.regularModules.put(file, modName);
					}
				}

			}
		}
		return ret;
	}

	/**
	 * @param workspaceMetadataFile
	 * @throws IOException
	 */
	public void loadFromFile(File pilarpatHelperFile) throws IOException {
		String fileContents = FileUtils.getFileContents(pilarpatHelperFile);
		if (fileContents == null || fileContents.trim().length() == 0) {
			throw new IOException("No loaded contents from: "
					+ pilarpatHelperFile);
		}
		setPilarPath(StringUtils.split(fileContents, '\n'));
	}

	/**
	 * @param pilarpatHelperFile
	 */
	public void saveToFile(File pilarpatHelperFile) {
		FileUtils.writeStrToFile(StringUtils.join("\n", this.pilarpath),
				pilarpatHelperFile);
	}

	public static boolean canAddAstInfoForSourceModule(ModulesKey key) {
		if (key.file != null && key.file.exists()) {

			if (PilarPathHelper.isValidSourceFile(key.file.getName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return true if PilarEdit.EDITOR_ID is set as the persistent property
	 *         (only if the file does not have an extension).
	 */
	public static boolean markAsAmanIDEFileIfDetected(IFile file) {
		String name = file.getName();
		if (name == null || name.indexOf('.') != -1) {
			return false;
		}

		String editorID;
		try {
			editorID = file.getPersistentProperty(IDE.EDITOR_KEY);
			return PilarEditor.EDITOR_ID.equals(editorID);
		} catch (Exception e) {
			if (file.exists()) {
				Log.log(e);
			}
		}
		return false;
	}

	public static final int OPERATION_MOVE = 1;
	public static final int OPERATION_COPY = 2;
	public static final int OPERATION_DELETE = 3;

	private static OrderedMap<String, String> getResourcePilarPathMap(
			Map<IProject, OrderedMap<String, String>> projectSourcePathMapsCache,
			IResource resource) {
		IProject project = resource.getProject();
		OrderedMap<String, String> sourceMap = projectSourcePathMapsCache
				.get(project);
		if (sourceMap == null) {
			IPilarPathNature pilarPathNature = PilarNature
					.getPilarPathNature(project);
			// Ignore resources that come from a non-Pilar project.
			if (pilarPathNature == null) {
				sourceMap = new OrderedMap<String, String>();
			} else {
				try {
					sourceMap = pilarPathNature
							.getProjectSourcePathResolvedToUnresolvedMap();
				} catch (CoreException e) {
					sourceMap = new OrderedMap<String, String>();
					Log.log(e);
				}
			}
			projectSourcePathMapsCache.put(project, sourceMap);
		}
		return sourceMap;
	}

	/**
	 * Helper to update the pilarpath when a copy, move or delete operation is
	 * done which could affect a source folder (so, should work when
	 * moving/copying/deleting the parent folder of a source folder for
	 * instance).
	 *
	 * Note that the destination may be null in a delete operation.
	 */
	public static void updatePilarPath(IResource[] copiedResources,
			IContainer destination, int operation) {
		try {

			Map<IProject, OrderedMap<String, String>> projectSourcePathMapsCache = new HashMap<IProject, OrderedMap<String, String>>();
			List<String> addToDestProjects = new ArrayList<String>();

			// Step 1: remove source folders from the copied projects
			HashSet<IProject> changed = new HashSet<IProject>();
			for (IResource resource : copiedResources) {
				if (!(resource instanceof IFolder)) {
					continue;
				}
				OrderedMap<String, String> sourceMap = PilarPathHelper
						.getResourcePilarPathMap(projectSourcePathMapsCache,
								resource);

				Set<String> keySet = sourceMap.keySet();
				for (Iterator<String> it = keySet.iterator(); it.hasNext();) {
					String next = it.next();
					IPath existingInPath = Path.fromPortableString(next);
					if (resource.getFullPath().isPrefixOf(existingInPath)) {
						if (operation == PilarPathHelper.OPERATION_MOVE
								|| operation == PilarPathHelper.OPERATION_DELETE) {
							it.remove(); // Remove from that project (but not on
											// copilar)
							changed.add(resource.getProject());
						}

						if (operation == PilarPathHelper.OPERATION_COPY
								|| operation == PilarPathHelper.OPERATION_MOVE) {
							// Add to new project (but not on delete)
							String addToNewProjectPath = destination
									.getFullPath()
									.append(existingInPath
											.removeFirstSegments(resource
													.getFullPath()
													.segmentCount() - 1))
									.toPortableString();
							addToDestProjects.add(addToNewProjectPath);
						}
					}
				}
			}

			if (operation != PilarPathHelper.OPERATION_DELETE) {
				// Step 2: add source folders to the project it was copied to
				OrderedMap<String, String> destSourceMap = PilarPathHelper
						.getResourcePilarPathMap(projectSourcePathMapsCache,
								destination);
				// Get the PILARPATH of the destination project. It may be
				// modified to include the pasted resources.
				IProject destProject = destination.getProject();

				for (String addToNewProjectPath : addToDestProjects) {
					String destActualPath = AmanIDEStructureConfigHelpers
							.convertToProjectRelativePath(destProject
									.getFullPath().toPortableString(),
									addToNewProjectPath);
					destSourceMap.put(addToNewProjectPath, destActualPath);
					changed.add(destProject);
				}
			}

			// Step 3: update the target project
			for (IProject project : changed) {
				OrderedMap<String, String> sourceMap = PilarPathHelper
						.getResourcePilarPathMap(projectSourcePathMapsCache,
								project);
				PilarNature nature = PilarNature.getPilarNature(project);
				if (nature == null) {
					continue; // don't change non-pilardev projects
				}
				nature.getPilarPathNature().setProjectSourcePath(
						StringUtils.join("|", sourceMap.values()));
				nature.rebuildPath();
			}

		} catch (Exception e) {
			Log.log(IStatus.ERROR,
					"Unexpected error setting project properties", e);
		}
	}

	/**
	 * Returns an array of resolvers. If none can be found, returns an empty
	 * array.
	 */
	private IPilarModuleResolver[] getPilarModuleResolvers() {
		// Make common case faster (unsynched).
		if (pilarModuleResolvers != null) {
			return pilarModuleResolvers;
		}

		synchronized (pilarModuleResolversLock) {
			// Who knows if it was changed in the meanwhile?
			if (pilarModuleResolvers != null) {
				return pilarModuleResolvers;
			}

			ArrayList<IPilarModuleResolver> tempPilarModuleResolvers = new ArrayList<>();
			@SuppressWarnings("unchecked")
			List<Object> resolvers = ExtensionHelper
					.getParticipants(ExtensionHelper.AMANIDE_PILAR_MODULE_RESOLVER);
			for (Object resolver : resolvers) {
				if (resolver instanceof IPilarModuleResolver) {
					tempPilarModuleResolvers
							.add((IPilarModuleResolver) resolver);
				}
			}
			pilarModuleResolvers = tempPilarModuleResolvers
					.toArray(new IPilarModuleResolver[0]);
			return pilarModuleResolvers;
		}
	}
}
