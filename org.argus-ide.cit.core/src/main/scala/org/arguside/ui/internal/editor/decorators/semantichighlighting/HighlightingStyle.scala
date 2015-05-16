package org.arguside.ui.internal.editor.decorators.semantichighlighting

import org.arguside.ui.syntax.JawaSyntaxClass
import org.arguside.ui.syntax.JawaSyntaxClasses._
import org.arguside.core.internal.decorators.semantichighlighting.Position
import org.arguside.core.internal.decorators.semantichighlighting.classifier.SymbolTypes
import org.arguside.core.internal.decorators.semantichighlighting.classifier.SymbolTypes._
import org.eclipse.jface.text.TextAttribute
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StyleRange
import org.eclipse.swt.graphics.Font
import org.arguside.ui.syntax.JawaSyntaxClasses

case class HighlightingStyle(styledTextAttribute: TextAttribute, enabled: Boolean, unstyledTextAttribute: TextAttribute) {
  val ta = if (enabled) styledTextAttribute else unstyledTextAttribute

  def style(position: Position): StyleRange = {
    val textAttribute = ta
    val s = textAttribute.getStyle()
    val fontStyle = s & (SWT.ITALIC | SWT.BOLD | SWT.NORMAL)
    val styleRange = new StyleRange(position.getOffset(), position.getLength(), textAttribute.getForeground(), textAttribute.getBackground(), fontStyle)
    styleRange.strikeout = (s & TextAttribute.STRIKETHROUGH) != 0
    styleRange.underline = (s & TextAttribute.UNDERLINE) != 0
    styleRange
  }
}

object HighlightingStyle {
  def apply(preferences: Preferences, symbolType: SymbolTypes.SymbolType): HighlightingStyle = {
    val syntaxClass = symbolTypeToSyntaxClass(symbolType)
    val enabled = syntaxClass.enabled(preferences.store)
    HighlightingStyle(syntaxClass.getTextAttribute(preferences.store), enabled, JawaSyntaxClasses.DEFAULT.getTextAttribute(preferences.store))
  }

  def symbolTypeToSyntaxClass(symbolType: SymbolTypes.SymbolType): JawaSyntaxClass = {
    symbolType match {
      case Annotation        => ANNOTATION
      case Class             => CLASS
      case LocalVar          => LOCAL_VAR
      case Method            => METHOD
      case Param             => PARAM
    }
  }
}