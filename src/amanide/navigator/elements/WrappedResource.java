package amanide.navigator.elements;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IContributorResourceAdapter;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.IWorkbenchAdapter2;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;

import amanide.utils.FastStringBuffer;
import amanide.utils.FullRepIterable;

/**
 * This class represents a resource that is wrapped for the pilar model.
 * 
 * copied from org.python.pydev.navigator.elements.WrappedResource.java
 *
 * @param <X>
 */
public class WrappedResource<X extends IResource> implements IWrappedResource,
		IContributorResourceAdapter, IAdaptable {

	protected IWrappedResource parentElement;
	protected X actualObject;
	protected PilarSourceFolder pilarSourceFolder;
	protected int rank;

	public WrappedResource(IWrappedResource parentElement, X actualObject,
			PilarSourceFolder pilarSourceFolder, int rank) {
		this.parentElement = parentElement;
		this.actualObject = actualObject;
		this.pilarSourceFolder = pilarSourceFolder;
		this.pilarSourceFolder.addChild(this);
		this.rank = rank;
	}

	@Override
	public X getActualObject() {
		return actualObject;
	}

	@Override
	public IWrappedResource getParentElement() {
		return parentElement;
	}

	@Override
	public PilarSourceFolder getSourceFolder() {
		return pilarSourceFolder;
	}

	@Override
	public int getRank() {
		return rank;
	}

	@Override
	public IResource getAdaptedResource(IAdaptable adaptable) {
		return getActualObject();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof IWrappedResource) {
			if (other == this) {
				return true;
			}
			IWrappedResource w = (IWrappedResource) other;
			return this.actualObject.equals(w.getActualObject());
		}
		return false;

		// now returns always false because it could generate null things in the
		// search page... the reason is that when the
		// decorator manager had an update and passed in the search page, it
		// thought that a file/folder was the python file/folder,
		// and then, later when it tried to update it with that info, it ended
		// up removing the element because it didn't know how
		// to handle it.
		//
		// -- and this was also not a correct equals, because other.equals(this)
		// would not return true as this was returning
		// (basically we can't compare apples to oranges)
		// return actualObject.equals(other);
	}

	@Override
	public int hashCode() {
		return this.getActualObject().hashCode();
	}

	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == IContributorResourceAdapter.class) {
			return this;
		}
		return WrappedResource.getAdapterFromActualObject(
				this.getActualObject(), adapter);
	}

	@Override
	public String toString() {
		FastStringBuffer buf = new FastStringBuffer();
		buf.append(FullRepIterable.getLastPart(super.toString()));
		buf.append(" (");
		buf.append(this.getActualObject().toString());
		buf.append(")");
		return buf.toString();
	}

	public static Object getAdapterFromActualObject(IResource actualObject2,
			Class adapter) {
		if (IDeferredWorkbenchAdapter.class.equals(adapter)
				|| IWorkbenchAdapter2.class.equals(adapter)
				|| IWorkbenchAdapter.class.equals(adapter)) {
			return null;
		}
		return actualObject2.getAdapter(adapter);
	}
}
