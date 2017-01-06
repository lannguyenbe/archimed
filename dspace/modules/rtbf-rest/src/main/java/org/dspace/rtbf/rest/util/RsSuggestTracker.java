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
import org.dspace.rtbf.suggest.SuggestHarvester;

public class RsSuggestTracker implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		SuggestHarvester.stopScheduler();
		
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		
		if (ConfigurationManager.getBooleanProperty(Constants.WEBAPP_NAME, "suggestTracker.autostart")) {
			SuggestHarvester.startNewScheduler();
		}
		
	}

}
