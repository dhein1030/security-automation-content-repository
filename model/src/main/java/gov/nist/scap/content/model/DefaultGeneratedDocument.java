package gov.nist.scap.content.model;


import gov.nist.scap.content.model.definitions.IGeneratedDocumentDefinition;

public class DefaultGeneratedDocument extends AbstractEntity<IGeneratedDocumentDefinition> implements IMutableGeneratedDocument {

	public DefaultGeneratedDocument(IGeneratedDocumentDefinition definition, IContentHandle contentHandle, IMutableEntity<?> parent) throws ContentException {
		super(definition, contentHandle, parent);
	}

	public IKey getKey() {
		return null;
	}

	public void accept(IContainerVisitor visitor) {
		visitor.visit(this);
	}
}