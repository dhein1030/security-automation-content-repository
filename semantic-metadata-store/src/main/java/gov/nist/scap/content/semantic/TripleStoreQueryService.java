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

import gov.nist.scap.content.model.IKey;
import gov.nist.scap.content.model.definitions.IEntityDefinition;
import gov.nist.scap.content.model.definitions.IExternalIdentifier;
import gov.nist.scap.content.semantic.exceptions.NonUniqueResultException;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

/**
 * A service class to provide any triple store searching related services
 */
public class TripleStoreQueryService {
    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final Logger log =
        Logger.getLogger(TripleStoreQueryService.class);

    private ValueFactory factory;

    private MetaDataOntology ontology;

    private RepositoryConnection conn;

    /**
     * @param persistContext the context for the triple store
     * @throws RepositoryException error while getting connection to repository
     */
    public TripleStoreQueryService(IPersistenceContext persistContext)
            throws RepositoryException {
        this.factory = persistContext.getRepository().getValueFactory();
        this.ontology = persistContext.getOntology();
        this.conn = persistContext.getRepository().getConnection();
    }

    /**
     * method to find an EntityURI from triple store based on given key.
     * 
     * @param key - key to search on
     * @return URI of entity associated with key, or null if no entity is found
     * @throws RepositoryException error while executing query
     */
    public URI findEntityURI(IKey key) throws RepositoryException {
        try {
            String entityURIVariableName = "_e";
            TupleQuery tupleQuery =
                conn.prepareTupleQuery(
                    QueryLanguage.SERQL,
                    generateEntitySearchQuery(key, entityURIVariableName));
            TupleQueryResult result = tupleQuery.evaluate();
            try {
                BindingSet resultSet = null;
                int resultSize = 0;
                while (result.hasNext()) {
                    if (resultSize > 1) {
                        throw new NonUniqueResultException();
                    }
                    resultSet = result.next();
                    resultSize++;
                }
                if (resultSet != null) {
                    Value entityURI = resultSet.getValue(entityURIVariableName);
                    return factory.createURI(entityURI.stringValue());
                }
            } finally {
                result.close();
            }
            return null;
        } catch (MalformedQueryException e) {
            throw new RepositoryException(e);
        } catch (QueryEvaluationException e) {
            throw new RepositoryException(e);
        }

    }

