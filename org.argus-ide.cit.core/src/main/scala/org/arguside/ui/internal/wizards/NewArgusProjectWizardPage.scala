package org.arguside.ui.internal.wizards

import org.eclipse.jface.wizard.WizardPage
import org.eclipse.swt.events.ModifyListener
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.events.FocusListener
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.widgets.Text
import org.eclipse.jface.fieldassist.ControlDecoration
import org.eclipse.swt.widgets.Control
import org.eclipse.jface.fieldassist.FieldDecorationRegistry
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.widgets.FileDialog
import java.io.File
import org.eclipse.core.runtime.Path
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.widgets.Combo
import org.sireum.util._
import com.android.sdklib.IAndroidTarget
import com.android.sdklib.AndroidVersion
import com.android.sdklib.SdkVersionInfo
import com.android.ide.eclipse.adt.internal.sdk.Sdk
import com.android.ide.eclipse.adt.AdtUtils
import sun.security.util.Length
import com.android.ide.eclipse.adt.internal.editors.IconFactory
import org.eclipse.swt.events.FocusEvent
import org.eclipse.core.runtime.IStatus
import org.arguside.core.internal.ArgusPlugin
import org.arguside.core.CitConstants
import org.eclipse.core.runtime.Status
import org.eclipse.core.resources.IWorkspace
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.resources.IResource
import org.eclipse.core.filesystem.URIUtil
import com.android.SdkConstants
import com.android.ide.eclipse.adt.internal.project.AndroidManifestHelper
import org.eclipse.swt.events.ModifyEvent
import org.eclipse.core.runtime.Platform
import org.arguside.util.parser.ManifestParser
import com.android.ide.eclipse.adt.internal.wizards.newproject.WorkingSetGroup
import org.eclipse.ui.IWorkingSet
import org.eclipse.jface.viewers.IStructuredSelection
import org.eclipse.ui.IWorkbenchPart
import com.android.ide.eclipse.adt.internal.wizards.newproject.WorkingSetHelper
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.DirectoryDialog
import org.eclipse.jface.dialogs.IMessageProvider


