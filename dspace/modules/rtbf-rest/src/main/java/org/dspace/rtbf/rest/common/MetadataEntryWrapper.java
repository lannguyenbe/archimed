package org.dspace.rtbf.rest.common;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAnyElement;

public class MetadataEntryWrapper {
	
	private MetadataEntry metadataEntry;
	private JAXBElement<String> element;
	
	public MetadataEntryWrapper() {}
	
	public MetadataEntryWrapper(MetadataEntry entry) {
		this.metadataEntry = entry;
	}

	@XmlAnyElement
	public JAXBElement<String> getElement() {
		element = new JAXBElement<String>(
				new javax.xml.namespace.QName(metadataEntry.getKey())
//				new javax.xml.namespace.QName(getPreferredLabel(entry.getKey()))
				, String.class
				, metadataEntry.getValue()
		);
		return element;
	}
}
