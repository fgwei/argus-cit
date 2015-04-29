package org.arguside.core.lexical

import org.eclipse.jface.text.IDocumentPartitioner
import org.eclipse.jface.text.IDocumentPartitionerExtension
import org.eclipse.jface.text.ITypedRegion
import org.arguside.core.internal.lexical.JawaDocumentPartitioner
import org.arguside.core.internal.lexical.JawaPartitionTokenizer

/** Entry point to jawa sources partitioners.
 *
 *  A partitioner takes a complete jawa source, and returns regions of the different parts
 *  of the source like plain code, comments, strings
 *  The partition types are defined in [[org.arguside.core.lexical.PilarPartitions]] and [[org.eclipse.jdt.ui.text.IJavaPartitions]].
 *
 *  Usually, the partitions are then parsed by a token scanner to extract the different elements (keywords, symbols, ...)
 *
 *  @see org.eclipse.jface.text.IDocumentPartitioner
 *  @see org.arguside.core.lexical.PilarCodeScannners
 */
object JawaCodePartitioner {

  /** Provides a document partitioner for Jawa sources.
   */
  def documentPartitioner(conservative: Boolean = false): IDocumentPartitioner with IDocumentPartitionerExtension =
    new JawaDocumentPartitioner(conservative)

  /** Partitions the given text as a Jawa source.
   */
  def partition(text: String): List[ITypedRegion] = JawaPartitionTokenizer.tokenise(text)

}