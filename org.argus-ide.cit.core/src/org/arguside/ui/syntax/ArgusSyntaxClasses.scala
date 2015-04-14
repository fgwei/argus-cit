package org.arguside.ui.syntax

import scalariform.lexer.Tokens._
import scalariform.lexer._
import ArgusSyntaxClass.Category

object ArgusSyntaxClasses {

  val SINGLE_LINE_COMMENT = ArgusSyntaxClass("Single-line comment", "syntaxColouring.singleLineComment")
  val MULTI_LINE_COMMENT = ArgusSyntaxClass("Multi-line comment", "syntaxColouring.multiLineComment")
  val SCALADOC = ArgusSyntaxClass("Scaladoc comment", "syntaxColouring.scaladoc")
  val SCALADOC_CODE_BLOCK = ArgusSyntaxClass("Scaladoc code block", "syntaxColouring.scaladocCodeBlock")
  val SCALADOC_ANNOTATION = ArgusSyntaxClass("Scaladoc annotation", "syntaxColouring.scaladocAnnotation")
  val SCALADOC_MACRO = ArgusSyntaxClass("Scaladoc macro", "syntaxColouring.scaladocMacro")
  val TASK_TAG = ArgusSyntaxClass("Task Tag", "syntaxColouring.taskTag")
  val OPERATOR = ArgusSyntaxClass("Operator", "syntaxColouring.operator")
  val KEYWORD = ArgusSyntaxClass("Keywords (excluding 'return')", "syntaxColouring.keyword")
  val RETURN = ArgusSyntaxClass("Keyword 'return'", "syntaxColouring.return")
  val STRING = ArgusSyntaxClass("Strings", "syntaxColouring.string")
  val CHARACTER = ArgusSyntaxClass("Characters", "syntaxColouring.character")
  val MULTI_LINE_STRING = ArgusSyntaxClass("Multi-line string", "syntaxColouring.multiLineString")
  val BRACKET = ArgusSyntaxClass("Brackets", "syntaxColouring.bracket")
  val DEFAULT = ArgusSyntaxClass("Others", "syntaxColouring.default")
  val SYMBOL = ArgusSyntaxClass("Symbol", "syntaxColouring.symbol")
  val NUMBER_LITERAL = ArgusSyntaxClass("Number literals", "syntaxColouring.numberLiteral")
  val ESCAPE_SEQUENCE = ArgusSyntaxClass("Escape sequences", "syntaxColouring.escapeSequence")

  val XML_COMMENT = ArgusSyntaxClass("Comments", "syntaxColouring.xml.comment")
  val XML_ATTRIBUTE_VALUE = ArgusSyntaxClass("Attribute values", "syntaxColouring.xml.attributeValue")
  val XML_ATTRIBUTE_NAME = ArgusSyntaxClass("Attribute names", "syntaxColouring.xml.attributeName")
  val XML_ATTRIBUTE_EQUALS = ArgusSyntaxClass("Attribute equal signs", "syntaxColouring.xml.equals")
  val XML_TAG_DELIMITER = ArgusSyntaxClass("Tag delimiters", "syntaxColouring.xml.tagDelimiter")
  val XML_TAG_NAME = ArgusSyntaxClass("Tag names", "syntaxColouring.xml.tagName")
  val XML_PI = ArgusSyntaxClass("Processing instructions", "syntaxColouring.xml.processingInstruction")
  val XML_CDATA_BORDER = ArgusSyntaxClass("CDATA delimiters", "syntaxColouring.xml.cdata")