class NewArgusProjectWizardPage(values: NewArgusProjectWizardState) extends WizardPage("newArgusProject")
        with ModifyListener with SelectionListener with FocusListener{
  
  import NewArgusProjectWizardPage._
  
  setTitle("New Argus Application")
  setDescription("Creates a new Argus Application")
  
  private final val FIELD_WIDTH = 300
  private final val WIZARD_PAGE_WIDTH = 600
  private final val INITIAL_MIN_SDK = 8
  
  private var mIgnore = false
  private val mMinNameToApi: MMap[String, Int] = mmapEmpty
  private final val mValues: NewArgusProjectWizardState = values
  private var sLastProjectLocation: String = System.getProperty("user.home")
  
  // widgets
  private var mApplicationText: Text = null
  private var mProjectText: Text = null
  private var mMinSdkCombo: Combo = null
  private var mTargetSdkCombo: Combo = null
  private var mBuildSdkCombo: Combo = null
  private var mBrowseApkButton: Button = null
  
  private var mHelpIcon: Label = null
  private var mTipLabel: Label = null
  
  private var mApplicationDec: ControlDecoration = null
  private var mProjectDec: ControlDecoration = null
  private var mMinSdkDec: ControlDecoration = null
  private var mTargetSdkDec: ControlDecoration = null
  private var mBuildTargetDec: ControlDecoration = null
  
  private var mUseDefaultLocationToggle: Button = null
  private var mLocationLabel: Label = null
  private var mLocationText: Text = null
  private var mChooseLocationButton: Button = null
  private var mWorkingSetGroup: WorkingSetGroup = new WorkingSetGroup
  setWorkingSets(ilistEmpty[IWorkingSet])
  
  /**
   * Returns the working sets to which the new project should be added.
   *
   * @return the selected working sets to which the new project should be added
   */
  private def getWorkingSets: IList[IWorkingSet] = {
    mWorkingSetGroup.getSelectedWorkingSets().toList
  }

  /**
   * Sets the working sets to which the new project should be added.
   *
   * @param workingSets the initial selected working sets
   */
  private def setWorkingSets(workingSets: IList[IWorkingSet]) = {
    assert(workingSets != null)
    mWorkingSetGroup.setWorkingSets(workingSets.toArray)
    mValues.workingSets = workingSets
  }
  
  private def setUseCustomLocation(en: Boolean) = {
    mValues.useDefaultLocation = !en
    mUseDefaultLocationToggle.setSelection(!en)
    if (!en) {
      updateProjectLocation(mValues.projectName)
    }

    mLocationLabel.setEnabled(en)
    mLocationText.setEnabled(en)
    mChooseLocationButton.setEnabled(en)
  }
  
  def init(selection: IStructuredSelection, activePart: IWorkbenchPart) = {
      setWorkingSets(WorkingSetHelper.getSelectedWorkingSet(selection, activePart).toList)
  }
  
  override def createControl(parent: Composite): Unit = {
    val container = new Composite(parent, SWT.NULL)
    setControl(container)
    val gl_container = new GridLayout(4, false)
    gl_container.horizontalSpacing = 10
    container.setLayout(gl_container)
    
    val applicationLabel = new Label(container, SWT.NONE)
    applicationLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1))
    applicationLabel.setText("Android APK:")
    
    mApplicationText = new Text(container, SWT.BORDER)
    val gdApplicationText = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1)
    gdApplicationText.widthHint = FIELD_WIDTH
    mApplicationText.setLayoutData(gdApplicationText)
    mApplicationText.addModifyListener(this)
    mApplicationText.addFocusListener(this)
    mApplicationText.setEnabled(true)
    mApplicationText.setEditable(false)
    mApplicationDec = createFieldDecoration(mApplicationText, "The apk you want to decompile.")
            
    mBrowseApkButton = new Button(container, SWT.NONE)
    mBrowseApkButton.setText("Browse...")
    mBrowseApkButton.addSelectionListener(this)
    mBrowseApkButton.setEnabled(true)
    
    val projectLabel = new Label(container, SWT.NONE)
    projectLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1))
    projectLabel.setText("Project Name:")
    mProjectText = new Text(container, SWT.BORDER)
    val gdProjectText = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1)
    gdProjectText.widthHint = FIELD_WIDTH
    mProjectText.setLayoutData(gdProjectText)
    mProjectText.addModifyListener(this)
    mProjectText.addFocusListener(this)
    mProjectText.setEditable(false)
    mProjectDec = createFieldDecoration(mProjectText,
            "The project name is only used by Eclipse, but must be unique within the " +
            "workspace. This can typically be the same as the application name.")
    
    new Label(container, SWT.NONE)
    new Label(container, SWT.NONE)
    new Label(container, SWT.NONE)
    new Label(container, SWT.NONE)
    
    // Min SDK
    
    val minSdkLabel = new Label(container, SWT.NONE)
    minSdkLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1))
    minSdkLabel.setText("Minimum Required SDK:")
    
    mMinSdkCombo = new Combo(container, SWT.READ_ONLY)
    val gdMinSdkCombo = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1)
    gdMinSdkCombo.widthHint = FIELD_WIDTH
    mMinSdkCombo.setLayoutData(gdMinSdkCombo)
    
    // Pick most recent platform
    val targets: IList[IAndroidTarget] = getCompilationTargets
    mMinNameToApi.clear()
    val targetLabels: MList[String] = mlistEmpty
    for(target <- targets){
      var targetLabel: String = ""
      if(target.isPlatform() && target.getVersion().getApiLevel() <= AdtUtils.getHighestKnownApiLevel()) {
        targetLabel = AdtUtils.getAndroidName(target.getVersion().getApiLevel())
      } else {
        targetLabel = AdtUtils.getTargetLabel(target)
      }
      targetLabels += targetLabel
      mMinNameToApi(targetLabel) = target.getVersion.getApiLevel
    }
    
    val codeNames: MList[String] = mlistEmpty
    var buildTargetIndex: Int = -1
    for(i <- 0 to targets.size - 1){
      val target = targets(i)
      val version = target.getVersion
      val apiLevel = version.getApiLevel
      if(version.isPreview()) {
        val codeName = version.getCodename
        val targetLabel = codeName + " Preview"
        codeNames += "targetLabel"
        mMinNameToApi(targetLabel) = apiLevel
      } else if (target.isPlatform() 
          && (mValues.target == null ||
                apiLevel > mValues.target.getVersion().getApiLevel())){
        mValues.target = target
        buildTargetIndex = i
      }
    }
    
    val labels: MList[String] = mlistEmpty
    labels ++= AdtUtils.getKnownVersions
    assert(labels.size >= 15)
    labels ++= codeNames
    val versions = labels.toArray
    mMinSdkCombo.setItems(versions)
    if(mValues.target != null && mValues.target.getVersion.isPreview()) {
      mValues.minSdk = mValues.target.getVersion.getCodename
      mMinSdkCombo.setText(mValues.minSdk)
      mValues.iconState.minSdk = mValues.target.getVersion().getApiLevel()
      mValues.minSdkLevel = mValues.iconState.minSdk
    } else {
      mMinSdkCombo.select(INITIAL_MIN_SDK - 1)
      mValues.minSdk = Integer.toString(INITIAL_MIN_SDK)
      mValues.minSdkLevel = INITIAL_MIN_SDK
      mValues.iconState.minSdk = INITIAL_MIN_SDK
    }
    mMinSdkCombo.addSelectionListener(this)
    mMinSdkCombo.addFocusListener(this)
    mMinSdkCombo.setEnabled(false)
    mMinSdkDec = createFieldDecoration(mMinSdkCombo,
            "Choose the lowest version of Android that your application will support. Lower " +
            "API levels target more devices, but means fewer features are available. By " +
            "targeting API 8 and later, you reach approximately 95% of the market.")
            
    new Label(container, SWT.NONE)
    
    // Target SDK
    val targetSdkLabel = new Label(container, SWT.NONE)
    targetSdkLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1))
    targetSdkLabel.setText("Target SDK:")

    mTargetSdkCombo = new Combo(container, SWT.READ_ONLY)
    val gdTargetSdkCombo = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1)
    gdTargetSdkCombo.widthHint = FIELD_WIDTH
    mTargetSdkCombo.setLayoutData(gdTargetSdkCombo)

    mTargetSdkCombo.setItems(versions)
    mTargetSdkCombo.select(mValues.targetSdkLevel - 1)

    mTargetSdkCombo.addSelectionListener(this)
    mTargetSdkCombo.addFocusListener(this)
    mTargetSdkCombo.setEnabled(false)
    mTargetSdkDec = createFieldDecoration(mTargetSdkCombo,
            "Choose the highest API level that the application is known to work with. " +
            "This attribute informs the system that you have tested against the target " +
            "version and the system should not enable any compatibility behaviors to " +
            "maintain your app's forward-compatibility with the target version. " +
            "The application is still able to run on older versions " +
            "(down to minSdkVersion). Your application may look dated if you are not " +
            "targeting the current version.")
            
    new Label(container, SWT.NONE)
    
    // Build Version

    val buildSdkLabel = new Label(container, SWT.NONE)
    buildSdkLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1))
    buildSdkLabel.setText("Compile With:")

    mBuildSdkCombo = new Combo(container, SWT.READ_ONLY)
    val gdBuildSdkCombo = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1)
    gdBuildSdkCombo.widthHint = FIELD_WIDTH
    mBuildSdkCombo.setLayoutData(gdBuildSdkCombo)
    mBuildSdkCombo.setData(targets)
    mBuildSdkCombo.setItems(targetLabels.toArray)
    if (buildTargetIndex != -1) {
        mBuildSdkCombo.select(buildTargetIndex)
    }

    mBuildSdkCombo.addSelectionListener(this)
    mBuildSdkCombo.addFocusListener(this)
