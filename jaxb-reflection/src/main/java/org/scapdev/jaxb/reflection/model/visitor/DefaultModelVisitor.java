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
package org.scapdev.jaxb.reflection.model.visitor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

import org.apache.log4j.Logger;
import org.scapdev.jaxb.reflection.model.JAXBClass;
import org.scapdev.jaxb.reflection.model.JAXBModel;
import org.scapdev.jaxb.reflection.model.JAXBProperty;
import org.scapdev.jaxb.reflection.model.instance.DefaultInstanceVisitor;

public class DefaultModelVisitor implements ModelVisitor {

	private static final Logger log = Logger.getLogger(DefaultInstanceVisitor.class);

	private final JAXBModel model;

	public DefaultModelVisitor(JAXBModel model) {
		this.model = model;
	}

	protected JAXBModel getModel() {
		return model;
	}

	public void visit(JAXBClass jaxbClass) {
		log.debug("visiting type: "+jaxbClass.getType().getName());
		processNode(jaxbClass);
	}

	public void visit(JAXBProperty property) {
		log.debug("visiting property: "+property.getName());
		processJaxbProperty(property);
	}

	public boolean beforeNode(JAXBClass jaxbClass) { return true; }
	public void afterNode(JAXBClass jaxbClass) { }

	public boolean beforeJAXBClass(JAXBClass jaxbClass) { return true; }
	public void afterJAXBClass(JAXBClass jaxbClass) { }

	public boolean beforeJAXBProperty(JAXBProperty property) { return true; }
	public void afterJAXBProperty(JAXBProperty property) {}

	protected void processNode(JAXBClass jaxbClass) {
		log.trace("visiting JAXBClass: "+jaxbClass.getType().getName());
		if (beforeNode(jaxbClass)) {
			processJaxbClass(jaxbClass);
		}
		afterNode(jaxbClass);
	}

	protected void processJaxbClass(JAXBClass jaxbClass) {
		if (beforeJAXBClass(jaxbClass)) {
	
			JAXBClass parent = jaxbClass.getSuperclass();
	
			// Handle parent first
			if (parent != null) processJaxbClass(parent);
	
			// Iterate over each property, starting with attributes
			for (JAXBProperty property : jaxbClass.getAttributeProperties().values()) {
				log.trace("Walking attribute property: "+property.getName());
				processJaxbProperty(property);
			}
			for (JAXBProperty property : jaxbClass.getElementProperties().values()) {
				log.trace("Walking element property: "+property.getName());
				processJaxbProperty(property);
			}
		}
		afterJAXBClass(jaxbClass);
	}

	protected void processJaxbProperty(JAXBProperty property) {
		if (beforeJAXBProperty(property)) {
			processPropertyValue(property);
		}
		afterJAXBProperty(property);
	}

	private void processPropertyValue(JAXBProperty property) {
		if (property.isList()) {
			// TODO: resolve generic
			processPropertyValueType(property, property.getActualType());
		} else {
			processPropertyValueType(property, property.getActualType());
		}
	}

	private void processPropertyValueType(JAXBProperty property, Class<?> clazz) {
		if (property.isAttribute()) {
			processPropertyValueInternal(property, clazz);
		} else if (property.isElement()) {
			XmlElements elements = property.getAnnotation(XmlElements.class);
			if (elements != null) {
				for (XmlElement element : elements.value()) {
					JAXBProperty subProperty = property.newSubstitutionJAXBProperty(element);
					processPropertyValueInternal(subProperty, subProperty.getActualType());
				}
			} else {
				processPropertyValueInternal(property, clazz);
			}
		}
	}

	private void processPropertyValueInternal(JAXBProperty property, Class<?> actualType) {
		JAXBClass jaxbClass = model.getClass(actualType);
		if (jaxbClass != null) {
			processNode(jaxbClass);
		}
	}
}
