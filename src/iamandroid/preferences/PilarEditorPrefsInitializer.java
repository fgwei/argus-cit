/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 20/08/2005
 */
package iamandroid.preferences;

import iamandroid.AmandroidPlugin;
import iamandroid.utils.Log;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.StringConverter;
import org.osgi.service.prefs.Preferences;

public class PilarEditorPrefsInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore node = AmandroidPlugin.getDefault().getPreferenceStore();
        //text
        node.setDefault(PilarEditorPrefs.SMART_INDENT_PAR, PilarEditorPrefs.DEFAULT_SMART_INDENT_PAR);
        node.setDefault(PilarEditorPrefs.AUTO_PAR, PilarEditorPrefs.DEFAULT_AUTO_PAR);
        node.setDefault(PilarEditorPrefs.AUTO_LINK, PilarEditorPrefs.DEFAULT_AUTO_LINK);
        node.setDefault(PilarEditorPrefs.AUTO_INDENT_TO_PAR_LEVEL, PilarEditorPrefs.DEFAULT_AUTO_INDENT_TO_PAR_LEVEL);
        node.setDefault(PilarEditorPrefs.AUTO_DEDENT_ELSE, PilarEditorPrefs.DEFAULT_AUTO_DEDENT_ELSE);
        node.setDefault(PilarEditorPrefs.AUTO_INDENT_AFTER_PAR_WIDTH, PilarEditorPrefs.DEFAULT_AUTO_INDENT_AFTER_PAR_WIDTH);
        node.setDefault(PilarEditorPrefs.AUTO_COLON, PilarEditorPrefs.DEFAULT_AUTO_COLON);
        node.setDefault(PilarEditorPrefs.AUTO_BRACES, PilarEditorPrefs.DEFAULT_AUTO_BRACES);
//        node.putBoolean(PilarEditorPrefs.AUTO_WRITE_IMPORT_STR, PilarEditorPrefs.DEFAULT_AUTO_WRITE_IMPORT_STR);
        node.setDefault(PilarEditorPrefs.AUTO_LITERALS, PilarEditorPrefs.DEFAULT_AUTO_LITERALS);
        node.setDefault(PilarEditorPrefs.SMART_LINE_MOVE, PilarEditorPrefs.DEFAULT_SMART_LINE_MOVE);

        node.setDefault(PilarEditorPrefs.TAB_WIDTH, PilarEditorPrefs.DEFAULT_TAB_WIDTH);
//        node.putInt(IWizardNewProjectNameAndLocationPage.PYDEV_NEW_PROJECT_CREATE_PREFERENCES,
//                IWizardNewProjectNameAndLocationPage.PYDEV_NEW_PROJECT_CREATE_PROJECT_AS_SRC_FOLDER);

        //comment blocks
//        node.put(CommentBlocksPreferences.MULTI_BLOCK_COMMENT_CHAR,
//                CommentBlocksPreferences.DEFAULT_MULTI_BLOCK_COMMENT_CHAR);
//        node.putBoolean(CommentBlocksPreferences.MULTI_BLOCK_COMMENT_SHOW_ONLY_CLASS_NAME,
//                CommentBlocksPreferences.DEFAULT_MULTI_BLOCK_COMMENT_SHOW_ONLY_CLASS_NAME);
//        node.putBoolean(CommentBlocksPreferences.MULTI_BLOCK_COMMENT_SHOW_ONLY_FUNCTION_NAME,
//                CommentBlocksPreferences.DEFAULT_MULTI_BLOCK_COMMENT_SHOW_ONLY_FUNCTION_NAME);
//        node.put(CommentBlocksPreferences.SINGLE_BLOCK_COMMENT_CHAR,
//                CommentBlocksPreferences.DEFAULT_SINGLE_BLOCK_COMMENT_CHAR);
//        node.putBoolean(CommentBlocksPreferences.SINGLE_BLOCK_COMMENT_ALIGN_RIGHT,
//                CommentBlocksPreferences.DEFAULT_SINGLE_BLOCK_COMMENT_ALIGN_RIGHT);

        //checkboxes
        node.setDefault(PilarEditorPrefs.SUBSTITUTE_TABS, PilarEditorPrefs.DEFAULT_SUBSTITUTE_TABS);
