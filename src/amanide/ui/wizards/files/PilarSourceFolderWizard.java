package amanide.ui.wizards.files;

import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import amanide.AmanIDEStructureConfigHelpers;
import amanide.natures.IPilarNature;
import amanide.natures.IPilarPathNature;
import amanide.natures.PilarNature;
import amanide.utils.Log;

public class PilarSourceFolderWizard extends AbstractPilarWizard {

	public PilarSourceFolderWizard() {
		super("Create a new Source Folder");
	}

	public static final String WIZARD_ID = "amanide.ui.wizards.files.PilarSourceFolderWizard";

	@Override
	protected AbstractPilarWizardPage createPathPage() {
		return new AbstractPilarWizardPage(this.description, selection) {

			@Override
			protected boolean shouldCreateSourceFolderSelect() {
				return false;
			}

			@Override
			protected boolean shouldCreatePackageSelect() {
				return false;
			}

			@Override
			protected String checkNameText(String text) {
				String result = super.checkNameText(text);
				if (result != null) {
					return result;
				}
				if (getValidatedProject().findMember(text) != null) {
					return "The source folder " + text
							+ " already exists in project "
							+ getValidatedProject().getName() + ".";
				}
				return null;
			}

		};
	}

	@Override
	protected void doCreateNew(IProgressMonitor monitor) throws CoreException {
		IProject project = filePage.getValidatedProject();
		String name = filePage.getValidatedName();
		if (project == null || !project.exists()) {
			throw new RuntimeException(
					"The project selected does not exist in the workspace.");
		}
		IPilarPathNature pathNature = PilarNature.getPilarPathNature(project);
		if (pathNature == null) {
			IPilarNature nature = PilarNature.addNature(project, monitor, null,
					null, null);
			pathNature = nature.getPilarPathNature();
			if (pathNature == null) {
				throw new RuntimeException(
						"Unable to add the nature to the seleted project.");
			}
		}
		IFolder folder = project.getFolder(name);
		if (folder.exists()) {
			Log.log("Source folder already exists. Nothing new was created");
			return;
		}
		folder.create(true, true, monitor);
		String newPath = folder.getFullPath().toString();

		String curr = pathNature.getProjectSourcePath(false);
		if (curr == null) {
			curr = "";
		}
		if (curr.endsWith("|")) {
			curr = curr.substring(0, curr.length() - 1);
		}
		String newPathRel = AmanIDEStructureConfigHelpers
				.convertToProjectRelativePath(project.getFullPath().toString(),
						newPath);
		if (curr.length() > 0) {
			// there is already some path
			Set<String> projectSourcePathSet = pathNature
					.getProjectSourcePathSet(true);
			if (!projectSourcePathSet.contains(newPath)) {
				// only add to the path if it doesn't already contain the new
				// path
				curr += "|" + newPathRel;
			}
		} else {
			// there is still no other path
			curr = newPathRel;
		}
		pathNature.setProjectSourcePath(curr);
		PilarNature.getPilarNature(project).rebuildPath();
		return;
	}

}
