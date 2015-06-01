package org.arguside.core.internal.jdt.search

import org.eclipse.jdt.core.search.SearchDocument
import org.eclipse.jdt.internal.core.search.indexing.AbstractIndexer
import org.arguside.core.internal.jdt.model.JawaSourceFile
import org.arguside.logging.HasLogger
import argus.tools.eclipse.contribution.weaving.jdt.indexerprovider.IIndexerFactory

class JawaSourceIndexerFactory extends IIndexerFactory {
  override def createIndexer(document : SearchDocument) = new JawaSourceIndexer(document)
}

class JawaSourceIndexer(document : SearchDocument) extends AbstractIndexer(document) with HasLogger {
  override def indexDocument() {
    logger.info("Indexing document: "+document.getPath)
    JawaSourceFile.createFromPath(document.getPath).map(_.addToIndexer(this))
  }
}
