package org.arguside.ui.internal.editor.decorators.semantichighlighting

import org.arguside.ui.syntax.JawaSyntaxClasses
import org.arguside.ui.syntax.JawaSyntaxClasses._
import org.arguside.core.internal.decorators.semantichighlighting.Position
import org.arguside.core.internal.decorators.semantichighlighting.classifier.SymbolTypes
import org.arguside.core.internal.decorators.semantichighlighting.classifier.SymbolTypes._
import org.eclipse.jface.text.TextAttribute
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StyleRange
import org.eclipse.swt.graphics.Font
import org.arguside.ui.syntax.JawaSyntaxClass

case class HighlightingStyle(styledTextAttribute: TextAttribute, enabled: Boolean, unstyledTextAttribute: TextAttribute, deprecation: DeprecationStyle) {
  val ta = if (enabled) styledTextAttribute else unstyledTextAttribute
  lazy val deprecatedTextAttribute: TextAttribute = deprecation.buildTextAttribute(ta)

  def style(position: Position): StyleRange = {
    val textAttribute = if (position.deprecated) deprecatedTextAttribute else ta
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
    val deprecation = DeprecationStyle(preferences.isStrikethroughDeprecatedDecorationEnabled())
    HighlightingStyle(syntaxClass.getTextAttribute(preferences.store), enabled, JawaSyntaxClasses.DEFAULT.getTextAttribute(preferences.store), deprecation)
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

case class DeprecationStyle(enabled: Boolean) {
  def buildTextAttribute(ta: TextAttribute) = if (enabled) new TextAttribute(ta.getForeground, ta.getBackground, ta.getStyle | TextAttribute.STRIKETHROUGH, ta.getFont) else ta
}

case class StringInterpolationStyle(enabled: Boolean, modifier: TextAttribute) {
  def buildTextAttribute(ta: TextAttribute) = if (enabled) new TextAttribute(ta.getForeground, Option(ta.getBackground).getOrElse(modifier.getBackground), ta.getStyle | modifier.getStyle, ta.getFont) else ta
}
