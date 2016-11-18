/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rtbf.rest;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.handle.HandleManager;
import org.dspace.rtbf.rest.common.Constants;
import org.dspace.rtbf.rest.common.RTBObject;
import org.dspace.rtbf.rest.common.Episode;
import org.dspace.rtbf.rest.common.Sequence;
import org.dspace.rtbf.rest.common.Serie;
import org.dspace.rtbf.rest.search.SearchParameters;
import org.dspace.usage.UsageEvent;
import org.dspace.usage.UsageSearchEvent;
import org.dspace.utils.DSpace;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: peterdietz
 * Date: 10/7/13
 * Time: 1:54 PM
 * To change this template use File | Settings | File Templates.
 */
@Path("/handle")
public class HandleResource {
    private static Logger log = Logger.getLogger(HandleResource.class);
    private static org.dspace.core.Context context;

    @GET
    @Path("/{prefix}/{suffix}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public org.dspace.rtbf.rest.common.RTBObject getObject(@PathParam("prefix") String prefix, @PathParam("suffix") String suffix
    			, @QueryParam("expand") String expand
        		, @QueryParam("omitExpand") @DefaultValue("true") boolean omitExpand
        		, @Context UriInfo info
        		, @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor
                , @Context HttpHeaders headers, @Context HttpServletRequest request)
                throws WebApplicationException
    {

    	int viewType = Constants.MIN_VIEW;

    	if (!omitExpand) { viewType = Constants.EXPANDELEM_VIEW; }

    	try {
            if(context == null || !context.isValid() ) {
                context = new org.dspace.core.Context();
                context.getDBConnection();
            }

            org.dspace.content.DSpaceObject dso = HandleManager.resolveToObject(context, prefix + "/" + suffix);
            if(dso == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            log.info("DSO Lookup by handle: [" + prefix + "] / [" + suffix + "] got result of: " + dso.getTypeText() + "_" + dso.getID());
            
            // Lan 18.10.2016 : log to statistics index
            writeStats(dso, request, context);
            
            switch(dso.getType()) {
            case Constants.COMMUNITY:
            	return new Serie(viewType, (org.dspace.content.Community) dso, expand+","+Constants.SERIE_EXPAND_OPTIONS, context);
            case Constants.COLLECTION:
            	return new Episode(viewType, (org.dspace.content.Collection) dso, expand+","+Constants.EPISODE_EXPAND_OPTIONS, context);
            case Constants.ITEM:
        		return new Sequence(viewType, (org.dspace.content.Item) dso, expand+","+Constants.SEQUENCE_EXPAND_OPTIONS, context);
            default:
            	return new RTBObject(dso);
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/{prefix}/{suffix}/{owningHandle}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public org.dspace.rtbf.rest.common.RTBObject getObjectByOwningHandle(@PathParam("prefix") String prefix, @PathParam("suffix") String suffix
    			, @PathParam("owningHandle") String owningHandle
    			, @QueryParam("expand") String expand
        		, @QueryParam("omitExpand") @DefaultValue("true") boolean omitExpand
        		, @Context UriInfo info
        		, @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor
                , @Context HttpHeaders headers, @Context HttpServletRequest request)
                throws WebApplicationException
    {

    	int viewType = Constants.MIN_VIEW;

    	if (!omitExpand) { viewType = Constants.EXPANDELEM_VIEW; }

    	try {
            if(context == null || !context.isValid() ) {
                context = new org.dspace.core.Context();
                context.getDBConnection();
            }

            org.dspace.content.DSpaceObject dso = HandleManager.resolveToObject(context, prefix + "/" + suffix);
            if(dso == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            log.info("DSO Lookup by handle: [" + prefix + "] / [" + suffix + "] / [" + owningHandle + "] got result of: " + dso.getTypeText() + "-" + dso.getID());
            
            // Lan 18.10.2016 : log to statistics index
            writeStats(dso, request, context);
            
            org.dspace.content.DSpaceObject owning_dso = HandleManager.resolveToObject(context, prefix + "/" + owningHandle);

            switch(dso.getType()) {
            case Constants.COMMUNITY:
            	return new Serie(viewType, (org.dspace.content.Community) dso, expand+","+Constants.SERIE_EXPAND_OPTIONS, context);
            case Constants.COLLECTION:
            	return new Episode(viewType, (org.dspace.content.Collection) dso, expand+","+Constants.EPISODE_EXPAND_OPTIONS, context);
            case Constants.ITEM:
            	if (owning_dso == null) {
            		return new Sequence(viewType, (org.dspace.content.Item) dso, expand+","+Constants.SEQUENCE_EXPAND_OPTIONS, context);
            	}
            	return new Sequence(viewType, (org.dspace.content.Item) dso, expand+",linkedDocuments"+","+Constants.SEQUENCE_EXPAND_OPTIONS, context, owning_dso.getType()+"-"+owning_dso.getID());
            default:
            	return new RTBObject(dso);
            }
            
        } catch (SQLException e) {
            log.error(e.getMessage());
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    
    @GET
    @Path("/admin/clearCache")
    @Produces({MediaType.TEXT_PLAIN})
    public String clearCache(@Context HttpServletRequest request) throws WebApplicationException
    // TODO : add WithAuthorizea 27.04.2016
    {
    	String done = "Done";
    	
    	try {
    		if (!(context == null || !context.isValid())) {
    			context.complete();
    		}
    	} catch (SQLException e) {
            log.error(e.getMessage());
            throw new WebApplicationException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    	}

		return done;
    }
    
    
    
    // Not use
    public org.dspace.rtbf.rest.common.RTBObject getObjectWithAuthorize(@PathParam("prefix") String prefix, @PathParam("suffix") String suffix
    		, @QueryParam("expand") String expand
    		, @QueryParam("omitExpand") @DefaultValue("true") boolean omitExpand
    		, @Context UriInfo info
    		, @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor
    		, @Context HttpHeaders headers, @Context HttpServletRequest request)
    				throws WebApplicationException
    				{

    	int viewType = Constants.MIN_VIEW;

    	if (!omitExpand) { viewType = Constants.EXPANDELEM_VIEW; }

    	try {
    		if(context == null || !context.isValid() ) {
    			context = new org.dspace.core.Context();
    			context.getDBConnection();
    		}

    		org.dspace.content.DSpaceObject dso = HandleManager.resolveToObject(context, prefix + "/" + suffix);
    		if(dso == null) {
    			throw new WebApplicationException(Response.Status.NOT_FOUND);
    		}
    		log.info("DSO Lookup by handle: [" + prefix + "] / [" + suffix + "] got result of: " + dso.getTypeText() + "_" + dso.getID());

    		if(AuthorizeManager.authorizeActionBoolean(context, dso, org.dspace.core.Constants.READ)) {
    			switch(dso.getType()) {
    			case Constants.COMMUNITY:
    				return new Serie(viewType, (org.dspace.content.Community) dso, expand+","+Constants.SERIE_EXPAND_OPTIONS, context);
    			case Constants.COLLECTION:
    				return new Episode(viewType, (org.dspace.content.Collection) dso, expand+","+Constants.EPISODE_EXPAND_OPTIONS, context);
    			case Constants.ITEM:
            		return new Sequence(viewType, (org.dspace.content.Item) dso, expand+","+Constants.SEQUENCE_EXPAND_OPTIONS, context);
    			default:
    				return new RTBObject(dso);
    			}
    		} else {
    			throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    		}
    	} catch (SQLException e) {
    		log.error(e.getMessage());
    		throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    	}
    }
    
    private static final boolean writeStatistics;
    static
    {
        writeStatistics = ConfigurationManager.getBooleanProperty("rtbf-rest", "stats", false);
    }

    protected void writeStats(DSpaceObject dspaceObject,
    		HttpServletRequest request, org.dspace.core.Context context)
    {
        if (!writeStatistics)
        {
            return;
        }
        
        //Fire our event
        new DSpace().getEventService().fireEvent(new UsageEvent(UsageEvent.Action.VIEW, request, context, dspaceObject));

        log.debug("fired event");
    }
    
    

}