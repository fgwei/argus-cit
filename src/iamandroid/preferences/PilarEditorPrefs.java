/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Fengguo Wei - Adapted for use in Amandroid
 *******************************************************************************/

package iamandroid.preferences;

import iamandroid.AmandroidPlugin;
import iamandroid.editors.StyledTextForShowingCodeFactory;
import iamandroid.editors.actions.PilarFormatStd;
import iamandroid.editors.actions.PilarFormatStd.FormatStd;
import iamandroid.ui.utils.RunInUiThread;
import iamandroid.utils.LinkFieldEditor;
import iamandroid.utils.Log;
import iamandroid.utils.Tuple;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

/**
 * The preference page for setting the editor options.
 * <p>
 * This class is internal and not intended to be used by clients.</p>
 */
public class PilarEditorPrefs extends AbstractPilarEditorPrefs {

    /**
     * Shows sample code with the new preferences.
     */
    private StyledText labelExample;

    /**
     * A local store that has the preferences set given the user configuration of colors.
     */
    private final IPreferenceStore localStore;

    /**
     * Helper to create the styled text and show the code later.
     */
    private StyledTextForShowingCodeFactory formatAndStyleRangeHelper;

    private IPropertyChangeListener updateLabelExampleOnPrefsChanges;

    public PilarEditorPrefs() {
        setDescription("Pilar editor appearance settings:");
        setPreferenceStore(AmandroidPlugin.getDefault().getPreferenceStore());

        fOverlayStore = createOverlayStore();
        localStore = new PreferenceStore();
    }

    @Override
    protected Control createAppearancePage(Composite parent) {
        Composite appearanceComposite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        appearanceComposite.setLayout(layout);

        addTextField(appearanceComposite, "Tab length:", TAB_WIDTH, 3, 0, true);

        addCheckBox(appearanceComposite, "Replace tabs with spaces when typing?", SUBSTITUTE_TABS, 0);

        addCheckBox(appearanceComposite, "Assume tab spacing when files contain tabs?", GUESS_TAB_SUBSTITUTION, 0);

        createColorOptions(appearanceComposite);

        formatAndStyleRangeHelper = new StyledTextForShowingCodeFactory();
        labelExample = formatAndStyleRangeHelper.createStyledTextForCodePresentation(appearanceComposite);
        updateLabelExample(PilarFormatStd.getFormat(), AmandroidPrefs.getChainedPrefStore());

        LinkFieldEditor colorsAndFontsLinkFieldEditor = new LinkFieldEditor("UNUSED",
                "Other settings:\n\n<a>Text Editors</a>: print margin, line numbers ...", appearanceComposite,
                new SelectionListener() {

                    public void widgetSelected(SelectionEvent e) {
                        String id = "org.eclipse.ui.preferencePages.GeneralTextEditor";
                        IWorkbenchPreferenceContainer workbenchPreferenceContainer = ((IWorkbenchPreferenceContainer) getContainer());
                        workbenchPreferenceContainer.openPage(id, null);
                    }

                    public void widgetDefaultSelected(SelectionEvent e) {
                    }
                });
        colorsAndFontsLinkFieldEditor.getLinkControl(appearanceComposite);

        colorsAndFontsLinkFieldEditor = new LinkFieldEditor("UNUSED",
                "<a>Colors and Fonts</a>: text font, content assist color ...", appearanceComposite,
                new SelectionListener() {

                    public void widgetSelected(SelectionEvent e) {
                        String id = "org.eclipse.ui.preferencePages.ColorsAndFonts";
                        IWorkbenchPreferenceContainer workbenchPreferenceContainer = ((IWorkbenchPreferenceContainer) getContainer());
                        workbenchPreferenceContainer.openPage(id, null);
                    }

                    public void widgetDefaultSelected(SelectionEvent e) {
                    }
                });
        colorsAndFontsLinkFieldEditor.getLinkControl(appearanceComposite);

        colorsAndFontsLinkFieldEditor = new LinkFieldEditor("UNUSED", "<a>Annotations</a>: occurrences, markers ...",
                appearanceComposite, new SelectionListener() {

                    public void widgetSelected(SelectionEvent e) {
                        String id = "org.eclipse.ui.editors.preferencePages.Annotations";
                        IWorkbenchPreferenceContainer workbenchPreferenceContainer = ((IWorkbenchPreferenceContainer) getContainer());
                        workbenchPreferenceContainer.openPage(id, null);
                    }

                    public void widgetDefaultSelected(SelectionEvent e) {
                    }
                });
        colorsAndFontsLinkFieldEditor.getLinkControl(appearanceComposite);

        return appearanceComposite;
    }

