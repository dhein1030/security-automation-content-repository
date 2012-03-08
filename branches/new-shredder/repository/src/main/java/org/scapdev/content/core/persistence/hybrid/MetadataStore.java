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
package org.scapdev.content.core.persistence.hybrid;

import gov.nist.scap.content.model.IEntity;
import gov.nist.scap.content.model.IKey;
import gov.nist.scap.content.model.definitions.collection.IMetadataModel;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface MetadataStore {

	/**
	 * 
	 * @param key
	 * @param contentRetrieverFactory
	 * @param model
	 * @return the entity if the key exists or <code>null</code> if it was not found
	 */
	IEntity<?> getEntity(IKey key, ContentRetrieverFactory contentRetrieverFactory, IMetadataModel model);
	/**
	 * 
	 * @param indirectType
	 * @param indirectIds
	 * @param entityTypes filter results to only these entity types or no filtering if empty
	 * @return
	 */
	Set<IKey> getKeysForIndirectIds(String indirectType, Collection<String> indirectIds, Set<String> entityTypes);
	void persist(Map<String, IEntity<?>> contentIdToEntityMap, IMetadataModel model);
//	Map<String, ? extends EntityStatistic> getEntityStatistics(Set<String> entityInfoIds, IMetadataModel model);
	void shutdown();
}