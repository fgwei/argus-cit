/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 12/06/2005
 */
package iamandroid.natures;

import iamandroid.core.ICodeCompletionASTManager;
import iamandroid.core.IGrammarVersionProvider;
import iamandroid.core.IModule;
import iamandroid.core.IToken;
import iamandroid.utils.MisconfigurationException;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * @author Fabio
 */
public interface IPilarNature extends IProjectNature, IGrammarVersionProvider {

    /**
     * Helper class to contain information about the versions
     */
    public static class Versions {
        public static final HashSet<String> ALL_PILAR_VERSIONS = new HashSet<String>();
        public static final HashSet<String> ALL_VERSIONS_ANY_FLAVOR = new HashSet<String>();
        public static final List<String> VERSION_NUMBERS = new ArrayList<String>();
        public static final String LAST_VERSION_NUMBER = "4.0";

        static {
            ALL_PILAR_VERSIONS.add(PILAR_VERSION_4_0);
            
            VERSION_NUMBERS.add("4.0");

            ALL_VERSIONS_ANY_FLAVOR.addAll(ALL_PILAR_VERSIONS);
        }
    }

    /**
     * Constants persisted. Probably a better way would be disassociating whether it's python/jython and the 
     * grammar version to be used (to avoid the explosion of constants below).
     */
    public static final String PILAR_VERSION_4_0 = "pilar 4.0";

    //NOTE: It's the latest in the 2 series (3 is as if it's a totally new thing)
    public static final String PILAR_VERSION_LATEST = PILAR_VERSION_4_0;

    /**
     * this id is provided so that we can have an identifier for python-related things (independent of its version)
     */
    public static final int INTERPRETER_TYPE_PYTHON = 0;

    /**
     * this id is provided so that we can have an identifier for jython-related things (independent of its version)
     */
    public static final int INTERPRETER_TYPE_JYTHON = 1;

    /**
     * This id is provided so that we can have an identifier for ironpython-related things (independent of its version)
     */
    public static final int INTERPRETER_TYPE_IRONPYTHON = 2;

    /**
     * Identifies an interpreter that will use the jython in the running eclipse platform.
     */
    public static final int INTERPRETER_TYPE_JYTHON_ECLIPSE = 3;

    public static final String DEFAULT_INTERPRETER = "Default";

    /**
     * @return the project version given the constants provided
     * @throws CoreException 
     */
    String getVersion() throws CoreException;

    /**
     * @return the default version
     * @throws CoreException 
     */
    String getDefaultVersion();

    /**
     * set the project version given the constants provided
     * 
     * @see PYTHON_VERSION_XX
     * @see JYTHON_VERSION_XX
     * 
     * @throws CoreException 
     */
    void setVersion(String version, String interpreter) throws CoreException;

    /**
     * @return the id that is related to this nature given its type
     * 
     * @see #INTERPRETER_TYPE_PYTHON
     * @see #INTERPRETER_TYPE_JYTHON
     * @see #INTERPRETER_TYPE_IRONPYTHON
     */
    int getInterpreterType() throws CoreException;

    /**
     * @return the directory where the completions should be saved (as well as deltas)
     */
    public File getCompletionsCacheDir();

    /**
     * Saves the ast manager information so that we can retrieve it later.
     */
    public void saveAstManager();

    String resolveModule(File file) throws MisconfigurationException;

    String resolveModule(String fileAbsolutePath) throws MisconfigurationException;

    String resolveModule(IResource resource) throws MisconfigurationException;

    String resolveModuleOnlyInProjectSources(String fileAbsolutePath, boolean addExternal) throws CoreException,
            MisconfigurationException;

    String resolveModuleOnlyInProjectSources(IResource fileAbsolutePath, boolean addExternal) throws CoreException,
            MisconfigurationException;

    ICodeCompletionASTManager getAstManager();

    /**
     * Rebuilds the path with the current path information (just to refresh it).
     * @throws CoreException 
     */
    void rebuildPath();

    /**
     * @return the tokens for the builtins. As getting the builtins is VERY usual, we'll keep them here.
     * (we can't forget to change it when the interpreter is changed -- on rebuildPath)
     * 
     * May return null if not set
     */
    IToken[] getBuiltinCompletions();

    /**
     * @param toks those are the tokens that are set as builtin completions.
     */
    void clearBuiltinCompletions();

    /**
     * @return the module for the builtins (may return null if not set)
     */
    IModule getBuiltinMod();

    void clearBuiltinMod();

    /**
     * Checks if the given resource is in the pythonpath
     * @throws MisconfigurationException 
     */
    boolean isResourceInPythonpath(IResource resource) throws MisconfigurationException;

    boolean isResourceInPythonpath(String resource) throws MisconfigurationException;

    boolean isResourceInPythonpathProjectSources(IResource fileAdapter, boolean addExternal)
            throws MisconfigurationException, CoreException;

    boolean isResourceInPythonpathProjectSources(String resource, boolean addExternal)
            throws MisconfigurationException, CoreException;

    /**
     * @return true if it is ok to use the nature
     */
    boolean startRequests();

    void endRequests();

    /**
     * @return the configured interpreter that should be used to get the completions (must be the same string
     * of one of the configured interpreters in the preferences).
     * 
     * Must always be a valid path (e.g.: if the interpreter is internally configured as "Default", it should
     * return the actual path, not the internal representation).
     * 
     * Note: the return can never be null (an exception is thrown if none can be determined) 
     * @throws PythonNatureWithoutProjectException 
     */
//    IInterpreterInfo getProjectInterpreter() throws MisconfigurationException, PythonNatureWithoutProjectException;

    boolean isOkToUse();

}
