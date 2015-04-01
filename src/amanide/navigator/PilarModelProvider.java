package amanide.navigator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.navigator.IPipelinedTreeContentProvider;
import org.eclipse.ui.navigator.PipelinedShapeModification;
import org.eclipse.ui.navigator.PipelinedViewerUpdate;

import amanide.natures.PilarNature;
import amanide.navigator.elements.IWrappedResource;
import amanide.navigator.elements.PilarFile;
import amanide.navigator.elements.PilarFolder;
import amanide.navigator.elements.PilarProjectSourceFolder;
import amanide.navigator.elements.PilarResource;
import amanide.navigator.elements.PilarSourceFolder;
import amanide.navigator.elements.ProjectConfigError;
import amanide.structure.TreeNode;
import amanide.utils.FastStack;
import amanide.utils.Log;

/**
 * This is the Model provider for pilar elements.
 * 
 * It intercepts the adds/removes and changes the original elements for elements
 * that actually reflect the pilar model (with source folder, etc).
 * 
 * 
 * Tests for package explorer: 1. Start eclipse with a file deep in the
 * structure and without having anything expanded in the tree, make a 'show in'
 * 
 * @author Fengguo Wei
 */
public final class PilarModelProvider extends PilarBaseModelProvider implements
		IPipelinedTreeContentProvider {

	@Override
	public Object[] getChildren(Object parentElement) {
		Object[] ret = super.getChildren(parentElement);
		if (parentElement instanceof PilarProjectSourceFolder) {
			PilarProjectSourceFolder projectSourceFolder = (PilarProjectSourceFolder) parentElement;
			Set<Object> set = new HashSet<Object>();
			fillChildrenForProject(set,
					(IProject) projectSourceFolder.getActualObject(),
					projectSourceFolder);
			if (set.size() > 0) {
				Object[] newRet = new Object[ret.length + set.size()];
				System.arraycopy(ret, 0, newRet, 0, ret.length);
				int i = ret.length;
				for (Object o : set) {
					newRet[i] = o;
					i++;
				}
				ret = newRet;
			}
		}
		return ret;
	}

	/**
	 * This method basically replaces all the elements for other resource
	 * elements or for wrapped elements.
	 * 
	 * @see org.eclipse.ui.navigator.IPipelinedTreeContentProvider#getPipelinedChildren(java.lang.Object,
	 *      java.util.Set)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void getPipelinedChildren(Object parent, Set currentElements) {
		if (DEBUG) {
			Log.log("getPipelinedChildren for: " + parent);
		}

		if (parent instanceof IWrappedResource) {
			// Note: It seems that this NEVER happens (IWrappedResources only
			// have getChildren called, not getPipelinedChildren)
			Object[] children = getChildren(parent);
			currentElements.clear();
			currentElements.addAll(Arrays.asList(children));
			if (DEBUG) {
				Log.log("getPipelinedChildren RETURN: " + currentElements);
			}
			if (parent instanceof PilarProjectSourceFolder) {
				PilarProjectSourceFolder projectSourceFolder = (PilarProjectSourceFolder) parent;
				IProject project = (IProject) projectSourceFolder
						.getActualObject();
				fillChildrenForProject(currentElements, project, parent);
			}
			return;

		} else if (parent instanceof IWorkspaceRoot) {
			switch (topLevelChoice.getRootMode()) {
			case TopLevelProjectsOrWorkingSetChoice.WORKING_SETS:
				currentElements.clear();
				currentElements.addAll(getWorkingSetsCallback
						.call((IWorkspaceRoot) parent));
			case TopLevelProjectsOrWorkingSetChoice.PROJECTS:
				// Just go on...
			}

		} else if (parent instanceof IWorkingSet) {
			IWorkingSet workingSet = (IWorkingSet) parent;
			currentElements.clear();
			currentElements.addAll(Arrays.asList(workingSet.getElements()));

		} else if (parent instanceof TreeNode) {
			TreeNode treeNode = (TreeNode) parent;
			currentElements.addAll(treeNode.getChildren());

		} else if (parent instanceof IProject) {
			IProject project = (IProject) parent;
			fillChildrenForProject(currentElements, project,
					getResourceInPilarModel(project));
		}

		PipelinedShapeModification modification = new PipelinedShapeModification(
				parent, currentElements);
		convertToPilarElementsAddOrRemove(modification, true);
		if (DEBUG) {
			Log.log("getPipelinedChildren RETURN: "
					+ modification.getChildren());
		}
	}

	@SuppressWarnings("unchecked")
	private void fillChildrenForProject(Set currentElements, IProject project,
			Object parent) {
		ProjectInfoForPackageExplorer projectInfo = getProjectInfo(project);
		if (projectInfo != null) {
			currentElements.addAll(projectInfo.configErrors);
		}
	}

	/**
	 * This method basically replaces all the elements for other resource
	 * elements or for wrapped elements.
	 * 
	 * @see org.eclipse.ui.navigator.IPipelinedTreeContentProvider#getPipelinedElements(java.lang.Object,
	 *      java.util.Set)
	 */
	@Override
	public void getPipelinedElements(Object input, Set currentElements) {
		if (DEBUG) {
			Log.log("getPipelinedElements for: " + input);
		}
		getPipelinedChildren(input, currentElements);
	}

	/**
	 * This method basically get the actual parent for the resource or the
	 * parent for a wrapped element (which may be a resource or a wrapped
	 * resource).
	 * 
	 * @see org.eclipse.ui.navigator.IPipelinedTreeContentProvider#getPipelinedParent(java.lang.Object,
	 *      java.lang.Object)
	 */
	@Override
	public Object getPipelinedParent(Object object, Object aSuggestedParent) {
		if (DEBUG) {
			Log.log("getPipelinedParent for: " + object);
		}
		// Now, we got the parent for the resources correctly at this point, but
		// there's one last thing we may need to
		// do: the actual parent may be a working set!
		Object p = this.topLevelChoice.getWorkingSetParentIfAvailable(object,
				getWorkingSetsCallback);
		if (p != null) {
			aSuggestedParent = p;

		} else if (object instanceof IWrappedResource) {
			IWrappedResource resource = (IWrappedResource) object;
			Object parentElement = resource.getParentElement();
			if (parentElement != null) {
				aSuggestedParent = parentElement;
			}

		} else if (object instanceof TreeNode<?>) {
			TreeNode<?> treeNode = (TreeNode<?>) object;
			return treeNode.getParent();

		} else if (object instanceof ProjectConfigError) {
			ProjectConfigError configError = (ProjectConfigError) object;
			return configError.getParent();

		}

		if (DEBUG) {
			System.out
					.println("getPipelinedParent RETURN: " + aSuggestedParent);
		}
		return aSuggestedParent;
	}

	/**
	 * This method intercepts some addition to the tree and converts its
	 * elements to pilar elements.
	 * 
	 * @see org.eclipse.ui.navigator.IPipelinedTreeContentProvider#interceptAdd(org.eclipse.ui.navigator.PipelinedShapeModification)
	 */
	@Override
	public PipelinedShapeModification interceptAdd(
			PipelinedShapeModification addModification) {
		if (DEBUG) {
			Log.log("interceptAdd");
		}
		convertToPilarElementsAddOrRemove(addModification, true);
		return addModification;
	}

	@Override
	public boolean interceptRefresh(PipelinedViewerUpdate refreshSynchronization) {
		if (DEBUG) {
			Log.log("interceptRefresh");
		}
		return convertToPilarElementsUpdateOrRefresh(refreshSynchronization
				.getRefreshTargets());
	}

	@Override
	public PipelinedShapeModification interceptRemove(
			PipelinedShapeModification removeModification) {
		if (DEBUG) {
			Log.log("interceptRemove");
		}
		convertToPilarElementsAddOrRemove(removeModification, false);
		return removeModification;
	}

	@Override
	public boolean interceptUpdate(PipelinedViewerUpdate updateSynchronization) {
		if (DEBUG) {
			debug("Before interceptUpdate", updateSynchronization);
		}
		boolean ret = convertToPilarElementsUpdateOrRefresh(updateSynchronization
				.getRefreshTargets());
		if (DEBUG) {
			debug("After interceptUpdate", updateSynchronization);
		}
		return ret;
	}

	/**
	 * Helper for debugging the things we have in an update
	 */
	private void debug(String desc, PipelinedViewerUpdate updateSynchronization) {
		Log.log("\nDesc:" + desc);
		Log.log("Refresh targets:");
		for (Object o : updateSynchronization.getRefreshTargets()) {
			Log.log(o.toString());
		}
	}

	/**
	 * Helper for debugging the things we have in a modification
	 */
	private void debug(String desc, PipelinedShapeModification modification) {
		Log.log("\nDesc:" + desc);
		Object parent = modification.getParent();
		Log.log("Parent:" + parent);
		Log.log("Children:");
		for (Object o : modification.getChildren()) {
			Log.log(o.toString());
		}
	}

	/**
	 * This is the function that is responsible for restoring the paths in the
	 * tree.
	 */
	@Override
	public void restoreState(IMemento memento) {
		new AmanIDEPackageStateSaver(this, viewer, memento).restoreState();
	}

	/**
	 * This is the function that is responsible for saving the paths in the
	 * tree.
	 */
	@Override
	public void saveState(IMemento memento) {
		new AmanIDEPackageStateSaver(this, viewer, memento).saveState();
	}

	/**
	 * Converts the shape modification to use Pilar elements.
	 * 
	 * @param modification
	 *            : the shape modification to convert
	 * @param isAdd
	 *            : boolean indicating whether this convertion is happening in
	 *            an add operation
	 */
	private void convertToPilarElementsAddOrRemove(
			PipelinedShapeModification modification, boolean isAdd) {
		if (DEBUG) {
			debug("Before", modification);
		}
		Object parent = modification.getParent();
		if (parent instanceof IContainer) {
			IContainer parentContainer = (IContainer) parent;
			Object pilarParent = getResourceInPilarModel(parentContainer, true);

			if (pilarParent instanceof IWrappedResource) {
				IWrappedResource parentResource = (IWrappedResource) pilarParent;
				modification.setParent(parentResource);
				wrapChildren(parentResource, parentResource.getSourceFolder(),
						modification.getChildren(), isAdd);

			} else if (pilarParent == null) {

				Object parentInWrap = parentContainer;
				PilarSourceFolder sourceFolderInWrap = null;

				// this may happen when a source folder is added or some element
				// that still doesn't have it's parent in the model...
				// so, we have to get the parent's parent until we actually
				// 'know' that it is not in the model (or until we run
				// out of parents to try)
				// the case in which we reproduce this is Test 1 (described in
				// the class)
				FastStack<Object> found = new FastStack<Object>(20);
				while (true) {

					// add the current to the found
					if (parentContainer == null) {
						break;
					}

					found.push(parentContainer);
					if (parentContainer instanceof IProject) {
						// we got to the project without finding any part of a
						// pilar model already there, so, let's see
						// if any of the parts was actually a source folder
						// (that was still not added)
						tryCreateModelFromProject((IProject) parentContainer,
								found);
						// and now, if it was created, try to convert it to the
						// pilar model (without any further add)
						convertToPilarElementsUpdateOrRefresh(modification
								.getChildren());
						return;
					}

					Object p = getResourceInPilarModel(parentContainer, true);

					if (p instanceof IWrappedResource) {
						IWrappedResource wrappedResource = (IWrappedResource) p;
						sourceFolderInWrap = wrappedResource.getSourceFolder();

						while (found.size() > 0) {
							Object f = found.pop();
							if (f instanceof IResource) {
								// no need to create it if it's already in the
								// model!
								Object child = sourceFolderInWrap
										.getChild((IResource) f);
								if (child != null
										&& child instanceof IWrappedResource) {
									wrappedResource = (IWrappedResource) child;
									continue;
								}
							}
							// creating is enough to add it to the model
							if (f instanceof IFile) {
								wrappedResource = new PilarFile(
										wrappedResource, (IFile) f,
										sourceFolderInWrap);
							} else if (f instanceof IFolder) {
								wrappedResource = new PilarFolder(
										wrappedResource, (IFolder) f,
										sourceFolderInWrap);
							}
						}
						parentInWrap = wrappedResource;
						break;
					}

					parentContainer = parentContainer.getParent();
				}

				wrapChildren(parentInWrap, sourceFolderInWrap,
						modification.getChildren(), isAdd);
			}

		} else if (parent == null) {
			wrapChildren(null, null, modification.getChildren(), isAdd);
		}

		if (DEBUG) {
			debug("After", modification);
		}
	}

	/**
	 * Given a Path from the 1st child of the project, will try to create that
	 * path in the pilar model.
	 * 
	 * @param project
	 *            the project
	 * @param found
	 *            a stack so that the last element added is the leaf of the path
	 *            we want to discover
	 */
	private void tryCreateModelFromProject(IProject project,
			FastStack<Object> found) {
		PilarNature nature = PilarNature.getPilarNature(project);
		if (nature == null) {
			return;// if the pilar nature is not available, we won't have any
					// pilar elements here
		}
		Set<String> sourcePathSet = new HashSet<String>();
		try {
			sourcePathSet = nature.getPilarPathNature()
					.getProjectSourcePathSet(true);
		} catch (CoreException e) {
			Log.log(e);
		}

		Object currentParent = project;
		PilarSourceFolder pilarSourceFolder = null;
		for (Iterator<Object> it = found.topDownIterator(); it.hasNext();) {
			Object child = it.next();
			if (child instanceof IFolder || child instanceof IProject) {
				if (pilarSourceFolder == null) {
					pilarSourceFolder = tryWrapSourceFolder(currentParent,
							(IContainer) child, sourcePathSet);

					if (pilarSourceFolder != null) {
						currentParent = pilarSourceFolder;

					} else if (child instanceof IContainer) {
						currentParent = child;

					}

					// just go on (if we found the source folder or not, because
					// if we found, that's ok, and if
					// we didn't, then the children will not be in the pilar
					// model anyway)
					continue;
				}
			}

			if (pilarSourceFolder != null) {
				IWrappedResource r = doWrap(currentParent, pilarSourceFolder,
						child);
				if (r != null) {
					child = r;
				}
			}
			currentParent = child;
		}
	}

	/**
	 * Actually wraps some resource into a wrapped resource.
	 * 
	 * @param parent
	 *            this is the parent it may be null -- in the case of a remove
	 *            it may be a wrapped resource (if it is in the pilar model) it
	 *            may be a resource (if it is a source folder)
	 * 
	 * 
	 * @param pilarSourceFolder
	 *            this is the pilar source folder for the resource (it may be
	 *            null if the resource itself is a source folder or if it is
	 *            actually a resource that has already been removed)
	 * @param currentChildren
	 *            those are the children that should be wrapped
	 * @param isAdd
	 *            whether this is an add operation or not
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected boolean wrapChildren(Object parent,
			PilarSourceFolder pilarSourceFolder, Set currentChildren,
			boolean isAdd) {
		LinkedHashSet convertedChildren = new LinkedHashSet();

		for (Iterator childrenItr = currentChildren.iterator(); childrenItr
				.hasNext();) {
			Object child = childrenItr.next();

			if (child == null) {
				// only case when a child is removed and another one is not
				// added (null)
				childrenItr.remove();
				continue;
			}

			// yeap, it may be an object that's not an actual resource (created
			// by some other plugin... just continue)
			if (!(child instanceof IResource)) {
				continue;
			}
			Object existing = getResourceInPilarModel((IResource) child, true);

			if (existing == null) {
				if (isAdd) {
					// add
					IWrappedResource w = doWrap(parent, pilarSourceFolder,
							child);
					if (w != null) { // if it is null, it is not below a pilar
										// source folder
						childrenItr.remove();
						convertedChildren.add(w);
					}
				} else {
					continue; // it has already been removed
				}

			} else { // existing != null
				childrenItr.remove();
				convertedChildren.add(existing);
				if (!isAdd) {
					// also remove it from the model
					IWrappedResource wrapped = (IWrappedResource) existing;
					wrapped.getSourceFolder().removeChild((IResource) child);
				}
			}
		}

		// if we did have some wrapping... go on and add them to the out list
		// (and return true)
		if (!convertedChildren.isEmpty()) {
			currentChildren.addAll(convertedChildren);
			return true;
		}

		// nothing happened, so, just say it
		return false;
	}

	/**
	 * This method tries to wrap a given resource as a wrapped resource (if
	 * possible)
	 * 
	 * @param parent
	 *            the parent of the wrapped resource
	 * @param pilarSourceFolder
	 *            the source folder that contains this resource
	 * @param child
	 *            the object that should be wrapped
	 * @return the object as an object from the pilar model
	 */
	protected IWrappedResource doWrap(Object parent,
			PilarSourceFolder pilarSourceFolder, Object child) {
		if (child instanceof IProject) {
			// ok, let's see if the child is a source folder (as the default
			// project can be the actual source folder)
			if (pilarSourceFolder == null && parent != null) {
				PilarSourceFolder f = doWrapPossibleSourceFolder(parent,
						(IProject) child);
				if (f != null) {
					return f;
				}
			}

		} else if (child instanceof IFolder) {
			IFolder folder = (IFolder) child;

			// it may be a PilarSourceFolder
			if (pilarSourceFolder == null && parent != null) {
				PilarSourceFolder f = doWrapPossibleSourceFolder(parent, folder);
				if (f != null) {
					return f;
				}
			}
			if (pilarSourceFolder != null) {
				return new PilarFolder((IWrappedResource) parent, folder,
						pilarSourceFolder);
			}

		} else if (child instanceof IFile) {
			if (pilarSourceFolder != null) {
				// if the pilar source folder is null, that means that this is
				// a file that is not actually below a source folder -- so,
				// don't wrap it
				return new PilarFile((IWrappedResource) parent, (IFile) child,
						pilarSourceFolder);
			}

		} else if (child instanceof IResource) {
			if (pilarSourceFolder != null) {
				return new PilarResource((IWrappedResource) parent,
						(IResource) child, pilarSourceFolder);
			}

		} else {
			throw new RuntimeException("Unexpected class:" + child.getClass());
		}

		return null;
	}

	/**
	 * Try to wrap a folder or project as a source folder...
	 */
	private PilarSourceFolder doWrapPossibleSourceFolder(Object parent,
			IContainer container) {
		try {
			IProject project;
			if (!(container instanceof IProject)) {
				project = ((IContainer) parent).getProject();
			} else {
				project = (IProject) container;
			}
			PilarNature nature = PilarNature.getPilarNature(project);
			if (nature != null) {
				// check for source folder
				Set<String> sourcePathSet = nature.getPilarPathNature()
						.getProjectSourcePathSet(true);
				PilarSourceFolder newSourceFolder = tryWrapSourceFolder(parent,
						container, sourcePathSet);
				if (newSourceFolder != null) {
					return newSourceFolder;
				}
			}
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
		return null;
	}

	/**
	 * This method checks if the given folder can be wrapped as a source-folder,
	 * and if that's possible, creates and returns it
	 * 
	 * @return a created source folder or null if it couldn't be created.
	 */
	private PilarSourceFolder tryWrapSourceFolder(Object parent,
			IContainer container, Set<String> sourcePathSet) {
		IPath fullPath = container.getFullPath();
		if (sourcePathSet.contains(fullPath.toString())) {
			PilarSourceFolder sourceFolder;
			if (container instanceof IFolder) {
				sourceFolder = new PilarSourceFolder(parent,
						(IFolder) container);
			} else if (container instanceof IProject) {
				sourceFolder = new PilarProjectSourceFolder(parent,
						(IProject) container);
			} else {
				return null; // some other container we don't know how to treat!
			}
			// Log.log("Created source folder: "+ret[i]+" - "+folder.getProject()+" - "+folder.getProjectRelativePath());
			Set<PilarSourceFolder> sourceFolders = getProjectSourceFolders(container
					.getProject());
			sourceFolders.add(sourceFolder);
			return sourceFolder;
		}
		return null;
	}

	/**
	 * Converts elements to the pilar model -- but only creates it if it's
	 * parent is found in the pilar model
	 */
	@SuppressWarnings("unchecked")
	protected boolean convertToPilarElementsUpdateOrRefresh(Set currentChildren) {
		LinkedHashSet<Object> convertedChildren = new LinkedHashSet();
		for (Iterator childrenItr = currentChildren.iterator(); childrenItr
				.hasNext();) {
			Object child = childrenItr.next();

			if (child == null) {
				// only case when a child is removed and another one is not
				// added (null)
				childrenItr.remove();
				continue;
			}

			if (child instanceof IResource
					&& !(child instanceof IWrappedResource)) {
				IResource res = (IResource) child;

				Object resourceInPilarModel = getResourceInPilarModel(res, true);
				if (resourceInPilarModel != null) {
					// if it is in the pilar model, just go on
					childrenItr.remove();
					convertedChildren.add(resourceInPilarModel);

				} else {
					// now, if it's not but its parent is, go on and create it
					IContainer p = res.getParent();
					if (p == null) {
						continue;
					}

					Object pilarParent = getResourceInPilarModel(p, true);
					if (pilarParent instanceof IWrappedResource) {
						IWrappedResource parent = (IWrappedResource) pilarParent;

						if (res instanceof IProject) {
							throw new RuntimeException(
									"A project's parent should never be an IWrappedResource!");

						} else if (res instanceof IFolder) {
							childrenItr.remove();
							convertedChildren.add(new PilarFolder(parent,
									(IFolder) res, parent.getSourceFolder()));

						} else if (res instanceof IFile) {
							childrenItr.remove();
							convertedChildren.add(new PilarFile(parent,
									(IFile) res, parent.getSourceFolder()));

						} else if (child instanceof IResource) {
							childrenItr.remove();
							convertedChildren
									.add(new PilarResource(parent,
											(IResource) child, parent
													.getSourceFolder()));
						}

					} else if (res instanceof IFolder) {
						// ok, still not in the model... could it be a
						// PilarSourceFolder
						IFolder folder = (IFolder) res;
						IProject project = folder.getProject();
						if (project == null) {
							continue;
						}
						PilarNature nature = PilarNature
								.getPilarNature(project);
						if (nature == null) {
							continue;
						}
						Set<String> sourcePathSet = new HashSet<String>();
						try {
							sourcePathSet = nature.getPilarPathNature()
									.getProjectSourcePathSet(true);
						} catch (CoreException e) {
							Log.log(e);
						}
						PilarSourceFolder wrapped = tryWrapSourceFolder(p,
								folder, sourcePathSet);
						if (wrapped != null) {
							childrenItr.remove();
							convertedChildren.add(wrapped);
						}
					}
				}

			}
		}
		if (!convertedChildren.isEmpty()) {
			currentChildren.addAll(convertedChildren);
			return true;
		}
		return false;

	}
}
