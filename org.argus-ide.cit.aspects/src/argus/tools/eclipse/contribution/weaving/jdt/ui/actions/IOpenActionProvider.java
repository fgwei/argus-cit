package argus.tools.eclipse.contribution.weaving.jdt.ui.actions;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.ui.actions.OpenAction;

@SuppressWarnings("restriction")
public interface IOpenActionProvider {
	public OpenAction getOpenAction(JavaEditor editor);
}
