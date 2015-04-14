package org.arguside.core.internal.jdt.util

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OutputStream
import java.io.Serializable
import org.eclipse.core.runtime.IPath
import org.eclipse.core.runtime.Path
import org.arguside.core.internal.project.ArgusInstallation
import org.arguside.core.internal.project.LabeledArgusInstallation
import org.arguside.core.internal.project.ArgusInstallationLabel
import org.arguside.core.internal.project.ArgusModule
import org.arguside.core.internal.ArgusPlugin
import sbt.ScalaInstance
import java.net.URLClassLoader
import java.io.ObjectStreamClass
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import org.eclipse.core.runtime.CoreException

class ContextualizedObjectInputStream(in: InputStream) extends ObjectInputStream(in) {

  override def resolveClass(desc: ObjectStreamClass) = {

    val res = Try[Class[_]](Thread.currentThread().getContextClassLoader().loadClass(desc.getName()))
    res match {
      case Success(cl) => cl
      case Failure(thrown) => throw new IllegalAccessException("Something went horribly wrong deserializing")
    }
  }

}

object LabeledScalaInstallationsSaveHelper {

  def readInstallations(input: InputStream): List[LabeledArgusInstallation] = {
    val is = new ContextualizedObjectInputStream(new BufferedInputStream(input)) {
      enableResolveObject(true)

      override protected def resolveObject(o: Object): Object = o match {
        case i: PathReplace => i.getPath()
        case i: LabeledArgusInstallationReplace => i.getLabeledScalaInstallation()
        case i: ArgusModuleReplace => i.getScalaModule()
        case _ => super.resolveObject(o)
      }
    }

    val res = is.readObject().asInstanceOf[List[LabeledArgusInstallation]]
    res
  }

  def writeInstallations(scalaInstallations: Seq[ArgusInstallation], output: OutputStream): Unit = {
    val os = new ObjectOutputStream(new BufferedOutputStream(output)) {
      enableReplaceObject(true)

      override protected def replaceObject(o: Object): Object = o match {
        case i: ArgusModule => new ArgusModuleReplace(i)
        case i: LabeledArgusInstallation => new LabeledArgusInstallationReplace(i)
        case e: IPath => new PathReplace(e)
        case _ => super.replaceObject(o)
      }
    }

    os.writeObject(scalaInstallations)
    os.flush()
  }

  /** A ScalaModule replacement used for object serialization
   */
  @SerialVersionUID(1001667379327078799L)
  class ArgusModuleReplace(val classJar: IPath, val srcJar: Option[IPath]) extends Serializable {
    def this(sm: ScalaModule) = this(sm.classJar, sm.sourceJar)
    def getScalaModule() = ScalaModule(classJar, srcJar)
  }

  /** A ScalaInstallation replacement used for object serialization
   */
  @SerialVersionUID(3901667379327078799L)
  class LabeledArgusInstallationReplace(val name: ArgusInstallationLabel, val compilerMod: ArgusModule, val libraryMod: ScalaModule, val extraJarsMods: Seq[ArgusModule]) extends Serializable {
    def this(ins: LabeledArgusInstallation) = this(ins.label, ins.compiler, ins.library, ins.extraJars)
    def getLabeledScalaInstallation() = new LabeledArgusInstallation() {
      override def label = name
      override def compiler = compilerMod
      override def library = libraryMod
      override def extraJars = extraJarsMods
      override def version = ArgusInstallation.extractVersion(library.classJar).getOrElse(NoScalaVersion)
    }
  }

  /** An IPath replacement used for object serialization
   */
  @SerialVersionUID(-2361259525684491181L)
  class PathReplace(val path: String) extends Serializable {
    def this(ip: IPath) = this(ip.toPortableString())
    def getPath() = Path.fromPortableString(path)
  }

}
