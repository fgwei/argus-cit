package argus.tools.eclipse.contribution.weaving.jdt.configuration;

import org.eclipse.jdt.core.ToolFactory;

/**
 * This aspect tests to see if the weaving service is properly installed.
 * 
 * @author Fengguo Wei
 * 
 * adapted from andrew
 */
public aspect IsWovenTester {

    interface PilarWeavingMarker { }
    
    /**
     * add a marker interface to an arbitrary class in JDT
     * later, we can see if the marker has been added.
     */
    declare parents : ToolFactory implements PilarWeavingMarker;
    
    private static boolean weavingActive = new ToolFactory() instanceof PilarWeavingMarker;
    
    public static boolean isWeavingActive() {
        return weavingActive;
    }
    
}
