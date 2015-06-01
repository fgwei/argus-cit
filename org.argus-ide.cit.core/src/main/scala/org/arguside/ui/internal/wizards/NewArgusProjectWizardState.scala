package org.arguside.ui.internal.wizards

import com.android.ide.eclipse.adt.AdtUtils
import com.android.ide.eclipse.adt.internal.assetstudio.CreateAssetSetWizardState
import com.android.sdklib.IAndroidTarget
import org.eclipse.ui.IWorkingSet
import org.sireum.util._

/**
 * Value object which holds the current state of the wizard pages for the
 * {@link NewArgusProjectWizard}
 */
class NewArgusProjectWizardState {
  
  /** The name of the project */
  var projectName: String = null
  
  /** Whether the project name has been edited by the user */
  var projectModified: Boolean = false
  
  /** The application name */
  var applicationName: String = null

  /** Whether the application name has been edited by the user */
  var applicationModified: Boolean = false

  /** The compilation target to use for this project */
  var target: IAndroidTarget = null

  /** The minimum SDK API level, as a string (if the API is a preview release with a codename) */
  var minSdk: String = null

  /** The minimum SDK API level to use */
  var minSdkLevel: Int = 0

  /** The target SDK level */
  var targetSdkLevel = AdtUtils.getHighestKnownApiLevel()

  // Delegated wizards

  /** State for the asset studio wizard, used to create custom icons */
  val iconState = new CreateAssetSetWizardState
  
  /** Whether a custom location should be used */
  var useDefaultLocation: Boolean = true

  /** Folder where the project should be created. */
  var projectLocation: String = null

  /** Configured parameters, by id */
  var parameters: IMap[String, Object] = imapEmpty

  /** The set of chosen working sets to use when creating the project */
  var workingSets: IList[IWorkingSet] = ilistEmpty

  val finalizingActions: FinalizingActions = new FinalizingActions
  
  /**
   * Returns the build target API level
   *
   * @return the build target API level
   */
  def getBuildApi: Int = {
    if(target == null) minSdkLevel
    else target.getVersion.getApiLevel
  }
}