/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 * 
 * Contributors:
 *     Appcelerator, Inc. - initial API and implementation
 *     Fengguo Wei - Adapted for use in Amandroid
 */
package amanide.editors;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;

import amanide.cache.ColorAndStyleCache;
import amanide.editors.actions.PilarFormatStd;
import amanide.editors.actions.PilarFormatStd.FormatStd;
import amanide.preferences.AmanIDEPrefs;
import amanide.utils.FastStringBuffer;
import amanide.utils.FontUtils;
import amanide.utils.IFontUsage;
import amanide.utils.StringUtils;
import amanide.utils.SyntaxErrorException;
import amanide.utils.Tuple;

/**
 * This class can create a styled text and later format a python code string and give style ranges for
 * that string so that it's properly highlighted with the colors in the passed preferences.  
 */
public class StyledTextForShowingCodeFactory implements IPropertyChangeListener {

    /**
     * The styled text returned.
     */
    private StyledText styledText;

    /**
     * Used to hold the background color (it cannot be disposed while we're using it).
     */
    private ColorAndStyleCache backgroundColorCache;

    /**
     * Used to hold other colors (always cleared when new preferences are set).
     */
    private ColorAndStyleCache colorCache;

    /**
     * @return a styled text that can be used to show code with the colors based on the color cache received.
     */
    public StyledText createStyledTextForCodePresentation(Composite parent) {
        styledText = new StyledText(parent, SWT.BORDER | SWT.READ_ONLY);
        this.backgroundColorCache = new ColorAndStyleCache(new PreferenceStore());
        this.colorCache = new ColorAndStyleCache(null);

        try {
            styledText.setFont(new Font(parent.getDisplay(), FontUtils.getFontData(IFontUsage.STYLED, true)));
        } catch (Throwable e) {
            //ignore
        }
        updateBackgroundColor();

        AmanIDEPrefs.getChainedPrefStore().addPropertyChangeListener(this);

        return styledText;
    }

    /**
     * Updates the color of the background.
     */
    private void updateBackgroundColor() {
        IPreferenceStore chainedPrefStore = AmanIDEPrefs.getChainedPrefStore();

        Color backgroundColor = null;
        if (!chainedPrefStore.getBoolean(PilarEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT)) {
            RGB backgroundRGB = PreferenceConverter.getColor(chainedPrefStore, PilarEditor.PREFERENCE_COLOR_BACKGROUND);
            backgroundColor = backgroundColorCache.getColor(backgroundRGB);
        }
        styledText.setBackground(backgroundColor);
    }

    /**
     * When the background changes, we need to update the background color (for the next refresh).
     */
    public void propertyChange(PropertyChangeEvent event) {
        String prop = event.getProperty();
        if (PilarEditor.PREFERENCE_COLOR_BACKGROUND.equals(prop)
                || PilarEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT.equals(prop)) {
            updateBackgroundColor();
        }
        ;
    }

    /**
     * It needs to be called so that we're properly garbage-collected and clear our caches.
     */
    public void dispose() {
        AmanIDEPrefs.getChainedPrefStore().removePropertyChangeListener(this);
        this.backgroundColorCache.dispose();
        this.colorCache.dispose();
    }

