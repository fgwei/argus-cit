package amanide.navigator.elements;

import org.eclipse.core.resources.IResource;

public class PilarResource extends WrappedResource<IResource> {

	public PilarResource(IWrappedResource parentElement, IResource object,
			PilarSourceFolder pilarSourceFolder) {
		super(parentElement, object, pilarSourceFolder,
				IWrappedResource.RANK_PILAR_RESOURCE);
		// System.out.println("Created PythonResource:"+this+" - "+actualObject+" parent:"+parentElement);
	}

}
