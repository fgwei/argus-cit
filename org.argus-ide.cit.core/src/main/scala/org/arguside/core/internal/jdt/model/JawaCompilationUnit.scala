package org.arguside.core.internal.jdt.model

import java.util.{ Map => JMap }
import scala.concurrent.SyncVar
import org.eclipse.core.internal.filebuffers.SynchronizableDocument
import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IResource
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.jdt.core.BufferChangedEvent
import org.eclipse.jdt.core.CompletionRequestor
import org.eclipse.jdt.core.IBuffer
import org.eclipse.jdt.core.IBufferChangedListener
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.core.IJavaModelStatusConstants
import org.eclipse.jdt.core.IProblemRequestor
import org.eclipse.jdt.core.ITypeRoot
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.JavaModelException
import org.eclipse.jdt.core.WorkingCopyOwner
import org.eclipse.jdt.core.IClassFile
import org.eclipse.jdt.internal.compiler.env
import org.eclipse.jdt.internal.core.BufferManager
import org.eclipse.jdt.internal.core.CompilationUnitElementInfo
import org.eclipse.jdt.internal.core.DefaultWorkingCopyOwner
import org.eclipse.jdt.internal.core.JavaModelStatus
import org.eclipse.jdt.internal.core.JavaProject
import org.eclipse.jdt.internal.core.Openable
import org.eclipse.jdt.internal.core.OpenableElementInfo
import org.eclipse.jdt.internal.core.SearchableEnvironment
import org.eclipse.jdt.internal.core.search.matching.MatchLocator
import org.eclipse.jdt.internal.core.search.matching.PossibleMatch
import org.eclipse.jdt.internal.ui.javaeditor.DocumentAdapter
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.ITextSelection
import argus.tools.eclipse.contribution.weaving.jdt.IJawaCompilationUnit
import org.arguside.ui.ArgusImages
import org.arguside.core.IArgusPlugin
import org.arguside.util.internal.ReflectionUtils
import org.eclipse.jdt.core._
import org.eclipse.jdt.internal.core.JavaElement
import org.eclipse.jdt.internal.core.SourceRefElement
import org.arguside.logging.HasLogger
import org.eclipse.jface.text.source.ISourceViewer
import org.eclipse.ui.texteditor.ITextEditor
import org.arguside.core.internal.hyperlink.BaseHyperlinkDetector
import org.arguside.util.eclipse.EditorUtils
import org.arguside.core.compiler.InteractiveCompilationUnit
import org.arguside.core.internal
import org.arguside.core.internal.ArgusPlugin
import org.eclipse.jdt.internal.corext.fix.LinkedProposalPositionGroup.PositionInformation
import org.sireum.jawa.sjc.io.AbstractFile
import org.arguside.core.internal.hyperlink.DeclarationHyperlinkDetector
import org.sireum.jawa.sjc.util.Position
import org.sireum.util._
import org.arguside.core.compiler.IJawaPresentationCompiler.Implicits._
import org.arguside.core.internal.jdt.search.JawaSourceIndexer

