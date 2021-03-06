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
package org.scapdev.content.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.scapdev.content.model.jaxb.ExternalIdentifierType;
import org.scapdev.content.model.jaxb.MetaModel;
import org.scapdev.content.model.jaxb.SchemaType;
import org.scapdev.jaxb.reflection.JAXBContextFactory;
import org.scapdev.jaxb.reflection.model.JAXBClass;
import org.scapdev.jaxb.reflection.model.JAXBModel;

public class JAXBMetadataModel implements MetadataModel {
	private final JAXBModel model;
	private final JAXBContext context;

	/**
	 * key; SchemaInfo.id, value: SchemaInfo
	 */
	private final Map<String, SchemaInfo> schemaMap;
	private final Map<JAXBClass, EntityInfo> entityMap;
	private final Map<JAXBClass, DocumentInfo> documentMap;
	private final Map<JAXBClass, RelationshipInfo> relationshipMap;
	private final Map<String, EntityInfo> keyIdToEntityMap;
	private final Map<String, RelationshipInfo> keyRefIdToRelationshipMap;
	private final Map<String, RelationshipInfo> relationshipIdToRelationshipMap;
	private final Map<String, EntityInfo> entityIdToEntityMap;
	private final Map<String, String> namespaceToPrefixMap;
	private final Map<String, ExternalIdentifierInfo> externalIdentifierIdToExternalIdentifierMap;
	private final List<EntityIdentifierMapping> entityIdentifierMappings;
	private final Map<String, IndirectRelationshipInfo> indirectRelationshipIdToInfoMap;
	private final Map<String, KeyedRelationshipInfo> keyedRelationshipIdToInfoMap;
	private final Map<String, Set<DocumentInfo>> entityIdToDocumentInfoMap;
	private final Map<String, StaticDocumentInfo> entityIdToStaticDocumentInfoMap;

	JAXBMetadataModel() throws IOException, JAXBException, ClassNotFoundException {
		// Initialize JAXB reflection model
		ClassLoader loader = MetaModel.class.getClassLoader();
//		ClassLoader loader = this.getClass().getClassLoader();
		context = JAXBContextFactory.getJAXBContext(loader, Collections.singleton(MetaModel.class.getPackage().getName()));
		model = JAXBModel.newInstanceFromPackageNames(JAXBContextFactory.getPackagesForContext(context), loader);

		// Identify objects of interest
		InitializingJAXBClassVisitor init = new InitializingJAXBClassVisitor(model);
		model.visit(init);

		schemaMap = new HashMap<String, SchemaInfo>();
		entityMap = new HashMap<JAXBClass, EntityInfo>();
		documentMap = new HashMap<JAXBClass, DocumentInfo>();
		relationshipMap = new HashMap<JAXBClass, RelationshipInfo>();
		keyIdToEntityMap = new HashMap<String, EntityInfo>();
		entityIdToEntityMap = new HashMap<String, EntityInfo>();
		keyRefIdToRelationshipMap = new HashMap<String, RelationshipInfo>();
		relationshipIdToRelationshipMap = new HashMap<String, RelationshipInfo>();
		namespaceToPrefixMap = new HashMap<String, String>();
		externalIdentifierIdToExternalIdentifierMap = new HashMap<String, ExternalIdentifierInfo>();
		entityIdentifierMappings = new LinkedList<EntityIdentifierMapping>();
		indirectRelationshipIdToInfoMap = new HashMap<String, IndirectRelationshipInfo>();
		keyedRelationshipIdToInfoMap = new HashMap<String, KeyedRelationshipInfo>();
		entityIdToDocumentInfoMap = new HashMap<String, Set<DocumentInfo>>();
		entityIdToStaticDocumentInfoMap = new HashMap<String, StaticDocumentInfo>();

		// Load metadata and associate with JAXB info
		loadMetadata(init);
	}

	@Override
	public Set<String> getEntityInfoIds() {
		return entityIdToEntityMap.keySet();
	}

	public Set<String> getRelationshipInfoIds() {
		return relationshipIdToRelationshipMap.keySet();
	}

	public Set<String> getExternalIdentifierInfoIds() {
		return externalIdentifierIdToExternalIdentifierMap.keySet();
	}

	private void loadMetadata(InitializingJAXBClassVisitor init) throws IOException, JAXBException {
		Unmarshaller unmarshaller = context.createUnmarshaller();

		InputStream is = this.getClass().getResourceAsStream("/META-INF/metamodels/manifest");
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		String file;
		while ((file = r.readLine()) != null) {
			String resource = "/META-INF/metamodels/"+file;

			MetaModel model = (MetaModel) unmarshaller.unmarshal(this.getClass().getResourceAsStream(resource));
			processModel(model, init);
		}

		// Get maps of individual relationship types
		for (Map.Entry<String, RelationshipInfo> entry : relationshipIdToRelationshipMap.entrySet()){
			RelationshipInfo value = entry.getValue();
			if (value instanceof IndirectRelationshipInfo){
				indirectRelationshipIdToInfoMap.put(entry.getKey(), (IndirectRelationshipInfo) entry.getValue());
			} else if (value instanceof KeyedRelationshipInfo) {
				keyedRelationshipIdToInfoMap.put(entry.getKey(), (KeyedRelationshipInfo) entry.getValue());
			}
		}

		for (EntityInfo entityInfo : entityMap.values()) {
			EntityIdentifierMapping mapping = entityInfo.getEntityIdentifierMapping();
			if (mapping != null) {
				entityIdentifierMappings.add(mapping);
			}
		}

		for (DocumentInfo documentInfo : documentMap.values()) {
			for (EntityInfo entityInfo : documentInfo.getSupportedEntityInfos()) {
				String entityId = entityInfo.getId();
				Set<DocumentInfo> documentInfos = entityIdToDocumentInfoMap.get(entityId);
				if (documentInfos == null) {
					documentInfos = new HashSet<DocumentInfo>();
					entityIdToDocumentInfoMap.put(entityId, documentInfos);
				}
				documentInfos.add(documentInfo);
			}
		}
	}

