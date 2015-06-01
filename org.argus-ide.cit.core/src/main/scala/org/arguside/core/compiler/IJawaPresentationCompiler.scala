package org.arguside.core.compiler

import org.eclipse.jdt.core.compiler.IProblem
import org.eclipse.core.resources.IFile
import scala.concurrent.duration._
import org.arguside.logging.HasLogger
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.core.IJavaProject
import org.arguside.core.IArgusPlugin
import org.arguside.core.IArgusProject
import org.arguside.core.internal.compiler.JawaPresentationCompiler
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.hyperlink.IHyperlink
import org.sireum.jawa.sjc.interactive.Global
import org.sireum.jawa.sjc.parser.JawaAstNode
import org.sireum.jawa.sjc.util.Position
import org.sireum.jawa.sjc.util.SourceFile
import org.sireum.jawa.sjc.interactive.Response
import org.sireum.util.IList
import org.sireum.jawa.sjc.lexer.{Token => JawaToken}
import org.sireum.jawa.sjc.util.FailedInterrupt
import org.sireum.jawa.sjc.interactive.FreshRunReq
import org.sireum.jawa.sjc.interactive.MissingResponse
import org.sireum.jawa.sjc.parser.CompilationUnit
import org.sireum.jawa.sjc.interactive.{JawaElement => SJCJawaElement}

/** This interface provides access to Jawa Presentation compiler services. Even though methods are inherited from
 *  `org.sireum.jawa.sjc.interactive.Global`, prefer the convenience methods offered in this trait.
 *
 *  The presentation compiler is an asynchronous layer on top of the Jawa resolver. The PC
 *  works by *managing* a set of compilation units. A managed unit is called 'loaded', and
 *  all loaded units are parsed and resolved together. A new compilation *run* is triggered
 *  by a *re-load*.
 *
 *  A unit can be in the following states:
 *    - unloaded (unmanaged). The PC won't attempt to parse it. Usually, any Jawa file that is not
 *               open in an editor is not loaded
 *    - loaded. This state corresponds to units open inside an editor. In this state, every askReload (for example,
 *              triggered by a keystroke) will parse and resolve (and report errors as a side effect) such units. A
 *              unit gets in this state after a call to `askReload(unit, contents)`.
 *    - dirty. Units that have changes that haven't been reloaded yet. This is usually a subset of `loaded` (excluding
 *             files that have been deleted or closed). A unit is added to the dirty set using `scheduleReload`, and removed
 *             when `flushScheduledReloads` is called (usually after the reconciliation timeout, 500ms). Dirty units are
 *             flushed automatically when calling various `ask` methods, so that completion and hyperlinking are always
 *             up-to-date
 *    - crashed. A loaded unit that caused the jawa resolver to crash will be in this state. It won't be parsed
 *               nor type-checked anymore. To re-enable it, call `askToDoFirst`, which is usually called when an editor is
 *               open (meaning that when a file was closed and reopen it will be retried).
 *
 *  @note The self-type is necessary, since it changes the way calls to overridden ask methods are dispatched. Without the self-type
 *        they would go to the `CompilerControl` implementation, missing the overrides that call `flushScheduledReloads`
 */
trait IJawaPresentationCompiler extends Global { self: JawaPresentationCompiler =>
  import IJawaPresentationCompiler._

  /** Removes source files and top-level symbols, and issues a new run.
   *
   *  @return A `Response[Unit]` to sync when the operation was completed.
   */
  def askFilesDeleted(sources: IList[SourceFile]): Response[Unit] =
    withResponse[Unit](askFilesDeleted(sources, _))

  /** Return the position of the definition of the given symbol in the given source file.
   *
   *  @param   sym      The symbol referenced by the link (might come from a classfile)
   *  @param   source   The source file that's supposed to contain the definition
   *  @return           A response that will be set to the following:
   *                    If `source` contains a definition that is referenced by the given link
   *                    the position of that definition, otherwise NoPosition.
   *
   *  @note This operation does not automatically load `source`. If `source`
   *  is unloaded, it stays that way.
   */
  def askLinkPos(token: JawaToken): Response[Position] =
    withResponse[Position](askLinkPos(token, _))


