/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rtbf.rest.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.log4j.Logger;
import org.dspace.rtbf.rest.util.RsConfigurationManager;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author peterdietz, Rostislav Novak (Computing and Information Centre, CTU in
 *         Prague)
 * 
 */
@XmlJavaTypeAdapter(MetadataEntryAdapter.class)
@XmlRootElement(name = "metadataentry")
public class MetadataEntry
{
    private static Logger log = Logger.getLogger(MetadataEntry.class);

    String key;

    String value;

    String language;

    public MetadataEntry()
    {
    }

    public MetadataEntry(String key, String value, String language)
    {
        this.key = key;
        this.value = value;
        this.language = language;
    }

    @JsonValue
    public String getValue() // Jackson only retains this as value for the whole object
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String language)
    {
        this.language = language;
    }
    
	public static String getPreferredLabel(String key) {
		Properties mapper = (Properties) RsConfigurationManager.getInstance().getAttribute(Constants.NAMINGMETA);
		if (mapper != null) {
	    	String label = mapper.getProperty(key);
	    	if (label != null) {
	    		return label;
	    	}
		}
    	return key;
	}

	public static String getSortLabel(String key) {
		Properties mapper = (Properties) RsConfigurationManager.getInstance().getAttribute(Constants.SORTMETA);
		if (mapper != null) {
	    	String label = mapper.getProperty(key);
	    	if (label != null) {
	    		return label;
	    	}
		}
    	return key;
	}

	public static Map<String,Object> listAsMap(List<MetadataEntry> entries) {
    	if (entries == null || entries.isEmpty()) {
    		return null;
    	}
    	
		Map<String,Object> elements = new HashMap<String,Object>();
		for (MetadataEntry entry : entries) {
			String k = getPreferredLabel(entry.getKey());
			if (k.isEmpty()) { continue; }
			List<String> lov;
			if ((lov = (List<String>) elements.get(k)) == null) { // new key
				lov = new ArrayList<String>();
			}
			lov.add(entry.getValue());
				
			elements.put(k, lov);
		}
		
		return elements;
    	
    }

}
