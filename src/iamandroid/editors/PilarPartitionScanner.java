package iamandroid.editors;

import iamandroid.utils.Log;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.*;

public class PilarPartitionScanner extends RuleBasedPartitionScanner implements IPilarPartitions {

	public PilarPartitionScanner() {

		super();
    List<IPredicateRule> rules = new ArrayList<IPredicateRule>();

    addMultilineStringRule(rules);
    addSinglelineStringRule(rules);
    addCommentRule(rules);
    addLocRule(rules);

    setPredicateRules(rules.toArray(new IPredicateRule[0]));
	}
	
	private void addSinglelineStringRule(List<IPredicateRule> rules) {
    IToken singleLineString1 = new Token(IPilarPartitions.PILAR_SINGLELINE_STRING1);
    IToken singleLineString2 = new Token(IPilarPartitions.PILAR_SINGLELINE_STRING2);
    // deal with "" and '' strings
    boolean breaksOnEOL = true;
    boolean breaksOnEOF = false;
    boolean escapeContinuesLine = true;
    rules.add(new PatternRule("'", "'", singleLineString1, '\\', breaksOnEOL, breaksOnEOF, escapeContinuesLine));
    rules.add(new PatternRule("\"", "\"", singleLineString2, '\\', breaksOnEOL, breaksOnEOF, escapeContinuesLine));
	}
	
	private void addMultilineStringRule(List<IPredicateRule> rules) {
	    IToken multiLineString = new Token(IPilarPartitions.PILAR_MULTILINE_STRING);
	    // deal with ''' and """ strings
	
	    boolean breaksOnEOF = true;
	    //If we don't add breaksOnEOF = true it won't properly recognize the rule while typing
	    //in the following case:
	    ///'''<new line>
	    //text
	    //''' <-- it's already lost at this point and the 'text' will not be in a multiline string partition.
	    rules.add(new MultiLineRule("\"\"\"", "\"\"\"", multiLineString, '\\', breaksOnEOF));
	}
	
	private void addCommentRule(List<IPredicateRule> rules) {
	    IToken singlelinecomment = new Token(IPilarPartitions.PILAR_SINGLELINE_COMMENT);
	    IToken multilinecomment = new Token(IPilarPartitions.PILAR_MULTILINE_COMMENT);
	    boolean breaksOnEOF = true;
	    rules.add(new MultiLineRule("/*", "*/", multilinecomment, (char) 0, breaksOnEOF));
			rules.add(new EndOfLineRule("//", singlelinecomment));
	}
	
	private void addLocRule(List<IPredicateRule> rules) {
    IToken loc = new Token(IPilarPartitions.PILAR_LOC);
    rules.add(new SingleLineRule("#", ".", loc, (char) 0, true));
	}
	
	/**
	 * @return all types recognized by this scanner (used by doc partitioner)
	 */
	static public String[] getTypes() {
	    return IPilarPartitions.types;
	}
	
	/**
	 * Checks if the partitioner is correctly set in the document.
	 * @return the partitioner that is set in the document
	 */
	public static IDocumentPartitioner checkPartitionScanner(IDocument document) {
	    if (document == null) {
	        return null;
	    }
	
	    IDocumentExtension3 docExtension = (IDocumentExtension3) document;
	    IDocumentPartitioner partitioner = docExtension.getDocumentPartitioner(IPilarPartitions.PILAR_PARTITION_TYPE);
	    if (partitioner == null) {
	        addPartitionScanner(document);
	        //get it again for the next check
	        partitioner = docExtension.getDocumentPartitioner(IPilarPartitions.PILAR_PARTITION_TYPE);
	    }
	    if (!(partitioner instanceof PilarPartitioner)) {
	        Log.log("Partitioner should be subclass of PilarPartitioner. It is " + partitioner.getClass());
	    }
	    return partitioner;
	}
	
	/**
	 * @see http://help.eclipse.org/help31/index.jsp?topic=/org.eclipse.platform.doc.isv/guide/editors_documents.htm
	 * @see http://jroller.com/page/bobfoster -  Saturday July 16, 2005
	 * @param document the document where we want to add the partitioner
	 * @return the added document partitioner (or null)
	 */
	public static IDocumentPartitioner addPartitionScanner(IDocument document) {
	    if (document != null) {
	        IDocumentExtension3 docExtension = (IDocumentExtension3) document;
	        IDocumentPartitioner curr = docExtension.getDocumentPartitioner(IPilarPartitions.PILAR_PARTITION_TYPE);
	
	        if (curr == null) {
	            //set the new one
	            IDocumentPartitioner partitioner = createPilarPartitioner();
	            partitioner.connect(document);
	            docExtension.setDocumentPartitioner(IPilarPartitions.PILAR_PARTITION_TYPE, partitioner);
	            return partitioner;
	        } else {
	            return curr;
	        }
	    }
	    return null;
	}
	
	public static PilarPartitioner createPilarPartitioner() {
	    return new PilarPartitioner(new PilarPartitionScanner(), getTypes());
	}
}
