package gov.nist.scap.content.server;

import gov.nist.scap.content.model.IEntity;
import gov.nist.scap.content.model.IKey;
import gov.nist.scap.content.model.IKeyedEntity;
import gov.nist.scap.content.model.IVersion;
import gov.nist.scap.content.model.definitions.IEntityDefinition;
import gov.nist.scap.content.model.definitions.IExternalIdentifier;
import gov.nist.scap.content.model.definitions.ProcessingException;
import gov.nist.scap.content.model.definitions.collection.IMetadataModel;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.scapdev.content.core.ContentException;
import org.scapdev.content.core.persistence.IEntityFilter;

public interface IConnection {

	Collection<? extends IKeyedEntity<?>> getEntities(IKey key,
			IVersion version, IEntityFilter<IKeyedEntity<?>> filter)
			throws ProcessingException;
	Collection<? extends IEntity<?>> getEntities(
			IExternalIdentifier externalIdentifier,
			Set<String> boundaryObjectIds,
			Set<? extends IEntityDefinition> entityTypes,
			IEntityFilter<IEntity<?>> filter) throws ProcessingException;
    void storeEntities(List<? extends IEntity<?>> entities, IMetadataModel model)
            throws ContentException;
//
//    void shutdown();
//
}