//    mBuildSdkCombo.setEnabled(false)
    mBuildTargetDec = createFieldDecoration(mBuildSdkCombo,
            "Choose a target API to compile your code against, from your installed SDKs. " +
            "This is typically the most recent version, or the first version that supports " +
            "all the APIs you want to directly access without reflection.")
            
    new Label(container, SWT.NONE)
    new Label(container, SWT.NONE)
    new Label(container, SWT.NONE)
    new Label(container, SWT.NONE)
    
    val label = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL)
    label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 4, 1))

    mHelpIcon = new Label(container, SWT.NONE)
    mHelpIcon.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 1))
    val icon = IconFactory.getInstance().getIcon("quickfix")
    mHelpIcon.setImage(icon)
    mHelpIcon.setVisible(false)

    mTipLabel = new Label(container, SWT.WRAP)
    mTipLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1))

    // Reserve space for 2 lines
    mTipLabel.setText("\n\n") //$NON-NLS-1$

    // Reserve enough width to accommodate the various wizard pages up front
    // (since they are created lazily, and we don't want the wizard to dynamically
    // resize itself for small size adjustments as each successive page is slightly
    // larger)
    val dummy = new Label(container, SWT.NONE)
    val data = new GridData()
    data.horizontalSpan = 4
    data.widthHint = WIZARD_PAGE_WIDTH
    dummy.setLayoutData(data)
    
    // line
    new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(
        new GridData(SWT.FILL, SWT.TOP, true, false, 4, 1))

    mUseDefaultLocationToggle = new Button(container, SWT.CHECK)
    mUseDefaultLocationToggle.setLayoutData(
            new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1))
    mUseDefaultLocationToggle.setText("Create Project in Workspace")
    mUseDefaultLocationToggle.addSelectionListener(this)

    mLocationLabel = new Label(container, SWT.NONE)
    mLocationLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1))
    mLocationLabel.setText("Location:")

    mLocationText = new Text(container, SWT.BORDER)
    mLocationText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1))
    mLocationText.addModifyListener(this)

    mChooseLocationButton = new Button(container, SWT.NONE)
    mChooseLocationButton.setText("Browse...")
    mChooseLocationButton.addSelectionListener(this)
    mChooseLocationButton.setEnabled(false)
    setUseCustomLocation(!mValues.useDefaultLocation)

    new Label(container, SWT.NONE).setLayoutData(
            new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1))

    val group = mWorkingSetGroup.createControl(container)
    group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 4, 1))
    
  }
  
  private def createFieldDecoration(control: Control, description: String): ControlDecoration = {
    val dec = new ControlDecoration(control, SWT.LEFT)
    dec.setMarginWidth(2)
    val errorFieldIndicator = FieldDecorationRegistry.getDefault().
       getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION)
    dec.setImage(errorFieldIndicator.getImage())
    dec.setDescriptionText(description)
    control.setToolTipText(description)
    dec
  }
  
  private def getCompilationTargets: IList[IAndroidTarget] = {
    val current = Sdk.getCurrent()
    if (current == null) {
      return ilistEmpty
    }
    val targets: IList[IAndroidTarget] = current.getTargets().toList
    targets
  }
  
  override def setVisible(visible: Boolean) = {
    super.setVisible(visible)
    
    if (visible) {
      try {
        mIgnore = true
        mUseDefaultLocationToggle.setSelection(mValues.useDefaultLocation)
        mLocationText.setText(mValues.projectLocation)
      } finally {
        mIgnore = false
      }
    }
    
    validatePage
  }
  
  /**
   * Returns the value of the project location field with leading and trailing
   * spaces removed.
   * 
   * @return the project location directory in the field
   */
  def getApplicationTextValue: String = {
    if (mApplicationText == null) {
      return "" //$NON-NLS-1$
    } else {
      return mApplicationText.getText().trim()
    }
  }
  
  // ---- Implements ModifyListener ----

  override def modifyText(e: ModifyEvent): Unit = {
    if(mIgnore) {
      return
    }
    val source = e.getSource
    source match {
      case x if x == mProjectText =>
        mValues.projectName = mProjectText.getText
        updateProjectLocation(mValues.projectName)
        mValues.projectModified = true
      case x if x == mApplicationText =>
        mValues.applicationName = mApplicationText.getText
        mValues.applicationModified = true
        try {
          mIgnore = true
          if(!mValues.projectModified){
            mValues.projectName = appNameToProjectName(mValues.applicationName)
            mProjectText.setText(mValues.projectName)
            mProjectText.setEditable(true)
            updateProjectLocation(mValues.projectName)
            updateSdkVersion
          }
        } finally {
          mIgnore = false
        }
      case x if x == mLocationText => mValues.projectLocation = mLocationText.getText.trim()
    }
    validatePage
  }
  
  private def appNameToProjectName(appName: String): String = {
    require(appName.endsWith(".apk"))
    var temp = appName.substring(appName.lastIndexOf("/") + 1, appName.length() - 4)
    // Strip out whitespace (and capitalize subsequent words where spaces were removed
    var upcaseNext = false
    var sb = new StringBuilder(temp.length())
    for (i <- 0 to temp.length() - 1) {
      val c = temp.charAt(i)
      if (c == ' ') {
        upcaseNext = true
      } else if (upcaseNext) {
        sb.append(Character.toUpperCase(c))
        upcaseNext = false
      } else {
        sb.append(c)
      }
    }

    temp = sb.toString().trim()

    val workspace = ResourcesPlugin.getWorkspace()
    val nameStatus = workspace.validateName(temp, IResource.PROJECT)
    if (nameStatus.isOK()) {
      return temp
    }

    sb = new StringBuilder(temp.length())
    for (i <- 0 to temp.length() - 1) {
      val c = temp.charAt(i)
      if (Character.isLetterOrDigit(c) || c == '.' || c == '-') {
        sb.append(c)
      }
    }

    sb.toString().trim()
  }
  
  /** If the project should be created in the workspace, then update the project location
   * based on the project name. */
  private def updateProjectLocation(projectName: String) = {
    var temp = projectName
    if (temp == null) {
      temp = ""
    }
    
    val useDefaultLocation = mUseDefaultLocationToggle.getSelection()

    if (useDefaultLocation) {
      val workspace = Platform.getLocation()
      val projectLocation = workspace.append(temp).toOSString()
      mLocationText.setText(projectLocation)
      mValues.projectLocation = projectLocation
    }
  }
  
  private def updateSdkVersion = {
    val apk = new File(mValues.applicationName)
    val (min, target, max) = ManifestParser.loadSdkVersionFromManifestFile(apk)
    mValues.minSdk = min.toString()
    mValues.minSdkLevel = min
    mValues.iconState.minSdk = min
    mValues.targetSdkLevel = target
    setSelectedMinSdk(min)
    setSelectedTargetSdk(target)
  }
  
  // ---- Implements SelectionListener ----
  override def widgetDefaultSelected(e: SelectionEvent): Unit = {}
  
  override def widgetSelected(event: SelectionEvent): Unit = {
    if(mIgnore) return
    
    val source = event.getSource
    source match {
      case x if x == mBrowseApkButton => handleImportBrowseButtonPressed
      case x if x == mMinSdkCombo => handleMinSdkComboPressed
      case x if x == mBuildSdkCombo => handleBuildSdkComboPressed
      case x if x == mTargetSdkCombo => mValues.targetSdkLevel = getSelectedTargetSdk
      case x if x == mUseDefaultLocationToggle => setUseCustomLocation(!mUseDefaultLocationToggle.getSelection)
      case x if x == mChooseLocationButton => 
        val dir = promptUserForLocation(getShell)
        if(dir != null){
          mLocationText.setText(dir)
          mValues.projectLocation = dir
        }
      case _ =>
    }
    
    validatePage
  }
  
  private def promptUserForLocation(shell: Shell): String = {
    val dd = new DirectoryDialog(shell)
    dd.setMessage("Select folder where project should be created")

    val curLocation = mLocationText.getText().trim()
    if (!curLocation.isEmpty()) {
      dd.setFilterPath(curLocation)
    } else if (sLastProjectLocation != null) {
      dd.setFilterPath(sLastProjectLocation)
    }

    val dir = dd.open()
    if (dir != null) {
      sLastProjectLocation = dir
    }

    dir
  }
  
  /**
   * Open an appropriate file browser
   */
  private def handleImportBrowseButtonPressed = {
    val dialog = new FileDialog(mApplicationText.getShell())
    val exts = Set( "apk" ).toArray
    dialog.setFilterExtensions(exts)

    val apkPath = getApplicationTextValue
    if (!apkPath.equals("")) { //$NON-NLS-1$
      val apk = new File(apkPath)
      if (apk.exists()) {
        dialog.setFilterPath(new Path(apkPath).toOSString())
      }
    }

    val selectedDirectory = dialog.open()
    if (selectedDirectory != null) {
      mApplicationText.setText(selectedDirectory)
    }
  }
  
  private def handleMinSdkComboPressed = {
    mValues.minSdk = getSelectedMinSdk
    val minSdk: Int = mMinNameToApi.getOrElse(mValues.minSdk, {
      try {
        Integer.parseInt(mValues.minSdk)
      } catch {
        case nfe: NumberFormatException =>
          // If not a number, then the string is a codename, so treat it
          // as a preview version.
          SdkVersionInfo.HIGHEST_KNOWN_API + 1
      }
    })
    mValues.iconState.minSdk = minSdk.intValue()
    mValues.minSdkLevel = minSdk.intValue()
    
    if (mValues.minSdkLevel > mValues.getBuildApi) {
      // Try to find a build target with an adequate build API
      val targets = mBuildSdkCombo.getData().asInstanceOf[IList[IAndroidTarget]]
      var best: IAndroidTarget = null
      var bestApi = Integer.MAX_VALUE
      var bestTargetIndex = -1
      for (i <- 0 to targets.length - 1) {
        val target = targets(i)
        if (target.isPlatform()) {
          val api = target.getVersion().getApiLevel()
          if (api >= mValues.minSdkLevel && api < bestApi) {
            best = target
            bestApi = api
            bestTargetIndex = i
          }
        }
      }

      if (best != null) {
        assert(bestTargetIndex != -1)
        mValues.target = best
        try {
          mIgnore = true
          mBuildSdkCombo.select(bestTargetIndex)
        } finally {
          mIgnore = false
        }
      }
    }
    // If higher than targetSdkVersion, adjust targetSdkVersion
    if (mValues.minSdkLevel > mValues.targetSdkLevel) {
      mValues.targetSdkLevel = mValues.minSdkLevel
      try {
        mIgnore = true
        setSelectedTargetSdk(mValues.targetSdkLevel)
      } finally {
        mIgnore = false
      }
    }
  }
  
  private def handleBuildSdkComboPressed = {
    mValues.target = getSelectedBuildTarget
  }
  
  private def getSelectedMinSdk: String = {
    // If you're using a preview build, such as android-JellyBean, you have
    // to use the codename, e.g. JellyBean, as the minimum SDK as well.
    val buildTarget = getSelectedBuildTarget
    if (buildTarget != null && buildTarget.getVersion().isPreview()) {
      return buildTarget.getVersion().getCodename()
    }

    // +1: First API level (at index 0) is 1
    Integer.toString(mMinSdkCombo.getSelectionIndex() + 1)
  }
  
  private def getSelectedTargetSdk: Int = {
    // +1: First API level (at index 0) is 1
    mTargetSdkCombo.getSelectionIndex() + 1
  }

  private def setSelectedMinSdk(api: Int) = {
    mMinSdkCombo.select(api - 1) // -1: First API level (at index 0) is 1
  }

  private def setSelectedTargetSdk(api: Int) = {
    mTargetSdkCombo.select(api - 1) // -1: First API level (at index 0) is 1
  }

  private def getSelectedBuildTarget: IAndroidTarget = {
    val targets = mBuildSdkCombo.getData().asInstanceOf[IList[IAndroidTarget]]
    val index = mBuildSdkCombo.getSelectionIndex()
    if (index >= 0 && index < targets.length) {
      targets(index)
    } else {
      null
    }
  }
  
  // ---- Implements FocusListener ----
  
  override def focusGained(e: FocusEvent) = {
    val source = e.getSource
    var tip = ""
    source match {
      case x if x == mApplicationText =>
        tip = mApplicationDec.getDescriptionText
      case x if x == mProjectText =>
        tip = mProjectDec.getDescriptionText
      case x if x == mBuildSdkCombo =>
        tip = mBuildTargetDec.getDescriptionText
      case x if x == mMinSdkCombo =>
        tip = mMinSdkDec.getDescriptionText
      case x if x == mTargetSdkCombo =>
        tip = mTargetSdkDec.getDescriptionText
      case _ =>
    }
    mTipLabel.setText(tip)
    mHelpIcon.setVisible(tip.length() > 0)
  }
  
  override def focusLost(e: FocusEvent) = {
    mTipLabel.setText("")
    mHelpIcon.setVisible(false)
  }
  
  // Validation
  private def validatePage = {
    var status: IStatus = null
    val projectStatus = validateProjectName
    if (projectStatus != null && (status == null
            || projectStatus.getSeverity() > status.getSeverity())) {
        status = projectStatus;
    }
    if (status == null || status.getSeverity() != IStatus.ERROR) {
      if (mValues.target == null) {
        status = new Status(IStatus.WARNING, CitConstants.PluginId,
                "Select an Android build target version");
      }
    }

    if (status == null || status.getSeverity() != IStatus.ERROR) {
      if (mValues.minSdk == null || mValues.minSdk.isEmpty()) {
        status = new Status(IStatus.WARNING, CitConstants.PluginId,
                "Select a minimum SDK version");
      } else {
        val version = mValues.target.getVersion()
        if (version.isPreview()) {
          if (version.getCodename().equals(mValues.minSdk) == false) {
            status = new Status(IStatus.ERROR, CitConstants.PluginId,
            "Preview platforms require the min SDK version to match their codenames.")
          }
        } else if (mValues.target.getVersion().compareTo(
                mValues.minSdkLevel,
                if(version.isPreview()) mValues.minSdk else null) < 0) {
          status = new Status(IStatus.ERROR, CitConstants.PluginId,
              "The minimum SDK version is higher than the build target version");
        }
        if (status == null || status.getSeverity() != IStatus.ERROR) {
          if (mValues.targetSdkLevel < mValues.minSdkLevel) {
            status = new Status(IStatus.ERROR, CitConstants.PluginId,
                "The target SDK version should be at least as high as the minimum SDK version");
          }
        }
      }
    }
    if(status == null || status.getSeverity != IStatus.ERROR){
      status = validateProjectLocation
    }
    setPageComplete(status == null || status.getSeverity() != IStatus.ERROR)
    if (status != null) {
      setMessage(status.getMessage(),
          if(status.getSeverity() == IStatus.ERROR) IMessageProvider.ERROR else IMessageProvider.WARNING)
    } else {
      setErrorMessage(null);
      setMessage(null);
    }
    
  }
  
  private def validateProjectName: IStatus = {
    val status = NewArgusProjectWizardPage.validateProjectName(mValues.projectName)
    updateDecorator(mProjectDec, status, true)
    status
  }
  
  private def updateDecorator(decorator: ControlDecoration, status: IStatus, hasInfo: Boolean): Unit = {
    if (hasInfo) {
      val severity = if(status != null) status.getSeverity() else IStatus.OK
      setDecoratorType(decorator, severity)
    } else {
      if (status == null || status.isOK()) {
        decorator.hide()
      } else {
        decorator.show()
      }
    }
  }

  private def setDecoratorType(decorator: ControlDecoration, severity: Int): Unit = {
    var id: String = null
    if (severity == IStatus.ERROR) {
      id = FieldDecorationRegistry.DEC_ERROR
    } else if (severity == IStatus.WARNING) {
      id = FieldDecorationRegistry.DEC_WARNING
    } else {
      id = FieldDecorationRegistry.DEC_INFORMATION
    }
    val errorFieldIndicator = FieldDecorationRegistry.getDefault().
          getFieldDecoration(id)
    decorator.setImage(errorFieldIndicator.getImage())
  }
  
  private def validateProjectLocation: IStatus = {
    if (mValues.useDefaultLocation) {
      return validateLocationInWorkspace(mValues)
    }

    val location = mLocationText.getText()
    if (location.trim().isEmpty()) {
      return new Status(IStatus.ERROR, CitConstants.PluginId,
              "Provide a valid file system location where the project should be created.")
    }

    val f = new File(location)
    if (f.exists()) {
      if (!f.isDirectory()) {
        return new Status(IStatus.ERROR, CitConstants.PluginId,
                String.format("'%s' is not a valid folder.", location))
      }

      val children = f.listFiles()
      if (children != null && children.length > 0) {
        return new Status(IStatus.ERROR, CitConstants.PluginId,
                String.format("Folder '%s' is not empty.", location))
      }
    }

    // if the folder doesn't exist, then make sure that the parent
    // exists and is a writable folder
    val parent = f.getParentFile();
    if (!parent.exists()) {
        return new Status(IStatus.ERROR, CitConstants.PluginId,
                String.format("Folder '%s' does not exist.", parent.getName()))
    }

    if (!parent.isDirectory()) {
        return new Status(IStatus.ERROR, CitConstants.PluginId,
                String.format("'%s' is not a folder.", parent.getName()))
    }

    if (!parent.canWrite()) {
        return new Status(IStatus.ERROR, CitConstants.PluginId,
                String.format("'%s' is not writeable.", parent.getName()))
    }

    null
  }
}

