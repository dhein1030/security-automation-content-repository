/*******************************************************************************
 * The MIT License
 * 
 * Copyright (c) 2011 David Waltermire
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package org.scapdev.content.core;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.scapdev.content.core.persistence.ContentPersistenceManager;
import org.scapdev.content.core.persistence.hybrid.DefaultHybridContentPersistenceManager;
import org.scapdev.content.core.query.DefaultQueryProcessor;
import org.scapdev.content.core.query.Query;
import org.scapdev.content.core.query.QueryProcessor;
import org.scapdev.content.core.query.QueryResult;
import org.scapdev.content.core.query.SimpleQuery;
import org.scapdev.content.core.resolver.LocalResolver;
import org.scapdev.content.core.resolver.Resolver;
import org.scapdev.content.core.writer.DefaultInstanceWriter;
import org.scapdev.content.core.writer.InstanceWriter;
import org.scapdev.content.core.writer.NamespaceMapper;
import org.scapdev.content.model.Key;
import org.scapdev.content.model.MetadataModel;
import org.scapdev.content.model.MetadataModelFactory;
import org.scapdev.content.model.processor.jaxb.JAXBEntityProcessor;

public class ContentRepository {
	private final ContentPersistenceManager persistenceManager;
	private final MetadataModel model;
	private final JAXBEntityProcessor processor;
//	private final ProcessingFactory processingFactory;
//	private final InstanceWriterFactory instanceWriterFactory;
	private final Resolver resolver;
	private final QueryProcessor queryProcessor;


	public ContentRepository(ClassLoader classLoader) throws IOException, JAXBException, ClassNotFoundException {
		model = MetadataModelFactory.newInstance();
//		persistenceManager = new MemoryResidentPersistenceManager();
		persistenceManager = new DefaultHybridContentPersistenceManager(model);
		processor = new JAXBEntityProcessor(model, persistenceManager);
		resolver = new LocalResolver(persistenceManager);
		queryProcessor = new DefaultQueryProcessor(resolver);
	}

	public MetadataModel getMetadataModel() {
		return model;
	}
//
//	public void retrieveContentFragment(Key key, OutputStream os) throws IOException {
//		QueryResult queryResult = queryProcessor.query(new SimpleQuery(key));
//		InstanceWriter writer = instanceWriterFactory.newInstanceWriter(os);
//		writer.write(queryResult);
//	}

	public QueryResult query(Key key) throws IOException {
		return query(key, false);
	}

	public QueryResult query(Key key, boolean resolveReferences) throws IOException {
		SimpleQuery query = new SimpleQuery(key);
		query.setResolveReferences(resolveReferences);
		return query(query);
	}

	public QueryResult query(String indirectType, Collection<String> indirectIds, Set<String> requestedEntityIds, boolean resolveReferences) throws IOException {
		IndirectQuery query = new IndirectQuery(indirectType, indirectIds, requestedEntityIds, persistenceManager);
		query.setResolveReferences(resolveReferences);
		return query(query);
	}

	public <RESULT extends QueryResult> RESULT query(Query<RESULT> query) {
		RESULT queryResult = queryProcessor.query(query);
		return queryResult;
	}

	public JAXBEntityProcessor getProcessor() {
		return processor;
	}

	public void shutdown() {
		processor.shutdown();
	}

	public InstanceWriter newInstanceWriter() throws JAXBException {
		Marshaller marshaller = getMarshaller();
		NamespaceMapper mapper = new NamespaceMapper(model);
		marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", mapper);
		return new DefaultInstanceWriter(marshaller, model);
	}

	private Marshaller getMarshaller() throws JAXBException {
		Marshaller marshaller = model.getJAXBContext().createMarshaller();
		return marshaller;
	}
}