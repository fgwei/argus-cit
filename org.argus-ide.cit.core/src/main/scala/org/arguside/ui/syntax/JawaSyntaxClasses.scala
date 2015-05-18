package org.arguside.ui.syntax

import JawaSyntaxClass.Category
import org.sireum.jawa.sjc.lexer.Token
import org.sireum.jawa.sjc.lexer.Tokens._

object JawaSyntaxClasses {

  val SINGLE_LINE_COMMENT = JawaSyntaxClass("Single-line comment", "syntaxColouring.singleLineComment")
  val MULTI_LINE_COMMENT = JawaSyntaxClass("Multi-line comment", "syntaxColouring.multiLineComment")
  val DOC_COMMENT = JawaSyntaxClass("Doc comment", "syntaxColouring.docComment")
  val TASK_TAG = JawaSyntaxClass("Task Tag", "syntaxColouring.taskTag")
  val OPERATOR = JawaSyntaxClass("Operator", "syntaxColouring.operator")
  val KEYWORD = JawaSyntaxClass("Keywords (excluding 'return')", "syntaxColouring.keyword")
  val RETURN = JawaSyntaxClass("Keyword 'return'", "syntaxColouring.return")
  val STRING = JawaSyntaxClass("Strings", "syntaxColouring.string")
  val CHARACTER = JawaSyntaxClass("Characters", "syntaxColouring.character")
  val MULTI_LINE_STRING = JawaSyntaxClass("Multi-line string", "syntaxColouring.multiLineString")
  val BRACKET = JawaSyntaxClass("Brackets", "syntaxColouring.bracket")
  val DEFAULT = JawaSyntaxClass("Others", "syntaxColouring.default")
  val LID = JawaSyntaxClass("LocationID", "syntaxColouring.lid")
  val NUMBER_LITERAL = JawaSyntaxClass("Number literals", "syntaxColouring.numberLiteral")
  val ESCAPE_SEQUENCE = JawaSyntaxClass("Escape sequences", "syntaxColouring.escapeSequence")

  val ANNOTATION = JawaSyntaxClass("Annotation", "syntaxColouring.semantic.annotation", canBeDisabled = true)
  val CLASS = JawaSyntaxClass("Class", "syntaxColouring.semantic.class", canBeDisabled = true)
  val LOCAL_VAR = JawaSyntaxClass("Local var", "syntaxColouring.semantic.localVar", canBeDisabled = true)
  val METHOD = JawaSyntaxClass("Method", "syntaxColouring.semantic.method", canBeDisabled = true)
  val LOCATION = JawaSyntaxClass("Location", "syntaxColouring.semantic.location", canBeDisabled = true)

  val jawaSyntacticCategory = Category("Syntactic", List(
    BRACKET, KEYWORD, RETURN, MULTI_LINE_STRING, OPERATOR, DEFAULT, STRING, CHARACTER, NUMBER_LITERAL, LID, ESCAPE_SEQUENCE))

  val jawaSemanticCategory = Category("Semantic", List(
    ANNOTATION, CLASS, LOCAL_VAR, METHOD, LOCATION))
    
  val commentsCategory = Category("Comments", List(
    SINGLE_LINE_COMMENT, MULTI_LINE_COMMENT, DOC_COMMENT, TASK_TAG))

  val categories = List(jawaSyntacticCategory, jawaSemanticCategory, commentsCategory)

  val ENABLE_SEMANTIC_HIGHLIGHTING = "syntaxColouring.semantic.enabled"

  val USE_SYNTACTIC_HINTS = "syntaxColouring.semantic.useSyntacticHints"
}

object JawaTokenToSyntaxClass {

  import org.arguside.ui.syntax.{ JawaSyntaxClasses => ssc }

  def apply(token: Token): JawaSyntaxClass = token.tokenType match {
    case LPAREN | RPAREN | LBRACE | RBRACE | LBRACKET | RBRACKET         => ssc.BRACKET
    case STRING_LITERAL                                                  => ssc.STRING
    case CHARACTER_LITERAL                                               => ssc.CHARACTER
    case TRUE | FALSE | NULL                                             => ssc.KEYWORD
    case RETURN                                                          => ssc.RETURN
    case LOCATION_ID                                                     => ssc.LID
    case t if t.isKeyword                                                => ssc.KEYWORD
    case LINE_COMMENT                                                    => ssc.SINGLE_LINE_COMMENT
    case MULTILINE_COMMENT                                               => ssc.MULTI_LINE_COMMENT
    case DOC_COMMENT                                                     => ssc.DOC_COMMENT
    case OP | ASSIGN_OP                                                  => ssc.OPERATOR
    case COLON | EQUALS | ARROW | RANGE                                  => ssc.OPERATOR
    case FLOATING_POINT_LITERAL | INTEGER_LITERAL                        => ssc.NUMBER_LITERAL
    case STATIC_ID | ID | AT | DOT | COMMA | SEMI | _                    => ssc.DEFAULT
  }
}
