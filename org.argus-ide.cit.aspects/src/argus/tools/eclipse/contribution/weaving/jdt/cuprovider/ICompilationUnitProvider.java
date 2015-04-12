package argus.tools.eclipse.contribution.weaving.jdt.cuprovider;

import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.PackageFragment;

@SuppressWarnings("restriction")
public interface ICompilationUnitProvider {
	public CompilationUnit create(PackageFragment parent, String name, WorkingCopyOwner owner);
}
