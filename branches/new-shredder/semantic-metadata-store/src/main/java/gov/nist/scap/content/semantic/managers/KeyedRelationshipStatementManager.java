/*******************************************************************************
 * The MIT License
 * 
 * Copyright (c) 2011 paul
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
package gov.nist.scap.content.semantic.managers;

import gov.nist.scap.content.model.IKey;
import gov.nist.scap.content.model.IKeyedRelationship;
import gov.nist.scap.content.model.definitions.IKeyedRelationshipDefinition;
import gov.nist.scap.content.model.definitions.collection.IMetadataModel;
import gov.nist.scap.content.semantic.IPersistenceContext;
import gov.nist.scap.content.semantic.MetaDataOntology;
import gov.nist.scap.content.semantic.builders.KeyedRelationshipBuilder;
import gov.nist.scap.content.semantic.entity.EntityBuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;

public class KeyedRelationshipStatementManager implements
		RegenerationStatementManager {
	private MetaDataOntology ontology;
	
	//all IDs of directRelationships
	private Collection<String> directRelationshipIds;
	
	// keys of all relatedEntities
	private Map<URI, IKey> relatedEntityKeys;
	
	// key = relatedEntityURI....this map is what this class builds
	private Map<URI, KeyedRelationshipBuilder> keyedRelationships = new HashMap<URI, KeyedRelationshipBuilder>();

	private IPersistenceContext ipc;

	/**
	 * The default constructor
	 * @param ipc the persistence context
	 * @param relatedEntityKeys all of the keys that are related to this entity via a HAS_KEYED_RELATIONSHIP
	 */
	public KeyedRelationshipStatementManager(IPersistenceContext ipc, Map<URI, IKey> relatedEntityKeys) {
		this.ontology = ipc.getOntology();
		this.relatedEntityKeys = relatedEntityKeys;
		this.directRelationshipIds = ontology.getKeyedRelationshipIds();
		this.ipc = ipc;
	}
	
	@Override
	public boolean scan(Statement statement) {
		URI predicate = statement.getPredicate();
		if (directRelationshipIds.contains(predicate.stringValue())){
			// a triple containing an indirect relationship predicate found
			populateKeyedRelationshipInfo(ontology, statement);
			return true;
		}
		return false;
	}
	
	@Override
	public void populateEntity(EntityBuilder builder) {
		for (Map.Entry<URI, KeyedRelationshipBuilder> entry : keyedRelationships.entrySet()){
			URI relatedEntityURI = entry.getKey();
			KeyedRelationshipBuilder keyedRelBuilder = entry.getValue();
			keyedRelBuilder.setRelatedEntityKey(relatedEntityKeys.get(relatedEntityURI));
			IKeyedRelationship keyedRelationship = keyedRelBuilder.build(ontology);
			builder.addRelationship(keyedRelationship);
		}
	}

	private void populateKeyedRelationshipInfo(IMetadataModel model2,
			Statement statement) {
		// hit on an keyedRelationship (called directRelationship in ontology) of some type
		String keyedRelationshipId = statement.getPredicate().stringValue();
		// this cast is okay because the statement has been vetted before getting here
		URI relatedEntityURI = (URI)statement.getObject();
		KeyedRelationshipBuilder keyedRelBuilder = keyedRelationships.get(relatedEntityURI);
		if (keyedRelBuilder == null){
			keyedRelBuilder = new KeyedRelationshipBuilder(ipc);
            keyedRelationships.put(relatedEntityURI, keyedRelBuilder);
		}
        keyedRelBuilder.setKeyedRelationshipInfo((IKeyedRelationshipDefinition) ontology.getRelationshipDefinitionById(keyedRelationshipId));
	}



}