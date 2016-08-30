package org.dspace.rtbf.rest.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryHitHighlightFieldConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilter;

public class RsDiscoveryConfiguration {
	
	private static RsDiscoveryConfiguration instance = new RsDiscoveryConfiguration();
	
	private List<org.dspace.discovery.configuration.DiscoveryConfiguration> configs;
	private List<org.dspace.discovery.configuration.DiscoveryHitHighlightFieldConfiguration> highlightFields = new ArrayList<DiscoveryHitHighlightFieldConfiguration>();
	private Map<String, org.dspace.discovery.configuration.DiscoverySearchFilter> searchFilters = new HashMap<String, DiscoverySearchFilter>();
	
	public static RsDiscoveryConfiguration getInstance() {
		return instance;
	}
	
	public void setConfiguration(List<DiscoveryConfiguration> configs) {
		this.configs = configs;

		for (DiscoveryConfiguration config : configs) {	
			if (config.getId().equals("homepageConfiguration") && config.getHitHighlightingConfiguration() != null) {
				if (config.getHitHighlightingConfiguration() != null) {
					this.highlightFields = config.getHitHighlightingConfiguration().getMetadataFields();
				}
			}
			if (config.getSearchFilters() != null) {
				for (DiscoverySearchFilter sf : config.getSearchFilters()) {
					String indexField = sf.getIndexFieldName();
					this.searchFilters.put(indexField, sf);
				}
			}
		}
	}
	
	private List<DiscoveryConfiguration> getConfigs() {
		return this.configs;
	}
	
	public static List<DiscoveryHitHighlightFieldConfiguration> getHighlightFieldConfigurations() {
		return(instance.highlightFields);
	}

	public static Map<String, DiscoverySearchFilter> getSearchFilters() {
		return(instance.searchFilters);
	}

}
