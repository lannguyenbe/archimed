package org.dspace.rtbf.rest;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dspace.discovery.DiscoverResult;
import org.dspace.rtbf.rest.search.EpisodesSearchResponse;
import org.dspace.rtbf.rest.search.Request;
import org.dspace.rtbf.rest.search.Resource;
import org.dspace.rtbf.rest.search.SearchParameters;
import org.dspace.rtbf.rest.search.SearchResponse;
import org.dspace.rtbf.rest.search.SequencesSearchResponse;
import org.dspace.rtbf.rest.search.SeriesSearchResponse;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

@Path("/")
public class SearchResource extends Resource {
	
	private static final Logger log = Logger.getLogger(SearchResource.class);
	
    @POST
    @Path("/searchp/sequences")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public SearchResponse getItemsSearchResponseByPost(SearchParameters params
    		, @Context UriInfo info
    		, @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor
            , @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {
    	// Default args
    	Map<String, String> defaults = new HashMap<String, String>() {
    		{
    			put("collapse", "true");
    		}
    	};
    	
    	MultivaluedMap<String, String> uriParameters = info.getQueryParameters(); // uriParameters may be empty but is not null
    	    	
    	if (params == null) { params = new SearchParameters(); }
		params.supersedeBy(uriParameters, defaults);
		
    	return getItemsSearchResponse(params);
    	

    }

    @GET
    @Path("/search/sequences")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public SearchResponse getItemsSearchResponseByGet(
    		@QueryParam("scope") String scope
    		, @QueryParam("q") String qterms
    		, @QueryParam("limit") Integer limit, @QueryParam("offset") Integer offset
    		, @QueryParam("sort_by") String orderBy, @QueryParam("order") String order
    		, @QueryParam("facet") Boolean isFacet
    		, @QueryParam("facet_limit") Integer facetLimit, @QueryParam("facet_offset") Integer facetOffset
    		, @QueryParam("highlight") Boolean isHighlight 
    		, @QueryParam("snippet") Boolean isSnippet
    		, @QueryParam("expand") String expand
    		, @Context UriInfo info
    		, @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor
            , @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {
    	// Default args
    	Map<String, String> defaults = new HashMap<String, String>() {
    		{
    			put("collapse", "true");
    		}
    	};

    	MultivaluedMap<String, String> uriParameters = info.getQueryParameters(); // uriParameters may be empty but is not null
    	    	    	
    	SearchParameters params = new SearchParameters().supersedeBy(uriParameters, defaults);
		
    	return getItemsSearchResponse(params);
    }
			
    @POST
    @Path("/searchp/episodes")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public SearchResponse getCollectionsSearchResponseByPost(SearchParameters params
    		, @Context UriInfo info
    		, @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor
            , @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {
    	MultivaluedMap<String, String> uriParameters = info.getQueryParameters(); // uriParameters may be empty but is not null
    	
    	if (params == null) { params = new SearchParameters(); }
		params.supersedeBy(uriParameters);
		
    	return getCollectionsSearchResponse(params);
    }

    @GET
    @Path("/search/episodes")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public SearchResponse getCollectionsSearchResponseByGet(
    		@QueryParam("scope") String scope
    		, @QueryParam("q") String qterms
    		, @QueryParam("limit") Integer limit, @QueryParam("offset") Integer offset
    		, @QueryParam("sort_by") String orderBy, @QueryParam("order") String order
    		, @QueryParam("expand") String expand
    		, @Context UriInfo info
    		, @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor
            , @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {
    	MultivaluedMap<String, String> uriParameters = info.getQueryParameters(); // uriParameters may be empty but is not null
    	
    	SearchParameters params = new SearchParameters().supersedeBy(uriParameters);
		
    	return getCollectionsSearchResponse(params);
    }

    @POST
    @Path("/searchp/series")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public SearchResponse getSeriesSearchResponseByGet(SearchParameters params
    		, @Context UriInfo info
    		, @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor
            , @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {
    	MultivaluedMap<String, String> uriParameters = info.getQueryParameters(); // uriParameters may be empty but is not null
    	
    	if (params == null) { params = new SearchParameters(); }
		params.supersedeBy(uriParameters);
		
    	return getSeriesSearchResponse(params);
    }
    
    @GET
    @Path("/search/series")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public SearchResponse getSeriesSearchResponseByGet(
    		@QueryParam("scope") String scope
    		, @QueryParam("q") String qterms
    		, @QueryParam("limit") Integer limit, @QueryParam("offset") Integer offset
    		, @QueryParam("sort_by") String orderBy, @QueryParam("order") String order
    		, @QueryParam("expand") String expand
    		, @Context UriInfo info
    		, @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor
            , @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {
    	MultivaluedMap<String, String> uriParameters = info.getQueryParameters(); // uriParameters may be empty but is not null
    	
    	SearchParameters params = new SearchParameters().supersedeBy(uriParameters);
		
    	return getSeriesSearchResponse(params);
    }
    
    @POST
    @Path("/groupp/episodes")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public SearchResponse getCollectionsGroupByPost(SearchParameters params
    		, @Context UriInfo info
    		, @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor
            , @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {
    	MultivaluedMap<String, String> uriParameters = info.getQueryParameters(); // uriParameters may be empty but is not null
    	
    	if (params == null) { params = new SearchParameters(); }
		params.supersedeBy(uriParameters);
		
    	return getCollectionsGroupResponse(params);
    }

    @GET
    @Path("/group/episodes")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public SearchResponse getCollectionsGroupByGet(
    		@QueryParam("scope") String scope
    		, @QueryParam("q") String qterms
    		, @QueryParam("limit") Integer limit, @QueryParam("offset") Integer offset
    		, @QueryParam("sort_by") String orderBy, @QueryParam("order") String order
    		, @QueryParam("expand") String expand
    		, @Context UriInfo info
    		, @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor
            , @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {
    	MultivaluedMap<String, String> uriParameters = info.getQueryParameters(); // uriParameters may be empty but is not null
    	
    	SearchParameters params = new SearchParameters().supersedeBy(uriParameters);
		
    	return getCollectionsGroupResponse(params);
    }


    @GET
    @Path("/group/series")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public SearchResponse getSeriesGroupByGet(
    		@QueryParam("scope") String scope
    		, @QueryParam("q") String qterms
    		, @QueryParam("limit") Integer limit, @QueryParam("offset") Integer offset
    		, @QueryParam("sort_by") String orderBy, @QueryParam("order") String order
    		, @QueryParam("expand") String expand
    		, @Context UriInfo info
    		, @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor
            , @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {
    	MultivaluedMap<String, String> uriParameters = info.getQueryParameters(); // uriParameters may be empty but is not null
    	
    	SearchParameters params = new SearchParameters().supersedeBy(uriParameters);
		
    	return getSeriesGroupResponse(params);
    }

    protected SearchResponse getItemsSearchResponse(SearchParameters params) {

    	Request searchRequest = (Request) params;

        String qterms = searchRequest.getQuery();
    	Boolean isFacet = searchRequest.isFacet();
    	Integer limit = searchRequest.getLimit();
    	Integer offset = searchRequest.getOffset();

    	String expand = params.getExpand();

    	org.dspace.core.Context context = null;
        log.info("Searching sequences(q=" + qterms + ").");
        SearchResponse response = null;
        DiscoverResult queryResults = null;

        try {        	
            context = new org.dspace.core.Context();
            context.getDBConnection();
                        
			// expand the results if there is a query
            if (qterms != null && qterms.length() > 0) {
            	expand += ",results";
            	if (isFacet) { expand += ",facets"; }
            }
            queryResults = getQueryResult(org.dspace.core.Constants.ITEM, context, searchRequest);
            response = new SequencesSearchResponse(queryResults, expand, context, limit, offset);

            context.complete();

        } catch (Exception e) {
           processException("Could not process search sequences. Message:"+e.getMessage(), context);
        } finally {
           processFinally(context);            
        }

        return response;    	
    }
    
			
    protected SearchResponse getCollectionsSearchResponse(SearchParameters params)
    {
        Request searchRequest = (Request) params;
        
        String qterms = searchRequest.getQuery();
    	Boolean isFacet = searchRequest.isFacet();
    	Integer limit = searchRequest.getLimit();
    	Integer offset = searchRequest.getOffset();

    	String expand = params.getExpand();

        org.dspace.core.Context context = null;
        log.info("Searching episodes(q=" + qterms + ").");
        SearchResponse response = null;
        DiscoverResult queryResults = null;

        try {
            context = new org.dspace.core.Context();
            context.getDBConnection();
            
            // expand the results if there is a query
            if (qterms != null && qterms.length() > 0) {
            	expand += ",results";
            	if (isFacet) { expand += ",facets"; }
            }
            queryResults = getQueryResult(org.dspace.core.Constants.COLLECTION, context, searchRequest);
            response = new EpisodesSearchResponse(queryResults, expand, context, limit, offset);
            
            context.complete();

        } catch (Exception e) {
           processException("Could not process search episodes. Message:"+e.getMessage(), context);
        } finally {
           processFinally(context);            
        }

        return response;
    }


    protected SearchResponse getCollectionsGroupResponse(SearchParameters params)
    {
        Request searchRequest = (Request) params;
        
        String qterms = searchRequest.getQuery();
    	Boolean isFacet = searchRequest.isFacet();
    	Integer limit = searchRequest.getLimit();
    	Integer offset = searchRequest.getOffset();

    	String expand = params.getExpand();

        org.dspace.core.Context context = null;
        log.info("Searching episodes groups(q=" + qterms + ").");
        SearchResponse response = null;
        DiscoverResult queryResults = null;

        try {
            context = new org.dspace.core.Context();
            context.getDBConnection();
            
            // expand the results if there is a query
            if (qterms != null && qterms.length() > 0) {
            	expand += ",results";
            	if (isFacet) { expand += ",facets"; }
            }
            queryResults = getGroupResult(org.dspace.core.Constants.COLLECTION, context, searchRequest);
            response = new EpisodesSearchResponse(queryResults, expand, context, limit, offset);
            
            context.complete();

        } catch (Exception e) {
           processException("Could not process search episodes groups. Message:"+e.getMessage(), context);
        } finally {
           processFinally(context);            
        }

        return response;
    }


    protected SearchResponse getSeriesSearchResponse(SearchParameters params)
    {
        Request searchRequest = (Request) params;
        
        String qterms = searchRequest.getQuery();
    	Boolean isFacet = searchRequest.isFacet();
    	Integer limit = searchRequest.getLimit();
    	Integer offset = searchRequest.getOffset();

    	String expand = params.getExpand();

    	org.dspace.core.Context context = null;
        log.info("Searching series(q=" + qterms + ").");
        SearchResponse response = null;
        DiscoverResult queryResults = null;

        try {
            context = new org.dspace.core.Context();
            context.getDBConnection();
            
            // expand the results if there is a query
            if (qterms != null && qterms.length() > 0) {
            	expand += ",results";
            	if (isFacet) { expand += ",facets"; }
            }
	        queryResults = getQueryResult(org.dspace.core.Constants.COMMUNITY, context, searchRequest);
	        response = new SeriesSearchResponse(queryResults, expand, context, limit, offset);

            context.complete();

        } catch (Exception e) {
           processException("Could not process search series. Message:"+e.getMessage(), context);
        } finally {
           processFinally(context);            
        }

        return response;
    }

    protected SearchResponse getSeriesGroupResponse(SearchParameters params)
    {
        Request searchRequest = (Request) params;
        
        String qterms = searchRequest.getQuery();
    	Boolean isFacet = searchRequest.isFacet();
    	Integer limit = searchRequest.getLimit();
    	Integer offset = searchRequest.getOffset();

    	String expand = params.getExpand();

        org.dspace.core.Context context = null;
        log.info("Searching series groups(q=" + qterms + ").");
        SearchResponse response = null;
        DiscoverResult queryResults = null;

        try {
            context = new org.dspace.core.Context();
            context.getDBConnection();
            
            // expand the results if there is a query
            if (qterms != null && qterms.length() > 0) {
            	expand += ",results";
            	if (isFacet) { expand += ",facets"; }
            }
            queryResults = getGroupResult(org.dspace.core.Constants.COMMUNITY, context, searchRequest);
	        response = new SeriesSearchResponse(queryResults, expand, context, limit, offset);
            
            context.complete();

        } catch (Exception e) {
            processException("Could not process search series groups. Message:"+e.getMessage(), context);
        } finally {
           processFinally(context);            
        }

        return response;
    }


}
