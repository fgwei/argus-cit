 package org.arguside.core.internal.builder

import org.eclipse.core.resources.IResource
import org.eclipse.core.resources.IMarker
import org.arguside.core.resources.MarkerFactory
import org.arguside.core.resources.MarkerFactory.Position
import org.arguside.core.resources.MarkerFactory.NoPosition
import org.arguside.core.resources.MarkerFactory.RegionPosition
import org.arguside.core.IArgusProject
import org.sireum.jawa.sjc.util.{ Position => JawaPosition }
import org.arguside.core.CitConstants

/** Factory for creating markers used to report build problems (i.e., compilation errors). */
object BuildProblemMarker extends MarkerFactory(CitConstants.ProblemMarkerId) {
  /** Create a marker indicating an error state for the passed Argus `project`. */
  def create(project: IArgusProject, e: Throwable): Unit =
    create(project.underlying, "Error in Jawa compiler: " + e.getMessage)

  /** Create a marker indicating an error state for the passed `resource`. */
  def create(resource: IResource, msg: String): Unit =
    create(resource, IMarker.SEVERITY_ERROR, msg)

  /** Create marker with a source position in the Problem view.
   *  @param resource The resource to use to create the marker (hence, the marker will be associated to the passed resource)
   *  @param severity Indicates the marker's error state. Its value can be one of:
   *                 [IMarker.SEVERITY_ERROR, IMarker.SEVERITY_WARNING, IMarker.SEVERITY_INFO]
   *  @param msg      The text message displayed by the marker. Note, the passed message is truncated to 21000 chars.
   *  @param pos      The source position for the marker.
   */
  def create(resource: IResource, severity: Int, msg: String, pos: JawaPosition): Unit =
    create(resource, severity, msg, position(pos))

  private def position(pos: JawaPosition): Position = {
    if (pos.isDefined) {
      val source = pos.source
      val length = source.identifier(pos).map(_.length).getOrElse(0)
      RegionPosition(pos.point, length, pos.line)
    } else NoPosition
  }
}