object NewArgusProjectWizardPage {
  def validateProjectName(projectName: String): IStatus = {
    if (projectName == null || projectName.length() == 0) {
      return new Status(IStatus.ERROR, CitConstants.PluginId,
              "Project name must be specified")
    } else {
      val workspace = ResourcesPlugin.getWorkspace()
      val nameStatus = workspace.validateName(projectName, IResource.PROJECT)
      if (!nameStatus.isOK()) {
        return nameStatus
      } else {
        // Note: the case-sensitiveness of the project name matters and can cause a
        // conflict *later* when creating the project resource, so let's check it now.
        for (existingProj <- workspace.getRoot().getProjects()) {
          if (projectName.equalsIgnoreCase(existingProj.getName())) {
            return new Status(IStatus.ERROR, CitConstants.PluginId,
                    "A project with that name already exists in the workspace")
          }
        }
      }
    }
    null
  }
  
  private def validateLocation(mValues: NewArgusProjectWizardState): IStatus = {

    // Validate location
    val path = new Path(new File(mValues.projectLocation).getPath())
    if (!mValues.useDefaultLocation) {
      // If not using the default value validate the location.
      val uri = URIUtil.toURI(path.toOSString())
      val workspace = ResourcesPlugin.getWorkspace()
      val handle = workspace.getRoot().getProject(mValues.projectName)
      val locationStatus = workspace.validateProjectLocationURI(handle, uri)
      if (!locationStatus.isOK()) {
          return locationStatus
      }
      // The location is valid as far as Eclipse is concerned (i.e. mostly not
      // an existing workspace project.) Check it either doesn't exist or is
      // a directory that is empty.
      val f = path.toFile()
      if (f.exists() && !f.isDirectory()) {
        return new Status(IStatus.ERROR, CitConstants.PluginId,
                "A directory name must be specified.")
      } else if (f.isDirectory()) {
        // However if the directory exists, we should put a
        // warning if it is not empty. We don't put an error
        // (we'll ask the user again for confirmation before
        // using the directory.)
        val l = f.list()
        if (l != null && l.length != 0) {
          return new Status(IStatus.WARNING, CitConstants.PluginId,
                  "The selected output directory is not empty.")
        }
      } else {
        // Otherwise validate the path string is not empty
        if (new File(mValues.projectLocation).getPath().length() == 0) {
          return new Status(IStatus.ERROR, CitConstants.PluginId,
                  "A directory name must be specified.")
        }
        val dest = path.toFile()
        if (dest.exists()) {
          return new Status(IStatus.ERROR, CitConstants.PluginId,
                  String.format(
                          "There is already a file or directory named \"%1$s\" in the selected location.",
                  mValues.projectName))
        }
      }
    } else {
      // Must be an existing directory
      val f = path.toFile()
      if (!f.isDirectory()) {
        return new Status(IStatus.ERROR, CitConstants.PluginId,
                "An existing directory name must be specified.")
      }
  
      // Check there's an android manifest in the directory
      val osPath = path.append(SdkConstants.FN_ANDROID_MANIFEST_XML).toOSString()
      val manifestFile = new File(osPath)
      if (!manifestFile.isFile()) {
        return new Status(IStatus.ERROR, CitConstants.PluginId,
                String.format(
                        "Choose a valid Android code directory\n" +
                        "(%1$s not found in %2$s.)",
                        SdkConstants.FN_ANDROID_MANIFEST_XML, f.getName()))
      }
  
      // Parse it and check the important fields.
      val manifestData = AndroidManifestHelper.parseForData(osPath)
      if (manifestData == null) {
        return new Status(IStatus.ERROR, CitConstants.PluginId,
                String.format("File %1$s could not be parsed.", osPath))
      }
      val packageName = manifestData.getPackage()
      if (packageName == null || packageName.length() == 0) {
        return new Status(IStatus.ERROR, CitConstants.PluginId,
                String.format("No package name defined in %1$s.", osPath))
      }
  
      // If there's already a .project, tell the user to use import instead.
      if (path.append(".project").toFile().exists()) {  //$NON-NLS-1$
        return new Status(IStatus.WARNING, CitConstants.PluginId,
                "An Eclipse project already exists in this directory.\n" +
                "Consider using File > Import > Existing Project instead.")
      }
    }

    return null
  }
  
  def validateLocationInWorkspace(values: NewArgusProjectWizardState): IStatus = {
    if (values.useDefaultLocation) {
      return null
    }

    // Validate location
    if (values.projectName != null) {
      val dest = Platform.getLocation().append(values.projectName).toFile()
      if (dest.exists()) {
        return new Status(IStatus.ERROR, CitConstants.PluginId, String.format(
           "There is already a file or directory named \"%1$s\" in the selected location.",
                values.projectName))
      }
    }

    null
  }
}