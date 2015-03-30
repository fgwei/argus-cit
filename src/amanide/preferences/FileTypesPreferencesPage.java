package amanide.preferences;

import java.util.List;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import amanide.AmanIDEPlugin;
import amanide.ui.utils.LabelFieldEditorWith2Cols;
import amanide.utils.StringUtils;
import amanide.utils.WrapAndCaseUtils;

/**
 * Preferences regarding the pilar file types available.
 * 
 * Also provides a better access to them and caches to make that access
 * efficient.
 *
 * @author Fengguo Wei
 */
public class FileTypesPreferencesPage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	public FileTypesPreferencesPage() {
		super(FLAT);
		setPreferenceStore(AmanIDEPlugin.getDefault().getPreferenceStore());
		setDescription("File Types Preferences");
	}

	public static final String VALID_SOURCE_FILES = "VALID_SOURCE_FILES";
	public final static String DEFAULT_VALID_SOURCE_FILES = "plr, pilar";

	public static final String FIRST_CHOICE_PILAR_SOURCE_FILE = "FIRST_CHOICE_PILAR_SOURCE_FILE";
	public final static String DEFAULT_FIRST_CHOICE_PILAR_SOURCE_FILE = "pilar";

	@Override
	protected void createFieldEditors() {
		final Composite p = getFieldEditorParent();

		addField(new LabelFieldEditorWith2Cols(
				"Label_Info_File_Preferences1",
				WrapAndCaseUtils
						.wrap("These setting are used to know which files should be considered valid internally, and are "
								+ "not used in the file association of those files to the pilar editor.\n\n",
								80), p) {
			@Override
			public String getLabelTextCol1() {
				return "Note:\n\n";
			}
		});

		addField(new LabelFieldEditorWith2Cols(
				"Label_Info_File_Preferences2",
				WrapAndCaseUtils
						.wrap("After changing those settings, a manual reconfiguration of the interpreter and a manual rebuild "
								+ "of the projects may be needed to update the inner caches that may be affected by those changes.\n\n",
								80), p) {
			@Override
			public String getLabelTextCol1() {
				return "Important:\n\n";
			}
		});

		addField(new StringFieldEditor(VALID_SOURCE_FILES,
				"Valid source files (comma-separated):",
				StringFieldEditor.UNLIMITED, p));
		addField(new StringFieldEditor(FIRST_CHOICE_PILAR_SOURCE_FILE,
				"Default pilar extension:", StringFieldEditor.UNLIMITED, p));
	}

	@Override
	public void init(IWorkbench workbench) {
		// pass
	}

	/**
	 * Helper to keep things cached as needed (so that we don't have to get it
	 * from the cache all the time.
	 *
	 * @author Fengguo Wei
	 */
	private static class PreferencesCacheHelper implements
			IPropertyChangeListener {
		private static PreferencesCacheHelper singleton;

		static synchronized PreferencesCacheHelper get() {
			if (singleton == null) {
				singleton = new PreferencesCacheHelper();
			}
			return singleton;
		}

		public PreferencesCacheHelper() {
			AmanIDEPrefs.getPreferences().addPropertyChangeListener(this);
		}

		@Override
		public void propertyChange(PropertyChangeEvent event) {
			this.wildcaldValidSourceFiles = null;
			this.dottedValidSourceFiles = null;
			this.pilarValidSourceFiles = null;
			// this.pythonValidInitFiles = null;
		}

		// return new String[] { "*.pilar", "*.plr" };
		private String[] wildcaldValidSourceFiles;

		public String[] getCacheWildcardValidSourceFiles() {
			String[] ret = wildcaldValidSourceFiles;
			if (ret == null) {
				String[] validSourceFiles = this.getCacheValidSourceFiles();
				String[] s = new String[validSourceFiles.length];
				for (int i = 0; i < validSourceFiles.length; i++) {
					s[i] = "*." + validSourceFiles[i];
				}
				wildcaldValidSourceFiles = s;
				ret = s;
			}
			return ret;
		}

		// return new String[] { ".plr", ".pilar" };
		private String[] dottedValidSourceFiles;

		public String[] getCacheDottedValidSourceFiles() {
			String[] ret = dottedValidSourceFiles;
			if (ret == null) {
				String[] validSourceFiles = this.getCacheValidSourceFiles();
				String[] s = new String[validSourceFiles.length];
				for (int i = 0; i < validSourceFiles.length; i++) {
					s[i] = "." + validSourceFiles[i];
				}
				dottedValidSourceFiles = s;
				ret = s;
			}
			return ret;
		}

		// return new String[] { "plr", "pilar" };
		private String[] pilarValidSourceFiles;

		public String[] getCacheValidSourceFiles() {
			String[] ret = pilarValidSourceFiles;
			if (ret == null) {
				String validStr = AmanIDEPrefs.getPreferences().getString(
						FileTypesPreferencesPage.VALID_SOURCE_FILES);
				final List<String> temp = StringUtils
						.splitAndRemoveEmptyTrimmed(validStr, ',');
				String[] s = temp.toArray(new String[temp.size()]);
				for (int i = 0; i < s.length; i++) {
					s[i] = s[i].trim();
				}
				pilarValidSourceFiles = s;
				ret = s;
			}
			return ret;
		}
	}

	// public interface with the hardcoded settings
	// --------------------------------------------------------------------

	public final static String getDefaultDottedPilarExtension() {
		return "."
				+ AmanIDEPrefs.getPreferences().getString(
						FIRST_CHOICE_PILAR_SOURCE_FILE);
	}

	// items that are customizable -- things gotten from the cache
	// -----------------------------------------------------

	public static String[] getWildcardValidSourceFiles() {
		return PreferencesCacheHelper.get().getCacheWildcardValidSourceFiles();
	}

	public final static String[] getDottedValidSourceFiles() {
		return PreferencesCacheHelper.get().getCacheDottedValidSourceFiles();
	}

	public final static String[] getValidSourceFiles() {
		return PreferencesCacheHelper.get().getCacheValidSourceFiles();
	}
}
