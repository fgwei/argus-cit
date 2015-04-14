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
import argus.tools.eclipse.contribution.weaving.jdt.IArgusElement
import argus.tools.eclipse.contribution.weaving.jdt.ui.IMethodOverrideInfo
import org.arguside.util.internal.ReflectionUtils

trait ArgusElement extends JavaElement with IArgusElement {
  def getElementInfo : AnyRef
  def getElementName : String
  def scalaName : String = getElementName
  def labelName : String = scalaName
  def getLabelText(flags : Long) : String = labelName
  def getImageDescriptor : ImageDescriptor = null
  def isVisible = true

  override def getCompilationUnit() = {
    val cu = super.getCompilationUnit()
    if (cu != null) cu else new CompilationUnitAdapter(getClassFile().asInstanceOf[ArgusClassFile])
  }

  override def getAncestor(ancestorType : Int) : IJavaElement = {
    val ancestor = super.getAncestor(ancestorType)
    if (ancestor != null)
      ancestor
    else if(ancestorType == IJavaElement.COMPILATION_UNIT)
      new CompilationUnitAdapter(getClassFile().asInstanceOf[ArgusClassFile])
    else
      null
  }
}

trait ArgusFieldElement extends ArgusElement

class ArgusSourceTypeElement(parent : JavaElement, name : String)
  extends SourceType(parent, name) with ArgusElement {

  def getCorrespondingElement(element : IJavaElement) : Option[IJavaElement] = {
    val name = element.getElementName
    val tpe = element.getElementType
    getChildren.find(e => e.getElementName == name && e.getElementType == tpe)
  }

  override def getType(typeName : String) : IType = {
    val tpe = super.getType(typeName)
    getCorrespondingElement(tpe).getOrElse(tpe).asInstanceOf[IType]
  }

  override def getField(fieldName : String) : IField = {
    val field = super.getField(fieldName)
    getCorrespondingElement(field).getOrElse(field).asInstanceOf[IField]
  }

  override def getMethod(selector : String, parameterTypeSignatures : Array[String]) : IMethod = {
    val method = super.getMethod(selector, parameterTypeSignatures)
    getCorrespondingElement(method).getOrElse(method).asInstanceOf[IMethod]
  }
}

class ArgusClassElement(parent : JavaElement, name : String, synthetic : Boolean)
  extends ArgusSourceTypeElement(parent, name) {
  override def getImageDescriptor = ArgusImages.ARGUS_CLASS
  override def isVisible = !synthetic
}

class ArgusAnonymousClassElement(parent : JavaElement, name : String)
  extends ArgusClassElement(parent, name, false) {
    override def getLabelText(flags : Long) = if (name != null ) "new "+name+" {...}" else "new {...}"
}

class ArgusDefElement(parent : JavaElement, name: String, paramTypes : Array[String], synthetic : Boolean, display : String, overrideInfo : Int)
  extends SourceMethod(parent, name, paramTypes) with ArgusElement with IMethodOverrideInfo {
  override def getLabelText(flags : Long) = display
  override def isVisible = !synthetic && !getElementInfo.isInstanceOf[ArgusSourceConstructorInfo]
  def getOverrideInfo = overrideInfo
}

class ArgusFunctionElement(declaringType : JavaElement, parent : JavaElement, name: String, paramTypes : Array[String], display : String)
  extends SourceMethod(parent, name, paramTypes) with ArgusElement {
  override def getDeclaringType() : IType = declaringType.asInstanceOf[IType]
  override def getLabelText(flags : Long) = display
}

class ArgusAccessorElement(parent : JavaElement, name: String, paramTypes : Array[String])
  extends SourceMethod(parent, name, paramTypes) with ArgusElement {
  override def isVisible = false
}

class ArgusValElement(parent : JavaElement, name: String, display : String)
  extends SourceField(parent, name) with ArgusFieldElement {
  override def getLabelText(flags : Long) = display
  override def getImageDescriptor = {
    val flags = getFlags
    if ((flags & ClassFileConstants.AccPublic) != 0)
      ArgusImages.PUBLIC_VAL
    else if ((flags & ClassFileConstants.AccProtected) != 0)
      ArgusImages.PROTECTED_VAL
    else
      ArgusImages.PRIVATE_VAL
  }
}

