/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * @author: atotic, Scott Schlesier
 * Created: March 5, 2005
 */
package iamandroid.editors;

import iamandroid.cache.ColorAndStyleCache;
import iamandroid.callbacks.ICallbackListener;
import iamandroid.utils.FastStringBuffer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWhitespaceDetector;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;

/**
 * PyCodeScanner - A scanner that looks for python keywords and code
 * and supports the updating of named colors through the colorCache
 * 
 * GreatWhite, GreatKeywordDetector came from PyEditConfiguration
 */
public class PilarCodeScanner extends RuleBasedScanner {

    private final ColorAndStyleCache colorCache;

    private IToken keywordToken;
    private IToken defaultToken;
    private IToken annotationToken;
    private IToken numberToken;
    private IToken recordNameToken;
    private IToken procedureNameToken;
    private IToken parensToken;
    private IToken operatorsToken;

    private String[] keywords;

    private ICodeScannerKeywords codeScannerKeywords;

    /**
     * Whitespace detector.
     * 
     * I know, naming the class after a band that burned
     * is not funny, but I've got to get my brain off my
     * annoyance with the abstractions of JFace.
     * So many classes and interfaces for a single method?
     * f$%@#$!!
     */
    static private class GreatWhite implements IWhitespaceDetector {
        public boolean isWhitespace(char c) {
            return Character.isWhitespace(c);
        }
    }

    /**
     * Python keyword detector
     */
    static private class GreatKeywordDetector implements IWordDetector {
  		private boolean isPilarSpec = false;
  		private int backquotecount = 1;
      public GreatKeywordDetector() {
      }

      public boolean isWordStart(char c) {
      	boolean res = false;
    		if(c == '`'){
    			res = true;
    			isPilarSpec = true;
    			backquotecount = 1;
    		} else if (Character.isJavaIdentifierStart(c)) {
    			res = true;
    			isPilarSpec = false;
    		}
        return res;
      }

      public boolean isWordPart(char c) {
      	boolean res = true;
      	if(isPilarSpec){
      		if(backquotecount >= 2){
      			res = false;
      		}
      		if(c == '`'){
      			backquotecount += 1;
      		}
      	} else {
      		res = Character.isJavaIdentifierPart(c);
      	}
        return res;
      }
    }
    
    static private class AnnotationDetector implements IWordDetector {

    	private int backquotecounter = 0;
    	private int whitespacecounter = 0;
    	private int parencounter = 0;
    	private boolean lock = false;
    	
      /**
       * @see org.eclipse.jface.text.rules.IWordDetector#isWordStart(char)
       */
      public boolean isWordStart(char c) {
      	boolean res = (c == '@');
      	if(res){
      		backquotecounter = 0;
      		whitespacecounter = 0;
      		parencounter = 0;
      		lock = false;
      	}
        return res;
      }

      /**
       * @see org.eclipse.jface.text.rules.IWordDetector#isWordPart(char)
       */
      public boolean isWordPart(char c) {
      	boolean res = true;
      	if(backquotecounter == 2) return false;
      	if(c != ' ') lock = false;
      	if(c == '`') backquotecounter += 1;
      	if(c == '(') parencounter += 1;
      	if(!lock && c == ' '){
      		whitespacecounter += 1;
      		lock = true;
      	}
      	if(whitespacecounter >= 2) res = false;
      	else if(backquotecounter == 0 && parencounter == 0){
        	if(c == '\n' || c == '\r' || c == ';' || c == '{' || c == '@' || c == ')') res = false;
      	}
      	if(c == ')') parencounter -= 1;
      	return res;
      }

    }

    static public class NumberDetector implements IWordDetector {

        /**
         * Used to keep the state of the token
         */
        private FastStringBuffer buffer = new FastStringBuffer();

        /**
         * Defines if we are at an hexa number
         */
        private boolean isInHexa;

        /**
         * @see org.eclipse.jface.text.rules.IWordDetector#isWordStart(char)
         */
        public boolean isWordStart(char c) {
            isInHexa = false;
            buffer.clear();
            buffer.append(c);
            return Character.isDigit(c);
        }

