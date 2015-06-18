package org.arguside.core.internal.builder.jawa

import org.arguside.core.IArgusProject
import org.eclipse.core.resources.IFile
import org.eclipse.core.runtime.IPath
import org.eclipse.core.resources.IContainer
import org.arguside.core.internal.builder.CachedAnalysisBuildManager
import org.arguside.logging.HasLogger
import org.eclipse.core.runtime.SubMonitor
import org.sireum.util._
import org.eclipse.core.runtime.OperationCanceledException
import org.arguside.core.internal.builder.BuildProblemMarker
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.resources.IResource
import org.arguside.core.internal.builder.TaskManager
import java.io.File
import org.eclipse.core.runtime.Path
import org.sireum.jawa.sjc.compile.CompileProgress
import org.arguside.core.internal.ArgusPlugin
import EclipseJawaBuildManager.FileHelper
import org.sireum.jawa.sjc.compile.CompileFailed
import org.sireum.jawa.sjc.compile.AggressiveCompile
import org.sireum.jawa.sjc.log.{Logger => sjcLogger}
import org.sireum.jawa.sjc.util.NoPosition
import org.sireum.jawa.sjc.log.Severity
import org.sireum.jawa.sjc.DefaultReporter


/**
 * @author fgwei
 */
class EclipseJawaBuildManager(val project: IArgusProject, analysisCache: Option[IFile] = None,
  addToClasspath: Seq[IPath] = Seq.empty, srcOutputs: Seq[(IContainer, IContainer)] = Seq.empty)
    extends CachedAnalysisBuildManager with HasLogger {
  import EclipseJawaBuildManager._
  
  /** Initialized in `build`, used by the SbtProgress. */
  private var monitor: SubMonitor = _

  private val sources: MSet[IFile] = msetEmpty

  def analysisStore = analysisCache.getOrElse(project.underlying.getFile(".cache"))
  private def cacheFile = analysisStore.getLocation.toFile
  
  // this directory is used by Jawa to store classfiles between
  // compilation runs to implement all-or-nothing compilation
  // sementics. Original files are copied over to tempDir and
  // moved back in case of compilation errors.
  private val tempDir = project.underlying.getFolder(".tmpBin")
  private def tempDirFile = tempDir.getLocation().toFile()

  private val jawaLogger = new sjcLogger {
    override def error(msg: String) = logger.error(msg)
    override def warn(msg: String) = logger.warn(msg)
    override def info(msg: String) = logger.info(msg)
    override def debug(msg: String) = logger.debug(msg)
    override def trace(exc: Throwable) = logger.error("", exc)
  }
  
  private lazy val jawaReporter = new DefaultReporter
  
  override def build(addedOrUpdated: Set[IFile], removed: Set[IFile], pm: SubMonitor): Unit = {
    jawaReporter.reset()
    val toBuild = addedOrUpdated -- removed
    monitor = pm
    hasInternalErrors = false
    try {
      update(toBuild, removed)
    } catch {
      case oce: OperationCanceledException =>
        throw oce
      case e: Throwable =>
        hasInternalErrors = true
        BuildProblemMarker.create(project, e)
        eclipseLog.error("Error in Jawa compiler", e)
        jawaReporter.error(NoPosition, "Jawa builder crashed while compiling. The error message is '" + e.getMessage() + "'. Check Error Log for details.")
    }
    hasInternalErrors = jawaReporter.hasErrors || hasInternalErrors
  }
  
  

  override def clean(implicit monitor: IProgressMonitor) {
    analysisStore.refreshLocal(IResource.DEPTH_ZERO, null)
    analysisStore.delete(true, false, monitor)
  }

  override def invalidateAfterLoad: Boolean = true

  override def canTrackDependencies: Boolean = true

  /** Remove the given files from the managed build process. */
  private def removeFiles(files: scala.collection.Set[IFile]) {
    if (!files.isEmpty)
      sources --= files
  }

  /** The given files have been modified by the user. Recompile
   *  them and their dependent files.
   */
  private def update(added: ISet[IFile], removed: ISet[IFile]) {
    if (added.isEmpty && removed.isEmpty)
      logger.info("No changes in project, running the builder for potential transitive changes.")
    clearTasks(added)
    removeFiles(removed)
    sources ++= added
    runCompiler(sources.asJFiles)
  }

  private def clearTasks(included: scala.collection.Set[IFile]) {
    included foreach TaskManager.clearTasks
  }

  private def runCompiler(sources: IList[File]): Unit = {
    logger.info(s"Running compiler")
    val progress = new JawaCompilerProgress
    val inputs = new JawaInputs(sources, project, monitor, progress)
    try
      aggressiveCompile(inputs, jawaLogger)
    catch {
      case _: CompileFailed => Nil
    }
//    analysis foreach setCached
  }
  
  private def aggressiveCompile(in: JawaInputs, log: sjcLogger): Unit = {
    val compilers = in.compilers
    val agg = new AggressiveCompile(cacheFile)
    val defClass = (f: File) => { val dc = Locator(f); (name: String) => dc.apply(name) }

    import compilers._
    agg(jawac, javac, in.sources, in.output, in.progress, in.javacOptions,
      defClass, jawaReporter)(log)
  }

  private class JawaCompilerProgress extends CompileProgress {
    private var lastWorked = 0
    private var savedTotal = 0
    private var throttledMessages = 0

    private var compiledFiles: Set[IFile] = Set()

    /** Return the set of files that were reported as being compiled during this session */
    def actualCompiledFiles: Set[IFile] = compiledFiles

    override def startUnit(unitPath: String): Unit = {
      def unitIPath: IPath = Path.fromOSString(unitPath)

      if (monitor.isCanceled)
        throw new OperationCanceledException

      // It turned out that updating `subTaks` too often slowed down the build... and the UI was blocked
      // going through all the updates, long after compilation finished. So we report only 1:10 updates.
      throttledMessages += 1
      if (throttledMessages == 10) {
        throttledMessages = 0
        val projectPath = project.javaProject.getProject.getLocation
        monitor.subTask("Compile " + unitIPath.makeRelativeTo(projectPath))
        
      }
    }

    override def advance(current: Int, total: Int): Boolean =
      if (monitor.isCanceled) {
        throw new OperationCanceledException
      } else {
        if (savedTotal != total) {
          monitor.setWorkRemaining(total - lastWorked)
          savedTotal = total
        }

        if (lastWorked < current) {
          monitor.worked(current - lastWorked)
          lastWorked = current
        }
        true
      }
  }
  
}

object EclipseJawaBuildManager {
  private implicit class FileHelper(val files: scala.collection.Set[IFile]) extends AnyVal {
    def asJFiles: IList[File] = files.map(ifile => ifile.getLocation.toFile).toList
  }
}