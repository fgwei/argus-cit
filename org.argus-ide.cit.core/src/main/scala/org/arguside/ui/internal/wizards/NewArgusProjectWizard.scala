package org.arguside.ui.internal.wizards

import org.eclipse.jface.wizard.Wizard
import org.eclipse.ui.INewWizard
import org.arguside.ui.ArgusImages
import org.eclipse.jface.wizard.WizardPage
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.OperationCanceledException
import scala.collection.mutable.ArrayBuffer
import com.android.ide.eclipse.adt.internal.wizards.templates.NewProjectWizard
import org.eclipse.ui.IWorkbench
import org.eclipse.jface.viewers.IStructuredSelection
import com.android.ide.eclipse.adt.internal.wizards.templates.NewProjectPage
import com.android.ide.eclipse.adt.internal.wizards.templates.NewProjectWizardState
import com.android.ide.eclipse.adt.AdtUtils
import org.eclipse.core.resources.ResourcesPlugin
import org.arguside.core.internal.ArgusPlugin
import org.arguside.logging.HasLogger
import org.eclipse.core.resources.IProject
import com.android.ide.eclipse.adt.internal.wizards.newproject.NewProjectCreator.ProjectPopulator
import java.io.File
import com.android.ide.eclipse.adt.internal.project.BaseProjectHelper
import com.android.ide.eclipse.adt.internal.project.ProjectHelper
import org.eclipse.core.resources.IResource
import org.eclipse.core.runtime.NullProgressMonitor


class NewArgusProjectWizard extends NewProjectWizard with HasLogger{
  
  private var mMainPage: NewArgusProjectWizardPage = null
  private var mValues: NewArgusProjectWizardState = null
  /** The project being created */
  private var mProject: IProject = null
  
  override def init(workbench: IWorkbench, selection: IStructuredSelection): Unit = {
    super.init(workbench, selection)
    setWindowTitle("New Argus Project")
    setDefaultPageImageDescriptor(ArgusImages.ARGUS_PROJECT_WIZARD)
    mValues = new NewArgusProjectWizardState()
    mMainPage = new NewArgusProjectWizardPage(mValues)
    mMainPage.setTitle("Create an Argus project")
    mMainPage.init(selection, AdtUtils.getActivePart)
  }
  
  override def addPages(): Unit = {
    addPage(mMainPage)
  }
  
  override def canFinish: Boolean = {
    for (page <- getPages()) {
      if (!page.isPageComplete()) {
        return false
      }
    }
    true
  }
  
  override protected def performFinish(monitor: IProgressMonitor): Boolean = {
    try {
      val root = ResourcesPlugin.getWorkspace().getRoot()
      val name = mValues.projectName
      val apk: File = new File(mValues.applicationName)
      mProject = root.getProject(name)
      NewArgusProjectCreator.create(monitor, mProject, mValues.target, apk, mValues.projectLocation, mValues.workingSets)
      
      val javaProject = BaseProjectHelper.getJavaProject(mProject)
      if(javaProject != null){
        ProjectHelper.enforcePreferredCompilerCompliance(javaProject)
      }
      
      try {
        mProject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor())
      } catch {
        case e: Exception =>
          logger.error(null, e)
      }
      
      true
    } catch {
      case ioe: Exception =>
        logger.error(null, ioe)
        false
    }
  }
}