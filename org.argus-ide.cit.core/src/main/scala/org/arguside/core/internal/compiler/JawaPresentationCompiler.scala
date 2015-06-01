package org.arguside.core.internal.compiler

import scala.collection.concurrent
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.SynchronizedMap
import org.eclipse.jdt.core.compiler.IProblem
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem
import org.eclipse.jdt.internal.compiler.problem.ProblemSeverities
import org.arguside.logging.HasLogger
import org.eclipse.jdt.core.IMethod
import org.arguside.core.internal.jdt.model.JawaSourceFile
import org.arguside.core.extensions.SourceFileProviderRegistry
import org.eclipse.core.runtime.Path
import org.eclipse.core.resources.IFile
import org.eclipse.jdt.internal.core.util.Util
import org.arguside.core.IArgusProject
import org.arguside.core.IArgusPlugin
import scala.util.Try
import org.eclipse.jface.text.IRegion
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jface.text.hyperlink.IHyperlink
import org.eclipse.jface.text.Region
import org.arguside.util.eclipse.RegionUtils
import org.sireum.jawa.sjc.interactive.Global
import org.arguside.core.compiler.IJawaPresentationCompiler
import org.arguside.core.compiler.InteractiveCompilationUnit
import org.sireum.jawa.sjc.util.SourceFile
import org.sireum.jawa.sjc.interactive.Response
import org.arguside.core.resources.EclipseFile
import org.sireum.jawa.sjc.util.Position
import org.sireum.util._
import org.sireum.jawa.sjc.parser.JawaAstNode
import org.sireum.jawa.sjc.io.AbstractFile
import org.arguside.core.compiler.JawaCompilationProblem
import org.sireum.jawa.sjc.util.RangePosition
import org.sireum.jawa.sjc.util.FgSourceFile
import org.sireum.jawa.sjc.interactive.InteractiveReporter
import org.sireum.jawa.sjc.interactive.Problem
import org.sireum.jawa.sjc.io.VirtualFile
import org.arguside.util.JawaWordFinder
import org.arguside.core.resources.EclipseResource
import org.arguside.core.compiler.IJawaPresentationCompiler._
import org.arguside.core.compiler.IJawaPresentationCompiler.Implicits._
import org.sireum.jawa.sjc.util.NoPosition
import org.sireum.jawa.sjc.lexer.{Token => JawaToken}
import org.sireum.jawa.sjc.parser.CompilationUnit
import org.arguside.core.internal.hyperlink.JawaHyperlink
import org.arguside.core.internal.jdt.model.JawaStructureBuilder
import org.arguside.core.internal.jdt.model.JawaJavaMapper
import org.arguside.core.internal.jdt.search.JawaIndexBuilder