//        node.putBoolean(PilarEditorPrefs.AUTO_ADD_SELF, PilarEditorPrefs.DEFAULT_AUTO_ADD_SELF);
        node.setDefault(PilarEditorPrefs.GUESS_TAB_SUBSTITUTION, PilarEditorPrefs.DEFAULT_GUESS_TAB_SUBSTITUTION);

        //matching
        node.setDefault(PilarEditorPrefs.USE_MATCHING_BRACKETS, PilarEditorPrefs.DEFAULT_USE_MATCHING_BRACKETS);
        node.setDefault(PilarEditorPrefs.MATCHING_BRACKETS_COLOR,
                StringConverter.asString(PilarEditorPrefs.DEFAULT_MATCHING_BRACKETS_COLOR));
        node.setDefault(PilarEditorPrefs.MATCHING_BRACKETS_STYLE, PilarEditorPrefs.DEFAULT_MATCHING_BRACKETS_STYLE);

        //colors
        node.setDefault(PilarEditorPrefs.CODE_COLOR, StringConverter.asString(PilarEditorPrefs.DEFAULT_CODE_COLOR));
        node.setDefault(PilarEditorPrefs.NUMBER_COLOR, StringConverter.asString(PilarEditorPrefs.DEFAULT_NUMBER_COLOR));
        node.setDefault(PilarEditorPrefs.ANNOTATION_COLOR, StringConverter.asString(PilarEditorPrefs.DEFAULT_ANNOTATION_COLOR));
        node.setDefault(PilarEditorPrefs.LOC_COLOR, StringConverter.asString(PilarEditorPrefs.DEFAULT_LOC_COLOR));
        node.setDefault(PilarEditorPrefs.KEYWORD_COLOR, StringConverter.asString(PilarEditorPrefs.DEFAULT_KEYWORD_COLOR));
//        node.put(PilarEditorPrefs.SELF_COLOR, StringConverter.asString(PilarEditorPrefs.DEFAULT_SELF_COLOR));
        node.setDefault(PilarEditorPrefs.STRING_COLOR, StringConverter.asString(PilarEditorPrefs.DEFAULT_STRING_COLOR));
        node.setDefault(PilarEditorPrefs.COMMENT_COLOR, StringConverter.asString(PilarEditorPrefs.DEFAULT_COMMENT_COLOR));
//        node.put(PilarEditorPrefs.BACKQUOTES_COLOR, StringConverter.asString(PilarEditorPrefs.DEFAULT_BACKQUOTES_COLOR));
        node.setDefault(PilarEditorPrefs.RECORD_NAME_COLOR, StringConverter.asString(PilarEditorPrefs.DEFAULT_RECORD_NAME_COLOR));
        node.setDefault(PilarEditorPrefs.PROCEDURE_NAME_COLOR, StringConverter.asString(PilarEditorPrefs.DEFAULT_PROCEDURE_NAME_COLOR));
        node.setDefault(PilarEditorPrefs.PARENS_COLOR, StringConverter.asString(PilarEditorPrefs.DEFAULT_PARENS_COLOR));
        node.setDefault(PilarEditorPrefs.OPERATORS_COLOR, StringConverter.asString(PilarEditorPrefs.DEFAULT_OPERATORS_COLOR));
        node.setDefault(PilarEditorPrefs.DOCSTRING_MARKUP_COLOR,
                StringConverter.asString(PilarEditorPrefs.DEFAULT_DOCSTRING_MARKUP_COLOR));
        //for selection colors see initializeDefaultColors()

        //font style
        node.setDefault(PilarEditorPrefs.CODE_STYLE, PilarEditorPrefs.DEFAULT_CODE_STYLE);
        node.setDefault(PilarEditorPrefs.NUMBER_STYLE, PilarEditorPrefs.DEFAULT_NUMBER_STYLE);
        node.setDefault(PilarEditorPrefs.ANNOTATION_STYLE, PilarEditorPrefs.DEFAULT_ANNOTATION_STYLE);
        node.setDefault(PilarEditorPrefs.LOC_STYLE, PilarEditorPrefs.DEFAULT_LOC_STYLE);
        node.setDefault(PilarEditorPrefs.KEYWORD_STYLE, PilarEditorPrefs.DEFAULT_KEYWORD_STYLE);
