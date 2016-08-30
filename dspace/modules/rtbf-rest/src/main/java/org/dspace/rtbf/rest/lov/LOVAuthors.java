package org.dspace.rtbf.rest.lov;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.dspace.rtbf.rest.common.SimpleNode;
import org.dspace.rtbf.rest.common.Constants;
import org.dspace.rtbf.rest.search.Resource;
import org.dspace.rtbf.rest.search.SearchParameters;

/**
 * Root resource (exposed at "authors" path)
 */
@Path("/")
public class LOVAuthors extends Resource {
    private static Logger log = Logger.getLogger(LOVAuthors.class);
    
    public static final String FACETFIELD = "author";
    public static final SimpleNode.Attribute ELEMENT = SimpleNode.Attribute.NAME;

    
    @GET
    @Path("{alternatePaths: authors|contributors/names}")
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
    		log.info("Reading all contributor name.");
            return(getAllACNodes(FACETFIELD, ELEMENT, params));
        } 
        
        log.info("Reading contributor name.(pt=" + partialTerms + ").");
        return(getACNodes(FACETFIELD, ELEMENT, partialTerms, params));
    }
    
    @GET
    @Path("contributors/roles")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public List<SimpleNode> getRoles(
            @QueryParam("pt") @DefaultValue(Constants.LOV_ALL) String pt
    		, @Context UriInfo info, @Context HttpHeaders headers, @Context HttpServletRequest request)
    throws WebApplicationException
    {
    	LOVParameters params = new LOVParameters(info.getQueryParameters());

    	String partialTerms = pt.trim();
        if (partialTerms.isEmpty()) {
        	return(new ArrayList<SimpleNode>());
        } else { // results are always the same list; arg pt= is ignored
    		log.info("Reading all contributor roles.");
            return(getAllACNodes("role", SimpleNode.Attribute.KEY, params));
        }
    }
    
    @GET
    @Path("contributors/{name}/roles")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public List<SimpleNode> getRolesOfName(@PathParam("name") String name
    		, @Context UriInfo info, @Context HttpHeaders headers, @Context HttpServletRequest request)
    throws WebApplicationException
    {        
    	LOVParameters params = new LOVParameters(info.getQueryParameters());

    	String partialTerms = name + " /"; // get name followed by /<role>
		log.info("Reading roles of.(pt=" + name + ").");
        return(getACNodes("contributor", SimpleNode.Attribute.NAME, partialTerms, params));
    }
}
