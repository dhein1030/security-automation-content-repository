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
package org.scapdev.content.core.resolver;

import java.util.Collections;
import java.util.Set;

import org.scapdev.content.model.Key;

public class UnresolvableKeysException extends ResolverException {

	/** the serial version UID */
	private static final long serialVersionUID = 1L;
	private Set<Key> unresolvableKeys;

	public UnresolvableKeysException() {
	}

	public UnresolvableKeysException(String message) {
		super(message);
	}

	public UnresolvableKeysException(Throwable cause) {
		super(cause);
	}

	public UnresolvableKeysException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @return the unresolvableKeys
	 */
	public Set<Key> getUnresolvableKeys() {
		return unresolvableKeys;
	}

	/**
	 * @param unresolvableKeys the unresolvableKeys to set
	 */
	public void setUnresolvableKeys(Set<Key> unresolvableKeys) {
		this.unresolvableKeys = Collections.unmodifiableSet(unresolvableKeys);
	}

}