//        node.setDefault(PilarEditorPrefs.SELF_STYLE, PilarEditorPrefs.DEFAULT_SELF_STYLE);
        node.setDefault(PilarEditorPrefs.STRING_STYLE, PilarEditorPrefs.DEFAULT_STRING_STYLE);
        node.setDefault(PilarEditorPrefs.COMMENT_STYLE, PilarEditorPrefs.DEFAULT_COMMENT_STYLE);
//        node.setDefault(PilarEditorPrefs.BACKQUOTES_STYLE, PilarEditorPrefs.DEFAULT_BACKQUOTES_STYLE);
        node.setDefault(PilarEditorPrefs.RECORD_NAME_STYLE, PilarEditorPrefs.DEFAULT_RECORD_NAME_STYLE);
        node.setDefault(PilarEditorPrefs.PROCEDURE_NAME_STYLE, PilarEditorPrefs.DEFAULT_PROCEDURE_NAME_STYLE);
        node.setDefault(PilarEditorPrefs.PARENS_STYLE, PilarEditorPrefs.DEFAULT_PARENS_STYLE);
        node.setDefault(PilarEditorPrefs.OPERATORS_STYLE, PilarEditorPrefs.DEFAULT_OPERATORS_STYLE);
        node.setDefault(PilarEditorPrefs.DOCSTRING_MARKUP_STYLE, PilarEditorPrefs.DEFAULT_DOCSTRING_MARKUP_STYLE);

        //Debugger
//        node.putInt(PilarEditorPrefs.CONNECT_TIMEOUT, PilarEditorPrefs.DEFAULT_CONNECT_TIMEOUT);
//        node.putBoolean(PilarEditorPrefs.RELOAD_MODULE_ON_CHANGE, PilarEditorPrefs.DEFAULT_RELOAD_MODULE_ON_CHANGE);
//        node.putBoolean(PilarEditorPrefs.DONT_TRACE_ENABLED, PilarEditorPrefs.DEFAULT_DONT_TRACE_ENABLED);
//        node.putBoolean(PilarEditorPrefs.DEBUG_MULTIPROCESSING_ENABLED,
//                PilarEditorPrefs.DEFAULT_DEBUG_MULTIPROCESSING_ENABLED);
//        node.putBoolean(PilarEditorPrefs.KILL_SUBPROCESSES_WHEN_TERMINATING_PROCESS,
//                PilarEditorPrefs.DEFAULT_KILL_SUBPROCESSES_WHEN_TERMINATING_PROCESS);

        //pydev todo tasks
//        node.put(PyTodoPrefPage.PY_TODO_TAGS, PyTodoPrefPage.DEFAULT_PY_TODO_TAGS);

        //builders
//        node.putBoolean(PyDevBuilderPrefPage.USE_PYDEV_BUILDERS, PyDevBuilderPrefPage.DEFAULT_USE_PYDEV_BUILDERS);
//        node.putBoolean(PyParserManager.USE_PYDEV_ANALYSIS_ONLY_ON_DOC_SAVE,
//                PyDevBuilderPrefPage.DEFAULT_USE_PYDEV_ONLY_ON_DOC_SAVE);
//        node.putInt(PyParserManager.PYDEV_ELAPSE_BEFORE_ANALYSIS,
//                PyDevBuilderPrefPage.DEFAULT_PYDEV_ELAPSE_BEFORE_ANALYSIS);
//        node.putBoolean(PyDevBuilderPrefPage.ANALYZE_ONLY_ACTIVE_EDITOR,
//                PyDevBuilderPrefPage.DEFAULT_ANALYZE_ONLY_ACTIVE_EDITOR);
//        node.putBoolean(PyDevBuilderPrefPage.REMOVE_ERRORS_WHEN_EDITOR_IS_CLOSED,
//                PyDevBuilderPrefPage.DEFAULT_REMOVE_ERRORS_WHEN_EDITOR_IS_CLOSED);
//        node.putInt(PyDevBuilderPrefPage.PYC_DELETE_HANDLING, PyDevBuilderPrefPage.DEFAULT_PYC_DELETE_HANDLING);

        //code folding
