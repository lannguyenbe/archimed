package org.dspace.rtbf.rest.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class RsConfigurationManager {
	
	private static RsConfigurationManager instance = new RsConfigurationManager();
	
	private Map<String, Properties> attributeProps = new HashMap<String, Properties>();
	
	private RsConfigurationManager() {}
	
	public static RsConfigurationManager getInstance() {
		return instance;
	}
	
	public void setAttribute(String attribute, Properties props) {
		attributeProps.put(attribute, props);
	}
	
	public Properties getAttribute(String attribute) {
		return attributeProps.get(attribute);		
	}
	
}
