//package amanide.navigator.elements;
//
//public class PilarNode implements Comparable, IWrappedResource {
//
//	/**
//	 * This is the parent (PythonFile or PythonNode) for this object
//	 */
//	public Object parent;
//
//	/**
//	 * The entry itself
//	 */
//	public ParsedItem entry;
//
//	/**
//	 * The pythonfile where this node is contained
//	 */
//	public PilarFile pythonFile;
//
//	/**
//	 * Constructor
//	 * 
//	 * @param pythonFile
//	 *            this is the file that contains this node
//	 * @param parent
//	 *            this is the parent for this item (a PythonFile or another
//	 *            PythonNode)
//	 * @param e
//	 *            the parsed item that represents this node.
//	 */
//	public PilarNode(PilarFile pythonFile, Object parent, ParsedItem e) {
//		this.parent = parent;
//		this.entry = e;
//		this.pythonFile = pythonFile;
//	}
//
//	@Override
//	public String toString() {
//		return entry.toString();
//	}
//
//	@Override
//	public int compareTo(Object o) {
//		if (!(o instanceof PilarNode)) {
//			return 0;
//		}
//		return entry.compareTo(((PilarNode) o).entry);
//	}
//
//	@Override
//	public Object getParentElement() {
//		return parent;
//	}
//
//	@Override
//	public ParsedItem getActualObject() {
//		return entry;
//	}
//
//	@Override
//	public PilarSourceFolder getSourceFolder() {
//		return pythonFile.getSourceFolder();
//	}
//
//	public PilarFile getPythonFile() {
//		return pythonFile;
//	}
//
//	@Override
//	public int getRank() {
//		return IWrappedResource.RANK_PILAR_NODE;
//	}
//
//	@Override
//	public Object getAdapter(Class adapter) {
//		// return pythonFile.getAdapter(adapter);
//		return null;
//	}
//
// }
