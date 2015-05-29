package org.arguside.ui.internal.wizards

import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.resources.IProject
import com.android.sdklib.IAndroidTarget
import org.sireum.util._
import org.eclipse.ui.IWorkingSet
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.Path
import org.eclipse.core.runtime.Platform
import org.eclipse.core.resources.IWorkspaceRunnable
import java.io.IOException
import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.IStatus
import org.arguside.core.CitConstants
import com.android.io.StreamException
import com.android.ide.eclipse.adt.internal.project.BaseProjectHelper
import org.eclipse.swt.widgets.Display
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.ui.PlatformUI
import org.eclipse.core.resources.IProjectDescription
import org.eclipse.core.runtime.SubProgressMonitor
import org.eclipse.core.runtime.OperationCanceledException
import org.eclipse.core.resources.IResource
import org.arguside.core.internal.project.Nature
import com.android.ide.eclipse.adt.AdtConstants
import org.arguside.util.decompile.ApkDecompiler
import org.eclipse.core.runtime.IPath
import java.io.File
import com.android.ide.eclipse.adt.internal.project.ProjectHelper
import org.eclipse.jdt.core.JavaCore
import org.eclipse.core.resources.IContainer
import org.eclipse.jdt.core.IClasspathEntry
import com.android.ide.eclipse.adt.internal.sdk.Sdk
import com.android.SdkConstants
import com.android.ide.eclipse.adt.AdtPlugin
import java.io.FileInputStream

object NewArgusProjectCreator {
  
  private final def BIN_DIRECTORY: String =
      SdkConstants.FD_OUTPUT + AdtConstants.WS_SEP
  private final def BIN_CLASSES_DIRECTORY: String =
      SdkConstants.FD_OUTPUT + AdtConstants.WS_SEP +
      SdkConstants.FD_CLASSES_OUTPUT + AdtConstants.WS_SEP
  
  private final def DEFAULT_DIRECTORIES = List(BIN_DIRECTORY, BIN_CLASSES_DIRECTORY)
      
