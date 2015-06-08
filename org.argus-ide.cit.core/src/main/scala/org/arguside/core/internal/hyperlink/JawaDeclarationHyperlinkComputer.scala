package org.arguside.core.internal.hyperlink

import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.hyperlink.IHyperlink
import org.arguside.logging.HasLogger
import org.arguside.core.compiler.InteractiveCompilationUnit
import org.arguside.core.compiler.IJawaPresentationCompiler.Implicits._
import org.sireum.jawa.sjc.lexer.{Token => JawaToken}
import org.sireum.jawa.sjc.util.Position
import org.sireum.jawa.sjc.parser.JawaAstNode
import org.sireum.jawa.sjc.parser.JawaSymbol

class JawaDeclarationHyperlinkComputer extends HasLogger {
  def findHyperlinks(icu: InteractiveCompilationUnit, wordRegion: IRegion): Option[List[IHyperlink]] = {
    findHyperlinks(icu, wordRegion, wordRegion)
  }

  def findHyperlinks(icu: InteractiveCompilationUnit, wordRegion: IRegion, mappedRegion: IRegion): Option[List[IHyperlink]] = {
    logger.info("detectHyperlinks: wordRegion = " + mappedRegion)

    icu.withSourceFile({ (sourceFile, compiler) =>
      if (mappedRegion == null || mappedRegion.getLength == 0)
        None
      else {
        val start = mappedRegion.getOffset
        val regionEnd = mappedRegion.getOffset + mappedRegion.getLength - 1
        // removing 1 handles correctly hyperlinking requests @ EOF
        val end = if (sourceFile.length == regionEnd) regionEnd - 1 else regionEnd

        val pos = Position.range(sourceFile, start, end - start + 1)

        val typed = compiler.askTypeAt(pos).getOption().getOrElse(None)

        typed map { sym =>
          compiler.mkHyperlink(sym, s"Open Declaration (${sym.id.text})", wordRegion).toList
        }
      }
    }).flatten
  }

}
