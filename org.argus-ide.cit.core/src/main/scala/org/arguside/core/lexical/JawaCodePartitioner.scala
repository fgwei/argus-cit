package org.arguside.core.lexical

import org.eclipse.jface.text.IDocumentPartitioner
import org.arguside.core.internal.lexical.JawaDocumentPartitioner
import org.arguside.core.internal.lexical.JawaPartitionTokeniser
import org.eclipse.jface.text.IDocumentPartitionerExtension
import org.eclipse.jface.text.ITypedRegion

/** Entry point to Jawa sources partitioners.
 *
 *  A partitioner takes a complete Jawa source, and returns regions of the different parts
 *  of the source like plain code, comments, strings, ...
 *  The partition types are defined in [[org.arguside.core.lexical.JawaPartitions]] and [[org.eclipse.jdt.ui.text.IJavaPartitions]].
 *
 *  Usually, the partitions are then parsed by a token scanner to extract the different elements (keywords, symbols, ...)
 *
 *  @see org.eclipse.jface.text.IDocumentPartitioner
 *  @see org.arguside.core.lexical.JawaCodeScannners
 */
object JawaCodePartitioner {

  /** Provides a document partitioner for Jawa sources.
   */
  def documentPartitioner(conservative: Boolean = false): IDocumentPartitioner with IDocumentPartitionerExtension =
    new JawaDocumentPartitioner(conservative)

  /** Partitions the given text as a Jawa source.
   */
  def partition(text: String): List[ITypedRegion] = JawaPartitionTokeniser.tokenise(text)

}