/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package amanide.cache;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.TextAttribute;

import amanide.preferences.PilarEditorPrefs;

public class ColorAndStyleCache extends ColorCache {

    public ColorAndStyleCache(IPreferenceStore prefs) {
        super(prefs);
    }

    public static boolean isColorOrStyleProperty(String property) {
        if (property.endsWith("_COLOR") || property.endsWith("_STYLE")) {
            return true;
        }
        return false;
    }

    public TextAttribute getCodeTextAttribute() {

        return new TextAttribute(getNamedColor(
                PilarEditorPrefs.CODE_COLOR), null, preferences.getInt(PilarEditorPrefs.CODE_STYLE));
    }

    public TextAttribute getAnnotationTextAttribute() {

        return new TextAttribute(getNamedColor(
                PilarEditorPrefs.ANNOTATION_COLOR), null, preferences.getInt(PilarEditorPrefs.ANNOTATION_STYLE));
    }
    
    public TextAttribute getLocTextAttribute() {

	      return new TextAttribute(getNamedColor(
	              PilarEditorPrefs.LOC_COLOR), null, preferences.getInt(PilarEditorPrefs.LOC_STYLE));
	  }

    public TextAttribute getNumberTextAttribute() {

        return new TextAttribute(getNamedColor(
                PilarEditorPrefs.NUMBER_COLOR), null, preferences.getInt(PilarEditorPrefs.NUMBER_STYLE));
    }

    public TextAttribute getRecordNameTextAttribute() {

        return new TextAttribute(getNamedColor(
                PilarEditorPrefs.RECORD_NAME_COLOR), null, preferences.getInt(PilarEditorPrefs.RECORD_NAME_STYLE));
    }

    public TextAttribute getProcedureNameTextAttribute() {

        return new TextAttribute(getNamedColor(
                PilarEditorPrefs.PROCEDURE_NAME_COLOR), null, preferences.getInt(PilarEditorPrefs.PROCEDURE_NAME_STYLE));
    }

    public TextAttribute getCommentTextAttribute() {

        return new TextAttribute(getNamedColor(
                PilarEditorPrefs.COMMENT_COLOR), null, preferences.getInt(PilarEditorPrefs.COMMENT_STYLE));
    }

    public TextAttribute getStringTextAttribute() {

        return new TextAttribute(getNamedColor(
                PilarEditorPrefs.STRING_COLOR), null, preferences.getInt(PilarEditorPrefs.STRING_STYLE));
    }

    public TextAttribute getKeywordTextAttribute() {

        return new TextAttribute(getNamedColor(
                PilarEditorPrefs.KEYWORD_COLOR), null, preferences.getInt(PilarEditorPrefs.KEYWORD_STYLE));
    }

    public TextAttribute getParensTextAttribute() {

        return new TextAttribute(getNamedColor(
                PilarEditorPrefs.PARENS_COLOR), null, preferences.getInt(PilarEditorPrefs.PARENS_STYLE));
    }

    public TextAttribute getOperatorsTextAttribute() {

        return new TextAttribute(getNamedColor(
                PilarEditorPrefs.OPERATORS_COLOR), null, preferences.getInt(PilarEditorPrefs.OPERATORS_STYLE));
    }

    public TextAttribute getDocstringMarkupTextAttribute() {

        return new TextAttribute(getNamedColor(
                PilarEditorPrefs.DOCSTRING_MARKUP_COLOR), null,
                preferences.getInt(PilarEditorPrefs.DOCSTRING_MARKUP_STYLE));
    }
    //[[[end]]]

}
