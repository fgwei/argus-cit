/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 14, 2006
 */
package iamandroid.core;

import iamandroid.natures.IPilarNature;

public interface IDefinition {

    IModule getModule();

    int getLine();

    int getCol();

    /**
     * @return the docstring for the definition.
     */
    String getDocstring(IPilarNature nature, ICompletionCache cache);

}