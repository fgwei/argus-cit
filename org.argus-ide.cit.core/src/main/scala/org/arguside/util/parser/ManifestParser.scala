package org.arguside.util.parser

import org.sireum.amandroid.parser.AndroidXMLHandler
import java.io.File
import java.io.InputStream
import java.io.IOException
import android.content.res.AXmlResourceParser
import android.util.TypedValue

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
      if (parser.getAttributeName(i).equals(attributeName))
        return getAttributeValue(parser, i)
    }
    null
  }
  
  private def getAttributeValue(parser: AXmlResourceParser,index: Int): String = {
    val typ: Int = parser.getAttributeValueType(index)
    val data: Int = parser.getAttributeValueData(index)
    if (typ == TypedValue.TYPE_STRING) {
      return parser.getAttributeValue(index);
    }
    if (typ==TypedValue.TYPE_ATTRIBUTE) {
      val pkg = getPackage(data)
      return f"?$pkg%s$data%08X"
    }
    if (typ==TypedValue.TYPE_REFERENCE) {
      val pkg = getPackage(data)
      return f"@$pkg%s$data%08X"
    }
    if (typ==TypedValue.TYPE_FLOAT) {
      return String.valueOf(data.toFloat)
    }
    if (typ==TypedValue.TYPE_INT_HEX) {
      return f"0x$data%08X"
    }
    if (typ==TypedValue.TYPE_INT_BOOLEAN) {
      return if(data!=0)"true"else"false"
    }
    if (typ==TypedValue.TYPE_DIMENSION) {
      return complexToFloat(data) + DIMENSION_UNITS(data & TypedValue.COMPLEX_UNIT_MASK)
    }
    if (typ == TypedValue.TYPE_FRACTION) {
      return complexToFloat(data) + FRACTION_UNITS(data & TypedValue.COMPLEX_UNIT_MASK)
    }
    if (typ >= TypedValue.TYPE_FIRST_COLOR_INT && typ<=TypedValue.TYPE_LAST_COLOR_INT) {
      return f"#$data%08X"
    }
    if (typ >= TypedValue.TYPE_FIRST_INT && typ<=TypedValue.TYPE_LAST_INT) {
      return String.valueOf(data)
    }
    return f"<0x$data%X, type 0x$typ%02X>"
  }
  
  private def getPackage(id: Int): String = {
    if (id>>>24==1) {
      return "android:"
    }
    return ""
  }
  
  def complexToFloat(complex: Int): Float = {
    return (complex & 0xFFFFFF00)*RADIX_MULTS((complex>>4) & 3)
  }
  
  private final def RADIX_MULTS = List(
    0.00390625F,3.051758E-005F,1.192093E-007F,4.656613E-010F
  )
  private final def DIMENSION_UNITS = List(
    "px","dip","sp","pt","in","mm","",""
  )
  private final def FRACTION_UNITS = List(
    "%","%p","","","","","",""
  )
}