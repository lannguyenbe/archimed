package org.dspace.rtbf.rest.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAnyElement;

/*
 * Use only by Jabx for xml output
 */
public class HighlightWrapper {

	public static class Entry {
		public String field;
		public List<String> highlights;
	}

    protected List<Entry> entries;
    
	public HighlightWrapper() {}
	
    public HighlightWrapper(Map<String,List<String>> v) {
    	List<Entry> adapte = new ArrayList<Entry>();
		for (java.util.Map.Entry<String, List<String>> mEntry : v.entrySet()) {
			Entry e = new Entry();
			e.field = mEntry.getKey();
			e.highlights = mEntry.getValue();
			adapte.add(e);
		}
		
		this.entries = adapte;
    }

    @XmlAnyElement
    public List<JAXBElement<String>> getHighlight() {
    	List<Entry> v = this.entries;
		List<JAXBElement<String>> elements = new ArrayList<JAXBElement<String>>();
		
		for (Entry entry : v) {
			if (entry.highlights == null) { continue; }
			for (String hl : entry.highlights) {
				elements.add(new JAXBElement<String>(
						new javax.xml.namespace.QName(entry.field)
						, String.class
						, hl
				));
			}
		}
        return elements;
    }
        	
}
