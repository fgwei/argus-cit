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


class Nature extends IProjectNature {
  private var project : IProject = _

  override def getProject = project
  override def setProject(project : IProject) = this.project = project

  override def configure() {
    if (project == null || !project.isOpen)
      return
  }

  override def deconfigure() {
    if (project == null || !project.isOpen)
      return
  }

}
