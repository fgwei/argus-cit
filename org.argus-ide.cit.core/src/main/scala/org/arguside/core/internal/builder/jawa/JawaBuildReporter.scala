package org.arguside.core.internal.builder.jawa

import org.arguside.core.resources.EclipseResource
import org.eclipse.core.resources.IMarker
import org.arguside.util.eclipse.FileUtils
import org.eclipse.core.runtime.Path
import org.arguside.core.IArgusProject
import org.arguside.core.internal.builder.BuildProblemMarker
import org.arguside.logging.HasLogger
import org.arguside.core.resources.MarkerFactory
import org.sireum.jawa.sjc.Reporter
import org.sireum.jawa.sjc.util.Position
import org.sireum.jawa.sjc.log.Problem
import org.sireum.util._
import org.sireum.jawa.sjc.lexer.Chars
import org.sireum.jawa.sjc.ReporterImpl
import org.sireum.jawa.sjc.log.Severity

private case class JawaProblem(severity: Severity.Value, message: String, position: Position, category: String) extends Problem {
  override def equals(other: Any): Boolean = other match {
    case otherProblem: Problem =>
      (message == otherProblem.message
        && severity == otherProblem.severity
        && position.line == otherProblem.position.line
        && position.source == otherProblem.position.source)
    case _ => false
  }

  /** Simple hashcode to satisfy the equality implementation above */
  override def hashCode: Int =
    message.hashCode + severity.hashCode
}

/** An Jawa Reporter that creates error markers as build errors are reported.
 *
 *  @note It removes duplicate errors.
 */
//private[jawa] class JawaBuildReporter(project: IArgusProject) extends ReporterImpl with HasLogger {
//  private val probs = new MList[Problem]
//  private var seenErrors = false
//  private var seenWarnings = false
//
//  override def reset() = {
//    seenErrors = false
//    seenWarnings = false
//    probs.clear()
//  }
//
//  override def hasErrors(): Boolean = seenErrors
//  override def hasWarnings(): Boolean = seenWarnings
////  override def printSummary(): Unit = {} //TODO
////  override def problems: Array[Problem] = probs.toArray
//  override def comment(pos: Position, msg: String): Unit = {}
//
//  override def info0(pos: Position, msg: String, sev: Severity) {
//    val problem = JawaProblem(sev, msg, pos, "compile")
//    if (!probs.contains(problem)) {
//      createMarker(pos, msg, sev)
//      probs += problem
//    }
//
//    import Severity._
//    sev match {
//      case Warn  => seenWarnings = true
//      case Error => seenErrors = true
//      case _     =>
//    }
//  }
//
//  def eclipseSeverity(severity: Severity.Value): Int = severity match {
//    case Severity.Info  => IMarker.SEVERITY_INFO
//    case Severity.Error => IMarker.SEVERITY_ERROR
//    case Severity.Warn  => IMarker.SEVERITY_WARNING
//  }
//
//  def createMarker(pos: Position, msg: String, sev: Severity.Value) = {
//    val severity = eclipseSeverity(sev)
//
//    val marker: Option[Unit] = for {
//      resource <- FileUtils.resourceForPath(new Path(pos.source.file.file.getAbsolutePath), project.underlying.getFullPath)
//    } yield if (resource.getFileExtension != "java") {
//      val markerPos = MarkerFactory.RegionPosition(pos.start, identifierLength(pos.lineContent, pos.point), pos.line)
//      BuildProblemMarker.create(resource, severity, msg, markerPos)
//    } else
//      logger.error(s"suppressed error in Java file ${resource.getFullPath}:$pos.line: $msg")
//
//    // if we couldn't determine what file/offset to put this marker on, create one on the project
//    if (!marker.isDefined) {
//      BuildProblemMarker.create(project.underlying, severity, msg)
//    }
//  }
//
//  /** Return the identifier starting at `start` inside `content`. */
//  private def identifierLength(content: String, start: Integer): Int = {
//    def isOK(c: Char) = Chars.isIdentifierPart(c, false) || Chars.isOperatorPart(c)
//    (content drop start takeWhile isOK).size
//  }
//}
