package org.arguside.core.internal

import org.arguside.core.IArgusPlugin
import org.eclipse.core.resources.IResourceChangeListener
import org.eclipse.jdt.core.IElementChangedListener
import org.eclipse.core.runtime.content.IContentType
import org.eclipse.core.runtime.Platform
import org.osgi.framework.BundleContext
import org.eclipse.ui.PlatformUI
import org.arguside.core.CitConstants
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.resources.IResourceChangeEvent
import org.eclipse.jdt.core.JavaCore
import org.sireum.util._
import org.eclipse.core.resources.IProject
import org.eclipse.ui.IEditorInput
import org.eclipse.jdt.core.IClassFile
import org.eclipse.core.resources.IResourceDeltaVisitor
import org.eclipse.core.resources.IResourceDelta
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.core.IJavaElementDelta
import org.eclipse.jdt.core.ElementChangedEvent
import org.arguside.core.internal.project.ArgusProject
import org.arguside.util.Utils.WithAsInstanceOfOpt
import org.eclipse.jdt.core.IJavaProject
import org.arguside.logging.HasLogger
import org.arguside.logging.PluginLogConfigurator
import org.arguside.ui.internal.editor.JawaDocumentProvider
import org.eclipse.jdt.core.ICompilationUnit
import org.arguside.core.internal.jdt.model.JawaSourceFile
import org.arguside.core.internal.jdt.model.JawaClassFile
import org.arguside.ui.internal.diagnostic
import org.arguside.core.internal.jdt.model.JawaCompilationUnit
import org.eclipse.core.resources.IFile

object ArgusPlugin {

  @volatile private var plugin: ArgusPlugin = _

  def apply(): ArgusPlugin = plugin

}

class ArgusPlugin extends IArgusPlugin with PluginLogConfigurator with IResourceChangeListener with IElementChangedListener with HasLogger {
//  import CompilerUtils.{ ShortArgusVersion, isBinaryPrevious, isBinarySame }

  import org.arguside.core.CitConstants._

  private lazy val citCoreBundle = getBundle()

  lazy val jawaSourceFileContentType: IContentType =
    Platform.getContentTypeManager().getContentType("argus.tools.eclipse.jawaSource")

  lazy val jawaClassFileContentType: IContentType =
    Platform.getContentTypeManager().getContentType("argus.tools.eclipse.jawaClass")

  /**
   * The document provider needs to exist only a single time because it caches
   * compilation units (their working copies). Each `JawaSourceFileEditor` is
   * associated with this document provider.
   */
  private[arguside] lazy val documentProvider = new JawaDocumentProvider

  override def start(context: BundleContext) = {
    ArgusPlugin.plugin = this
    super.start(context)

    PlatformUI.getWorkbench.getEditorRegistry.setDefaultEditor("*.pilar", CitConstants.EditorId)
    PlatformUI.getWorkbench.getEditorRegistry.setDefaultEditor("*.plr", CitConstants.EditorId)
    diagnostic.StartupDiagnostics.run
    ResourcesPlugin.getWorkspace.addResourceChangeListener(this, IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.POST_CHANGE)
    JavaCore.addElementChangedListener(this)
  }

  override def stop(context: BundleContext) = {
    ResourcesPlugin.getWorkspace.removeResourceChangeListener(this)
    for {
      iProject <- ResourcesPlugin.getWorkspace.getRoot.getProjects
      if iProject.isOpen
      argusProject <- asArgusProject(iProject)
    } argusProject.projectSpecificStorage.save()
    super.stop(context)
    ArgusPlugin.plugin = null
  }

  // Argus project instances
  private val projects: MMap[IProject, ArgusProject] = mmapEmpty

  override def jawaCompilationUnit(input: IEditorInput): Option[JawaCompilationUnit] = {
    def unitOfSourceFile = Option(documentProvider.getWorkingCopy(input).asInstanceOf[JawaCompilationUnit])

    def unitOfClassFile = input.getAdapter(classOf[IClassFile]) match {
      case tr: JawaClassFile => Some(tr)
      case _                  => None
    }

    unitOfSourceFile orElse unitOfClassFile
  }

  def getJavaProject(project: IProject) = JavaCore.create(project)

  override def getArgusProject(project: IProject): ArgusProject = projects.synchronized {
    projects.get(project) getOrElse {
      val argusProject = ArgusProject(project)
      projects(project) = argusProject
      argusProject
    }
  }

  override def asArgusProject(project: IProject): Option[ArgusProject] = {
    if (ArgusProject.isArgusProject(project)) {
      Some(getArgusProject(project))
    } else {
      None
    }
  }

  def disposeProject(project: IProject): Unit = {
    projects.synchronized {
      projects.get(project) foreach { (argusProject) =>
        projects.remove(project)
        argusProject.dispose()
      }
    }
  }

  override def resourceChanged(event: IResourceChangeEvent) {
    (event.getResource, event.getType) match {
      case (project: IProject, IResourceChangeEvent.PRE_CLOSE) =>
        disposeProject(project)
      case _ =>
    }
    (Option(event.getDelta()) foreach (_.accept(new IResourceDeltaVisitor() {
      override def visit(delta: IResourceDelta): Boolean = {
        // This is obtained at project opening or closing, meaning the 'openness' state changed
        if (delta.getFlags == IResourceDelta.OPEN){
          val resource = delta.getResource().asInstanceOfOpt[IProject]
          resource foreach {(r) =>
            asArgusProject(r) foreach { (p) =>
              try {
                p.projectSpecificStorage.save()
              } finally {
              }
            }
          }
          false
        } else
        true
      }
    })))
  }

