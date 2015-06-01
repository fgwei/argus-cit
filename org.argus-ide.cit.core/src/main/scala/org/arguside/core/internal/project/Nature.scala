package org.arguside.core.internal.project

import scala.collection.mutable.ArrayBuffer
import org.eclipse.core.resources.ICommand
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IProjectNature
import org.eclipse.core.resources.IResource
import org.eclipse.jdt.core.IClasspathEntry
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.launching.JavaRuntime
import org.eclipse.core.runtime.Path
import org.arguside.core.CitConstants
import org.arguside.util.eclipse.EclipseUtils
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.NullProgressMonitor
import com.android.ide.eclipse.adt.AdtConstants
import org.sireum.util._
import com.android.ide.eclipse.adt.internal.build.builders.PostCompilerBuilder


class Nature extends IProjectNature {
  private var project : IProject = _

  override def getProject = project
  override def setProject(project : IProject) = this.project = project

  override def configure() {
    if (project == null || !project.isOpen)
      return
      
    updateBuilders(project, List(JavaCore.BUILDER_ID), Some(CitConstants.BuilderId))
  }

  override def deconfigure() {
    if (project == null || !project.isOpen)
      return
      
    updateBuilders(project, List(CitConstants.BuilderId), Some(JavaCore.BUILDER_ID))
  }
  
  private def updateBuilders(project: IProject, buildersToRemove: List[String], builderToAdd: Option[String]) {
    EclipseUtils.withSafeRunner(s"Error occurred while trying to update builder of project '$project'") {
      val description = project.getDescription
      val previousCommands = description.getBuildSpec
      val filteredCommands = previousCommands.filterNot(buildersToRemove contains _.getBuilderName).toList
      val newCommands: IList[ICommand] = 
        if (!builderToAdd.isDefined || filteredCommands.exists(_.getBuilderName == builderToAdd.get))
          filteredCommands
        else
          filteredCommands :+ {
            val newBuilderCommand = description.newCommand
            newBuilderCommand.setBuilderName(builderToAdd.get)
            newBuilderCommand
          }
      //always keep the android builder at the end
      val newerCommands: IList[ICommand] = newCommands.filterNot(_.getBuilderName == PostCompilerBuilder.ID) ::: newCommands.filter(_.getBuilderName == PostCompilerBuilder.ID)
      description.setBuildSpec(newerCommands.toArray)
      project.setDescription(description, IResource.FORCE, null)
    }
  }

}

object Nature {
  def setupProjectNatures(project: IProject, monitor: IProgressMonitor, addAndroidNature: Boolean) {
    var usemonitor = monitor
    if(project == null || !project.isOpen()) return
    if(usemonitor == null) usemonitor = new NullProgressMonitor
    updateNatureIds(project, monitor)(_ filter (x => false))
    // Add the natures. We need to add the Java nature first, so it adds its builder to the
    // project first. This way, when the android nature is added, we can control where to put
    // the android builders in relation to the java builder.
    // Adding the java nature after the android one, would place the java builder before the
    // android builders.
    updateNatureIds(project, monitor)(JavaCore.NATURE_ID +: _)
    if(addAndroidNature) updateNatureIds(project, monitor)(AdtConstants.NATURE_DEFAULT +: _)
    updateNatureIds(project, monitor)(CitConstants.NatureId +: _)
  }
  
  def updateNatureIds(project: IProject, monitor: IProgressMonitor)(natureIdUpdater: Array[String] => Array[String]) {
    val projectDescription = project.getDescription
    val currentNatureIds = projectDescription.getNatureIds
    val updatedNatureIds = natureIdUpdater(currentNatureIds)
    projectDescription.setNatureIds(updatedNatureIds)
    project.setDescription(projectDescription, monitor)
    project.touch(null)
  }
}