trait JawaCompilationUnit extends Openable
  with env.ICompilationUnit
  with JawaElement
  with IJawaCompilationUnit
  with IBufferChangedListener
  with InteractiveCompilationUnit
  with HasLogger {

  override def argusProject: internal.project.ArgusProject =
    ArgusPlugin().getArgusProject(getJavaProject.getProject)

  override val file : AbstractFile
  
  override def workspaceFile: IFile = getUnderlyingResource.asInstanceOf[IFile]

  override def bufferChanged(e : BufferChangedEvent) {
    if (!e.getBuffer.isClosed)
      argusProject.presentationCompiler(_.scheduleReload(this, sourceFile))

    super.bufferChanged(e)
  }

  /** Ensure the underlying buffer is open. Otherwise, `bufferChanged` events won't be fired,
   *  meaning `askReload` won't be called, and presentation compiler errors won't be reported.
   *
   *  This code is copied from org.eclipse.jdt.internal.core.CompilationUnit
   */
  private def ensureBufferOpen(info: OpenableElementInfo, pm: IProgressMonitor) {
    // ensure buffer is opened
    val buffer = super.getBufferManager().getBuffer(this);
    if (buffer == null) {
      super.openBuffer(pm, info); // open buffer independently from the info, since we are building the info
    }
  }

  override def buildStructure(info: OpenableElementInfo, pm: IProgressMonitor, newElements: JMap[_, _], underlyingResource: IResource): Boolean = {
    ensureBufferOpen(info, pm)

    argusProject.presentationCompiler.internal { compiler =>
      val unsafeElements = newElements.asInstanceOf[JMap[AnyRef, AnyRef]]
      val tmpMap = new java.util.HashMap[AnyRef, AnyRef]
      val sourceLength = sourceFile.length

      try {
        logger.info("[%s] buildStructure for %s (%s)".format(argusProject.underlying.getName(), this.getResource(), sourceFile.file))

        compiler.askStructure(sourceFile).get match {
          case Left(cu) =>
            compiler.asyncExec {
              new compiler.StructureBuilderTraverser(this, info, tmpMap, sourceLength).traverse(cu)
            }.getOption() // block until the traverser finished
          case _ =>
        }
        info match {
          case cuei: CompilationUnitElementInfo =>
            cuei.setSourceLength(sourceLength)
          case _ =>
        }

        unsafeElements.putAll(tmpMap)
        true
      } catch {
        case e: InterruptedException =>
          Thread.currentThread().interrupt()
          logger.info("ignored InterruptedException in build structure")
          false

        case ex: Exception =>
          logger.error("Compiler crash while building structure for %s".format(file), ex)
          false
      }
    } getOrElse false
    true
  }
  
  /** Index this source file, but only if the project has the Jawa nature.
   *
   */
  def addToIndexer(indexer : JawaSourceIndexer) {
    if (argusProject.hasArgusNature) {
      try argusProject.presentationCompiler.internal { compiler =>
        val cu = compiler.parseCompilationUnit(sourceFile).get
        new compiler.IndexBuilderTraverser(indexer).traverse(cu)
      } catch {
        case ex: Throwable => logger.error("Compiler crash during indexing of %s".format(getResource()), ex)
      }
    }
  }

  override def getSourceElementAt(pos : Int) : IJavaElement = {
    getChildAt(this, pos) match {
      case null => this
      case elem => elem
    }
  }

  private def getChildAt(element: IJavaElement, pos: Int): IJavaElement = {
    /* companion-class can be selected instead of the object in the JDT-'super'
       implementation and make the private method and fields unreachable.
       To avoid this, we look for deepest element containing the position
     */

    def depth(e: IJavaElement): Int = if (e == element || e == null) 0 else (depth(e.getParent()) + 1)

    element match {
      case parent: IParent => {
        var resultElement= element
        // look through the list of children from the end, because the constructor (at
        // the beginning) covers the whole source code
        for (child <- parent.getChildren().reverse) {
          child match {
            case sourceReference: ISourceReference => {
              // check if the range of the child contains the position
              val range= sourceReference.getSourceRange
              val rangeStart= range.getOffset
              if (rangeStart <= pos && pos <= rangeStart + range.getLength) {
                // look in the child itself
                val foundChild = getChildAt(child, pos)
                // check if the found element is more precise than the one previously found
                if (depth(foundChild) > depth(resultElement))
                  resultElement = foundChild
              }
            }
          }
        }
        resultElement
      }
      case elem => elem
    }
  }
  // TODO
  override def codeSelect(cu: env.ICompilationUnit, offset: Int, length: Int, workingCopyOwner: WorkingCopyOwner): Array[IJavaElement] = {
    withSourceFile { (srcFile, compiler) =>
      val pos = Position.range(srcFile, offset, 1)

      val res: MList[IJavaElement] = mlistEmpty

      res
    } getOrElse (Array.empty[IJavaElement])
    Array.empty[IJavaElement]
  }

  override def codeComplete(cu : env.ICompilationUnit, unitToSkip : env.ICompilationUnit, position : Int,
                            requestor : CompletionRequestor, owner : WorkingCopyOwner, typeRoot : ITypeRoot, monitor : IProgressMonitor) {
    // This is a no-op. The jawa IDE provides code completions via an extension point
  }

  override def reportMatches(matchLocator : MatchLocator, possibleMatch : PossibleMatch) {
//    argusProject.presentationCompiler.internal { compiler =>
//      compiler.askLoadedTyped(sourceFile, false).get match {
//        case Left(tree) =>
//          compiler.asyncExec {
//            compiler.MatchLocator(this, matchLocator, possibleMatch).traverse(tree)
//          }.getOption() // wait until the traverser finished
//        case _ => () // no op
//      }
//
//    }
  }

  override def createOverrideIndicators(annotationMap : JMap[_, _]) {
    if (argusProject.hasArgusNature){}
      argusProject.presentationCompiler.internal { compiler =>
        try {
          compiler.askStructure(sourceFile).get match {
            case Left(cu) =>
//              compiler.asyncExec {
//                new compiler.OverrideIndicatorBuilderTraverser(this, annotationMap.asInstanceOf[JMap[AnyRef, AnyRef]]).traverse(tree)
//              }.getOption()  // block until traverser finished
            case _ =>
          }

          
        } catch {
          case ex: Exception =>
           logger.error("Exception thrown while creating override indicators for %s".format(sourceFile), ex)
        }
      }
  }

  override def getImageDescriptor = {
    import scala.util.control.Exception

    val descriptor = Exception.catching(classOf[JavaModelException]).opt(getCorrespondingResource) map { file =>
      val javaProject = JavaCore.create(argusProject.underlying)
      if (javaProject.isOnClasspath(file)) ArgusImages.JAWA_FILE else ArgusImages.EXCLUDED_JAWA_FILE
    }
    descriptor.orNull
  }

  def followDeclaration(editor : ITextEditor, selection : ITextSelection): Unit =
    followReference(DeclarationHyperlinkDetector(), editor, selection)

  def followReference(detectionStrategy: BaseHyperlinkDetector, editor : ITextEditor, selection : ITextSelection): Unit = {
    val region = EditorUtils.textSelection2region(selection)

    Option(detectionStrategy.detectHyperlinks(editor, region, canShowMultipleHyperlinks = false)) match {
      case Some(Array(first, _*)) => first.open
      case _ => ()
    }
  }
}

object OpenableUtils extends ReflectionUtils {
  private val oClazz = classOf[Openable]
  private val openBufferMethod = getDeclaredMethod(oClazz, "openBuffer", classOf[IProgressMonitor], classOf[AnyRef])
  private val getBufferManagerMethod = getDeclaredMethod(oClazz, "getBufferManager")

  def openBuffer(o : Openable, pm : IProgressMonitor, info : AnyRef) : IBuffer = openBufferMethod.invoke(o, pm, info).asInstanceOf[IBuffer]
  def getBufferManager(o : Openable) : BufferManager = getBufferManagerMethod.invoke(o).asInstanceOf[BufferManager]
}
