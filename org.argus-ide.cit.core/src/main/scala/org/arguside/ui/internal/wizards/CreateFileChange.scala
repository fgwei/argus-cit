package org.arguside.ui.internal.wizards

import org.eclipse.ltk.core.refactoring.resource.ResourceChange
import org.eclipse.core.runtime.IPath
import java.io.File
import org.eclipse.core.resources.IResource
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.ltk.core.refactoring.RefactoringStatus
import org.eclipse.ltk.core.refactoring.Change
import com.google.common.io.Files
import java.io.InputStream
import org.eclipse.core.resources.IFile
import com.android.ide.eclipse.adt.AdtUtils
import org.eclipse.core.runtime.SubProgressMonitor
import org.arguside.core.internal.ArgusPlugin
import com.google.common.io.Closeables


/**
 * Change which lazily copies a file
 * @author fgwei
 */
class CreateFileChange(mName: String, mPath: IPath, mSource: File) extends ResourceChange {

    override protected def getModifiedResource(): IResource = {
      return ResourcesPlugin.getWorkspace().getRoot().getFile(mPath)
    }

    override def getName: String = mName

    override def isValid(pm: IProgressMonitor): RefactoringStatus = {
        val result = new RefactoringStatus()
        val file = ResourcesPlugin.getWorkspace().getRoot().getFile(mPath)
        val location = file.getLocationURI()
        if (location == null) {
          result.addFatalError("Unknown location " + file.getFullPath().toString())
          return result
        }
        return result
    }

    override def perform(pm: IProgressMonitor): Change = {
      val supplier = Files.newInputStreamSupplier(mSource)
      var is: InputStream = null
      try {
        pm.beginTask("Creating file", 3)
        val file = getModifiedResource().asInstanceOf[IFile]

        val parent = file.getParent()
        if (parent != null && !parent.exists()) {
          val folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(
                  parent.getFullPath())
          AdtUtils.ensureExists(folder)
        }

        is = supplier.getInput()
        file.create(is, false, new SubProgressMonitor(pm, 1))
        pm.worked(1)
      } catch {
        case e: Exception =>
          ArgusPlugin().logError(null, e)
      } finally {
          Closeables.closeQuietly(is)
          pm.done()
      }
      return null
    }
}