//        node.putBoolean(PyDevCodeFoldingPrefPage.USE_CODE_FOLDING, PyDevCodeFoldingPrefPage.DEFAULT_USE_CODE_FOLDING);
//        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_IF, PyDevCodeFoldingPrefPage.DEFAULT_FOLD_IF);
//        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_WHILE, PyDevCodeFoldingPrefPage.DEFAULT_FOLD_WHILE);
//        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_FOR, PyDevCodeFoldingPrefPage.DEFAULT_FOLD_FOR);
//        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_CLASSDEF, PyDevCodeFoldingPrefPage.DEFAULT_FOLD_CLASSDEF);
//        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_FUNCTIONDEF, PyDevCodeFoldingPrefPage.DEFAULT_FOLD_FUNCTIONDEF);
//        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_COMMENTS, PyDevCodeFoldingPrefPage.DEFAULT_FOLD_COMMENTS);
//        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_STRINGS, PyDevCodeFoldingPrefPage.DEFAULT_FOLD_STRINGS);
//        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_WITH, PyDevCodeFoldingPrefPage.DEFAULT_FOLD_WITH);
//        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_TRY, PyDevCodeFoldingPrefPage.DEFAULT_FOLD_TRY);
//        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_IMPORTS, PyDevCodeFoldingPrefPage.DEFAULT_FOLD_IMPORTS);

        //coding style
//        node.putBoolean(PyCodeStylePreferencesPage.USE_LOCALS_AND_ATTRS_CAMELCASE,
//                PyCodeStylePreferencesPage.DEFAULT_USE_LOCALS_AND_ATTRS_CAMELCASE);
//        node.putInt(PyCodeStylePreferencesPage.USE_METHODS_FORMAT,
//                PyCodeStylePreferencesPage.DEFAULT_USE_METHODS_FORMAT);

        //Editor title
//        node.putBoolean(PyTitlePreferencesPage.TITLE_EDITOR_NAMES_UNIQUE,
//                PyTitlePreferencesPage.DEFAULT_TITLE_EDITOR_NAMES_UNIQUE);
//        node.putBoolean(PyTitlePreferencesPage.TITLE_EDITOR_SHOW_EXTENSION,
//                PyTitlePreferencesPage.DEFAULT_TITLE_EDITOR_SHOW_EXTENSION);
//        node.putBoolean(PyTitlePreferencesPage.TITLE_EDITOR_CUSTOM_INIT_ICON,
//                PyTitlePreferencesPage.DEFAULT_TITLE_EDITOR_CUSTOM_INIT_ICON);
//        node.put(PyTitlePreferencesPage.TITLE_EDITOR_INIT_HANDLING,
//                PyTitlePreferencesPage.DEFAULT_TITLE_EDITOR_INIT_HANDLING);
//        node.put(PyTitlePreferencesPage.TITLE_EDITOR_DJANGO_MODULES_HANDLING,
//                PyTitlePreferencesPage.DEFAULT_TITLE_EDITOR_DJANGO_MODULES_HANDLING);

        //code formatting
        node.setDefault(PilarCodeFormatterPage.USE_ASSIGN_WITH_PACES_INSIDER_PARENTESIS,
                PilarCodeFormatterPage.DEFAULT_USE_ASSIGN_WITH_PACES_INSIDE_PARENTESIS);
        node.setDefault(PilarCodeFormatterPage.USE_OPERATORS_WITH_SPACE,
                PilarCodeFormatterPage.DEFAULT_USE_OPERATORS_WITH_SPACE);
        node.setDefault(PilarCodeFormatterPage.USE_SPACE_AFTER_COMMA, PilarCodeFormatterPage.DEFAULT_USE_SPACE_AFTER_COMMA);
        node.setDefault(PilarCodeFormatterPage.ADD_NEW_LINE_AT_END_OF_FILE,
                PilarCodeFormatterPage.DEFAULT_ADD_NEW_LINE_AT_END_OF_FILE);
        node.setDefault(PilarCodeFormatterPage.FORMAT_BEFORE_SAVING, PilarCodeFormatterPage.DEFAULT_FORMAT_BEFORE_SAVING);
        node.setDefault(PilarCodeFormatterPage.AUTO_FORMAT_ONLY_WORKSPACE_FILES,
                PilarCodeFormatterPage.DEFAULT_AUTO_FORMAT_ONLY_WORKSPACE_FILES);
        node.setDefault(PilarCodeFormatterPage.FORMAT_ONLY_CHANGED_LINES,
                PilarCodeFormatterPage.DEFAULT_FORMAT_ONLY_CHANGED_LINES);
        node.setDefault(PilarCodeFormatterPage.TRIM_LINES, PilarCodeFormatterPage.DEFAULT_TRIM_LINES);
        node.setDefault(PilarCodeFormatterPage.USE_SPACE_FOR_PARENTESIS,
                PilarCodeFormatterPage.DEFAULT_USE_SPACE_FOR_PARENTESIS);
        node.setDefault(PilarCodeFormatterPage.SPACES_BEFORE_COMMENT,
                PilarCodeFormatterPage.DEFAULT_SPACES_BEFORE_COMMENT);
        node.setDefault(PilarCodeFormatterPage.SPACES_IN_START_COMMENT,
                PilarCodeFormatterPage.DEFAULT_SPACES_IN_START_COMMENT);

        //initialize pyunit prefs
