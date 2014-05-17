/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package iamandroid.preferences;

import iamandroid.AmandroidPlugin;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;

/**
 * Helper to deal with the pydev preferences.
 * 
 * @author Fabio
 */
public class AmandroidPrefs {

    /**
     * This is a preference store that combines the preferences for Amandroid with the general preferences for editors.
     */
    private static IPreferenceStore fChainedPrefStore;

    /**
     * @return the place where this plugin preferences are stored.
     */
    public static IPreferenceStore getPreferences() {
        return getPreferenceStore();
    }

    /**
     * @return a preference store that has the pydev preference store and the default editors text store
     */
    public synchronized static IPreferenceStore getChainedPrefStore() {
        if (AmandroidPrefs.fChainedPrefStore == null) {
            List<IPreferenceStore> stores = getDefaultStores(true);
            AmandroidPrefs.fChainedPrefStore = new ChainedPreferenceStore(
                    stores.toArray(new IPreferenceStore[stores.size()]));
        }
        return AmandroidPrefs.fChainedPrefStore;
    }

    public static List<IPreferenceStore> getDefaultStores(boolean addEditorsUIStore) {
        List<IPreferenceStore> stores = new ArrayList<IPreferenceStore>();
        stores.add(AmandroidPlugin.getDefault().getPreferenceStore());
        if (addEditorsUIStore) {
            stores.add(EditorsUI.getPreferenceStore());
        }
        return stores;
    }

    public static IPreferenceStore getPreferenceStore() {
        return AmandroidPlugin.getDefault().getPreferenceStore();
    }
}
