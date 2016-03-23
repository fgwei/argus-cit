package org.arguside.core.internal.jdt.model

import scala.collection.immutable.Seq
import scala.reflect.NameTransformer
import org.eclipse.jdt.core.IField
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.core.IMember
import org.eclipse.jdt.core.IMethod
import org.eclipse.jdt.core.IType
import org.eclipse.jdt.core.ITypeParameter
import org.eclipse.jdt.core.Flags
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants
import org.eclipse.jdt.internal.core.BinaryType
import org.eclipse.jdt.internal.core.JavaElement
import org.eclipse.jdt.internal.core.JavaElementInfo
import org.eclipse.jdt.internal.core.LocalVariable
import org.eclipse.jdt.internal.core.SourceConstructorInfo
import org.eclipse.jdt.internal.core.SourceField
import org.eclipse.jdt.internal.core.SourceFieldElementInfo
import org.eclipse.jdt.internal.core.SourceMethod
import org.eclipse.jdt.internal.core.SourceMethodElementInfo
import org.eclipse.jdt.internal.core.SourceMethodInfo
import org.eclipse.jdt.internal.core.SourceType
import org.eclipse.jdt.internal.core.SourceTypeElementInfo
import org.eclipse.jdt.internal.core.OpenableElementInfo
import org.eclipse.jdt.internal.core.TypeParameterElementInfo
import org.eclipse.jdt.internal.ui.JavaPlugin
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider
import org.eclipse.jdt.ui.JavaElementImageDescriptor
import org.eclipse.jface.resource.ImageDescriptor
import org.eclipse.swt.graphics.Image
import org.arguside.ui.ArgusImages
import argus.tools.eclipse.contribution.weaving.jdt.IJawaElement
import argus.tools.eclipse.contribution.weaving.jdt.ui.IMethodOverrideInfo
import org.arguside.util.internal.ReflectionUtils
import org.sireum.jawa.JawaType

trait JawaElement extends JavaElement with IJawaElement {
  def getElementInfo: AnyRef
  def getElementName: String
  def jawaName: String = getElementName
  def labelName: String = jawaName
  def getLabelText(flags: Long): String = labelName
  def getImageDescriptor: ImageDescriptor = null
  def isVisible = true

  override def getCompilationUnit() = {
    val cu = super.getCompilationUnit()
    if (cu != null) cu else new CompilationUnitAdapter(getClassFile().asInstanceOf[JawaClassFile])
  }

  override def getAncestor(ancestorType: Int): IJavaElement = {
    val ancestor = super.getAncestor(ancestorType)
    if (ancestor != null)
      ancestor
    else if(ancestorType == IJavaElement.COMPILATION_UNIT)
      new CompilationUnitAdapter(getClassFile().asInstanceOf[JawaClassFile])
    else
      null
  }
}

class JawaSourceTypeElement(parent: JavaElement, name: String)
  extends SourceType(parent, name) with JawaElement {

  def getCorrespondingElement(element: IJavaElement): Option[IJavaElement] = {
    val name = element.getElementName
    val tpe = element.getElementType
    getChildren.find(e => e.getElementName == name && e.getElementType == tpe)
  }

  override def getType(typeName: String): IType = {
    val tpe = super.getType(typeName)
    getCorrespondingElement(tpe).getOrElse(tpe).asInstanceOf[IType]
  }

  override def getField(fieldName: String): IField = {
    val field = super.getField(fieldName)
    getCorrespondingElement(field).getOrElse(field).asInstanceOf[IField]
  }

  override def getMethod(selector: String, parameterTypeSignatures: Array[String]): IMethod = {
    val method = super.getMethod(selector, parameterTypeSignatures)
    getCorrespondingElement(method).getOrElse(method).asInstanceOf[IMethod]
  }
}

class JawaClassElement(parent: JavaElement, typ: JawaType)
  extends JawaSourceTypeElement(parent, typ.name.substring(typ.name.lastIndexOf(".") + 1)) {
  override def getImageDescriptor = ArgusImages.JAWA_CLASS
}

class JawaInterfaceElement(parent: JavaElement, typ: JawaType)
  extends JawaSourceTypeElement(parent, typ.name.substring(typ.name.lastIndexOf(".") + 1)) {
  override def getImageDescriptor = ArgusImages.JAWA_INTERFACE
}

