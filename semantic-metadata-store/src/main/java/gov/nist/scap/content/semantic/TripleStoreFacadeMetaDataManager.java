/*******************************************************************************
 * The MIT License
 * 
 * Copyright (c) 2011 Paul Cichonski
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
package gov.nist.scap.content.semantic;

import static org.scapdev.content.core.query.entity.EntityQuery.selectEntitiesWith;
import gov.nist.scap.content.model.IEntity;
import gov.nist.scap.content.model.IEntityVisitor;
import gov.nist.scap.content.model.IKey;
import gov.nist.scap.content.model.IKeyedEntity;
import gov.nist.scap.content.model.IVersion;
import gov.nist.scap.content.model.definitions.IEntityDefinition;
import gov.nist.scap.content.model.definitions.IExternalIdentifier;
import gov.nist.scap.content.model.definitions.IKeyedEntityDefinition;
import gov.nist.scap.content.model.definitions.ProcessingException;
import gov.nist.scap.content.semantic.entity.EntityProxy;
import gov.nist.scap.content.semantic.entity.KeyedEntityProxy;
import gov.nist.scap.content.semantic.translation.EntityMetadataMap;
import gov.nist.scap.content.semantic.translation.KeyTranslator;
import gov.nist.scap.content.semantic.translation.ToRDFEntityVisitor;
import gov.nist.scap.content.shredder.rules.xmlbeans.XmlbeansRules;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.xmlbeans.XmlException;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.openrdf.sail.memory.MemoryStore;
import org.scapdev.content.core.persistence.hybrid.ContentRetrieverFactory;
import org.scapdev.content.core.persistence.hybrid.MetadataStore;
import org.scapdev.content.core.query.entity.EntityQuery;
import org.scapdev.content.core.query.entity.Key;
import org.scapdev.content.core.query.sparql.EntityQueryParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * At this point this is just going to be a facade into the triple store REST
 * interfaces
 */
