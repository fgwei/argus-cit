package org.arguside.extensions

/**
 * Base interface for all Argus IDE extensions.
 */
trait ArgusIdeExtension {

  /**
   * The setting information is used to describe the behavior of the IDE
   * extension.
   *
   * Describing the behavior means that users may see information about this
   * extension in the "Argus" preference page of Eclipse.
   */
  def setting: ExtensionSetting
}

object ExtensionSetting {
  import reflect.runtime.universe._

  def fullyQualifiedName[A : TypeTag]: String =
    typeOf[A].typeSymbol.fullName

  def simpleName[A : TypeTag]: String =
    typeOf[A].typeSymbol.name.toString()
}

trait ExtensionSetting