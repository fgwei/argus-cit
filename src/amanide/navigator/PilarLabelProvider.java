package amanide.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import amanide.AmanIDEPlugin;
import amanide.editors.codecompletion.PilarPathHelper;
import amanide.navigator.elements.IWrappedResource;
import amanide.navigator.elements.PilarFolder;
import amanide.navigator.elements.PilarProjectSourceFolder;
import amanide.navigator.elements.PilarSourceFolder;
import amanide.navigator.elements.ProjectConfigError;
import amanide.preferences.FileTypesPreferencesPage;
import amanide.structure.TreeNode;
import amanide.ui.UIConstants;
import amanide.utils.Log;

/**
 * Provides the labels for the amanide package explorer.
 * 
 * @author Fengguo Wei
 */
public class PilarLabelProvider implements ILabelProvider {

	private WorkbenchLabelProvider provider;

	private volatile Image projectWithError = null;

	private Object lock = new Object();

	public PilarLabelProvider() {
		provider = new WorkbenchLabelProvider();
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof PilarProjectSourceFolder) {
			return AmanIDEPlugin.getImageCache().get(
					UIConstants.PROJECT_SOURCE_FOLDER_ICON);
		}
		if (element instanceof PilarSourceFolder) {
			return AmanIDEPlugin.getImageCache().get(
					UIConstants.SOURCE_FOLDER_ICON);
		}
		if (element instanceof PilarFolder) {
			PilarFolder folder = (PilarFolder) element;
			IFolder actualObject = folder.getActualObject();
			if (actualObject != null) {
				final String[] validSourceFiles = FileTypesPreferencesPage
						.getWildcardValidSourceFiles();
				for (String file : validSourceFiles) {
					if (actualObject.getFile(file).exists()) {
						return AmanIDEPlugin.getImageCache().get(
								UIConstants.FOLDER_PACKAGE_ICON);
					}
				}
			}
			return provider.getImage(actualObject);
		}
		if (element instanceof IWrappedResource) {
			IWrappedResource resource = (IWrappedResource) element;
			Object actualObject = resource.getActualObject();
			if (actualObject instanceof IFile) {
				IFile iFile = (IFile) actualObject;
				final String name = iFile.getName();

				if (name.indexOf('.') == -1) {
					try {
						if (PilarPathHelper.markAsAmanIDEFileIfDetected(iFile)) {
							return AmanIDEPlugin.getImageCache().get(
									UIConstants.PILAR_FILE_ICON);
						}
					} catch (Exception e) {
						// Ignore
					}
				}
			}
			return provider.getImage(actualObject);
		}
		if (element instanceof ProjectConfigError) {
			return AmanIDEPlugin.getImageCache().get(UIConstants.ERROR);
		}
		if (element instanceof TreeNode<?>) {
			TreeNode<?> treeNode = (TreeNode<?>) element;
			LabelAndImage data = (LabelAndImage) treeNode.getData();
			return data.image;
		}
		if (element instanceof IProject) {
			IProject project = (IProject) element;
			if (!project.isOpen()) {
				return null;
			}
			IMarker[] markers;
			try {
				markers = project
						.findMarkers(
								PilarBaseModelProvider.AMANIDE_PACKAGE_EXPORER_PROBLEM_MARKER,
								true, 0);
			} catch (CoreException e1) {
				Log.log(e1);
				return null;
			}
			if (markers == null || markers.length == 0) {
				return null;
			}

			// We have errors: make them explicit.
			if (projectWithError == null) {
				synchronized (lock) {
					if (projectWithError == null) {
						Image image = provider.getImage(element);
						try {
							DecorationOverlayIcon decorationOverlayIcon = new DecorationOverlayIcon(
									image, AmanIDEPlugin.getImageCache()
											.getDescriptor(
													UIConstants.ERROR_SMALL),
									IDecoration.BOTTOM_LEFT);
							projectWithError = decorationOverlayIcon
									.createImage();
						} catch (Exception e) {
							Log.log("Unable to create error decoration for project icon.",
									e);
							projectWithError = image;
						}
					}
				}
			}

			return projectWithError;
		}

		if (element instanceof IWorkingSet) {
			return AmanIDEPlugin.getImageCache().get(UIConstants.WORKING_SET);
		}
		return null;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof PilarSourceFolder) {
			PilarSourceFolder sourceFolder = (PilarSourceFolder) element;
			return provider.getText(sourceFolder.container);
		}

		if (element instanceof IWrappedResource) {
			IWrappedResource resource = (IWrappedResource) element;
			return provider.getText(resource.getActualObject());
		}
		if (element instanceof TreeNode<?>) {
			TreeNode<?> treeNode = (TreeNode<?>) element;
			LabelAndImage data = (LabelAndImage) treeNode.getData();
			return data.label;
		}
		if (element instanceof ProjectConfigError) {
			return ((ProjectConfigError) element).getLabel();
		}

		return provider.getText(element);
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
		provider.addListener(listener);
	}

	@Override
	public void dispose() {
		provider.dispose();
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return provider.isLabelProperty(element, property);
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		provider.removeListener(listener);
	}

}
