/*
 * Copyright 2005-2010 LAMP/EPFL
 */
// $Id$

package argus.tools.eclipse.contribution.weaving.jdt;

import amanide.core.model.PilarCompilationUnit;

public interface IPilarRecordFile extends IPilarCompilationUnit {
	public String getSourceFileName();

	public PilarCompilationUnit getPilarCompilationUnit();
}
