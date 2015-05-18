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
import org.sireum.jawa.sjc.parser.{Param => JawaParam}
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.Region
import org.sireum.jawa.sjc.util.Range
import org.sireum.jawa.sjc.parser.Annotation
import org.sireum.jawa.sjc.ObjectType

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
    symbolInfos += SymbolInfo(Class, classes.toList)
    symbolInfos += SymbolInfo(Method, methods.toList)
    symbolInfos += SymbolInfo(SymbolTypes.Location, classes.toList)
    symbolInfos += SymbolInfo(SymbolTypes.Annotation, classes.toList)
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
      case Annotation(at, id, value)                 => 
        annotations += id.range.toRegion
        id.text match {
          case "owner" | "classDescriptor" => classes += value.get.range.toRegion
          case _ =>
        }
      case typ: Type                                 => 
        typ.typ match{
          case ot: ObjectType if !ot.isJavaPrimitive(ot.typ) => classes += typ.baseTypeID.range.toRegion
          case _ =>
        }
      case MethodDeclaration(_, _, nameID, _, _, _)  =>
        methods += nameID.range.toRegion
      case JawaParam(_, nameID, _)                   =>
        localvars += nameID.range.toRegion
      case LocalVarDeclaration(nameID, _, _)         =>
        localvars += nameID.range.toRegion
      case CallStatement(_, _, _, invokeID, _, _)    => 
        methods += invokeID.range.toRegion
      case ArgClause(_, varIDs, _) =>
        localvars ++= varIDs.map(_._1.range.toRegion)
      case ThrowStatement(_, varID)                  =>
        localvars += varID.range.toRegion
      case IfStatement(_, _, _, locationID)          =>
        locations += locationID.range.toRegion
      case GotoStatement(_, locationID)              =>
        locations += locationID.range.toRegion
      case SwitchStatement(_, cond, _, _)            =>
        localvars += cond.range.toRegion
      case SwitchCase(_, _, _, _, locationID)        =>
        locations += locationID.range.toRegion
      case SwitchDefaultCase(_, _, _, _, locationID) =>
        locations += locationID.range.toRegion
      case ne @ NameExpression(nameID, _)            =>
        if(!ne.isStatic) localvars += nameID.range.toRegion
      case IndexingExpression(baseID, _)             =>
        localvars += baseID.range.toRegion
      case AccessExpression(baseID, _, _)            =>
        localvars += baseID.range.toRegion
      case CmpExpression(_, _, var1ID, _, var2ID, _) =>
        localvars += var1ID.range.toRegion
        localvars += var2ID.range.toRegion
      case CatchClause(_, _, _, _, locationID, _)    =>
        locations += locationID.range.toRegion
      case CatchRange(_, _, fromID, _, toID, _)      =>
        locations += fromID.range.toRegion
        locations += toID.range.toRegion
      case _ =>
    }
    astNode.immediateChildren.foreach(scan)
  }
}
