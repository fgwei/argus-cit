package argus.tools.eclipse.contribution.weaving.jdt.ui.javaeditor;

import org.eclipse.jdt.ui.text.JavaTextTools;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.jface.text.IDocumentPartitioner;

public aspect ArgusPartitionerAspect {
  
  pointcut doSetInput(AbstractTextEditor editor) :
    execution(void AbstractTextEditor.doSetInput(IEditorInput)) &&
    target(editor);
  
  pointcut createDocumentPartitioner() :
    call(IDocumentPartitioner JavaTextTools.createDocumentPartitioner());
          
  IDocumentPartitioner around(AbstractTextEditor editor) :
           createDocumentPartitioner() &&
   cflow(doSetInput(editor)) {
   if (editor instanceof IArgusEditor)
     return ((IArgusEditor)editor).createDocumentPartitioner();
   else
     return proceed(editor);
  }
}
