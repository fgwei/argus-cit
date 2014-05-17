package iamandroid.editors;

import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.text.*;

public class PilarScanner extends RuleBasedScanner {

	public PilarScanner(ColorManager manager) {
		IToken numerical =
				new Token(
					new TextAttribute(
						manager.getColor(IPilarColorConstants.NUMERICAL)));
		
		IToken annotation =
			new Token(
				new TextAttribute(
					manager.getColor(IPilarColorConstants.ANNOTATION)));
		IToken keywordToken = 
				new Token(
						new TextAttribute(
								manager.getColor(IPilarColorConstants.KEYWORD)));
    IToken defaultToken =
        new Token(
           new TextAttribute(
              manager.getColor(IPilarColorConstants.DEFAULT)));
    
		WordRule keywordRule = new WordRule(new PilarKeywordsDetector(),defaultToken);
		for(String str : PilarKeywordList.keywords) {
		   keywordRule.addWord(str, keywordToken);
		}
		
		IRule[] rules = new IRule[4];
		//Add rule for processing numbers
		rules[0] = new NumberRule(numerical);
		//Add rule for processing msStrings
//		rules[1] = new MultiLineRule("\"\"\"", "\"\"\"", string);
		//Add rule for processing strings
//		rules[2] = new SingleLineRule("\"", "\"", string);
		//Add rule for processing annotations
		rules[1] = new SingleLineRule("@", "", annotation);
		// Add generic whitespace rule.
		rules[2] = new WhitespaceRule(new PilarWhitespaceDetector());
		// Add rule for processing keyword
		rules[3] = keywordRule;

		setRules(rules);
	}
}
