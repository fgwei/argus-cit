package amanide.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;

public class BaseExtensionHelper {

	/**
	 * This should be used to add participants at test-time. It should be the
	 * name of the extension point to a list (which will be returned)
	 */
	public static Map<String, List<Object>> testingParticipants;

	private static Map<String, IExtension[]> extensionsCache = new HashMap<String, IExtension[]>();

	public static IExtension[] getExtensions(String type) {
		IExtension[] extensions = extensionsCache.get(type);
		if (extensions == null) {
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			if (registry != null) { // we may not be in eclipse env when testing
				try {
					IExtensionPoint extensionPoint = registry
							.getExtensionPoint(type);
					extensions = extensionPoint.getExtensions();
					extensionsCache.put(type, extensions);
				} catch (Exception e) {
					Log.log(IStatus.ERROR, "Error getting extension for:"
							+ type, e);
					throw new RuntimeException(e);
				}
			} else {
				extensions = new IExtension[0];
			}
		}
		return extensions;
	}

	/**
	 * @param type
	 *            the name of the extension
	 * @param allowOverride
	 *            if true, the last registered participant will be returned,
	 *            thus "overriding" any previously registered participants. If
	 *            false, an exception is thrown if more than one participant is
	 *            registered.
	 * @return the participant for the given extension type, or null if none is
	 *         registered.
	 */
	public static Object getParticipant(String type, boolean allowOverride) {
		List<Object> participants = getParticipants(type);
		if (participants.isEmpty()) {
			return null;
		}
		if (!allowOverride && participants.size() > 1) {
			// only one participant may be used for this
			throw new RuntimeException(
					"More than one participant is registered for type:" + type);
		}
		return participants.get(participants.size() - 1);
	}

	/**
	 * @param type
	 *            the extension we want to get
	 * @return a list of classes created from those extensions
	 */
	public static List getParticipants(String type) {
		List<Object> list = null;

		list = new ArrayList<Object>();
		// For each extension ...
		try {
			for (IExtension extension : getExtensions(type)) {
				IConfigurationElement[] elements = extension
						.getConfigurationElements();
				// For each member of the extension ...
				for (IConfigurationElement element : elements) {
					try {
						list.add(element.createExecutableExtension("class"));
					} catch (Throwable e) {
						Log.log(e);
					}
				}
			}
		} catch (SecurityException e) {

		}
		return list;
	}
}