  override def elementChanged(event: ElementChangedEvent) {
    import scala.collection.mutable.ListBuffer
    import IJavaElement._
    import IJavaElementDelta._
    
    // check if the changes are linked with the build path
    val modelDelta = event.getDelta()
    // check that the notification is about a change (CHANGE) of some elements (F_CHILDREN) of the java model (JAVA_MODEL)
    if (modelDelta.getElement().getElementType() == JAVA_MODEL && modelDelta.getKind() == CHANGED && (modelDelta.getFlags() & F_CHILDREN) != 0) {
      for (innerDelta <- modelDelta.getAffectedChildren()) {
        // check that the notification no the child is about a change (CHANDED) relative to a resolved classpath change (F_RESOLVED_CLASSPATH_CHANGED)
        if (innerDelta.getKind() == CHANGED && (innerDelta.getFlags() & IJavaElementDelta.F_RESOLVED_CLASSPATH_CHANGED) != 0) {
          innerDelta.getElement() match {
            // classpath change should only impact projects
            case javaProject: IJavaProject => {
              asArgusProject(javaProject.getProject()).foreach{ (p) => p.classpathHasChanged(false) }
            }
            case _ =>
          }
        }
      }
    }
    // process deleted files
    val buff: MList[JawaSourceFile] = mlistEmpty
    val changed: MList[ICompilationUnit] = mlistEmpty
    val projectsToReset: MSet[ArgusProject] = msetEmpty

    def findRemovedSources(delta: IJavaElementDelta) {
      val isChanged = delta.getKind == CHANGED
      val isRemoved = delta.getKind == REMOVED
      val isAdded = delta.getKind == ADDED

      def hasFlag(flag: Int) = (delta.getFlags & flag) != 0

      val elem = delta.getElement
      val processChildren: Boolean = elem.getElementType match {
        case JAVA_MODEL =>
          true

        case JAVA_PROJECT if isRemoved =>
          disposeProject(elem.getJavaProject.getProject)
          false

        case JAVA_PROJECT if !hasFlag(F_CLOSED) =>
          true

        case PACKAGE_FRAGMENT_ROOT =>
          val hasContentChanged = isRemoved || hasFlag(F_REMOVED_FROM_CLASSPATH | F_ADDED_TO_CLASSPATH | F_ARCHIVE_CONTENT_CHANGED)
          if (hasContentChanged) {
            logger.info("package fragment root changed (resetting presentation compiler): " + elem.getElementName())
            asArgusProject(elem.getJavaProject().getProject).foreach(projectsToReset += _)
          }
          !hasContentChanged

        case PACKAGE_FRAGMENT =>
          val hasContentChanged = isAdded || isRemoved
          if (hasContentChanged) {
            logger.debug("package fragment added or removed: " + elem.getElementName())
            asArgusProject(elem.getJavaProject().getProject).foreach(projectsToReset += _)
          }
          // stop recursion here, we need to reset the PC anyway
          !hasContentChanged

        // TODO: the check should be done with isInstanceOf[JawaSourceFile]
        case COMPILATION_UNIT if isChanged && elem.getResource != null && (elem.getResource.getName.endsWith(PilarFileExtn) || elem.getResource.getName.endsWith(PilarFileExtnShort)) =>
          val hasContentChanged = hasFlag(IJavaElementDelta.F_CONTENT)
          if (hasContentChanged)
            // mark the changed Argus files to be refreshed in the presentation compiler if needed
            changed += elem.asInstanceOf[ICompilationUnit]
          false

        case COMPILATION_UNIT if elem.isInstanceOf[JawaSourceFile] && isRemoved =>
          buff += elem.asInstanceOf[JawaSourceFile]
          false

        case COMPILATION_UNIT if isAdded =>
          logger.debug("added compilation unit " + elem.getElementName())
          asArgusProject(elem.getJavaProject().getProject).foreach(projectsToReset += _)
          false

        case _ =>
          false
      }

      if (processChildren)
        delta.getAffectedChildren foreach findRemovedSources
    }
    findRemovedSources(event.getDelta)
    // ask for the changed jawa files to be refreshed in each project presentation compiler if needed
    if (changed.nonEmpty) {
      changed.toList groupBy (_.getJavaProject.getProject) foreach {
        case (project, units) =>
          asArgusProject(project) foreach { p =>
            if (project.isOpen && !projectsToReset(p)) {
              p.presentationCompiler(_.refreshChangedFiles(units.map(_.getResource.asInstanceOf[IFile])))
            }
          }
      }
    }
    projectsToReset.foreach(_.presentationCompiler.askRestart())
    if (buff.nonEmpty) {
      buff.toList groupBy (_.getJavaProject.getProject) foreach {
        case (project, srcs) =>
          asArgusProject(project) foreach { p =>
            if (project.isOpen && !projectsToReset(p))
              p.presentationCompiler.internal (_.filesDeleted(srcs))
          }
      }
    }
  }

  def logError(message: String, exception: Throwable): Unit = {
    logger.error(if(message == null) "" else message, exception)
  }
  
  def logError(message: String): Unit = {
    logger.error(if(message == null) "" else message)
  }
  
  def eclipseError(message: String): Unit = {
    eclipseLog.error(message)
  }
  
}