class JawaPresentationCompiler(name: String) extends {
  /*
   * Lock object for protecting compiler names. Names are cached in a global `Array[Char]`
   * and concurrent access may lead to overwritten names.
   *
   * @note This field is EARLY because `newTermName` is hit during construction of the superclass `Global`,
   *       and the lock object has to be constructed already.
   */
  private val nameLock = new Object

} with Global(name, new JawaPresentationCompiler.PresentationReporter)
  with JawaStructureBuilder
  with JawaJavaMapper
  with JawaIndexBuilder
  with LocateAST
  with IJawaPresentationCompiler
  with HasLogger { self =>

//  override lazy val analyzer = new {
//    val global: JawaPresentationCompiler.this.type = JawaPresentationCompiler.this
//  }
  
  def presentationReporter = reporter.asInstanceOf[JawaPresentationCompiler.PresentationReporter]
  presentationReporter.compiler = this

  def compilationUnits: IList[InteractiveCompilationUnit] = {
    val files = managedFiles.toList
    for {
      f <- files.collect { case ef: EclipseFile => ef }
      icu <- SourceFileProviderRegistry.getProvider(f.workspacePath).createFrom(f.workspacePath)
      if icu.exists
    } yield icu
  }

  def askReloadManagedUnits() {
    askReload(compilationUnits)
  }

  /**
   * The set of compilation units to be reloaded at the next refresh round.
   * Refresh rounds can be triggered by the reconciler, but also interactive requests
   * (e.g. completion)
   */
  private val scheduledUnits: MMap[InteractiveCompilationUnit, SourceFile] = mmapEmpty
  
  /**
   * Add a compilation unit (CU) to the set of CUs to be Reloaded at the next refresh round.
   */
  def scheduleReload(icu : InteractiveCompilationUnit, srcFile: SourceFile) : Unit = {
    scheduledUnits.synchronized { scheduledUnits += ((icu, srcFile)) }
  }

  /**
   * Reload the scheduled compilation units and reset the set of scheduled reloads.
   *  For any CU unknown by the compiler at reload, this is a no-op.
   */
  def flushScheduledReloads(): Response[Unit] = {
    val res = new Response[Unit]
    scheduledUnits.synchronized {
      val reloadees = scheduledUnits.filter{(scu:(InteractiveCompilationUnit, SourceFile)) => compilationUnits.contains(scu._1)}.toList

      if (reloadees.isEmpty) res.set(())
      else {
        val reloadFiles = reloadees map { case (_, srcFile) => srcFile }
        askReload(reloadFiles, res)
        logger.info(s"Flushed ${reloadFiles.mkString("", ",", "")}")
      }
      scheduledUnits.clear()
    }
    res.get
    res
  }

  override def askFilesDeleted(sources: IList[SourceFile], response: Response[Unit]): Unit = {
    flushScheduledReloads()
    super.askFilesDeleted(sources, response)
  }

  override def askLinkPos(token: JawaToken, response: Response[Position]): Unit = {
    flushScheduledReloads()
    super.askLinkPos(token, response)
  }

  override def askParsedEntered(source: SourceFile, keepLoaded: Boolean, response: Response[CompilationUnit]): Unit = {
    flushScheduledReloads()
    super.askParsedEntered(source, keepLoaded, response)
  }

  override def askToDoFirst(source: SourceFile): Unit = {
    flushScheduledReloads()
    super.askToDoFirst(source)
  }

  override def askStructure(sourceFile: SourceFile, keepLoaded: Boolean): Response[CompilationUnit] = {
    withResponse[CompilationUnit](askStructure(keepLoaded)(sourceFile, _))
  }

  def problemsOf(file: AbstractFile): IList[JawaCompilationProblem] = {
    getCompilationUnit(file) match {
      case Some(unit) =>
        unit.problems.toList flatMap presentationReporter.eclipseProblem
      case None =>
        logger.info("Missing unit for file %s when retrieving errors. Errors will not be shown in this file".format(file))
        logger.info(getCompilationUnits.toString)
        Nil
    }
  }

  def problemsOf(scu: InteractiveCompilationUnit): IList[JawaCompilationProblem] = problemsOf(scu.file)


  /** Perform `op` on the compiler thread. This method returns a `Response` that may
   *  never complete (there is no default timeout). In very rare cases, the current presentation compiler
   *  might restart and miss to complete a pending request. Clients should always specify
   *  a timeout value when awaiting on a future returned by this method.
   */
  def asyncExec[A](op: => A): Response[A] = {
    askForResponse(() => op)
  }

  /** Ask to put scu in the beginning of the list of files to be resolved.
   *
   *  If the file has not been 'reloaded' first, it does nothing.
   */
  def askToDoFirst(scu: InteractiveCompilationUnit) {
    askToDoFirst(scu.sourceFile)
  }

  /** Reload the given compilation unit. If the unit is not tracked by the presentation
   *  compiler, it will be from now on.
   */
  def askReload(scu: InteractiveCompilationUnit, source: SourceFile): Response[Unit] = {
    withResponse[Unit] { res => askReload(List(source), res) }
  }

  /** Atomically load a list of units in the current presentation compiler. */
  def askReload(units: List[InteractiveCompilationUnit]): Response[Unit] = {
    withResponse[Unit] { res => askReload(units.map(_.sourceFile), res) }
  }

  def filesDeleted(units: Seq[InteractiveCompilationUnit]) {
    logger.info("files deleted:\n" + (units map (_.file.path) mkString "\n"))
    if (!units.isEmpty)
      askFilesDeleted(units.map(_.sourceFile).toList)
  }

  def discardCompilationUnit(scu: InteractiveCompilationUnit): Unit = {
    logger.info("discarding " + scu.file.path)
    asyncExec { removeUnitOf(scu.sourceFile) }.getOption()
  }

  /** Tell the presentation compiler to refresh the given files,
   *  if they are not managed by the presentation compiler already.
   */
  def refreshChangedFiles(files: IList[IFile]) {
    // transform to batch source files
    val freshSources: IList[SourceFile] = files.collect {
      // When a compilation unit is moved (e.g. using the Move refactoring) between packages,
      // an ElementChangedEvent is fired but with the old IFile name. Ignoring the file does
      // not seem to cause any bad effects later on, so we simply ignore these files -- Mirko
      // using an Util class from jdt.internal to read the file, Eclipse doesn't seem to
      // provide an API way to do it -- Luc
      case file if file.exists => new FgSourceFile(EclipseResource(file), Util.getResourceContentsAsCharArray(file))
    }

    // only the files not already managed should be refreshed
    val managedFs = managedFiles
    val notLoadedFiles = freshSources.filter(f => !managedFs(f.file))

    notLoadedFiles.foreach(file => {
      // call askParsedEntered to force the refresh without loading the file
      val r = withResponse[CompilationUnit] { askParsedEntered(file, false, _) }

      r.get
    })

    // reconcile the opened editors if some files have been refreshed
    if (notLoadedFiles.nonEmpty)
      askReloadManagedUnits()
  }

//  override def synchronizeNames = true

  override def logError(msg: String, t: Throwable) =
    eclipseLog.error(msg, t)

  def destroy() {
    logger.info("shutting down presentation compiler on project: " + name)
    askShutdown()
  }

  /** Add a new completion proposal to the buffer. Skip constructors and accessors.
   *
   *  Computes a very basic relevance metric based on where the symbol comes from
   *  (in decreasing order of relevance):
   *    - members defined by the owner
   *    - inherited members
   *    - members added by views
   *    - packages
   *    - members coming from Any/AnyRef/Object
   *
   *  TODO We should have a more refined strategy based on the context (inside an import, case
   *       pattern, 'new' call, etc.)
   */
//  def mkCompletionProposal(prefix: String, start: Int, sym: Symbol, tpe: Type,
//    inherited: Boolean, viaView: Symbol, context: CompletionContext.ContextType, project: IScalaProject): CompletionProposal = {
//
//    /** Some strings need to be enclosed in back-ticks to be usable as identifiers in scala
//     *  source. This function adds the back-ticks to a given identifier, if necessary.
//     */
//    def addBackTicksIfNecessary(identifier: String): String = {
//      def needsBackTicks(identifier: String) = {
//        import scalariform.lexer.Tokens._
//
//        try {
//          val tokens = ScalaLexer.tokenise(identifier)  // The last token is always EOF
//          tokens.size match {
//            case 1 => true    // whitespace
//            case 2 => !(IDS contains tokens.head.tokenType)
//            case more => true
//          }
//        } catch {case _: ScalaLexerException => true  /* Illegal chars encountered */}
//      }
//
//      if(needsBackTicks(identifier)) s"`$identifier`" else identifier
//    }
//
//    import org.scalaide.core.completion.MemberKind._
//
//    val kind = if (sym.isSourceMethod && !sym.hasFlag(Flags.ACCESSOR | Flags.PARAMACCESSOR)) Def
//    else if (sym.hasPackageFlag) Package
//    else if (sym.isClass) Class
//    else if (sym.isTrait) Trait
//    else if (sym.isPackageObject) PackageObject
//    else if (sym.isModule) Object
//    else if (sym.isType) Type
//    else Val
//
//    val name = if (sym.isConstructor)
//      sym.owner.decodedName
//    else if (sym.hasGetter)
//      (sym.getter: Symbol).decodedName
//    else sym.decodedName
//
//    val signature =
//      if (sym.isMethod) {
//        declPrinter.defString(sym, flagMask = 0L, showKind = false)(tpe)
//      } else name
//    val container = sym.owner.enclClass.fullName
//
//    // rudimentary relevance, place own members before inherited ones, and before view-provided ones
//    var relevance = 100
//    if (!sym.isLocalToBlock) relevance -= 10 // non-local symbols are less relevant than local ones
//    if (!sym.hasGetter) relevance -= 5 // fields are more relevant than non-fields
//    if (inherited) relevance -= 10
//    if (viaView != NoSymbol) relevance -= 20
//    if (sym.hasPackageFlag) relevance -= 30
//    // theoretically we'd need an 'ask' around this code, but given that
//    // Any and AnyRef are definitely loaded, we call directly to definitions.
//    if (sym.owner == definitions.AnyClass
//      || sym.owner == definitions.AnyRefClass
//      || sym.owner == definitions.ObjectClass) {
//      relevance -= 40
//    }
//
//    // global symbols are less relevant than local symbols
//    sym.owner.enclosingPackage.fullName match {
//      case "java"  => relevance -= 15
//      case "scala" => relevance -= 10
//      case _ =>
//    }
//
//    val casePenalty = if (name.substring(0, prefix.length) != prefix) 50 else 0
//    relevance -= casePenalty
//
//    val namesAndTypes = for {
//      section <- tpe.paramss
//      if section.isEmpty || !section.head.isImplicit
//    } yield for (param <- section) yield (param.name.toString, param.tpe.toString)
//
//    val (scalaParamNames, paramTypes) = namesAndTypes.map(_.unzip).unzip
//
//    // we save this value to make sure it's evaluated in the PC thread
//    // the closure below can be evaluated in any thread
//    val isJavaMethod = sym.isJavaDefined && sym.isMethod
//    val getParamNames = () => {
//      if (isJavaMethod) {
//        getJavaElement(sym, project.javaProject) collect {
//          case method: IMethod => List(method.getParameterNames.toList)
//        } getOrElse scalaParamNames
//      } else scalaParamNames
//    }
//    val docFun = () => {
//      val comment = parsedDocComment(sym, sym.enclClass, project.javaProject)
//      val header = headerForSymbol(sym, tpe)
//      if (comment.isDefined) (new ScalaDocHtmlProducer).getBrowserInput(this)(comment.get, sym, header.getOrElse("")) else None
//    }
//
//    CompletionProposal(
//      kind,
//      context,
//      start,
//      addBackTicksIfNecessary(name),
//      signature,
//      container,
//      relevance,
//      sym.isJavaDefined,
//      getParamNames,
//      paramTypes,
//      sym.fullName,
//      false,
//      docFun)
//  }

  def mkHyperlink(node: JawaAstNode, name: String, region: IRegion, javaProject: IJavaProject, label: JawaAstNode => String = defaultHyperlinkLabel _): Option[IHyperlink] = {
    import org.arguside.util.eclipse.RegionUtils._

    asyncExec {
      findDeclaration(node, javaProject) map {
        case (f, pos) =>
          val nodeLen = node.toCode.length()
          val targetRegion = (new Region(pos, nodeLen)).map(f.jawaPos)
          new JawaHyperlink(openableOrUnit = f,
              region = targetRegion,
              label = label(node),
              text = name,
              wordRegion = region)
      }
      None
    }.getOrElse(None)()
  }

  private [core] def defaultHyperlinkLabel(node: JawaAstNode): String = s"${node.toCode}"

  override def inform(msg: String): Unit =
    logger.debug("[%s]: %s".format(name, msg))
}

