package argus.tools.eclipse.contribution.weaving.jdt;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class ArgusJDTWeavingPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String ID = "org.argus-ide.cit.aspects"; //$NON-NLS-1$
	
	private static ArgusJDTWeavingPlugin INSTANCE;
	  	  
	public ArgusJDTWeavingPlugin() {
		super();
		INSTANCE = this;
	}
	
		  
	public static void logException(Throwable t) {
		INSTANCE.getLog().log(new Status(IStatus.ERROR, ID, t.getMessage(), t));
	}
		  
	public static void logErrorMessage(String msg) {
		INSTANCE.getLog().log(new Status(IStatus.ERROR, ID, msg));
	}
		  
		  
	public static ArgusJDTWeavingPlugin getInstance() {
		return INSTANCE;
	}
		  
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}
}
