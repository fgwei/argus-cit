package org.arguside.ui.internal.preferences

import org.arguside.core.IArgusPlugin
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.resource.StringConverter
import org.eclipse.swt.graphics.RGB
import org.arguside.ui.syntax.JawaSyntaxClasses._
import org.arguside.ui.syntax.JawaSyntaxClass

class ColorPreferenceInitializer extends AbstractPreferenceInitializer {

  override def initializeDefaultPreferences() {
    doInitializeDefaultPreferences()
  }

  private def doInitializeDefaultPreferences() {
    val jawaPrefStore = IArgusPlugin().getPreferenceStore()

    jawaPrefStore.setDefault(ENABLE_SEMANTIC_HIGHLIGHTING, true)
    jawaPrefStore.setDefault(USE_SYNTACTIC_HINTS, true)

    setDefaultsForSyntaxClasses(jawaPrefStore)
  }

  private def setDefaultsForSyntaxClasses(implicit scalaPrefStore: IPreferenceStore) {
    // Jawa syntactic
    setDefaultsForSyntaxClass(SINGLE_LINE_COMMENT, new RGB(63, 127, 95))
    setDefaultsForSyntaxClass(MULTI_LINE_COMMENT, new RGB(63, 127, 95))
    setDefaultsForSyntaxClass(DOC_COMMENT, new RGB(63, 127, 95))
    setDefaultsForSyntaxClass(TASK_TAG, new RGB(127, 159, 191), bold = true)
    setDefaultsForSyntaxClass(KEYWORD, new RGB(127, 0, 85), bold = true)
    setDefaultsForSyntaxClass(STRING, new RGB(42, 0, 255))
    setDefaultsForSyntaxClass(CHARACTER, new RGB(42, 0, 255))
    setDefaultsForSyntaxClass(MULTI_LINE_STRING, new RGB(42, 0, 255))
    setDefaultsForSyntaxClass(DEFAULT, new RGB(0, 0, 0))
    setDefaultsForSyntaxClass(OPERATOR, new RGB(0, 0, 0))
    setDefaultsForSyntaxClass(BRACKET, new RGB(0, 0, 0))
    setDefaultsForSyntaxClass(RETURN, new RGB(127, 0, 85), bold = true)
    setDefaultsForSyntaxClass(NUMBER_LITERAL, new RGB(196, 140, 255))
    setDefaultsForSyntaxClass(LID, new RGB(63, 127, 95), italic = true)
    setDefaultsForSyntaxClass(ESCAPE_SEQUENCE, new RGB(196, 140, 255))

    // Jawa semantic:
    setDefaultsForSyntaxClass(ANNOTATION, new RGB(222, 0, 172), enabled = true)
    setDefaultsForSyntaxClass(CLASS, new RGB(50, 147, 153), enabled = false)
    setDefaultsForSyntaxClass(LOCAL_VAR, new RGB(94, 94, 255), enabled = true)
    setDefaultsForSyntaxClass(METHOD, new RGB(76, 76, 76), italic = true, enabled = false)
    setDefaultsForSyntaxClass(LOCATION, new RGB(63, 127, 95), italic = true, enabled = true)

  }

  private def setDefaultsForSyntaxClass(
    syntaxClass: JawaSyntaxClass,
    foregroundRGB: RGB,
    enabled: Boolean = true,
    backgroundRGBOpt: Option[RGB] = None,
    bold: Boolean = false,
    italic: Boolean = false,
    underline: Boolean = false)(implicit jawaPrefStore: IPreferenceStore) =
    {
      lazy val WHITE = new RGB(255, 255, 255)
      jawaPrefStore.setDefault(syntaxClass.enabledKey, enabled)
      jawaPrefStore.setDefault(syntaxClass.foregroundColorKey, StringConverter.asString(foregroundRGB))
      val defaultBackgroundColor = StringConverter.asString(backgroundRGBOpt getOrElse WHITE)
      jawaPrefStore.setDefault(syntaxClass.backgroundColorKey, defaultBackgroundColor)
      jawaPrefStore.setDefault(syntaxClass.backgroundColorEnabledKey, backgroundRGBOpt.isDefined)
      jawaPrefStore.setDefault(syntaxClass.boldKey, bold)
      jawaPrefStore.setDefault(syntaxClass.italicKey, italic)
      jawaPrefStore.setDefault(syntaxClass.underlineKey, underline)
    }

}
