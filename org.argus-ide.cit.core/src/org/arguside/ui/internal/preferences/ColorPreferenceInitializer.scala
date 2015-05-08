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
    if (!IArgusPlugin().headlessMode) {
      doInitializeDefaultPreferences()
    }
  }

  private def doInitializeDefaultPreferences() {
    val argusPrefStore = IArgusPlugin().getPreferenceStore()

    argusPrefStore.setDefault(ENABLE_SEMANTIC_HIGHLIGHTING, true)
    argusPrefStore.setDefault(USE_SYNTACTIC_HINTS, true)
    argusPrefStore.setDefault(STRIKETHROUGH_DEPRECATED, true)

    setDefaultsForSyntaxClasses(argusPrefStore)
  }

  private def setDefaultsForSyntaxClasses(implicit argusPrefStore: IPreferenceStore) {
    // Scala syntactic
    setDefaultsForSyntaxClass(SINGLE_LINE_COMMENT, new RGB(63, 127, 95))
    setDefaultsForSyntaxClass(MULTI_LINE_COMMENT, new RGB(63, 127, 95))
    setDefaultsForSyntaxClass(TASK_TAG, new RGB(127, 159, 191), bold = true)
    setDefaultsForSyntaxClass(KEYWORD, new RGB(127, 0, 85), bold = true)
    setDefaultsForSyntaxClass(STRING, new RGB(42, 0, 255))
    setDefaultsForSyntaxClass(CHARACTER, new RGB(42, 0, 255))
    setDefaultsForSyntaxClass(MULTI_LINE_STRING, new RGB(42, 0, 255))
    setDefaultsForSyntaxClass(DEFAULT, new RGB(0, 0, 0))
    setDefaultsForSyntaxClass(OPERATOR, new RGB(0, 0, 0))
    setDefaultsForSyntaxClass(BRACKET, new RGB(0, 0, 0))
    setDefaultsForSyntaxClass(RETURN, new RGB(127, 0, 85), bold = true)
    setDefaultsForSyntaxClass(BRACKET, new RGB(0, 0, 0))
    setDefaultsForSyntaxClass(NUMBER_LITERAL, new RGB(196, 140, 255))
    setDefaultsForSyntaxClass(ESCAPE_SEQUENCE, new RGB(196, 140, 255))

    // Scala semantic:
    setDefaultsForSyntaxClass(ANNOTATION, new RGB(222, 0, 172), enabled = true)
    setDefaultsForSyntaxClass(CLASS, new RGB(50, 147, 153), enabled = false)
    setDefaultsForSyntaxClass(LOCAL_VAR, new RGB(255, 94, 94), enabled = true)
    setDefaultsForSyntaxClass(METHOD, new RGB(76, 76, 76), italic = true, enabled = false)
    setDefaultsForSyntaxClass(PARAM, new RGB(100, 0, 103), enabled = false)
  }

  private def setDefaultsForSyntaxClass(
    syntaxClass: JawaSyntaxClass,
    foregroundRGB: RGB,
    enabled: Boolean = true,
    backgroundRGBOpt: Option[RGB] = None,
    bold: Boolean = false,
    italic: Boolean = false,
    strikethrough: Boolean = false,
    underline: Boolean = false)(implicit argusPrefStore: IPreferenceStore) =
    {
      lazy val WHITE = new RGB(255, 255, 255)
      argusPrefStore.setDefault(syntaxClass.enabledKey, enabled)
      argusPrefStore.setDefault(syntaxClass.foregroundColorKey, StringConverter.asString(foregroundRGB))
      val defaultBackgroundColor = StringConverter.asString(backgroundRGBOpt getOrElse WHITE)
      argusPrefStore.setDefault(syntaxClass.backgroundColorKey, defaultBackgroundColor)
      argusPrefStore.setDefault(syntaxClass.backgroundColorEnabledKey, backgroundRGBOpt.isDefined)
      argusPrefStore.setDefault(syntaxClass.boldKey, bold)
      argusPrefStore.setDefault(syntaxClass.italicKey, italic)
      argusPrefStore.setDefault(syntaxClass.underlineKey, underline)
    }

}
