package org.arguside.core

object CitConstants {

  // Eclipse ids

  final val PluginId = "org.argus-ide.cit.core"
  final val EditorId = "argus.tools.eclipse.JawaSourceFileEditor"
  final val ArgusPerspectiveId = "org.argus-ide.cit.core.perspective"

  final val BuilderId = "org.argus-ide.cit.core.jawabuilder"
  
  // project nature
  final val NatureId = "org.argus-ide.cit.core.argusnature"

    // ZW: containers.... to see how to make it works
  final val ArgusLibContId = "org.argus-ide.sdt.launching.ARGUS_CONTAINER"
  final val ArgusCompilerContId = "org.argus-ide.sdt.launching.ARGUS_COMPILER_CONTAINER"

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
  
}
