package org.arguside.ui.internal.wizards

import org.sireum.util._

/**
 * @author Fengguo Wei
 */
class FinalizingActions {
  private val mFinalizingActions: MList[Runnable] = mlistEmpty
  def addAction(act: Runnable) = mFinalizingActions += act
  def getActions: IList[Runnable] = mFinalizingActions.toList
}