  val ANNOTATION = ArgusSyntaxClass("Annotation", "syntaxColouring.semantic.annotation", canBeDisabled = true)
  val CASE_CLASS = ArgusSyntaxClass("Case class", "syntaxColouring.semantic.caseClass", canBeDisabled = true)
  val CASE_OBJECT = ArgusSyntaxClass("Case object", "syntaxColouring.semantic.caseObject", canBeDisabled = true)
  val CLASS = ArgusSyntaxClass("Class", "syntaxColouring.semantic.class", canBeDisabled = true)
  val LAZY_LOCAL_VAL = ArgusSyntaxClass("Lazy local val", "syntaxColouring.semantic.lazyLocalVal", canBeDisabled = true)
  val LAZY_TEMPLATE_VAL = ArgusSyntaxClass("Lazy template val", "syntaxColouring.semantic.lazyTemplateVal", canBeDisabled = true)
  val LOCAL_VAL = ArgusSyntaxClass("Local val", "syntaxColouring.semantic.localVal", canBeDisabled = true)
  val LOCAL_VAR = ArgusSyntaxClass("Local var", "syntaxColouring.semantic.localVar", canBeDisabled = true)
  val METHOD = ArgusSyntaxClass("Method", "syntaxColouring.semantic.method", canBeDisabled = true)
  val OBJECT = ArgusSyntaxClass("Object", "syntaxColouring.semantic.object", canBeDisabled = true)
  val PACKAGE = ArgusSyntaxClass("Package", "syntaxColouring.semantic.package", canBeDisabled = true)
  val PARAM = ArgusSyntaxClass("Parameter", "syntaxColouring.semantic.methodParam", canBeDisabled = true)
  val TEMPLATE_VAL = ArgusSyntaxClass("Template val", "syntaxColouring.semantic.templateVal", canBeDisabled = true)
  val TEMPLATE_VAR = ArgusSyntaxClass("Template var", "syntaxColouring.semantic.templateVar", canBeDisabled = true)
  val TRAIT = ArgusSyntaxClass("Trait", "syntaxColouring.semantic.trait", canBeDisabled = true)
  val TYPE = ArgusSyntaxClass("Type", "syntaxColouring.semantic.type", canBeDisabled = true)
  val TYPE_PARAMETER = ArgusSyntaxClass("Type parameter", "syntaxColouring.semantic.typeParameter", canBeDisabled = true)
  val IDENTIFIER_IN_INTERPOLATED_STRING = ArgusSyntaxClass("Identifier in interpolated string", "syntaxColouring.semantic.identifierInInterpolatedString", hasForegroundColor = false, canBeDisabled = true)
  val CALL_BY_NAME_PARAMETER = ArgusSyntaxClass("By-name parameter at call site", "syntaxColouring.semantic.byNameParameterAtCallSite", hasForegroundColor = true, canBeDisabled = true)

  val DYNAMIC_SELECT = ArgusSyntaxClass("Call of selectDynamic", "syntaxColouring.semantic.selectDynamic", canBeDisabled = true)
  val DYNAMIC_UPDATE = ArgusSyntaxClass("Call of updateDynamic", "syntaxColouring.semantic.updateDynamic", canBeDisabled = true)
  val DYNAMIC_APPLY = ArgusSyntaxClass("Call of applyDynamic", "syntaxColouring.semantic.applyDynamic", canBeDisabled = true)
  val DYNAMIC_APPLY_NAMED = ArgusSyntaxClass("Call of applyDynamicNamed", "syntaxColouring.semantic.applyDynamicNamed", canBeDisabled = true)

  val scalaSyntacticCategory = Category("Syntactic", List(
    BRACKET, KEYWORD, RETURN, MULTI_LINE_STRING, OPERATOR, DEFAULT, STRING, CHARACTER, NUMBER_LITERAL, ESCAPE_SEQUENCE, SYMBOL))

  val scalaSemanticCategory = Category("Semantic", List(
    ANNOTATION, CASE_CLASS, CASE_OBJECT, CLASS, LAZY_LOCAL_VAL, LAZY_TEMPLATE_VAL,
    LOCAL_VAL, LOCAL_VAR, METHOD, OBJECT, PACKAGE, PARAM, TEMPLATE_VAL, TEMPLATE_VAR,
    TRAIT, TYPE, TYPE_PARAMETER, IDENTIFIER_IN_INTERPOLATED_STRING, CALL_BY_NAME_PARAMETER))

