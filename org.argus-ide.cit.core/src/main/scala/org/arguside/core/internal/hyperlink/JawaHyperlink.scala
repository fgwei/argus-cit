package org.arguside.core.internal.hyperlink

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility
import org.eclipse.jdt.internal.core.Openable
import org.eclipse.ui.texteditor.ITextEditor
import org.eclipse.jface.text.hyperlink.IHyperlink
import org.sireum.jawa.sjc.parser.ClassSym
import org.eclipse.core.resources.IProject
import org.eclipse.jdt.ui.JavaUI
import org.eclipse.core.runtime.Path
import org.eclipse.jdt.core.IJavaElement
import org.sireum.jawa.io.Position
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.jface.text.IRegion

/** An implementation of `IHyperlink` for Jawa editors.
 *
 */
class JawaHyperlink(elementOrPos: Either[IJavaElement, Position], label: String, text: String, wordRegion: IRegion) extends IHyperlink {

  override def getHyperlinkRegion = wordRegion

  override def getTypeLabel = label

  override def getHyperlinkText = text
  
  override def open() = {
    elementOrPos match {
      case Left(je) => JavaUI.openInEditor(je)
      case Right(pos) =>
        val path = new Path(pos.source.path)
        val root = ResourcesPlugin.getWorkspace().getRoot()
        val part = root.findFilesForLocationURI(path.toFile.toURI) match {
          case Array(f) => EditorUtility.openInEditor(f, true)
          case _ =>
        }
        part match {
          case editor: ITextEditor => editor.selectAndReveal(pos.start, pos.end - pos.start + 1)
          case _ =>
        }
    }
  }
}