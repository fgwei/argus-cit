package org.arguside.core.internal.formatter

import org.eclipse.jdt.internal.corext.fix.CodeFormatFix
import org.eclipse.jdt.core.ICompilationUnit
import org.eclipse.jdt.core.refactoring.CompilationUnitChange
import org.eclipse.jdt.ui.cleanup.ICleanUpFix
import org.eclipse.jface.text.Document
import org.eclipse.jface.text.TextUtilities
import org.eclipse.text.edits.{ TextEdit => JFaceTextEdit, _ }
import argus.tools.eclipse.contribution.weaving.jdt.ui.javaeditor.formatter.IFormatterCleanUpProvider
import org.arguside.util.eclipse.EclipseUtils.asEclipseTextEdit
import org.eclipse.jdt.internal.ui.javaeditor.DocumentAdapter

class ScalaFormatterCleanUpProvider extends IFormatterCleanUpProvider {

  def createCleanUp(cu: ICompilationUnit): ICleanUpFix = {
    val document = cu.getBuffer match {
      case adapter: DocumentAdapter => adapter.getDocument
      case _ => new Document(cu.getBuffer.getContents)
    }
    val lineDelimiter = TextUtilities.getDefaultLineDelimiter(document)

    val preferences = FormatterPreferences.getPreferences(cu.getJavaProject)
    val edits =
      try ArgusFormatter.formatAsEdits(cu.getSource, preferences, Some(lineDelimiter))
      catch { case e: ArgusParserException => List() }

    val multiEdit = new MultiTextEdit
    multiEdit.addChildren(edits.map(asEclipseTextEdit).toArray)
    val change = new CompilationUnitChange("Formatting", cu)
    change.setEdit(multiEdit)
    new CodeFormatFix(change)
  }

}