    /**
     * method to find and entityURI by contentID
     * 
     * @param contentId the content ID to search for
     * @return the URI of the entity
     * @throws RepositoryException error while running query
     */
    URI findEntityURIbyContentId(String contentId) throws RepositoryException {
        try {
            String entityURIVariableName = "_e";
            TupleQuery tupleQuery =
                conn.prepareTupleQuery(
                    QueryLanguage.SERQL,
                    generateEntitySearchQuery(contentId, entityURIVariableName));
            TupleQueryResult result = tupleQuery.evaluate();
            try {
                BindingSet resultSet = null;
                int resultSize = 0;
                while (result.hasNext()) {
                    if (resultSize > 1) {
                        throw new NonUniqueResultException();
                    }
                    resultSet = result.next();
                    resultSize++;
                }
                if (resultSet != null) {
                    Value entityURI = resultSet.getValue(entityURIVariableName);
                    return factory.createURI(entityURI.stringValue());
                }
            } finally {
                result.close();
            }
            return null;
        } catch (MalformedQueryException e) {
            throw new RepositoryException(e);
        } catch (QueryEvaluationException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * <p>
     * method that will generate list of all entityURIs of entities related to
     * owningEntityURI through a HAS_KEYED_RELATIONSHIP predicate.
     * </p>
     * 
     * @param owningEntityURI the base entity URI
     * @return the list of related URIs
     * @throws RepositoryException error while running query
     */
    public List<URI> findAllRelatedEntityURIs(URI owningEntityURI)
            throws RepositoryException {
        try {
            String relatedEntityVariableName = "_e";
            TupleQuery tupleQuery =
                conn.prepareTupleQuery(
                    QueryLanguage.SERQL,
                    generateRelatedEntitySearchQuery(
                        owningEntityURI,
                        relatedEntityVariableName));
            TupleQueryResult result = tupleQuery.evaluate();
            List<URI> relatedEntityURIs = new LinkedList<URI>();
            try {
                BindingSet resultSet = null;
                while (result.hasNext()) {
                    resultSet = result.next();
                    Value entityURI =
                        resultSet.getValue(relatedEntityVariableName);
                    relatedEntityURIs.add(factory.createURI(entityURI.stringValue()));
                }
            } finally {
                result.close();
            }
            return relatedEntityURIs;
        } catch (MalformedQueryException e) {
            throw new RepositoryException(e);
        } catch (QueryEvaluationException e) {
            throw new RepositoryException(e);
        }

    }

    /**
     * Find all entityURIs associated with the specific boundary objects,
     * filtered based on entity type.
     * 
     * @param externalIdentifier the boundary object type
     * @param boundaryIdentifierValues the boundary object values
     * @param entityTypes the entity types to filter on
     * @param conn the repository connection
     * @return a map with boundary object values as keys, and lists of URIs that
     *         reference those boundary object as values
     * @throws RepositoryException error while executing query
     */
    Map<String, List<URI>> findEntityUrisFromBoundaryObjectIds(
            IExternalIdentifier externalIdentifier,
            Collection<String> boundaryIdentifierValues,
            Set<? extends IEntityDefinition> entityTypes,
            RepositoryConnection conn) throws RepositoryException {
        try {
            String entityURIVariableName = "_e";
            String boundaryIdVariableName = "_boundaryObjectValue";
            TupleQuery tupleQuery =
                conn.prepareTupleQuery(
                    QueryLanguage.SERQL,
                    generateRelatedBoundaryObjectSearchString(
                        externalIdentifier.getId(),
                        boundaryIdentifierValues,
                        entityTypes,
                        boundaryIdVariableName,
                        entityURIVariableName));
            TupleQueryResult result = tupleQuery.evaluate();
            Map<String, List<URI>> relatedEntityURIs =
                new HashMap<String, List<URI>>();
            try {
                BindingSet resultSet = null;
                while (result.hasNext()) {
                    resultSet = result.next();
                    Value boundaryId =
                        resultSet.getValue(boundaryIdVariableName);
                    Value entityURI = resultSet.getValue(entityURIVariableName);
                    if (!relatedEntityURIs.containsKey(boundaryId.stringValue())) {
                        relatedEntityURIs.put(
                            boundaryId.stringValue(),
                            new LinkedList<URI>());
                    }
                    relatedEntityURIs.get(boundaryId.stringValue()).add(
                        factory.createURI(entityURI.stringValue()));
                }
            } finally {
                result.close();
            }
            return relatedEntityURIs;
        } catch (MalformedQueryException e) {
            throw new RepositoryException(e);
        } catch (QueryEvaluationException e) {
            throw new RepositoryException(e);
        }

    }

    /**
     * Find all entities of a given type
     * 
     * @param entityTypeId the entity type ID
     * @return list of URI of the entities of the given type
     * @throws RepositoryException error while executing query
     */
    List<URI> findEntityURIsByEntityType(String entityTypeId)
            throws RepositoryException {
        try {
            String entityURIVariableName = "_e";
            TupleQuery tupleQuery =
                conn.prepareTupleQuery(
                    QueryLanguage.SERQL,
                    generateEntitiesFromEntityTypeSearchString(
                        entityTypeId,
                        entityURIVariableName));
            TupleQueryResult result = tupleQuery.evaluate();
            List<URI> relatedEntityURIs = new LinkedList<URI>();
            try {
                BindingSet resultSet = null;
                while (result.hasNext()) {
                    resultSet = result.next();
                    Value entityURI = resultSet.getValue(entityURIVariableName);
                    relatedEntityURIs.add(factory.createURI(entityURI.stringValue()));
                }
            } finally {
                result.close();
            }
            return relatedEntityURIs;
        } catch (MalformedQueryException e) {
            throw new RepositoryException(e);
        } catch (QueryEvaluationException e) {
            throw new RepositoryException(e);
        }

    }

    /**
     * Find all relationships associated with the specific entity
     * 
     * @param entityUri the target entity
     * @return the relationships
     * @throws RepositoryException error while running query
     */
    List<String> findAllRelationshipsFromEntity(URI entityUri)
            throws RepositoryException {
        try {
            String relationshipPredicateVariableName = "_p";
            TupleQuery tupleQuery =
                conn.prepareTupleQuery(
                    QueryLanguage.SERQL,
                    generateRelationshipsFromEntitySearchString(
                        relationshipPredicateVariableName,
                        entityUri));
            TupleQueryResult result = tupleQuery.evaluate();
            List<String> relationshipIds = new LinkedList<String>();
            try {
                BindingSet resultSet = null;
                while (result.hasNext()) {
                    resultSet = result.next();
                    Value relationshipPredicate =
                        resultSet.getValue(relationshipPredicateVariableName);
                    relationshipIds.add(relationshipPredicate.stringValue());
                }
            } finally {
                result.close();
            }
            return relationshipIds;
        } catch (MalformedQueryException e) {
            throw new RepositoryException(e);
        } catch (QueryEvaluationException e) {
            throw new RepositoryException(e);
        }
    }

    private String generateRelationshipsFromEntitySearchString(
            String relationshipPredicateVariable,
            URI entityURI) {
        String unusedObject = "_o";
        String topLevelPredicateVariableName = "_topLevelPredicate";

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ").append(relationshipPredicateVariable).append(
            " ").append(NEW_LINE);
        // _e relationshipPredicateVariable _o
        queryBuilder.append("FROM {<").append(entityURI).append(">} ");
        queryBuilder.append(relationshipPredicateVariable).append(" ");
        queryBuilder.append("{").append(unusedObject).append("}").append(",").append(
            NEW_LINE);
        // relationshipPredicateVariable rdfs:subPropertyOf _topLevelPredicate
        queryBuilder.append("{").append(relationshipPredicateVariable).append(
            "}").append(" ");
        queryBuilder.append(" <").append(RDFS.SUBPROPERTYOF).append("> ");
        queryBuilder.append("{").append(topLevelPredicateVariableName).append(
            "}").append(NEW_LINE);
        // WHERE _topLevelPredicate = HAS_INDIRECT_REL OR HAS_DIRECT_REL
        queryBuilder.append("WHERE").append(NEW_LINE);
        queryBuilder.append(topLevelPredicateVariableName).append(" = <").append(
            ontology.HAS_KEYED_RELATIONSHIP_TO.URI).append(">").append(NEW_LINE);
        queryBuilder.append("OR").append(NEW_LINE);
        queryBuilder.append(topLevelPredicateVariableName).append(" = <").append(
            ontology.HAS_BOUNDARY_OBJECT_RELATIONSHIP_TO.URI).append(">").append(
            NEW_LINE);

        return queryBuilder.toString();
    }

    private String generateRelatedBoundaryObjectSearchString(
            String boudaryObjectType,
            Collection<String> boundaryIdentifierValues,
            Set<? extends IEntityDefinition> entityTypes,
            String boundaryIdVariableName,
            String entityURIVariableName) {
        String entityTypeVariable = "entityType";
        String boundaryObjectVariable = "boundaryObject";

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ").append(boundaryIdVariableName).append(
            ", ").append(entityURIVariableName).append(" ").append(NEW_LINE);
        // _e hasEntityType {entityType};
        queryBuilder.append("FROM {").append(entityURIVariableName).append("} ");
        queryBuilder.append("  <").append(ontology.HAS_ENTITY_TYPE.URI).append(
            "> ");
        queryBuilder.append("{").append(entityTypeVariable).append("}").append(
            ";").append(NEW_LINE);
        // hasIndirectRelationship {boundaryObject},
        queryBuilder.append("  <").append(
            ontology.HAS_BOUNDARY_OBJECT_RELATIONSHIP_TO.URI).append("> ");
        queryBuilder.append("{").append(boundaryObjectVariable).append("}").append(
            ",").append(NEW_LINE);
        // {boundaryObject} hasType {boundaryObjectType};
        queryBuilder.append("{").append(boundaryObjectVariable).append("}").append(
            " ");
        queryBuilder.append("<").append(ontology.HAS_BOUNDARY_OBJECT_TYPE.URI).append(
            "> ");
        queryBuilder.append("{\"").append(boudaryObjectType).append("\"};").append(
            NEW_LINE);
        // hasValue {boundaryObjectValue}
        queryBuilder.append("<").append(ontology.HAS_BOUNDARY_OBJECT_VALUE.URI).append(
            "> ");
        queryBuilder.append("{").append(boundaryIdVariableName).append("}").append(
            " ").append(NEW_LINE);
        // WHERE
        queryBuilder.append("WHERE").append(NEW_LINE);
        if (!entityTypes.isEmpty()) {
            Collection<String> target = new HashSet<String>();
            for (IEntityDefinition ied : entityTypes) {
                target.add(ied.getId());
            }
            queryBuilder.append(entityTypeVariable).append(" IN ").append(
                convertStringSetForInClause(target)).append(NEW_LINE);
            queryBuilder.append("AND").append(NEW_LINE);
        }
        queryBuilder.append(boundaryIdVariableName).append(" IN ").append(
            convertStringSetForInClause(boundaryIdentifierValues)).append(
            NEW_LINE);

        return queryBuilder.toString();
    }

