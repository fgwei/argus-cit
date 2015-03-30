package amanide.editors;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;

public class PilarDocumentProvider extends TextFileDocumentProvider {

	private Map<Object, IAnnotationModel> moreInfo = new HashMap<Object, IAnnotationModel>();

  @Override
  public void connect(Object element) throws CoreException {
      super.connect(element);
  }

  @Override
  public IAnnotationModel getAnnotationModel(Object element) {
      IAnnotationModel annotationModel = super.getAnnotationModel(element);
      if (annotationModel == null) {
          annotationModel = moreInfo.get(element);
      }
      return annotationModel;
  }

  /**
   * The file buffer for the given element or null if no buffer is available.
   */
  public ITextFileBuffer getFileBuffer(Object element) {
      FileInfo fileInfo = super.getFileInfo(element);
      if (fileInfo == null) {
          return null;
      }
      return fileInfo.fTextFileBuffer;
  }

  /**
   * The instance that should be used when this provider is needed.
   */
  public final static PilarDocumentProvider instance = new PilarDocumentProvider();
	
}