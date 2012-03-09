package gov.nist.scap.content.model.definitions;

import gov.nist.scap.content.model.IContainer;
import gov.nist.scap.content.model.KeyException;

import org.apache.xmlbeans.XmlBeans;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;

public class XPathKeyedField extends AbstractKeyedField {
	private final XPathRetriever retriever;

	public XPathKeyedField(String name, String xpath) throws XmlException {
		super(name);
		this.retriever = new XPathRetriever(XmlBeans.compilePath(xpath));
	}

	@Override
	protected String retrieveValue(IContainer<?> parentContext, XmlCursor cursor) throws KeyException {
		String retval = retriever.getValue(cursor);

		if (retval == null) {
			throw new KeyException("unable to retrieve key field value: "+getName());
		}
		return retval;
	}
}
