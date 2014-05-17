/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package iamandroid.preferences;

import iamandroid.AmandroidPlugin;
import iamandroid.preferences.CheckDefaultPreferencesDialog.CheckInfo;
import iamandroid.ui.dialogs.DialogHelpers;
import iamandroid.ui.field_editor.ButtonFieldEditor;
import iamandroid.utils.EditorUtils;
import iamandroid.utils.StringUtils;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class AmandroidRootPrefs extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String CHECK_PREFERRED_AMANDROID_SETTINGS = "CHECK_PREFERRED_AMANDROID_SETTINGS";
    public static final boolean DEFAULT_CHECK_PREFERRED_AMANDROID_SETTINGS = true;

    public AmandroidRootPrefs() {
        setDescription(StringUtils.format("Amandroid version: %s",
                AmandroidPlugin.getVersion()));
        setPreferenceStore(AmandroidPlugin.getDefault().getPreferenceStore());
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    @Override
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();

        final BooleanFieldEditor booleanField = new BooleanFieldEditor(CHECK_PREFERRED_AMANDROID_SETTINGS,
                "Check preferred Eclipse settings for Amandroid on editor open", p);
        addField(booleanField);

        addField(new ButtonFieldEditor("__UNUSED__", "Check preferred settings now.", p, new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                CheckInfo[] missing = CheckInfo.getMissing();
                if (missing.length == 0) {
                    DialogHelpers.openInfo("Checked",
                            "Preferences in Eclipse already match preferred Amandroid settings.");
                    return;
                }
                Shell shell = EditorUtils.getShell();
                CheckDefaultPreferencesDialog dialog = new CheckDefaultPreferencesDialog(shell, missing);
                dialog.open();
                booleanField.load();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        }));
    }

    public static void setCheckPreferredPydevSettings(boolean b) {
    		AmandroidPlugin.getDefault().getPreferenceStore().setValue(CHECK_PREFERRED_AMANDROID_SETTINGS, b);
    }

    public static boolean getCheckPreferredPydevSettings() {
        return AmandroidPlugin.getDefault().getPreferenceStore().getBoolean(CHECK_PREFERRED_AMANDROID_SETTINGS);
    }

}
