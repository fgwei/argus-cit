package org.arguside.core.internal.jdt.model

import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants
import org.arguside.logging.HasLogger
import org.eclipse.jdt.core._
import org.eclipse.jdt.internal.core.JavaModelManager
import org.eclipse.core.runtime.Path
import org.arguside.core.compiler.IJawaPresentationCompiler.Implicits._
import org.arguside.core.internal.compiler.InternalCompilerServices
import org.arguside.core.internal.compiler.JawaPresentationCompiler
import org.sireum.jawa.sjc.interactive.{JawaElement => SJCJawaElement}
import org.sireum.jawa.sjc.interactive.JawaClass
import org.sireum.jawa.sjc.interactive.JawaMethod
import org.sireum.jawa.sjc.interactive.JawaField
import org.sireum.jawa.sjc.AccessFlag
import org.sireum.jawa.sjc.JawaType

/** Implementation of a internal compiler services dealing with mapping Jawa types and symbols
 *  to internal JDT counterparts.
 */
trait JawaJavaMapper extends InternalCompilerServices with HasLogger { self: JawaPresentationCompiler =>

  /** Return the Java Element corresponding to the given Jawa Element, looking in the
   *  given project list
   *
   *  If the symbol exists in several projects, it returns one of them.
   */
  def getJavaElement(je: SJCJawaElement, _projects: IJavaProject*): Option[IJavaElement] = {
    assert(je ne null)
    if (je.isUnknown) return None

    val projects: Seq[IJavaProject] = if (_projects.isEmpty) JavaModelManager.getJavaModelManager.getJavaModel.getJavaProjects.toSeq else _projects

    val sjName = je match {
      case c: JawaClass => c.getName
      case m: JawaMethod => m.getName
      case f: JawaField => f.getName
      case _ => return None
    }

    def matchesMethod(meth: IMethod): Boolean = {
      import Signature._
      val sameName = meth.getElementName == sjName
      sameName && {
        if(je.isInstanceOf[JawaMethod]){
          val paramsTpe = je.asInstanceOf[JawaMethod].getParamTypes.map(formatTypeToSignature(_))
          val methParamsTpe = meth.getParameterTypes.map(tp => getTypeErasure(getElementType(tp)))
          methParamsTpe.sameElements(paramsTpe)
        } else false
      }
    }

    je match {
      case c: JawaClass =>
        val fullClassName = c.getName
        val results = projects.map(p => Option(p.findType(fullClassName)))
        results.find(_.isDefined).flatten.headOption
      case m: JawaMethod =>
        getJavaElement(m.getDeclaringClass, projects: _*) match {
          case Some(ownerClass: IType) =>
            ownerClass.getMethods.find(matchesMethod)
          case _ => None
        }
      case f: JawaField =>
        getJavaElement(f.getDeclaringClass, projects: _*) match {
          case Some(ownerClass: IType) =>
            val fieldName = f.name
            ownerClass.getFields.find(_.getElementName == fieldName.toString)
          case _ => None
        }
    }
  }

  override def mapModifiers(je: SJCJawaElement): Int = {
    mapModifiers(je.getAccessFlags)
  }
  
  override def mapModifiers(af: Int): Int = {
    var jdtMods = 0
    if(AccessFlag.isPrivate(af))
      jdtMods = jdtMods | ClassFileConstants.AccPrivate
    else
      jdtMods = jdtMods | ClassFileConstants.AccPublic

    if(AccessFlag.isAbstract(af))
      jdtMods = jdtMods | ClassFileConstants.AccAbstract

    if(AccessFlag.isFinal(af))
      jdtMods = jdtMods | ClassFileConstants.AccFinal

    if(AccessFlag.isInterface(af))
      jdtMods = jdtMods | ClassFileConstants.AccInterface

    jdtMods
  }

  override def javaDescriptor(tpe: JawaType): String =
    formatTypeToSignature(tpe)

  override def enclosingTypeName(je : SJCJawaElement): String =
    if (je.isUnknown) ""
    else {
      je match {
        case c: JawaClass =>
          c.getOuterClass match {
            case Some(o) => o.getName
            case None => ""
          }
        case m: JawaMethod =>
          m.getDeclaringClass.getName
        case f: JawaField =>
          f.getDeclaringClass.getName
      }
    }

  /** Return the enclosing package. Correctly handle the empty package, by returning
   *  the empty string, instead of <empty>.
   */
  override def javaEnclosingPackage(je: SJCJawaElement): String = {
    val enclPackage = je match {
      case c: JawaClass => c.getPackage
      case m: JawaMethod => m.getDeclaringClass.getPackage
      case f: JawaField => f.getDeclaringClass.getPackage
    }
    enclPackage
  }
}
