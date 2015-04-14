package argus.tools.eclipse.contribution.weaving.jdt.ui.javaeditor;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;

@SuppressWarnings("restriction")
public privileged aspect ArgusCloseStringsAspect {
  pointcut setCloseStringsEnabled(boolean enabled) :
    args(enabled) &&
    execution(void CompilationUnitEditor.BracketInserter.setCloseStringsEnabled(boolean));
  
  pointcut invocations(IArgusEditor editor) :
    target(editor) &&
    (execution(void CompilationUnitEditor.createPartControl(Composite)) ||
     execution(void CompilationUnitEditor.handlePreferenceStoreChanged(PropertyChangeEvent)));
  
  void around(IArgusEditor editor, boolean enabled) :
    setCloseStringsEnabled(enabled) && cflow(invocations(editor)) {
    proceed(editor, false);
  }
}
