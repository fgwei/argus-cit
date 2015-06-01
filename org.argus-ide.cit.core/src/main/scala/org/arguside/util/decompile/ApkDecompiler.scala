package org.arguside.util.decompile

import org.eclipse.core.runtime.IPath
import java.io.File
import org.sireum.util._
import org.sireum.amandroid.decompile.Dex2PilarConverter
import org.eclipse.core.runtime.Path
import org.sireum.util.FileResourceUri
import org.arguside.util.parser.ManifestParser
import org.sireum.jawa.sjc.util.MyFileUtil
import org.arguside.core.CitConstants

object ApkDecompiler {
  def decompile(apk: File, projectLocation: IPath, removeSupportGen: Boolean): Option[ISet[String]] = {
    val out = AmDecoder.decode(FileUtil.toUri(apk), FileUtil.toUri(projectLocation.toFile()), false)
    val dexFile = out + "/classes.dex"
    if(FileUtil.toFile(out).exists()) {
      val src = Dex2PilarConverter.convert(dexFile, out + "/src")
      if(removeSupportGen) return Some(removeSupportLibAndGen(src, ManifestParser.loadPackageName(apk)))
    }
    None
  }
  
  def removeSupportLibAndGen(src: FileResourceUri, pkg: String): ISet[String] = {
    val dependencies: MSet[String] = msetEmpty
    val pkgPath = pkg.replaceAll("\\.", "/")
    val srcDir = FileUtil.toFile(src)
    val worklist: MList[File] = mlistEmpty
    MyFileUtil.listFilesAndDir(srcDir) foreach {
      f =>
        if(f.isDirectory()){
          if(f.getAbsolutePath.endsWith("/android/support/v4")){
            worklist += f
            dependencies += CitConstants.MAVEN_SUPPORT_V4
          } else if (f.getAbsolutePath.endsWith("/android/support/v13")) {
            worklist += f
            dependencies += CitConstants.MAVEN_SUPPORT_V13
          } else if (f.getAbsolutePath.endsWith("/android/support/v7/appcompat")){
            worklist += f
            dependencies += CitConstants.MAVEN_APPCOMPAT
          }
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
      val f = worklist.remove(0)
      MyFileUtil.deleteDir(f)
    }
    MyFileUtil.clearDirIfNoFile(srcDir)
    dependencies.toSet
  }
}