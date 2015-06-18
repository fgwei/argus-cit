package org.arguside.core.internal.builder.jawa

import java.io.File
import org.eclipse.core.runtime.SubMonitor
import org.eclipse.core.resources.IProject
import org.arguside.util.eclipse.FileUtils
import argus.tools.eclipse.contribution.weaving.jdt.jcompiler.BuildManagerStore
import org.eclipse.core.resources.IncrementalProjectBuilder.INCREMENTAL_BUILD
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.resources.IResource
import org.arguside.core.internal.builder.JDTBuilderFacade
import org.arguside.core.IArgusPlugin
import org.sireum.jawa.sjc.compile.JavaCompiler
import org.sireum.util._
import org.sireum.jawa.sjc.log.Logger

/** Eclipse Java compiler interface, used by the jawa builder.
 *  This class forwards to the internal Eclipse Java compiler.
 */
class JavaEclipseCompiler(p: IProject, monitor: SubMonitor) extends JavaCompiler with JDTBuilderFacade {

  override def project = p

  def compile(sources: IList[File], options: IList[String], log: Logger) {
    val argusProject = IArgusPlugin().getArgusProject(project)

    val allSourceFiles = argusProject.allSourceFiles()
    val depends = argusProject.directDependencies
    if (allSourceFiles.exists(FileUtils.hasBuildErrors(_)))
      depends.toArray
    else {
      ensureProject

      // refresh output directories
      for (folder <- argusProject.outputFolders) {
        val container = ResourcesPlugin.getWorkspace().getRoot().getFolder(folder)
        container.refreshLocal(IResource.DEPTH_INFINITE, null)
      }

      BuildManagerStore.INSTANCE.setJavaSourceFilesToCompile(sources.toArray, project)
      try
        jawaJavaBuilder.build(INCREMENTAL_BUILD, new java.util.HashMap(), monitor)
      finally
        BuildManagerStore.INSTANCE.setJavaSourceFilesToCompile(null, project)

      refresh()
    }
  }
}
