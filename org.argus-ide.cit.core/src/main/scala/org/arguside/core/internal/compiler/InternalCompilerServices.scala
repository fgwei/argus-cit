package org.arguside.core.internal.compiler

import org.sireum.jawa.sjc.lexer.Token
import org.sireum.jawa.sjc.JawaType
import org.sireum.jawa.sjc.interactive.Global
import org.sireum.jawa.sjc.interactive.{JawaElement => SJCJawaElement}

/** This trait groups methods are only available to core IDE implementations.
 *  They may change without notice or deprecation cycle.
 */
trait InternalCompilerServices extends Global {
  /** Return the enclosing package. Correctly handle the empty package, by returning
   *  the empty string, instead of <empty>.
   */
  private[core] def javaEnclosingPackage(je: SJCJawaElement): String

  /** Return the full name of the enclosing type name, without enclosing packages. */
  private[core] def enclosingTypeName(je : SJCJawaElement): String

  /** Return the descriptor of the given type. A typed descriptor is defined
   *  by the JVM Specification Section 4.3 (http://docs.oracle.com/javase/specs/vms/se7/html/jvms-4.html#jvms-4.3)
   *
   *  Example:
   *   javaDescriptor(Array[List[Int]]) == "[Lscala/collection/immutable/List;"
   */
  private[core] def javaDescriptor(tpe: JawaType): String

  /** Return a JDT specific value for the modifiers of given symbol/ */
  private[core] def mapModifiers(je: SJCJawaElement): Int
  
  /** Return a JDT specific value for the modifiers of given symbol/ */
  private[core] def mapModifiers(af: Int): Int
}