	private void processModel(MetaModel metaModel, InitializingJAXBClassVisitor init) {
		for (ExternalIdentifierType externalIdentifierType : metaModel.getExternalIdentifiers().getExternalIdentifier()) {
			ExternalIdentifierInfo externalIdentifier = new ExternalIdentifierInfoImpl(externalIdentifierType);
			externalIdentifierIdToExternalIdentifierMap.put(externalIdentifier.getId(), externalIdentifier);
		}

		for (SchemaType schemaType : metaModel.getSchemas().getSchema()) {
			SchemaInfo schema = new SchemaInfoImpl(schemaType, this, init);
			schemaMap.put(schema.getId(), schema);

			namespaceToPrefixMap.put(schema.getNamespace(), schema.getPrefix());
		}
	}

	void registerEntity(EntityInfoImpl entity) {
		entityMap.put(entity.getBinding().getJaxbClass(), entity);
		keyIdToEntityMap.put(entity.getKeyInfo().getId(), entity);
		entityIdToEntityMap.put(entity.getId(), entity);
	}

	void registerRelationship(JAXBClass typeInfo, KeyedRelationshipInfo relationship) {
		relationshipMap.put(typeInfo, relationship);
		relationshipIdToRelationshipMap.put(relationship.getId(), relationship);
		KeyRefInfo keyRefInfo = relationship.getKeyRefInfo();
		keyRefIdToRelationshipMap.put(keyRefInfo.getId(), relationship);
	}

	void registerRelationship(JAXBClass typeInfo, IndirectRelationshipInfo relationship) {
		relationshipMap.put(typeInfo, relationship);
		relationshipIdToRelationshipMap.put(relationship.getId(), relationship);
	}

	public void registerDocument(AbstractDocumentInfo<?> document) {
		documentMap.put(document.getBinding().getJaxbClass(), document);

		if (StaticDocumentInfo.class.isAssignableFrom(document.getClass())) {
			StaticDocumentInfo staticDocumentInfo = (StaticDocumentInfo)document;
			entityIdToStaticDocumentInfoMap.put(staticDocumentInfo.getEntityInfo().getId(), staticDocumentInfo);
		}
	}

	public JAXBContext getJAXBContext() {
		return context;
	}

	public JAXBModel getModel() {
		return model;
	}

	public EntityInfo getEntityInfoByKeyId(String keyId) {
		return keyIdToEntityMap.get(keyId);
	}

	public RelationshipInfo getRelationshipInfoByKeyRefId(String keyRefId) {
		return keyRefIdToRelationshipMap.get(keyRefId);
	}

	@Override
	public RelationshipInfo getRelationshipInfoById(String id) {
		return relationshipIdToRelationshipMap.get(id);
	}

	public EntityInfo getEntityInfoById(String id) {
		return entityIdToEntityMap.get(id);
	}

	@Override
	public Map<String, String> getNamespaceToPrefixMap() {
		return namespaceToPrefixMap;
	}

	@Override
	public ExternalIdentifierInfo getExternalIdentifierInfoById(String id) {
		return externalIdentifierIdToExternalIdentifierMap.get(id);
	}

	@Override
	public Key getKeyFromMappedIdentifier(String identifier) {
		Key result = null;
		for (EntityIdentifierMapping mapping : entityIdentifierMappings) {
			result = mapping.getKeyForIdentifier(identifier);
			if (result != null) break;
		}
		return result;
	}

	public ExternalIdentifier getExternalIdentifierById(String identifier) {
		ExternalIdentifier result = null;
		top: for (ExternalIdentifierInfo info : externalIdentifierIdToExternalIdentifierMap.values()) {
			for (Pattern pattern : info.getPattern()) {
				if (pattern.matcher(identifier).matches()) {
					result = new ExternalIdentifier(info, identifier);
					break top;
				}
			}
		}
		return result;
	}

	@Override
	public Set<String> getIndirectRelationshipIds() {
		return indirectRelationshipIdToInfoMap.keySet();
	}

	@Override
	public Set<String> getKeyedRelationshipIds() {
		return keyedRelationshipIdToInfoMap.keySet();
	}

	@Override
	public Set<DocumentInfo> getDocumentInfosContaining(EntityInfo info) {
		return entityIdToDocumentInfoMap.get(info.getId());
	}

	@Override
	public StaticDocumentInfo getStaticDocumentInfoByEntityId(String id) {
		return entityIdToStaticDocumentInfoMap.get(id);
	}
}
