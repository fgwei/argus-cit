package org.arguside.core.internal.decorators.semantichighlighting.classifier

import org.arguside.logging.HasLogger
import org.arguside.core.internal.decorators.semantichighlighting.classifier.SymbolTypes._
import org.eclipse.core.runtime.IProgressMonitor
import org.arguside.core.compiler.IJawaPresentationCompiler
import org.arguside.core.compiler.IJawaPresentationCompiler.Implicits._
import org.sireum.util._
import org.sireum.jawa.io.AbstractFile
import org.sireum.jawa.io.SourceFile
import org.sireum.jawa.sjc.parser._
import org.sireum.jawa.sjc.parser.{Param => JawaParam}
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.Region
import org.sireum.jawa.io.Range
import org.sireum.jawa.sjc.parser.Annotation
import org.arguside.core.internal.ArgusPlugin

class SymbolClassification(protected val sourceFile: SourceFile, val global: IJawaPresentationCompiler, useSyntacticHints: Boolean)
  extends HasLogger {

  private implicit class RangeOps(range: Range) {
    def toRegion: IRegion = new Region(range.offset, range.length)
  }
  
  def compilationUnitOfFile(f: AbstractFile) = global.getCompilationUnit(f)
  
  import scala.collection.mutable
  
  def classifySymbols(progressMonitor: IProgressMonitor): IList[SymbolInfo] = {
    val symbolInfos: MList[SymbolInfo] = mlistEmpty
    for(cu <- global.parseCompilationUnit(sourceFile)){
      scan(cu)
    }
    symbolInfos += SymbolInfo(Class, classes.toList)
    symbolInfos += SymbolInfo(Method, methods.toList)
    symbolInfos += SymbolInfo(SymbolTypes.Location, locations.toList)
    symbolInfos += SymbolInfo(SymbolTypes.Annotation, annotations.toList)
    symbolInfos += SymbolInfo(LocalVar, localvars.toList)
    symbolInfos.toList
  }
  
  private val classes: MList[IRegion] = mlistEmpty
  private val methods: MList[IRegion] = mlistEmpty
  private val locations: MList[IRegion] = mlistEmpty
  private val annotations: MList[IRegion] = mlistEmpty
  private val localvars: MList[IRegion] = mlistEmpty
  
  def scan(astNode: JawaAstNode) {
    astNode match {
      case Annotation(_, id, _)                  =>
        annotations += id.range.toRegion
      case TypeDefSymbol(id)                        => 
        classes += id.range.toRegion
      case MethodDefSymbol(id)  =>
        methods += id.range.toRegion
      case VarDefSymbol(id)                    =>
        localvars += id.range.toRegion
      case TypeSymbol(id)                        => 
        classes += id.range.toRegion
      case MethodNameSymbol(id)  =>
        methods += id.range.toRegion
      case VarSymbol(id)                    =>
        localvars += id.range.toRegion
      case LocationSymbol(id)                =>
        locations += id.range.toRegion
      case _ =>
    }
    astNode.immediateChildren.foreach(scan)
  }
}
