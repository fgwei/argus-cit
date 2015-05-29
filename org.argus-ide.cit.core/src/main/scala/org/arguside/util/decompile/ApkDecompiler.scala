package org.arguside.util.decompile

import org.eclipse.core.runtime.IPath
import java.io.File
import org.sireum.util.FileUtil
import org.sireum.amandroid.decompile.Dex2PilarConverter

object ApkDecompiler {
  def decompile(apk: File, projectLocation: IPath) = {
    val out = AmDecoder.decode(FileUtil.toUri(apk), FileUtil.toUri(projectLocation.toFile()), false)
    val dexFile = out + "/classes.dex"
    if(FileUtil.toFile(out).exists()) {
      Dex2PilarConverter.convert(dexFile, out + "/src")
    }
  }
}