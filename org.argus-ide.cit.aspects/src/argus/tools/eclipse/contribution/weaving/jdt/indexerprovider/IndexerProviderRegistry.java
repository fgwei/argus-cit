package argus.tools.eclipse.contribution.weaving.jdt.indexerprovider;

import argus.tools.eclipse.contribution.weaving.jdt.util.AbstractProviderRegistry;

public class IndexerProviderRegistry extends AbstractProviderRegistry<IIndexerFactory> {

        public static String INDEXING_PROVIDERS_EXTENSION_POINT = "org.argus-ide.cit.aspects.indexerprovider"; //$NON-NLS-1$

	private static final IndexerProviderRegistry INSTANCE = new IndexerProviderRegistry();

	public static IndexerProviderRegistry getInstance() {
		return INSTANCE;
	}

	@Override
	protected String getExtensionPointId() {
	    return INDEXING_PROVIDERS_EXTENSION_POINT;
	}
}
