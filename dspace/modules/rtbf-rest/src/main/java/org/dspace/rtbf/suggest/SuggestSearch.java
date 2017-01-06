/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rtbf.suggest;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.*;
import org.dspace.core.ConfigurationManager;

import java.io.*;
import java.util.*;

/*
 * Much inspired by Dspace SolrLogger.java
 * Lan 06.01.2017
 */
public class SuggestSearch
{
    private static final Logger log = Logger.getLogger(SuggestSearch.class);
	
    private static final HttpSolrServer solr;
    
    private static final String LAST_INDEXED_FIELD = "SolrIndexer.lastIndexed";
    private static final String ADMIN_RECORD = "@1@";

    public static final String DATE_FORMAT_8601 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static final String DATE_FORMAT_DCDATE = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    public static enum SuggestType {
   		ADMIN ("admin"),
   		SEARCH ("search");

   		private final String text;

        SuggestType(String text) {
   	        this.text = text;
   	    }
   	    public String toString()   { return text; }
   	}


    static
    {   	
        HttpSolrServer server = null;
        
        if (ConfigurationManager.getProperty("solr-suggest", "server") != null)
        {
            try
            {
                server = new HttpSolrServer(ConfigurationManager.getProperty("solr-suggest", "server"));
                SolrQuery solrQuery = new SolrQuery()
                        .setQuery(ADMIN_RECORD);
                server.query(solrQuery);

            } catch (Exception e) {
            	log.error(e.getMessage(), e);
            }
        }
        solr = server;
    }


    
    
    public static class GetProcessor
    {

        public void execute(String query) throws SolrServerException, IOException {
        	
        	SolrQuery solrQuery = new SolrQuery();
        	solrQuery.setRequestHandler("/get");
        	solrQuery.set("id", query);
        	
        	QueryResponse queryResponse;
        	try {
            	queryResponse = solr.query(solrQuery);
            } catch (SolrServerException e) {
            	log.error("Error using query " + solrQuery, e);
            	throw e;
            }
        	
            // process the only document if any
        	if (queryResponse.getResponse().size() > 0) {
        		SolrDocument solrDoc = (SolrDocument) queryResponse.getResponse().getVal(0);
        		if (solrDoc != null) {
        			process(solrDoc);
        		}
        	}

        }

        public void commit() throws IOException, SolrServerException {
            solr.commit();
        }

        /**
         * Override to manage the document
         * @param doc
         */
        public void process(SolrDocument doc) throws IOException, SolrServerException {

        }
    }

    public static void merge(String q, long count) throws SolrServerException, IOException {
    	
        final List<SolrDocument> docsToMerge = new ArrayList<SolrDocument>();
        SolrInputDocument inputDoc;
    	
    	GetProcessor processor = new GetProcessor(){
            public void process(SolrDocument doc) throws IOException, SolrServerException {
                docsToMerge.add(doc);
            }
        };

        processor.execute(q); // get doc with that q as id
        
        if (docsToMerge.size() > 0) { 
        	// doc already exists do merge
        	inputDoc = ClientUtils.toSolrInputDocument(docsToMerge.get(0));
        } else { 
        	// create new doc
        	inputDoc = new SolrInputDocument();
        	
        	inputDoc.setField("type", SuggestType.SEARCH.toString());
        	inputDoc.setField("q", q);
        }

        inputDoc.setField("query_freq", count);
        inputDoc.setField(LAST_INDEXED_FIELD, DateFormatUtils.format(new Date(), DATE_FORMAT_8601));
        solr.add(inputDoc);    	
    }


    public static void query(String query, int max) throws SolrServerException
    {
        query(query, null, null,0, max, null, null, null, null, null, false, null);
    }

    public static QueryResponse query(String query, int max, String requestHandler) throws SolrServerException
    {
        return query(query, null, null, max , max, null, null, null, null, null, false, requestHandler);
    }




    public static Map<String, Integer> queryFacetQuery(String query,
            String filterQuery, List<String> facetQueries)
            throws SolrServerException
    {
        QueryResponse response = query(query, filterQuery, null,0, 1, null, null,
                null, facetQueries, null, false);
        return response.getFacetQuery();
    }


