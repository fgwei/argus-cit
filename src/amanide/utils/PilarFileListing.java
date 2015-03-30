package amanide.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import amanide.editors.codecompletion.PilarPathHelper;
import amanide.preferences.FileTypesPreferencesPage;

/**
 * Helper class for finding out about pilar files below some source folder.
 *
 * @author Fengguo Wei
 */
public class PilarFileListing {

	/**
	 * Information about a pilar file found (the actual file and the way it was
	 * resolved as a pilar module)
	 */
	public static final class PilarFileInfo {

		private final String relPath;

		private final File file;

		public PilarFileInfo(File file, String relPath) {
			this.file = file;
			this.relPath = relPath;
		}

		/** File object. */
		public File getFile() {
			return file;
		}

		/** Returns fully qualified name of the package. */
		public String getPackageName() {
			return relPath;
		}

		@Override
		public String toString() {
			return StringUtils.join("", "PilarFileInfo:", file, " - ", relPath);
		}

		/**
		 * @return the name of the module represented by this info.
		 */
		public String getModuleName(FastStringBuffer temp) {
			String scannedModuleName = this.getPackageName();

			String modName;
			String name = file.getName();
			if (scannedModuleName.length() != 0) {
				modName = temp.clear().append(scannedModuleName).append('.')
						.append(PilarPathHelper.stripExtension(name))
						.toString();
			} else {
				modName = PilarPathHelper.stripExtension(name);
			}
			return modName;
		}
	}

	/**
	 * Returns the directories and pilar files in a list.
	 *
	 * @param addSubFolders
	 *            indicates if sub-folders should be added
	 * @param canonicalFolders
	 *            used to know if we entered a loop in the listing (with
	 *            symlinks)
	 * @return An object with the results of making that listing.
	 */
	private static PilarFileListing getPilarFilesBelow(PilarFileListing result,
			File file, FileFilter filter, IProgressMonitor monitor,
			boolean addSubFolders, int level, String currModuleRep,
			Set<File> canonicalFolders) {

		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}

		if (file != null && file.exists()) {
			// only check files that actually exist

			if (file.isDirectory()) {
				FastStringBuffer buf = new FastStringBuffer(currModuleRep, 128);
				if (level != 0) {
					FastStringBuffer newModuleRep = buf;
					if (newModuleRep.length() != 0) {
						newModuleRep.append('.');
					}
					newModuleRep.append(file.getName());
					currModuleRep = newModuleRep.toString();
				}

				// check if it is a symlink loop
				try {
					File canonicalizedDir = file.getCanonicalFile();
					if (!canonicalizedDir.equals(file)) {
						if (canonicalFolders.contains(canonicalizedDir)) {
							return result;
						}
					}
					canonicalFolders.add(canonicalizedDir);
				} catch (IOException e) {
					Log.log(e);
				}

				File[] files;

				if (filter != null) {
					files = file.listFiles(filter);
				} else {
					files = file.listFiles();
				}

				List<File> foldersLater = new LinkedList<File>();

				if (files != null) {
					for (File file2 : files) {

						if (monitor.isCanceled()) {
							break;
						}

						if (file2.isFile()) {
							result.addPyFileInfo(new PilarFileInfo(file2,
									currModuleRep));

							monitor.worked(1);
							monitor.setTaskName(buf.clear().append("Found:")
									.append(file2.toString()).toString());
						} else {
							foldersLater.add(file2);
						}
					}
					if (level == 0) {
						result.foldersFound.add(file);

						for (File folder : foldersLater) {

							if (monitor.isCanceled()) {
								break;
							}

							if (folder.isDirectory() && addSubFolders) {

								getPilarFilesBelow(result, folder, filter,
										monitor, addSubFolders, level + 1,
										currModuleRep, canonicalFolders);

								monitor.worked(1);
							}
						}
					}
				}

			} else { // not dir: must be file
				result.addPyFileInfo(new PilarFileInfo(file, currModuleRep));

			}
		}

		return result;
	}

	private static PilarFileListing getPilarFilesBelow(File file,
			FileFilter filter, IProgressMonitor monitor, boolean addSubFolders) {
		PilarFileListing result = new PilarFileListing();
		return getPilarFilesBelow(result, file, filter, monitor, addSubFolders,
				0, "", new HashSet<File>());
	}

	public static PilarFileListing getPilarFilesBelow(File file,
			FileFilter filter, IProgressMonitor monitor) {
		return getPilarFilesBelow(file, filter, monitor, true);
	}

	/**
	 * @param includeDirs
	 *            determines if we can include subdirectories
	 * @return a file filter only for pilar files (and other dirs if specified)
	 */
	public static FileFilter getPyFilesFileFilter(final boolean includeDirs) {

		return new FileFilter() {

			private final String[] dottedValidSourceFiles = FileTypesPreferencesPage
					.getDottedValidSourceFiles();

			@Override
			public boolean accept(File pathname) {
				if (includeDirs) {
					if (pathname.isDirectory()) {
						return true;
					}
					if (PilarPathHelper.isValidSourceFile(pathname.toString(),
							dottedValidSourceFiles)) {
						return true;
					}
					return false;
				} else {
					if (pathname.isDirectory()) {
						return false;
					}
					if (PilarPathHelper.isValidSourceFile(pathname.toString(),
							dottedValidSourceFiles)) {
						return true;
					}
					return false;
				}
			}

		};
	}

	/**
	 * Returns the directories and pilar files in a list.
	 *
	 * @param file
	 * @return tuple with files in pos 0 and folders in pos 1
	 */
	public static PilarFileListing getPilarFilesBelow(File file,
			IProgressMonitor monitor, final boolean includeDirs) {
		FileFilter filter = getPyFilesFileFilter(includeDirs);
		return getPilarFilesBelow(file, filter, monitor, true);
	}

	/**
	 * @return All the IFiles below the current folder that are pilar files
	 *         (does not check if it has an __init__ path)
	 */
	public static List<IFile> getAllIFilesBelow(IContainer member) {
		final ArrayList<IFile> ret = new ArrayList<IFile>();
		try {
			member.accept(new IResourceVisitor() {

				@Override
				public boolean visit(IResource resource) {
					if (resource instanceof IFile) {
						ret.add((IFile) resource);
						return false; // has no members
					}
					return true;
				}

			});
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
		return ret;
	}

	/**
	 * The files we found as being valid for the given filter
	 */
	private final List<PilarFileInfo> pilarFileInfos = new ArrayList<PilarFileInfo>();

	/**
	 * The folders we found as being valid for the given filter
	 */
	private List<File> foldersFound = new ArrayList<File>();

	public PilarFileListing() {
	}

	public Collection<PilarFileInfo> getFoundPilarFileInfos() {
		return pilarFileInfos;
	}

	public Collection<File> getFoundFolders() {
		return foldersFound;
	}

	private void addPyFileInfo(PilarFileInfo info) {
		pilarFileInfos.add(info);
	}

}