    private String generateEntitiesFromEntityTypeSearchString(
            String entityType,
            String entityURIVariableName) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ").append(entityURIVariableName).append(" ").append(
            NEW_LINE);
        // _e hasEntityType {entityType};
        queryBuilder.append("FROM {").append(entityURIVariableName).append("} ");
        queryBuilder.append("<").append(ontology.HAS_ENTITY_TYPE.URI).append(
            "> ");
        queryBuilder.append("{\"").append(entityType).append("\"}");
        return queryBuilder.toString();
    }

    private static String convertStringSetForInClause(Collection<String> target) {
        StringBuilder inClauseBuilder = new StringBuilder();
        inClauseBuilder.append("(");
        boolean beginning = true;
        for (String atom : target) {
            if (beginning) {
                beginning = false;
            } else {
                inClauseBuilder.append(", ");
            }
            inClauseBuilder.append("\"").append(atom).append("\"");
        }
        inClauseBuilder.append(")");
        return inClauseBuilder.toString();
    }

    private String generateRelatedEntitySearchQuery(
            URI owningEntityURI,
            String relatedEntityVariableName) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ").append(relatedEntityVariableName).append(
            " ").append(NEW_LINE);
        // _e hasDirectRelationshipTo relatedEntityURI
        queryBuilder.append("FROM {<").append(owningEntityURI).append(">} ");
        queryBuilder.append("<").append(ontology.HAS_KEYED_RELATIONSHIP_TO.URI).append(
            ">");
        queryBuilder.append(" {").append(relatedEntityVariableName).append("}").append(
            NEW_LINE);
        return queryBuilder.toString();
    }

    /**
     * <p>
     * Produces the list of all triples required to rebuild all entity keys
     * related to owningEntityURI through a HAS_KEYED_RELATIONSHIP predicate.
     * </p>
     * 
     * @param owningEntityURI the target entity URI
     * @return the resulting triples
     * @throws RepositoryException error while running the query
     */
    List<Statement> findRelatedEntityKeyStatements(URI owningEntityURI)
            throws RepositoryException {
        try {
            GraphQuery graphQuery =
                conn.prepareGraphQuery(
                    QueryLanguage.SERQL,
                    generateRelatedEntityKeyConstructQuery(owningEntityURI));
            GraphQueryResult result = graphQuery.evaluate();
            List<Statement> statements = new LinkedList<Statement>();
            try {
                while (result.hasNext()) {
                    Statement statement = result.next();
                    log.info(statement.getSubject() + " "
                        + statement.getPredicate() + " "
                        + statement.getObject());
                    statements.add(statement);

                }
            } finally {
                result.close();
            }
            return statements;
        } catch (MalformedQueryException e) {
            throw new RepositoryException(e);
        } catch (QueryEvaluationException e) {
            throw new RepositoryException(e);
        }

    }

    private String generateEntitySearchQuery(
            String contentId,
            String entityURIVariableName) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ").append(entityURIVariableName).append(" ").append(
            NEW_LINE);
        // _e hasContentId contentId
        queryBuilder.append("FROM {").append(entityURIVariableName).append("} ");
        queryBuilder.append("<").append(ontology.HAS_CONTENT_ID.URI).append(">");
        queryBuilder.append(" {\"").append(contentId).append("\"}").append(
            NEW_LINE);
        return queryBuilder.toString();
    }

    private String generateRelatedEntityKeyConstructQuery(URI owningEntityURI) {
        StringBuilder queryBuilder = new StringBuilder();
        String relatedEntityVariable = "{relEntity}";
        String relatedKeyVariable = "{relKey}";
        String relatedKeyTypeVariable = "{relKeyType}";
        String relatedKeyFieldVariable = "{keyField}";
        String relatedKeyFieldIdVariable = "{fieldId}";
        String relatedKeyFieldValueVariable = "{fieldValue}";
        // CONSTRUCT {relEntity} hasKey {relKey}
        queryBuilder.append("CONSTRUCT ").append(relatedEntityVariable).append(
            " <").append(ontology.HAS_KEY.URI).append("> ").append(
            relatedKeyVariable).append(",").append(NEW_LINE);
        // {relKey} hasKeyType {keyType};
        queryBuilder.append(relatedKeyVariable).append(" <").append(
            ontology.HAS_KEY_TYPE.URI).append("> ").append(
            relatedKeyTypeVariable).append(";").append(NEW_LINE);
        // hasKeyField {keyField},
        queryBuilder.append("<").append(ontology.HAS_FIELD_DATA.URI).append(
            "> ").append(relatedKeyFieldVariable).append(",").append(NEW_LINE);
        // {keyField} hasFieldId {fieldId};
        queryBuilder.append(relatedKeyFieldVariable).append(" <").append(
            ontology.HAS_FIELD_NAME.URI).append("> ").append(
            relatedKeyFieldIdVariable).append(";").append(NEW_LINE);
        // hasFieldValue {fieldVAlue}
        queryBuilder.append("<").append(ontology.HAS_FIELD_VALUE.URI).append(
            "> ").append(relatedKeyFieldValueVariable).append(NEW_LINE);
        // FROM {owningEntity hasDirectRelationshipTo {relEntity},
        queryBuilder.append("FROM {<").append(owningEntityURI).append(">} <").append(
            ontology.HAS_KEYED_RELATIONSHIP_TO.URI).append("> ").append(
            relatedEntityVariable).append(",").append(NEW_LINE);
        // {relEntity} hasKey {relKey}
        queryBuilder.append(relatedEntityVariable).append(" <").append(
            ontology.HAS_KEY.URI).append("> ").append(relatedKeyVariable).append(
            ",").append(NEW_LINE);
        // {relKey} hasKeyType {keyType};
        queryBuilder.append(relatedKeyVariable).append(" <").append(
            ontology.HAS_KEY_TYPE.URI).append("> ").append(
            relatedKeyTypeVariable).append(";").append(NEW_LINE);
        // hasKeyField {keyField},
        queryBuilder.append("<").append(ontology.HAS_FIELD_DATA.URI).append(
            "> ").append(relatedKeyFieldVariable).append(",").append(NEW_LINE);
        // {keyField} hasFieldId {fieldId};
        queryBuilder.append(relatedKeyFieldVariable).append(" <").append(
            ontology.HAS_FIELD_NAME.URI).append("> ").append(
            relatedKeyFieldIdVariable).append(";").append(NEW_LINE);
        // hasFieldValue {fieldVAlue}
        queryBuilder.append("<").append(ontology.HAS_FIELD_VALUE.URI).append(
            "> ").append(relatedKeyFieldValueVariable);
        return queryBuilder.toString();
    }

    private String generateEntitySearchQuery(
            IKey key,
            String entityURIVariableName) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ").append(entityURIVariableName).append(" ").append(
            NEW_LINE);
        // _e hasKey _key
        queryBuilder.append("FROM {").append(entityURIVariableName).append("} ");
        queryBuilder.append("<").append(ontology.HAS_KEY.URI).append(">");
        queryBuilder.append(" {_key},").append(NEW_LINE);
        // _key hasKeyType _keyType
        queryBuilder.append("{_key} ");
        queryBuilder.append("<").append(ontology.HAS_KEY_TYPE.URI).append(">");
        queryBuilder.append(" {\"").append(key.getId()).append("\"},").append(
            NEW_LINE);
        int fieldNumber = 1;
        for (Map.Entry<String, String> keyFieldEntry : key.getFieldNameToValueMap().entrySet()) {
            if (fieldNumber > 1) {
                queryBuilder.append(",").append(NEW_LINE);
            }
            // _key hasField _fieldX
            String fieldNameDeclaration = "{_field" + fieldNumber + "}";
            queryBuilder.append("{_key} ");
            queryBuilder.append("<").append(ontology.HAS_FIELD_DATA.URI).append(
                ">");
            queryBuilder.append(" ").append(fieldNameDeclaration).append(",").append(
                NEW_LINE);
            // _fieldx hasFieldType type
            queryBuilder.append(fieldNameDeclaration);
            queryBuilder.append(" <").append(ontology.HAS_FIELD_NAME.URI).append(
                ">");
            queryBuilder.append(" {\"").append(keyFieldEntry.getKey()).append(
                "\"},").append(NEW_LINE);
            // _fieldx hasFieldValue value
            queryBuilder.append(fieldNameDeclaration);
            queryBuilder.append(" <").append(ontology.HAS_FIELD_VALUE.URI).append(
                ">");
            queryBuilder.append(" {\"").append(keyFieldEntry.getValue()).append(
                "\"}");
            fieldNumber++;
        }
        return queryBuilder.toString();
    }

    /**
     * Helper method to find the context associated with the triples necessary
     * to re-constitute an entity
     * 
     * @param entityURI the target entity
     * @return the context associated with the target entity
     * @throws RepositoryException error while executing query
     */
    public Resource findEntityContext(URI entityURI)
            throws RepositoryException {
        try {
            StringBuilder queryBuilder = new StringBuilder();
            String sourceVariableName = "source";
            queryBuilder.append("SELECT source FROM CONTEXT ").append(
                sourceVariableName).append(" {<");
            queryBuilder.append(entityURI).append(">} ");
            queryBuilder.append("<").append(RDF.TYPE).append("> ");
            queryBuilder.append("{<").append(ontology.ENTITY_CLASS.URI).append(
                ">} ");
            TupleQuery tupleQuery =
                conn.prepareTupleQuery(
                    QueryLanguage.SERQL,
                    queryBuilder.toString());
            TupleQueryResult result = tupleQuery.evaluate();
            BindingSet resultSet = null;
            int resultSize = 0;
            while (result.hasNext()) {
                if (resultSize > 1) {
                    throw new NonUniqueResultException();
                }
                resultSet = result.next();
                resultSize++;
            }
            if (resultSet != null) {
                Value entityContextURI = resultSet.getValue(sourceVariableName);
                return factory.createBNode(entityContextURI.stringValue());
            }
            return null;
        } catch (MalformedQueryException e) {
            throw new RepositoryException(e);
        } catch (QueryEvaluationException e) {
            throw new RepositoryException(e);
        }

    }

}