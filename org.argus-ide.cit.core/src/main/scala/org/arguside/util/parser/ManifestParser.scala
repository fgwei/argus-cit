package org.arguside.util.parser

import org.sireum.amandroid.parser.AndroidXMLHandler
import java.io.File
import java.io.InputStream
import java.io.IOException
import brut.androlib.res.decoder.AXmlResourceParser
import argus.tools.eclipse.contribution.weaving.jdt.ArgusJDTWeavingPlugin

object ManifestParser {
  def loadPackageName(apk: File): String = {
    var pkg: String = ""
    AndroidXMLParser.handleAndroidXMLFiles(apk, Set("AndroidManifest.xml"), new AndroidXMLHandler() {
      
      override def handleXMLFile(fileName: String, fileNameFilter: Set[String], stream: InputStream) = {
        try {
          if (fileNameFilter.contains(fileName)){
            pkg = getPackageNameFromManifest(stream)
          }
        }
        catch {
          case ex: IOException =>
            System.err.println("Could not read AndroidManifest file: " + ex.getMessage())
            ex.printStackTrace()
        }
      }
      
    })
    
    pkg
  }
  
  def loadSdkVersionFromManifestFile(apk: File): (Int, Int, Int) = {
    var min: Int = 1
    var target: Int = min
    var max: Int = target
    AndroidXMLParser.handleAndroidXMLFiles(apk, Set("AndroidManifest.xml"), new AndroidXMLHandler() {
      
      override def handleXMLFile(fileName: String, fileNameFilter: Set[String], stream: InputStream) = {
        try {
          if (fileNameFilter.contains(fileName)){
            val (mint, targett, maxt) = getSdkVersionFromBinaryManifest(stream)
            min = mint
            target = targett
            max = maxt
          }
        }
        catch {
          case ex: IOException =>
            System.err.println("Could not read AndroidManifest file: " + ex.getMessage())
            ex.printStackTrace()
        }
      }
      
    })
    (min, target, max)
  }
  
  protected def getPackageNameFromManifest(manifestIS: InputStream): String = {
    var pkg: String = ""
    try {
      val parser = new AXmlResourceParser()
      parser.open(manifestIS)
      var typ = parser.next()
      while (typ != 0x00000001) { // XmlPullParser.END_DOCUMENT
        typ match {
          case 0x00000000 => // XmlPullParser.START_DOCUMENT
          case 0x00000002 => //XmlPullParser.START_TAG
            val tagName = parser.getName
            if(tagName.equals("manifest")){
              pkg = getAttributeValue(parser, "package")
              if(pkg == null) pkg = ""
            }
          case 0x00000003 => //XmlPullParser.END_TAG
          case 0x00000004 => //XmlPullParser.TEXT
        }
        typ = parser.next()
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
    }
    pkg
  }
  
  protected def getSdkVersionFromBinaryManifest(manifestIS: InputStream): (Int, Int, Int) = {
    var min: Int = 1
    var target: Int = min
    var max: Int = target
    try {
      val parser = new AXmlResourceParser()
      parser.open(manifestIS)
      var typ = parser.next()
      while (typ != 0x00000001) { // XmlPullParser.END_DOCUMENT
         typ match {
          case 0x00000000 => // XmlPullParser.START_DOCUMENT
          case 0x00000002 => //XmlPullParser.START_TAG
            val tagName = parser.getName()
            if (tagName.equals("uses-sdk")){
              var attrValue = getAttributeValue(parser, "minSdkVersion")
              if (attrValue != null) min = attrValue.toInt
              attrValue = getAttributeValue(parser, "targetSdkVersion")
              if (attrValue != null) target = attrValue.toInt
              attrValue = getAttributeValue(parser, "maxSdkVersion")
              if (attrValue != null) max = attrValue.toInt
            }
            
          case 0x00000003 => //XmlPullParser.END_TAG
          case 0x00000004 => //XmlPullParser.TEXT
        }
        typ = parser.next()
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
    } finally {
      if(min < 1) min = 1
      if(target < min) target = min
      if(max < target) max = target
    }
    (min, target, max)
  }
  
  private def getAttributeValue(parser: AXmlResourceParser, attributeName: String): String = {
    val count = parser.getAttributeCount
    for (i <- 0 to count - 1){
      ArgusJDTWeavingPlugin.logErrorMessage(parser.getAttributeName(i).equals(attributeName) + "")
      ArgusJDTWeavingPlugin.logErrorMessage(parser.getAttributeValue(i))      
      if (parser.getAttributeName(i).equals(attributeName))
        return parser.getAttributeValue(i)
    }
    null
  }
}