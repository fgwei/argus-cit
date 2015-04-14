package org.arguside.core.internal

import org.eclipse.jdt.core.IJavaProject
import scala.collection.mutable
import scala.util.control.ControlThrowable
import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IResourceChangeEvent
import org.eclipse.core.resources.IResourceChangeListener
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Platform
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.content.IContentTypeSettings
import org.eclipse.jdt.core.ElementChangedEvent
import org.eclipse.jdt.core.IElementChangedListener
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.core.IJavaElementDelta
import org.eclipse.jdt.core.IPackageFragmentRoot
import org.eclipse.jdt.internal.core.JavaModel
import org.eclipse.jdt.internal.core.JavaProject
import org.eclipse.jdt.internal.core.PackageFragment
import org.eclipse.jdt.internal.core.PackageFragmentRoot
import org.eclipse.jdt.internal.core.util.Util
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.graphics.Color
import org.eclipse.ui.IEditorInput
import org.eclipse.ui.IFileEditorInput
import org.eclipse.ui.PlatformUI
import org.eclipse.ui.IPartListener
import org.eclipse.ui.IWorkbenchPart
import org.eclipse.ui.IWorkbenchPage
import org.eclipse.ui.IPageListener
import org.eclipse.ui.IEditorPart
import org.eclipse.ui.part.FileEditorInput
import org.eclipse.ui.plugin.AbstractUIPlugin
import org.osgi.framework.BundleContext
import org.arguside.core.internal.jdt.model.ScalaSourceFile
import org.arguside.util.eclipse.OSGiUtils
import org.arguside.ui.internal.templates.ScalaTemplateManager
import org.eclipse.jdt.ui.PreferenceConstants
import org.eclipse.core.resources.IResourceDelta
import org.arguside.logging.HasLogger
import org.osgi.framework.Bundle
import org.eclipse.jdt.core.ICompilationUnit
import org.arguside.core.resources.EclipseResource
import org.arguside.logging.PluginLogConfigurator
import org.arguside.core.internal.project.ArgusProject
import org.arguside.ui.internal.diagnostic
import org.arguside.util.internal.CompilerUtils
import org.arguside.core.internal.builder.zinc.CompilerInterfaceStore
import org.arguside.util.internal.FixedSizeCache
import org.arguside.core.IScalaInstallation
import org.arguside.core.internal.project.ScalaInstallation.platformInstallation
import org.eclipse.core.runtime.content.IContentType
import org.arguside.core.CitConstants
import org.arguside.ui.internal.migration.RegistryExtender
import org.arguside.core.IArgusPlugin
import org.eclipse.core.resources.IResourceDeltaVisitor
import org.arguside.util.Utils._
import org.arguside.core.internal.jdt.model.ScalaCompilationUnit
import org.arguside.ui.internal.editor.ScalaDocumentProvider
import org.arguside.core.internal.jdt.model.ScalaClassFile
import org.eclipse.jdt.core.IClassFile
import org.arguside.util.Utils.WithAsInstanceOfOpt

object ArgusPlugin {

  @volatile private var plugin: ArgusPlugin = _

  def apply(): ArgusPlugin = plugin

}

class ArgusPlugin extends IArgusPlugin with PluginLogConfigurator with IResourceChangeListener with IElementChangedListener with HasLogger {
//  import CompilerUtils.{ ShortScalaVersion, isBinaryPrevious, isBinarySame }

  import org.arguside.core.CitConstants._

   /** Check if the given version is compatible with the current plug-in version.
   *  Check on the major/minor number, discard the maintenance number.
   *
   *  For example 2.9.1 and 2.9.2-SNAPSHOT are compatible versions whereas
   *  2.8.1 and 2.9.0 aren't.
   */
//  def isCompatibleVersion(version: ScalaVersion, project: ScalaProject): Boolean = {
//    if (project.isUsingCompatibilityMode())
//      isBinaryPrevious(ScalaVersion.current, version)
//    else
//      isBinarySame(ScalaVersion.current, version)// don't treat 2 unknown versions as equal
//  }

  private lazy val citCoreBundle = getBundle()

//  lazy val sbtCompilerBundle = Platform.getBundle(SbtPluginId)
//  lazy val sbtCompilerInterfaceBundle = Platform.getBundle(SbtCompilerInterfacePluginId)
//  lazy val sbtCompilerInterface = OSGiUtils.pathInBundle(sbtCompilerInterfaceBundle, "/")

  lazy val templateManager = new PilarTemplateManager()

  lazy val pilarSourceFileContentType: IContentType =
    Platform.getContentTypeManager().getContentType("argus.tools.eclipse.pilarSource")

  lazy val pilarClassFileContentType: IContentType =
    Platform.getContentTypeManager().getContentType("argus.tools.eclipse.pilarClass")

  /**
   * The document provider needs to exist only a single time because it caches
   * compilation units (their working copies). Each `PilarSourceFileEditor` is
   * associated with this document provider.
   */
  private[arguside] lazy val documentProvider = new PilarDocumentProvider

