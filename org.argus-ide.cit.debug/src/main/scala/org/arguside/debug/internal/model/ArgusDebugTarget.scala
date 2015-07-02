package org.arguside.debug.internal.model

import org.arguside.core.IArgusPlugin
import org.arguside.debug.internal.BaseDebuggerActor
import org.arguside.debug.internal.PoisonPill
import org.arguside.debug.internal.ArgusSourceLookupParticipant
import org.arguside.debug.internal.breakpoints.ArgusDebugBreakpointManager
import org.arguside.debug.internal.hcr.ClassFileResource
import org.arguside.debug.internal.hcr.HotCodeReplaceExecutor
import org.arguside.debug.internal.hcr.ArgusHotCodeReplaceManager
import org.arguside.debug.internal.hcr.ui.HotCodeReplaceListener
import org.arguside.debug.internal.preferences.HotCodeReplacePreferences
import org.arguside.logging.HasLogger

import org.eclipse.core.resources.IMarkerDelta
import org.eclipse.debug.core.DebugEvent
import org.eclipse.debug.core.DebugPlugin
import org.eclipse.debug.core.ILaunch
import org.eclipse.debug.core.model.IBreakpoint
import org.eclipse.debug.core.model.IDebugTarget
import org.eclipse.debug.core.model.IProcess
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector
import org.osgi.framework.Version

import com.sun.jdi.ClassNotLoadedException
import com.sun.jdi.ThreadReference
import com.sun.jdi.VirtualMachine
import com.sun.jdi.event.ThreadDeathEvent
import com.sun.jdi.event.ThreadStartEvent
import com.sun.jdi.event.VMDeathEvent
import com.sun.jdi.event.VMDisconnectEvent
import com.sun.jdi.event.VMStartEvent
import com.sun.jdi.request.ThreadDeathRequest
import com.sun.jdi.request.ThreadStartRequest

object ArgusDebugTarget extends HasLogger {

  def apply(virtualMachine: VirtualMachine,
            launch: ILaunch,
            process: IProcess,
            allowDisconnect: Boolean,
            allowTerminate: Boolean,
            classPath: Option[Seq[String]] = None): ArgusDebugTarget = {

    val threadStartRequest = JdiRequestFactory.createThreadStartRequest(virtualMachine)

    val threadDeathRequest = JdiRequestFactory.createThreadDeathRequest(virtualMachine)

    val debugTarget = new ArgusDebugTarget(virtualMachine, launch, process, allowDisconnect, allowTerminate, classPath) {
      override val companionActor = ArgusDebugTargetActor(threadStartRequest, threadDeathRequest, this)
      override val breakpointManager: ArgusDebugBreakpointManager = ArgusDebugBreakpointManager(this)
      override val hcrManager: Option[ArgusHotCodeReplaceManager] = ArgusHotCodeReplaceManager.create(companionActor)
      override val eventDispatcher: ArgusJdiEventDispatcher = ArgusJdiEventDispatcher(virtualMachine, companionActor)
      override val cache: ArgusDebugCache = ArgusDebugCache(this, companionActor)
    }

    launch.addDebugTarget(debugTarget)

    launch.getSourceLocator match {
      case sourceLookupDirector: ISourceLookupDirector =>
        sourceLookupDirector.addParticipants(Array(ArgusSourceLookupParticipant))
      case sourceLocator =>
        logger.warn("Unable to recognize source locator %s, cannot add Argus participant".format(sourceLocator))
    }

    debugTarget.startJdiEventDispatcher()

    debugTarget.fireCreationEvent()

    debugTarget
  }

  val versionStringPattern = "version ([^-]*).*".r

  /** A message sent to the companion actor to indicate we're attached to the VM. */
  private[model] object AttachedToVM
  private[internal] case class UpdateStackFramesAfterHcr(dropAffectedFrames: Boolean)
  private[internal] case class ReplaceClasses(changedClasses: Seq[ClassFileResource])
}

/**
 * A debug target in the Argus debug model.
 * This class is thread safe. Instances have be created through its companion object.
 */
