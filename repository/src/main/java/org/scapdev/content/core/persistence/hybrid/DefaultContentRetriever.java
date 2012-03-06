/*******************************************************************************
 * The MIT License
 * 
 * Copyright (c) 2011 davidwal
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

import gov.nist.scap.content.model.definitions.collection.IMetadataModel;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;

public class DefaultContentRetriever extends AbstractContentRetriever {
	private ContentStore contentStore;

	public DefaultContentRetriever(String contentId, IMetadataModel model, ContentStore contentStore) {
		super(contentId, model);
		this.contentStore = contentStore;
	}

	@Override
	protected XmlObject getContentInternal(String contentId, IMetadataModel model) {
		return contentStore.getContent(contentId);
	}

	@Override
	public XmlCursor getCursor() {
		throw new UnsupportedOperationException("implement");
	}
}
