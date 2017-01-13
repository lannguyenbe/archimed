package org.dspace.discovery;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverResult.SearchDocument;
import org.dspace.utils.DSpace;

public class DiscoverSubItems {

    private static final Logger log = Logger.getLogger(DiscoverSubItems.class);
    private static final int LIMITMAX = 5000;
    
    
    protected Context context;
    protected DiscoverResult queryResults;
    protected DiscoverQuery queryArgs;
    protected DSpaceObject dso;
    
    public DiscoverSubItems(Context context, DSpaceObject dso){
        this.dso = dso;
        this.context = context;
    }
    
    
    public void performSearch() throws SearchServiceException, SQLException {

        DiscoverQuery query = new DiscoverQuery();

        query.addProperty("qt", "/selectSequence");
        query.addFilterQueries("owning_collection:"+ org.dspace.core.Constants.COLLECTION + "-" + dso.getID());
        query.setMaxResults(LIMITMAX);
        
        queryResults = SearchUtils.getSearchService().search(context, query);
    }

    /**
     * List of items of the owning_collection
     */
    public DiscoverResult getqueryResults() 
    {
    	try {
			performSearch();
		} catch (Exception e) {
            log.error("Error while searching for sub items", e);

            return null;
		}

        return queryResults;
    }
    
}
