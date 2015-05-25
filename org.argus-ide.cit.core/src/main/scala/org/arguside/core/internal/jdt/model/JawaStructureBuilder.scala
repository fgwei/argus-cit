package org.arguside.core.internal.jdt.model

import java.io.PrintWriter
import java.io.StringWriter
import java.util.{ Map => JMap }
import org.eclipse.core.resources.IFile
import org.eclipse.jdt.core.IAnnotation
import org.eclipse.jdt.core.ICompilationUnit
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.core.IMemberValuePair
import org.eclipse.jdt.core.Signature
import org.eclipse.jdt.core.compiler.CharOperation
import org.eclipse.jdt.internal.core.{ Annotation => JDTAnnotation }
import org.eclipse.jdt.internal.core.{ AnnotationInfo => JDTAnnotationInfo }
import org.eclipse.jdt.internal.core.AnnotatableInfo
import org.eclipse.jdt.internal.core.{ CompilationUnit => JDTCompilationUnit }
import org.eclipse.jdt.internal.core.ImportContainer
import org.eclipse.jdt.internal.core.ImportContainerInfo
import org.eclipse.jdt.internal.core.ImportDeclaration
import org.eclipse.jdt.internal.core.ImportDeclarationElementInfo
import org.eclipse.jdt.internal.core.JavaElement
import org.eclipse.jdt.internal.core.JavaElementInfo
import org.eclipse.jdt.internal.core.MemberValuePair
import org.eclipse.jdt.internal.core.OpenableElementInfo
import org.eclipse.jdt.internal.core.SourceRefElement
import org.eclipse.jdt.internal.core.TypeParameter
import org.eclipse.jdt.internal.core.TypeParameterElementInfo
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants
import org.eclipse.jdt.ui.JavaElementImageDescriptor
import scala.collection.Map
import scala.collection.mutable.HashMap
import org.arguside.util.internal.ReflectionUtils
import org.arguside.core.internal.compiler.JawaPresentationCompiler
import org.arguside.core.internal.jdt.util.SourceRefElementInfoUtils
import org.arguside.core.internal.jdt.util.ImportContainerInfoUtils
import org.arguside.core.compiler.IJawaPresentationCompiler
import org.sireum.jawa.sjc.parser.JawaAstNode
import org.sireum.jawa.sjc.parser.ClassOrInterfaceDeclaration
import org.sireum.jawa.sjc.parser.MethodDeclaration
import org.sireum.jawa.sjc.parser.Field
import org.sireum.jawa.sjc.parser.Declaration
import org.sireum.jawa.sjc.parser.CompilationUnit
import org.sireum.jawa.sjc.ObjectType
import org.sireum.jawa.sjc.lexer.{Token => JawaToken}
import org.sireum.jawa.sjc.lexer.Tokens._
import org.sireum.jawa.sjc.ResolveLevel
import org.sireum.jawa.sjc.interactive.JawaClass
import org.sireum.jawa.sjc.interactive.JawaMethod
import org.sireum.jawa.AccessFlag

trait JawaStructureBuilder extends IJawaPresentationCompiler { pc : JawaPresentationCompiler =>


  class StructureBuilderTraverser(scu : JawaCompilationUnit, unitInfo : OpenableElementInfo, newElements0 : JMap[AnyRef, AnyRef], sourceLength : Int) {

    type OverrideInfo = Int
    val overrideInfos = (new collection.mutable.HashMap[JawaMethod, OverrideInfo]).withDefaultValue(0)

    def fillOverrideInfos(m : JawaMethod) {
      if(m.isOverride){
        overrideInfos += m -> JavaElementImageDescriptor.OVERRIDES 
      } else if(m.isImplements) {
        overrideInfos += m -> JavaElementImageDescriptor.IMPLEMENTS
      }
    }

    trait Owner {self =>
      def parent : Owner
      def jdtOwner = this

      def element : JavaElement
      def elementInfo : JavaElementInfo
      def compilationUnitBuilder : CompilationUnitBuilder = parent.compilationUnitBuilder

      def addClass(c : ClassOrInterfaceDeclaration) : Owner = this
      def addField(v : Field with Declaration) : Owner = this

      def addMethod(d: MethodDeclaration) : Owner = this

      def addChild(child : JavaElement) =
        elementInfo match {
          case jawaMember : JawaMemberElementInfo => jawaMember.addChild0(child)
          case openable : OpenableElementInfo => openable.addChild(child)
        }

      def classes : Map[JawaClass, (JawaElement, JawaElementInfo)] = Map.empty
    }

    trait ClassOwner extends Owner { self =>
      override val classes = new HashMap[JawaClass, (JawaElement, JawaElementInfo)]

      override def addClass(c : ClassOrInterfaceDeclaration) : Owner = {
        val typ: ObjectType = c.typ
        val clazz: JawaClass = resolveClassFromSource(typ, c.firstToken.file, ResolveLevel.HIERARCHY)

        val classElem = new JawaClassElement(element, typ)

        resolveDuplicates(classElem)
        addChild(classElem)

        val classElemInfo = new JawaElementInfo
        classes(clazz) = (classElem, classElemInfo)
        classElemInfo.setHandle(classElem)
        
        classElemInfo.setFlags0((mapModifiers(clazz)))

        val superClass = clazz.getSuperClass match {
          case Some(su) => classElemInfo.setSuperclassName(su.getName.toCharArray)
          case None =>
        }
        
        val interfaceNames = clazz.getInterfaces.map(_.getName.toCharArray)
        classElemInfo.setSuperInterfaceNames(interfaceNames.toArray)
        
        val start: Int = c.cityp.baseTypeID.pos.start
        val end: Int = start + typ.name.length() - 1
        classElemInfo.setNameSourceStart0(start)
        classElemInfo.setNameSourceEnd0(end)
        setSourceRange(classElemInfo, c.cityp.baseTypeID)
        newElements0.put(classElem, classElemInfo)

        clazz.getMethods foreach{fillOverrideInfos(_)}
        

        new Builder {
          val parent = self
          val element = classElem
          val elementInfo = classElemInfo
        }
      }
    }

