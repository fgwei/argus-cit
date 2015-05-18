package argus.tools.eclipse.contribution.weaving.jdt.ui.actions;

import argus.tools.eclipse.contribution.weaving.jdt.util.AbstractProviderRegistry;

public class OpenActionProviderRegistry extends AbstractProviderRegistry<IOpenActionProvider> {
	public static String OPEN_ACTION_PROVIDERS_EXTENSION_POINT = "org.argus-ide.cit.aspects.openactionprovider"; //$NON-NLS-1$

	private static final OpenActionProviderRegistry INSTANCE = new OpenActionProviderRegistry();

	public static OpenActionProviderRegistry getInstance() {
		return INSTANCE;
	}

	@Override
	protected String getExtensionPointId() {
		return OPEN_ACTION_PROVIDERS_EXTENSION_POINT;
	}
}
