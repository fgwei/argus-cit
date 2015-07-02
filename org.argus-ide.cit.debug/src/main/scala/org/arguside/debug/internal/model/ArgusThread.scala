package org.arguside.debug.internal.model

import com.sun.jdi.ClassType
import com.sun.jdi.IncompatibleThreadStateException
import com.sun.jdi.Method
import com.sun.jdi.ObjectCollectedException
import com.sun.jdi.ObjectReference
import com.sun.jdi.ThreadReference
import com.sun.jdi.Value
import com.sun.jdi.VMCannotBeModifiedException
import com.sun.jdi.VMDisconnectedException
import org.eclipse.debug.core.DebugEvent
import org.eclipse.debug.core.model.IThread
import org.eclipse.debug.core.model.IBreakpoint
import org.eclipse.debug.core.model.IStackFrame
import org.eclipse.jdt.internal.debug.core.model.JDIStackFrame
import org.arguside.debug.internal.BaseDebuggerActor
import org.arguside.debug.internal.JDIUtil._
import org.arguside.debug.internal.command.ArgusStepOver
import org.arguside.debug.internal.command.ArgusStep
import org.arguside.debug.internal.command.ArgusStepInto
import org.arguside.debug.internal.command.ArgusStepReturn
import org.arguside.debug.internal.preferences.HotCodeReplacePreferences
import org.arguside.logging.HasLogger
import scala.actors.Future
import scala.collection.JavaConverters.asScalaBufferConverter

class ThreadNotSuspendedException extends Exception

object ArgusThread {
  def apply(target: ArgusDebugTarget, thread: ThreadReference): ArgusThread = {
    val scalaThread = new ArgusThread(target, thread) {
      override val companionActor = ArgusThreadActor(this)
    }
    scalaThread.fireCreationEvent()
    scalaThread
  }
}

/**
 * A thread in the Argus debug model.
 * This class is thread safe. Instances have be created through its companion object.
 */