  private def createEclipseProject(
      monitor: IProgressMonitor,
      project: IProject,
      description: IProjectDescription,
      target: IAndroidTarget): IProject = {
    
    val legacy = target.getVersion.getApiLevel < 4
    project.create(description, new SubProgressMonitor(monitor, 10))
    if(monitor.isCanceled()) throw new OperationCanceledException
    // Create project and open it
    project.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 10))
    // Add the Java, android and argus nature to the project
    Nature.setupProjectNatures(project, monitor, true)
    
    addDefaultDirectories(project, AdtConstants.WS_ROOT, DEFAULT_DIRECTORIES, monitor)
    
    val sourceFolders: IList[String] = List("src")
    
    val javaProject = JavaCore.create(project)
    setupSourceFolders(javaProject, sourceFolders, monitor)
    
    // add the default proguard config
    val libFolder = new File(AdtPlugin.getOsSdkToolsFolder,
          SdkConstants.FD_LIB);
    addLocalFile(project,
          new File(libFolder, SdkConstants.FN_PROJECT_PROGUARD_FILE),
          // Write ProGuard config files with the extension .pro which
          // is what is used in the ProGuard documentation and samples
          SdkConstants.FN_PROJECT_PROGUARD_FILE,
          monitor);

    // Set output location
    javaProject.setOutputLocation(project.getFolder(BIN_CLASSES_DIRECTORY).getFullPath(),
            monitor);
    
    Sdk.getCurrent().initProject(project, target)
    ProjectHelper.fixProject(project)
    project
  }
  
  /**
   * Adds default directories to the project.
   *
   * @param project The Java Project to update.
   * @param parentFolder The path of the parent folder. Must end with a
   *        separator.
   * @param folders Folders to be added.
   * @param monitor An existing monitor.
   * @throws CoreException if the method fails to create the directories in
   *         the project.
   */
  private def addDefaultDirectories(project: IProject, parentFolder: String,
          folders: IList[String], monitor: IProgressMonitor): Unit = {
    for (name <- folders) {
      if (name.length() > 0) {
        val folder = project.getFolder(parentFolder + name)
        if (!folder.exists()) {
          folder.create(true /* force */, true /* local */,
                  new SubProgressMonitor(monitor, 10))
        }
      }
    }
  }
  
  /**
   * Adds the given folder to the project's class path.
   *
   * @param javaProject The Java Project to update.
   * @param sourceFolders Template Parameters.
   * @param monitor An existing monitor.
   * @throws JavaModelException if the classpath could not be set.
   */
  private def setupSourceFolders(javaProject: IJavaProject, sourceFolders: IList[String],
          monitor: IProgressMonitor): Unit = {
    val project = javaProject.getProject()

    // get the list of entries.
    var entries: IList[IClasspathEntry] = javaProject.getRawClasspath().toList

    // remove the project as a source folder (This is the default)
    entries = removeSourceClasspath(entries, project)

    // add the source folders.
    for (sourceFolder <- sourceFolders) {
      val srcFolder = project.getFolder(sourceFolder)

      // remove it first in case.
      entries = removeSourceClasspath(entries, srcFolder)
      entries = ProjectHelper.addEntryToClasspath(entries.toArray,
              JavaCore.newSourceEntry(srcFolder.getFullPath())).toList
    }

    javaProject.setRawClasspath(entries.toArray, new SubProgressMonitor(monitor, 10));
  }
  
  /**
   * Adds a file to the root of the project
   * @param project the project to add the file to.
   * @param destName the name to write the file as
   * @param source the file to add. It'll keep the same filename once copied into the project.
   * @param monitor the monitor to report progress to
   * @throws FileNotFoundException if the file to be added does not exist
   * @throws CoreException if writing the file does not work
   */
  def addLocalFile(project: IProject, source: File, destName: String,
          monitor: IProgressMonitor): Unit = {
    val dest = project.getFile(destName)
    if (dest.exists() == false) {
      val stream = new FileInputStream(source)
      dest.create(stream, false /* force */, new SubProgressMonitor(monitor, 10))
    }
  }
  
  /**
   * Removes the corresponding source folder from the class path entries if
   * found.
   *
   * @param entries The class path entries to read. A copy will be returned.
   * @param folder The parent source folder to remove.
   * @return A new class path entries array.
   */
  private def removeSourceClasspath(entries: IList[IClasspathEntry], folder: IContainer): IList[IClasspathEntry] = {
    if (folder == null) {
      return entries
    }
    val source = JavaCore.newSourceEntry(folder.getFullPath())
    entries.filterNot(_.equals(source))
  }
  
  def create(monitor: IProgressMonitor,
             project: IProject,
             target: IAndroidTarget,
             apk: File,
             projectLocation: String,
             workingSets: IList[IWorkingSet]): Unit = {
    val workspace = ResourcesPlugin.getWorkspace
    val description = workspace.newProjectDescription(project.getName)
    
    if(projectLocation != null) {
      val path = new Path(projectLocation)
      val parent = new Path(path.toFile().getParent)
      val workspaceLocation = Platform.getLocation
      if(!workspaceLocation.equals(parent)) {
        description.setLocation(path)
      }
    }
    
    val workspaceRunnable = new IWorkspaceRunnable() {
      override def run(submonitor: IProgressMonitor) = {
        try {
          ApkDecompiler.decompile(apk, new Path(projectLocation))
          createEclipseProject(monitor, project, description, target)
        } catch {
          case e: IOException =>
            throw new CoreException(new Status(IStatus.ERROR, CitConstants.PluginId,
                            "Unexpected error while creating project", e))
          case e: StreamException =>
            throw new CoreException(new Status(IStatus.ERROR, CitConstants.PluginId,
                            "Unexpected error while creating project", e))
        }
        if(workingSets != null && workingSets.length > 0) {
          val javaProject = BaseProjectHelper.getJavaProject(project)
          if(javaProject != null) {
            Display.getDefault.syncExec(new WorksetAdder(javaProject, workingSets))
          }
        }
      }
    }
    
    ResourcesPlugin.getWorkspace.run(workspaceRunnable, monitor)
  }
  
  private class WorksetAdder(mProject: IJavaProject, mWorkingSets: IList[IWorkingSet]) extends Runnable {
    override def run: Unit = {
      if(mWorkingSets.length > 0 && mProject != null && mProject.exists){
        PlatformUI.getWorkbench.getWorkingSetManager.addToWorkingSets(mProject, mWorkingSets.toArray)
      }
    }
  }
}