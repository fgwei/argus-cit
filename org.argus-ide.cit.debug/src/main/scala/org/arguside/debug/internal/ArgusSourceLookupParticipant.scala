package org.arguside.debug.internal

import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupParticipant
import org.arguside.debug.internal.model.ArgusStackFrame

/**
 * SourceLookupParticipant providing a source names when using the
 * Scala debugger
 */
object ArgusSourceLookupParticipant extends AbstractSourceLookupParticipant {

  def getSourceName(obj: AnyRef): String = {
    obj match {
      case stackFrame: ArgusStackFrame =>
        stackFrame.getSourcePath
      case _ =>
        null
    }
  }

}