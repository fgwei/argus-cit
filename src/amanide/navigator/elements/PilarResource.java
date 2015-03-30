package amanide.navigator.elements;

import org.eclipse.core.resources.IResource;

public class PilarResource extends WrappedResource<IResource> {

	public PilarResource(IWrappedResource parentElement, IResource object,
			PilarSourceFolder pythonSourceFolder) {
		super(parentElement, object, pythonSourceFolder,
				IWrappedResource.RANK_PILAR_RESOURCE);
		// System.out.println("Created PythonResource:"+this+" - "+actualObject+" parent:"+parentElement);
	}

}
