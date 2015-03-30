package amanide.editors;

import org.eclipse.jface.text.rules.IWordDetector;

public class PilarKeywordsDetector implements IWordDetector{

	
	@Override
	public boolean isWordStart(char c) {
		boolean result = false;
		for(String str : PilarKeywordList.keywords){
			if(str.charAt(0) == c){
				result = true;
			}
		}
		return result;
	}

	@Override
	public boolean isWordPart(char c) {
		boolean result = false;
		for(String str : PilarKeywordList.keywords){
			if(str.indexOf(c) >= 0){
				result = true;
			}
		}
		return result;
	}

}