abstract class ArgusDebugTarget private(val virtualMachine: VirtualMachine,
                                        launch: ILaunch, process: IProcess, allowDisconnect: Boolean,
                                        allowTerminate: Boolean, val classPath: Option[Seq[String]])
  extends ArgusDebugElement(null) with IDebugTarget with HasLogger {

  // Members declared in org.eclipse.debug.core.IBreakpointListener
  // ZW: what does these "???" 
  override def breakpointAdded(breakponit: IBreakpoint): Unit = ???

  override def breakpointChanged(breakpoint: IBreakpoint, delta: IMarkerDelta): Unit = ???

  override def breakpointRemoved(breakpoint: IBreakpoint, delta: IMarkerDelta): Unit = ???

  // Members declared in org.eclipse.debug.core.model.IDebugElement

  override def getLaunch: org.eclipse.debug.core.ILaunch = launch

  // Members declared in org.eclipse.debug.core.model.IDebugTarget

  // TODO: need better name
  override def getName: String = "Argus Debug Target"

  override def getProcess: org.eclipse.debug.core.model.IProcess = process

  override def getThreads: Array[org.eclipse.debug.core.model.IThread] = threads.toArray

  override def hasThreads: Boolean = !threads.isEmpty

  override def supportsBreakpoint(breakpoint: IBreakpoint): Boolean = ???

  // Members declared in org.eclipse.debug.core.model.IDisconnect

  override def canDisconnect(): Boolean = allowDisconnect && running

  override def disconnect(): Unit = {
    virtualMachine.dispose()
  }

  override def isDisconnected(): Boolean = !running

  // Members declared in org.eclipse.debug.core.model.IMemoryBlockRetrieval

  override def getMemoryBlock(startAddress: Long, length: Long): org.eclipse.debug.core.model.IMemoryBlock = ???

  override def supportsStorageRetrieval: Boolean = ???

  // Members declared in org.eclipse.debug.core.model.ISuspendResume

  // ZW: Why three false here
  // TODO: need real logic
  override def canResume: Boolean = false

  // TODO: need real logic
  override def canSuspend: Boolean = false

  // TODO: need real logic
  override def isSuspended: Boolean = false

  override def resume(): Unit = ???

  override def suspend(): Unit = ???

  // Members declared in org.eclipse.debug.core.model.ITerminate

  override def canTerminate: Boolean = allowTerminate && running

  override def isTerminated: Boolean = !running

  override def terminate(): Unit = {
    virtualMachine.exit(1)
    // manually clean up, as VMDeathEvent and VMDisconnectedEvent are not fired
    // when abruptly terminating the vM
    vmDisconnected()
    companionActor ! PoisonPill
  }

  override def getDebugTarget: ArgusDebugTarget = this

  // ---

  @volatile
  private var running: Boolean = true
  @volatile
  private var threads = List[ArgusThread]()

  @volatile
  private[internal] var isPerformingHotCodeReplace: Boolean = false

  private[debug] val eventDispatcher: ArgusJdiEventDispatcher
  private[debug] val breakpointManager: ArgusDebugBreakpointManager
  private[debug] val hcrManager: Option[ArgusHotCodeReplaceManager]
  private[debug] val companionActor: BaseDebuggerActor
  private[debug] val cache: ArgusDebugCache

  /**
   * Initialize the dependent components
   */
  private def startJdiEventDispatcher() = {
    // start the event dispatcher thread
    DebugPlugin.getDefault.asyncExec(new Runnable() {
      override def run(): Unit = {
        val thread = new Thread(eventDispatcher, "Argus debugger JDI event dispatcher")
        thread.setDaemon(true)
        thread.start()
      }
    })
  }

  /**
   * Callback from the breakpoint manager when a platform breakpoint is hit
   */
  private[debug] def threadSuspended(thread: ThreadReference, eventDetail: Int): Unit = {
    companionActor ! ArgusDebugTargetActor.ThreadSuspended(thread, eventDetail)
  }

  /*
   * JDI wrapper calls
   */
  /** Return a reference to the object with the given name in the debugged VM.
   *
   * @param objectName the name of the object, as defined in code (without '$').
   * @param tryForceLoad indicate if it should try to forceLoad the type if it is not loaded yet.
   * @param thread the thread to use to if a force load is needed. Can be `null` if tryForceLoad is `false`.
   *
   * @throws ClassNotLoadedException if the class was not loaded yet.
   * @throws IllegalArgumentException if there is no object of the given name.
   * @throws DebugException
   */
  def objectByName(objectName: String, tryForceLoad: Boolean, thread: ArgusThread): ArgusObjectReference = {
    val moduleClassName = objectName + '$'
    wrapJDIException("Exception while retrieving module debug element `" + moduleClassName + "`") {
      classByName(moduleClassName, tryForceLoad, thread).fieldValue("MODULE$").asInstanceOf[ArgusObjectReference]
    }
  }

  /** Return a reference to the type with the given name in the debugged VM.
   *
   * @param objectName the name of the object, as defined in code (without '$').
   * @param tryForceLoad indicate if it should try to forceLoad the type if it is not loaded yet.
   * @param thread the thread to use to if a force load is needed. Can be `null` if tryForceLoad is `false`.
   *
   * @throws ClassNotLoadedException if the class was not loaded yet.
   */
  private def classByName(typeName: String, tryForceLoad: Boolean, thread: ArgusThread): ArgusReferenceType = {
    import scala.collection.JavaConverters._
    virtualMachine.classesByName(typeName).asScala.toList match {
      case t :: _ =>
        ArgusType(t, this)
      case Nil =>
        if (tryForceLoad) {
          forceLoad(typeName, thread)
        } else {
          throw new ClassNotLoadedException(typeName, "No force load requested for " + typeName)
        }
    }
  }

  /** Attempt to force load a type, by finding the classloader of `scala.Predef` and calling `loadClass` on it.
   */
  private def forceLoad(typeName: String, thread: ArgusThread): ArgusReferenceType = {
    val predef = objectByName("scala.Predef", false, null)
    val classLoader = getClassLoader(predef, thread)
    classLoader.invokeMethod("loadClass", thread, ArgusValue(typeName, this))
    val entities = virtualMachine.classesByName(typeName)
    if (entities.isEmpty()) {
      throw new ClassNotLoadedException(typeName, "Unable to force load")
    } else {
      ArgusType(entities.get(0), this)
    }
  }

  /** Return the classloader of the given object.
   */
  private def getClassLoader(instance: ArgusObjectReference, thread: ArgusThread): ArgusObjectReference = {
    val typeClassLoader = instance.underlying.referenceType().classLoader()
    if (typeClassLoader == null) {
      // JDI returns null for classLoader() if the classloader is the boot classloader.
      // Fetch the boot classloader by using ClassLoader.getSystemClassLoader()
      val classLoaderClass = classByName("java.lang.ClassLoader", false, null).asInstanceOf[ArgusClassType]
      classLoaderClass.invokeMethod("getSystemClassLoader", thread).asInstanceOf[ArgusObjectReference]
    } else {
      new ArgusObjectReference(typeClassLoader, this)
    }
  }

  /** Called when attaching to a remote VM. Makes the companion actor run the initialization
   *  protocol (listen to the event queue, etc.)
   *
   *  @note This method has no effect if the actor was already initialized
   */
  def attached(): Unit = {
    companionActor ! ArgusDebugTarget.AttachedToVM
  }

  /*
   * Methods used by the companion actor to update this object internal states
   * FOR THE COMPANION ACTOR ONLY.
   */

  /** Callback form the actor when the connection with the vm is enabled.
   *
   *  This method initializes the debug target object:
   *   - retrieves the initial list of threads and creates the corresponding debug elements.
   *   - initializes the breakpoint manager
   *   - fires a change event
   */
  private[model] def vmStarted(): Unit = {
    // get the current requests
    import scala.collection.JavaConverters._
    initializeThreads(virtualMachine.allThreads.asScala.toList)
    breakpointManager.init()
    hcrManager.foreach(_.init())
    fireChangeEvent(DebugEvent.CONTENT)
  }

  /**
   * Callback from the actor when the connection with the vm as been lost
   */
  private[model] def vmDisconnected(): Unit = {
    running = false
    eventDispatcher.dispose()
    breakpointManager.dispose()
    hcrManager.foreach(_.dispose())
    cache.dispose()
    disposeThreads()
    fireTerminateEvent()
  }

  private def disposeThreads(): Unit = {
    threads.foreach {
      _.dispose()
    }
    threads = Nil
  }

  /**
   * Add a thread to the list of threads.
   * FOR THE COMPANION ACTOR ONLY.
   */
  private[model] def addThread(thread: ThreadReference): Unit = {
    if (!threads.exists(_.threadRef eq thread))
      threads = threads :+ ArgusThread(this, thread)
  }

  /**
   * Remove a thread from the list of threads
   * FOR THE COMPANION ACTOR ONLY.
   */
  private[model] def removeThread(thread: ThreadReference): Unit = {
    val (removed, remainder) = threads.partition(_.threadRef eq thread)
    threads = remainder
    removed.foreach(_.terminatedFromArgus())
  }

  /**
   * Set the initial list of threads.
   * FOR THE COMPANION ACTOR ONLY.
   */
  private[model] def initializeThreads(t: List[ThreadReference]): Unit = {
    threads = t.map(ArgusThread(this, _))
  }

  /**
   * Refreshes frames of all suspended, non-system threads and optionally drops affected stack frames.
   */
  private[internal] def updateStackFramesAfterHcr(dropAffectedFrames: Boolean): Unit =
    companionActor ! ArgusDebugTarget.UpdateStackFramesAfterHcr(dropAffectedFrames)

  /**
   * Return the current list of threads
   */
  private[model] def getArgusThreads: List[ArgusThread] = threads

  private[model] def canPopFrames: Boolean = running && virtualMachine.canPopFrames()

}

