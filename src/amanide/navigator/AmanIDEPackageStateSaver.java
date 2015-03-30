package amanide.navigator;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkingSet;

import amanide.navigator.elements.IWrappedResource;
import amanide.navigator.ui.AmanIDEPackageExplorer.AmanIDECommonViewer;
import amanide.utils.Log;

/**
 * This class saves and restores the expanded and selected items in the tree.
 */
public class AmanIDEPackageStateSaver {

	private PilarModelProvider provider;
	private Viewer viewer;
	private IMemento memento;

	public AmanIDEPackageStateSaver(PilarModelProvider provider, Viewer viewer,
			IMemento memento) {
		this.provider = provider;
		this.viewer = viewer;
		this.memento = memento;
	}

	public void restoreState() {
		try {
			if (!(viewer instanceof AmanIDECommonViewer) || memento == null) {
				// we have to check that because we can be asked to restore
				// things in the ProjectExplorer too, and not
				// only in the pydev package explorer -- and in this case, the
				// method: getTreePathFromItem(Item item) is
				// not be overridden and can cause the method to fail.
				return;
			}

			AmanIDECommonViewer treeViewer = (AmanIDECommonViewer) viewer;

			// we have to restore it only at the 'right' time... see
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=195184 for more
			// details
			if (!treeViewer.availableToRestoreMemento)
				return;

			IMemento[] expanded = memento.getChildren("expanded");
			for (IMemento m : expanded) {
				Object resource = getResourceFromPath(m);
				if (resource != null) {
					// it has to be done level by level because the children may
					// be created
					// for each expand (so, we 1st must expand the source folder
					// in order to
					// get the correct folders beneath it).
					treeViewer.expandToLevel(resource, 1);
				}
			}

			ArrayList<TreePath> paths = new ArrayList<TreePath>();
			IMemento[] selected = memento.getChildren("selected");
			for (IMemento m : selected) {
				Object resource = getResourceFromPath(m);

				if (resource != null) {
					treeViewer.expandToLevel(resource, 1);
					paths.add(new TreePath(getCompletPath(resource).toArray()));
				}
			}

			treeViewer.setSelection(
					new TreeSelection(paths.toArray(new TreePath[0])), true);
		} catch (Exception e) {
			Log.log(e);
		}
	}

	/**
	 * This method will get the complete path in the tree for a resource (or
	 * wrapped resource)
	 */
	private ArrayList<Object> getCompletPath(Object resource) {
		int max = 100; // cannot have more than 100 levels... ok? (this is just
						// a 'safeguard')
		int i = 0;
		ArrayList<Object> ret = new ArrayList<Object>();
		ret.add(0, resource);

		while (true) {
			i++;
			if (i > max) {
				Log.log("Could not get the structure for: " + resource);
				return new ArrayList<Object>();// something strange happened...

			} else if (resource instanceof IProject
					|| resource instanceof IWorkspaceRoot
					|| resource instanceof IWorkingSet) {
				break;

			} else if (resource instanceof IWrappedResource) {
				IWrappedResource w = (IWrappedResource) resource;
				resource = w.getParentElement();
				if (resource == null) {
					break;
				}
				ret.add(0, resource);

			} else if (resource instanceof IResource) {
				IResource r = (IResource) resource;
				resource = r.getParent();
				if (resource == null) {
					break;
				}
				ret.add(0, resource);
			}
		}

		return ret;
	}

	private Object getResourceFromPath(IMemento m) {
		IPath path = Path.fromPortableString(m.getID());
		// IResource resource = new
		// PySourceLocatorBase().getFileForLocation(path,
		// null);
		// if (resource == null || !resource.exists()) {
		// resource = new PySourceLocatorBase().getContainerForLocation(path,
		// null);
		// }
		// if (resource != null && resource.exists()) {
		// return provider.getResourceInPilarModel(resource);
		// }
		return null;
	}

	/**
	 * This is the function that is responsible for saving the paths in the
	 * tree.
	 */
	public void saveState() {
		try {

			if (!(viewer instanceof AmanIDECommonViewer)) {
				// we have to check that because we can be asked to restore
				// things in the ProjectExplorer too, and not
				// only in the pydev package explorer -- and in this case, the
				// method: getTreePathFromItem(Item item) is
				// not be overridden and can cause the method to fail.
				return;
			}

			AmanIDECommonViewer treeViewer = (AmanIDECommonViewer) viewer;
			TreePath[] expandedTreePaths = treeViewer.getExpandedTreePaths();
			for (TreePath path : expandedTreePaths) {
				save(path, "expanded");
			}

			ISelection selection = viewer.getSelection();
			if (selection instanceof ITreeSelection) {
				ITreeSelection treeSelection = (ITreeSelection) selection;
				TreePath[] paths = treeSelection.getPaths();
				for (TreePath path : paths) {
					save(path, "selected");
				}
			}
		} catch (Exception e) {
			Log.log(e);
		}
	}

	/**
	 * Saves some selection in the memento object.
	 */
	private void save(TreePath treePath, String type) {
		if (treePath != null) {
			Object object = treePath.getLastSegment();
			if (object instanceof IAdaptable) {
				IAdaptable adaptable = (IAdaptable) object;
				IResource resource = (IResource) adaptable
						.getAdapter(IResource.class);
				if (resource != null) {
					IPath path = resource.getLocation();
					if (path != null) {
						memento.createChild(type, path.toPortableString());
					}
				}
			}
		}
	}

}