public class TripleStoreFacadeMetaDataManager implements MetadataStore,
        IPersistenceContext {

    public static final String TRIPLE_STORE_DIR =
        TripleStoreFacadeMetaDataManager.class.getCanonicalName()
            + ".TripleStoreDir";
    public static final String RULES_FILE =
        TripleStoreFacadeMetaDataManager.class.getCanonicalName()
            + ".RulesFile";

    private static final Logger log =
        LoggerFactory.getLogger(TripleStoreFacadeMetaDataManager.class);
    private static final String BASE_URI =
        "http://scap.nist.gov/resource/content/individuals#";

    private Repository repository;

    private ValueFactory factory;

    private MetaDataOntology ontology;

    private TripleStoreQueryService queryService;

    private ContentRetrieverFactory contentRetrieverFactory;

    private Map<Object, RepositoryConnection> sessionMap =
        new HashMap<Object, RepositoryConnection>();

    private TripleStoreFacadeMetaDataManager(
            ContentRetrieverFactory contentRetrieverFactory) {
        this(
            contentRetrieverFactory,
            System.getProperty(TRIPLE_STORE_DIR),
            System.getProperty(RULES_FILE));
    }

    /**
     * The default constructor
     * 
     * @param contentRetrieverFactory the content retriever factory for the
     *            content store
     * @param tripleStoreDir the path to the triple store (can be null)
     * @param rulesPath the path to the rules file
     */
    public TripleStoreFacadeMetaDataManager(
            ContentRetrieverFactory contentRetrieverFactory,
            String tripleStoreDir,
            String rulesPath) {
        // NOTE: this type is non-inferencing, see
        // http://www.openrdf.org/doc/sesame2/2.3.2/users/ch08.html for more
        // detail
        try {
            boolean loadRules = false;
            if (tripleStoreDir != null) {
                File f = new File(tripleStoreDir);
                if (!f.exists()) {
                    if (!f.mkdir()) {
                        throw new RuntimeException(
                            "Could not access or create triple store directory");
                    }
                }
                if (!f.isDirectory()) {
                    throw new RuntimeException(
                        "Triple store directory is not a directory");
                }

                if (f.list().length == 0) {
                    loadRules = true;
                }
                MemoryStore ms = new MemoryStore(new File(tripleStoreDir));
                ms.setPersist(true);
                // prevent file lock issues
                ms.setSyncDelay(1000L);
                repository =
                    new SailRepository(new ForwardChainingRDFSInferencer(ms));

            } else {
                repository =
                    new SailRepository(new ForwardChainingRDFSInferencer(
                        new MemoryStore()));
                loadRules = true;
            }

            // repository = new
            // HTTPRepository("http://localhost:8080/openrdf-sesame",
            // "scapCmsTest");

            repository.initialize();
            factory = repository.getValueFactory();
            ontology = new MetaDataOntology(factory);
            queryService = new TripleStoreQueryService(this);
            this.contentRetrieverFactory = contentRetrieverFactory;

            InputStream is;
            if (rulesPath == null) {
                throw new RuntimeException("System property must be set: "
                    + RULES_FILE);
            }
            if (rulesPath.startsWith("/")) {
                is =
                    TripleStoreFacadeMetaDataManager.class.getResourceAsStream(rulesPath);
                if (is == null) {
                    throw new RuntimeException(
                        "Could not find file on class path: " + rulesPath);
                }
            } else {
                File f = new File(rulesPath);
                if (!f.exists()) {
                    throw new RuntimeException("File does not exist: "
                        + f.getCanonicalPath());
                }
                is = new BufferedInputStream(new FileInputStream(f));
            }
            XmlbeansRules xmlbeansRules = new XmlbeansRules(is);

            if (loadRules) {
                ontology.loadModel(repository.getConnection(), xmlbeansRules);
            } else {
                ontology.loadModel(null, xmlbeansRules);
            }
        } catch (RepositoryException e) {
            log.error("Exception iniitalizing triple store", e);
        } catch (XmlException e) {
            log.error("Exception iniitalizing triple store", e);
        } catch (IOException e) {
            log.error("Exception iniitalizing triple store", e);
        }

    }

    // Used for debuggings
    // public void writeOutAllStatements (OutputStream os) throws
    // RepositoryException, UnsupportedEncodingException, IOException {
    // Set<Statement> result =
    // Iterations.addAll(
    // repository.getConnection().getStatements(null, null, null, true),
    // new HashSet<Statement>());
    // for( Statement s : result ) {
    // logStatement(
    // s.getSubject(),
    // s.getPredicate(),
    // s.getObject(),
    // s.getContext(), os);
    // }
    //
    // }
    //
    // private void logStatement(
    // Resource subject,
    // URI predicate,
    // Value object,
    // Resource context, OutputStream os) throws UnsupportedEncodingException,
    // IOException {
    // StringBuilder sb = new StringBuilder();
    // if (context != null) {
    // sb.append("[" + context + "] ");
    // }
    // sb.append(subject.stringValue() + " " + predicate.stringValue() + " "
    // + object.stringValue());
    // os.write((sb.toString() + "\n").getBytes("UTF-8"));
    // }

    /**
     * get an instance of this manager
     * 
     * @param contentRetrieverFactory a factory to create content retrievers
     *            (relative to the persistence store)
     * @return this manager
     */
    public static TripleStoreFacadeMetaDataManager getInstance(
            ContentRetrieverFactory contentRetrieverFactory) {
        return new TripleStoreFacadeMetaDataManager(contentRetrieverFactory);
    }

    /**
     * TODO delete this method later...it's only for testing
     * 
     * @param sparql
     * @return
     */
    public List<SortedMap<String, String>> runSPARQL(String sparql)
            throws RepositoryException {
        try {
            RepositoryConnection conn = repository.getConnection();
            TupleQuery tupleQuery =
                conn.prepareTupleQuery(QueryLanguage.SPARQL, sparql);
            TupleQueryResult result = tupleQuery.evaluate();
            try {
                List<SortedMap<String, String>> returnVal =
                    new LinkedList<SortedMap<String, String>>();
                BindingSet bs;
                while (result.hasNext()) {
                    SortedMap<String, String> sm =
                        new TreeMap<String, String>();
                    bs = result.next();
                    for (String key : bs.getBindingNames()) {
                        sm.put(key, bs.getValue(key).stringValue());
                    }
                    returnVal.add(sm);
                }
                return returnVal;
            } finally {
                result.close();
                conn.close();
            }
        } catch (MalformedQueryException e) {
            throw new RepositoryException(e);
        } catch (QueryEvaluationException e) {
            throw new RepositoryException(e);
        } catch (RepositoryException e) {
            throw new RepositoryException(e);
        }

    }

    @Override
    public <T extends IEntity<?>> Collection<? extends T> getEntities(
            EntityQuery query,
            Class<T> clazz) throws ProcessingException {
        try {
            RepositoryConnection conn = repository.getConnection();
            try {
                Constructor<T> c =
                    clazz.getConstructor(IPersistenceContext.class, URI.class);

                TupleQuery tupleQuery = EntityQueryParser.parse(query, conn);

                TupleQueryResult result = tupleQuery.evaluate();
                Set<URI> uris = new HashSet<URI>();
                while (result.hasNext()) {
                    Value value =
                        result.next().getValue(
                            EntityQueryParser.ENTITY_URI_VARIABLE_NAME);
                    uris.add((URI)value);
                }
                Set<T> returnSet = new HashSet<T>();
                for (URI u : uris) {
                    returnSet.add(c.newInstance(this, u));
                }
                return Collections.unmodifiableCollection(returnSet);
            } catch (QueryEvaluationException e) {
                throw new ProcessingException(e);
            } catch (MalformedQueryException e) {
                throw new ProcessingException(e);
            } catch (SecurityException e) {
                throw new ProcessingException(e);
            } catch (NoSuchMethodException e) {
                throw new ProcessingException(
                    "Class "
                        + clazz.getCanonicalName()
                        + " must have a constructor that takes (gov.nist.scap.content.semantic.IPersistenceContext ipc, org.openrdf.model.URI entityURI)",
                    e);
            } catch (IllegalArgumentException e) {
                throw new ProcessingException(e);
            } catch (InstantiationException e) {
                throw new ProcessingException(e);
            } catch (IllegalAccessException e) {
                throw new ProcessingException(e);
            } catch (InvocationTargetException e) {
                throw new ProcessingException(e);
            } finally {
                conn.close();
            }

        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            throw new ProcessingException(e);
        }
    }

    @Override
    public Collection<? extends IKeyedEntity<?>> getEntities(
            IKey key,
            IVersion version) throws ProcessingException {
        // try {
        // RepositoryConnection conn = repository.getConnection();
        // try {
        // Set<URI> uris = queryService.findEntityURIs(key, version);
        // Set<IKeyedEntity<?>> returnSet = new HashSet<IKeyedEntity<?>>();
        // for (URI u : uris) {
        // returnSet.add(new KeyedEntityProxy<IKeyedEntityDefinition,
        // IKeyedEntity<IKeyedEntityDefinition>>(
        // this,
        // u));
        // }
        // return Collections.unmodifiableCollection(returnSet);
        // } finally {
        // conn.close();
        // }
        //
        // } catch (RepositoryException e) {
        // log.error(e);
        // }
        // return null;

        List<Key.Field> fields = new LinkedList<Key.Field>();
        for (String fieldName : key.getFieldNames()) {
            fields.add(Key.field(fieldName, key.getValue(fieldName)));
        }
        Key qKey = Key.key(key.getId(), fields.toArray(new Key.Field[0]));

        if (version != null) {
            //TODO add version
        }
        EntityQuery query = selectEntitiesWith(qKey);
        @SuppressWarnings("unchecked")
        Collection<? extends KeyedEntityProxy<?,?>> coll = (Collection<? extends KeyedEntityProxy<?,?>>)getEntities(query, KeyedEntityProxy.class);
        return coll;
    }

    @Override
    public IEntity<?> getEntity(String contentId) {
        try {
            RepositoryConnection conn = repository.getConnection();
            try {
                URI entityURI =
                    queryService.findEntityURIbyContentId(contentId);
                if (entityURI != null) {
                    return new EntityProxy<IKeyedEntityDefinition, IKeyedEntity<IKeyedEntityDefinition>>(
                        this,
                        entityURI);
                }
            } finally {
                conn.close();
            }

        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    @Deprecated
    public Map<String, Set<? extends IKey>> getKeysForBoundaryIdentifier(
            IExternalIdentifier externalIdentifier,
            Collection<String> boundaryObjectIds,
            Set<? extends IEntityDefinition> entityTypes) {
        try {
            RepositoryConnection conn = repository.getConnection();
            try {
                Map<String, List<URI>> entityURIs =
                    queryService.findEntityUrisFromBoundaryObjectIds(
                        externalIdentifier,
                        boundaryObjectIds,
                        entityTypes,
                        conn);
                return findEntityKeys(entityURIs, this);

            } catch (MalformedQueryException e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e);
            } catch (QueryEvaluationException e2) {
                log.error(e2.getMessage(), e2);
                throw new RuntimeException(e2);
            } finally {
                conn.close();
            }

        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<String> persist(
            LinkedHashMap<String, IEntity<?>> contentIdToEntityMap) {
        return persist(contentIdToEntityMap, null);
    }

    /*
     * TODO: NOTE: there is an issue in some cases where an Entity will be sent
     * in containing duplicate relationships. At present only the first of the
     * set of duplicates is added; from the context of the triple store this is
     * the only useful behavior. The duplicate relationships make sense within
     * the context of a larger XML document, but the abstraction of the
     * metamodel removes that context, and therefore removes the usefulness of
     * the duplicate relationships. An example of this is the
     * "urn:scap-content:relationship:org.mitre.oval:criterion" Keyed
     * relationship in an Oval definition. An oval def. may have multiple
     * criterion relationships that are all equal (i.e. reference same test)
     * except for the fact that they appear under distinct criteria operators;
     * since the metamodel does not capture this extra context the relationships
     * just appear to be exactly the same. WE NEED TO FIGURE OUT HOW TO HANDLE
     * THIS.
     */
    @Override
    public List<String> persist(
            LinkedHashMap<String, IEntity<?>> contentIdToEntityMap,
            Object session) {
        List<String> returnVal = new LinkedList<String>();
        try {
            RepositoryConnection conn = repository.getConnection();
            conn.setAutoCommit(false);
            if (session != null) {
                sessionMap.put(session, conn);
            }
            try {
                EntityMetadataMap emm =
                    new DefaultURIToEntityMap(
                        factory,
                        BASE_URI,
                        contentIdToEntityMap);
                IEntityVisitor entityVisitor =
                    new ToRDFEntityVisitor(factory, ontology, emm, conn);
                for (Map.Entry<String, IEntity<?>> entry : contentIdToEntityMap.entrySet()) {
                    entry.getValue().accept(entityVisitor);
                    returnVal.add(emm.getContentId(entry.getValue()));
                }

            } finally {
                if (session == null) {
                    conn.commit();
                    conn.close();
                }
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
        }
        return returnVal;
    }

    @Override
    public boolean commit(Object session) {
        RepositoryConnection conn = sessionMap.remove(session);
        if (conn != null) {
            try {
                conn.commit();
                conn.close();
            } catch (RepositoryException e) {
                log.error(e.getMessage(), e);
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean rollback(Object session) {
        RepositoryConnection conn = sessionMap.remove(session);
        if (conn != null) {
            try {
                conn.rollback();
                conn.close();
            } catch (RepositoryException e) {
                log.error(e.getMessage(), e);
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    private Map<String, Set<? extends IKey>> findEntityKeys(
            Map<String, List<URI>> entityURIs,
            IPersistenceContext ipc)
            throws RepositoryException, QueryEvaluationException,
            MalformedQueryException {
        KeyTranslator keyTranslator = new KeyTranslator(ontology);
        Map<String, Set<? extends IKey>> boundaryIdToKeyMap =
            new HashMap<String, Set<? extends IKey>>();
        for (Map.Entry<String, List<URI>> entry : entityURIs.entrySet()) {
            Set<IKey> map = new HashSet<IKey>();
            boundaryIdToKeyMap.put(entry.getKey(), map);
            for (URI entityURI : entry.getValue()) {
                IKey entityKey = keyTranslator.translateToJava(ipc, entityURI);
                map.add(entityKey);
            }
        }
        return boundaryIdToKeyMap;
    }

    @Override
    public void shutdown() {
        try {
            repository.shutDown();
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
        }
    }

    private class DefaultURIToEntityMap implements EntityMetadataMap {

        private final Map<IEntity<?>, String> entityToContentIdMap =
            new HashMap<IEntity<?>, String>();
        private final String baseURI;

        public DefaultURIToEntityMap(
                ValueFactory factory,
                String baseURI,
                Map<String, IEntity<?>> contentIdToEntityMap) {
            for (Map.Entry<String, IEntity<?>> entry : contentIdToEntityMap.entrySet()) {
                entityToContentIdMap.put(entry.getValue(), entry.getKey());
            }
            this.baseURI = baseURI;
        }

        @Override
        public URI getResourceURI(IEntity<?> entity) {
            return generateResourceId(entityToContentIdMap.get(entity));
        }

        @Override
        public String getContentId(IEntity<?> entity) {
            return entityToContentIdMap.get(entity);
        }

        private URI generateResourceId(String contentId) {
            return factory.createURI(baseURI + contentId);
        }

    }

    @Override
    public ContentRetrieverFactory getContentRetrieverFactory() {
        return contentRetrieverFactory;
    }

    @Override
    public MetaDataOntology getOntology() {
        return ontology;
    }

    @Override
    public Repository getRepository() {
        return repository;
    }

}