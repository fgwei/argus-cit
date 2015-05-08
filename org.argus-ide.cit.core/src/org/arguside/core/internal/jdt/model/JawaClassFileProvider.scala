package org.arguside.core.internal.jdt.model

import java.io.ByteArrayInputStream
import scala.collection.mutable.WeakHashMap
import argus.tools.eclipse.contribution.weaving.jdt.cfprovider.IClassFileProvider
import org.arguside.logging.HasLogger
import org.eclipse.jdt.core.IClassFile
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.core.IPackageFragmentRoot
import org.eclipse.jdt.internal.core.ClassFile
import org.eclipse.jdt.internal.core.PackageFragment
import org.arguside.core.internal.project.ArgusProject

class JawaClassFileProvider extends IClassFileProvider with HasLogger {

  /** @return a JawaClassFile implementation if bytes represent a Jawa classfile, or `null`
   *          if the default JDT implementation should be used.
   */
  override def create(contents: Array[Byte], parent: PackageFragment, name: String): ClassFile = {
    def updateCache(isJawaClassfile: Boolean) {
      val pfr = parent.getPackageFragmentRoot()
      if (pfr ne null)
        jawaPackageFragments.synchronized {
          if (!jawaPackageFragments.isDefinedAt(pfr)) {
            logger.debug(s"Setting ${pfr.getElementName} (because of class $name) to be ${if (isJawaClassfile) "Jawa" else "Java"}")
            jawaPackageFragments += pfr -> isJawaClassfile
          }
        }
    }

    val scalaCF = JawaClassFileDescriber.isJawa(new ByteArrayInputStream(contents)) match {
      case Some(sourcePath) => new JawaClassFile(parent, name, sourcePath)
      case None                => null
    }
    updateCache(scalaCF ne null)
    scalaCF
  }

  /** Return `true` if the classfile could be a Jawa classfile.
   *
   *  @note This method caches the result of the first classfile read from a package fragment (usually a jar).
   *        This heuristic might fail if a single jar mixes Java and Jawa classfiles, and if the first classfile
   *        is comes from Java, a plain Java classfile editor and icon would be used for all classfiles in that jar.
   */
  override def isInteresting(classFile: IClassFile): Boolean = {
    if (ArgusProject.isArgusProject(classFile.getJavaProject)) {
      val pfr = ancestorFragmentRoot(classFile)
      // synchronized needed for visibility
      jawaPackageFragments.synchronized {
        pfr.map(jawaPackageFragments.getOrElse(_, true)).getOrElse(false)
      }
    } else
      false
  }

  private def ancestorFragmentRoot(classFile: IClassFile): Option[IPackageFragmentRoot] =
    classFile.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT) match {
      case pfr: IPackageFragmentRoot => Some(pfr)
      case _                         => None
    }

  private val jawaPackageFragments: WeakHashMap[IPackageFragmentRoot, Boolean] = WeakHashMap.empty
}
