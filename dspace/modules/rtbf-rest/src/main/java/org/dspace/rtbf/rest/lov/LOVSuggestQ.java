package org.dspace.rtbf.rest.lov;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SpellCheckResponse.Collation;
import org.apache.solr.common.SolrDocument;
import org.dspace.rtbf.rest.common.Constants;
import org.dspace.rtbf.rest.common.SimpleNode;
import org.dspace.rtbf.rest.search.Request;
import org.dspace.rtbf.rest.search.Resource;
import org.dspace.sort.OrderFormat;
import org.dspace.suggest.SuggestSearch;

@Path("/")
public class LOVSuggestQ extends Resource {
	private static Logger log = Logger.getLogger(LOVSuggestQ.class);
	
    public static final String FACETFIELD = "query_q";
    public static final SimpleNode.Attribute ELEMENT = SimpleNode.Attribute.KEY;

    @GET
	@Path("suggestQ")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	public List<SimpleNode> getQueryQ(
    		@QueryParam("pt") @DefaultValue(Constants.LOV_ALL) String pt
    		, @Context UriInfo info, @Context HttpHeaders headers, @Context HttpServletRequest request)
	throws WebApplicationException
	{
		LOVParameters params = new LOVParameters(info.getQueryParameters());
		
		String partialTerms = pt.trim();
		if (partialTerms.isEmpty()) {
			return(new ArrayList<SimpleNode>());
		} else if (partialTerms.equals(Constants.LOV_ALL)) {
    		log.info("Reading q from statistics index.");
            return(getAllACNodes(FACETFIELD, ELEMENT, params, "/selectQ"));
		}
		
        log.info("Reading q from statistics index(pt=" + partialTerms + ").");		
        return(getACNodes(FACETFIELD, ELEMENT, partialTerms, params, "/selectQ"));
		
	}
	
	
    /**
     * Differ from the others LOV which use "search" index, this 
     * - do the search on "suggest" index,
     * - get result from doc, not from facets
     * - need to import statistics.SolrLogger.java, not discovery.SolrServiceImpl
     * 
     * @see org.dspace.rtbf.rest.search.Resource#getACNodes(java.lang.String, org.dspace.rtbf.rest.common.SimpleNode.Attribute, java.lang.String, org.dspace.rtbf.rest.search.Request, java.lang.String)
     */
    @Override
    public List<SimpleNode> getACNodes(
            String field, SimpleNode.Attribute attr
            , String pTerms
            , Request params
            , String handler
           ) throws WebApplicationException
    {
        List<SimpleNode> results = new ArrayList<SimpleNode>();
        
    	int limit = params.getLimit();
    	if (limit < 0) {limit = org.dspace.rtbf.rest.common.Constants.DEFAULT_LOV_RPP;}
    	if (org.dspace.rtbf.rest.common.Constants.LIMITMAX < limit ) {
    		limit = org.dspace.rtbf.rest.common.Constants.LIMITMAX;
    	}

    	// limit the search to partial terms
        String qterms = null;
        String partialTerms = (pTerms == null) ? null : pTerms.trim();
        String query = null;
        if (partialTerms != null && !partialTerms.isEmpty()) {
            // Remove diacritic + escape all but alphanum
            qterms = OrderFormat.makeSortString(partialTerms, null, OrderFormat.TEXT)
                        .replaceAll("([^\\p{Alnum}\\s])", "\\\\$1");

            query = qterms;
            log.debug("Suggest query terms.(qterms=" + qterms + ").");
        }
		
        QueryResponse solrQueryResponse = null;
        
        try {
        	solrQueryResponse = SuggestSearch.query(query, limit, "/selectQ");
		} catch (SolrServerException e) {
	        log.error(e.getMessage());
	        throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		}
        
        if (solrQueryResponse != null) {
        	if (solrQueryResponse.getResults() != null) {
        		for (SolrDocument doc : solrQueryResponse.getResults()) {
        			results.add(new SimpleNode().setAttribute(attr, (String) doc.getFieldValue(field)));
        		}
        	}

        	if (solrQueryResponse.getSpellCheckResponse() != null)
            {
                if (solrQueryResponse.getSpellCheckResponse().getCollatedResults() != null) {
	                for (Collation collation : solrQueryResponse.getSpellCheckResponse().getCollatedResults()) {
	        			results.add(new SimpleNode().setAttribute(SimpleNode.Attribute.COLLATION, collation.getCollationQueryString()));
					}
	
                }
            }

            return results;
        }
        return null;

    }
	
}
