package org.arguside.ui.internal.reconciliation

import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.NullProgressMonitor
import org.arguside.logging.HasLogger
import org.eclipse.jface.text._
import org.eclipse.jface.text.reconciler.IReconcilingStrategy
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension
import org.eclipse.jface.text.reconciler.DirtyRegion
import org.eclipse.jdt.internal.ui.text.java.IJavaReconcilingListener
import org.eclipse.jdt.core.ICompilationUnit
import org.arguside.ui.editor.InteractiveCompilationUnitEditor
import org.arguside.util.Utils._

class JawaReconcilingStrategy(icuEditor: InteractiveCompilationUnitEditor) extends IReconcilingStrategy with IReconcilingStrategyExtension with HasLogger {

  /**
   * The underlying compilation unit, in general implemented by a JawaSourceFile.
   *
   * @note This member is a def, not a lazy val, to avoid doc/reconciler
   * desynchronizations if the underlying document is swapped.
   */
  private def icUnit = icuEditor.getInteractiveCompilationUnit()

  // Our icuEditor can be a source-attached binary, a.k.a ScalaClassFileEditor,
  // for which reconciliation of the locally opened editor makes little sense
  // (it's more properly a JawaClassFileViewer) but we still want to flush
  // scheduled reloads nonetheless
  private val listeningEditor: Option[IJavaReconcilingListener] =
    icuEditor.asInstanceOfOpt[IJavaReconcilingListener]

  override def setDocument(doc: IDocument) {}

  override def setProgressMonitor(pMonitor: IProgressMonitor) {}

  override def reconcile(dirtyRegion: DirtyRegion, subRegion: IRegion) {
    logger.debug("Incremental reconciliation not implemented.")
  }

  override def reconcile(partition: IRegion) {
    listeningEditor.foreach(_.aboutToBeReconciled())
    val errors = icUnit.forceReconcile()

    // Some features, such as quick fixes, are dependent upon getting an ICompilationUnit there
    val cu: Option[ICompilationUnit] = icUnit.asInstanceOfOpt[ICompilationUnit]
    // we only update the edited compilation unit
    icuEditor.updateErrorAnnotations(errors, cu.orNull)

    // reconciled expects a jdt.core.dom.CompilationUnitEditor as first argument,
    // which JawaSourceFileEditor and other ICU Editors aren't
    // it is possible we starve Java-Side IReconcilingListeners here
    listeningEditor.foreach(_.reconciled(null, false, new NullProgressMonitor()))
  }

  override def initialReconcile() {
    // an askReload there adds the scUnit to the list of managed CUs
    icUnit.initialReconcile()
    reconcile(null)
  }

}