//        node.putInt(PyUnitPrefsPage2.TEST_RUNNER, PyUnitPrefsPage2.DEFAULT_TEST_RUNNER);
//        node.putBoolean(PyUnitPrefsPage2.USE_PYUNIT_VIEW, PyUnitPrefsPage2.DEFAULT_USE_PYUNIT_VIEW);
//        node.put(PyUnitPrefsPage2.TEST_RUNNER_DEFAULT_PARAMETERS,
//                PyUnitPrefsPage2.DEFAULT_TEST_RUNNER_DEFAULT_PARAMETERS);

        // Docstrings
//        node.put(DocstringsPrefPage.P_DOCSTRINGCHARACTER, DocstringsPrefPage.DEFAULT_P_DOCSTRINGCHARACTER);
//        node.put(DocstringsPrefPage.P_DOCSTRINGSTYLE, DocstringsPrefPage.DEFAULT_P_DOCSTIRNGSTYLE);
//        node.put(DocstringsPrefPage.P_TYPETAGGENERATION, DocstringsPrefPage.DEFAULT_P_TYPETAGGENERATION);
//        node.put(DocstringsPrefPage.P_DONT_GENERATE_TYPETAGS, DocstringsPrefPage.DEFAULT_P_DONT_GENERATE_TYPETAGS);

        //file types
        node.setDefault(FileTypesPreferencesPage.VALID_SOURCE_FILES, FileTypesPreferencesPage.DEFAULT_VALID_SOURCE_FILES);
        node.setDefault(FileTypesPreferencesPage.FIRST_CHOICE_PILAR_SOURCE_FILE,
                FileTypesPreferencesPage.DEFAULT_FIRST_CHOICE_PILAR_SOURCE_FILE);

        //imports
