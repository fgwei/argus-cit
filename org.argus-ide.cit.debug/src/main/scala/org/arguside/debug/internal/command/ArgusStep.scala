package org.arguside.debug.internal.command

import org.arguside.debug.internal.BaseDebuggerActor

object ArgusStep {
  case object Step
  case object Stop
}

/** A step in the Scala debug model. Implementations need to be thread safe. */
trait ArgusStep {
  /** Initiate the step action. */
  def step(): Unit

  /** Terminates the step action and clean the resources. */
  def stop(): Unit
}

class ArgusStepImpl(companionActor: BaseDebuggerActor) extends ArgusStep {
  override def step(): Unit = companionActor ! ArgusStep.Step
  override def stop(): Unit = companionActor ! ArgusStep.Stop
}