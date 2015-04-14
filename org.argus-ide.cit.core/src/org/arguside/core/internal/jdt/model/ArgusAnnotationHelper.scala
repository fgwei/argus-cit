package org.arguside.core.internal.jdt.model

import org.arguside.core.compiler.IArgusPresentationCompiler
import org.arguside.core.internal.compiler.ArgusPresentationCompiler

trait ScalaAnnotationHelper extends IArgusPresentationCompiler { self: ArgusPresentationCompiler =>

  protected[internal] def hasTransientAnn(sym: Symbol) = sym.hasAnnotation(definitions.TransientAttr)
  protected[internal] def hasVolatileAnn(sym: Symbol) = sym.hasAnnotation(definitions.VolatileAttr)
  protected[internal] def hasNativeAnn(sym: Symbol) = sym.hasAnnotation(definitions.NativeAttr)
  protected[internal] def hasStrictFPAnn(sym: Symbol) = sym.hasAnnotation(definitions.ScalaStrictFPAttr)
  protected[internal] def hasThrowsAnn(sym: Symbol) = sym.hasAnnotation(definitions.ThrowsClass)
  protected[internal] def hasDeprecatedAnn(sym: Symbol) = sym.hasAnnotation(definitions.DeprecatedAttr)

  protected[internal] def isScalaAnnotation(ann: AnnotationInfo) = {
    val isJava = ann.atp.typeSymbol.isJavaDefined
    !isJava
  }
}
