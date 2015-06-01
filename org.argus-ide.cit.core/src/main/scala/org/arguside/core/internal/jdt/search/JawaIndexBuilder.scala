package org.arguside.core.internal.jdt.search

import org.eclipse.core.resources.IFile
import scala.tools.nsc.symtab.Flags
import org.arguside.core.IArgusPlugin
import org.arguside.core.internal.compiler.JawaPresentationCompiler
import org.arguside.core.compiler.IJawaPresentationCompiler.Implicits._
import org.arguside.logging.HasLogger
import org.sireum.jawa.sjc.parser.CompilationUnit
import org.sireum.jawa.sjc.parser.ClassOrInterfaceDeclaration
import org.sireum.jawa.sjc.AccessFlag
import org.sireum.jawa.sjc.ObjectType
import org.sireum.jawa.sjc.parser.Field
import org.sireum.jawa.sjc.parser.Declaration
import org.sireum.jawa.sjc.parser.MethodDeclaration
import org.sireum.jawa.sjc.parser.ClassOrInterfaceDeclaration

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

    def getSuperNames(supers: List[ObjectType]): Array[Array[Char]] = {
      val superNames = supers map (_.name.toArray)
      superNames.toArray
    }

    def addClass(c : ClassOrInterfaceDeclaration) {
      val classType = c.typ
      val enclClassNames = classType.getEnclosingTypes.map(_.name.toCharArray())
      indexer.addClassDeclaration(
        mapModifiers(AccessFlag.getAccessFlags(c.accessModifier)),
        c.typ.pkg.toCharArray,
        c.typ.simpleName.toCharArray,
        enclClassNames.toArray,
        Array.empty,
        getSuperNames(c.parents),
        Array.empty,
        true
      )
    }

    def addField(v : Field with Declaration) {
      indexer.addFieldDeclaration(v.typ.typ.name.toCharArray, v.fieldName.toCharArray())
    }

    def addMethod(d : MethodDeclaration) {
      val name = d.name.toCharArray()
      val paramTypes = d.signature.getParameterTypes()
      val returnType = d.signature.getReturnType()
      indexer.addMethodDeclaration(
        name,
        paramTypes.map(_.name.toCharArray()).toArray,
        returnType.name.toCharArray(),
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