private[model] object ArgusDebugTargetActor {

  case class ThreadSuspended(thread: ThreadReference, eventDetail: Int)

  def apply(threadStartRequest: ThreadStartRequest, threadDeathRequest: ThreadDeathRequest, debugTarget: ArgusDebugTarget): ArgusDebugTargetActor = {
    val actor = new ArgusDebugTargetActor(threadStartRequest, threadDeathRequest, debugTarget)
    if (!IArgusPlugin().headlessMode) actor.subscribe(HotCodeReplaceListener)
    actor.start()
    actor
  }
}

/**
 * Actor used to manage a Argus debug target. It keeps track of the existing threads.
 * This class is thread safe. Instances are not to be created outside of the ArgusDebugTarget object.
 *
 * The `ArgusDebugTargetActor` is linked to both the `ArgusJdiEventDispatcherActor and the
 * `ArgusDebugBreakpointManagerActor`, this implies that if any of the three actors terminates (independently
 * of the reason), all other actors will also be terminated (an `Exit` message will be sent to each of the
 * linked actors).
 */
private class ArgusDebugTargetActor private(threadStartRequest: ThreadStartRequest, threadDeathRequest: ThreadDeathRequest, protected val debugTarget: ArgusDebugTarget)
    extends BaseDebuggerActor
    with HotCodeReplaceExecutor {

  import ArgusDebugTargetActor._

  /** Is this actor initialized and listening to thread events? */
  private var initialized = false

  override protected def behavior = {
    case _: VMStartEvent =>
      initialize()
      reply(false)
    case ArgusDebugTarget.AttachedToVM =>
      initialize()
    case threadStartEvent: ThreadStartEvent =>
      debugTarget.addThread(threadStartEvent.thread)
      reply(false)
    case threadDeathEvent: ThreadDeathEvent =>
      debugTarget.removeThread(threadDeathEvent.thread)
      reply(false)
    case _: VMDeathEvent | _: VMDisconnectEvent =>
      vmDisconnected()
      reply(false)
    case ThreadSuspended(thread, eventDetail) =>
      // forward the event to the right thread
      debugTarget.getArgusThreads.find(_.threadRef == thread).foreach(_.suspendedFromArgus(eventDetail))
    case msg: ArgusDebugTarget.UpdateStackFramesAfterHcr =>
      val nonSystemThreads = debugTarget.getArgusThreads.filterNot(_.isSystemThread)
      nonSystemThreads.foreach(_.updateStackFramesAfterHcr(msg))
    case ArgusDebugTarget.ReplaceClasses(changedClasses) =>
      replaceClassesIfVMAllows(changedClasses)
  }

  /** Initialize this debug target actor:
   *
   *   - listen to thread start/death events
   *   - initialize the companion debug target
   */
  private def initialize(): Unit = {
    if (!initialized) {
      val eventDispatcher = debugTarget.eventDispatcher
      // enable the thread management requests
      eventDispatcher.setActorFor(this, threadStartRequest)
      threadStartRequest.enable()
      eventDispatcher.setActorFor(this, threadDeathRequest)
      threadDeathRequest.enable()
      debugTarget.vmStarted()
      initialized = true
    }
  }

  override protected def preExit(): Unit = {
    debugTarget.vmDisconnected()
  }

  private def vmDisconnected(): Unit = poison()
}
