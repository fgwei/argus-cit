package org.arguside.core.internal.logging

import org.eclipse.jface.util.PropertyChangeEvent
import org.arguside.core.internal.ArgusPlugin
import org.arguside.core.internal.logging.log4j.Log4JFacade
import org.arguside.core.internal.logging.LoggingPreferenceConstants._
import org.arguside.util.eclipse.SWTUtils
import org.arguside.logging.HasLogger
import org.arguside.logging.Level

object LogManager extends Log4JFacade with HasLogger {

  private def updateLogLevel(event: PropertyChangeEvent): Unit = {
    if (event.getProperty == LogLevel) {
      val level = event.getNewValue.asInstanceOf[String]
      setLogLevel(Level.withName(level))
    }
  }

  private def updateConsoleAppenderStatus(event: PropertyChangeEvent): Unit = {
    if (event.getProperty == IsConsoleAppenderEnabled) {
      val enable = event.getNewValue.asInstanceOf[Boolean]
      withoutConsoleRedirects {
        updateConsoleAppender(enable)
      }
    }
  }

  private def updateStdRedirectStatus(event: PropertyChangeEvent): Unit = {
    if (event.getProperty == RedirectStdErrOut) {
      val enable = event.getNewValue.asInstanceOf[Boolean]
      if (enable) redirectStdOutAndStdErr()
      else disableRedirectStdOutAndStdErr()

      // we need to restart the presentation compilers so that
      // the std out/err streams are refreshed by Console.in/out
      if (enable != event.getOldValue.asInstanceOf[Boolean])
        ArgusPlugin().resetAllPresentationCompilers()
    }
  }

  override protected def logFileName = "argus-ide.log"

  override def configure(logOutputLocation: String, preferredLogLevel: Level.Value) {
    import SWTUtils.fnToPropertyChangeListener

    super.configure(logOutputLocation, preferredLogLevel)

    val prefStore = ArgusPlugin().getPreferenceStore
    prefStore.addPropertyChangeListener(updateLogLevel _)
    prefStore.addPropertyChangeListener(updateConsoleAppenderStatus _)
    prefStore.addPropertyChangeListener(updateStdRedirectStatus _)

    if (prefStore.getBoolean(RedirectStdErrOut)) {
      redirectStdOutAndStdErr()
      ArgusPlugin().resetAllPresentationCompilers()
    }
  }

  override protected def setLogLevel(level: Level.Value) {
    super.setLogLevel(level)
    logger.info("Log level is `%s`".format(level))
  }

  override def currentLogLevel: Level.Value = {
    val levelName = ArgusPlugin().getPreferenceStore.getString(LogLevel)
    if (levelName.isEmpty) defaultLogLevel
    else Level.withName(levelName)
  }

  def defaultLogLevel: Level.Value = Level.WARN

  override def isConsoleAppenderEnabled: Boolean =
    ArgusPlugin().getPreferenceStore.getBoolean(IsConsoleAppenderEnabled)

  private def withoutConsoleRedirects(f: => Unit) {
    try {
      disableRedirectStdOutAndStdErr()
      f
    }
    finally { redirectStdOutAndStdErr() }
  }

  private def redirectStdOutAndStdErr() {
    StreamRedirect.redirectStdOutput()
    StreamRedirect.redirectStdError()
  }

  private def disableRedirectStdOutAndStdErr() {
    StreamRedirect.disableRedirectStdOutput()
    StreamRedirect.disableRedirectStdError()
  }
}
