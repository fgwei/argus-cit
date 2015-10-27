package org.arguside.util.internal

import org.eclipse.core.resources.IProject
import org.sireum.jawa.ObjectType
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.core.JavaCore
import org.sireum.jawa.Signature
import java.util.concurrent.atomic.AtomicBoolean
import org.eclipse.jdt.core.search.SearchRequestor
import org.eclipse.jdt.core.search.SearchMatch
import org.eclipse.jdt.core.IMethod
import org.eclipse.jdt.core.Flags
import org.eclipse.jdt.ui.JavaUI
import org.eclipse.core.runtime.CoreException
import org.arguside.core.internal.ArgusPlugin
import org.eclipse.jdt.core.search.SearchEngine
import org.eclipse.jdt.core.search.IJavaSearchScope
import org.eclipse.jdt.core.IType
import org.eclipse.jdt.core.search.SearchParticipant
import org.eclipse.jdt.core.search.SearchPattern
import org.eclipse.jdt.core.search.IJavaSearchConstants
import org.eclipse.core.runtime.NullProgressMonitor
import org.sireum.jawa.JavaKnowledge
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.jdt.internal.corext.util.MethodOverrideTester
import org.eclipse.jdt.core.ITypeHierarchy
import org.sireum.util._
import org.eclipse.jdt.core.IField

/**
 * @author fgwei
 */
object JavaElementFinder {
  def findJavaClass(project: IProject, typ: ObjectType): Option[IType] = {
    val fqcn = typ.name
    var fqcn2: String = fqcn
    // Handle inner classes
    if (fqcn.indexOf('$') != -1) {
      fqcn2 = fqcn.replaceAll("\\$", ".") //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    try {
      if(project.hasNature(JavaCore.NATURE_ID)){
        val javaProject = JavaCore.create(project)
        Option(javaProject.findType(fqcn2)) match {
          case Some(t) => Some(t)
          case None =>
            if(fqcn != fqcn2) Option(javaProject.findType(fqcn))
            else None
        }
      } else None
    } catch {
      case e: Throwable =>
        ArgusPlugin().logError(fqcn, e)
        None
    }
  }
  
  def findJavaMethod(project: IProject, sig: Signature): Option[IMethod] = {
    if(sig == null) return None
    
    var result: Option[IMethod] = None
    try {
      val receiverTypeOpt: Option[IType] = findJavaClass(project, sig.getClassType)
      receiverTypeOpt match {
        case Some(c) =>
          val suphie = c.newSupertypeHierarchy(new NullProgressMonitor())
          c.getMethods foreach {
            m =>
              if(matchMethod(sig, m)) throw FoundMethod(m)
          }
          findMethod(c, sig, suphie)
        case None =>
      }
    } catch {
      case found @ FoundMethod(m) =>
        result = Some(m)
      case e: Throwable =>
        ArgusPlugin().logError(null, e)
    }
    result
  }
  
  private def findMethod(c: IType, sig: Signature, h: ITypeHierarchy): Unit = {
    val tars: MList[IType] = mlistEmpty
    val su = h.getSuperclass(c) 
    if(su != null) tars += su
    tars ++= h.getSuperInterfaces(c) 
    tars foreach {
      case tar =>
        tar.getMethods foreach {
          m =>
            if(matchMethod(sig, m)) throw FoundMethod(m)
        }
    }
    tars foreach (findMethod(_, sig, h))
  }
  
  private def matchMethod(mysig: Signature, tarmethod: IMethod): Boolean = {
    val myParams = mysig.getParameterTypes()
    val tarParams = tarmethod.getParameterTypes
    var ok = true
    val myMethodName: String = mysig.methodNamePart.replace(JavaKnowledge.constructorName, mysig.getClassType.simpleName)
    if(myMethodName != tarmethod.getElementName) ok = false
    if(ok && myParams.size == tarParams.size){
      for(i <- 0 to myParams.size - 1){
        val myParam = myParams(i)
        val tarParam = tarParams(i)
        val tarType = JavaKnowledge.formatSignatureToType(tarParam)
        if(tarType.name != myParam.name) ok = false
      }
    } else ok = false
    ok
  }
  
  def findJavaField(project: IProject, fqn: String): Option[IField] = {
    if(fqn == null) return None
    var result: Option[IField] = None
    try {
      val classType: ObjectType = JavaKnowledge.getClassTypeFromFieldFQN(fqn)
      val receiverTypeOpt: Option[IType] = findJavaClass(project, classType)
      receiverTypeOpt match {
        case Some(c) =>
          val suphie = c.newSupertypeHierarchy(new NullProgressMonitor())
          c.getFields foreach {
            f =>
              if(matchField(fqn, f)) throw FoundField(f)
          }
          findField(c, fqn, suphie)
        case None =>
      }
    } catch {
      case found @ FoundField(f) =>
        result = Some(f)
      case e: Throwable =>
        ArgusPlugin().logError(null, e)
    }
    result
  }
  
  private def findField(c: IType, fqn: String, h: ITypeHierarchy): Unit = {
    val tars: MList[IType] = mlistEmpty
    val su = h.getSuperclass(c) 
    if(su != null) tars += su
    tars ++= h.getSuperInterfaces(c) 
    tars foreach {
      case tar =>
        tar.getFields foreach {
          f =>
            if(matchField(fqn, f)) throw FoundField(f)
        }
    }
    tars foreach (findField(_, fqn, h))
  }
  
  private def matchField(fqn: String, f: IField): Boolean = {
    val myFieldName: String = JavaKnowledge.getFieldNameFromFieldFQN(fqn)
    val tarFieldName: String = f.getElementName
    myFieldName == tarFieldName
  }
  
  case class FoundMethod(m: IMethod) extends Exception()
  case class FoundField(f: IField) extends Exception()
}