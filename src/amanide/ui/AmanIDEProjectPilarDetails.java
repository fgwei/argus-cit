package amanide.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PropertyPage;

import amanide.callbacks.ICallback;
import amanide.natures.IPilarNature;
import amanide.natures.PilarNature;
import amanide.utils.Log;
import amanide.utils.StringUtils;

/**
 * @author <a href="mailto:wfg611004900521@gmail.com">Fengguo Wei</a>
 */
public class AmanIDEProjectPilarDetails extends PropertyPage {

	/**
	 * This class provides a way to show to the user the options available to
	 * configure a project with the correct interpreter and grammar.
	 */
	public static class ProjectGrammarConfig {
		// private static final String INTERPRETER_NOT_CONFIGURED_MSG =
		// "<a>Please configure an interpreter before proceeding.</a>";
		public Button radioPilar;
		public Combo comboGrammarVersion;
		public Label versionLabel;
		private SelectionListener selectionListener;
		private ICallback<Object, Object> onSelectionChanged;

		public ProjectGrammarConfig() {
			// Don't want to display "config interpreter" dialog when this
			// dialog does
			// that already.
			// DialogHelpers.enableAskInterpreterStep(false);
		}

		/**
		 * Optionally, a callback may be passed to be called whenever the
		 * selection of the project type changes.
		 */
		public ProjectGrammarConfig(ICallback<Object, Object> callback) {
			this.onSelectionChanged = callback;
		}

		public Control doCreateContents(Composite p) {
			Composite topComp = new Composite(p, SWT.NONE);
			GridLayout innerLayout = new GridLayout();
			innerLayout.numColumns = 1;
			innerLayout.marginHeight = 0;
			innerLayout.marginWidth = 0;
			topComp.setLayout(innerLayout);
			GridData gd = new GridData(GridData.FILL_BOTH);
			topComp.setLayoutData(gd);

			// Project type
			Group group = new Group(topComp, SWT.NONE);
			group.setText("Choose the project type");
			GridLayout layout = new GridLayout();
			layout.horizontalSpacing = 8;
			layout.numColumns = 3;
			group.setLayout(layout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			group.setLayoutData(gd);

			radioPilar = new Button(group, SWT.RADIO | SWT.LEFT);
			radioPilar.setText("Pilar");

			// Grammar version
			versionLabel = new Label(topComp, 0);
			versionLabel.setText("Grammar Version");
			gd = new GridData(GridData.FILL_HORIZONTAL);
			versionLabel.setLayoutData(gd);

			comboGrammarVersion = new Combo(topComp, SWT.READ_ONLY);
			for (String s : IPilarNature.Versions.VERSION_NUMBERS) {
				comboGrammarVersion.add(s);
			}

			gd = new GridData(GridData.FILL_HORIZONTAL);
			comboGrammarVersion.setLayoutData(gd);

			selectionListener = new SelectionListener() {

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {

				}

				/**
				 * @param e
				 *            can be null to force an update.
				 */
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (e != null) {
						Button source = (Button) e.getSource();
						if (!source.getSelection()) {
							return; // we'll get 2 notifications: selection of
									// one and deselection of the other, so,
									// let's just treat the selection
						}
					}
					triggerCallback();
				}
			};
			radioPilar.addSelectionListener(selectionListener);
			return topComp;
		}

		private void triggerCallback() {
			if (onSelectionChanged != null) {
				try {
					onSelectionChanged.call(null);
				} catch (Exception e1) {
					Log.log(e1);
				}
			}
		}

		/**
		 * @return a string as specified in the constants in IPilarNature
		 * @see IPilarNature#PILAR_VERSION_XXX
		 */
		public String getSelectedPilarGrammarVersion() {
			if (radioPilar.getSelection()) {
				return "Pilar " + comboGrammarVersion.getText();
			}
			throw new RuntimeException("Some radio must be selected");
		}

		// public String getProjectInterpreter() {
		// if (INTERPRETER_NOT_CONFIGURED_MSG.equals(interpreterNoteText
		// .getText())) {
		// return null;
		// }
		// return interpretersChoice.getText();
		// }

		public void setDefaultSelection() {
			radioPilar.setSelection(true);
			comboGrammarVersion
					.setText(IPilarNature.Versions.LAST_VERSION_NUMBER);
			// Just to update things
			this.selectionListener.widgetSelected(null);
		}

	}

	/**
	 * The element.
	 */
	public IAdaptable element;

	public ProjectGrammarConfig projectConfig = new ProjectGrammarConfig();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchPropertyPage#getElement()
	 */
	@Override
	public IAdaptable getElement() {
		return element;
	}

	/**
	 * Sets the element that owns properties shown on this page.
	 *
	 * @param element
	 *            the element
	 */
	@Override
	public void setElement(IAdaptable element) {
		this.element = element;
	}

	public IProject getProject() {
		return (IProject) getElement().getAdapter(IProject.class);
	}

	@Override
	public Control createContents(Composite p) {
		Control contents = projectConfig.doCreateContents(p);
		setSelected();
		return contents;
	}

	private void setSelected() {
		PilarNature pilarNature = PilarNature.getPilarNature(getProject());
		try {
			String version = pilarNature.getVersion();
			if (IPilarNature.Versions.ALL_PILAR_VERSIONS.contains(version)) {
				projectConfig.radioPilar.setSelection(true);

			}

			// We must set the grammar version too (that's from a string in the
			// format "Pilar 2.4" and we only want
			// the version).
			String v = StringUtils.split(version, ' ').get(1);
			projectConfig.comboGrammarVersion.setText(v);

			// Update interpreter
			// projectConfig.selectionListener.widgetSelected(null);
			// String configuredInterpreter = pilarNature
			// .getProjectInterpreterName();
			// if (configuredInterpreter != null) {
			// projectConfig.interpretersChoice.setText(configuredInterpreter);
			// }

		} catch (CoreException e) {
			Log.log(e);
		}
	}

	@Override
	protected void performApply() {
		doIt();
	}

	@Override
	public boolean performOk() {
		return doIt();
	}

	@Override
	public boolean performCancel() {
		// re-enable "configure interpreter" dialogs
		// DialogHelpers.enableAskInterpreterStep(true);
		return super.performCancel();
	}

	private boolean doIt() {
		IProject project = getProject();

		if (project != null) {
			PilarNature pilarNature = PilarNature.getPilarNature(project);

			try {
				pilarNature.setVersion(projectConfig
						.getSelectedPilarGrammarVersion());
			} catch (CoreException e) {
				Log.log(e);
			}
		}
		// re-enable "configure interpreter" dialogs
		// DialogHelpers.enableAskInterpreterStep(true);
		return true;
	}
}