class ArgusVarElement(parent : JavaElement, name: String, display : String)
  extends SourceField(parent, name) with ArgusFieldElement {
  override def getLabelText(flags : Long) = display
}

class ArgusTypeElement(parent : JavaElement, name : String, display : String)
  extends SourceField(parent, name) with ArgusFieldElement {
  override def getLabelText(flags : Long) = display
  override def getImageDescriptor = ArgusImages.ARGUS_TYPE
}

class ArgusLocalVariableElement(
  parent : JavaElement, name : String,
  declarationSourceStart : Int, declarationSourceEnd : Int, nameStart : Int, nameEnd : Int,
  typeSignature : String, display : String, jdtFlags : Int, methodParameter : Boolean ) extends LocalVariable(
  parent, name, declarationSourceStart, declarationSourceEnd, nameStart, nameEnd, typeSignature, null, jdtFlags, methodParameter) with
  ArgusElement {
  override def getLabelText(flags : Long) = display
}

object ArgusMemberElementInfo extends ReflectionUtils {
  val jeiClazz = Class.forName("org.eclipse.jdt.internal.core.JavaElementInfo")
  val meiClazz = Class.forName("org.eclipse.jdt.internal.core.MemberElementInfo")
  val aiClazz = Class.forName("org.eclipse.jdt.internal.core.AnnotatableInfo")
  val sreiClazz = Class.forName("org.eclipse.jdt.internal.core.SourceRefElementInfo")
  val setFlagsMethod = getDeclaredMethod(meiClazz, "setFlags", classOf[Int])
  val getNameSourceStartMethod = try {
    getDeclaredMethod(meiClazz, "getNameSourceStart")
  } catch {
    case _ : NoSuchMethodException => getDeclaredMethod(aiClazz, "getNameSourceStart")
  }
  val getNameSourceEndMethod = try {
    getDeclaredMethod(meiClazz, "getNameSourceEnd")
  } catch {
    case _ : NoSuchMethodException => getDeclaredMethod(aiClazz, "getNameSourceEnd")
  }
  val setNameSourceStartMethod = try {
    getDeclaredMethod(meiClazz, "setNameSourceStart", classOf[Int])
  } catch {
    case _ : NoSuchMethodException => getDeclaredMethod(aiClazz, "setNameSourceStart", classOf[Int])
  }
  val setNameSourceEndMethod = try {
    getDeclaredMethod(meiClazz, "setNameSourceEnd", classOf[Int])
  } catch {
    case _ : NoSuchMethodException => getDeclaredMethod(aiClazz, "setNameSourceEnd", classOf[Int])
  }
  val setSourceRangeStartMethod = getDeclaredMethod(sreiClazz, "setSourceRangeStart", classOf[Int])
  val setSourceRangeEndMethod = getDeclaredMethod(sreiClazz, "setSourceRangeEnd", classOf[Int])
  val getDeclarationSourceStartMethod = getDeclaredMethod(sreiClazz, "getDeclarationSourceStart")
  val getDeclarationSourceEndMethod = getDeclaredMethod(sreiClazz, "getDeclarationSourceEnd")
  val hasChildrenField = try {
    getDeclaredField(jeiClazz, "children")
    true
  } catch {
    case _ : NoSuchFieldException => false
  }
  val addChildMethod = if (hasChildrenField) getDeclaredMethod(jeiClazz, "addChild", classOf[IJavaElement]) else null
}

trait SourceRefArgusElementInfo extends JavaElementInfo {
  import ArgusMemberElementInfo._

  def getDeclarationSourceStart0 : Int = getDeclarationSourceStartMethod.invoke(this).asInstanceOf[Integer].intValue
  def getDeclarationSourceEnd0 : Int = getDeclarationSourceEndMethod.invoke(this).asInstanceOf[Integer].intValue
  def setSourceRangeStart0(start : Int) : Unit = setSourceRangeStartMethod.invoke(this, new Integer(start))
  def setSourceRangeEnd0(end : Int) : Unit = setSourceRangeEndMethod.invoke(this, new Integer(end))
}

trait ArgusMemberElementInfo extends SourceRefArgusElementInfo {
  import ArgusMemberElementInfo._
  import java.lang.Integer

  def addChild0(child : IJavaElement) : Unit

