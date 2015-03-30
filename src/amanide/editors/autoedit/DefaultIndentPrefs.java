/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 5, 2005
 * 
 * @author Fabio Zadrozny
 */
package amanide.editors.autoedit;

import amanide.AmanIDEPlugin;
import amanide.cache.PilarPreferencesCache;
import amanide.core.IIndentPrefs;
import amanide.preferences.PilarEditorPrefs;


/**
 * Provides indentation preferences from the preferences set in the preferences pages within eclipse.
 */
public class DefaultIndentPrefs extends AbstractIndentPrefs {

    /** 
     * Cache for indentation string 
     */
    private String indentString = null;

    private boolean useSpaces;

    private int tabWidth;

    private static PilarPreferencesCache cache;

    /**
     * Singleton instance for the preferences
     */
    private static IIndentPrefs indentPrefs;

    /**
     * Should only be used on tests (and on a finally it should be set to null again in the test).
     */
    public synchronized static void set(IIndentPrefs indentPrefs) {
        DefaultIndentPrefs.indentPrefs = indentPrefs;
    }

    /**
     * @return the indentation preferences to be used
     */
    public synchronized static IIndentPrefs get() {
        if (indentPrefs == null) {
            indentPrefs = new DefaultIndentPrefs();
        }
        return indentPrefs;
    }

    /**
     * @return a cache for the preferences.
     */
    private PilarPreferencesCache getCache() {
        if (cache == null) {
            cache = new PilarPreferencesCache(AmanIDEPlugin.getDefault().getPreferenceStore());
        }
        return cache;
    }

    /**
     * Not singleton (each pyedit may force to use tabs or not).
     */
    public DefaultIndentPrefs() {
        PilarPreferencesCache c = getCache();
        useSpaces = c.getBoolean(PilarEditorPrefs.SUBSTITUTE_TABS);
        tabWidth = c.getInt(PilarEditorPrefs.TAB_WIDTH, 4);
    }

    public boolean getUseSpaces(boolean considerForceTabs) {
        PilarPreferencesCache c = getCache();
        if (useSpaces != c.getBoolean(PilarEditorPrefs.SUBSTITUTE_TABS)) {
            useSpaces = c.getBoolean(PilarEditorPrefs.SUBSTITUTE_TABS);
            regenerateIndentString();
        }
        if (considerForceTabs && getForceTabs()) {
            return false; //forcing tabs.
        }
        return useSpaces;
    }

    @Override
    public void setForceTabs(boolean forceTabs) {
        super.setForceTabs(forceTabs);
        regenerateIndentString(); //When forcing tabs, we must update the cache.
    }

    public static int getStaticTabWidth() {
        AmanIDEPlugin default1 = AmanIDEPlugin.getDefault();
        if (default1 == null) {
            return 4;
        }
        int w = default1.getPluginPreferences().getInt(PilarEditorPrefs.TAB_WIDTH);
        if (w <= 0) { //tab width should never be 0 or less (in this case, let's make the default 4)
            w = 4;
        }
        return w;
    }

    public int getTabWidth() {
        PilarPreferencesCache c = getCache();
        if (tabWidth != c.getInt(PilarEditorPrefs.TAB_WIDTH, 4)) {
            tabWidth = c.getInt(PilarEditorPrefs.TAB_WIDTH, 4);
            regenerateIndentString();
        }
        return tabWidth;
    }

    public void regenerateIndentString() {
        PilarPreferencesCache c = getCache();
        c.clear(PilarEditorPrefs.TAB_WIDTH);
        c.clear(PilarEditorPrefs.SUBSTITUTE_TABS);
        indentString = super.getIndentationString();
    }

    /**
     * This class also puts the indentation string in a cache and redoes it 
     * if the preferences are changed.
     * 
     * @return the indentation string. 
     */
    @Override
    public String getIndentationString() {
        if (indentString == null) {
            regenerateIndentString();
        }

        return indentString;
    }

    /** 
     * @see org.python.pydev.core.IIndentPrefs#getAutoParentesis()
     */
    public boolean getAutoParentesis() {
        return getCache().getBoolean(PilarEditorPrefs.AUTO_PAR);
    }

    public boolean getAutoLink() {
        return getCache().getBoolean(PilarEditorPrefs.AUTO_LINK);
    }

    public boolean getIndentToParLevel() {
        return getCache().getBoolean(PilarEditorPrefs.AUTO_INDENT_TO_PAR_LEVEL);
    }

    public boolean getAutoColon() {
        return getCache().getBoolean(PilarEditorPrefs.AUTO_COLON);
    }

    public boolean getAutoBraces() {
        return getCache().getBoolean(PilarEditorPrefs.AUTO_BRACES);
    }

    public boolean getSmartIndentPar() {
        return getCache().getBoolean(PilarEditorPrefs.SMART_INDENT_PAR);
    }

    public boolean getAutoDedentElse() {
        return getCache().getBoolean(PilarEditorPrefs.AUTO_DEDENT_ELSE);
    }

    public int getIndentAfterParWidth() {
        return getCache().getInt(PilarEditorPrefs.AUTO_INDENT_AFTER_PAR_WIDTH, 1);
    }

    public boolean getSmartLineMove() {
        return getCache().getBoolean(PilarEditorPrefs.SMART_LINE_MOVE);
    }

    public boolean getAutoLiterals() {
        return getCache().getBoolean(PilarEditorPrefs.AUTO_LITERALS);
    }

}