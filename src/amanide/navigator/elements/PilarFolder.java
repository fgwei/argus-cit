package amanide.navigator.elements;

import org.eclipse.core.resources.IFolder;

/**
 * Class representing a folder within the pilarpath.
 * 
 * @author Fengguo Wei
 */
public class PilarFolder extends WrappedResource<IFolder> {

	public PilarFolder(IWrappedResource parentElement, IFolder folder,
			PilarSourceFolder pythonSourceFolder) {
		super(parentElement, folder, pythonSourceFolder,
				IWrappedResource.RANK_PILAR_FOLDER);
	}
}
