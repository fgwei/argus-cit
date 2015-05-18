package org.arguside.core
package compiler

import org.eclipse.core.resources.IFile
import org.eclipse.jdt.core.compiler.IProblem
import org.arguside.core.IArgusProject
import org.sireum.jawa.sjc.interactive.Response
import org.sireum.util.IList
import org.sireum.jawa.sjc.io.AbstractFile
import org.sireum.jawa.sjc.util.SourceFile
import org.sireum.jawa.sjc.util.FgSourceFile

/** Position information relative to a source transformation.
 *
 *  This translates sources from an original source to a target source,
 *  and performs offset to line manipulations.
 *
 *  All methods may throw `IndexOutOfBoundsException` if the given input is invalid.
 */
trait IPositionInformation extends (Int => Int) {
  /** Map the given offset to the target offset. */
  def apply(offset: Int): Int

  /** Return the line number corresponding to this offset. */
  def offsetToLine(offset: Int): Int

  /** Return the offset corresponding to this line number. */
  def lineToOffset(line: Int): Int
}

object IPositionInformation {

  /** A plain Scala implementation based on the given source file.
   *
   *  This performs no transformation on positions.
   */
  def plainJawa(sourceFile: SourceFile): IPositionInformation =
    new PlainJawaPosition(sourceFile)
}

/** An implementation of position information that is based on a Scala SourceFile implementation
 */
class PlainJawaPosition(sourceFile: SourceFile) extends IPositionInformation {
  def apply(pos: Int): Int = pos

  def offsetToLine(offset: Int): Int = sourceFile.offsetToLine(offset)

  def lineToOffset(line: Int): Int = sourceFile.lineToOffset(line)
}

/** A Jawa compilation unit.
 *
 *  This class is a stable representation of a compilation unit.
 *  Implementations are expected to be thread-safe.
 */
trait InteractiveCompilationUnit {

  /** The `SourceFile` that the Jawa compiler uses to read this compilation unit. It should not change through the lifetime of this unit. */
  def file: AbstractFile
  
  lazy val sourceFile = new FgSourceFile(file)
  
  /** Map from the original source into the corresponding position in the Scala translation. */
  def jawaPos: IPositionInformation = IPositionInformation.plainJawa(sourceFile)

  /** Return the current contents of this compilation unit. This is the 'original' contents, that may be
   *  translated to a Scala source using `sourceMap`.
   *
   *  If we take Play templates as an example, this method would return HTML interspersed with Scala snippets. If
   *  one wanted the translated Scala source, he'd have to call `lastSourceMap().scalaSource`
   */
  def getContents(): Array[Char]
  
  /** The workspace file corresponding to this compilation unit. */
  def workspaceFile: IFile

  /** Does this unit exist in the workspace? */
  def exists(): Boolean

  /** The Argus project to which this compilation unit belongs. */
  def argusProject: IArgusProject

  /** Schedule this unit for reconciliation with the new contents. This marks the current unit as *dirty*. 
   *  At the next reconciliation
   *  round (typically after 500ms of inactivity), all dirty units are flushed and all managed
   *  units are checked.
   *
   *  @param newContents The new contents of this compilation unit.
   */
  def scheduleReconcile(newContents: Array[Char]): Unit = {
    argusProject.presentationCompiler { pc =>
      pc.scheduleReload(this, sourceFile)
    }
  }

  /** Force a reconciliation round. This involves flushing all pending (dirty) compilation
   *  units and waiting for this compilation unit to be type-checked. It returns all compilation
   *  problems corresponding to this unit.
   *
   *  @note This is usually called from an active editor that needs to update error annotations.
   *        Other code should prefer calling `currentProblems`, which won't interfere with the
   *        reconciliation strategy.
   */
  def forceReconcile(): IList[JawaCompilationProblem] = {
    argusProject.presentationCompiler(_.flushScheduledReloads())
    currentProblems()
  }

  /** Schedule the unit for reconciliation and add it to the presentation compiler managed units. This should
   *  be called before any other calls to {{{IScalaPresentationCompiler.scheduleReload}}}
   *
   *  This method is the entry-point to the managed units in the presentation compiler: it should perform an initial
   *  askReload and add the unit to the managed set, so from now on `scheduleReload` can be used instead.
   *
   *  This method should not block.
   */
  def initialReconcile(): Response[Unit] = {
    val reloaded = argusProject.presentationCompiler { compiler =>
      compiler.askReload(this, sourceFile)
    } getOrElse {
      val dummy = new Response[Unit]
      dummy.set(())
      dummy
    }
    reloaded
  }

  /** Return all compilation errors from this unit.
   *
   *  Compilation errors and warnings are positioned relative to the original source.
   */
  def currentProblems(): List[JawaCompilationProblem] = {
    import scala.util.control.Exception.failAsValue

    argusProject.presentationCompiler { pc =>
      val probs = pc.problemsOf(this)
      for (p <- probs) yield {
        p.copy(start = failAsValue(classOf[IndexOutOfBoundsException])(0)(jawaPos(p.start)),
          end = failAsValue(classOf[IndexOutOfBoundsException])(1)(jawaPos(p.end)),
          lineNumber = failAsValue(classOf[IndexOutOfBoundsException])(1)(jawaPos(p.lineNumber - 1)) + 1)
      }
    }.getOrElse(Nil)
  }

  /** Perform an operation on the source file, with the current presentation compiler.
   *
   *  @param op The operation to be performed
   */
  def withSourceFile[T](op: (SourceFile, IJawaPresentationCompiler) => T): Option[T] = {
    argusProject.presentationCompiler(op(sourceFile, _))
  }
}
