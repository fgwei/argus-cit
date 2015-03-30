package amanide;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import amanide.cache.ImageCache;
import amanide.ui.bundle.BundleInfo;
import amanide.ui.bundle.IBundleInfo;
import amanide.utils.FileUtils;
import amanide.utils.Log;

/**
 * The activator class controls the plug-in life cycle
 */
public class AmanIDEPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "IAmandroid"; //$NON-NLS-1$

	public static final String DEFAULT_AMANDROID_SCOPE = PLUGIN_ID;
	
	// The shared instance
	private static AmanIDEPlugin plugin;
	
	/**
	 * The constructor
	 */
	public AmanIDEPlugin() {
	}
	
	public static String getVersion() {
	    try {
	        return Platform.getBundle(PLUGIN_ID).getHeaders().get("Bundle-Version");
	    } catch (Exception e) {
	        Log.log(e);
	        return "Unknown";
	    }
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static AmanIDEPlugin getDefault() {
		return plugin;
	}
	
	public static String getPluginID() {
    return getDefault().getBundle().getSymbolicName();
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
	
	private static ImageCache imageCache = null;
	
	/**
   * @return the cache that should be used to access images within the pydev plugin.
   */
  public static ImageCache getImageCache() {
      if (imageCache == null) {
          imageCache = AmanIDEPlugin.getBundleInfo().getImageCache();
      }
      return imageCache;
  }
  
  public static IBundleInfo info;

  public static IBundleInfo getBundleInfo() {
      if (AmanIDEPlugin.info == null) {
      	AmanIDEPlugin.info = new BundleInfo(AmanIDEPlugin.getDefault().getBundle());
      }
      return AmanIDEPlugin.info;
  }

  public static void setBundleInfo(IBundleInfo b) {
  	AmanIDEPlugin.info = b;
  }
	
	/**
   * Given a resource get the string in the filesystem for it.
   */
  public static String getIResourceOSString(IResource f) {
      IPath rawLocation = f.getRawLocation();
      if (rawLocation == null) {
          return null; //yes, we could have a resource that was deleted but we still have it's representation...
      }
      String fullPath = rawLocation.toOSString();
      //now, we have to make sure it is canonical...
      File file = new File(fullPath);
      if (file.exists()) {
          return FileUtils.getFileAbsolutePath(file);
      } else {
          //it does not exist, so, we have to check its project to validate the part that we can
          IProject project = f.getProject();
          IPath location = project.getLocation();
          File projectFile = location.toFile();
          if (projectFile.exists()) {
              String projectFilePath = FileUtils.getFileAbsolutePath(projectFile);

              if (fullPath.startsWith(projectFilePath)) {
                  //the case is all ok
                  return fullPath;
              } else {
                  //the case appears to be different, so, let's check if this is it...
                  if (fullPath.toLowerCase().startsWith(projectFilePath.toLowerCase())) {
                      String relativePart = fullPath.substring(projectFilePath.length());

                      //at least the first part was correct
                      return projectFilePath + relativePart;
                  }
              }
          }
      }

      //it may not be correct, but it was the best we could do...
      return fullPath;
  }
}
