package org.arguside.debug.internal.command

import org.arguside.debug.internal.BaseDebuggerActor
import org.arguside.debug.internal.model.JdiRequestFactory
import org.arguside.debug.internal.model.ArgusDebugTarget
import org.arguside.debug.internal.model.ArgusStackFrame
import org.arguside.debug.internal.model.ArgusThread
import org.eclipse.debug.core.DebugEvent
import com.sun.jdi.event.StepEvent
import com.sun.jdi.request.StepRequest

object ArgusStepReturn {
  def apply(scalaStackFrame: ArgusStackFrame): ArgusStep = {
    val stepReturnRequest = JdiRequestFactory.createStepRequest(StepRequest.STEP_LINE, StepRequest.STEP_OUT, scalaStackFrame.thread)

    val companionActor = new ArgusStepReturnActor(scalaStackFrame.getDebugTarget, scalaStackFrame.thread, stepReturnRequest) {
      // TODO: when implementing support without filtering, need to workaround problem reported in Eclipse bug #38744
      override val scalaStep: ArgusStep = new ArgusStepImpl(this)
    }
    companionActor.start()

    companionActor.scalaStep
  }
}

/**
 * Actor used to manage a Scala step return. It keeps track of the request needed to perform this step.
 * This class is thread safe. Instances are not to be created outside of the ArgusStepReturn object.
 */
private[command] abstract class ArgusStepReturnActor(debugTarget: ArgusDebugTarget, thread: ArgusThread, stepReturnRequest: StepRequest) extends BaseDebuggerActor {

  private var enabled = false

  protected[command] def scalaStep: ArgusStep

  override protected def postStart(): Unit = link(thread.companionActor)

  override protected def behavior = {
    // JDI event triggered when a step has been performed
    case stepEvent: StepEvent =>
      reply {
        if (!debugTarget.cache.isTransparentLocation(stepEvent.location)) {
          terminate()
          thread.suspendedFromArgus(DebugEvent.STEP_RETURN)
          true
        }
        else false
      }
    case ArgusStep.Step => step()    // user step request
    case ArgusStep.Stop => terminate() // step is terminated
  }

  private def step(): Unit = {
    enable()
    thread.resumeFromArgus(scalaStep, DebugEvent.STEP_RETURN)
  }

  private def terminate(): Unit = {
    disable()
    poison()
  }

  private def enable(): Unit = {
    if (!enabled) {
      debugTarget.eventDispatcher.setActorFor(this, stepReturnRequest)
      stepReturnRequest.enable()
      enabled = true
    }
  }

  private def disable(): Unit = {
    if (enabled) {

      stepReturnRequest.disable()
      debugTarget.eventDispatcher.unsetActorFor(stepReturnRequest)
      debugTarget.virtualMachine.eventRequestManager.deleteEventRequest(stepReturnRequest)
      enabled = false
    }
  }

  override protected def preExit(): Unit = {
    unlink(thread.companionActor)
    disable()
  }
}