package org.arguside.logging

import org.arguside.core.internal.logging.LogManager
import org.arguside.core.internal.logging.EclipseLogger

// ZW: where are these log message going...
/**
 * Classes can mix this trait for having access to both the "default" {{{logger}}}
 * and the {{{eclipseLog}}}. The {{{eclipseLog}}} is a handle on the
 * {{{org.eclipse.core.runtime.Plugin.getLog}} instance.
 *
 * Clients can inject different loggers if needed.
 */
trait HasLogger {
  /** The Argus IDE logger.
   *  To use to log messages in the Argus IDE log.
   */
  protected[this] lazy val logger: Logger = {
    val clazz = this.getClass
    LogManager.getLogger(clazz)
  }

  /** The Eclipse platform logger.
   *  To use to log messages in the platform log.
   */
  protected[this] def eclipseLog: Logger = EclipseLogger
}