        /**
         * Check if we are still in the number
         */
        public boolean isWordPart(char c) {
            //ok, we have to test for scientific notation e.g.: 10.9e10

            if ((c == 'x' || c == 'X') && buffer.length() == 1 && buffer.charAt(0) == '0') {
                //it is an hexadecimal
                buffer.append(c);
                isInHexa = true;
                return true;
            } else {
                buffer.append(c);
            }

            if (isInHexa) {
                return Character.isDigit(c) || c == 'a' || c == 'A' || c == 'b' || c == 'B' || c == 'c' || c == 'C'
                        || c == 'd' || c == 'D' || c == 'e' || c == 'E' || c == 'f' || c == 'F';

            } else {
                return Character.isDigit(c) || c == 'e' || c == '.' || c == 'f' || c == 'F' || c == 'i' || c == 'I'
                		|| c == 'd' || c == 'D' || c == 'l' || c == 'L';
            }
        }

    }

    public PilarCodeScanner(ColorAndStyleCache colorCache) {
        this(colorCache, PilarKeywordList.keywords.toArray(new String[0]));
    }

    public PilarCodeScanner(ColorAndStyleCache colorCache, String[] keywords) {
        super();
        this.keywords = keywords;
        this.colorCache = colorCache;

        setupRules();
    }

    /**
     * @param colorCache2
     * @param codeScannerKeywords
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public PilarCodeScanner(ColorAndStyleCache colorCache, ICodeScannerKeywords codeScannerKeywords) {
        super();
        this.colorCache = colorCache;
        this.codeScannerKeywords = codeScannerKeywords;
        this.keywords = codeScannerKeywords.getKeywords();

        setupRules();

        codeScannerKeywords.getOnChangeCallbackWithListeners().registerListener(new ICallbackListener() {

            public Object call(Object obj) {
                keywords = PilarCodeScanner.this.codeScannerKeywords.getKeywords();
                setupRules();
                return null;
            }
        });
    }

    public void updateColors() {
        setupRules();
    }

    private void setupRules() {
        keywordToken = new Token(colorCache.getKeywordTextAttribute());

        defaultToken = new Token(colorCache.getCodeTextAttribute());

        annotationToken = new Token(colorCache.getAnnotationTextAttribute());

        numberToken = new Token(colorCache.getNumberTextAttribute());

        recordNameToken = new Token(colorCache.getRecordNameTextAttribute());

        procedureNameToken = new Token(colorCache.getProcedureNameTextAttribute());

        parensToken = new Token(colorCache.getParensTextAttribute());

        operatorsToken = new Token(colorCache.getOperatorsTextAttribute());

        setDefaultReturnToken(defaultToken);
        List<IRule> rules = new ArrayList<IRule>();

        // Scanning strategy:
        // 1) whitespace
        // 2) code
        // 3) regular words?

        WhitespaceRule whitespaceRule;
        try {
            whitespaceRule = new WhitespaceRule(new GreatWhite(), defaultToken);
        } catch (Throwable e) {
            //Compatibility with Eclipse 3.4 and below.
            whitespaceRule = new WhitespaceRule(new GreatWhite());
        }
        rules.add(whitespaceRule);

        Map<String, IToken> defaults = new HashMap<String, IToken>();
//        defaults.put("self", selfToken);

        PilarWordRule wordRule = new PilarWordRule(new GreatKeywordDetector(), defaultToken, recordNameToken, procedureNameToken,
                parensToken, operatorsToken);
        for (String keyword : keywords) {
            IToken token = defaults.get(keyword);
            if (token == null) {
                token = keywordToken;
            }
            wordRule.addWord(keyword, token);
        }

        rules.add(wordRule);

        rules.add(new WordRule(new AnnotationDetector(), annotationToken));
        rules.add(new WordRule(new NumberDetector(), numberToken));

        setRules(rules.toArray(new IRule[0]));
    }

    /**
     * Used from the django templates editor.
     */
    public void setKeywords(String[] keywords) {
        this.keywords = keywords;
        this.setupRules();
    }
}
