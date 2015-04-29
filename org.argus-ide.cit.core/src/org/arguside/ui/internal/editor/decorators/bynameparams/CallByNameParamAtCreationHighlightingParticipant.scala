package org.arguside.ui.internal.editor.decorators.bynameparams

import org.arguside.core.extensions.SemanticHighlightingParticipant

class CallByNameParamAtCreationHighlightingParticipant extends
  SemanticHighlightingParticipant(viewer => new CallByNameParamAtCreationPresenter(viewer)) {

}
