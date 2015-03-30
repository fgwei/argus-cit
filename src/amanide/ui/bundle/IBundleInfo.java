package amanide.ui.bundle;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import amanide.cache.ImageCache;

/**
 * @author Fengguo Wei
 */
public interface IBundleInfo {

	/**
	 * Should return a file from a relative path.
	 * 
	 * @param relative
	 * @return
	 * @throws CoreException
	 */
	File getRelativePath(IPath relative) throws CoreException;

	String getPluginID();

	ImageCache getImageCache();

}
