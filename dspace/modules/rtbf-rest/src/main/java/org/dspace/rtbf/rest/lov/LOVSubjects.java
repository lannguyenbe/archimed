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

@Path("/")
public class LOVSubjects extends Resource {
    private static Logger log = Logger.getLogger(LOVSubjects.class);
    
    public static final String FACETFIELD = "subject";
    public static final SimpleNode.Attribute ELEMENT = SimpleNode.Attribute.KEY;

    
    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @GET
    @Path("{alternatePaths: subjects|matters}")
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
    		log.info("Reading all subjects");
            return(getAllACNodes(FACETFIELD, ELEMENT, params));
        } 
        
        log.info("Reading subjects.(pt=" + partialTerms + ").");
        return(getACNodes(FACETFIELD, ELEMENT, partialTerms, params));
    }
    
}