  /** If source is not yet loaded, get an outline view with askParsedEntered.
   *  If source is loaded, wait for it to be resolved.
   *  In both cases, set response to parsed tree.
   *
   *  @param keepSrcLoaded If set to `true`, source file will be kept as a loaded unit afterwards.
   *  @param keepLoaded    Whether to keep that file in the PC if it was not loaded before. If
   *                       the file is already loaded, this flag is ignored.
   */
  def askStructure(sourceFile: SourceFile, keepLoaded: Boolean = false): Response[CompilationUnit]

  /** Ask to put scu in the beginning of the list of files to be resolved.
   *
   *  If the file has not been 'reloaded' first, it does nothing. If the file was marked as `crashed`,
   *  this method will add it back to the managed file set, and type-check it from now on.
   */
  def askToDoFirst(scu: InteractiveCompilationUnit): Unit

  /** Asks for a computation to be done quickly on the presentation compiler thread
   *
   *  This operation might interrupt background type-checking and take precedence. It
   *  is important that such operations are fast, or otherwise they will 'starve' any
   *  job waiting for a full type-check.
   */
  def asyncExec[A](op: => A): Response[A]

  /** Ask a fresh type-checking round on all loaded compilation units. */
  def askReloadManagedUnits(): Unit

  /** Start a new type-checking round for changes in loaded compilation units.
   *
   *  Unlike `askReloadManagedUnits`, this one causes a reload of only units that have
   *  changes that were not yet re-type-checked.
   */
  def flushScheduledReloads(): Response[Unit]

  /** Return a list of all loaded compilation units */
  def compilationUnits: IList[InteractiveCompilationUnit]

  /** Add a compilation unit (CU) to the set of CUs to be Reloaded at the next refresh round.
   */
  def scheduleReload(icu: InteractiveCompilationUnit, contents: SourceFile): Unit

  /** Reload the given compilation unit. If the unit is not tracked by the presentation
   *  compiler, it will be from now on.
   */
  def askReload(scu: InteractiveCompilationUnit, content: SourceFile): Response[Unit]

  /** Atomically load a list of units in the current presentation compiler. */
  def askReload(units: List[InteractiveCompilationUnit]): Response[Unit]

  /** Stop compiling the given unit. Usually called when the user
   *  closed an editor.
   */
  def discardCompilationUnit(scu: InteractiveCompilationUnit): Unit

  /** Tell the presentation compiler to refresh the given files,
   *  if they are not managed by the presentation compiler already.
   *
   *  This is usually called when files changed on disk and the associated compiler
   *  symbols need to be refreshed. For example, a git checkout will trigger
   *  such a call.
   */
  def refreshChangedFiles(files: List[IFile]): Unit

  /** Return the compilation errors for the given unit. It will block until the
   *  type-checker finishes (but subsequent calls are fast once the type-checker finished).
   *
   *  @note This method does not trigger a fresh type-checking round on its own. Instead,
   *        it reports compiler errors/warnings from the last type-checking round.
   */
  def problemsOf(scu: InteractiveCompilationUnit): List[JawaCompilationProblem]

  /** Find the definition of given symbol. Returns a compilation unit and an offset in that unit.
   *
   *  @note The offset is relative to the Jawa source file represented by the given unit. This may
   *        be different from the absolute offset in the workspace file of that unit if the unit is
   *        not a Jawa source file.
   */
  def findDeclaration(node: JawaAstNode, javaProject: IJavaProject): Option[(InteractiveCompilationUnit, Int)]

  /** Return the JDT element corresponding to this Jawa symbol. This method is time-consuming
   *  and may trigger building the structure of many Jawa files.
   *
   *  @param sym      The symbol to map to a Java element.
   *  @param projects A number of projects to look for Java elements. It is important
   *                  that you limit the search to the smallest number of projects possible.
   *                  This search is an exhaustive search, and if no projects are specified
   *                  it uses all Java projects in the workspace. Usually, a single projects
   *                  is passed.
   */
  def getJavaElement(token: SJCJawaElement, projects: IJavaProject*): Option[IJavaElement]

