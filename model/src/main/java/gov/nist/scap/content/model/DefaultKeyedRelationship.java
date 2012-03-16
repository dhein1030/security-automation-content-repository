package gov.nist.scap.content.model;

import gov.nist.scap.content.model.definitions.IKeyedRelationshipDefinition;

public class DefaultKeyedRelationship extends AbstractReferencingRelationship<IKeyedRelationshipDefinition, IKeyedEntity<?>> implements IKeyedRelationship {
	public DefaultKeyedRelationship(IKeyedRelationshipDefinition definition,
			IEntity<?> owningEntity, IKeyedEntity<?> referencedEntity) {
		super(definition, owningEntity, referencedEntity);
	}

	public IKey getKey() {
		return getReferencedEntity().getKey();
	}

	public void accept(IRelationshipVisitor visitor) {
		visitor.visit(this);
	}
}
