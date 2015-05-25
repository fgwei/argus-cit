package org.arguside.core.internal.jdt.model

import java.util.{ HashMap => JHashMap }
import java.util.{ Map => JMap }
import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IResource
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.jdt.core.IBuffer
import org.eclipse.jdt.core.ICompilationUnit
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.core.IType
import org.eclipse.jdt.core.WorkingCopyOwner
import org.eclipse.jdt.core.compiler.IProblem
import org.eclipse.jdt.internal.core.util.HandleFactory
import org.eclipse.jdt.internal.core.BufferManager
import org.eclipse.jdt.internal.core.{ CompilationUnit => JDTCompilationUnit }
import org.eclipse.jdt.internal.core.OpenableElementInfo
import org.eclipse.jdt.internal.core.PackageFragment
import org.eclipse.jdt.internal.corext.util.JavaModelUtil
import org.eclipse.swt.widgets.Display
import argus.tools.eclipse.contribution.weaving.jdt.IJawaSourceFile
import org.eclipse.jdt.core.compiler.CharOperation
import org.arguside.core.extensions.SourceFileProvider
import org.eclipse.jdt.core.JavaModelException
import org.eclipse.core.runtime.IPath
import org.eclipse.core.runtime.NullProgressMonitor
import scala.util.control.Exception
import org.eclipse.core.runtime.CoreException
import org.arguside.core.compiler.InteractiveCompilationUnit
import org.sireum.jawa.sjc.io.AbstractFile
import org.arguside.core.resources.EclipseFile
import org.sireum.jawa.sjc.io.VirtualFile
import org.sireum.jawa.sjc.interactive.Response
import org.arguside.core.compiler.JawaCompilationProblem
import org.arguside.core.internal.ArgusPlugin
import argus.tools.eclipse.contribution.weaving.jdt.ArgusJDTWeavingPlugin
import org.eclipse.jdt.core.IJavaModelMarker

class JawaSourceFileProvider extends SourceFileProvider {
  override def createFrom(path: IPath): Option[InteractiveCompilationUnit] =
    JawaSourceFile.createFromPath(path.toString)
}

object JawaSourceFile {

  /** Considering [[org.eclipse.jdt.internal.core.util.HandleFactory]] isn't thread-safe, and because
   *  `ScalaSourceFile#createFromPath` can be called concurrently from different threads, using a
   *  `ThreadLocal` ensures that a `HandleFactory` instance is never shared across threads.
   */
  private val handleFactory: ThreadLocal[HandleFactory] = new ThreadLocal[HandleFactory] {
    override protected def initialValue(): HandleFactory = new HandleFactory
  }

  /** Creates a jawa source file handle if the given resource path points to a pilar source.
   *  The resource path is a path to a Jawa source file in the workbench (e.g. /Proj/a/b/c/Foo.pilar).
   *
   *  @note This assumes that the resource path is the toString() of an `IPath`.
   *
   *  @param path Is a path to a Jawa source file in the workbench.
   */
  def createFromPath(path: String): Option[JawaSourceFile] = {
    if (!path.endsWith(".pilar") && !path.endsWith(".plr"))
      None
    else {
      // Always `null` because `handleFactory.createOpenable` is only called to open source files, and the `scope` is not needed for this.
      val unusedScope = null
      val source = handleFactory.get().createOpenable(path, unusedScope)
      source match {
        case ssf : JawaSourceFile => Some(ssf)
        case _ => None
      }
    }
  }
}

class JawaSourceFile(fragment : PackageFragment, elementName: String, workingCopyOwner : WorkingCopyOwner)
  extends JDTCompilationUnit(fragment, elementName, workingCopyOwner) with JawaCompilationUnit with IJawaSourceFile {
  
  override def getMainTypeName : Array[Char] = {
    val en = getElementName
    if (en endsWith ".pilar") {
      en.substring(0, en.length - ".pilar".length).toCharArray()
    } else en.substring(0, en.length - ".plr".length).toCharArray()
  }

  /** Schedule this source file for reconciliation. Add the file to
   *  the loaded files managed by the presentation compiler.
   */
  override def initialReconcile(): Response[Unit] = {
    val reloaded = super.initialReconcile()
    this.reconcile(
        ICompilationUnit.NO_AST,
        false /* don't force problem detection */,
        null /* use primary owner */,
        null /* no progress monitor */);

    reloaded
  }

  /* getProblems should be reserved for a Java context, @see getProblems */
  def reconcile(newContents: String): List[JawaCompilationProblem] = {
    super.forceReconcile()
  }

  override def reconcile(
      astLevel : Int,
      reconcileFlags : Int,
      workingCopyOwner : WorkingCopyOwner,
      monitor : IProgressMonitor) : org.eclipse.jdt.core.dom.CompilationUnit = {
    /* This explicit call to super matters, presumably exercised
      through AspectJ. See #1002016. */
    super.reconcile(ICompilationUnit.NO_AST, reconcileFlags, workingCopyOwner, monitor)
  }

  override def makeConsistent(
    astLevel : Int,
    resolveBindings : Boolean,
    reconcileFlags : Int,
    problems : JHashMap[_,_],
    monitor : IProgressMonitor) : org.eclipse.jdt.core.dom.CompilationUnit = {
    // don't rerun this expensive operation unless necessary
    if (!isConsistent()) {
      val info = createElementInfo.asInstanceOf[OpenableElementInfo]
      openWhenClosed(info, true, monitor)
    }
    null
  }

  override def codeSelect(offset : Int, length : Int, workingCopyOwner : WorkingCopyOwner) : Array[IJavaElement] =
    codeSelect(this, offset, length, workingCopyOwner)

  override lazy val file : AbstractFile = {
    val res = try { getCorrespondingResource } catch { case _: JavaModelException => null }
    res match {
      case f : IFile => new EclipseFile(f)
      case _ => new VirtualFile(getElementName, getPath.toString)
    }
  }
  
  /** Implementing the weaving interface requires to return `null` for an empty array. */
  override def getProblems: Array[IProblem] = {
    val probs = currentProblems()
    if (probs.isEmpty) null else probs.toArray
    null
  }

  override def getType(name : String) : IType = {
    new LazyToplevelClass(this, name)
  }

  override def getContents() : Array[Char] = {
    // in the following case, super#getContents() logs an exception for no good reason
    if (getBufferManager().getBuffer(this) == null && getResource().getLocation() == null && getResource().getLocationURI() == null) {
      return CharOperation.NO_CHAR
    }
    Exception.failAsValue(classOf[CoreException])(CharOperation.NO_CHAR) { super.getContents() }
  }

  /** Makes sure {{{this}}} source is not in the ignore buffer of the compiler and ask the compiler to reload it. */
  final def forceReload(): Unit ={
    argusProject.presentationCompiler { compiler =>
      compiler.askToDoFirst(this)
      reload()
    }
  }

  /** Ask the compiler to reload {{{this}}} source. */
  final def reload(): Unit = {
    argusProject.presentationCompiler { _.askReload(this, sourceFile) }
  }

  /** Ask the compiler to discard {{{this}}} source. */
  final def discard(): Unit = {
    argusProject.presentationCompiler { _.discardCompilationUnit(this) }
  }
  getUnderlyingResource.deleteMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE)
}
