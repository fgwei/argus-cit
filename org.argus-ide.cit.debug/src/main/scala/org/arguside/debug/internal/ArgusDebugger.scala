package org.arguside.debug.internal

import org.arguside.core.IArgusPlugin

import org.eclipse.debug.core.model.IDebugModelProvider
import org.eclipse.debug.internal.ui.contexts.DebugContextManager
import org.eclipse.debug.ui.contexts.DebugContextEvent
import org.eclipse.debug.ui.contexts.IDebugContextListener
import org.eclipse.jface.viewers.ISelection
import org.eclipse.jface.viewers.IStructuredSelection
import org.arguside.core.IArgusPlugin

import com.sun.jdi.StackFrame

import model.ArgusStackFrame
import model.ArgusThread

object ArgusDebugger {

  val classIDebugModelProvider = classOf[IDebugModelProvider]

  val modelProvider = new IDebugModelProvider {
    def getModelIdentifiers() = {
      Array(ArgusDebugPlugin.id)
    }
  }

  @volatile private var _currentThread: ArgusThread = null
  @volatile private var _currentStackFrame: ArgusStackFrame = null
  @volatile private var _currentFrameIndex: Int = 0

  /**
   * Currently selected thread & stack frame in the debugger UI view.
   *
   * WARNING:
   * Mind that this code is by design subject to race-condition, clients accessing these members need to handle the case where the
   * values of `currentThread` & `currentStackFrame` are not the expected ones. Practically, this means that accesses to these members
   * should always happen within a try..catch block. Failing to do so can cause the whole debug session to shutdown for no good reasons.
   */
  def currentThread: ArgusThread = _currentThread
  def currentStackFrame: ArgusStackFrame = _currentStackFrame
  def currentFrame(): Option[StackFrame] = Option(currentThread).map(_.threadRef.frame(_currentFrameIndex))

  private[debug] def updateCurrentThread(selection: ISelection): Unit = {
    def setValues(thread: ArgusThread, frame: ArgusStackFrame, frameIndex: Int = 0): Unit = {
      _currentThread = thread
      _currentStackFrame = frame
      _currentFrameIndex = frameIndex
    }

    selection match {
      case structuredSelection: IStructuredSelection =>
        structuredSelection.getFirstElement match {
          case scalaThread: ArgusThread =>
            setValues(thread = scalaThread, frame = scalaThread.getTopStackFrame, frameIndex = 0)
          case scalaStackFrame: ArgusStackFrame =>
            setValues(thread = scalaStackFrame.thread, frame = scalaStackFrame, frameIndex = scalaStackFrame.index)
          case _ =>
            setValues(thread = null, frame = null)
        }
      case _ =>
        setValues(thread = null, frame = null)
    }
  }

  def init(): Unit = {
    if (!IArgusPlugin().headlessMode) {
      ArgusDebuggerContextListener.register()
    }
  }

  /**
   * `IDebugContextListener` is part of the Eclipse UI code, by extending it in a different
   *  object, it will not be loaded as soon as `ArgusDebugger` is used.
   *  This allow to use `ArgusDebugger` even if the application is launched in `headless` mode, like while running tests.
   */
  private object ArgusDebuggerContextListener extends IDebugContextListener {

    def register(): Unit = {
      DebugContextManager.getDefault().addDebugContextListener(this)
    }

    override def debugContextChanged(event: DebugContextEvent): Unit = {
      ArgusDebugger.updateCurrentThread(event.getContext())
    }
  }

}
