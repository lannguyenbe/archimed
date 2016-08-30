/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rtbf.rest;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
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
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.CollectionIterator;
import org.dspace.content.CommunityIterator;
import org.dspace.rtbf.rest.common.Constants;
import org.dspace.rtbf.rest.common.Episode;
import org.dspace.rtbf.rest.common.MetadataEntry;
import org.dspace.rtbf.rest.common.Serie;
import org.dspace.rtbf.rest.search.Resource;

/**
 * Class which provides CRUD methods over communities.
 * 
 * @author Rostislav Novak (Computing and Information Centre, CTU in Prague)
 * 
 */
@Path("/series")
public class SeriesResource extends Resource
{
    private static Logger log = Logger.getLogger(SeriesResource.class);

    /**
     * It returns an array of series
     * 
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Serie[] getSeries(@QueryParam("expand") String expand, @QueryParam("limit") @DefaultValue(Constants.DEFAULT_LIMIT) Integer limit,
            @QueryParam("offset") @DefaultValue("0") Integer offset, @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {
        org.dspace.core.Context context = null;
    	int viewType = Constants.MIN_VIEW;

        log.info("Reading communities.(offset=" + offset + ",limit=" + limit + ").");
        List<Serie> series = null;

        try
        {
            context = new org.dspace.core.Context();
            context.getDBConnection();
            
            if (!((limit != null) && (limit >= 0) && (offset != null) && (offset >= 0)))
            {
                log.warn("Pagging was badly set, using default values.");
                limit = 100;
                offset = 0;
            }

            CommunityIterator dspaceCommunities = org.dspace.content.CommunityAdd.findAllCursor(context);
            series = new ArrayList<Serie>();

            while(dspaceCommunities.hasNext()) {
                org.dspace.content.Community community = dspaceCommunities.next();
                	series.add(new Serie(viewType, community, null, context));
            }

            context.complete();
        }
        catch (SQLException e)
        {
            processException("Something went wrong while reading communitites from database. Message: " + e, context);
        }
        finally
        {
            processFinally(context);
        }

        log.trace("Items were successfully read.");
        return series.toArray(new Serie[0]);
    }

    /**
     * Returns community with basic properties
     * 
     */
    @GET
    @Path("/{community_id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Serie getSerie(@PathParam("community_id") Integer communityId, @QueryParam("expand") String expand,
    		@QueryParam("omitExpand") @DefaultValue("true") boolean omitExpand,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {
        org.dspace.core.Context context = null;
    	int viewType = Constants.MIN_VIEW;

    	if (!omitExpand) { viewType = Constants.EXPANDELEM_VIEW; }
    	
    	log.info("Reading community(id=" + communityId + ").");
        Serie serie = null;

        try
        {
            context = new org.dspace.core.Context();
            context.getDBConnection();
            
            org.dspace.content.Community dspaceCommunity = findCommunity(context, communityId, org.dspace.core.Constants.READ);

            serie = new Serie(viewType, dspaceCommunity, expand+","+Constants.SERIE_EXPAND_OPTIONS, context);
            context.complete();

        }
        catch (SQLException e)
        {
            processException("Could not read community(id=" + communityId + "), SQLException. Message:" + e, context);
        }
        finally
        {
            processFinally(context);
        }


        log.trace("Community(id=" + communityId + ") was successfully read.");
        return serie;
    }


    /**
     * Return all top communities in DSpace. Top communities are communities on
     * the root of tree.
     * 
     */
    @GET
    @Path("/top-series")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Serie[] getTopSeries(@QueryParam("expand") String expand,
            @QueryParam("limit") @DefaultValue("20") Integer limit, @QueryParam("offset") @DefaultValue("0") Integer offset,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {
        org.dspace.core.Context context = null;
    	int viewType = Constants.MIN_VIEW;

        log.info("Reading all top communities.(offset=" + offset + " ,limit=" + limit + ").");
        List<Serie> series = null;

        try
        {
            context = new org.dspace.core.Context();
            context.getDBConnection();
            
            org.dspace.content.Community[] dspaceCommunities = org.dspace.content.Community.findAllTop(context);
            series = new ArrayList<Serie>();

            if (!((limit != null) && (limit >= 0) && (offset != null) && (offset >= 0)))
            {
                log.warn("Pagging was badly set, using default values.");
                limit = 100;
                offset = 0;
            }

            for (int i = offset; (i < (offset + limit)) && i < dspaceCommunities.length; i++)
            {
                if (AuthorizeManager.authorizeActionBoolean(context, dspaceCommunities[i], org.dspace.core.Constants.READ))
                {
                    Serie serie = new Serie(viewType, dspaceCommunities[i], expand, context);
                    series.add(serie);
                }
            }

            context.complete();
        }
        catch (SQLException e)
        {
            processException("Could not read top communities, SQLException. Message:" + e, context);
        }
        finally
        {
            processFinally(context);
        }

        log.trace("All top communities successfully read.");
        return series.toArray(new Serie[0]);
    }

    /**
     * Return all collections of community.
     * 
     */
    @GET
    @Path("/{community_id}/episodes")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Episode[] getSerieEpisodes(@PathParam("community_id") Integer communityId,
            @QueryParam("expand") String expand, @QueryParam("limit") @DefaultValue(Constants.DEFAULT_LIMIT) Integer limit,
            @QueryParam("offset") @DefaultValue("0") Integer offset, @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {
        org.dspace.core.Context context = null;
    	int viewType = Constants.MIN_VIEW;

        log.info("Reading community(id=" + communityId + ") collections.");
        List<Episode> episodes = null;

        try
        {
            context = new org.dspace.core.Context();
            context.getDBConnection();
            
            org.dspace.content.Community dspaceCommunity = findCommunity(context, communityId, org.dspace.core.Constants.READ);            
            
            episodes = new ArrayList<Episode>();
            CollectionIterator childCollections = dspaceCommunity.getCollections(limit, offset);
            while(childCollections.hasNext()) {
                org.dspace.content.Collection collection = childCollections.next();
                	episodes.add(new Episode(viewType, collection, null, context));
            }                    	

            context.complete();
        }
        catch (SQLException e)
        {
            processException("Could not read community(id=" + communityId + ") collections, SQLException. Message:" + e, context);
        }
        finally
        {
            processFinally(context);
        }

        log.trace("Community(id=" + communityId + ") collections were successfully read.");
        return episodes.toArray(new Episode[0]);
    }

    /**
     * Return all subcommunities of community.
     * 
     */
    @GET
    @Path("/{community_id}/series")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Serie[] getSerieSeries(@PathParam("community_id") Integer communityId,
            @QueryParam("expand") String expand, @QueryParam("limit") @DefaultValue("20") Integer limit,
            @QueryParam("offset") @DefaultValue("0") Integer offset, @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {
        org.dspace.core.Context context = null;
    	int viewType = Constants.MIN_VIEW;

        log.info("Reading community(id=" + communityId + ") subcommunities.");
        List<Serie> series = null;

        try
        {
            context = new org.dspace.core.Context();
            context.getDBConnection();
            
            org.dspace.content.Community dspaceCommunity = findCommunity(context, communityId, org.dspace.core.Constants.READ);

            series = new ArrayList<Serie>();
            CommunityIterator childCommunities = dspaceCommunity.getSubCommunities(limit, offset);
            while(childCommunities.hasNext()) {
                org.dspace.content.Community community = childCommunities.next();
                	series.add(new Serie(viewType, community, null, context));
            }                    	

            context.complete();
        }
        catch (SQLException e)
        {
            processException("Could not read community(id=" + communityId + ") subcommunities, SQLException. Message:" + e,
                    context);
        }
        finally
        {
            processFinally(context);
        }

        log.trace("Community(id=" + communityId + ") subcommunities were successfully read.");
        return series.toArray(new Serie[0]);
    }


    /**
     * Returns serie metadata 
     * 
     */
    @GET
    @Path("/{community_id}/metadata")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public MetadataEntry[] getSerieMetadata(@PathParam("community_id") Integer communityId, @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {
        org.dspace.core.Context context = null;
    	int viewType = Constants.MIN_VIEW;
    	
    	log.info("Reading community(id=" + communityId + ") metadata.");
        List<MetadataEntry> metadata = null;

        try
        {
            context = new org.dspace.core.Context();
            context.getDBConnection();

            org.dspace.content.Community dspaceCommunity = findCommunity(context, communityId, org.dspace.core.Constants.READ);

            metadata = new org.dspace.rtbf.rest.common.Serie(viewType, dspaceCommunity, "metadata", context).getMetadataEntries();
            context.complete();
        }
        catch (SQLException e)
        {
            processException("Could not read community(id=" + communityId + "), SQLException. Message: " + e, context);
        }
        finally
        {
            processFinally(context);
        }

        log.trace("Community(id=" + communityId + ") metadata were successfully read.");
      return metadata.toArray(new MetadataEntry[0]);
    }


    /**
     * Find community from DSpace database. It is encapsulation of method
     * org.dspace.content.Community.find with checking if item exist and if user
     * logged into context has permission to do passed action.
     * 
     * @param context
     *            Context of actual logged user.
     * @param id
     *            Id of community in DSpace.
     * @param action
     *            Constant from org.dspace.core.Constants.
     * @return It returns DSpace collection.
     * @throws WebApplicationException
     *             Is thrown when item with passed id is not exists and if user
     *             has no permission to do passed action.
     */
    private org.dspace.content.Community findCommunity(org.dspace.core.Context context, int id, int action)
            throws WebApplicationException
    {
        org.dspace.content.Community community = null;
        try
        {
            community = org.dspace.content.Community.find(context, id);

            if (community == null)
            {
                context.abort();
                log.warn("Community(id=" + id + ") was not found!");
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

        }
        catch (SQLException e)
        {
            processException("Something get wrong while finding community(id=" + id + "). SQLException, Message:" + e, context);
        }
        return community;
    }
}