    trait FieldOwner extends Owner { self =>
      override def addField(f : Field with Declaration) : Owner = {
        require(element.isInstanceOf[JawaClassElement])
        val elemName = f.fieldName
        val display = f.FQN
        val field = getField(f.FQN).get

        val fieldElem = new JawaFieldElement(element, elemName.toString, display)

        resolveDuplicates(fieldElem)
        addChild(fieldElem)

        val fieldElemInfo = new JawaSourceFieldElementInfo
        fieldElemInfo.setFlags0(mapModifiers(field))

        val start = f.nameID.pos.point
        val end = start+display.length-1

        fieldElemInfo.setNameSourceStart0(start)
        fieldElemInfo.setNameSourceEnd0(end)
        setSourceRange(fieldElemInfo, f.nameID)
        newElements0.put(fieldElem, fieldElemInfo)

        fieldElemInfo.setTypeName(field.getType.name.toCharArray())

        self
      }
    }

    trait MethodOwner extends Owner { self =>
      override def addMethod(m: MethodDeclaration): Owner = {
        val method = getMethod(m.signature).get
        val isCtor0 = method.isConstructor
        val nameString = method.getName

        val paramsTypes = method.getParamTypes.map(n => n.name).toArray

        /** Return the parameter names. Make sure that parameter names and the
         *  parameter types have the same length. A mismatch here will crash the JDT later.
         */
        def paramNames: (Array[Array[Char]]) = {
          paramsTypes.map(n => n.toCharArray)
        }

        val display = method.getSignature.signature

        val methodElem = new JawaMethodElement(element, nameString, paramsTypes, method.isSynthetic, display, overrideInfos(method))
        resolveDuplicates(methodElem)
        addChild(methodElem)

        val methodElemInfo: FnInfo =
          if(isCtor0)
            new JawaSourceConstructorInfo
          else
            new JawaSourceMethodInfo

        methodElemInfo.setArgumentNames(paramNames)
        methodElemInfo.setReturnType(method.returnType.name.toCharArray())

        val mods = mapModifiers(method)

        methodElemInfo.setFlags0(mods)

        if (isCtor0) {
          elementInfo match {
            case smei : JawaMemberElementInfo =>
              methodElemInfo.setNameSourceStart0(smei.getNameSourceStart0)
              methodElemInfo.setNameSourceEnd0(smei.getNameSourceEnd0)
              methodElemInfo.setSourceRangeStart0(smei.getDeclarationSourceStart0)
              methodElemInfo.setSourceRangeEnd0(smei.getDeclarationSourceEnd0)
            case _ =>
          }
        } else {
          val start = m.nameID.pos.pointOrElse(-1)
          val end = if (start >= 0) start+methodElem.labelName.length-1 else -1

          methodElemInfo.setNameSourceStart0(start)
          methodElemInfo.setNameSourceEnd0(end)
          setSourceRange(methodElemInfo, m.nameID)
        }

        newElements0.put(methodElem, methodElemInfo)

        self
      }
    }

    def resolveDuplicates(handle : SourceRefElement) {
      while (newElements0.containsKey(handle)) {
        handle.occurrenceCount += 1
      }
    }

    class CompilationUnitBuilder extends ClassOwner {
      val parent = null
      val element = scu
      val elementInfo = unitInfo
      override def compilationUnitBuilder = this
    }
    
    abstract class Builder extends ClassOwner with FieldOwner with MethodOwner

    def setSourceRange(info: JawaMemberElementInfo, token: JawaToken) {
      val pos = token.pos
      val (start, end) =
        if (pos.isDefined) {
          val pos0 =  pos
          val start0 = pos0.start
          (start0, pos0.end-1)
        }
        else
          (-1, -1)

      info.setSourceRangeStart0(start)
      info.setSourceRangeEnd0(end)
    }

    def traverse(tree: JawaAstNode) {
      val traverser = new TreeTraverser
      traverser.traverse(tree, new CompilationUnitBuilder)
    }

    private[JawaStructureBuilder] class TreeTraverser {

      def traverse(tree: JawaAstNode, builder: Owner) {
        val (newBuilder, children) = {
          tree match {
            case cu: CompilationUnit =>  
              (builder, cu.topDecls)
            case cd: ClassOrInterfaceDeclaration =>
              (builder.addClass(cd), cd.instanceFields ++ cd.staticFields ++ cd.methods)
            case vd: Field with Declaration => (builder.addField(vd), Nil)
            case dd: MethodDeclaration => (builder.addMethod(dd), Nil)
            case _ => (builder, Nil)
          }
        }
        children.foreach {traverse(_, newBuilder)}
      }
    }
  }
}

object JDTAnnotationUtils extends ReflectionUtils {
  val aiClazz = classOf[AnnotatableInfo]
  val annotationsField = getDeclaredField(aiClazz, "annotations")

  def getAnnotations(ai : AnnotatableInfo) = annotationsField.get(ai).asInstanceOf[Array[IAnnotation]]
  def setAnnotations(ai : AnnotatableInfo, annotations : Array[IAnnotation]) = annotationsField.set(ai, annotations)
}