class JawaFieldElement(parent: JavaElement, name: String, display: String)
  extends SourceField(parent, name) with JawaElement {
  override def getLabelText(flags: Long) = display
}

class JawaMethodElement(parent : JavaElement, name: String, paramTypes : Array[String], synthetic : Boolean, display : String, overrideInfo : Int)
  extends SourceMethod(parent, name, paramTypes) with JawaElement with IMethodOverrideInfo {
  override def getLabelText(flags : Long) = display
  def getOverrideInfo = overrideInfo
}

class JawaLocalVariableElement(
  parent: JavaElement, name: String,
  declarationSourceStart: Int, declarationSourceEnd: Int, nameStart: Int, nameEnd: Int,
  typeSignature: String, display: String, jdtFlags: Int, methodParameter: Boolean ) extends LocalVariable(
  parent, name, declarationSourceStart, declarationSourceEnd, nameStart, nameEnd, typeSignature, null, jdtFlags, methodParameter) with
  JawaElement {
  override def getLabelText(flags: Long) = display
}

object JawaMemberElementInfo extends ReflectionUtils {
  val jeiClazz = Class.forName("org.eclipse.jdt.internal.core.JavaElementInfo")
  val meiClazz = Class.forName("org.eclipse.jdt.internal.core.MemberElementInfo")
  val aiClazz = Class.forName("org.eclipse.jdt.internal.core.AnnotatableInfo")
  val sreiClazz = Class.forName("org.eclipse.jdt.internal.core.SourceRefElementInfo")
  val setFlagsMethod = getDeclaredMethod(meiClazz, "setFlags", classOf[Int])
  val getNameSourceStartMethod = try {
    getDeclaredMethod(meiClazz, "getNameSourceStart")
  } catch {
    case _: NoSuchMethodException => getDeclaredMethod(aiClazz, "getNameSourceStart")
  }
  val getNameSourceEndMethod = try {
    getDeclaredMethod(meiClazz, "getNameSourceEnd")
  } catch {
    case _: NoSuchMethodException => getDeclaredMethod(aiClazz, "getNameSourceEnd")
  }
  val setNameSourceStartMethod = try {
    getDeclaredMethod(meiClazz, "setNameSourceStart", classOf[Int])
  } catch {
    case _: NoSuchMethodException => getDeclaredMethod(aiClazz, "setNameSourceStart", classOf[Int])
  }
  val setNameSourceEndMethod = try {
    getDeclaredMethod(meiClazz, "setNameSourceEnd", classOf[Int])
  } catch {
    case _: NoSuchMethodException => getDeclaredMethod(aiClazz, "setNameSourceEnd", classOf[Int])
  }
  val setSourceRangeStartMethod = getDeclaredMethod(sreiClazz, "setSourceRangeStart", classOf[Int])
  val setSourceRangeEndMethod = getDeclaredMethod(sreiClazz, "setSourceRangeEnd", classOf[Int])
  val getDeclarationSourceStartMethod = getDeclaredMethod(sreiClazz, "getDeclarationSourceStart")
  val getDeclarationSourceEndMethod = getDeclaredMethod(sreiClazz, "getDeclarationSourceEnd")
  val hasChildrenField = try {
    getDeclaredField(jeiClazz, "children")
    true
  } catch {
    case _: NoSuchFieldException => false
  }
  val addChildMethod = if (hasChildrenField) getDeclaredMethod(jeiClazz, "addChild", classOf[IJavaElement]) else null
}

trait SourceRefJawaElementInfo extends JavaElementInfo {
  import JawaMemberElementInfo._

  def getDeclarationSourceStart0: Int = getDeclarationSourceStartMethod.invoke(this).asInstanceOf[Integer].intValue
  def getDeclarationSourceEnd0: Int = getDeclarationSourceEndMethod.invoke(this).asInstanceOf[Integer].intValue
  def setSourceRangeStart0(start: Int): Unit = setSourceRangeStartMethod.invoke(this, new Integer(start))
  def setSourceRangeEnd0(end: Int): Unit = setSourceRangeEndMethod.invoke(this, new Integer(end))
}

