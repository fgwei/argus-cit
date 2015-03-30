/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Sep 17, 2006
 * @author Fabio
 */
package amanide.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import amanide.utils.MisconfigurationException;

public interface IGrammarVersionProvider {

	public static final int GRAMMAR_PILAR_VERSION_4_0 = 1;
	public static final int LATEST_GRAMMAR_VERSION = GRAMMAR_PILAR_VERSION_4_0;

	/**
	 * @return the version of the grammar as defined in
	 *         IPythonNature.GRAMMAR_PYTHON_VERSION...
	 * @throws MisconfigurationException
	 */
	public int getGrammarVersion() throws MisconfigurationException;

	public static List<Integer> grammarVersions = GrammarsIterator.createList();

	public static Map<Integer, String> grammarVersionToRep = GrammarsIterator
			.createDict();

}

class GrammarsIterator {

	public static List<Integer> createList() {
		List<Integer> grammarVersions = new ArrayList<Integer>();
		grammarVersions.add(IGrammarVersionProvider.GRAMMAR_PILAR_VERSION_4_0);
		return grammarVersions;
	}

	public static Map<Integer, String> createDict() {
		HashMap<Integer, String> ret = new HashMap<Integer, String>();
		ret.put(IGrammarVersionProvider.GRAMMAR_PILAR_VERSION_4_0, "4.0");
		return ret;
	}
}
