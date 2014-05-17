package iamandroid.editors;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import iamandroid.AmandroidPlugin;
import iamandroid.cache.ColorAndStyleCache;
import iamandroid.callbacks.CallbackWithListeners;
import iamandroid.callbacks.ICallbackWithListeners;
import iamandroid.core.IGrammarVersionProvider;
import iamandroid.core.IIndentPrefs;
import iamandroid.core.IPilarFormatStdProvider;
import iamandroid.editors.autoedit.DefaultIndentPrefs;
import iamandroid.natures.IPilarNature;
import iamandroid.natures.PilarNature;
import iamandroid.preferences.AmandroidPrefs;
import iamandroid.preferences.CheckDefaultPreferencesDialog;
import iamandroid.preferences.PilarEditorPrefs;
import iamandroid.utils.EditorUtils;
import iamandroid.utils.Log;
import iamandroid.utils.MisconfigurationException;
import iamandroid.utils.TextSelectionUtils;
import iamandroid.utils.Tuple;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.DefaultRangeIndicator;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.sireum.pilar.ast.Model;
import org.sireum.pilar.parser.PilarParser;

import scala.Option;
import scala.Tuple2;
import scala.util.Left;

public class PilarEditor extends TextEditor implements IPilarFormatStdProvider, IPilarSyntaxHighlightingAndCodeCompletionEditor {

	private PilarContentOutlinePage outlinepage;
	
	public static final String PILAR_EDIT_CONTEXT = "#PilarEditorContext";
  public static final String PILAR_EDIT_RULER_CONTEXT = "#PilarEditorRulerContext";

  static public final String EDITOR_ID = "iamandroid.editor.PilarEditor";

  static public final String ACTION_OPEN = "OpenEditor";

  static private final Set<PilarEditor> currentlyOpenedEditors = new HashSet<PilarEditor>();
  static private final Object currentlyOpenedEditorsLock = new Object();

  /** color cache */
  private ColorAndStyleCache colorCache;

  // Listener waits for tab/spaces preferences that affect sourceViewer
  private IPropertyChangeListener prefListener;

  /** need it to support GUESS_TAB_SUBSTITUTION preference */
//  private PilarAutoIndentStrategy indentStrategy;

  /** need to hold onto it to support indentPrefix change through preferences */
  private PilarConfiguration editConfiguration;

  public PilarConfiguration getEditConfiguration() {
      return editConfiguration;
  }

  public IAnnotationModel getAnnotationModel() {
      final IDocumentProvider documentProvider = getDocumentProvider();
      if (documentProvider == null) {
          return null;
      }
      return documentProvider.getAnnotationModel(getEditorInput());
  }

  public ColorAndStyleCache getColorCache() {
      return colorCache;
  }

  /**
   * Important: keep for scripting
   */
  public PilarSelection createPySelection() {
      return new PilarSelection(this);
  }

  
  public TextSelectionUtils createTextSelectionUtils() {
      return new PilarSelection(this);
  }

  /**
   * AST that created python model
   */
//  private volatile SimpleNode ast;
//  private volatile long astModificationTimeStamp = -1;

  /**
   * The last parsing error description we got.
   */
//  private volatile ErrorDescription errorDescription;

  // ---------------------------- listeners stuff
  /**
   * Those are the ones that register with the PYDEV_PYEDIT_LISTENER extension point
   */
//  private static List<IPilarListener> editListeners;

  /**
   * This is the scripting engine that is binded to this interpreter.
   */
//  private PyEditScripting pyEditScripting;

  public final ICallbackWithListeners<Composite> onCreatePartControl = new CallbackWithListeners<Composite>();
  public final ICallbackWithListeners<ISourceViewer> onAfterCreatePartControl = new CallbackWithListeners<ISourceViewer>();
  public final ICallbackWithListeners<PilarEditor> onCreateActions = new CallbackWithListeners<PilarEditor>();
  public final ICallbackWithListeners<Class<?>> onGetAdapter = new CallbackWithListeners<Class<?>>();
  public final ICallbackWithListeners<LineNumberRulerColumn> onInitializeLineNumberRulerColumn = new CallbackWithListeners<LineNumberRulerColumn>();
  public final ICallbackWithListeners<?> onDispose = new CallbackWithListeners<Object>();
  public final ICallbackWithListeners<PropertyChangeEvent> onHandlePreferenceStoreChanged = new CallbackWithListeners<PropertyChangeEvent>();
//  public final ICallbackWithListeners<PySourceViewer> onCreateSourceViewer = new CallbackWithListeners<PySourceViewer>();

  public ISourceViewer getISourceViewer() {
      return getSourceViewer();
  }

  public IVerticalRuler getIVerticalRuler() {
      return getVerticalRuler();
  }

  @Override
  protected void initializeLineNumberRulerColumn(LineNumberRulerColumn rulerColumn) {
      super.initializeLineNumberRulerColumn(rulerColumn);
      this.onInitializeLineNumberRulerColumn.call(rulerColumn);
  }

