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
import com.android.ide.eclipse.adt.internal.actions.AddSupportJarAction
import org.arguside.core.internal.ArgusPlugin
import org.eclipse.core.resources.IFile
import com.android.ide.eclipse.adt.AdtUtils
import org.eclipse.ltk.core.refactoring.NullChange
import org.apache.commons.io.IOUtils
import com.google.common.io.Files
import com.google.common.base.Charsets
import com.android.SdkConstants._
import com.android.ide.eclipse.adt.internal.editors.formatting.EclipseXmlPrettyPrinter
import com.android.ide.eclipse.adt.internal.editors.formatting.EclipseXmlFormatPreferences
import org.eclipse.jdt.core.ToolFactory
import org.eclipse.jdt.core.formatter.CodeFormatter
import org.eclipse.ltk.core.refactoring.Change
import org.eclipse.ltk.core.refactoring.TextFileChange
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.BadLocationException
import org.eclipse.text.edits.InsertEdit
import org.eclipse.ltk.core.refactoring.CompositeChange
import java.lang.reflect.InvocationTargetException

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
             workingSets: IList[IWorkingSet],
             mValues: NewArgusProjectWizardState): Unit = {
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
          val dependencies = ApkDecompiler.decompile(apk, new Path(projectLocation), true).get
          dependencies foreach {
            d =>
              d match {
                case CitConstants.MAVEN_SUPPORT_V4 | CitConstants.MAVEN_SUPPORT_V13 => 
                  val path = 
                    if(d == CitConstants.MAVEN_SUPPORT_V4) AddSupportJarAction.getSupportJarFile
                    else AddSupportJarAction.getSupport13JarFile
                  val to = getTargetPath(SdkConstants.FD_NATIVE_LIBS + '/' + path.getName)
                  mValues.finalizingActions.addAction(new Runnable(){
                    override def run(): Unit = {
                      try {
                        val changes: ISet[Change] = copy(path, to, project)
                        if (!changes.isEmpty) {
                          monitor.beginTask("Creating project...", changes.size)
                          try {
                            val composite = new CompositeChange("", changes.toArray)
                            composite.perform(monitor);
                          } catch {
                            case e: CoreException =>
                              ArgusPlugin().logError(null, e)
                              throw new InvocationTargetException(e)
                          } finally {
                            monitor.done()
                          }
                        }
                      } catch {
                        case ioe: IOException =>
                          ArgusPlugin().logError(null, ioe)
                      }
                    }
                  })
                case CitConstants.MAVEN_APPCOMPAT =>
                  mValues.finalizingActions.addAction(new Runnable(){
                    override def run(): Unit = {
                      AddSupportJarAction.installAppCompatLibrary(project, true)
                    }
                  })
                case _ =>
              }
          }
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
  
  private def getTargetPath(relative: String): IPath = {
    var temp = relative
    if (temp.indexOf('\\') != -1) {
      temp = temp.replace('\\', '/')
    }
    return new Path(temp)
  }
  
  /**
   * Copies the given source file into the given destination file (where the
   * source is allowed to be a directory, in which case the whole directory is
   * copied recursively)
   */
  private def copy(src: File, path: IPath, project: IProject): ISet[Change] = {
    val changes: MSet[Change] = msetEmpty
    if (src.isDirectory()) {
      val children = src.listFiles()
      if (children != null) {
        for (child <- children) {
          changes ++= copy(child, path.append(child.getName()), project)
        }
      }
    } else {
      val dest = project.getFile(path)
      if (dest.exists() && !(dest.isInstanceOf[IFile])) {// Don't attempt to overwrite a folder
        return changes.toSet
      }
      val file: IFile = dest.asInstanceOf[IFile]
      val targetName = path.lastSegment()
      if (dest.isInstanceOf[IFile]) {
        if (dest.exists() && isIdentical(Files.toByteArray(src), file)) {
          val label = String.format(
                  "Not overwriting %1$s because the files are identical", targetName)
          val change = new NullChange(label)
          change.setEnabled(false)
          changes.add(change)
          return changes.toSet
        }
      }
      
      if (targetName.endsWith(DOT_XML)
        || targetName.endsWith(DOT_JAVA)
        || targetName.endsWith(DOT_TXT)
        || targetName.endsWith(DOT_RS)
        || targetName.endsWith(DOT_AIDL)
        || targetName.endsWith(DOT_SVG)) {

        var newFile = Files.toString(src, Charsets.UTF_8)
        newFile = format(project, newFile, path)

        val addFile = createNewFileChange(file)
        addFile.setEdit(new InsertEdit(0, newFile))
        changes += addFile
      } else {
          // Write binary file: Need custom change for that
          val workspacePath = project.getFullPath().append(path)
          changes += new CreateFileChange(targetName, workspacePath, src)
      }
    }
    changes.toSet
  }
  
  private def format(project: IProject, contents: String, to: IPath): String = {
    
    import scala.collection.JavaConversions._
    
    val name = to.lastSegment()
    if (name.endsWith(DOT_XML)) {
      val formatStyle = EclipseXmlPrettyPrinter.getForFile(to)
      val prefs = EclipseXmlFormatPreferences.create()
      return EclipseXmlPrettyPrinter.prettyPrint(contents, prefs, formatStyle, null)
    } else if (name.endsWith(DOT_JAVA)) {
      var options: java.util.Map[_, _] = null
      if (project != null && project.isAccessible()) {
        try {
          val javaProject = BaseProjectHelper.getJavaProject(project)
          if (javaProject != null) {
            options = javaProject.getOptions(true)
          }
        } catch {
          case e: CoreException =>
            ArgusPlugin().logError(null, e)
        }
      }
      if (options == null) {
        options = JavaCore.getOptions()
      }

      val formatter = ToolFactory.createCodeFormatter(options)

      try {
        val doc = new org.eclipse.jface.text.Document()
        // format the file (the meat and potatoes)
        doc.set(contents);
        val edit = formatter.format(
              CodeFormatter.K_COMPILATION_UNIT | CodeFormatter.F_INCLUDE_COMMENTS,
              contents, 0, contents.length(), 0, null);
        if (edit != null) {
          edit.apply(doc)
        }

        return doc.get()
      } catch {
        case e: Exception =>
          ArgusPlugin().logError(null, e)
      }
    }

    contents
  }
  
  private def createNewFileChange(targetFile: IFile): TextFileChange = {
    val fileName = targetFile.getName()
    var message: String = null
    if (targetFile.exists()) {
        message = String.format("Replace %1$s", fileName)
    } else {
        message = String.format("Create %1$s", fileName)
    }

    val change = new TextFileChange(message, targetFile) {
        override protected def acquireDocument(pm: IProgressMonitor): IDocument = {
          val document = super.acquireDocument(pm)

          // In our case, we know we *always* use this TextFileChange
          // to *create* files, we're not appending to existing files.
          // However, due to the following bug we can end up with cached
          // contents of previously deleted files that happened to have the
          // same file name:
          //   https://bugs.eclipse.org/bugs/show_bug.cgi?id=390402
          // Therefore, as a workaround, wipe out the cached contents here
          if (document.getLength() > 0) {
              try {
                document.replace(0, document.getLength(), "");
              } catch {
                case e: BadLocationException =>
                  // pass
              }
          }

          document
        }
    }
    change.setTextType(fileName.substring(fileName.lastIndexOf('.') + 1))
    change
  }
  
  /** Returns true if the given file contains the given bytes */
  private def isIdentical(data: Array[Byte], dest: IFile): Boolean = {
    assert(dest.exists())
    val existing = AdtUtils.readData(dest)
    java.util.Arrays.equals(existing, data)
  }
  
  private class WorksetAdder(mProject: IJavaProject, mWorkingSets: IList[IWorkingSet]) extends Runnable {
    override def run: Unit = {
      if(mWorkingSets.length > 0 && mProject != null && mProject.exists){
        PlatformUI.getWorkbench.getWorkingSetManager.addToWorkingSets(mProject, mWorkingSets.toArray)
      }
    }
  }
}