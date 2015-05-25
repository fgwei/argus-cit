package org.arguside.ui.editor

import org.eclipse.jface.text.source.Annotation

/**
 * Marker interface for all annotations that are created for the jawa editor.
 * Features of the jawa editor like quick assists can search for  annotations
 * of this type to operate on them.
 */
trait JawaEditorAnnotation extends Annotation