  @Override
  protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
      super.handlePreferenceStoreChanged(event);
      this.onHandlePreferenceStoreChanged.call(event);
  }

  @Override
  public void createPartControl(Composite parent) {
      Composite newParent = (Composite) this.onCreatePartControl.call(parent);
      if (newParent != null) {
          parent = newParent;
      }
      super.createPartControl(parent);
      this.onAfterCreatePartControl.call(getSourceViewer());
  }

  private boolean disposed = false;

  public boolean isDisposed() {
      return disposed;
  }

  /**
   * Anyone may register to know when PyEdits are created.
   */
  public static final ICallbackWithListeners<PilarEditor> onPilarEditorCreated = new CallbackWithListeners<PilarEditor>();

  // ---------------------------- end listeners stuff

	public PilarEditor() {
//		super();
//		colorCache = new ColorAndStyleCache(AmandroidPrefs.getChainedPrefStore());
//		setSourceViewerConfiguration(new PilarConfigurationWithoutEditor(colorCache));
//		setDocumentProvider(new PilarDocumentProvider());
		super();
    synchronized (currentlyOpenedEditorsLock) {
        currentlyOpenedEditors.add(this);
    }
    try {
        onPilarEditorCreated.call(this);
    } catch (Throwable e) {
        Log.log(e);
    }
    try {
        //initialize the 'save' listeners of PyEdit
//        if (editListeners == null) {
//            editListeners = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_PYEDIT_LISTENER);
//        }
//        notifier.notifyEditorCreated();
      colorCache = new ColorAndStyleCache(AmandroidPrefs.getChainedPrefStore());

      editConfiguration = new PilarConfiguration(colorCache, this, AmandroidPrefs.getChainedPrefStore());
      setSourceViewerConfiguration(editConfiguration);
//        indentStrategy = editConfiguration.getPyAutoIndentStrategy();
      setRangeIndicator(new DefaultRangeIndicator()); // enables standard
      // vertical ruler

      //Added to set the code folding.
//        CodeFoldingSetter codeFoldingSetter = new CodeFoldingSetter(this);
//        this.addModelListener(codeFoldingSetter);
//        this.addPropertyListener(codeFoldingSetter);

      //Don't show message anymore now that funding on indiegogo has finished.
      //PydevShowBrowserMessage.show();
      setDocumentProvider(new PilarDocumentProvider());
      CheckDefaultPreferencesDialog.askAboutSettings();
    } catch (Throwable e) {
        Log.log(e);
    }
	}
	
	/**
   * Initializes everyone that needs document access
   *  
   */
  @Override
  public void init(final IEditorSite site, final IEditorInput input) throws PartInitException {
      try {
          super.init(site, input);

          final IDocument document = getDocument(input);

          // check the document partitioner (sanity check / fix)
          PilarPartitionScanner.checkPartitionScanner(document);

          // Also adds Python nature to the project.
          // The reason this is done here is because I want to assign python
          // nature automatically to any project that has active python files.
//          final IPilarNature nature = PilarNature.addNature(input);

          //we also want to initialize our shells...
          //we use 2: one for the main thread and one for the other threads.
          //just preemptively start the one for the main thread.
//          final int mainThreadShellId = AbstractShell.getShellId();
//          Thread thread2 = new Thread() {
//              @Override
//              public void run() {
//                  try {
//                      try {
//                          AbstractShell.getServerShell(nature, mainThreadShellId);
//                      } catch (RuntimeException e1) {
//                      }
//                  } catch (Exception e) {
//                  }
//
//              }
//          };
//          thread2.setName("Shell starter");
//          thread2.start();

          // listen to changes in TAB_WIDTH preference
          prefListener = createPrefChangeListener(this);
          resetForceTabs();
          AmandroidPrefs.getChainedPrefStore().addPropertyChangeListener(prefListener);

//          Runnable runnable = new Runnable() {
//
//              public void run() {
//                  try {
//                      //let's do that in a thread, so that we don't have any delays in setting up the editor
//                      pyEditScripting = new PyEditScripting();
//                      addPyeditListener(pyEditScripting);
//                  } finally {
//                      //if it fails, still mark it as finished.
//                      markInitFinished();
//                  }
//              }
//          };
//          Thread thread = new Thread(runnable);
//          thread.setName("PyEdit initializer");
//          thread.start();
      } catch (Throwable e) {
          //never fail in the init
          Log.log(e);
      }
  }
  
  public static IPropertyChangeListener createPrefChangeListener(
      final IPilarSyntaxHighlightingAndCodeCompletionEditor editor) {
  		return new IPropertyChangeListener() {

      public void propertyChange(PropertyChangeEvent event) {
          try {
              String property = event.getProperty();
              //tab width
              if (property.equals(PilarEditorPrefs.TAB_WIDTH)) {
                  ISourceViewer sourceViewer = editor.getEditorSourceViewer();
                  if (sourceViewer == null) {
                      return;
                  }
                  editor.getIndentPrefs().regenerateIndentString();
                  sourceViewer.getTextWidget().setTabs(DefaultIndentPrefs.getStaticTabWidth());
                  editor.resetIndentPrefixes();

              } else if (property.equals(PilarEditorPrefs.SUBSTITUTE_TABS)) {
                  editor.getIndentPrefs().regenerateIndentString();
                  editor.resetIndentPrefixes();

                  //auto adjust for file tabs
              } else if (property.equals(PilarEditorPrefs.GUESS_TAB_SUBSTITUTION)) {
                  editor.resetForceTabs();
                  editor.resetIndentPrefixes();

                  //colors and styles
              } else if (ColorAndStyleCache.isColorOrStyleProperty(property)) {
                  editor.getColorCache().reloadProperty(property); //all reference this cache
                  editor.getEditConfiguration().updateSyntaxColorAndStyle(); //the style needs no reloading
                  editor.getEditorSourceViewer().invalidateTextPresentation();
              }
          } catch (Exception e) {
              Log.log(e);
          }
      }
  };
}
	
	@Override
	protected void createActions() {
		// TODO Auto-generated method stub
		super.createActions();
	}
	
	@Override
	public void dispose() {
		colorCache.dispose();
		super.dispose();
	}
	
	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		if (IContentOutlinePage.class.equals(adapter)) {
		  if (outlinepage == null) {
		  	outlinepage = new PilarContentOutlinePage(getDocumentProvider(), this);
		  	outlinepage.setInput(getEditorInput());
		  }
		  return outlinepage;
		}
	  return super.getAdapter(adapter);
	}
	
	/**
   * @param input the input from where we want to get the document
   * @return the document for the passed input
   */
  private IDocument getDocument(final IEditorInput input) {
      return getDocumentProvider().getDocument(input);
  }

	@Override
	public Object getFormatStd() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
   * @return the pilar nature associated with this editor.
   * @throws NotConfiguredInterpreterException 
   */
  public IPilarNature getPilarNature() throws MisconfigurationException {
      IProject project = getProject();
      if (project == null || !project.isOpen()) {
          return null;
      }
//      IPilarNature pilarNature = PilarNature.getPilarNature(project);
//      if (pilarNature != null) {
//          return pilarNature;
//      }
//
//      //if it's an external file, there's the possibility that it won't be added even here.
//      pilarNature = PilarNature.addNature(this.getEditorInput());
//
//      if (pilarNature != null) {
//          return pilarNature;
//      }
//
//      Tuple<IPilarNature, String> infoForFile = AmandroidPlugin.getInfoForFile(getEditorFile());
//      if (infoForFile == null) {
//          NotConfiguredInterpreterException e = new NotConfiguredInterpreterException();
//          ErrorDialog.openError(EditorUtils.getShell(), "Error: no interpreter configured",
//                  "Interpreter not configured\n(Please, Configure it under window->preferences->PyDev)",
//                  PydevPlugin.makeStatus(IStatus.ERROR, e.getMessage(), e));
//          throw e;
//
//      }
//      pilarNature = infoForFile.o1;
      return null;
  }
  
  /**
   * @return the project for the file that's being edited (or null if not available)
   */
  public IProject getProject() {
      IEditorInput editorInput = this.getEditorInput();
      if (editorInput instanceof FileEditorInput) {
          IFile file = (IFile) ((FileEditorInput) editorInput).getAdapter(IFile.class);
          return file.getProject();
      }
      return null;
  }

  @Override
  protected void initializeEditor() {
      super.initializeEditor();
      try {
          this.setPreferenceStore(AmandroidPrefs.getChainedPrefStore());
          setEditorContextMenuId(PILAR_EDIT_CONTEXT);
          setRulerContextMenuId(PILAR_EDIT_RULER_CONTEXT);
          setDocumentProvider(PilarDocumentProvider.instance);
      } catch (Throwable e) {
          Log.log(e);
      }
  }

	@Override
	public IGrammarVersionProvider getGrammarVersionProvider() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IIndentPrefs getIndentPrefs() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
   * Checks if there's a syntax error at the document... if there is, returns false.
   * 
   * Note: This function will also set the status line error message if there's an error message.
   * Note: This function will actually do a parse operation when called (so, it should be called with care).
   */
  public boolean hasSyntaxError(IDocument doc) throws MisconfigurationException {
//      Tuple2<Option<Model>, String> reparse = PilarParser.parseWithErrorAsString(new Left<String, String>(doc.get()), Model.class, 0);
//      if (reparse._2.length() > 0) {
//          this.getStatusLineManager().setErrorMessage(reparse._2);
//          return true;
//      }
      return false;
  }
  
  /**
   * Returns the status line manager of this editor.
   * @return the status line manager of this editor
   */
  @Override
  public IStatusLineManager getStatusLineManager() {
      return EditorUtils.getStatusLineManager(this);
  }

	@Override
	public ISourceViewer getEditorSourceViewer() {
		return getSourceViewer();
	}

	@Override
	public void resetForceTabs() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public File getEditorFile() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void resetIndentPrefixes() {
		// TODO Auto-generated method stub
		
	}
	
}
