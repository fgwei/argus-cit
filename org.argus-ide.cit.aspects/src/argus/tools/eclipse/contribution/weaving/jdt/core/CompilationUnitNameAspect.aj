package argus.tools.eclipse.contribution.weaving.jdt.core;

import org.eclipse.jdt.internal.core.util.Util;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.internal.core.JavaModelStatus;

/**
 * We override the behaviour of isValidCompilationUnitName() for .pilar & .plr files.
 * The standard implementation applies Java identifier rules on the prefix of
 * the file name, so that, for example, "package.pilar" would not be judged
 * valid.
 */
@SuppressWarnings("restriction")
public aspect CompilationUnitNameAspect {

	private static boolean isPilarFileName(String name) {
		return name != null && (name.endsWith(".pilar") || name.endsWith(".plr"));
	}

	pointcut isValidCompilationUnitName(String name, String sourceLevel, String complianceLevel):
	    args(name, sourceLevel, complianceLevel) &&
		execution(boolean Util.isValidCompilationUnitName(String, String, String));

	boolean around(String name, String sourceLevel, String complianceLevel):
	    isValidCompilationUnitName(name, sourceLevel, complianceLevel) {
		if (isPilarFileName(name))
			return true;
		else
			return proceed(name, sourceLevel, complianceLevel);
	}

	pointcut validateCompilationUnitName(String name, String sourceLevel, String complianceLevel):
	    args(name, sourceLevel, complianceLevel) &&
		execution(IStatus JavaConventions.validateCompilationUnitName(String, String, String));

	IStatus around(String name, String sourceLevel, String complianceLevel):
	    validateCompilationUnitName(name, sourceLevel, complianceLevel) {
		if (isPilarFileName(name))
			return JavaModelStatus.VERIFIED_OK;
		else
			return proceed(name, sourceLevel, complianceLevel);
	}
}
