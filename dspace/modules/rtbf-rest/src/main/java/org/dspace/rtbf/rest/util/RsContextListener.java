package org.dspace.rtbf.rest.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.dspace.core.ConfigurationManager;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryHitHighlightFieldConfiguration;
import org.dspace.rtbf.rest.common.Constants;

public class RsContextListener implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		// noop
		
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
    	int idx;
		String definition;    	
        RsConfigurationManager configManager = RsConfigurationManager.getInstance();

        // For sortable fields, build map between frontend name and solr index name
        idx = 1;
	    Properties sortableEntries = new Properties();
	    while ((definition = ConfigurationManager.getProperty(Constants.WEBAPP_NAME, Constants.SORTMETA+".field." + idx)) != null) {
	        List<String> fields = new ArrayList<String>();
	        fields = Arrays.asList(definition.split(":", 2));
            sortableEntries.put(fields.get(0), fields.get(1));
	    	
	    	idx++;
	    }
        configManager.setAttribute(Constants.SORTMETA, sortableEntries);

    	// For frontend names, build map between frontend name and canonical dspace field name schema.element.qualifier
        idx = 1;
	    Properties namingEntries = new Properties();
	    while ((definition = ConfigurationManager.getProperty(Constants.WEBAPP_NAME, Constants.NAMINGMETA+".field." + idx)) != null) {
	        List<String> fields = new ArrayList<String>();
	        fields = Arrays.asList(definition.split(":", 2));
            namingEntries.put(fields.get(0), fields.get(1));
	    	
	    	idx++;
	    }
        configManager.setAttribute(Constants.NAMINGMETA, namingEntries);

    	// For frontend names, build map between frontend name and searchFilter
        idx = 1;
	    Properties filterEntries = new Properties();
	    while ((definition = ConfigurationManager.getProperty(Constants.WEBAPP_NAME, Constants.FILTERMETA+".field." + idx)) != null) {
	        List<String> fields = new ArrayList<String>();
	        fields = Arrays.asList(definition.split(":", 2));
            filterEntries.put(fields.get(0), fields.get(1));
	    	
	    	idx++;
	    }
        configManager.setAttribute(Constants.FILTERMETA, filterEntries);

        ServletContext sc = event.getServletContext();
        sc.setAttribute(Constants.WEBAPP_NAME, configManager); // keep ref to avoid garbage collector

        // Get List of highlight fields from discovery.xml
        // Get List of searchFilter fields from discovery.xml
        RsDiscoveryConfiguration discoveryConfig = RsDiscoveryConfiguration.getInstance();
        discoveryConfig.setConfiguration(SearchUtils.getAllDiscoveryConfigurations());
		
	}

}