  def setFlags0(flags : Int) = setFlagsMethod.invoke(this, new Integer(flags))
  def getNameSourceStart0 : Int = getNameSourceStartMethod.invoke(this).asInstanceOf[Integer].intValue
  def getNameSourceEnd0 : Int = getNameSourceEndMethod.invoke(this).asInstanceOf[Integer].intValue
  def setNameSourceStart0(start : Int) = setNameSourceStartMethod.invoke(this, new Integer(start))
  def setNameSourceEnd0(end : Int) = setNameSourceEndMethod.invoke(this, new Integer(end))
}

trait AuxChildrenElementInfo extends JavaElementInfo {
  import ArgusMemberElementInfo._

  var auxChildren : Array[IJavaElement] = if (hasChildrenField) null else new Array(0)

  override def getChildren = if (hasChildrenField) super.getChildren else auxChildren

  def addChild0(child : IJavaElement) : Unit =
    if (hasChildrenField)
      addChildMethod.invoke(this, child)
    else if (auxChildren.length == 0)
      auxChildren = Array(child)
    else if (!auxChildren.contains(child))
      auxChildren = auxChildren ++ Seq(child)
}

trait HasTypeParameters {
  def setTypeParameters(typeParams: Array[ITypeParameter])
}

class TypeParameterScalaElementInfo extends TypeParameterElementInfo with SourceRefArgusElementInfo

class ArgusElementInfo extends SourceTypeElementInfo with ArgusMemberElementInfo with HasTypeParameters {
  import ArgusMemberElementInfo._

  override def addChild0(child : IJavaElement) : Unit = {
    if (hasChildrenField)
      addChildMethod.invoke(this, child)
    else if (children.length == 0)
      children = Array(child)
    else if (!children.contains(child))
      children = children ++ Seq(child)
  }

  override def setHandle(handle : IType) = super.setHandle(handle)
  override def setSuperclassName(superclassName : Array[Char]) = super.setSuperclassName(superclassName)
  override def setSuperInterfaceNames(superInterfaceNames : Array[Array[Char]]) = super.setSuperInterfaceNames(superInterfaceNames)
  override def setTypeParameters(tps : Array[ITypeParameter]) {
    typeParameters = tps
  }
}

trait FnInfo extends SourceMethodElementInfo with ArgusMemberElementInfo with HasTypeParameters {
  override def setArgumentNames(argumentNames : Array[Array[Char]]) = super.setArgumentNames(argumentNames)
  def setReturnType(returnType : Array[Char])
  override def setExceptionTypeNames(exceptionTypeNames : Array[Array[Char]]) = super.setExceptionTypeNames(exceptionTypeNames)
}

class ArgusSourceConstructorInfo extends SourceConstructorInfo with FnInfo with AuxChildrenElementInfo with HasTypeParameters {
  override def setReturnType(returnType : Array[Char]) = super.setReturnType(returnType)
  override def setTypeParameters(tps : Array[ITypeParameter]) {
    typeParameters = tps
  }
}

class ArgusSourceMethodInfo extends SourceMethodInfo with FnInfo with AuxChildrenElementInfo with HasTypeParameters {
  override def setReturnType(returnType : Array[Char]) = super.setReturnType(returnType)
  override def setTypeParameters(tps : Array[ITypeParameter]) {
    typeParameters = tps
  }
}

class ArgusSourceFieldElementInfo extends SourceFieldElementInfo with ArgusMemberElementInfo with AuxChildrenElementInfo {
  override def setTypeName(name : Array[Char]) = super.setTypeName(name)
}

class LazyToplevelClass(unit : ArgusCompilationUnit, name : String) extends SourceType(unit, name) with IType with ArgusElement {

  /** I rewrote this method from the previous implementation, to what I believe was the initial intention.
   *  The commented line is the original, in case this causes any problems.
   *
   *  TODO: Revisit this once there is a better structure builder.
   */
  lazy val mirror: Option[ArgusSourceTypeElement] = {
//    unit.getElementInfo.asInstanceOf[OpenableElementInfo].getChildren.find(e => e.getElementName == name).map(_.asInstanceOf[ScalaSourceTypeElement])
    unit.getElementInfo match {
      case openable: OpenableElementInfo =>
        openable.getChildren.find(e => e.getElementType == IJavaElement.TYPE && e.getElementName == name) map (_.asInstanceOf[ArgusSourceTypeElement])
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
