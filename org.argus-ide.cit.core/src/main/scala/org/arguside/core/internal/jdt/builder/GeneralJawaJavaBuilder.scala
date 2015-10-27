package org.arguside.core.internal.jdt.builder

import org.eclipse.core.resources.IProject
import org.eclipse.core.internal.resources.BuildConfiguration
import org.arguside.util.internal.ReflectionUtils
import argus.tools.eclipse.contribution.weaving.jdt.builderoptions.JawaJavaBuilder

/**
 *
 *  The JavaBuilder in 3.7 introduces build configurations, so instead of setting
 *  the current project we need to create a BuildConfiguration and pass that around.
 *
 */
class GeneralJawaJavaBuilder extends JawaJavaBuilder {
  // (Indigo) this sets a dummy BuildConfiguration and avoids an NPE in InternalBuilder.getProject
  setProject0(null)

  override def setProject0(project: IProject) {
    JawaJavaBuilderUtils.setBuildConfig(this, project)
  }
}

object JawaJavaBuilderUtils extends ReflectionUtils {
  private lazy val ibClazz = Class.forName("org.eclipse.core.internal.events.InternalBuilder")

  private lazy val jbClazz = Class.forName("org.eclipse.jdt.internal.core.builder.JavaBuilder")
  private lazy val initializeBuilderMethod = getDeclaredMethod(jbClazz, "initializeBuilder", classOf[Int], classOf[Boolean])

  private lazy val IBuildConfigClass = Class.forName("org.eclipse.core.resources.IBuildConfiguration")
  private lazy val setBuildConfigMethod = getDeclaredMethod(ibClazz, "setBuildConfig", IBuildConfigClass)

  def initializeBuilder(builder : JawaJavaBuilder, kind : Int, forBuild : Boolean) = initializeBuilderMethod.invoke(builder, int2Integer(kind), boolean2Boolean(forBuild))

  def setBuildConfig(builder: JawaJavaBuilder, project: IProject) {
    val buildConfig = new BuildConfiguration(project)
    setBuildConfigMethod.invoke(builder, buildConfig)
  }
}
