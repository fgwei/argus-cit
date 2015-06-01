package org.arguside.core.internal.compiler

import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.Path
import org.eclipse.jdt.core.ICodeAssist
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.core.WorkingCopyOwner
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.ITextViewer
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector
import org.eclipse.jface.text.hyperlink.IHyperlink
import org.eclipse.jdt.internal.core.ClassFile
import org.eclipse.jdt.internal.core.Openable
import org.eclipse.jdt.internal.core.SearchableEnvironment
import org.eclipse.jdt.internal.core.JavaProject
import org.eclipse.ui.texteditor.ITextEditor
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit
import org.eclipse.jdt.ui.actions.SelectionDispatchAction
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility
import org.eclipse.jdt.internal.ui.javaeditor.JavaElementHyperlink
import org.eclipse.core.runtime.IPath
import org.eclipse.jdt.core.IJavaProject
import org.arguside.core.compiler.InteractiveCompilationUnit
import org.sireum.jawa.sjc.lexer.{Token => JawaToken}
import org.sireum.jawa.sjc.ObjectType
import org.arguside.core.compiler.IJawaPresentationCompiler.Implicits._
import org.sireum.jawa.sjc.util.NoPosition
import org.arguside.core.extensions.SourceFileProviderRegistry
import org.arguside.core.internal.jdt.model.JawaClassFile
import org.sireum.jawa.sjc.parser.JawaAstNode

trait LocateAST { self: JawaPresentationCompiler =>

  def findCompilationUnit(node: JawaAstNode, javaProject: IJavaProject): Option[InteractiveCompilationUnit] = {

    /**
     * need to rethink later
     */
    def tokenClassType(node: JawaAstNode): Option[ObjectType] = asyncExec {
      node.enclosingTopLevelClass
    }.getOption()

    def findClassFile(): Option[InteractiveCompilationUnit] = {
      logger.debug("Looking for a classfile for " + node.toCode)

      val typOpt = tokenClassType(node)
      typOpt.flatMap { typ =>
        val pfs = new SearchableEnvironment(javaProject.asInstanceOf[JavaProject], null: WorkingCopyOwner).nameLookup.findPackageFragments(typ.pkg, false)
        if (pfs eq null) None else pfs.toStream map
          { pf => logger.debug("Trying out to get " + typ); pf.getClassFile(typ.name) } collectFirst
          {
            case classFile: JawaClassFile =>
              logger.debug("Found class file: " + classFile.getElementName)
              classFile
          }
      }
    }

    def findPath(): Option[IPath] = {
      logger.info("Looking for a compilation unit for " + node.toCode)
      val nameLookup = new SearchableEnvironment(javaProject.asInstanceOf[JavaProject], null: WorkingCopyOwner).nameLookup

      val name = tokenClassType(node)
      logger.debug("Looking for compilation unit " + name)
      name.flatMap { n =>
        Option(nameLookup.findCompilationUnit(n.name)) map (_.getResource().getFullPath())
      }
    }

    def findSourceFile(): Option[IPath] =
      if (node.firstToken.file ne null) {
        val path = new Path(node.firstToken.file.path)
        val root = ResourcesPlugin.getWorkspace().getRoot()
        root.findFilesForLocationURI(path.toFile.toURI) match {
          case Array(f) => Some(f.getFullPath)
          case _ => findPath()
        }
      } else
        findPath()

    findSourceFile.map { f =>
      SourceFileProviderRegistry.getProvider(f).createFrom(f)
    }.get
  }

  def findDeclaration(node: JawaAstNode, javaProject:IJavaProject): Option[(InteractiveCompilationUnit, Int)] =
    findCompilationUnit(node, javaProject) flatMap { cunit =>
//      val pos = if (token.pos eq NoPosition) {
//        cunit.withSourceFile { (f, _) =>
//          val pos = askLinkPos(token)
//          pos.get.left.toOption
//        }.flatten
//      } else Some(token.pos)
//
//      pos flatMap { p =>
//        if (p eq NoPosition) None
//        else Some((cunit, p.point))
//      }
      None
    }
}