    private void createColorOptions(Composite appearanceComposite) {
        GridLayout layout;
        Label l = new Label(appearanceComposite, SWT.LEFT);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        gd.horizontalSpan = 2;
        gd.heightHint = convertHeightInCharsToPixels(1) / 2;
        l.setLayoutData(gd);

        l = new Label(appearanceComposite, SWT.LEFT);
        l.setText("Appearance color options:");
        gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        gd.horizontalSpan = 2;
        l.setLayoutData(gd);

        Composite editorComposite = new Composite(appearanceComposite, SWT.NONE);
        layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 2;
        layout.marginWidth = 0;
        editorComposite.setLayout(layout);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        gd.horizontalSpan = 2;
        editorComposite.setLayoutData(gd);

        fAppearanceColorList = new List(editorComposite, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
        gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
        gd.heightHint = convertHeightInCharsToPixels(8);
        fAppearanceColorList.setLayoutData(gd);

        Composite stylesComposite = new Composite(editorComposite, SWT.NONE);
        layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.numColumns = 2;
        stylesComposite.setLayout(layout);
        stylesComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        l = new Label(stylesComposite, SWT.LEFT);
        l.setText("Color:");
        gd = new GridData();
        gd.horizontalAlignment = GridData.BEGINNING;
        l.setLayoutData(gd);

        fAppearanceColorEditor = new ColorEditor(stylesComposite);
        Button foregroundColorButton = fAppearanceColorEditor.getButton();
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalAlignment = GridData.BEGINNING;
        foregroundColorButton.setLayoutData(gd);

        SelectionListener colorDefaultSelectionListener = new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                boolean systemDefault = fAppearanceColorDefault.getSelection();
                fAppearanceColorEditor.getButton().setEnabled(!systemDefault);

                int i = fAppearanceColorList.getSelectionIndex();
                String key = fAppearanceColorListModel[i][2];
                if (key != null) {
                    fOverlayStore.setValue(key, systemDefault);
                }
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
        };

        fAppearanceColorDefault = new Button(stylesComposite, SWT.CHECK);
        fAppearanceColorDefault.setText("System default");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalAlignment = GridData.BEGINNING;
        gd.horizontalSpan = 2;
        fAppearanceColorDefault.setLayoutData(gd);
        fAppearanceColorDefault.setVisible(false);
        fAppearanceColorDefault.addSelectionListener(colorDefaultSelectionListener);

        fAppearanceColorList.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {
                // do nothing
            }

            public void widgetSelected(SelectionEvent e) {
                handleAppearanceColorListSelection();
            }
        });
        foregroundColorButton.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {
                // do nothing
            }

            public void widgetSelected(SelectionEvent e) {
                int i = fAppearanceColorList.getSelectionIndex();
                String key = fAppearanceColorListModel[i][1];

                PreferenceConverter.setValue(fOverlayStore, key, fAppearanceColorEditor.getColorValue());
                onAppearanceRelatedPreferenceChanged();
            }
        });

        fFontBoldCheckBox = addStyleCheckBox(stylesComposite, "Bold");
        fFontItalicCheckBox = addStyleCheckBox(stylesComposite, "Italic");
    }

    public void updateLabelExample(FormatStd formatStd, IPreferenceStore store) {
        if (labelExample != null && !labelExample.isDisposed()) {
            String str = 
            				"// This is a example to show current format.\n" +
            				"record `Example`  @type class @AccessFlag PUBLIC {\n" +
                    "`boolean` `Example.tag` @AccessFlag PRIVATE;\n" +
                    "}\n" +
                    "global `int` `@@Example.i` @AccessFlag PUBLIC_STATIC_FINAL;\n" +
                    "procedure `void` `Example.foo` (`Example` v1 @type `this`) @owner `Example` @signature `LExample;.foo.()V` @Access PUBLIC_CONSTRUCTOR {\n" +
                    "   v0;\n" +
                    "   v1;\n" +
                    "\n" +
                    "   #L1. v0:= \"Test\" @type object;\n" +
                    "   #L2. v1:= new `java.lang.StringBuilder`;\n" +
                    "   #L3. v2:= 1 + 5;\n" +
                    "   #L4. return @void ;\n" +
                    "}\n" +
                    "";
            Tuple<String, StyleRange[]> result = formatAndStyleRangeHelper.formatAndGetStyleRanges(formatStd, str,
                    store, false);
            labelExample.setText(result.o1);
            labelExample.setStyleRanges(result.o2);
        }
    }

    @Override
    protected void onAppearanceRelatedPreferenceChanged() {
        localStore.setValue(KEYWORD_COLOR, fOverlayStore.getString(KEYWORD_COLOR));
        localStore.setValue(CODE_COLOR, fOverlayStore.getString(CODE_COLOR));
        localStore.setValue(ANNOTATION_COLOR, fOverlayStore.getString(ANNOTATION_COLOR));
        localStore.setValue(LOC_COLOR, fOverlayStore.getString(LOC_COLOR));
        localStore.setValue(NUMBER_COLOR, fOverlayStore.getString(NUMBER_COLOR));
        localStore.setValue(PROCEDURE_NAME_COLOR, fOverlayStore.getString(PROCEDURE_NAME_COLOR));
        localStore.setValue(RECORD_NAME_COLOR, fOverlayStore.getString(RECORD_NAME_COLOR));
        localStore.setValue(STRING_COLOR, fOverlayStore.getString(STRING_COLOR));
        localStore.setValue(COMMENT_COLOR, fOverlayStore.getString(COMMENT_COLOR));
        localStore.setValue(PARENS_COLOR, fOverlayStore.getString(PARENS_COLOR));
        localStore.setValue(OPERATORS_COLOR, fOverlayStore.getString(OPERATORS_COLOR));
        localStore.setValue(DOCSTRING_MARKUP_COLOR, fOverlayStore.getString(DOCSTRING_MARKUP_COLOR));

        localStore.setValue(KEYWORD_STYLE, fOverlayStore.getInt(KEYWORD_STYLE));
        localStore.setValue(CODE_STYLE, fOverlayStore.getInt(CODE_STYLE));
        localStore.setValue(ANNOTATION_STYLE, fOverlayStore.getInt(ANNOTATION_STYLE));
        localStore.setValue(LOC_STYLE, fOverlayStore.getInt(LOC_STYLE));
        localStore.setValue(NUMBER_STYLE, fOverlayStore.getInt(NUMBER_STYLE));
        localStore.setValue(PROCEDURE_NAME_STYLE, fOverlayStore.getInt(PROCEDURE_NAME_STYLE));
        localStore.setValue(RECORD_NAME_STYLE, fOverlayStore.getInt(RECORD_NAME_STYLE));
        localStore.setValue(STRING_STYLE, fOverlayStore.getInt(STRING_STYLE));
        localStore.setValue(COMMENT_STYLE, fOverlayStore.getInt(COMMENT_STYLE));
        localStore.setValue(PARENS_STYLE, fOverlayStore.getInt(PARENS_STYLE));
        localStore.setValue(OPERATORS_STYLE, fOverlayStore.getInt(OPERATORS_STYLE));
        localStore.setValue(DOCSTRING_MARKUP_STYLE, fOverlayStore.getInt(DOCSTRING_MARKUP_STYLE));

        this.updateLabelExample(PilarFormatStd.getFormat(), localStore);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (formatAndStyleRangeHelper != null) {
            formatAndStyleRangeHelper.dispose();
            formatAndStyleRangeHelper = null;
        }
        if (updateLabelExampleOnPrefsChanges != null) {
            AmandroidPrefs.getChainedPrefStore().removePropertyChangeListener(updateLabelExampleOnPrefsChanges);
            updateLabelExampleOnPrefsChanges = null;
        }
        if (labelExample != null) {
            try {
                labelExample.dispose();
            } catch (Exception e) {
                Log.log(e);
            }
            labelExample = null;
        }
    }

    public void setUpdateLabelExampleOnPrefsChanges() {
        updateLabelExampleOnPrefsChanges = new IPropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {
                RunInUiThread.async(new Runnable() {

                    public void run() {
                        updateLabelExample(PilarFormatStd.getFormat(), AmandroidPrefs.getChainedPrefStore());
                    }
                });
            }
        };
        AmandroidPrefs.getChainedPrefStore().addPropertyChangeListener(updateLabelExampleOnPrefsChanges);

    }
}