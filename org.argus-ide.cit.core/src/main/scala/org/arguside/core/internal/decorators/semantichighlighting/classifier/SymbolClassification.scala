package org.arguside.core.internal.decorators.semantichighlighting.classifier

import org.arguside.logging.HasLogger
import org.arguside.core.internal.decorators.semantichighlighting.classifier.SymbolTypes._
import org.eclipse.core.runtime.IProgressMonitor
import org.arguside.core.compiler.IJawaPresentationCompiler
import org.arguside.core.compiler.IJawaPresentationCompiler.Implicits._
import org.sireum.util._
import org.sireum.jawa.sjc.io.AbstractFile
import org.sireum.jawa.sjc.util.SourceFile
import org.sireum.jawa.sjc.parser._
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.Region
import org.sireum.jawa.sjc.util.Range
import org.sireum.jawa.sjc.parser.Annotation

class SymbolClassification(protected val sourceFile: SourceFile, val global: IJawaPresentationCompiler, useSyntacticHints: Boolean)
  extends HasLogger {

  private implicit class RangeOps(range: Range) {
    def toRegion: IRegion = new Region(range.offset, range.length)
  }
  
  def compilationUnitOfFile(f: AbstractFile) = global.unitOfFile.get(f)

  def classifySymbols(progressMonitor: IProgressMonitor): IList[SymbolInfo] = {
    val symbolInfos: MList[SymbolInfo] = mlistEmpty
    for(rcu <- compilationUnitOfFile(sourceFile.file)){
      scan(rcu.cu)
    }
    symbolInfos.toList
  }
  
  var classes: ISet[IRegion] = isetEmpty
  var methods: ISet[IRegion] = isetEmpty
  var params: ISet[IRegion] = isetEmpty
  var locations: ISet[IRegion] = isetEmpty
  var annotations: ISet[IRegion] = isetEmpty
  var localvar: ISet[IRegion] = isetEmpty
  
  def scan(astNode: JawaAstNode) {
    astNode match {
      case NewExpression(_, typ)                             => typ.rangeOpt foreach(classes += _.toRegion)
      case ExtendsAndImplimentsClauses(_, p, ps)             => (p :: ps.map(_._2)) map (_.rangeOpt) filter (_.isDefined) foreach (classes += _.get.toRegion)
      case Annotation(at, id, _)                             => annotations += Range(at.pos.start, id.pos.end - id.pos.start + 1).toRegion
      case cs @ CallStatement(_, lhs, _, invokeID, args, _)  => 
        lhs.rangeOpt.foreach(localvar += _.toRegion)
        cs.typ
      case _ =>
    }
    astNode.immediateChildren.foreach(scan)
  }
}
