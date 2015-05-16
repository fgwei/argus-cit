package org.arguside.ui.editor

import org.eclipse.jface.text.source.Annotation

/**
 * Marker interface for all annotations that are created for the argus editor.
 * Features of the argus editor like quick assists can search for  annotations
 * of this type to operate on them.
 */
trait ArgusEditorAnnotation extends Annotation