//        node.putBoolean(ImportsPreferencesPage.GROUP_IMPORTS, ImportsPreferencesPage.DEFAULT_GROUP_IMPORTS);
//        node.putBoolean(ImportsPreferencesPage.MULTILINE_IMPORTS, ImportsPreferencesPage.DEFAULT_MULTILINE_IMPORTS);
//        node.put(ImportsPreferencesPage.BREAK_IMPORTS_MODE, ImportsPreferencesPage.DEFAULT_BREAK_IMPORTS_MODE);
//        node.putBoolean(ImportsPreferencesPage.PEP8_IMPORTS, ImportsPreferencesPage.DEFAULT_PEP8_IMPORTS);
//        node.putBoolean(ImportsPreferencesPage.DELETE_UNUSED_IMPORTS,
//                ImportsPreferencesPage.DEFAULT_DELETE_UNUSED_IMPORTS);
//        node.putBoolean(ImportsPreferencesPage.FROM_IMPORTS_FIRST, ImportsPreferencesPage.DEFAULT_FROM_IMPORTS_FIRST);
//        node.putBoolean(ImportsPreferencesPage.SORT_NAMES_GROUPED, ImportsPreferencesPage.DEFAULT_SORT_NAMES_GROUPED);

        //hover
//        node.putBoolean(PyHoverPreferencesPage.SHOW_DOCSTRING_ON_HOVER,
//                PyHoverPreferencesPage.DEFAULT_SHOW_DOCSTRING_ON_HOVER);
//        node.putBoolean(PyHoverPreferencesPage.SHOW_DEBUG_VARIABLES_VALUES_ON_HOVER,
//                PyHoverPreferencesPage.DEFAULT_SHOW_DEBUG_VARIABLES_VALUES_ON_HOVER);

        //source locator
//        node.putInt(PySourceLocatorPrefs.ON_SOURCE_NOT_FOUND,
//                PySourceLocatorPrefs.DEFAULT_ON_FILE_NOT_FOUND_IN_DEBUGGER);
//        node.putInt(PySourceLocatorPrefs.FILE_CONTENTS_TIMEOUT, PySourceLocatorPrefs.DEFAULT_FILE_CONTENTS_TIMEOUT);

        //general interpreters
//        node.putBoolean(InterpreterGeneralPreferencesPage.NOTIFY_NO_INTERPRETER_PY,
//                InterpreterGeneralPreferencesPage.DEFAULT_NOTIFY_NO_INTERPRETER_PY);
//        node.putBoolean(InterpreterGeneralPreferencesPage.NOTIFY_NO_INTERPRETER_JY,
//                InterpreterGeneralPreferencesPage.DEFAULT_NOTIFY_NO_INTERPRETER_JY);
//        node.putBoolean(InterpreterGeneralPreferencesPage.NOTIFY_NO_INTERPRETER_IP,
//                InterpreterGeneralPreferencesPage.DEFAULT_NOTIFY_NO_INTERPRETER_IP);
//
//        node.putBoolean(InterpreterGeneralPreferencesPage.CHECK_CONSISTENT_ON_STARTUP,
//                InterpreterGeneralPreferencesPage.DEFAULT_CHECK_CONSISTENT_ON_STARTUP);
//
//        node.putBoolean(InterpreterGeneralPreferencesPage.UPDATE_INTERPRETER_INFO_ON_FILESYSTEM_CHANGES,
//                InterpreterGeneralPreferencesPage.DEFAULT_UPDATE_INTERPRETER_INFO_ON_FILESYSTEM_CHANGES);

        //save actions
//        node.putBoolean(PydevSaveActionsPrefPage.SORT_IMPORTS_ON_SAVE,
//                PydevSaveActionsPrefPage.DEFAULT_SORT_IMPORTS_ON_SAVE);
//
//        node.putBoolean(PydevSaveActionsPrefPage.ENABLE_DATE_FIELD_ACTION,
//                PydevSaveActionsPrefPage.DEFAULT_ENABLE_DATE_FIELD_ACTION);
//
//        node.put(PydevSaveActionsPrefPage.DATE_FIELD_NAME, PydevSaveActionsPrefPage.DEFAULT_DATE_FIELD_NAME);
//        node.put(PydevSaveActionsPrefPage.DATE_FIELD_FORMAT, PydevSaveActionsPrefPage.DEFAULT_DATE_FIELD_FORMAT);

        //root
//        node.putBoolean(PydevRootPrefs.CHECK_PREFERRED_PYDEV_SETTINGS,
//                PydevRootPrefs.DEFAULT_CHECK_PREFERRED_PYDEV_SETTINGS);

    }

}
