package gov.nist.scap.content.semantic.translation;

import gov.nist.scap.content.model.IContentNode;
import gov.nist.scap.content.model.IEntity;
import gov.nist.scap.content.model.IEntityVisitor;
import gov.nist.scap.content.model.IGeneratedDocument;
import gov.nist.scap.content.model.IKeyedDocument;
import gov.nist.scap.content.model.IRelationship;
import gov.nist.scap.content.model.IRelationshipVisitor;
import gov.nist.scap.content.semantic.MetaDataOntology;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

/**
 * Used to persist statements about an entity to the triple store. Only one
 * instance of this class is needed for unlimited entities.
 * 
 * @author Adam Halbardier
 */
public class ToRDFEntityVisitor implements IEntityVisitor {

    private final ValueFactory valueFactory;
    private final MetaDataOntology ontology;
    private final EntityMetadataMap entityMetadataMap;
    private final RepositoryConnection conn;

    /**
     * default constructor
     * 
     * @param valueFactory the value factory
     * @param ontology the ontology
     * @param entityMetadataMap a callback to get entity information
     * @param conn a repository connection
     */
    public ToRDFEntityVisitor(
            ValueFactory valueFactory,
            MetaDataOntology ontology,
            EntityMetadataMap entityMetadataMap,
            RepositoryConnection conn) {
        this.valueFactory = valueFactory;
        this.ontology = ontology;
        this.entityMetadataMap = entityMetadataMap;
        this.conn = conn;
    }

    @Override
    public void visit(IContentNode entity) {
        URI resourceId = entityMetadataMap.getResourceURI(entity);
        BNode context = valueFactory.createBNode();
        try {
            conn.add(
                createEntityStatements(
                    resourceId,
                    entity,
                    entityMetadataMap.getContentId(entity)),
                context);
            conn.add(
                createKeyRelationships(
                    resourceId,
                    entity.getKey().getId(),
                    entity.getKey().getFieldNameToValueMap()),
                context);
        } catch (RepositoryException e) {
            // TODO: Implement better error handling!!!
            throw new RuntimeException(e);
        }
        iterateThroughRelationship(resourceId, entity, context);
    }

    @Override
    public void visit(IGeneratedDocument entity) {
        URI resourceId = entityMetadataMap.getResourceURI(entity);
        BNode context = valueFactory.createBNode();
        try {
            conn.add(
                createEntityStatements(
                    resourceId,
                    entity,
                    entityMetadataMap.getContentId(entity)),
                context);
        } catch (RepositoryException e) {
            // TODO: Implement better error handling!!!
            throw new RuntimeException(e);
        }
        iterateThroughRelationship(resourceId, entity, context);
    }

    @Override
    public void visit(IKeyedDocument entity) {
        URI resourceId = entityMetadataMap.getResourceURI(entity);
        BNode context = valueFactory.createBNode();
        try {
            conn.add(
                createEntityStatements(
                    resourceId,
                    entity,
                    entityMetadataMap.getContentId(entity)),
                context);
            conn.add(
                createKeyRelationships(
                    resourceId,
                    entity.getKey().getId(),
                    entity.getKey().getFieldNameToValueMap()),
                context);
        } catch (RepositoryException e) {
            // TODO: Implement better error handling!!!
            throw new RuntimeException(e);
        }
        iterateThroughRelationship(resourceId, entity, context);
    }

    private void iterateThroughRelationship(
            URI resourceId,
            IEntity<?> e,
            Resource context) {
        IRelationshipVisitor visitor =
            new ToRDFRelationshipVisitor(
                valueFactory,
                ontology,
                entityMetadataMap,
                resourceId,
                conn,
                context);
        for (IRelationship<?> rel : e.getRelationships()) {
            rel.accept(visitor);
        }
    }

    private List<Statement> createEntityStatements(
            URI resourceId,
            IEntity<?> entity,
            String contentId) {
        List<Statement> target = new LinkedList<Statement>();
        target.add(valueFactory.createStatement(
            resourceId,
            ontology.HAS_CONTENT_ID.URI,
            valueFactory.createLiteral(contentId)));
        // Shouldn't this be a URI to the type?
        target.add(valueFactory.createStatement(
            resourceId,
            ontology.HAS_ENTITY_TYPE.URI,
            valueFactory.createLiteral(entity.getDefinition().getId())));
        if (entity.getParent() != null) {
            target.add(valueFactory.createStatement(
                resourceId,
                ontology.HAS_PARENT_RELATIONSHIP_TO.URI,
                entityMetadataMap.getResourceURI(entity.getParent())));
        }
        if (entity.getVersion() != null) {
            BNode versionBNode = valueFactory.createBNode();
            target.add(valueFactory.createStatement(
                resourceId,
                ontology.HAS_VERSION.URI,
                versionBNode));
            target.add(valueFactory.createStatement(
                versionBNode,
                ontology.HAS_VERSION_TYPE.URI,
                valueFactory.createURI(entity.getVersion().getDefinition().getMethod().getId())));
            target.add(valueFactory.createStatement(
                versionBNode,
                ontology.HAS_VERSION_VALUE.URI,
                valueFactory.createLiteral(entity.getVersion().getValue())));
        }
        return target;
    }

    private List<Statement> createKeyRelationships(
            URI resourceId,
            String entityKeyId,
            Map<String, String> fieldNameToValueMap) {
        List<Statement> target = new LinkedList<Statement>();

        // now handle the key of the entity, right now I see no reason to not
        // use bnodes....that may change
        BNode key = valueFactory.createBNode();
        // Is there a specific key ID? Probably not
        target.add(valueFactory.createStatement(
            resourceId,
            ontology.HAS_KEY.URI,
            key));

        target.add(valueFactory.createStatement(
            key,
            ontology.HAS_KEY_TYPE.URI,
            valueFactory.createLiteral(entityKeyId)));
        for (Map.Entry<String, String> keyFieldEntry : fieldNameToValueMap.entrySet()) {
            BNode keyField = valueFactory.createBNode();
            target.add(valueFactory.createStatement(
                key,
                ontology.HAS_FIELD_DATA.URI,
                keyField));
            target.add(valueFactory.createStatement(
                keyField,
                ontology.HAS_FIELD_NAME.URI,
                valueFactory.createLiteral(keyFieldEntry.getKey())));
            target.add(valueFactory.createStatement(
                keyField,
                ontology.HAS_FIELD_VALUE.URI,
                valueFactory.createLiteral(keyFieldEntry.getValue())));
        }
        return target;

    }

}
