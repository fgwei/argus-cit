package org.arguside.core

object CitConstants {

  // Eclipse ids

  final val PluginId = "org.argus-ide.cit.core"
  final val EditorId = "argus.tools.eclipse.JawaSourceFileEditor"
  final val ArgusPerspectiveId = "org.argus-ide.cit.core.perspective"

  final val BuilderId = "org.argus-ide.cit.core.jawabuilder"
  
  // project nature
  final val NatureId = "org.argus-ide.cit.core.argusnature"

  // marker ids
  final val ProblemMarkerId = "org.argus-ide.cit.core.problem"
  final val ClasspathProblemMarkerId = "org.argus-ide.cit.core.classpathProblem"
  final val SettingProblemMarkerId = "org.scala-ide.sdt.core.settingProblem"
  final val TaskMarkerId = "org.argus-ide.cit.core.task"
  /** All Argus error markers. */
  final val ArgusErrorMarkerIds = Set(ClasspathProblemMarkerId, ProblemMarkerId, SettingProblemMarkerId)
  
  // wizards
  final val ClassWizId = "org.scala-ide.sdt.core.wizards.newClass"
  final val ProjectWizId = "org.scala-ide.sdt.core.wizards.newProject"

  // file extensions
  final val PilarFileExtn = ".pilar"
  final val PilarFileExtnShort = ".plr"
  final val JavaFileExtn = ".java"

  final val IssueTracker = "https://github.com/fgwei/argus-ide/issues"

  // dependency libs
  final val MAVEN_SUPPORT_V4 = "support-v4"   //$NON-NLS-1$
  final val MAVEN_SUPPORT_V13 = "support-v13" //$NON-NLS-1$
  final val MAVEN_APPCOMPAT = "appcompat-v7"  //$NON-NLS-1$
  
}