  override def start(context: BundleContext) = {
    ArgusPlugin.plugin = this
    super.start(context)

    if (!headlessMode) {
      PlatformUI.getWorkbench.getEditorRegistry.setDefaultEditor("*.pilar", CitConstants.EditorId)
      PlatformUI.getWorkbench.getEditorRegistry.setDefaultEditor("*.plr", CitConstants.EditorId)
      diagnostic.StartupDiagnostics.run

      new RegistryExtender().perform()
    }
    ResourcesPlugin.getWorkspace.addResourceChangeListener(this, IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.POST_CHANGE)
    JavaCore.addElementChangedListener(this)
    logger.info("Scala compiler bundle: " + platformInstallation.compiler.classJar.toOSString() )
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

  /** The compiler-interface store, located in this plugin configuration area (usually inside the metadata directory */
  lazy val compilerInterfaceStore: CompilerInterfaceStore = new CompilerInterfaceStore(Platform.getStateLocation(sdtCoreBundle), this)

  /** A LRU cache of class loaders for Scala builders */
  lazy val classLoaderStore: FixedSizeCache[IScalaInstallation,ClassLoader] = new FixedSizeCache(initSize = 2, maxSize = 3)

  // Scala project instances
  private val projects = new mutable.HashMap[IProject, ScalaProject]

  override def scalaCompilationUnit(input: IEditorInput): Option[ScalaCompilationUnit] = {
    def unitOfSourceFile = Option(documentProvider.getWorkingCopy(input).asInstanceOf[ScalaCompilationUnit])

    def unitOfClassFile = input.getAdapter(classOf[IClassFile]) match {
      case tr: ScalaClassFile => Some(tr)
      case _                  => None
    }

    unitOfSourceFile orElse unitOfClassFile
  }

  def getJavaProject(project: IProject) = JavaCore.create(project)

  override def getArgusProject(project: IProject): ArgusProject = projects.synchronized {
    projects.get(project) getOrElse {
      val scalaProject = ScalaProject(project)
      projects(project) = scalaProject
      scalaProject
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
      projects.get(project) foreach { (scalaProject) =>
        projects.remove(project)
        scalaProject.dispose()
      }
    }
  }

  /** Restart all presentation compilers in the workspace. Need to do it in order
   *  for them to pick up the new std out/err streams.
   */
  def resetAllPresentationCompilers() {
    for {
      iProject <- ResourcesPlugin.getWorkspace.getRoot.getProjects
      if iProject.isOpen
      scalaProject <- asScalaProject(iProject)
    } scalaProject.presentationCompiler.askRestart()
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
            // that particular classpath check can set the Installation (used, e.g., for sbt-eclipse imports)
            // setting the Installation triggers a recursive check
            asScalaProject(r) foreach { (p) =>
              try {
                // It's important to save this /before/ checking classpath : classpath
                // checks create their own preference modifications under some conditions.
                // Doing them concurrently can wreak havoc.
                p.projectSpecificStorage.save()
              } finally {
                p.checkClasspath(true)
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
    val buff = new ListBuffer[ScalaSourceFile]
    val changed = new ListBuffer[ICompilationUnit]
    val projectsToReset = new mutable.HashSet[ScalaProject]

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
            asScalaProject(elem.getJavaProject().getProject).foreach(projectsToReset += _)
          }
          !hasContentChanged

        case PACKAGE_FRAGMENT =>
          val hasContentChanged = isAdded || isRemoved
          if (hasContentChanged) {
            logger.debug("package fragment added or removed: " + elem.getElementName())
            asScalaProject(elem.getJavaProject().getProject).foreach(projectsToReset += _)
          }
          // stop recursion here, we need to reset the PC anyway
          !hasContentChanged

        // TODO: the check should be done with isInstanceOf[ScalaSourceFile] instead of
        // endsWith(scalaFileExtn), but it is not working for Play 2.0 because of #1000434
        case COMPILATION_UNIT if isChanged && elem.getResource != null && (elem.getResource.getName.endsWith(PilarFileExtn) || elem.getResource.getName.endsWith(PilarFileExtnShort)) =>
          val hasContentChanged = hasFlag(IJavaElementDelta.F_CONTENT)
          if (hasContentChanged)
            // mark the changed Scala files to be refreshed in the presentation compiler if needed
            changed += elem.asInstanceOf[ICompilationUnit]
          false

        case COMPILATION_UNIT if elem.isInstanceOf[ScalaSourceFile] && isRemoved =>
          buff += elem.asInstanceOf[ScalaSourceFile]
          false

        case COMPILATION_UNIT if isAdded =>
          logger.debug("added compilation unit " + elem.getElementName())
          asScalaProject(elem.getJavaProject().getProject).foreach(projectsToReset += _)
          false

        case _ =>
          false
      }

      if (processChildren)
        delta.getAffectedChildren foreach findRemovedSources
    }
    findRemovedSources(event.getDelta)

    // ask for the changed scala files to be refreshed in each project presentation compiler if needed
    if (changed.nonEmpty) {
      changed.toList groupBy (_.getJavaProject.getProject) foreach {
        case (project, units) =>
          asScalaProject(project) foreach { p =>
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
          asScalaProject(project) foreach { p =>
            if (project.isOpen && !projectsToReset(p))
              p.presentationCompiler.internal (_.filesDeleted(srcs))
          }
      }
    }
  }

}