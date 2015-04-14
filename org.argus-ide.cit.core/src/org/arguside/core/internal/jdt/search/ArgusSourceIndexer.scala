package org.arguside.core.internal.jdt.search

import org.eclipse.jdt.core.search.SearchDocument
import org.eclipse.jdt.internal.core.search.indexing.AbstractIndexer
import org.arguside.core.internal.jdt.model.ArgusSourceFile
import argus.tools.eclipse.contribution.weaving.jdt.indexerprovider.IIndexerFactory
import org.arguside.logging.HasLogger

class ArgusSourceIndexerFactory extends IIndexerFactory {
  override def createIndexer(document : SearchDocument) = new ArgusSourceIndexer(document);
}

class ArgusSourceIndexer(document : SearchDocument) extends AbstractIndexer(document) with HasLogger {
  override def indexDocument() {
    logger.info("Indexing document: "+document.getPath)
    ArgusSourceFile.createFromPath(document.getPath).map(_.addToIndexer(this))
  }
}
