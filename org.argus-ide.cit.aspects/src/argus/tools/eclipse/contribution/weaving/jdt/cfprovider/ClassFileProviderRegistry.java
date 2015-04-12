package argus.tools.eclipse.contribution.weaving.jdt.cfprovider;

import argus.tools.eclipse.contribution.weaving.jdt.util.AbstractProviderRegistry;

public class ClassFileProviderRegistry extends AbstractProviderRegistry<IClassFileProvider> {

	private static final ClassFileProviderRegistry INSTANCE = new ClassFileProviderRegistry();

        public static String CFPROVIDERS_EXTENSION_POINT = "org.argus-ide.cit.aspects.cfprovider"; //$NON-NLS-1$

	public static ClassFileProviderRegistry getInstance() {
		return INSTANCE;
	}

	@Override
	protected String getExtensionPointId() {
	    return CFPROVIDERS_EXTENSION_POINT;
	}
}