    public static String getLastSynced() throws SolrServerException {

    	if (solr == null)
        {
            return null;
        }

        SolrQuery solrQuery = new SolrQuery().setRows(1).setQuery("q:"+ADMIN_RECORD);
               
        QueryResponse queryResponse;
        try {
        	queryResponse = solr.query(solrQuery);
        } catch (SolrServerException e) {
        	log.error("Error using query " + solrQuery, e);
        	throw e;
        }
        if (queryResponse.getResults() != null && queryResponse.getResults().getNumFound() > 0 ) {
        	SolrDocument doc = queryResponse.getResults().get(0);
        	String lastSynced = (doc.getFieldValue("lastSynced") == null) ? null : DateFormatUtils.formatUTC((Date) doc.getFieldValue("lastSynced"),"yyyy-MM-dd'T'HH:mm:ss'Z'");
        	return lastSynced;
        }

        return null;
    	
    }

    public static void updLastSynced(String lastSynced) throws SolrServerException, IOException {
    	

    	if (solr == null)
        {
            return;
        }

    	SolrInputDocument inputDoc;
    	SolrQuery solrQuery = new SolrQuery().setRequestHandler("/get");
    	solrQuery.set("id", ADMIN_RECORD);
    	
    	QueryResponse queryResponse = solr.query(solrQuery);
   	
        // process the only document if any
    	if (queryResponse.getResponse().size() > 0) {
    		SolrDocument solrDocument = (SolrDocument) queryResponse.getResponse().getVal(0);
    		if (solrDocument != null) {
                inputDoc = ClientUtils.toSolrInputDocument(solrDocument);
    		} else { 
            	// create new doc
            	inputDoc = new SolrInputDocument();
            	
            	inputDoc.setField("type", SuggestType.ADMIN.toString());
            	inputDoc.setField("q", ADMIN_RECORD);
            }    	
    		inputDoc.setField("lastSynced", lastSynced);
            inputDoc.setField(LAST_INDEXED_FIELD, DateFormatUtils.format(new Date(), DATE_FORMAT_8601));
    		solr.add(inputDoc);
                	
    	}
        
    }
    
    public static QueryResponse query(String query, String filterQuery,
            String facetField, int rows, int max, String dateType, String dateStart,
            String dateEnd, List<String> facetQueries, String sort, boolean ascending)
            throws SolrServerException
    {
    	return query(query, filterQuery,
                facetField, rows, max, dateType, dateStart,
                dateEnd, facetQueries, sort, ascending, null);
    }


    // Lan 12.12.2016 : set request handler
    public static QueryResponse query(String query, String filterQuery,
            String facetField, int rows, int max, String dateType, String dateStart,
            String dateEnd, List<String> facetQueries, String sort, boolean ascending
            , String handler)
            throws SolrServerException
    {
        if (solr == null)
        {
            return null;
        }

        SolrQuery solrQuery = new SolrQuery().setRows(rows).setQuery(query)
                .setFacetMinCount(1);
        
        // Lan 12.12.2016 : set request handler
        if (handler != null) {
        	solrQuery.setRequestHandler(handler);
        }
        
        // Lan 21.12.2016 : set spellcheck
        solrQuery.setParam(SpellingParams.SPELLCHECK_Q, query);
        solrQuery.setParam(SpellingParams.SPELLCHECK_COLLATE, Boolean.TRUE);
        solrQuery.setParam("spellcheck", Boolean.TRUE);
        

        // Set the date facet if present
        if (dateType != null)
        {
            solrQuery.setParam("facet.date", "time")
                    .
                    // EXAMPLE: NOW/MONTH+1MONTH
                    setParam("facet.date.end",
                            "NOW/" + dateType + dateEnd + dateType).setParam(
                            "facet.date.gap", "+1" + dateType)
                    .
                    // EXAMPLE: NOW/MONTH-" + nbMonths + "MONTHS
                    setParam("facet.date.start",
                            "NOW/" + dateType + dateStart + dateType + "S")
                    .setFacet(true);
        }
        if (facetQueries != null)
        {
            for (int i = 0; i < facetQueries.size(); i++)
            {
                String facetQuery = facetQueries.get(i);
                solrQuery.addFacetQuery(facetQuery);
            }
            if (0 < facetQueries.size())
            {
                solrQuery.setFacet(true);
            }
        }

        if (facetField != null)
        {
            solrQuery.addFacetField(facetField);
        }

        // Set the top x of if present
        if (max != -1)
        {
            solrQuery.setFacetLimit(max);
        }


        if(sort != null){
            solrQuery.setSortField(sort, (ascending ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc));
        }


        if (filterQuery != null)
        {
            solrQuery.addFilterQuery(filterQuery);
            
        }

        QueryResponse response;
        try
        {
            // solr.set
            response = solr.query(solrQuery);
        }
        catch (SolrServerException e)
        {
            System.err.println("Error using query " + query);
            throw e;
        }
        return response;
    }

}
