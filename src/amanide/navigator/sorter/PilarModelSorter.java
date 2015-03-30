package amanide.navigator.sorter;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import amanide.navigator.PilarLabelProvider;
import amanide.navigator.elements.ISortedElement;
import amanide.structure.TreeNode;

/**
 * @author Fengugo Wei
 */
public class PilarModelSorter extends ViewerSorter {

	private PilarLabelProvider labelProvider;

	public PilarModelSorter() {
		labelProvider = new PilarLabelProvider();
	}

	@Override
	public int category(Object element) {
		if (element instanceof TreeNode) {
			return ISortedElement.RANK_TREE_NODE;
		}

		if (element instanceof ISortedElement) {
			ISortedElement iSortedElement = (ISortedElement) element;
			return iSortedElement.getRank();
		}

		if (element instanceof IContainer) {
			return ISortedElement.RANK_REGULAR_FOLDER;
		}
		if (element instanceof IFile) {
			return ISortedElement.RANK_REGULAR_FILE;
		}
		if (element instanceof IResource) {
			return ISortedElement.RANK_REGULAR_RESOURCE;
		}
		return ISortedElement.UNKNOWN_ELEMENT;
	}

	@SuppressWarnings("unchecked")
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		// Could be super.compare, but we don't have a way to override getLabel,
		// so, copying the whole code.
		int cat1 = category(e1);
		int cat2 = category(e2);

		if (cat1 != cat2) {
			return cat1 - cat2;
		}

		String name1 = getLabel(viewer, e1);
		String name2 = getLabel(viewer, e2);

		// use the comparator to compare the strings
		int compare = getComparator().compare(name1, name2);
		return compare;
	}

	private String getLabel(Viewer viewer, Object e1) {
		String text = labelProvider.getText(e1);
		return text;
	}
}