abstract class ArgusThread private(target: ArgusDebugTarget, val threadRef: ThreadReference)
  extends ArgusDebugElement(target) with IThread with HasLogger {
  import ArgusThreadActor._
  import BaseDebuggerActor._

  // Members declared in org.eclipse.debug.core.model.IStep

  override def canStepInto: Boolean = canStep
  override def canStepOver: Boolean = canStep
  override def canStepReturn: Boolean = canStep
  override def isStepping: Boolean = ???
  private def canStep = suspended && !target.isPerformingHotCodeReplace

  override def stepInto(): Unit = stepIntoFrame(stackFrames.head)
  override def stepOver(): Unit = {
    wrapJDIException("Exception while performing `step over`") { ArgusStepOver(stackFrames.head).step() }
  }
  override def stepReturn(): Unit = {
    wrapJDIException("Exception while performing `step return`") { ArgusStepReturn(stackFrames.head).step() }
  }

  // Members declared in org.eclipse.debug.core.model.ISuspendResume

  override def canResume: Boolean = suspended && !target.isPerformingHotCodeReplace
  override def canSuspend: Boolean = !suspended // TODO: need real logic
  override def isSuspended: Boolean = util.Try(threadRef.isSuspended).getOrElse(false)

  override def resume(): Unit = resumeFromArgus(DebugEvent.CLIENT_REQUEST)
  override def suspend(): Unit = {
    (safeThreadCalls(()) or wrapJDIException("Exception while retrieving suspending stack frame")) {
      threadRef.suspend()
      suspendedFromArgus(DebugEvent.CLIENT_REQUEST)
    }
  }

  // Members declared in org.eclipse.debug.core.model.IThread

  override def getBreakpoints: Array[IBreakpoint] = Array.empty // TODO: need real logic

  override def getName: String = {
    (safeThreadCalls("Error retrieving name") or wrapJDIException("Exception while retrieving stack frame's name")){
      name = threadRef.name
      name
    }
  }

  override def getPriority: Int = ???
  override def getStackFrames: Array[IStackFrame] = stackFrames.toArray
  final def getArgusStackFrames: List[ArgusStackFrame] = stackFrames
  override def getTopStackFrame: ArgusStackFrame = stackFrames.headOption.getOrElse(null)
  override def hasStackFrames: Boolean = !stackFrames.isEmpty

  // ----

  // state
  @volatile
  private var suspended = false

  /**
   * The current list of stack frames.
   * THE VALUE IS MODIFIED ONLY BY THE COMPANION ACTOR, USING METHODS DEFINED LOWER.
   */
  @volatile
  private var stackFrames: List[ArgusStackFrame] = Nil

  // keep the last known name around, for when the vm is not available anymore
  @volatile
  private var name: String = null

  protected[debug] val companionActor: BaseDebuggerActor

  val isSystemThread: Boolean = {
    safeThreadCalls(false) { Option(threadRef.threadGroup).exists(_.name == "system") }
  }

  def suspendedFromArgus(eventDetail: Int): Unit = companionActor ! SuspendedFromArgus(eventDetail)

  def resumeFromArgus(eventDetail: Int): Unit = companionActor ! ResumeFromArgus(None, eventDetail)

  def resumeFromArgus(step: ArgusStep, eventDetail: Int): Unit = companionActor ! ResumeFromArgus(Some(step), eventDetail)

  def terminatedFromArgus(): Unit = dispose()

  /** Invoke the given method on the given instance with the given arguments.
   *
   *  This method should not be called directly.
   *  Use [[ArgusObjectReference.invokeMethod(String, ArgusThread, ArgusValue*)]]
   *  or [[ArgusObjectReference.invokeMethod(String, String, ArgusThread, ArgusValue*)]] instead.
   */
  def invokeMethod(objectReference: ObjectReference, method: Method, args: Value*): Value = {
    processMethodInvocationResult(syncSend(companionActor, InvokeMethod(objectReference, method, args.toList)))
  }

  /** Invoke the given static method on the given type with the given arguments.
   *
   *  This method should not be called directly.
   *  Use [[ArgusClassType.invokeMethod(String, ArgusThread,ArgusValue*)]] instead.
   */
  def invokeStaticMethod(classType: ClassType, method: Method, args: Value*): Value = {
    processMethodInvocationResult(syncSend(companionActor, InvokeStaticMethod(classType, method, args.toList)))
  }

  private def stepIntoFrame(stackFrame: => ArgusStackFrame): Unit =
    wrapJDIException("Exception while performing `step into`") { ArgusStepInto(stackFrame).step() }

  /**
   * It's not possible to drop the bottom stack frame. Moreover all dropped frames and also
   * the one below the target frame can't be native.
   *
   * @param frame frame which we'd like to drop and step into it once again
   * @param relatedToHcr when dropping frames automatically after Hot Code Replace we need a bit different processing
   */
  private[model] def canDropToFrame(frame: ArgusStackFrame, relatedToHcr: Boolean = false): Boolean = {
    val frames = stackFrames
    val indexOfFrame = frames.indexOf(frame)

    val atLeastLastButOne = frames.size >= indexOfFrame + 2
    val canDropObsoleteFrames = HotCodeReplacePreferences.allowToDropObsoleteFramesManually

    // Obsolete frames are marked as native so we have to ignore isNative when user really wants to drop obsolete frames manually.
    // User has to be aware that it can cause problems when he'd really try to drop the native frame marked as obsolete.
    def isNativeAndIsNotObsoleteWhenObsoleteAllowed(f: ArgusStackFrame) = f.isNative && !(canDropObsoleteFrames && f.isObsolete)

    def notNative = !frames.take(indexOfFrame + 2).exists(isNativeAndIsNotObsoleteWhenObsoleteAllowed)
    canPopFrames && atLeastLastButOne && (relatedToHcr || (!target.isPerformingHotCodeReplace && notNative))
  }

  private[model] def canPopFrames: Boolean = isSuspended && target.canPopFrames

  private[model] def dropToFrame(frame: ArgusStackFrame): Unit = companionActor ! DropToFrame(frame)

  /**
   * Removes all top stack frames starting from a given one and performs StepInto to reach the given frame again.
   * FOR THE COMPANION ACTOR ONLY.
   */
  private[model] def dropToFrameInternal(frame: ArgusStackFrame, relatedToHcr: Boolean = false): Unit =
    (safeThreadCalls(()) or wrapJDIException("Exception while performing Drop To Frame")) {
      if (canDropToFrame(frame, relatedToHcr)) {
        val frames = stackFrames
        val startFrameForStepInto = frames(frames.indexOf(frame) + 1)
        threadRef.popFrames(frame.stackFrame)
        stepIntoFrame(startFrameForStepInto)
      }
    }

  /**
   * @param shouldFireChangeEvent fire an event after refreshing frames to refresh also UI elements
   */
  def refreshStackFrames(shouldFireChangeEvent: Boolean): Unit =
    companionActor ! RebindStackFrames(shouldFireChangeEvent)

  private[internal] def updateStackFramesAfterHcr(msg: ArgusDebugTarget.UpdateStackFramesAfterHcr): Unit = companionActor ! msg

  private def processMethodInvocationResult(res: Option[Any]): Value = res match {
    case Some(Right(null)) =>
      null
    case Some(Right(res: Value)) =>
      res
    case Some(Left(e: Exception)) =>
      throw e
    case None =>
      null
    case Some(v) =>
      // to make the match exhaustive. Should not happen.
      logger.error(s"Not recognized method invocation result: $v")
      null
  }

  /**
   * release all resources
   */
  def dispose(): Unit = {
    stackFrames = Nil
    companionActor ! TerminatedFromArgus
  }

  /*
   * Methods used by the companion actor to update this object internal states
   * FOR THE COMPANION ACTOR ONLY.
   */

  /**
   * Set the this object internal states to suspended.
   * FOR THE COMPANION ACTOR ONLY.
   */
  private[model] def suspend(eventDetail: Int) = {
    (safeThreadCalls(()) or wrapJDIException("Exception while suspending thread")) {
      // FIXME: `threadRef.frames` should handle checked exception `IncompatibleThreadStateException`
      stackFrames = threadRef.frames.asScala.zipWithIndex.map { case (frame, index) =>
        ArgusStackFrame(this, frame, index)
      }(collection.breakOut)
      suspended = true
      fireSuspendEvent(eventDetail)
    }
  }

  /**
   * Set the this object internal states to resumed.
   * FOR THE COMPANION ACTOR ONLY.
   */
  private[model] def resume(eventDetail: Int): Unit = {
    suspended = false
    stackFrames = Nil
    fireResumeEvent(eventDetail)
  }

  /**
   * Rebind the Argus stack frame to the new underlying frames.
   * TO BE USED ONLY IF THE NUMBER OF FRAMES MATCHES
   * FOR THE COMPANION ACTOR ONLY.
   */
  private[model] def rebindArgusStackFrames(): Unit = (safeThreadCalls(()) or wrapJDIException("Exception while rebinding stack frames")) {
    rebindFrames()
  }

  private def rebindFrames(): Unit = {
    // FIXME: Should check that `threadRef.frames == stackFrames` before zipping
    threadRef.frames.asScala.zip(stackFrames).foreach {
      case (jdiStackFrame, argusStackFrame) => argusStackFrame.rebind(jdiStackFrame)
    }
  }

  /**
   * Refreshes frames and optionally drops affected ones.
   * FOR THE COMPANION ACTOR ONLY.
   */
  private[model] def updateArgusStackFramesAfterHcr(dropAffectedFrames: Boolean): Unit =
    (safeThreadCalls(()) or wrapJDIException("Exception while rebinding stack frames")) {
      // obsolete frames will be marked as native so we need to check this before we'll rebind frames
      val nativeFrameIndex = stackFrames.indexWhere(_.isNative)

      rebindFrames()
      if (dropAffectedFrames) {
        val topNonNativeFrames =
          if (nativeFrameIndex == -1) stackFrames
          else stackFrames.take(nativeFrameIndex - 1) // we can't drop to native frame and also the first older frame can't be native
        val obsoleteFrames = topNonNativeFrames.filter(_.isObsolete)
        for (frame <- obsoleteFrames.lastOption)
          dropToFrameInternal(frame, relatedToHcr = true)
      }
      fireChangeEvent(DebugEvent.CONTENT)
    }

  import scala.util.control.Exception
  import Exception.Catch

  /** Wrap calls to the underlying VM thread reference to handle exceptions gracefully. */
  private def safeThreadCalls[A](defaultValue: A): Catch[A] =
    (safeVmCalls(defaultValue)
      or Exception.failAsValue(
        classOf[IncompatibleThreadStateException],
        classOf[VMCannotBeModifiedException])(defaultValue))
}

