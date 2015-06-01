package org.arguside.util.decompile

import com.sun.jna.Library
import com.sun.jna.Native
import com.android.ide.eclipse.adt.internal.sdk.Sdk
import org.eclipse.core.resources.IProject
import java.io.File
import org.arguside.core.internal.ArgusPlugin
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import com.android.SdkConstants

/**
 * @author fgwei
 */
object Dex2Jawa {
  trait CTest extends Library {
    def helloFromC()
  }
//  def decompile(project: IProject, apk: File): IStatus = {
//    val current = Sdk.getCurrent()
//    if (current == null) {
//      ArgusPlugin().logError("Dex2Jawa: missing current SDK")
//      return Status.OK_STATUS
//    }
//    val buildToolInfo = current.getLatestBuildTool()
//    if (buildToolInfo == null) {
//      ArgusPlugin().logError("SDK missing build tools. Please install build tools using SDK Manager.")
//      return Status.OK_STATUS
//    }
//  
//    val buildToolsFolder = buildToolInfo.getLocation()
//    val dexDumpFile = new File(buildToolsFolder, SdkConstants.FN_DEXDUMP)
//    val ctest = Native.loadLibrary(dexDumpFile.getPath, CTest).asInstanceOf[CTest]
//    ctest.helloFromC()
//  }
}