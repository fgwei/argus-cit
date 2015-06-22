package org.arguside.core.internal.jdt.search

import java.{ util => ju }
import scala.collection.mutable.ArrayBuffer
import org.eclipse.jdt.core.Signature
import org.eclipse.jdt.core.compiler.CharOperation
import org.eclipse.jdt.core.compiler.InvalidInputException
import org.eclipse.jdt.core.search.IJavaSearchConstants
import org.eclipse.jdt.internal.codeassist.ISearchRequestor
import org.eclipse.jdt.internal.codeassist.ISelectionRequestor
import org.eclipse.jdt.internal.codeassist.impl.AssistParser
import org.eclipse.jdt.internal.codeassist.impl.Engine
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit
import org.eclipse.jdt.internal.compiler.env.AccessRestriction
import org.eclipse.jdt.internal.core.JavaElement
import org.eclipse.jdt.internal.core.SearchableEnvironment
import org.arguside.logging.HasLogger
import org.arguside.core.compiler.InteractiveCompilationUnit
import org.arguside.util.JawaWordFinder
import org.arguside.core.internal.jdt.model.JawaLocalVariableElement
import org.arguside.core.compiler.IJawaPresentationCompiler.Implicits._
import org.sireum.jawa.sjc.util.Position

class JawaSelectionEngine(nameEnvironment: SearchableEnvironment, requestor: JawaSelectionRequestor, settings: ju.Map[_, _]) extends Engine(settings) with ISearchRequestor with HasLogger {

  var actualSelectionStart: Int = _
  var actualSelectionEnd: Int = _
  var selectedIdentifier: Array[Char] = _

  val acceptedClasses = new ArrayBuffer[(Array[Char], Array[Char], Int)]
  val acceptedInterfaces = new ArrayBuffer[(Array[Char], Array[Char], Int)]
  val acceptedAnnotations = new ArrayBuffer[(Array[Char], Array[Char], Int)]

  def select(icu: InteractiveCompilationUnit, selectionStart0: Int, selectionEnd0: Int) {
    val src = icu.lastSourceMap().sourceFile
    icu.argusProject.presentationCompiler { compiler =>
      val source = icu.lastSourceMap().jawaSource
      val region = JawaWordFinder.findWord(source, selectionStart0)

      val (selectionStart, selectionEnd) =
        if (selectionStart0 <= selectionEnd0)
          (selectionStart0, selectionEnd0)
        else {
          (region.getOffset, if (region.getLength > 0) region.getOffset + region.getLength - 1 else region.getOffset)
        }

      val wordStart = region.getOffset

      actualSelectionStart = selectionStart
      actualSelectionEnd = selectionEnd
      val length = 1 + selectionEnd - selectionStart
      selectedIdentifier = new Array(length)
      Array.copy(source, selectionStart, selectedIdentifier, 0, length)
      logger.info("selectedIdentifier: " + selectedIdentifier.mkString("", "", ""))

      val ssr = requestor

      /** Delay the action. Necessary so that the payload is run outside of 'ask'. */
      class Cont(f: () => Unit) {
        def apply() = f()
      }

      object Cont {
        def apply(next: => Unit) = new Cont({ () => next })
        val Noop = new Cont(() => ())
      }

      
      val pos = Position.range(src, actualSelectionStart, actualSelectionEnd - actualSelectionStart)

      if (!ssr.hasSelection) {
        // only reaches here if no selection could be derived from the parsed tree
        // thus use the selected source and perform a textual type search

        nameEnvironment.findTypes(selectedIdentifier, false, false, IJavaSearchConstants.TYPE, this)

        // accept qualified types only if no unqualified type was accepted
        if (!ssr.hasSelection) {
          def acceptTypes(accepted: ArrayBuffer[(Array[Char], Array[Char], Int)]) {
            if (!accepted.isEmpty) {
              for (t <- accepted)
                requestor.acceptType(t._1, t._2, t._3, false, null, actualSelectionStart, actualSelectionEnd)
              accepted.clear
            }
          }

          acceptTypes(acceptedClasses)
          acceptTypes(acceptedInterfaces)
          acceptTypes(acceptedAnnotations)
        }
      }
    }
  }

  override def acceptType(packageName: Array[Char], simpleTypeName: Array[Char], enclosingTypeNames: Array[Array[Char]], modifiers: Int, accessRestriction: AccessRestriction) {
    val typeName =
      if (enclosingTypeNames == null)
        simpleTypeName
      else
        CharOperation.concat(
          CharOperation.concatWith(enclosingTypeNames, '.'),
          simpleTypeName,
          '.')

    if (CharOperation.equals(simpleTypeName, selectedIdentifier)) {
      val flatEnclosingTypeNames =
        if (enclosingTypeNames == null || enclosingTypeNames.length == 0)
          null
        else
          CharOperation.concatWith(enclosingTypeNames, '.')
      if (mustQualifyType(packageName, simpleTypeName, flatEnclosingTypeNames, modifiers)) {
        val accepted = (packageName, typeName, modifiers)
        val kind = modifiers & (ClassFileConstants.AccInterface | ClassFileConstants.AccEnum | ClassFileConstants.AccAnnotation)
        kind match {
          case x if (x == ClassFileConstants.AccAnnotation) || (x == (ClassFileConstants.AccAnnotation | ClassFileConstants.AccInterface)) =>
            acceptedAnnotations += accepted
          case ClassFileConstants.AccInterface =>
            acceptedInterfaces += accepted
          case _ =>
            acceptedClasses += accepted
        }
      } else {
        requestor.acceptType(
          packageName,
          typeName,
          modifiers,
          false,
          null,
          actualSelectionStart,
          actualSelectionEnd)
      }
    }
  }

  override def getParser(): AssistParser = {
    throw new UnsupportedOperationException();
  }

  override def acceptPackage(packageName: Array[Char]) {
    // NOP
  }

  override def acceptConstructor(
    modifiers: Int,
    simpleTypeName: Array[Char],
    parameterCount: Int,
    signature: Array[Char],
    parameterTypes: Array[Array[Char]],
    parameterNames: Array[Array[Char]],
    typeModifiers: Int,
    packageName: Array[Char],
    extraFlags: Int,
    path: String,
    accessRestriction: AccessRestriction) {
    // NOP
  }
}
