package org.arguside.ui.internal.editor.decorators.bynameparams

import org.eclipse.jface.text.source.Annotation
import org.arguside.ui.editor.ArgusEditorAnnotation

object CallByNameParamAtCreationAnnotation {
  val ID = "argus.tools.eclipse.semantichighlighting.callByNameParam.creationAnnotation"
}

final class CallByNameParamAtCreationAnnotation(text: String)
  extends Annotation(CallByNameParamAtCreationAnnotation.ID, false, text) with ArgusEditorAnnotation {

}
