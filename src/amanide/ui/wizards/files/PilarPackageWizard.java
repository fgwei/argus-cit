/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 17, 2006
 */
package amanide.ui.wizards.files;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import amanide.utils.StringUtils;

public class PilarPackageWizard extends AbstractPilarWizard {

	public PilarPackageWizard() {
		super("Create a new Pilar package");
	}

	public static final String WIZARD_ID = "amanide.ui.wizards.files.PilarPackageWizard";

	@Override
	protected AbstractPilarWizardPage createPathPage() {
		return new AbstractPilarWizardPage(this.description, selection) {

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
				if (getValidatedSourceFolder().findMember(text) != null) {
					return "The package " + text
							+ " already exists in the source folder "
							+ getValidatedSourceFolder().getName() + ".";
				}
				return null;
			}

		};
	}

	/**
	 * We will create the complete package path given by the user.
	 */
	@Override
	protected void doCreateNew(IProgressMonitor monitor) throws CoreException {
		createPackage(monitor, filePage.getValidatedSourceFolder(),
				filePage.getValidatedName());
	}

	public static void createPackage(IProgressMonitor monitor,
			IContainer validatedSourceFolder, String packageName)
			throws CoreException {
		if (validatedSourceFolder == null) {
			return;
		}
		IContainer parent = validatedSourceFolder;
		for (String packagePart : StringUtils.dotSplit(packageName)) {
			IFolder folder = parent.getFolder(new Path(packagePart));
			if (!folder.exists()) {
				folder.create(true, true, monitor);
			}
			parent = folder;
		}
	}

}