    /**
     * This method will format the passed string with the passed standard and create style ranges for the returned
     * string, so that the code is properly seen by the user in a StyledText.
     * 
     * @param formatStd the coding standard that should be used for the parse.
     * @param str the string that should be formatted and have the colors applied.
     * @param prefs the preferences that contain the colors to be used for each partition.
     * @param showSpacesAndNewLines if true, spaces will be shown as dots and new lines shown as a '\n' string
     *        (otherwise they're not visible).
     */
    @SuppressWarnings("unchecked")
    public Tuple<String, StyleRange[]> formatAndGetStyleRanges(FormatStd formatStd, String str, IPreferenceStore prefs,
            boolean showSpacesAndNewLines) {
        //When new preferences are set, the cache is reset (the background color doesn't need to be 
        //cleared because the colors are gotten from the rgb and not from the names).
        this.colorCache.setPreferences(prefs);

        PilarFormatStd formatter = new PilarFormatStd();
        try {
            Document doc = new Document(str);
            formatter.formatAll(doc, null, false, formatStd, false);
            str = doc.get();
        } catch (SyntaxErrorException e) {
        }
        FastStringBuffer buf = new FastStringBuffer();
        for (String line : StringUtils.splitInLines(str)) {
            buf.append(line);
            char c = buf.lastChar();
            if (c == '\n') {
                buf.deleteLast();
                if (showSpacesAndNewLines) {
                    buf.append("\\n");
                }
                //Adds chars so that the initial presentation is bigger (they are later changed for spaces).
                //If that's not done, the created editor would be too small... especially if we consider
                //that the code-formatting can change for that editor (so, some parts wouldn't appear if we
                //need more space later on).
                buf.appendN('|', 8);
                buf.append(c);
            }
        }
        String result = buf.toString();

        String finalResult;
        if (showSpacesAndNewLines) {
            finalResult = result.replace(' ', '.');
        } else {
            finalResult = result;
        }
        finalResult = finalResult.replace('|', ' ');

        PilarPartitionScanner pyPartitionScanner = new PilarPartitionScanner();
        FastPartitioner fastPartitioner = new FastPartitioner(pyPartitionScanner, IPilarPartitions.types);
        Document doc = new Document(result);
        fastPartitioner.connect(doc);

        TextPresentation textPresentation = new TextPresentation();
        PilarCodeScanner scanner = new PilarCodeScanner(colorCache);
        try {
            ITypedRegion[] computePartitioning = fastPartitioner.computePartitioning(0, doc.getLength());
            for (ITypedRegion region : computePartitioning) {
                String type = region.getType();
                int offset = region.getOffset();
                int len = region.getLength();
                if (IPilarPartitions.PILAR_DEFAULT.equals(type) || type == null) {
                    createDefaultRanges(textPresentation, scanner, doc, offset, len);

                } else if (IPilarPartitions.PILAR_SINGLELINE_COMMENT.equals(type)
                				|| IPilarPartitions.PILAR_MULTILINE_COMMENT.equals(type)) {
                    TextAttribute textAttribute = colorCache.getCommentTextAttribute();
                    textPresentation.addStyleRange(new StyleRange(offset, len, textAttribute.getForeground(), null,
                            textAttribute.getStyle()));
                    
                } else if (IPilarPartitions.PILAR_LOC.equals(type)) {
                  TextAttribute textAttribute = colorCache.getLocTextAttribute();
                  textPresentation.addStyleRange(new StyleRange(offset, len, textAttribute.getForeground(), null,
                          textAttribute.getStyle()));

                } else if (IPilarPartitions.PILAR_MULTILINE_STRING.equals(type)
                        || IPilarPartitions.PILAR_SINGLELINE_STRING1.equals(type)
                        || IPilarPartitions.PILAR_SINGLELINE_STRING2.equals(type)) {
                    TextAttribute textAttribute = colorCache.getStringTextAttribute();
                    textPresentation.addStyleRange(new StyleRange(offset, len, textAttribute.getForeground(), null,
                            textAttribute.getStyle()));
                }
            }
        } finally {
            fastPartitioner.disconnect();
        }

        if (showSpacesAndNewLines) {
            for (int i = 0; i < result.length(); i++) {
                char curr = result.charAt(i);
                if (curr == '\\' && i + 1 < result.length() && result.charAt(i + 1) == 'n') {
                    textPresentation.mergeStyleRange(new StyleRange(i, 2, colorCache.getColor(new RGB(180, 180, 180)),
                            null));
                    i += 1;
                } else if (curr == ' ') {
                    int finalI = i;
                    for (; finalI < result.length() && result.charAt(finalI) == ' '; finalI++) {
                        //just iterate (the finalI will have the right value at the end).
                    }
                    textPresentation.mergeStyleRange(new StyleRange(i, finalI - i, colorCache.getColor(new RGB(180,
                            180, 180)), null));

                }
            }
        }

        ArrayList<StyleRange> list = new ArrayList<StyleRange>();
        Iterator<StyleRange> it = textPresentation.getAllStyleRangeIterator();
        while (it.hasNext()) {
            list.add(it.next());
        }
        StyleRange[] ranges = list.toArray(new StyleRange[list.size()]);
        return new Tuple<String, StyleRange[]>(finalResult, ranges);
    }

    /**
     * Creates the ranges from parsing the code with the PyCodeScanner.
     * 
     * @param textPresentation this is the container of the style ranges.
     * @param scanner the scanner used to parse the document.
     * @param doc document to parse.
     * @param partitionOffset the offset of the document we should parse.
     * @param partitionLen the length to be parsed.
     */
    private void createDefaultRanges(TextPresentation textPresentation, PilarCodeScanner scanner, Document doc,
            int partitionOffset, int partitionLen) {

        scanner.setRange(doc, partitionOffset, partitionLen);

        IToken nextToken = scanner.nextToken();
        while (!nextToken.isEOF()) {
            Object data = nextToken.getData();
            if (data instanceof TextAttribute) {
                TextAttribute textAttribute = (TextAttribute) data;
                int offset = scanner.getTokenOffset();
                int len = scanner.getTokenLength();
                Color foreground = textAttribute.getForeground();
                Color background = textAttribute.getBackground();
                int style = textAttribute.getStyle();
                textPresentation.addStyleRange(new StyleRange(offset, len, foreground, background, style));

            }
            nextToken = scanner.nextToken();
        }
    }

}
