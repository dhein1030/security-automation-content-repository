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

import org.scapdev.content.core.persistence.semantic.TripleStoreMetadataStoreFactory;
import org.scapdev.content.util.ServiceLoaderUtil;



public class DefaultHybridContentPersistenceManager extends AbstractHybridContentPersistenceManager {
	public static final String METADATA_STORE_FACTORY = "repository.content.hybrid.metadata-manager-factory";
	public static final String CONTENT_STORE_FACTORY = "repository.content.hybrid.content-manager-factory";

	public DefaultHybridContentPersistenceManager() {
		super(getMetadataStore(), getContentStore());
	}

	protected DefaultHybridContentPersistenceManager(MetadataStore metadataStore, ContentStore contentStore) {
		super(metadataStore, contentStore);
	}

	private static MetadataStore getMetadataStore() {
		MetadataStoreFactory factory = ServiceLoaderUtil.load(MetadataStoreFactory.class, METADATA_STORE_FACTORY, TripleStoreMetadataStoreFactory.class, DefaultHybridContentPersistenceManager.class.getClassLoader());
		return factory.newMetadataStore();
	}

	private static ContentStore getContentStore() {
		ContentStoreFactory factory = ServiceLoaderUtil.load(ContentStoreFactory.class, CONTENT_STORE_FACTORY, MemoryResidentContentStoreFactory.class, DefaultHybridContentPersistenceManager.class.getClassLoader());
		return factory.newContentStore();
	}
}