trait JawaMemberElementInfo extends SourceRefJawaElementInfo {
  import JawaMemberElementInfo._
  import java.lang.Integer

  def addChild0(child: IJavaElement): Unit

  def setFlags0(flags: Int) = setFlagsMethod.invoke(this, new Integer(flags))
  def getNameSourceStart0: Int = getNameSourceStartMethod.invoke(this).asInstanceOf[Integer].intValue
  def getNameSourceEnd0: Int = getNameSourceEndMethod.invoke(this).asInstanceOf[Integer].intValue
  def setNameSourceStart0(start: Int) = setNameSourceStartMethod.invoke(this, new Integer(start))
  def setNameSourceEnd0(end: Int) = setNameSourceEndMethod.invoke(this, new Integer(end))
}

trait AuxChildrenElementInfo extends JavaElementInfo {
  import JawaMemberElementInfo._

  var auxChildren: Array[IJavaElement] = if (hasChildrenField) null else new Array(0)

  override def getChildren = if (hasChildrenField) super.getChildren else auxChildren

  def addChild0(child: IJavaElement): Unit =
    if (hasChildrenField)
      addChildMethod.invoke(this, child)
    else if (auxChildren.length == 0)
      auxChildren = Array(child)
    else if (!auxChildren.contains(child))
      auxChildren = auxChildren ++ Seq(child)
}

class TypeParameterScalaElementInfo extends TypeParameterElementInfo with SourceRefJawaElementInfo

class JawaElementInfo extends SourceTypeElementInfo with JawaMemberElementInfo {
  import JawaMemberElementInfo._

  override def addChild0(child: IJavaElement): Unit = {
    if (hasChildrenField)
      addChildMethod.invoke(this, child)
    else if (children.length == 0)
      children = Array(child)
    else if (!children.contains(child))
      children = children ++ Seq(child)
  }

  override def setHandle(handle: IType) = super.setHandle(handle)
  override def setSuperclassName(superclassName: Array[Char]) = super.setSuperclassName(superclassName)
  override def setSuperInterfaceNames(superInterfaceNames: Array[Array[Char]]) = super.setSuperInterfaceNames(superInterfaceNames)
}

trait FnInfo extends SourceMethodElementInfo with JawaMemberElementInfo {
  override def setArgumentNames(argumentNames: Array[Array[Char]]) = super.setArgumentNames(argumentNames)
  def setReturnType(returnType: Array[Char])
  override def setExceptionTypeNames(exceptionTypeNames: Array[Array[Char]]) = super.setExceptionTypeNames(exceptionTypeNames)
}

class JawaSourceConstructorInfo extends SourceConstructorInfo with FnInfo with AuxChildrenElementInfo {
  override def setReturnType(returnType: Array[Char]) = super.setReturnType(returnType)
}

class JawaSourceMethodInfo extends SourceMethodInfo with FnInfo with AuxChildrenElementInfo {
  override def setReturnType(returnType: Array[Char]) = super.setReturnType(returnType)
}

class JawaSourceFieldElementInfo extends SourceFieldElementInfo with JawaMemberElementInfo with AuxChildrenElementInfo {
  override def setTypeName(name: Array[Char]) = super.setTypeName(name)
}

class LazyToplevelClass(unit: JawaCompilationUnit, name: String) extends SourceType(unit, name) with IType with JawaElement {

  /** I rewrote this method from the previous implementation, to what I believe was the initial intention.
   *  The commented line is the original, in case this causes any problems.
   *
   *  TODO: Revisit this once there is a better structure builder.
   */
  lazy val mirror: Option[JawaSourceTypeElement] = {
//    unit.getElementInfo.asInstanceOf[OpenableElementInfo].getChildren.find(e => e.getElementName == name).map(_.asInstanceOf[ScalaSourceTypeElement])
    unit.getElementInfo match {
      case openable: OpenableElementInfo =>
        openable.getChildren.find(e => e.getElementType == IJavaElement.TYPE && e.getElementName == name) map (_.asInstanceOf[JawaSourceTypeElement])
      case _ => None
    }
  }

  override def isAnonymous = false
  override def isLocal = false
  override def isEnum = false
  override def isInterface = mirror map (_.isInterface) getOrElse false
  override def getDeclaringType = null
  override def exists = mirror.isDefined
}
