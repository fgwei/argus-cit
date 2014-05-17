/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package iamandroid.editors;

import iamandroid.cache.ColorAndStyleCache;
import iamandroid.core.IIndentPrefs;
import iamandroid.natures.IPilarNature;
import iamandroid.utils.MisconfigurationException;

import java.io.File;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.source.ISourceViewer;

/**
 * This is the interface needed for an editor that has syntax highlighting and code-completion
 * (used by the PilarEdit and PilarMergeViewer -- in the compare editor).
 */
public interface IPilarSyntaxHighlightingAndCodeCompletionEditor extends IAdaptable {

    IIndentPrefs getIndentPrefs();

    ISourceViewer getEditorSourceViewer();

    void resetForceTabs();

    ColorAndStyleCache getColorCache();

    PilarConfigurationWithoutEditor getEditConfiguration();

    PilarSelection createPySelection();

    IPilarNature getPilarNature() throws MisconfigurationException;

    File getEditorFile();

    void resetIndentPrefixes();

}
