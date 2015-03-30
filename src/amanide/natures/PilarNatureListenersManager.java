package amanide.natures;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;

import amanide.utils.Log;

/**
 * This class is used to pass notifications about the pilar nature around for
 * those interested.
 * 
 * @author <a href="mailto:fgwei@k-state.edu">Fengguo Wei</a>
 */
public class PilarNatureListenersManager {
	private static List<WeakReference<IPilarNatureListener>> pilarNatureListeners = new ArrayList<WeakReference<IPilarNatureListener>>();

	public static void addPilarNatureListener(IPilarNatureListener listener) {
		pilarNatureListeners.add(new WeakReference<IPilarNatureListener>(
				listener));
	}

	public static void removePilarNatureListener(IPilarNatureListener provider) {
		for (Iterator<WeakReference<IPilarNatureListener>> it = pilarNatureListeners
				.iterator(); it.hasNext();) {
			WeakReference<IPilarNatureListener> ref = it.next();
			if (ref.get() == provider) {
				it.remove();
			}
		}
	}

	/**
	 * Notification that the pilarpath has been rebuilt.
	 * 
	 * @param project
	 *            is the project that had the pilarpath rebuilt
	 * @param nature
	 *            the nature related to the project (can be null if the nature
	 *            has actually been removed)
	 */
	public static void notifyPilarPathRebuilt(IProject project,
			IPilarNature nature) {
		for (Iterator<WeakReference<IPilarNatureListener>> it = pilarNatureListeners
				.iterator(); it.hasNext();) {
			WeakReference<IPilarNatureListener> ref = it.next();
			try {
				IPilarNatureListener listener = ref.get();
				if (listener == null) {
					it.remove();
				} else {
					listener.notifyPilarPathRebuilt(project, nature);
				}
			} catch (Throwable e) {
				Log.log(e);
			}
		}
	}

}
