/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.discovery;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.DiscoverResult.SearchDocument;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

/**
 * Displays related items to the currently viewable item
 *
 * @author Lan
 */
public class ExpandedItems
{
    private static final Logger log = Logger.getLogger(ExpandedItems.class);
    
    protected Context context;
    protected String contextPath;    
    protected DiscoverResult queryResults;
    protected DiscoverQuery queryArgs;
    protected DSpaceObject dso;


    public ExpandedItems(Context context, String contextPath, DSpaceObject dso){
        this.dso = dso;
        this.context = context;
        this.contextPath = contextPath;
    }
    
    protected SearchService getSearchService()
    {
        DSpace dspace = new DSpace();
        
        org.dspace.kernel.ServiceManager manager = dspace.getServiceManager() ;

        return manager.getServiceByName(SearchService.class.getName(),SearchService.class);
    }
    
    // TODO : externalize searchFields in config
    private static String[] searchFields = {"dc.title", "identifier_attributor", "dcterms.isPartOf.title"};
    public void performSearch() throws SearchServiceException, UIException, SQLException {

        DiscoverQuery query = new DiscoverQuery();
        String handle = dso.getHandle();
        
        for (String sf : searchFields) {
        	query.addSearchField(sf);			
		}
        // Join search
        query.addFilterQueries("{!join from=identifier_origin to=identifier_origin}handle:"+handle);
        
        queryResults =  getSearchService().search(context, query);
    }

    /**
     * Display items related to the given item
     */
    public void addBody(Body body) throws SAXException, WingException,
            SQLException, IOException, AuthorizeException
    {

        try {
            // Questionne solr for expanded results
            performSearch(); 
        }catch (Exception e){
            log.error("Error while searching for expanded items", e);

            return;
        }
        
        if (queryResults != null && 0 < queryResults.getDspaceObjects().size()) {
            // normally exactly 1 result that represents the most representativ of the items having the same indentifier_origin
            // the return result may have a different handle from the one we are questionning 
            DSpaceObject resultDso = queryResults.getDspaceObjects().get(0);
            String handle = dso.getHandle(); // questionning handle
            org.dspace.app.xmlui.wing.element.List expandList = null;

            if (!handle.equals(resultDso.getHandle())) {
                // take the first doc, should be the only one
                DiscoverResult.SearchDocument doc = queryResults.getSearchDocument(resultDso).get(0);
                expandList = body.addDivision("item-expanded").addList("item-expanded");
                expandList.setHead("Linked items"); // TODO n18i                
                addExpandDoc(expandList, doc);                
            }
            
            List<DiscoverResult.SearchDocument> expandDocuments = queryResults.getExpandDocuments(resultDso);
            if (expandDocuments != null && expandDocuments.size() > 0) {
                for (SearchDocument docE : expandDocuments) {
                    String handleE = docE.getSearchFieldValues("handle").get(0);
                    if (!handle.equals(handleE)) {
                    	if (expandList == null) {
                            expandList = body.addDivision("item-expanded").addList("item-expanded");
                    	}
                    	addExpandDoc(expandList, docE);
                    }
                }
            }
        }

    }
    
    private void addExpandDoc(org.dspace.app.xmlui.wing.element.List list, SearchDocument doc) throws WingException {

    	org.dspace.app.xmlui.wing.element.List expandDocList = list.addList("item-expanded-doc");
    	
    	String handle = doc.getSearchFieldValues("handle").get(0);
        String title = doc.getSearchFieldValues("dc.title").get(0);
        String link = contextPath + "/handle/" + handle;
        expandDocList.addItem().addXref(link).addContent(title);
        
        String collection = doc.getSearchFieldValues("dcterms.isPartOf.title").get(0);
        expandDocList.addItem(collection);                
        
        String source = doc.getSearchFieldValues("identifier_attributor").get(0);
        expandDocList.addItem(source);                
    }
    
    public static Message message(String key)
    {
        return new Message("default", key);
    }

    /**
     * List of items related to the given item
     */
    public List<DiscoverResult.SearchDocument> getDocuments() 
    {

        List<DiscoverResult.SearchDocument> expandList = new ArrayList<DiscoverResult.SearchDocument>();

        try {
            // Questionne solr for expanded results
            performSearch(); 
        }catch (Exception e){
            log.error("Error while searching for expanded items", e);

            return expandList;
        }
                
        if (queryResults != null && 0 < queryResults.getDspaceObjects().size()) {
            // normally exactly 1 result that represents the most representativ of the items having the same indentifier_origin
            // the return result may have a different handle from the one we are questionning 
            DSpaceObject resultDso = queryResults.getDspaceObjects().get(0);
            String handle = dso.getHandle(); // questionning handle

            if (!handle.equals(resultDso.getHandle())) { // result handle and questionning handle are different
                // take the first doc, should be the only one
                DiscoverResult.SearchDocument doc = queryResults.getSearchDocument(resultDso).get(0);
                expandList.add(doc);
            }
            
            List<DiscoverResult.SearchDocument> expandDocuments = queryResults.getExpandDocuments(resultDso);
            for (SearchDocument docE : expandDocuments) {
            	String handleE = docE.getSearchFieldValues("handle").get(0);
            	if (!handle.equals(handleE)) {
            		expandList.add(docE);
            	}
            }
        }
        
        return expandList;
    }
    
}
