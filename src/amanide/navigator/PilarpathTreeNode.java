package amanide.navigator;

import java.io.File;
import java.net.URI;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.graphics.Image;

import amanide.AmanIDEPlugin;
import amanide.cache.ImageCache;
import amanide.editors.codecompletion.PilarPathHelper;
import amanide.navigator.elements.ISortedElement;
import amanide.structure.TreeNode;
import amanide.ui.UIConstants;

/**
 * It sets packages with a package icon and pilar files with a pilar icon (other
 * files/folders have default icons)
 */
public class PilarpathTreeNode extends TreeNode<LabelAndImage> implements
		ISortedElement, IAdaptable {

	private static final File[] EMPTY_FILES = new File[0];

	/**
	 * The file/folder we're wrapping here.
	 */
	public final File file;

	/**
	 * Identifies whether we already calculated the children
	 */
	private boolean calculated = false;

	/**
	 * Is this a file for a directory?
	 */
	private boolean isDir;

	/**
	 * Is it added as a package if a directory?
	 */
	private boolean isPackage;

	/**
	 * The files beneath this directory (if not a directory, it remains null)
	 */
	private File[] dirFiles;

	public PilarpathTreeNode(TreeNode<LabelAndImage> parent, File file) {
		this(parent, file, null, false);
	}

	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == URI.class) {
			return file.toURI();
		}
		return null;
	}

	public PilarpathTreeNode(TreeNode<LabelAndImage> parent, File file,
			Image icon, boolean isPilarpathRoot) {
		super(parent, null); // data will be set later
		try {
			this.file = file;
			this.isDir = file.isDirectory();
			if (isDir) {
				dirFiles = file.listFiles();
				if (dirFiles == null) {
					dirFiles = EMPTY_FILES;
				}
				// This one can only be a package if its parent is a root or if
				// it's also a package.
				if (isPilarpathRoot) {
					isPackage = true;

				} else if (parent instanceof PilarpathTreeNode
						&& ((PilarpathTreeNode) parent).isPackage) {
					isPackage = true;
				}
			}

			// Update the icon if it wasn't received.
			if (icon == null) {
				ImageCache imageCache = AmanIDEPlugin.getImageCache();
				if (isDir) {
					if (isPackage) {
						icon = imageCache.get(UIConstants.FOLDER_PACKAGE_ICON);
					} else {
						icon = imageCache.get(UIConstants.FOLDER_ICON);
					}
				} else {
					if (PilarPathHelper.isValidSourceFile(file.getName())) {
						icon = imageCache.get(UIConstants.PILAR_FILE_ICON);
					} else {
						icon = imageCache.get(UIConstants.FILE_ICON);
					}
				}
			}
		} finally {
			setData(new LabelAndImage(getLabel(file, isPilarpathRoot), icon));
		}
	}

	private static String getLabel(File file, boolean isPilarpathRoot) {
		if (isPilarpathRoot) {
			File parent2 = file.getParentFile();
			if (parent2 != null) {
				return parent2.getName() + "/" + file.getName();

			}
			return file.getName();
		} else {
			return file.getName();
		}
	}

	@Override
	public boolean hasChildren() {
		return (isDir && dirFiles != null && dirFiles.length > 0) || (!isDir);
	}

	@Override
	public int getRank() {
		return isDir ? ISortedElement.RANK_PILAR_FOLDER
				: ISortedElement.RANK_PILAR_FILE;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public synchronized List<TreeNode> getChildren() {
		if (!calculated) {
			this.calculated = true;
			if (isDir && dirFiles != null) {
				for (File file : dirFiles) {
					// just creating it will already add it to the children
					new PilarpathTreeNode(this, file);
				}
			}
		}
		return super.getChildren();
	}

}