object JawaPresentationCompiler {
  case class InvalidThread(msg: String) extends RuntimeException(msg)

  class PresentationReporter extends InteractiveReporter {
    var compiler: JawaPresentationCompiler = null

    def citSeverityToEclipse(severityLevel: Int) =
      severityLevel match {
        case ERROR.id   => ProblemSeverities.Error
        case WARNING.id => ProblemSeverities.Warning
        case INFO.id    => ProblemSeverities.Ignore
      }

    def eclipseProblem(prob: Problem): Option[JawaCompilationProblem] = {
      import prob._
      if (pos.isDefined) {
        val source = pos.source
        val reducedPos: Option[Position]=
          if (pos.isRange)
            Some(toSingleLine(pos))
          else{
            val wordPos = Try(JawaWordFinder.findWord(source.content, pos.start).getLength).toOption
            wordPos map ((p) => new RangePosition(pos.source, pos.point, p, pos.line, pos.column))
          }

        reducedPos flatMap { reducedPos =>
          val fileName =
            source.file match {
              case EclipseFile(file) =>
                Some(file.getFullPath().toString)
              case vf: VirtualFile =>
                Some(vf.path)
              case _ =>
                None
            }
          fileName.map(JawaCompilationProblem(
            _,
            citSeverityToEclipse(severityLevel),
            formatMessage(msg),
            reducedPos.start,
            scala.math.max(reducedPos.start, reducedPos.end - 1),
            reducedPos.line,
            reducedPos.column))
        }
      } else None
    }

    /** Original code from Position.toSingleLine, copied since it was removed from 2.11 */
    def toSingleLine(pos: Position): Position = pos.source match {
      case bs: FgSourceFile if pos.end > 0 && bs.offsetToLine(pos.start) < bs.offsetToLine(pos.end - 1) =>
        val pointLine = bs.offsetToLine(pos.point)
        new RangePosition(pos.source, pos.start, bs.lineToOffset(pointLine + 1) - pos.start + 1, pos.line, pos.column)
      case _ => pos
    }

    def formatMessage(msg: String) = msg.map {
      case '\n' | '\r' => ' '
      case c           => c
    }
  }
}