  val dynamicCategory = Category("Dynamic", List(
    DYNAMIC_SELECT, DYNAMIC_UPDATE, DYNAMIC_APPLY, DYNAMIC_APPLY_NAMED))

  val commentsCategory = Category("Comments", List(
    SINGLE_LINE_COMMENT, MULTI_LINE_COMMENT, SCALADOC, SCALADOC_CODE_BLOCK, SCALADOC_ANNOTATION, SCALADOC_MACRO, TASK_TAG))

  val xmlCategory = Category("XML", List(
    XML_ATTRIBUTE_NAME, XML_ATTRIBUTE_VALUE, XML_ATTRIBUTE_EQUALS, XML_CDATA_BORDER, XML_COMMENT, XML_TAG_DELIMITER,
    XML_TAG_NAME, XML_PI))

  val categories = List(scalaSyntacticCategory, scalaSemanticCategory, dynamicCategory, commentsCategory, xmlCategory)

  val ENABLE_SEMANTIC_HIGHLIGHTING = "syntaxColouring.semantic.enabled"

  val USE_SYNTACTIC_HINTS = "syntaxColouring.semantic.useSyntacticHints"

  val STRIKETHROUGH_DEPRECATED = "syntaxColouring.semantic.strikeDeprecated"

}

object ScalariformToSyntaxClass {

  import org.arguside.ui.syntax.{ ArgusSyntaxClasses => ssc }

  // TODO: Distinguish inside from outside of CDATA; distinguish XML tag and attribute name

  /**
   * If one wants to tokenize source code by Scalariform, one probably also needs to translate the
   * token to a format the UI-Classes of Eclipse can understand. If this the case than this method
   * should be used.
   *
   * Because Scalariform does not treat all token the way the IDE needs them, for some of them they
   * are replaced with a different kind of token.
   */
  def apply(token: Token): ArgusSyntaxClass = token.tokenType match {
    case LPAREN | RPAREN | LBRACE | RBRACE | LBRACKET | RBRACKET         => ssc.BRACKET
    case STRING_LITERAL                                                  => ssc.STRING
    case TRUE | FALSE | NULL                                             => ssc.KEYWORD
    case RETURN                                                          => ssc.RETURN
    case t if t.isKeyword                                                => ssc.KEYWORD
    case LINE_COMMENT                                                    => ssc.SINGLE_LINE_COMMENT
    case MULTILINE_COMMENT if token.isScalaDocComment                    => ssc.SCALADOC
    case MULTILINE_COMMENT                                               => ssc.MULTI_LINE_COMMENT
    case PLUS | MINUS | STAR | PIPE | TILDE | EXCLAMATION                => ssc.OPERATOR
    case DOT | COMMA | COLON | USCORE | EQUALS | SEMI | LARROW |
         ARROW | SUBTYPE | SUPERTYPE | VIEWBOUND | AT | HASH             => ssc.OPERATOR
    case VARID if Chars.isOperatorPart(token.text(0))                    => ssc.OPERATOR
    case FLOATING_POINT_LITERAL | INTEGER_LITERAL                        => ssc.NUMBER_LITERAL
    case SYMBOL_LITERAL                                                  => ssc.SYMBOL
    case XML_START_OPEN | XML_EMPTY_CLOSE | XML_TAG_CLOSE | XML_END_OPEN => ssc.XML_TAG_DELIMITER
    case XML_NAME                                                        => ssc.XML_TAG_NAME
    case XML_ATTR_EQ                                                     => ssc.XML_ATTRIBUTE_EQUALS
    case XML_PROCESSING_INSTRUCTION                                      => ssc.XML_PI
    case XML_COMMENT                                                     => ssc.XML_COMMENT
    case XML_ATTR_VALUE                                                  => ssc.XML_ATTRIBUTE_VALUE
    case XML_CDATA                                                       => ssc.XML_CDATA_BORDER
    case XML_UNPARSED | XML_WHITESPACE | XML_PCDATA | VARID | _          => ssc.DEFAULT
  }

}
