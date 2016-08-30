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
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.dspace.rtbf.rest.common.Constants;
import org.dspace.rtbf.rest.common.SimpleNode;
import org.dspace.rtbf.rest.search.Resource;

@Path("{alternatePaths: serie_titles|ispartof_titles}")public class LOVSerieTitles extends Resource {
    private static Logger log = Logger.getLogger(LOVSerieTitles.class);
    
    // public static final String FACETFIELD = "ispartof_title";
    /*
     *  Lan 02.05.2015 : now is list of title of all communitites even if there is no child sequence
     *  title_keyword only exists for document of resource_type_id=4
     */
    public static final String FACETFIELD = "serie_title";
    public static final SimpleNode.Attribute ELEMENT = SimpleNode.Attribute.TITLE;

    
    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public List<SimpleNode> getNames(
    		@QueryParam("pt") @DefaultValue(Constants.LOV_ALL) String pt
    		, @Context UriInfo info, @Context HttpHeaders headers, @Context HttpServletRequest request)
    throws WebApplicationException
    {
    	LOVParameters params = new LOVParameters(info.getQueryParameters());

        String partialTerms = pt.trim();
        if (partialTerms.isEmpty()) {
        	return(new ArrayList<SimpleNode>());
        } else if (partialTerms.equals(Constants.LOV_ALL)) {
    		log.info("Reading all series titles.");
            return(getAllACNodes(FACETFIELD, ELEMENT, params));
        } 

        log.info("Reading series titles.(pt=" + partialTerms + ").");
        return(getACNodes(FACETFIELD, ELEMENT, partialTerms, params));
    }
    
}

