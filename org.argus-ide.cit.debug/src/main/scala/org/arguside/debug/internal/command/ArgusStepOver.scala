package org.arguside.debug.internal.command

import scala.Option.option2Iterable
import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.mutable.ListBuffer
import org.arguside.debug.internal.JDIUtil.methodToLines
import org.arguside.debug.internal.model.ArgusThread
import org.arguside.debug.internal.model.ArgusStackFrame
import org.arguside.debug.internal.model.ArgusDebugTarget
import org.arguside.debug.internal.model.JdiRequestFactory
import org.arguside.debug.internal.model.ArgusDebugCache
import org.eclipse.debug.core.DebugEvent
import com.sun.jdi.event.StepEvent
import com.sun.jdi.event.ClassPrepareEvent
import com.sun.jdi.event.BreakpointEvent
import com.sun.jdi.request.StepRequest
import com.sun.jdi.request.EventRequest
import org.arguside.debug.internal.BaseDebuggerActor
import com.sun.jdi.ReferenceType

object ArgusStepOver {

  final val LINE_NUMBER_UNAVAILABLE = -1

  def apply(scalaStackFrame: ArgusStackFrame): ArgusStep = {

    import scala.collection.JavaConverters._

    val debugTarget = scalaStackFrame.getDebugTarget

    val location = scalaStackFrame.stackFrame.location

    val typeName = location.declaringType.name

    val stepOverRequest = JdiRequestFactory.createStepRequest(StepRequest.STEP_LINE, StepRequest.STEP_OVER, scalaStackFrame.thread)

    val requests = ListBuffer[EventRequest](stepOverRequest)

    val companionActor = if (location.lineNumber == LINE_NUMBER_UNAVAILABLE) {

      new ArgusStepOverActor(debugTarget, typeName, rangeOpt = None, scalaStackFrame.thread, requests) {
        override val scalaStep: ArgusStep = new ArgusStepImpl(this)
      }

    } else {

      // find anonFunction in range
      val currentMethodLastLine = methodToLines(location.method).max

      val range = Range(location.lineNumber, (location.method.declaringType.methods.asScala.flatten(methodToLines(_)).filter(_ > currentMethodLastLine) :+ Int.MaxValue).min)

      val nestedAnonFuncPrefix = if (typeName.last == '$') {
        typeName + "$anonfun$"
      } else {
        typeName + "$$anonfun$"
      }

      val loadedAnonFunctionsInRange = debugTarget.cache.getLoadedNestedTypes(typeName).filter(_.name().startsWith(nestedAnonFuncPrefix)).flatMap(debugTarget.cache.getAnonFunctionsInRange(_, range)).toBuffer

      // if we are in an anonymous function, add the method
      if (typeName.contains("$$anonfun$")) {
        loadedAnonFunctionsInRange ++= debugTarget.cache.getAnonFunction(location.declaringType)
      }

      requests ++= loadedAnonFunctionsInRange.map(JdiRequestFactory.createMethodEntryBreakpoint(_, scalaStackFrame.thread))

      new ArgusStepOverActor(debugTarget, typeName, Some(range), scalaStackFrame.thread, requests) {
        override val scalaStep: ArgusStep = new ArgusStepImpl(this)
      }
    }

    companionActor.start()
    companionActor.scalaStep
  }

}

/**
 * Actor used to manage a Scala step over. It keeps track of the request needed to perform this step.
 * This class is thread safe. Instances are not to be created outside of the ArgusStepOver object.
 */
private[command] abstract class ArgusStepOverActor(debugTarget: ArgusDebugTarget, typeName: String, rangeOpt: Option[Range], thread: ArgusThread, requests: ListBuffer[EventRequest]) extends BaseDebuggerActor {

  protected[command] def scalaStep: ArgusStep

  private var enabled = false

  override protected def postStart(): Unit = link(thread.companionActor)

  override protected def behavior = {
    // JDI event triggered when a class has been loaded
    case classPrepareEvent: ClassPrepareEvent =>
      for {
        range <- rangeOpt
        method <- debugTarget.cache.getAnonFunctionsInRange(classPrepareEvent.referenceType, range)
      } {
        val breakpoint = JdiRequestFactory.createMethodEntryBreakpoint(method, thread)
        requests += breakpoint
        debugTarget.eventDispatcher.setActorFor(this, breakpoint)
        breakpoint.enable()
      }
      reply(false)
    // JDI event triggered when a step has been performed
    case stepEvent: StepEvent =>
      reply(if (!debugTarget.cache.isTransparentLocation(stepEvent.location)) {
        terminate
        thread.suspendedFromArgus(DebugEvent.STEP_OVER)
        true
      } else {
        false
      })
    // JDI event triggered when a breakpoint is hit
    case breakpointEvent: BreakpointEvent =>
      terminate
      thread.suspendedFromArgus(DebugEvent.STEP_OVER)
      reply(true)
    // user step request
    case ArgusStep.Step =>
      step
    // step is terminated
    case ArgusStep.Stop =>
      terminate
  }

  private def step(): Unit = {
    enable()
    thread.resumeFromArgus(scalaStep, DebugEvent.STEP_OVER)
  }

  private def terminate(): Unit = {
    disable()
    poison()
  }

  private def enable(): Unit = {
    if (!enabled) {
      val eventDispatcher = debugTarget.eventDispatcher

      debugTarget.cache.addClassPrepareEventListener(this, typeName)
      requests.foreach {
        request =>
          eventDispatcher.setActorFor(this, request)
          request.enable()
      }
      enabled= true
    }
  }

  private def disable(): Unit = {
    if (enabled) {
      val eventDispatcher = debugTarget.eventDispatcher
      val eventRequestManager = debugTarget.virtualMachine.eventRequestManager

      for (request <- requests) {
        request.disable()
        eventDispatcher.unsetActorFor(request)
        eventRequestManager.deleteEventRequest(request)
      }
      debugTarget.cache.removeClassPrepareEventListener(this, typeName)
      enabled= false
    }
  }

  override protected def preExit(): Unit = {
    unlink(thread.companionActor)
    disable()
  }
}