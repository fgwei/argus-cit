package amanide.navigator.elements;

import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import amanide.editors.codecompletion.PilarPathHelper;

/**
 * Note that the pilar file here does not actually mean a .pilar file (it can be
 * any file, such as .txt, .gif, etc)
 * 
 * @author Fengguo Wei
 */
public class PilarFile extends WrappedResource<IFile> {

	public PilarFile(IWrappedResource parentElement, IFile actualObject,
			PilarSourceFolder pilarSourceFolder) {
		super(parentElement, actualObject, pilarSourceFolder,
				IWrappedResource.RANK_PILAR_FILE);
		PilarPathHelper.markAsAmanIDEFileIfDetected(actualObject);
	}

	public InputStream getContents() throws CoreException {
		try {
			return this.actualObject.getContents();
		} catch (CoreException e) {
			// out of sync
			this.actualObject.refreshLocal(IResource.DEPTH_ZERO,
					new NullProgressMonitor());
			return this.actualObject.getContents();
		}
	}

}
