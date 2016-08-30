/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.core.Constants;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.kernel.ServiceManager;
import org.dspace.utils.DSpace;

import java.sql.SQLException;
import java.util.*;

/**
 * Util methods used by discovery
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class SearchUtils {
    /** Cached search service **/
    private static SearchService searchService;


    public static SearchService getSearchService()
    {
        if(searchService ==  null){
            DSpace dspace = new DSpace();
            org.dspace.kernel.ServiceManager manager = dspace.getServiceManager() ;
            searchService = manager.getServiceByName(SearchService.class.getName(),SearchService.class);
        }
        return searchService;
    }

    // Lan 02.05.2016 : use in builDocument by type (community, collection, item)
    public static DiscoveryConfiguration getDiscoveryConfiguration(int dsoType){
        DiscoveryConfigurationService configurationService = getConfigurationService();

        DiscoveryConfiguration result = null;
        
        result = configurationService.getMap().get(Constants.typeText[dsoType]);

        if(result == null){
            result = getDiscoveryConfiguration(null);
        }

        return result;
    }
    
    public static DiscoveryConfiguration getDiscoveryConfiguration(){
        return getDiscoveryConfiguration(null);
    }

    // Lan 02.05.2016 : use in builDocument for Item
    public static DiscoveryConfiguration getDiscoveryConfiguration(DSpaceObject dso){
        DiscoveryConfigurationService configurationService = getConfigurationService();

        DiscoveryConfiguration result = null;
        if(dso == null){
            result = configurationService.getMap().get("site");
        }else{
            result = configurationService.getMap().get(dso.getHandle());
        }

        // Lan 28.04.2016
        if(result == null){
            //Get config of resource_type
            result = configurationService.getMap().get(dso.getTypeText());
        }

        if(result == null){
            //No specific configuration, get the default one
            result = configurationService.getMap().get("default");
        }

        return result;
    }
    
    public static DiscoveryConfigurationService getConfigurationService() {
        DSpace dspace  = new DSpace();
        ServiceManager manager = dspace.getServiceManager();
        return manager.getServiceByName(DiscoveryConfigurationService.class.getName(), DiscoveryConfigurationService.class);
    }

    public static List<String> getIgnoredMetadataFields(int type)
    {
        return getConfigurationService().getToIgnoreMetadataFields().get(type);
    }

    public static List<String> getCatchAllMetadataFields(int type)
    {
        return getConfigurationService().getToCatchAllMetadataFields().get(type);
    }
    /**
     * Method that retrieves a list of all the configuration objects from the given item
     * A configuration object can be returned for each parent community/collection
     * @param item the DSpace item
     * @return a list of configuration objects
     * 
     * 02.05.2016 Lan : there was a bug in getId (which returns always null), this function used to return only 1 configuration, the "site" one;
     * by now it returns many configurations, at least 2 : the "default" and the "site" configurations,
     * this causes failure when builDocument for Item : for instance due to multiple add values to a single value field identifier_origin
     */
    public static List<DiscoveryConfiguration> getAllDiscoveryConfigurations(Item item) throws SQLException {
        Map<String, DiscoveryConfiguration> result = new HashMap<String, DiscoveryConfiguration>();
        
	        Collection[] collections = item.getCollections();
	        for (Collection collection : collections) {
	            DiscoveryConfiguration configuration = getDiscoveryConfiguration(collection);
	            if(!result.containsKey(configuration.getId())){
	                result.put(configuration.getId(), configuration);
	            }
	        }
	
	        //Also add one for the default
	        DiscoveryConfiguration configuration = getDiscoveryConfiguration(null);
	        if(!result.containsKey(configuration.getId())){
	            result.put(configuration.getId(), configuration);
	        }
	
	        return Arrays.asList(result.values().toArray(new DiscoveryConfiguration[result.size()]));
    }
        

    public static List<DiscoveryConfiguration> getAllDiscoveryConfigurations() {
        Map<String, DiscoveryConfiguration> result = new HashMap<String, DiscoveryConfiguration>();
        DiscoveryConfigurationService configurationService = getConfigurationService();
        DiscoveryConfiguration configuration = null;

        // Get Site config
        configuration = configurationService.getMap().get("site");
        
        if (configuration != null) { result.put(configuration.getId(), configuration); }
        
        //Get Default config
        configuration = configurationService.getMap().get("default");
        if (configuration != null) { result.put(configuration.getId(), configuration); }

        //Get ITEM config
        configuration = configurationService.getMap().get(Constants.typeText[org.dspace.core.Constants.ITEM]);
        if (configuration != null) { result.put(configuration.getId(), configuration); }

        //Get COLLECTION config
        configuration = configurationService.getMap().get(Constants.typeText[org.dspace.core.Constants.COLLECTION]);
        if (configuration != null) { result.put(configuration.getId(), configuration); }
        
        //Get COMMUNITY config
        configuration = configurationService.getMap().get(Constants.typeText[org.dspace.core.Constants.COMMUNITY]);
        if (configuration != null) { result.put(configuration.getId(), configuration); }
        
        return Arrays.asList(result.values().toArray(new DiscoveryConfiguration[result.size()]));
    }
    


    public static List<DiscoveryConfiguration> getDiscoveryConfigurations(DSpaceObject dso) throws SQLException {
        Map<String, DiscoveryConfiguration> result = new HashMap<String, DiscoveryConfiguration>();
        
        DiscoveryConfiguration configuration = getDiscoveryConfiguration(dso);
        if(!result.containsKey(configuration.getId())){
            result.put(configuration.getId(), configuration);
        }

        // Also add one for the site
        configuration = getDiscoveryConfiguration(null);
        if(!result.containsKey(configuration.getId())){
            result.put(configuration.getId(), configuration);
        }

        return Arrays.asList(result.values().toArray(new DiscoveryConfiguration[result.size()]));
    }

}
