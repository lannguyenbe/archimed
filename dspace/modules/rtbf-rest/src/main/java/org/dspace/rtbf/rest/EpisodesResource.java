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
import org.dspace.content.ItemIterator;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.DiscoverSubItems;
import org.dspace.rtbf.rest.common.Constants;
import org.dspace.rtbf.rest.common.Episode;
import org.dspace.rtbf.rest.common.MetadataEntry;
import org.dspace.rtbf.rest.common.RTBObject;
import org.dspace.rtbf.rest.common.Sequence;
import org.dspace.rtbf.rest.search.Resource;
import org.dspace.rtbf.rest.search.SearchResponseParts;

/**
 * This class provides all CRUD operation over collections.
 * 
 * @author Rostislav Novak (Computing and Information Centre, CTU in Prague)
 * 
 * 25.10.2015 Lan : Restrict to GET only
 * 
 */
@Path("/episodes")
public class EpisodesResource extends Resource
{
    private static Logger log = Logger.getLogger(EpisodesResource.class);

    /**
     * Return instance of collection with passed id
     */
    @GET
    @Path("/{collection_id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Episode getEpisode(@PathParam("collection_id") Integer collectionId, @QueryParam("expand") String expand, 
    		@QueryParam("omitExpand") @DefaultValue("true") boolean omitExpand,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request) 
            throws WebApplicationException
    {
        org.dspace.core.Context context = null;
    	int viewType = Constants.MIN_VIEW;

    	if (!omitExpand) { viewType = Constants.EXPANDELEM_VIEW; }
    	
        log.info("Reading collection(id=" + collectionId + ").");
        Episode episode = null;

        try
        {
            context = new org.dspace.core.Context();
            context.getDBConnection();
            
            org.dspace.content.Collection dspaceCollection = findCollection(context, collectionId, org.dspace.core.Constants.READ);

            episode = new org.dspace.rtbf.rest.common.Episode(viewType, dspaceCollection, expand+","+Constants.EPISODE_EXPAND_OPTIONS, context);
            context.complete();

        }
        catch (SQLException e)
        {
            processException("Could not read collection(id=" + collectionId + "), SQLException. Message: " + e, context);
        }
        finally
        {
            processFinally(context);
        }

        log.trace("Collection(id=" + collectionId + ") has been successfully read.");
        return episode;
    }

    
    /**
     * Return array of items in collection
     * 
     */
    @GET
    @Path("/{collection_id}/sequences")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Sequence[] getEpisodeSequences(@PathParam("collection_id") Integer collectionId,
            @QueryParam("expand") String expand, @QueryParam("limit") @DefaultValue(Constants.DEFAULT_LIMIT) Integer limit,
            @QueryParam("offset") @DefaultValue("0") Integer offset, @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {
        org.dspace.core.Context context = null;
    	// int viewType = Constants.MIN_VIEW;
        // 02.05.2016 Lan : Constants.PLAYLIST_VIEW to get date diffusion and channel
    	int viewType = Constants.PLAYLIST_VIEW;

        log.info("Reading collection(id=" + collectionId + ") items.");
        List<RTBObject> sequences = null;

        try
        {
            context = new org.dspace.core.Context();
            context.getDBConnection();
            
            org.dspace.content.Collection collection = findCollection(context, collectionId, org.dspace.core.Constants.READ);

        	DiscoverResult queryResults = new DiscoverSubItems(context, collection).getqueryResults();
        	SearchResponseParts.Result resultsWrapper = new SearchResponseParts.Result(viewType, queryResults, context);
            sequences = resultsWrapper.getLst();

            context.complete();
        }
        catch (SQLException e)
        {
            processException("Could not read collection items, SQLException. Message: " + e, context);
        }
        finally
        {
            processFinally(context);
        }

        log.trace("All items in collection(id=" + collectionId + ") were successfully read.");
        return sequences.toArray(new Sequence[0]);
    }

    
    /**
     * Returns episode metadata 
     * 
     */
    @GET
    @Path("/{collection_id}/metadata")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public MetadataEntry[] getEpisodeMetadata(@PathParam("collection_id") Integer collectionId, @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {
        org.dspace.core.Context context = null;
    	int viewType = Constants.MIN_VIEW;
    	
    	log.info("Reading collection(id=" + collectionId + ") metadata.");
        List<MetadataEntry> metadata = null;

        try
        {
            context = new org.dspace.core.Context();
            context.getDBConnection();

            org.dspace.content.Collection dspaceCollection = findCollection(context, collectionId, org.dspace.core.Constants.READ);

            metadata = new org.dspace.rtbf.rest.common.Episode(viewType, dspaceCollection, "metadata", context).getMetadataEntries();
            context.complete();
        }
        catch (SQLException e)
        {
            processException("Could not read collection(id=" + collectionId + "), SQLException. Message: " + e, context);
        }
        finally
        {
            processFinally(context);
        }

        log.trace("Collection(id=" + collectionId + ") metadata were successfully read.");
      return metadata.toArray(new MetadataEntry[0]);
    }

    /**
     * Find collection from DSpace database. It is encapsulation of method
     * org.dspace.content.Collection.find with checking if item exist and if
     * user logged into context has permission to do passed action.
     * 
     * @param context
     *            Context of actual logged user.
     * @param id
     *            Id of collection in DSpace.
     * @param action
     *            Constant from org.dspace.core.Constants.
     * @return It returns DSpace collection.
     * @throws WebApplicationException
     *             Is thrown when item with passed id is not exists and if user
     *             has no permission to do passed action.
     */
    private org.dspace.content.Collection findCollection(org.dspace.core.Context context, int id, int action)
            throws WebApplicationException
    {
        org.dspace.content.Collection collection = null;
        try
        {
            collection = org.dspace.content.Collection.find(context, id);

            if (collection == null)
            {
                context.abort();
                log.warn("Collection(id=" + id + ") was not found!");
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

        }
        catch (SQLException e)
        {
            processException("Something get wrong while finding collection(id=" + id + "). SQLException, Message: " + e, context);
        }
        return collection;
    }
}
