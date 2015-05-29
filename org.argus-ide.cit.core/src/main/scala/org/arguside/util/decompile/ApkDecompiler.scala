package org.arguside.util.decompile

import org.eclipse.core.runtime.IPath
import java.io.File
import org.sireum.util._
import org.sireum.amandroid.decompile.Dex2PilarConverter
import org.eclipse.core.runtime.Path
import org.sireum.util.FileResourceUri
import org.arguside.util.parser.ManifestParser
import org.sireum.jawa.sjc.util.MyFileUtil

object ApkDecompiler {
  def decompile(apk: File, projectLocation: IPath, removeSupportGen: Boolean) = {
    val out = AmDecoder.decode(FileUtil.toUri(apk), FileUtil.toUri(projectLocation.toFile()), false)
    val dexFile = out + "/classes.dex"
    if(FileUtil.toFile(out).exists()) {
      val src = Dex2PilarConverter.convert(dexFile, out + "/src")
      if(removeSupportGen) removeSupportLibAndGenPilars(src, ManifestParser.loadPackageName(apk))
    }
  }
  
  def removeSupportLibAndGenPilars(src: FileResourceUri, pkg: String) = {
    val pkgPath = pkg.replaceAll(".", "/")
    val srcDir = FileUtil.toFile(src)
    val worklist: MList[File] = mlistEmpty
    FileUtil.listFiles(src, "", true) foreach {
      uri =>
        val f = FileUtil.toFile(uri)
        if(f.isDirectory()){
          if(f.getAbsolutePath.endsWith("/android/support/v4")) worklist += f
        }
        if(f.getAbsolutePath.contains("/" + pkgPath + "/BuildConfig.pilar") ||
           f.getAbsolutePath.contains("/" + pkgPath + "/Manifest.pilar") ||
           f.getAbsolutePath.contains("/" + pkgPath + "/Manifest$") ||
           f.getAbsolutePath.contains("/" + pkgPath + "/R.pilar") ||
           f.getAbsolutePath.contains("/" + pkgPath + "/R$")) {
          if(!f.isDirectory()) worklist += f
        }
    }
    while(!worklist.isEmpty){
      val f = worklist.remove(1)
      MyFileUtil.deleteDir(f)
    }
  }
}