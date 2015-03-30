package amanide.ui.perspective;

import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.progress.IProgressConstants;

import amanide.ui.wizards.files.PilarPackageWizard;
import amanide.ui.wizards.files.PilarSourceFolderWizard;

/**
 * Pilar perspective constructor
 * 
 * @author Fengguo Wei
 */
public class PilarPerspectiveFactory implements IPerspectiveFactory {

	public static final String PERSPECTIVE_ID = "amanide.ui.PilarPerspective";

	/**
	 * Creates Pilar perspective layout
	 */
	@Override
	public void createInitialLayout(IPageLayout layout) {
		defineLayout(layout);
		defineActions(layout);
	}

	public void defineLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		IFolderLayout topLeft = layout.createFolder(
				"topLeft", IPageLayout.LEFT, (float) 0.26, editorArea); //$NON-NLS-1$
		topLeft.addView("amanide.navigator.view");

		IFolderLayout outputfolder = layout.createFolder(
				"bottom", IPageLayout.BOTTOM, (float) 0.75, editorArea); //$NON-NLS-1$
		// outputfolder.addView(IPageLayout.ID_PROBLEM_VIEW);
		outputfolder.addPlaceholder(NewSearchUI.SEARCH_VIEW_ID);
		outputfolder.addPlaceholder(IConsoleConstants.ID_CONSOLE_VIEW);
		outputfolder.addPlaceholder(IPageLayout.ID_BOOKMARKS);
		outputfolder.addPlaceholder(IProgressConstants.PROGRESS_VIEW_ID);
	}

	/**
	 * @param layout
	 */
	public void defineActions(IPageLayout layout) {
		layout.addNewWizardShortcut(PilarSourceFolderWizard.WIZARD_ID); //$NON-NLS-1$        
		layout.addNewWizardShortcut(PilarPackageWizard.WIZARD_ID); //$NON-NLS-1$        
		layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.folder");//$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.file");//$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.ui.editors.wizards.UntitledTextFileWizard");//$NON-NLS-1$

		// layout.addShowViewShortcut("org.python.pydev.views.PyCodeCoverageView");
		// layout.addShowViewShortcut("org.python.pydev.navigator.view");
		// layout.addShowViewShortcut("org.python.pydev.debug.pyunit.pyUnitView");
		layout.addShowViewShortcut(NewSearchUI.SEARCH_VIEW_ID);
		layout.addShowViewShortcut(IConsoleConstants.ID_CONSOLE_VIEW);
		layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
		layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
		// layout.addShowViewShortcut(IPageLayout.ID_RES_NAV);-- Navigator no
		// longer supported
		layout.addShowViewShortcut("org.eclipse.pde.runtime.LogView");
		layout.addShowViewShortcut(IPageLayout.ID_TASK_LIST);

		layout.addActionSet(IDebugUIConstants.LAUNCH_ACTION_SET);
		layout.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET);

	}

}
