package amanide.navigator.elements;

import org.eclipse.core.resources.IProject;

import amanide.utils.Log;

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
		Log.log("Created PilarProjectSourceFolder:" + this + " - " + project
				+ " parent:" + parentElement);
	}

	@Override
	public String toString() {
		return "PilarProjectSourceFolder [" + this.getActualObject() + "]";
	}
}
