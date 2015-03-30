package amanide.editors;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

public class PilarContentOutlinePage extends ContentOutlinePage {
	public PilarContentOutlinePage(IDocumentProvider documentProvider,
			PilarEditor pilarEditor) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		TreeViewer viewer = getTreeViewer();
//		viewer.setContentProvider(new MyContentProvider());
//		viewer.setLabelProvider(new MyLabelProvider());
//		viewer.addSelectionChangedListener(this);
//		viewer.setInput(myInput);
	}

	public void setInput(IEditorInput editorInput) {
		// TODO Auto-generated method stub
		
	}
}
