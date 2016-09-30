/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rtbf.rest;

import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.rtbf.rest.common.Constants;
import org.dspace.rtbf.rest.common.MetadataEntry;
import org.dspace.rtbf.rest.common.Sequence;
import org.dspace.rtbf.rest.search.Resource;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


/**
 * Class which provide all CRUD methods over items.
 * 
 * @author Rostislav Novak (Computing and Information Centre, CTU in Prague)
 *
 * 18.10.2015 Lan : Restrict to GET only
 * 
 */
// Every DSpace class used without namespace is from package org.dspace.rest.common.*. Otherwise namespace is defined.
@SuppressWarnings("deprecation")
@Path("/sequences")
public class SequencesResource extends Resource
{

    private static final Logger log = Logger.getLogger(SequencesResource.class);


    /**
     * Returns item
     * 
     */
    @GET
    @Path("/{item_id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Sequence getSequence(@PathParam("item_id") Integer itemId, @QueryParam("expand") String expand,
    		@QueryParam("omitExpand") @DefaultValue("true") boolean omitExpand,
            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {
        org.dspace.core.Context context = null;
    	int viewType = Constants.MIN_VIEW;
    	
    	if (!omitExpand) { viewType = Constants.EXPANDELEM_VIEW; }

    	log.info("Reading item(id=" + itemId + ") metadata.");
        Sequence sequence = null;

        try
        {
            context = new org.dspace.core.Context();
            context.getDBConnection();

            org.dspace.content.Item dspaceItem = findItem(context, itemId, org.dspace.core.Constants.READ);

            sequence = new org.dspace.rtbf.rest.common.Sequence(viewType, dspaceItem, expand+","+Constants.SEQUENCE_EXPAND_OPTIONS, context);
            context.complete();
        }
        catch (SQLException e)
        {
            processException("Could not read item(id=" + itemId + "), SQLException. Message: " + e, context);
        }
        finally
        {
            processFinally(context);
        }

        log.trace("Item(id=" + itemId + ") was successfully read.");
        return sequence;
    }


    /**
     * Returns item metadata 
     * 
     */
    @GET
    @Path("/{item_id}/metadata")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public MetadataEntry[] getSequenceMetadata(@PathParam("item_id") Integer itemId, @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {
        org.dspace.core.Context context = null;
    	int viewType = Constants.MIN_VIEW;
    	
    	log.info("Reading item(id=" + itemId + ") metadata.");
        List<MetadataEntry> metadata = null;

        try
        {
            context = new org.dspace.core.Context();
            context.getDBConnection();

            org.dspace.content.Item dspaceItem = findItem(context, itemId, org.dspace.core.Constants.READ);

            metadata = new org.dspace.rtbf.rest.common.Sequence(viewType, dspaceItem, "metadata", context).getMetadataEntries();
            context.complete();
        }
        catch (SQLException e)
        {
            processException("Could not read item(id=" + itemId + "), SQLException. Message: " + e, context);
        }
        finally
        {
            processFinally(context);
        }

        log.trace("Item(id=" + itemId + ") metadata were successfully read.");
      return metadata.toArray(new MetadataEntry[0]);
    }


    
    /**
     * Find item from DSpace database. It is encapsulation of method
     * org.dspace.content.Item.find with checking if item exist and if user
     * logged into context has permission to do passed action.
     * 
     * @param context
     *            Context of actual logged user.
     * @param id
     *            Id of item in DSpace.
     * @param action
     *            Constant from org.dspace.core.Constants.
     * @return It returns DSpace item.
     * @throws WebApplicationException
     *             Is thrown when item with passed id is not exists and if user
     *             has no permission to do passed action.
     */
    private org.dspace.content.Item findItem(org.dspace.core.Context context, int id, int action) throws WebApplicationException
    {
        org.dspace.content.Item item = null;
        try
        {
            item = org.dspace.content.Item.find(context, id);

            if (item == null)
            {
                context.abort();
                log.warn("Item(id=" + id + ") was not found!");
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

        }
        catch (SQLException e)
        {
            processException("Something get wrong while finding item(id=" + id + "). SQLException, Message: " + e, context);
        }
        return item;
    }
}
