package org.arguside.core.internal.builder

import java.io.InputStream
import org.eclipse.core.internal.resources.ResourceException
import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IMarker
import org.eclipse.core.resources.IResource
import org.eclipse.jdt.core.IJavaModelMarker
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.compiler.IProblem
import org.eclipse.jdt.internal.core.builder.JavaBuilder
import org.arguside.core.IArgusProject
import org.arguside.core.CitConstants
import org.arguside.core.internal.jdt.model.JawaSourceFile
import org.arguside.core.resources.EclipseResource
import org.sireum.jawa.sjc.util.Position
import org.sireum.jawa.sjc.util.SourceFile
import org.sireum.jawa.sjc.lexer.JawaLexer
import org.sireum.jawa.sjc.DefaultReporter

object TaskManager {

  private case class Comment(msg: String, pos: Position)

  /**
   * Removes all task markers from this file.
   */
  def clearTasks(file: IFile) = try {
    file.deleteMarkers(CitConstants.TaskMarkerId, true, IResource.DEPTH_INFINITE)
  } catch {
    case _: ResourceException => // Ignore
  }

  /**
   * Updates all tasks (`TODO`s and `FIXME`s) for given project in given files.
   */
  def updateTasks(project: IArgusProject, files: Set[IFile]): Unit = {
    val taskScanner = new TaskScanner(project)

    for {
      iFile <- files
      jawaFile <- JawaSourceFile.createFromPath(iFile.getFullPath.toOSString)
      sourceFile = jawaFile.lastSourceMap().sourceFile
      Comment(msg, pos) <- extractComments(sourceFile, iFile.getContents, iFile.getCharset)
      if pos.isDefined
      task <- taskScanner.extractTasks(msg, pos)
      if task.pos.isDefined
    } task.pos.source.file match {
      case EclipseResource(file: IFile) => registerTask(file, task)
      case _ => // ignore
    }
  }

  private def extractComments(sourceFile: SourceFile, contentStream: InputStream, charset: String): Seq[Comment] = {
    val contents = try {
      scala.io.Source.fromInputStream(contentStream)(charset).mkString
    } finally contentStream.close()

    for {
      token <- JawaLexer.rawTokenise(Left(contents), new DefaultReporter)
      if (token.tokenType.isComment)
    } yield {
      val position = Position.range(sourceFile, token.offset, token.lastCharacterOffset - token.offset + 1)
      Comment(token.text, position)
    }
  }

  private def registerTask(file: IFile, task: TaskScanner.Task) = {
    val marker = file.createMarker(CitConstants.TaskMarkerId)

    val prioNum = task.priority match {
      case JavaCore.COMPILER_TASK_PRIORITY_HIGH => IMarker.PRIORITY_HIGH
      case JavaCore.COMPILER_TASK_PRIORITY_LOW => IMarker.PRIORITY_LOW
      case _ => IMarker.PRIORITY_NORMAL
    }

    val attributes = Seq(
      IMarker.MESSAGE -> s"${task.tag} ${task.msg}",
      IMarker.PRIORITY -> Integer.valueOf(prioNum),
      IJavaModelMarker.ID -> Integer.valueOf(IProblem.Task),
      IMarker.CHAR_START -> Integer.valueOf(task.pos.start),
      IMarker.CHAR_END -> Integer.valueOf(task.pos.end + 1),
      IMarker.LINE_NUMBER -> Integer.valueOf(task.pos.line),
      IMarker.USER_EDITABLE -> java.lang.Boolean.valueOf(false),
      IMarker.SOURCE_ID -> JavaBuilder.SOURCE_ID)

    attributes.foreach {
      case (key, value) => marker.setAttribute(key, value)
    }
  }
}
