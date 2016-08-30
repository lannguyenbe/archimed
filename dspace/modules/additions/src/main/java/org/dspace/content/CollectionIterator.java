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
 * Specialized iterator for DSpace Collections. Inspired by ItemIterator
 * 
 * @author Lan Nguyen
 */
public class CollectionIterator
{
    /*
     * This class basically wraps a TableRowIterator.
     */

    /** Our context */
    private Context ourContext;

    /** The table row iterator of Collection rows */
    private TableRowIterator collectionRows;

    /** a real iterator which works over the collection ids when present */
    private Iterator<Integer> idcolr;
    
    /**
     * Construct an collection iterator using a set of TableRow objects from
     * the collection table
     * 
     * @param context
     *            our context
     * @param rows
     *            the rows that correspond to the Collections to be iterated over
     */
    public CollectionIterator(Context context, TableRowIterator rows)
    {
        ourContext = context;
        collectionRows = rows;
    }

    /**
     * Construct an collection iterator using an array list of collection ids
     * 
     * @param context
     *            our context
     * @param cids
     *            the array list to be iterated over
     */
    public CollectionIterator(Context context, List<Integer> cids)
    {
    	ourContext = context;
    	idcolr = cids.iterator();
    }
    
    /**
     * Find out if there are any more collections to iterate over
     * 
     * @return <code>true</code> if there are more collections
     * @throws SQLException
     */
    public boolean hasNext() throws SQLException
    {
    	if (idcolr != null)
    	{
    		return idcolr.hasNext();
    	}
    	else if (collectionRows != null)
    	{
    		return collectionRows.hasNext();
    	}
    	return false;
    }

    /**
     * Get the next collection in the iterator. Returns <code>null</code> if there
     * are no more collections.
     * 
     * @return the next collection, or <code>null</code>
     * @throws SQLException
     */
    public Collection next() throws SQLException
    {
    	if (idcolr != null)
    	{
    		return nextByID();
    	}
    	else if (collectionRows != null)
    	{
    		return nextByRow();
    	}
    	return null;
    }
    
    /**
     * This private method knows how to get the next result out of the 
     * collection id iterator
     * 
     * @return	the next collection instantiated from the id
     * @throws SQLException
     */
    private Collection nextByID()
    	throws SQLException
    {
    	if (idcolr.hasNext())
        {
    		// get the id
    		int id = idcolr.next().intValue();
    		
            // Check cache
            Collection fromCache = (Collection) ourContext.fromCache(Collection.class, id);

            if (fromCache != null)
            {
                return fromCache;
            }
            else
            {
                return Collection.find(ourContext, id);
            }
        }
        else
        {
            return null;
        }
    }
    
    /**
     * return the id of the next collection.
     * 
     * @return	the next id or -1 if none
     */
    public int nextID()
    	throws SQLException
    {
    	if (idcolr != null)
    	{
    		return nextByIDID();
    	}
    	else if (collectionRows != null)
    	{
    		return nextByRowID();
    	}
    	return -1;
    }
    
    /**
     * Sorry about the name of this one!  It returns the ID of the collection
     * as opposed to the collection itself when we are iterating over an ArrayList
     * of collection ids
     * 
     * @return	the collection id, or -1 if none
     */
    private int nextByIDID()
    {
    	if (idcolr.hasNext())
        {
    		// get the id
    		int id = idcolr.next().intValue();
    		
            return id;
        }
        else
        {
            return -1;
        }
    }
    
    /**
     * Returns the ID of the collection as opposed to the collection itself when we are
     * iterating over the TableRow array.
     * 
     * @return	the collection id, or -1 if none
     */
    private int nextByRowID()
    	throws SQLException
    {
    	if (collectionRows.hasNext())
        {
            TableRow row = collectionRows.next();
            return row.getIntColumn("collection_id");
        }
        else
        {
            return -1;
        }
    }
    
    /**
     * Return the next collection instantiated from the supplied TableRow
     * 
     * @return	the collection or null if none
     * @throws SQLException
     */
    private Collection nextByRow()
    	throws SQLException
    {
    	if (collectionRows.hasNext())
        {
            // Convert the row into an Collection object
            TableRow row = collectionRows.next();

            // Check cache
            Collection fromCache = (Collection) ourContext.fromCache(Collection.class, row
                    .getIntColumn("collection_id"));

            if (fromCache != null)
            {
                return fromCache;
            }
            else
            {
                return new Collection(ourContext, row);
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
        if (collectionRows != null)
        {
            collectionRows.close();
        }
    }
}
