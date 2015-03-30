package amanide.navigator.elements;

/**
 * Interface for a child resource (a resource that has a parent)
 * 
 * copyed from org.python.pydev.navigator.elements.ISortedElement.java
 */
class counter {
	private static int curr = -1;

	static int next() {
		curr += 1;
		return curr;
	}
}

public interface ISortedElement {

	int RANK_ERROR = counter.next();

	int RANK_SOURCE_FOLDER = counter.next();
	int RANK_PILAR_FOLDER = counter.next();

	int RANK_PILAR_FILE = counter.next();
	int RANK_PILAR_RESOURCE = counter.next();

	int RANK_REGULAR_FOLDER = counter.next();
	int RANK_REGULAR_FILE = counter.next();
	int RANK_REGULAR_RESOURCE = counter.next();

	int RANK_LIBS = counter.next();

	int RANK_PILAR_NODE = counter.next();

	// Used if we don't know how to categorize it.
	int UNKNOWN_ELEMENT = counter.next();

	// Tree nodes come after everything
	int RANK_TREE_NODE = counter.next();

	/**
	 * @return the ranking for the object. Lower values have higher priorities
	 */
	int getRank();
}
