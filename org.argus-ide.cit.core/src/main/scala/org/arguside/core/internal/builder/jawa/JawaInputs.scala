package org.arguside.core.internal.builder.jawa

import java.io.File
import org.arguside.core.IArgusProject
import org.eclipse.core.runtime.SubMonitor
import org.sireum.jawa.sjc.compile.CompileProgress
import org.sireum.jawa.sjc.compile.Compilers
import org.sireum.jawa.sjc.compile.JawaCompiler
import org.sireum.jawa.sjc.compile.DefinesClass
import java.util.zip.ZipFile
import org.sireum.jawa.sjc.util.cp.ClasspathUtilities
import org.sireum.jawa.sjc.util.cp.Locate
import org.sireum.util._
import org.sireum.jawa.sjc.compile.MultipleOutput
import org.eclipse.core.resources.IContainer


/**
 * @author fgwei
 */
class JawaInputs(
    sourceFiles: IList[File],
    project: IArgusProject,
    javaMonitor: SubMonitor,
    jawaProgress: CompileProgress,
    srcOutputs: IList[(IContainer, IContainer)] = ilistEmpty) {

  def sources = sourceFiles
  
  def progress = jawaProgress
  
  def output = new MultipleOutput {
    private def sourceOutputFolders =
      if (srcOutputs.nonEmpty) srcOutputs else project.sourceOutputFolders.toList

    def outputGroups: IList[OutputGroup] = sourceOutputFolders.map {
      case (src, out) => new OutputGroup {
        def sourceDirectory = src.getLocation.toFile
        def outputDirectory = out.getLocation.toFile
      }
    }
  }
  
  def javacOptions: IList[String] = ilistEmpty
  
  def compilers: Compilers[JawaCompiler] = {
    new Compilers[JawaCompiler] {
      def javac = new JavaEclipseCompiler(project.underlying, javaMonitor)
      def jawac = new JawaCompiler()
    }
  }
  
}

private[jawa] object Locator {
  val NoClass = new DefinesClass {
    def apply(className: String) = false
  }

  def apply(f: File): DefinesClass =
    if (f.isDirectory)
      new DirectoryLocator(f)
    else if (f.exists && ClasspathUtilities.isArchive(f))
      new JarLocator(f)
    else
      NoClass

  class DirectoryLocator(dir: File) extends DefinesClass {
    def apply(className: String): Boolean = Locate.classFile(dir, className).isFile
  }

  class JarLocator(jar: File) extends DefinesClass {
    lazy val entries: Set[String] = {
      val zipFile = new ZipFile(jar, ZipFile.OPEN_READ)
      try {
        import scala.collection.JavaConverters._
        zipFile.entries.asScala.filterNot(_.isDirectory).map(_.getName).toSet
      } finally
        zipFile.close()
    }

    def apply(className: String): Boolean = entries.contains(className)
  }
}