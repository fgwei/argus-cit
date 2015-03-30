package amanide.ui.wizards.files;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import amanide.AmanIDEPlugin;
import amanide.utils.Log;

public abstract class AbstractPilarWizard extends Wizard implements INewWizard {

	public static void startWizard(AbstractPilarWizard wizard, String title) {
		IWorkbenchPart part = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().getActivePart();
		IStructuredSelection sel = (IStructuredSelection) part.getSite()
				.getSelectionProvider().getSelection();

		startWizard(wizard, title, sel);
	}

	/**
	 * Must be called in the UI thread.
	 * 
	 * @param sel
	 *            will define what appears initially in the project/source
	 *            folder/name.
	 */
	public static void startWizard(AbstractPilarWizard wizard, String title,
			IStructuredSelection sel) {
		IWorkbenchPart part = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().getActivePart();

		wizard.init(part.getSite().getWorkbenchWindow().getWorkbench(), sel);
		wizard.setWindowTitle(title);

		Shell shell = part.getSite().getShell();
		if (shell == null) {
			shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getShell();
		}
		WizardDialog dialog = new WizardDialog(shell, wizard);
		dialog.setPageSize(350, 500);
		dialog.setHelpAvailable(false);
		dialog.create();
		dialog.open();
	}

	/**
	 * The workbench.
	 */
	protected IWorkbench workbench;

	/**
	 * The current selection.
	 */
	protected IStructuredSelection selection;

	protected String title;
	protected String description = "";

	public AbstractPilarWizard(String title) {
		this.title = title;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
		this.selection = selection;

		initializeDefaultPageImageDescriptor();

	}

	/**
	 * Set Pilar logo to top bar
	 */
	protected void initializeDefaultPageImageDescriptor() {
		ImageDescriptor desc = AmanIDEPlugin.imageDescriptorFromPlugin(
				AmanIDEPlugin.getPluginID(), "icons/plr_logo.png");//$NON-NLS-1$
		setDefaultPageImageDescriptor(desc);
	}

	/** Wizard page asking filename */
	protected AbstractPilarWizardPage filePage;

	/**
	 * @see org.eclipse.jface.wizard.IWizard#addPages()
	 */
	@Override
	public void addPages() {
		filePage = createPathPage();
		filePage.setTitle(this.title);
		filePage.setDescription(this.description);
		addPage(filePage);
	}

	/**
	 * @return
	 */
	protected abstract AbstractPilarWizardPage createPathPage();

	/**
	 * User clicks Finish
	 */
	@Override
	public boolean performFinish() {
		try {
			// Create file object
			doCreateNew(new NullProgressMonitor());
		} catch (Exception e) {
			Log.log(e);
			return false;
		}
		return true;
	}

	/**
	 * Subclasses may override to do something after the editor was opened with
	 * a given file.
	 * 
	 * @param openEditor
	 *            the opened editor
	 */
	protected void afterEditorCreated(IEditorPart openEditor) {

	}

	/**
	 * This method must be overriden to create the needed resource.
	 * 
	 * @return the created resource
	 */
	protected abstract void doCreateNew(IProgressMonitor monitor)
			throws CoreException;

}
