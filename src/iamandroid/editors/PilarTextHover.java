package iamandroid.editors;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ISourceViewer;

public class PilarTextHover implements ITextHover {

	public PilarTextHover(ISourceViewer sourceViewer, String contentType) {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		try {
      IDocument doc = textViewer.getDocument();
//      EscriptModel em = EscriptModel.getModel(doc, null);
//      return em.getElementAt(r.getOffset()).getHoverHelp();
      return "Hover";
		}
		catch (Exception e) {
			return "";
		}
	}

	@Override
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		return new Region(offset, 0);
	}

}