  /** Return the compilation unit (Jawa File) in which the symbol passed as an argument is defined.
   *  This method is time-consuming and may trigger building the structure of many Scala files.
   *
   *  @param token      The token to seek a compilation unit for.
   */
  def findCompilationUnit(node: JawaAstNode, javaProject: IJavaProject): Option[InteractiveCompilationUnit]



  /** Create a hyperlink to the given symbol. This is an exit point from the compiler cake.
   *
   *  @note The resulting type does not have any path-dependent types coming from the
   *        compiler instance.
   *
   * @param token         The token definition to which the hyperlink should go
   * @param name        The primary information to be displayed, if more than one hyperlink is available
   * @param region      The region to be underlined in the editor
   * @param javaProject The java project where to search for the definition of this symbol
   * @param label       A way to compute the attached hyperlink label. Normally this can be ignored and use the default label,
   *                    consisting of the symbol kind and full name.
   */
  def mkHyperlink(token: JawaToken, name: String, region: IRegion, javaProject: IJavaProject, label: JawaToken => String = defaultHyperlinkLabel _): Option[IHyperlink]
}

object IJawaPresentationCompiler extends HasLogger {
  /** The maximum time to wait for an `askOption` call to finish. */
  final val AskTimeout: Duration = 10000.millis

  /** Convenience method for creating a Response */
  def withResponse[A](op: Response[A] => Any): Response[A] = {
    val response = new Response[A]
    op(response)
    response
  }
  
  object Implicits {
    implicit class RichResponse[A](val resp: Response[A]) extends AnyVal {

      /** Extract the value from this response, blocking the calling thread.
       *
       *  @param default The default value to be returned in case the underlying Response failed
       *                 or a timeout occurred
       *
       *  Clients should always specify a timeout value when calling this method. In rare cases
       *  a response is never completed (for example, when the presentation compiler restarts).
       *
       *  Failures are logged:
       *   - TypeError and FreshRunReq are printed to stdout, all the others are logged in the platform error log.
       */
      def getOrElse[B >: A](default: => B)(timeout: Duration = AskTimeout): B = {
        getOption(timeout).getOrElse(default)
      }

      /** Extract the value from this response, blocking the calling thread.
       *
       *  Clients should always specify a timeout value when calling this method. In rare cases
       *  a response is never completed (for example, when the presentation compiler restarts).
       *
       *  Failures are logged:
       *   - TypeError and FreshRunReq are printed to stdout, all the others are logged in the platform error log.
       */
      def getOption(timeout: Duration = AskTimeout): Option[A] = {
        val res = resp.get(timeout.toMillis)

        res match {
          case None =>
            eclipseLog.info("Timeout in askOption", new Throwable) // log a throwable for its stacktrace
            None

          case Some(result) =>
            result match {
              case Right(fi: FailedInterrupt) =>
                fi.getCause() match {
                  case f: FreshRunReq          => logger.info("FreshRunReq in ask:\n", f)
                  case m: MissingResponse      => logger.info("MissingResponse in ask. Called from: ", m)
                  // This can happen if you ask long queries of the
                  // PC, triggering long sleep() sessions on caller
                  // side.
                  case i: InterruptedException => logger.debug("InterruptedException in asyncExec", i) // no need to call `interrupt`, `Response` already did it.
                  case e                       => eclipseLog.error("Error during asyncExec (FailedInterrupt)", e)
                }
                None

              case Right(m: MissingResponse) =>
                logger.info("MissingResponse in ask. Called from: ", m)
                None

              case Right(ie: InterruptedException) =>
                logger.error("Ignoring InterruptException")
                Thread.currentThread().interrupt()
                None

              case Right(f: FreshRunReq)          =>
                logger.info("FreshRunReq in ask:\n", f)
                None

              case Right(e: Throwable) =>
                eclipseLog.error("Throwable during asyncExec", e)
                None

              case Left(v) => Some(v)
            }
        }
      }
    }
  }
}
