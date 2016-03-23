package org.arguside.core.internal.compiler

import org.eclipse.core.resources.IProject
import com.android.ide.eclipse.adt.AdtUtils
import org.sireum.jawa.sjc.parser._
import org.eclipse.jdt.core.IJavaProject
import org.arguside.util.internal.JavaElementFinder
import org.arguside.core.compiler.InteractiveCompilationUnit
import org.sireum.jawa.io.Position
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.core.runtime.IPath
import org.eclipse.core.runtime.Path
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.jdt.core.WorkingCopyOwner
import org.eclipse.jdt.internal.core.SearchableEnvironment
import org.eclipse.jdt.internal.core.JavaProject
import org.arguside.core.internal.jdt.model.JawaClassFile
import org.arguside.core.extensions.SourceFileProviderRegistry
import org.arguside.core.compiler.IJawaPresentationCompiler.Implicits._
import org.sireum.jawa.JawaType

trait LocateAST { self: JawaPresentationCompiler =>

  def findCompilationUnit(sym: JawaSymbol, javaProject: IJavaProject): Option[InteractiveCompilationUnit] = {

    /**
     * need to rethink later
     */
    def astClassType(node: JawaAstNode): Option[JawaType] = asyncExec {
      node.enclosingTopLevelClass.typ
    }.getOption()

    def findClassFile(): Option[InteractiveCompilationUnit] = {
      logger.debug("Looking for a classfile for " + sym.toCode)

      val typOpt = astClassType(sym)
      typOpt.flatMap { typ =>
        val pfs = new SearchableEnvironment(javaProject.asInstanceOf[JavaProject], null: WorkingCopyOwner).nameLookup.findPackageFragments(typ.getPackageName, false)
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
      logger.info("Looking for a compilation unit for " + sym.toCode)
      val nameLookup = new SearchableEnvironment(javaProject.asInstanceOf[JavaProject], null: WorkingCopyOwner).nameLookup

      val name = astClassType(sym)
      logger.debug("Looking for compilation unit " + name)
      name.flatMap { n =>
        Option(nameLookup.findCompilationUnit(n.name)) map (_.getResource().getFullPath())
      }
    }

    def findSourceFile(): Option[IPath] =
      if (sym.firstToken.file ne null) {
        val path = new Path(sym.firstToken.file.path)
        val root = ResourcesPlugin.getWorkspace().getRoot()
        root.findFilesForLocationURI(path.toFile.toURI) match {
          case Array(f) => Some(f.getFullPath)
          case _ => findPath()
        }
      } else
        findPath()

    findSourceFile.fold(findClassFile()) { f =>
      Option(SourceFileProviderRegistry.getProvider(f)).flatMap(_.createFrom(f))
    }
  }

  private def getProject: IProject = {
    val file = AdtUtils.getActiveFile()
    if (file != null) {
      return file.getProject()
    }
    return null
  }
  
  def findDeclaration(sym: JawaSymbol): Either[IJavaElement, Position] = {
    val project = getProject
    sym match {
      case cs: TypeSymbol => 
        if(project != null) {
          JavaElementFinder.findJavaClass(project, cs.typ) match {
            case Some(c) => Left(c)
            case None => Right(cs.pos)
          }
        } else Right(cs.pos)
      case ms: MethodNameSymbol =>
        if(project != null) {
          JavaElementFinder.findJavaMethod(project, ms.signature) match {
            case Some(m) => Left(m)
            case None => Right(ms.pos)
          }
        } else Right(ms.pos)
      case ss: SignatureSymbol =>
        if(project != null) {
          JavaElementFinder.findJavaMethod(project, ss.signature) match {
            case Some(m) => Left(m)
            case None => Right(ss.pos)
          }
        } else Right(ss.pos)
      case fs: FieldNameSymbol =>
        if(project != null) {
          JavaElementFinder.findJavaField(project, fs.FQN) match {
            case Some(f) => Left(f)
            case None => Right(fs.pos)
          }
        } else Right(fs.pos)
      case vs: VarSymbol =>
        val vdsopt = vs.owner.getAllChildren.find{
          ast =>
            ast match {
              case vd: VarDefSymbol =>
                vd.varName == vs.varName
              case _ => false
            }
        }
        vdsopt match {
          case Some(vds) => Right(vds.pos)
          case None => Right(vs.pos)
        }
      case ls: LocationSymbol =>
        val ldsopt = ls.owner.getAllChildren.find{
          ast =>
            ast match {
              case lds: LocationDefSymbol =>
                lds.location == ls.location
              case _ => false
            }
        }
        ldsopt match {
          case Some(lds) => Right(lds.pos)
          case None => Right(ls.pos)
        }
      case ds: DefSymbol =>
        Right(ds.pos)
      case _ =>
        Right(sym.pos)
    }
  }
}
