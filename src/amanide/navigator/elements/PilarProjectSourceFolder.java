package amanide.navigator.elements;

import org.eclipse.core.resources.IProject;

/**
 * Basically, this class represents a project when it is specified as a source
 * folder.
 * 
 * adapted from
 * org.python.pydev.navigator.elements.PythonProjectSourceFolder.java
 * 
 * @author Fengguo Wei
 */
public class PilarProjectSourceFolder extends PilarSourceFolder {

	public PilarProjectSourceFolder(Object parentElement, IProject project) {
		super(parentElement, project);
	}

}
