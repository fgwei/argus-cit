package org.arguside.debug.internal.launching

import java.util.{ List => JList }
import java.util.{ Map => JMap }
import org.arguside.debug.internal.ArgusDebugPlugin
import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import org.eclipse.debug.core.ILaunch
import org.eclipse.jdi.Bootstrap
import org.eclipse.jdt.launching.IVMConnector
import com.sun.jdi.connect.AttachingConnector
import com.sun.jdi.connect.Connector
import org.arguside.debug.internal.model.ArgusDebugTarget
import java.io.IOException
import com.sun.jdi.connect.TransportTimeoutException
import org.eclipse.jdi.TimeoutException

/**
 * Attach connector creating a Scala debug session.
 * Added to the platform through extension point.
 */
class SocketAttachConnectorArgus extends IVMConnector with SocketConnectorArgus {
  import SocketConnectorArgus._

  override def connector(): AttachingConnector = {
    import scala.collection.JavaConverters._
    Bootstrap.virtualMachineManager().attachingConnectors().asScala.find(_.name() == SocketAttachName).getOrElse(
        throw ArgusDebugPlugin.wrapInCoreException("Unable to find JDI AttachingConnector", null))
  }

  // from org.eclipse.jdt.launching.IVMConnector

  override val getArgumentOrder: JList[String] = {
    import scala.collection.JavaConverters._
    List(HostnameKey, PortKey).asJava
  }

  override val getIdentifier: String = ArgusDebugPlugin.id + ".socketAttachConnector"

  override def getName(): String = "Scala debugger (Socket Attach)"

  override def connect(params: JMap[String, String], monitor: IProgressMonitor, launch: ILaunch): Unit = {

    val arguments = generateArguments(params)

    try {
      // connect and create the debug session
      val virtualMachine = connector.attach(arguments)
      val target = ArgusDebugTarget(virtualMachine, launch, null, allowDisconnect = true, allowTerminate = allowTerminate(launch))
      target.attached() // tell the debug target to initialize
    } catch {
      case e: TimeoutException =>
        throw ArgusDebugPlugin.wrapInCoreException("Unable to connect to the remote VM", e)
      case e: IOException =>
        throw ArgusDebugPlugin.wrapInCoreException("Unable to connect to the remote VM", e)
    }
  }

  // ------------

}