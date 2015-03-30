package amanide.ui.wizards.project;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.dialogs.WorkingSetConfigurationBlock;

import amanide.AmanIDEPlugin;
import amanide.AmanIDEStructureConfigHelpers;
import amanide.callbacks.ICallback;
import amanide.natures.IPilarNature;
import amanide.ui.AmanIDEProjectPilarDetails;

/**
 * First page for the import project wizard. This page collects the name and
 * location of the imported project.
 * 
 * @author <a href="mailto:wfg611004900521@gmail.com">Fengguo Wei</a>
 */

public class ImportProjectAndLocationWizardPage extends AbstractNewProjectPage
		implements IWizardImportProjectAndLocationPage {

	private String filteredSuffix;

	// Whether to use default or custom project location
	private boolean useDefaults = true;

	// initial value stores
	private IPath initialLocationFieldValue;

	// the value the user has entered
	private String importedFilePathFieldValue;
	private String customLocationFieldValue;

	// widgets
	private Text importedFilePathField;

	private Label importedFileLabel;

	private Text locationPathField;

	private Label locationLabel;

	private Button browseApkButton;
	private Button browseLocationButton;

	private AmanIDEProjectPilarDetails.ProjectGrammarConfig details;

	/**
	 * @return a string as specified in the constants in IPilarNature
	 * @see IPilarNature#PILAR_VERSION_XXX
	 */
	@Override
	public String getProjectType() {
		return details.getSelectedPilarGrammarVersion();
	}

	// public String getProjectInterpreter() {
	// return details.getProjectInterpreter();
	// }

	// private Listener nameModifyListener = new Listener() {
	// @Override
	// public void handleEvent(Event e) {
	// setLocationForSelection();
	// setPageComplete(validatePage());
	// }
	// };

	/**
	 * @param composite
	 */
	private void createProjectDetails(Composite parent) {
		Font font = parent.getFont();
		Composite projectDetails = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		projectDetails.setLayout(layout);
		projectDetails.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		projectDetails.setFont(font);

		Label projectTypeLabel = new Label(projectDetails, SWT.NONE);
		projectTypeLabel.setFont(font);
		projectTypeLabel.setText("Project type");
		// let him choose the type of the project
		details = new AmanIDEProjectPilarDetails.ProjectGrammarConfig(
				new ICallback<Object, Object>() {

					// Whenever the configuration changes there, we must
					// evaluate whether the page is complete
					@Override
					public Object call(Object args) {
						setPageComplete(ImportProjectAndLocationWizardPage.this
								.validatePage());
						return null;
					}
				});

		Control createdOn = details.doCreateContents(projectDetails);
		details.setDefaultSelection();
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		createdOn.setLayoutData(data);
	}

	private Listener locationModifyListener = new Listener() {
		@Override
		public void handleEvent(Event e) {
			setPageComplete(validatePage());
		}
	};

	private final static String RESOURCE = "org.eclipse.ui.resourceWorkingSetPage"; //$NON-NLS-1$

	private final class WorkingSetGroup {

		private WorkingSetConfigurationBlock fWorkingSetBlock;

		public WorkingSetGroup() {
			String[] workingSetIds = new String[] { RESOURCE };
			fWorkingSetBlock = new WorkingSetConfigurationBlock(workingSetIds,
					AmanIDEPlugin.getDefault().getDialogSettings());
		}

		public Control createControl(Composite composite) {
			Group workingSetGroup = new Group(composite, SWT.NONE);
			workingSetGroup.setFont(composite.getFont());
			workingSetGroup.setText("Working sets");
			workingSetGroup.setLayout(new GridLayout(1, false));

			fWorkingSetBlock.createContent(workingSetGroup);

			return workingSetGroup;
		}

		public void setWorkingSets(IWorkingSet[] workingSets) {
			fWorkingSetBlock.setWorkingSets(workingSets);
		}

		public IWorkingSet[] getSelectedWorkingSets() {
			return fWorkingSetBlock.getSelectedWorkingSets();
		}
	}

	// constants
	private static final int SIZING_TEXT_FIELD_WIDTH = 250;

	private final WorkingSetGroup fWorkingSetGroup;

	/**
	 * Creates a new project creation wizard page.
	 *
	 * @param pageName
	 *            the name of this page
	 */
	public ImportProjectAndLocationWizardPage(String pageName,
			String projectType, String filteredSuffix) {
		super(pageName);
		setTitle("Amandroid Project");
		setDescription("Load " + filteredSuffix + " as a new " + projectType
				+ " Project.");
		setPageComplete(false);
		this.filteredSuffix = filteredSuffix;
		this.initialLocationFieldValue = Platform.getLocation();
		this.customLocationFieldValue = ""; //$NON-NLS-1$

		fWorkingSetGroup = new WorkingSetGroup();
		setWorkingSets(new IWorkingSet[0]);
	}

	/*
	 * (non-Javadoc) Method declared on IDialogPage.
	 */
	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setFont(parent.getFont());

		importApkControl(composite);
		createProjectLocationGroup(composite);
		createProjectDetails(composite);

		Control workingSetControl = createWorkingSetControl(composite);
		workingSetControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		validatePage();

		// Show description on opening
		setErrorMessage(null);
		setMessage(null);
		setControl(composite);
	}

	/**
	 * Creates the controls for the working set selection.
	 *
	 * @param composite
	 *            the parent composite
	 * @return the created control
	 */
	protected Control createWorkingSetControl(Composite composite) {
		return fWorkingSetGroup.createControl(composite);
	}

	/**
	 * Returns the working sets to which the new project should be added.
	 *
	 * @return the selected working sets to which the new project should be
	 *         added
	 */
	@Override
	public IWorkingSet[] getWorkingSets() {
		return fWorkingSetGroup.getSelectedWorkingSets();
	}

	/**
	 * Sets the working sets to which the new project should be added.
	 *
	 * @param workingSets
	 *            the initial selected working sets
	 */
	public void setWorkingSets(IWorkingSet[] workingSets) {
		if (workingSets == null) {
			throw new IllegalArgumentException();
		}
		fWorkingSetGroup.setWorkingSets(workingSets);
	}

	/**
	 * Creates the project location specification controls.
	 *
	 * @param parent
	 *            the parent composite
	 */
	private final void createProjectLocationGroup(Composite parent) {
		Font font = parent.getFont();
		// project specification group
		Composite projectGroup = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		projectGroup.setLayout(layout);
		projectGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		projectGroup.setFont(font);

		// new project label
		Label projectContentsLabel = new Label(projectGroup, SWT.NONE);
		projectContentsLabel.setFont(font);

		projectContentsLabel.setText("Project contents:");

		GridData labelData = new GridData();
		labelData.horizontalSpan = 3;
		projectContentsLabel.setLayoutData(labelData);

		final Button useDefaultsButton = new Button(projectGroup, SWT.CHECK
				| SWT.RIGHT);
		useDefaultsButton.setText("Use &default");
		useDefaultsButton.setSelection(useDefaults);
		useDefaultsButton.setFont(font);

		GridData buttonData = new GridData();
		buttonData.horizontalSpan = 3;
		useDefaultsButton.setLayoutData(buttonData);

		createUserSpecifiedProjectLocationGroup(projectGroup, !useDefaults);

		SelectionListener listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				useDefaults = useDefaultsButton.getSelection();
				browseLocationButton.setEnabled(!useDefaults);
				locationPathField.setEnabled(!useDefaults);
				locationLabel.setEnabled(!useDefaults);
				if (useDefaults) {
					customLocationFieldValue = locationPathField.getText();
					setLocationForSelection();
				} else {
					locationPathField.setText(customLocationFieldValue);
				}
			}
		};
		useDefaultsButton.addSelectionListener(listener);
	}

	/**
	 * Creates the project name specification controls.
	 *
	 * @param parent
	 *            the parent composite
	 */
	private final void importApkControl(Composite parent) {
		Font font = parent.getFont();
		// project specification group
		Composite projectGroup = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		projectGroup.setLayout(layout);
		projectGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		projectGroup.setFont(font);

		// location label
		importedFileLabel = new Label(projectGroup, SWT.NONE);
		importedFileLabel.setFont(font);
		importedFileLabel.setText("Import APK:");
		importedFileLabel.setEnabled(true);

		// project location entry field
		importedFilePathField = new Text(projectGroup, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = SIZING_TEXT_FIELD_WIDTH;
		importedFilePathField.setLayoutData(data);
		importedFilePathField.setFont(font);
		importedFilePathField.setEnabled(true);
		importedFilePathField.setEditable(false);

		// browse button
		browseApkButton = new Button(projectGroup, SWT.PUSH);
		browseApkButton.setFont(font);
		browseApkButton.setText("B&rowse");
		browseApkButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				handleImportBrowseButtonPressed();
			}
		});

		browseApkButton.setEnabled(true);

		// Set the initial value first before listener
		// to avoid handling an event during the creation.
		importedFilePathField.addListener(SWT.Modify, locationModifyListener);
	}

	/**
	 * Creates the project location specification controls.
	 *
	 * @param projectGroup
	 *            the parent composite
	 * @param enabled
	 *            the initial enabled state of the widgets created
	 */
	private void createUserSpecifiedProjectLocationGroup(
			Composite projectGroup, boolean enabled) {
		Font font = projectGroup.getFont();
		// location label
		locationLabel = new Label(projectGroup, SWT.NONE);
		locationLabel.setFont(font);
		locationLabel.setText("Director&y");
		locationLabel.setEnabled(enabled);

		// project location entry field
		locationPathField = new Text(projectGroup, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = SIZING_TEXT_FIELD_WIDTH;
		locationPathField.setLayoutData(data);
		locationPathField.setFont(font);
		locationPathField.setEnabled(enabled);

		// browse button
		browseLocationButton = new Button(projectGroup, SWT.PUSH);
		browseLocationButton.setFont(font);
		browseLocationButton.setText("B&rowse");
		browseLocationButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				handleLocationBrowseButtonPressed();
			}
		});

		browseLocationButton.setEnabled(enabled);

		// Set the initial value first before listener
		// to avoid handling an event during the creation.
		if (initialLocationFieldValue != null) {
			locationPathField.setText(initialLocationFieldValue.toOSString());
		}
		locationPathField.addListener(SWT.Modify, locationModifyListener);
	}

	/**
	 * Returns the current project location path as entered by the user, or its
	 * anticipated initial value.
	 *
	 * @return the project location path, its anticipated initial value, or
	 *         <code>null</code> if no project location path is known
	 */
	@Override
	public IPath getLocationPath() {
		if (useDefaults) {
			return initialLocationFieldValue;
		}

		return new Path(getProjectLocationFieldValue());
	}

	/**
	 * Creates a project resource handle for the current project name field
	 * value.
	 * <p>
	 * This method does not create the project resource; this is the
	 * responsibility of <code>IProject::create</code> invoked by the new
	 * project resource wizard.
	 * </p>
	 *
	 * @return the new project resource handle
	 */
	@Override
	public IProject getProjectHandle() {
		return AmanIDEStructureConfigHelpers.getProjectHandle(getProjectName());
	}

	private String getProjectName() {
		String filepath = getImportedFilePathFieldValue();
		if (filepath == null)
			return "";
		return new File(filepath).getName()
				.replace("." + this.filteredSuffix, "").trim();
	}

	/**
	 * Returns the value of the project location field with leading and trailing
	 * spaces removed.
	 * 
	 * @return the project location directory in the field
	 */
	private String getImportedFilePathFieldValue() {
		if (importedFilePathField == null) {
			return ""; //$NON-NLS-1$
		} else {
			return importedFilePathField.getText().trim();
		}
	}

	/**
	 * Returns the value of the project location field with leading and trailing
	 * spaces removed.
	 * 
	 * @return the project location directory in the field
	 */
	private String getProjectLocationFieldValue() {
		if (locationPathField == null) {
			return ""; //$NON-NLS-1$
		} else {
			return locationPathField.getText().trim();
		}
	}

	/**
	 * Open an appropriate file browser
	 */
	private void handleImportBrowseButtonPressed() {
		FileDialog dialog = new FileDialog(importedFilePathField.getShell());
		String[] exts = new String[] { filteredSuffix };
		dialog.setFilterExtensions(exts);

		String apkPath = getImportedFilePathFieldValue();
		if (!apkPath.equals("")) { //$NON-NLS-1$
			File apk = new File(apkPath);
			if (apk.exists()) {
				dialog.setFilterPath(new Path(apkPath).toOSString());
			}
		}

		String selectedDirectory = dialog.open();
		if (selectedDirectory != null) {
			importedFilePathFieldValue = selectedDirectory;
			importedFilePathField.setText(importedFilePathFieldValue);
		}
	}

	/**
	 * Open an appropriate directory browser
	 */
	private void handleLocationBrowseButtonPressed() {
		DirectoryDialog dialog = new DirectoryDialog(
				locationPathField.getShell());
		dialog.setMessage("Select the project contents directory.");

		String dirName = getProjectLocationFieldValue();
		if (!dirName.equals("")) { //$NON-NLS-1$
			File path = new File(dirName);
			if (path.exists()) {
				dialog.setFilterPath(new Path(dirName).toOSString());
			}
		}

		String selectedDirectory = dialog.open();
		if (selectedDirectory != null) {
			customLocationFieldValue = selectedDirectory;
			locationPathField.setText(customLocationFieldValue);
		}
	}

	/**
	 * Returns whether the currently specified project content directory points
	 * to an existing project
	 */
	private boolean isDotProjectFileInLocation() {
		// Want to get the path of the containing folder, even if workspace
		// location is used
		IPath path = new Path(getProjectLocationFieldValue());
		path = path.append(IProjectDescription.DESCRIPTION_FILE_NAME);
		return path.toFile().exists();
	}

	/**
	 * Set the location to the default location if we are set to useDefaults.
	 */
	private void setLocationForSelection() {
		if (useDefaults) {
			IPath defaultPath = Platform.getLocation().append(getProjectName());
			locationPathField.setText(defaultPath.toOSString());
		}
	}

	/**
	 * Returns whether this page's controls currently all contain valid values.
	 *
	 * @return <code>true</code> if all controls are valid, and
	 *         <code>false</code> if at least one is invalid
	 */
	protected boolean validatePage() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();

		String importedFileName = getImportedFilePathFieldValue();
		if (importedFileName.equals("")) {
			setErrorMessage(null);
			setMessage("Have not specify " + this.filteredSuffix
					+ " file to be imported.");
			return false;
		} else if (!importedFileName.endsWith("." + this.filteredSuffix)) {
			setErrorMessage(null);
			setMessage("Imported file has to be end with ."
					+ this.filteredSuffix);
			return false;
		}

		String locationFieldContents = getProjectLocationFieldValue();

		if (locationFieldContents.equals("")) { //$NON-NLS-1$
			setErrorMessage(null);
			setMessage("Project location is empty");
			return false;
		}

		IPath path = new Path(""); //$NON-NLS-1$
		if (!path.isValidPath(locationFieldContents)) {
			setErrorMessage("Project location is not valid");
			return false;
		}

		// commented out. See comments on
		// https://sourceforge.net/tracker/?func=detail&atid=577329&aid=1798364&group_id=85796
		// if (!useDefaults
		// && Platform.getLocation().isPrefixOf(
		// new Path(locationFieldContents))) {
		// setErrorMessage("Default location error");
		// return false;
		// }

		IProject projectHandle = getProjectHandle();
		if (projectHandle.exists()) {
			setErrorMessage("Project already exists");
			return false;
		}

		if (!useDefaults) {
			path = getLocationPath();
			if (path.equals(workspace.getRoot().getLocation())) {
				setErrorMessage("Project location cannot be the workspace location.");
				return false;
			}
		}

		if (isDotProjectFileInLocation()) {
			setErrorMessage(".project found in: "
					+ getLocationPath().toOSString()
					+ " (use the Import Project wizard instead).");
			return false;
		}

		// if (getProjectInterpreter() == null) {
		// setErrorMessage("Project interpreter not specified");
		// return false;
		// }

		setErrorMessage(null);
		setMessage(null);

		// Look for existing Python files in the destination folder.
		// File locFile = (!useDefaults ? getLocationPath() : getLocationPath()
		// .append(projectFieldContents)).toFile();
		// PilarFileListing pyFileListing =
		// PythonPathHelper.getModulesBelow(locFile, null);
		// if (pyFileListing != null) {
		// boolean foundInit = false;
		// Collection<PyFileInfo> modulesBelow =
		// pyFileListing.getFoundPyFileInfos();
		// for (PyFileInfo fileInfo : modulesBelow) {
		// // Only notify existence of init files in the top-level directory.
		// if (PythonPathHelper.isValidInitFile(fileInfo.getFile().getPath())
		// && fileInfo.getFile().getParentFile().equals(locFile)) {
		// setMessage("Project location contains an __init__.py file. Consider using the location's parent folder instead.");
		// foundInit = true;
		// break;
		// }
		// }
		// if (!foundInit && modulesBelow.size() > 0) {
		// setMessage("Project location contains existing Python files. The created project will include them.");
		// }
		// }

		return true;
	}

	/*
	 * see @DialogPage.setVisible(boolean)
	 */
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			importedFilePathField.setFocus();
		}
	}

	@Override
	public IPath getImportedFilePath() {
		return new Path(getImportedFilePathFieldValue());
	}
}