private[model] object ArgusThreadActor {
  case class SuspendedFromArgus(eventDetail: Int)
  case class ResumeFromArgus(step: Option[ArgusStep], eventDetail: Int)
  case class InvokeMethod(objectReference: ObjectReference, method: Method, args: List[Value])
  case class InvokeStaticMethod(classType: ClassType, method: Method, args: List[Value])
  case class DropToFrame(frame: ArgusStackFrame)
  case object TerminatedFromArgus
  case class RebindStackFrames(shouldFireChangeEvent: Boolean)

  def apply(thread: ArgusThread): BaseDebuggerActor = {
    val actor = new ArgusThreadActor(thread)
    actor.start()
    actor
  }
}

/**
 * Actor used to manage a Argus thread. It keeps track of the existing stack frames, and of the execution status.
 * This class is thread safe. Instances are not to be created outside of the ArgusThread object.
 */
private[model] class ArgusThreadActor private(thread: ArgusThread) extends BaseDebuggerActor {
  import ArgusThreadActor._

  // step management
  private var currentStep: Option[ArgusStep] = None

  override protected def postStart(): Unit = link(thread.getDebugTarget.companionActor)

  override protected def behavior = {
    case SuspendedFromArgus(eventDetail) =>
      currentStep.foreach(_.stop())
      currentStep = None
      thread.suspend(eventDetail)
    case ResumeFromArgus(step, eventDetail) =>
      currentStep = step
      thread.resume(eventDetail)
      thread.threadRef.resume()
    case DropToFrame(frame) =>
      thread.dropToFrameInternal(frame)
    case RebindStackFrames(shouldFireChangeEvent) =>
      if (thread.isSuspended) {
        thread.rebindArgusStackFrames()
        if (shouldFireChangeEvent) thread.fireChangeEvent(DebugEvent.CONTENT)
      }
    case ArgusDebugTarget.UpdateStackFramesAfterHcr(dropAffectedFrames) =>
      if (thread.isSuspended) thread.updateArgusStackFramesAfterHcr(dropAffectedFrames)
    case InvokeMethod(objectReference, method, args) =>
      reply(
        if (!thread.isSuspended) {
          Left(new ThreadNotSuspendedException())
        } else {
          try {
            import scala.collection.JavaConverters._
            // invoke the method
            // FIXME: Doesn't handle checked exceptions `InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException, InvocationException`
            val result = objectReference.invokeMethod(thread.threadRef, method, args.asJava, ObjectReference.INVOKE_SINGLE_THREADED)
            // update the stack frames
            thread.rebindArgusStackFrames()
            Right(result)
          } catch {
            case e: Exception =>
              Left(e)
          }
        })
    case InvokeStaticMethod(classType, method, args) =>
      reply(
        if (!thread.isSuspended) {
          Left(new ThreadNotSuspendedException())
        } else {
          try {
            import scala.collection.JavaConverters._
            // invoke the method
            // FIXME: Doesn't handle checked exceptions `InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException, InvocationException`
            val result = classType.invokeMethod(thread.threadRef, method, args.asJava, ObjectReference.INVOKE_SINGLE_THREADED)
            // update the stack frames
            thread.rebindArgusStackFrames()
            Right(result)
          } catch {
            case e: Exception =>
              Left(e)
          }
        })
    case TerminatedFromArgus =>
      currentStep.foreach(_.stop())
      currentStep = None
      thread.fireTerminateEvent()
      poison()
  }

  override protected def preExit(): Unit = {
    // before shutting down the actor we need to unlink it from the `debugTarget` actor to prevent that normal termination of
    // a `ArgusThread` leads to shutting down the whole debug session.
    unlink(thread.getDebugTarget.companionActor)
  }
}
