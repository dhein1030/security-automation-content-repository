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
package org.scapdev.content.core.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.scapdev.content.core.ContentException;
import org.scapdev.content.core.query.EntityStatistic;
import org.scapdev.content.model.Entity;
import org.scapdev.content.model.Key;
import org.scapdev.content.model.MetadataModel;


public interface ContentPersistenceManager {
	Entity getEntityByKey(Key key, MetadataModel model);
	/**
	 * 
	 * @param indirectType
	 * @param indirectIds
	 * @param entityType the entity types to filter the results based on or empty if all results should be provided
	 * @return
	 */
	Set<Key> getKeysForIndirectIds(String indirectType, Collection<String> indirectIds, Set<String> entityType);
	void storeEntities(List<? extends Entity> entities, MetadataModel model) throws ContentException;
	Map<String, ? extends EntityStatistic> getEntityStatistics(Set<String> entityInfoIds, MetadataModel model);
	void shutdown();
}
