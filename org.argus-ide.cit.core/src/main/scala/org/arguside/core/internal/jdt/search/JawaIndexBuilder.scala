package org.arguside.core.internal.jdt.search

import org.eclipse.core.resources.IFile
import scala.tools.nsc.symtab.Flags
import org.arguside.core.IArgusPlugin
import org.arguside.core.internal.compiler.JawaPresentationCompiler
import org.arguside.core.compiler.IJawaPresentationCompiler.Implicits._
import org.arguside.logging.HasLogger
import org.sireum.jawa.sjc.parser.CompilationUnit
import org.sireum.jawa.sjc.parser.ClassOrInterfaceDeclaration
import org.sireum.jawa.AccessFlag
import org.sireum.jawa.JawaType
import org.sireum.jawa.sjc.parser.Field
import org.sireum.jawa.sjc.parser.Declaration
import org.sireum.jawa.sjc.parser.MethodDeclaration
import org.sireum.jawa.sjc.parser.ClassOrInterfaceDeclaration
import org.arguside.core.internal.ArgusPlugin

/** Add entries to the JDT index.
 *
 *  The indexer builds a map from names to documents that mention that name. Names are
 *  categorized (for instance, as method definitions, method references, annotation references, etc.).
 *
 *  The indexer is later used by the JDT to narrow the scope of a search. For instance, a search
 *  for test methods would first check the index for documents that have annotation references to
 *  'Test' in 'org.junit', and then pass those documents to the structure builder for
 *  precise parsing, where names are actually resolved.
 */
trait JawaIndexBuilder extends HasLogger { self: JawaPresentationCompiler =>

  class IndexBuilderTraverser(indexer : JawaSourceIndexer) {

    def addClass(c : ClassOrInterfaceDeclaration) {
      val classType = c.typ
      
      val pack = c.typ.getPackageName
      val sName: String = c.typ.name.substring(c.typ.name.lastIndexOf("."))
      val enclClassNames = classType.getEnclosingTypes.map(_.canonicalName.toCharArray())
      val superName = c.superClassOpt.getOrElse(JAVA_TOPLEVEL_OBJECT_TYPE).canonicalName
      val interfaceNames = c.interfaces map (_.canonicalName.toArray)
      indexer.addClassDeclaration(
        mapModifiers(AccessFlag.getAccessFlags(c.accessModifier)),
        pack.toCharArray,
        sName.toCharArray,
        enclClassNames.toArray,
        superName.toArray,
        interfaceNames.toArray,
        Array.empty,
        true
      )
    }

    def addField(v : Field with Declaration) {
      indexer.addFieldDeclaration(v.typ.typ.canonicalName.toCharArray, v.fieldName.toCharArray())
    }

    def addMethod(d : MethodDeclaration) {
      val name = 
        if(d.isConstructor) d.enclosingTopLevelClass.typ.simpleName.toCharArray()
        else d.name.toCharArray()
      
      val paramTypes = d.signature.getParameterTypes()
      val returnType = d.signature.getReturnType()
      indexer.addMethodDeclaration(
        name,
        paramTypes.map(_.canonicalName.toCharArray()).toArray,
        returnType.canonicalName.toCharArray(),
        Array.empty
      )
    }

    def traverse(cu: CompilationUnit): Unit = {
      cu.topDecls foreach {
        cid =>
          traverseClass(cid)
      }
    }
    
    private def traverseClass(cid: ClassOrInterfaceDeclaration) = {
      addClass(cid)
      cid.fields foreach {
        fd =>
          traverseField(fd)
      }
      cid.methods foreach {
        md =>
          traverseMethod(md)
      }
    }
    
    private def traverseField(fd: Field with Declaration) = {
      addField(fd)
    }
    
    private def traverseMethod(md: MethodDeclaration) = {
      addMethod(md)
    }
  }
}
