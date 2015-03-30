package amanide.editors;

import org.eclipse.jface.text.IDocument;

public interface IPilarPartitions {
	public final static String PILAR_SINGLELINE_COMMENT = "__pilar_singleline_comment"; // //...
	public final static String PILAR_MULTILINE_COMMENT = "__pilar_multiline_comment"; // /* ... */
	public final static String PILAR_SINGLELINE_STRING1 = "__pilar_singleline_string1"; //'
	public final static String PILAR_SINGLELINE_STRING2 = "__pilar_singleline_string2"; //"
	public final static String PILAR_MULTILINE_STRING = "__pilar_multiline_string"; //"""
	public final static String PILAR_LOC = "__pilar_loc"; // #xxxx.
	public final static String PILAR_DEFAULT = IDocument.DEFAULT_CONTENT_TYPE;
	
	public final static String[] types = {PILAR_SINGLELINE_COMMENT, PILAR_MULTILINE_COMMENT, PILAR_SINGLELINE_STRING1, PILAR_SINGLELINE_STRING2, 
		PILAR_MULTILINE_STRING, PILAR_LOC, PILAR_DEFAULT};
	public final static String PILAR_PARTITION_TYPE = "__PILAR_PARTITION_TYPE";
}
