package argus.tools.eclipse.contribution.weaving.jdt.imagedescriptor;

import org.eclipse.jdt.internal.ui.text.java.LazyJavaCompletionProposal;
import org.eclipse.jface.resource.ImageDescriptor;

@SuppressWarnings("restriction")
public interface IImageDescriptorSelector {
	/**
     * Creates the image descriptor for Java-like elements appearing in open type dialogs and the search view
     * Arguments are passed in from the Aspect
     */
    public ImageDescriptor getTypeImageDescriptor(boolean isInner, boolean isInInterfaceOrAnnotation, int flags, boolean useLightIcons, Object element);
    
    /**
     * Creates the image descriptor for Java-like elements appearing in content assist
     */
    public ImageDescriptor createCompletionProposalImageDescriptor(LazyJavaCompletionProposal proposal);
}
