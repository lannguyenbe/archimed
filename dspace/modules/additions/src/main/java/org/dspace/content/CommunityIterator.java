/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.dspace.core.Context;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Specialized iterator for DSpace Communities. Inspired by ItemIterator
 * 
 * @author Lan Nguyen
 */
public class CommunityIterator
{
    /*
     * This class basically wraps a TableRowIterator.
     */

    /** Our context */
    private Context ourContext;

    /** The table row iterator of Community rows */
    private TableRowIterator communityRows;

    /** a real iterator which works over the community ids when present */
    private Iterator<Integer> idcomr;
    
    /**
     * Construct an community iterator using a set of TableRow objects from
     * the community table
     * 
     * @param context
     *            our context
     * @param rows
     *            the rows that correspond to the Communities to be iterated over
     */
    public CommunityIterator(Context context, TableRowIterator rows)
    {
        ourContext = context;
        communityRows = rows;
    }

    /**
     * Construct an community iterator using an array list of community ids
     * 
     * @param context
     *            our context
     * @param cids
     *            the array list to be iterated over
     */
    public CommunityIterator(Context context, List<Integer> cids)
    {
    	ourContext = context;
    	idcomr = cids.iterator();
    }
    
    /**
     * Find out if there are any more communities to iterate over
     * 
     * @return <code>true</code> if there are more communities
     * @throws SQLException
     */
    public boolean hasNext() throws SQLException
    {
    	if (idcomr != null)
    	{
    		return idcomr.hasNext();
    	}
    	else if (communityRows != null)
    	{
    		return communityRows.hasNext();
    	}
    	return false;
    }

    /**
     * Get the next community in the iterator. Returns <code>null</code> if there
     * are no more communities.
     * 
     * @return the next community, or <code>null</code>
     * @throws SQLException
     */
    public Community next() throws SQLException
    {
    	if (idcomr != null)
    	{
    		return nextByID();
    	}
    	else if (communityRows != null)
    	{
    		return nextByRow();
    	}
    	return null;
    }
    
    /**
     * This private method knows how to get the next result out of the 
     * community id iterator
     * 
     * @return	the next community instantiated from the id
     * @throws SQLException
     */
    private Community nextByID()
    	throws SQLException
    {
    	if (idcomr.hasNext())
        {
    		// get the id
    		int id = idcomr.next().intValue();
    		
            // Check cache
            Community fromCache = (Community) ourContext.fromCache(Community.class, id);

            if (fromCache != null)
            {
                return fromCache;
            }
            else
            {
                return Community.find(ourContext, id);
            }
        }
        else
        {
            return null;
        }
    }
    
    /**
     * return the id of the next community.
     * 
     * @return	the next id or -1 if none
     */
    public int nextID()
    	throws SQLException
    {
    	if (idcomr != null)
    	{
    		return nextByIDID();
    	}
    	else if (communityRows != null)
    	{
    		return nextByRowID();
    	}
    	return -1;
    }
    
    /**
     * Sorry about the name of this one!  It returns the ID of the community
     * as opposed to the community itself when we are iterating over an ArrayList
     * of community ids
     * 
     * @return	the community id, or -1 if none
     */
    private int nextByIDID()
    {
    	if (idcomr.hasNext())
        {
    		// get the id
    		int id = idcomr.next().intValue();
    		
            return id;
        }
        else
        {
            return -1;
        }
    }
    
    /**
     * Returns the ID of the community as opposed to the community itself when we are
     * iterating over the TableRow array.
     * 
     * @return	the community id, or -1 if none
     */
    private int nextByRowID()
    	throws SQLException
    {
    	if (communityRows.hasNext())
        {
            TableRow row = communityRows.next();
            return row.getIntColumn("community_id");
        }
        else
        {
            return -1;
        }
    }
    
    /**
     * Return the next community instantiated from the supplied TableRow
     * 
     * @return	the community or null if none
     * @throws SQLException
     */
    private Community nextByRow()
    	throws SQLException
    {
    	if (communityRows.hasNext())
        {
            // Convert the row into an Community object
            TableRow row = communityRows.next();

            // Check cache
            Community fromCache = (Community) ourContext.fromCache(Community.class, row
                    .getIntColumn("community_id"));

            if (fromCache != null)
            {
                return fromCache;
            }
            else
            {
                return new Community(ourContext, row);
            }

        }
        else
        {
            return null;
        }
    }

    /**
     * Dispose of this Iterator, and it's underlying resources
     */
    public void close()
    {
        if (communityRows != null)
        {
            communityRows.close();
        }
    }
}
