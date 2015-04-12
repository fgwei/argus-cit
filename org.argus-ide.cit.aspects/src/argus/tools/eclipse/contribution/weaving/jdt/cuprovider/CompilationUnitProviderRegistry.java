package argus.tools.eclipse.contribution.weaving.jdt.cuprovider;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

import argus.tools.eclipse.contribution.weaving.jdt.ArgusJDTWeavingPlugin;

public class CompilationUnitProviderRegistry {
    public static String CUPROVIDERS_EXTENSION_POINT = "org.argus-ide.cit.aspects.cuprovider"; //$NON-NLS-1$
    
    private static final CompilationUnitProviderRegistry INSTANCE = 
        new CompilationUnitProviderRegistry();
    
    public static CompilationUnitProviderRegistry getInstance() {
        return INSTANCE;
    }

    private CompilationUnitProviderRegistry() { 
        // do nothing
    }
    
    private Map<String, ICompilationUnitProvider> registry; 
    
    void registerCompilationUnitProvider(String key, ICompilationUnitProvider provider) {
        registry.put(key, provider);
    }
    
    ICompilationUnitProvider getProvider(String key) {
        if (!isRegistered()) {
            registerProviders();
        }
        return (ICompilationUnitProvider) registry.get(key);
    }
    
    
    public boolean isRegistered() {
        return registry != null;
    }
    
    public void registerProviders() {
        registry = new HashMap<String, ICompilationUnitProvider>();
        IExtensionPoint exP =
            Platform.getExtensionRegistry().getExtensionPoint(CUPROVIDERS_EXTENSION_POINT);
        if (exP != null) {
            IExtension[] exs = exP.getExtensions();
            for (int i = 0; i < exs.length; i++) {
                IConfigurationElement[] configs = exs[i].getConfigurationElements();
                for (int j = 0; j < configs.length; j++) {
                    try {
                        IConfigurationElement config = configs[j];
                        if (config.isValid()) {
                            ICompilationUnitProvider provider = (ICompilationUnitProvider) 
                                    config.createExecutableExtension("class"); //$NON-NLS-1$
                            registerCompilationUnitProvider(config.getAttribute("file_extension"), provider); //$NON-NLS-1$
                        }
                    } catch (CoreException e) {
                      ArgusJDTWeavingPlugin.logException(e);
                    } 
                }
            }
        }
    }
}
