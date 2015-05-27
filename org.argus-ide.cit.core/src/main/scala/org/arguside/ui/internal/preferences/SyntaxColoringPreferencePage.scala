package org.arguside.ui.internal.preferences

import org.eclipse.jdt.internal.ui.preferences.OverlayPreferenceStore.BOOLEAN
import org.eclipse.jdt.internal.ui.preferences.OverlayPreferenceStore.OverlayKey
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.arguside.core.IArgusPlugin
import org.arguside.ui.syntax.JawaSyntaxClasses
import org.arguside.ui.syntax.JawaSyntaxClasses._
import org.arguside.ui.syntax.preferences.BaseSyntaxColoringPreferencePage
import org.arguside.util.eclipse.SWTUtils._
import org.arguside.core.internal.decorators.semantichighlighting.Position
import org.arguside.core.internal.decorators.semantichighlighting.classifier.SymbolTypes
import org.sireum.jawa.sjc.lexer.JawaLexer
import org.sireum.jawa.sjc.DefaultReporter

/** Syntax Coloring preference page for the Jawa editors.
 */
class SyntaxColoringPreferencePage extends BaseSyntaxColoringPreferencePage(
  JawaSyntaxClasses.categories,
  jawaSyntacticCategory,
  IArgusPlugin().getPreferenceStore,
  SyntaxColoringPreferencePage.PreviewText,
  SemanticPreviewerFactoryConfiguration) {

  private var enableSemanticHighlightingCheckBox: Button = _
  private var extraAccuracyCheckBox: Button = _

  override def additionalOverlayKeys: List[OverlayKey] = List(
    new OverlayKey(BOOLEAN, ENABLE_SEMANTIC_HIGHLIGHTING),
    new OverlayKey(BOOLEAN, USE_SYNTACTIC_HINTS))

  override def additionalPerformDefaults() {
    enableSemanticHighlightingCheckBox.setSelection(overlayStore getBoolean ENABLE_SEMANTIC_HIGHLIGHTING)
    extraAccuracyCheckBox.setEnabled(enableSemanticHighlightingCheckBox.getSelection)
    extraAccuracyCheckBox.setSelection(overlayStore getBoolean USE_SYNTACTIC_HINTS)
  }

  override def additionalCreateContent(parent: Composite) {
    enableSemanticHighlightingCheckBox = new Button(parent, SWT.CHECK)
    enableSemanticHighlightingCheckBox.setText("Enable semantic highlighting")
    enableSemanticHighlightingCheckBox.setLayoutData(gridData(horizontalSpan = 2))
    enableSemanticHighlightingCheckBox.setSelection(overlayStore.getBoolean(ENABLE_SEMANTIC_HIGHLIGHTING))

    extraAccuracyCheckBox = new Button(parent, SWT.CHECK)
    extraAccuracyCheckBox.setText("Use slower but more accurate semantic highlighting")
    extraAccuracyCheckBox.setLayoutData(gridData(horizontalSpan = 2))
    extraAccuracyCheckBox.setSelection(overlayStore.getBoolean(USE_SYNTACTIC_HINTS))
    extraAccuracyCheckBox.setEnabled(enableSemanticHighlightingCheckBox.getSelection)

    setUpSelectionListener
  }

  private def setUpSelectionListener() = {
    enableSemanticHighlightingCheckBox.addSelectionListener { () =>
      overlayStore.setValue(ENABLE_SEMANTIC_HIGHLIGHTING, enableSemanticHighlightingCheckBox.getSelection)
      extraAccuracyCheckBox.setEnabled(enableSemanticHighlightingCheckBox.getSelection)
      handleSyntaxColorListSelection()
    }
    extraAccuracyCheckBox.addSelectionListener { () =>
      overlayStore.setValue(USE_SYNTACTIC_HINTS, extraAccuracyCheckBox.getSelection)
    }

  }
}

object SyntaxColoringPreferencePage {

  val PreviewText =
    ("""/**
        | * Jawadoc
        | */
        |record `a.a.aa`  @type class @AccessFlag PUBLIC_ABSTRACT  {
        |  `a.a.ab` `a.a.aa.a`    @AccessFlag PRIVATE;
        |}
        |global `java.lang.String`[] `@@a.a.aa.z`    @AccessFlag PRIVATE_STATIC_FINAL;
        |procedure `void` `a.a.aa.b` (`a.a.aa` v1 @type `this`, `a.a.ab` v2 @type `object`) @owner `a.a.aa` @signature `La/a/aa;.b:(La/a/ab;)V` @Access `PRIVATE_DECLARED_SYNCHRONIZED` {
        |  temp ;
        |  v0;
        |
        |  #L00e375.   v0 := "asdf";
        |  #L00e37c.   `@@a.a.aa.z` := 0;
        |  #L00e37e.   v1.`a.a.aa.a`  := v2 @type `object`;
        |  #L00e382.   call temp:=  `a.a.aa.c`(v1) @signature `La/a/aa;.c:()Z` @classDescriptor `a.a.aa` @type direct;
        |  #L00e384.   return @void ;
        |  #L00e386.   v0:= Exception  @type `object`;
        |  #L00e38a.   throw v0;
        |    catch  `any` @[L00e37e..L00e382] goto L00e386;
        |}
        |""").stripMargin

  case class ColoringInfo(symbolType: SymbolTypes.SymbolType)

  import SymbolTypes._
  private val identifierToSyntaxClass: Map[String, ColoringInfo] = Map(
    "a.a.aa" -> ColoringInfo(Class),
    "type" -> ColoringInfo(Annotation),
    "AccessFlag" -> ColoringInfo(Annotation),
    "owner" -> ColoringInfo(Annotation),
    "signature" -> ColoringInfo(Annotation),
    "classDescriptor" -> ColoringInfo(Annotation),
    "Access" -> ColoringInfo(Annotation),
    "a.a.ab" -> ColoringInfo(Class),
    "java.lang.String" -> ColoringInfo(Class),
    "a.a.aa.b" -> ColoringInfo(Method),
    "temp" -> ColoringInfo(LocalVar),
    "v0" -> ColoringInfo(LocalVar),
    "v1" -> ColoringInfo(LocalVar),
    "v2" -> ColoringInfo(LocalVar),
    "a.a.aa.c" -> ColoringInfo(Method),
    "L00e37e" -> ColoringInfo(Location),
    "L00e382" -> ColoringInfo(Location),
    "L00e386" -> ColoringInfo(Location)
    )

  val semanticLocations: List[Position] =
    for {
      token <- JawaLexer.rawTokenise(Left(PreviewText), new DefaultReporter)
      if token.tokenType.isId
      ColoringInfo(symbolType) <- identifierToSyntaxClass get token.text
    } yield new Position(token.offset, token.length, symbolType)
}
