package argus.tools.eclipse.contribution.weaving.jdt.cuprovider;


import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.PackageFragment;

import argus.tools.eclipse.contribution.weaving.jdt.ArgusJDTWeavingPlugin;

/**
 * Captures all creations of {@link CompilationUnit}.  Uses a registry to determine 
 * what kind of CompilationUnit to create.  Clients can provide their own
 * CompilationUnit by using the cuprovider extension and associating a CompilationUnit
 * subclass with a file extension.
 * 
 * @author andrew
 * @created Dec 2, 2008
 */
public aspect CompilationUnitProviderAspect {
    
    /**
     * Captures creations of Compilation units
     */
    pointcut compilationUnitCreations(PackageFragment parent, String name, WorkingCopyOwner owner) : 
            call(public CompilationUnit.new(PackageFragment, String, WorkingCopyOwner)) &&
            (
                    within(org.eclipse.jdt..*) ||
                    within(org.codehaus.jdt.groovy.integration.internal.*) ||  // Captures GroovyLanguageSupport if groovy plugin is installed
                    within(org.codehaus.jdt.groovy.integration.*) // Captures DefaultLanguageSupport if groovy plugin is installed
            ) &&
            args(parent, name, owner);

    CompilationUnit around(PackageFragment parent, String name, WorkingCopyOwner owner) : 
        compilationUnitCreations(parent, name, owner) {
        String newName = trimName(name);
        String extension = findExtension(newName);
        ICompilationUnitProvider provider = 
            CompilationUnitProviderRegistry.getInstance().getProvider(extension);
        if (provider != null) {
            try {
                return provider.create(parent, newName, owner);
            } catch (Throwable t) {
                ArgusJDTWeavingPlugin.logException(t);
            }
        }        
        return proceed(parent, name, owner);
    }

    /**
     * hacks off any excess parts of the compilation unit name that indicate
     * extra characters were included in the name
     * 
     * @param original the original name of the compilation unit
     * @return new name trimmed to ensure that all trailing memento parts are removed
     */
    private String trimName(String original) {
        String noo = original;
        int extensionIndex = original.indexOf('.') + 1;
        if (extensionIndex >= 0) {
            int mementoIndex = extensionIndex;
            while (mementoIndex < original.length() && (Character.isJavaIdentifierPart(original.charAt(mementoIndex)) ||
                    original.charAt(mementoIndex) == '.')) {
                mementoIndex++;
            }
            noo = original.substring(0, mementoIndex);
        }
        return noo;
    }
    
    private String findExtension(String name) {
        int extensionIndex = name.lastIndexOf('.') + 1;
        String extension;
        if (extensionIndex > 0) {
            extension = name.substring(extensionIndex);
        } else {
            extension = ""; //$NON-NLS-1$
        }
        return extension;
    }
    
}
