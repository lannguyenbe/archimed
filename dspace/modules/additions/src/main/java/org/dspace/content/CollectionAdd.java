package org.dspace.content;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.content.ItemAdd.DiffusionItem;
import org.dspace.content.ItemAdd.SupportItem;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseAccess;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

public class CollectionAdd extends Collection {

    public CollectionAdd(Collection collection) throws SQLException 
    {
        super(collection.ourContext, collection.getCollectionRow());
    }
        
    public static CollectionIterator findAllCursor(Context context) throws SQLException
    {
        String myQuery = "SELECT * FROM collection";

        TableRowIterator rows = DatabaseManager.queryTable(context, "collection", myQuery);

        return new CollectionIterator(context, rows);
    }

    public static CollectionIterator findByCommunity(Context context, int community_id) throws SQLException
    {
        String myQuery = "SELECT collection.* "
        		+ " FROM collection, community2collection"
                + " WHERE collection.collection_id = community2collection.collection_id"
                + " AND community2collection.community_id = " + community_id
                + " ORDER BY collection.collection_id";

        TableRowIterator rows = DatabaseManager.queryTable(context, "collection", myQuery);

        return new CollectionIterator(context, rows);
    }

    public static CollectionIterator findAllByCommunity(Context context, int community_id) throws SQLException
    {
        String myQuery = "SELECT collection.* "
        		+ " FROM collection, "
        		+ " ("
        		+ " SELECT c2col.community_id, c2col.collection_id"
        		+ " FROM community2collection c2col"
        		+ " UNION"
        		+ " SELECT c2c.parent_comm_id community_id, c2col.collection_id"
        		+ " FROM community2collection c2col, community2community c2c"
        		+ " WHERE c2c.parent_comm_id = " + community_id
        		+ " AND c2col.community_id = c2c.child_comm_id"
        		+ ") top2col"
        		+ " WHERE collection.collection_id = top2col.collection_id"
        		+ " AND top2col.community_id = " + community_id
                + " ORDER BY collection.collection_id";

        TableRowIterator rows = DatabaseManager.queryTable(context, "collection", myQuery);

        return new CollectionIterator(context, rows);
    }

    public static CollectionIterator findGeId(Context context, int id) throws SQLException
    {
        String myQuery = "SELECT * FROM collection WHERE collection_id >= " +id
                + " ORDER BY collection.collection_id";

        TableRowIterator rows = DatabaseManager.queryTable(context, "collection", myQuery);

        return new CollectionIterator(context, rows);
    }
    
    public static CollectionIterator findBetweenId(Context context, int id, int idto) throws SQLException
    {
        String myQuery = "SELECT * FROM collection WHERE collection_id between " +id
        		+ " and " +idto
                + " ORDER BY collection.collection_id";

        TableRowIterator rows = DatabaseManager.queryTable(context, "collection", myQuery);

        return new CollectionIterator(context, rows);
    }
    
    
    public List<String> findChannelsIssuedById()
            throws SQLException
    {
    	int id = this.getID();
    	String myQuery = "SELECT distinct diff.channel"
        	+ " from"
        	+ " (SELECT d.resource_type_id, d.resource_id, d.diffusion_path"
        	+ " FROM t_diffusion d"
        	+ " WHERE d.resource_type_id = " + Constants.COLLECTION
        	+ " AND d.resource_id = " + id
        	+ " AND d.is_premdiff = 1) premdiff"
        	+ " , t_diffusion diff"
        	+ " WHERE diff.resource_type_id = premdiff.resource_type_id"
        	+ " AND diff.resource_id = premdiff.resource_id"
        	+ " ORDER BY diff.channel";
    	
    	        	
    	TableRowIterator tri =  null;
    	List<String> channels = new ArrayList<String> ();
    	    	
    	try {
			tri =  DatabaseAccess.query(this.ourContext, myQuery);
			while (tri.hasNext()) {
				TableRow row = tri.next();
				channels.add( row.getStringColumn("channel"));
			}
		} finally {
			if (tri != null) { tri.close(); }
		}
    	
    	if (channels.isEmpty()) { return null; }

    	return channels;
    }
     
    public static class DiffusionCollection extends Diffusion {

    	public DiffusionCollection(String diffusion_path, int community_id, int collection_id, int item_id, String date_event, String date_diffusion, String channel) {
    		this.community_id = community_id;
    		this.collection_id = collection_id;
    		this.item_id = item_id;
    		this.diffusion_path = diffusion_path;
    		this.date_event = date_event;
    		this.date_diffusion = date_diffusion;
    		this.channel = channel;
    	}
    	
        public static DiffusionCollection[] findById(Context context, int collection_id)
                throws SQLException
        {
        	String myQuery = "SELECT t.diffusion_path, c2c.community_id, t.resource_id collection_id, null item_id"
    	    	+ " , to_char(t.event_date,'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') date_event"
    	    	+ " , to_char(t.diffusion_datetime,'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') date_diffusion"
    	    	+ " , t.channel"
    	    	+ " FROM t_diffusion t"
    	    	+ " , community2collection c2c"
     	    	+ " WHERE resource_type_id = " + Constants.COLLECTION
    	    	+ " AND resource_id = " + collection_id
    	    	+ " AND c2c.collection_id = t.resource_id"    	
    	        + " ORDER BY t.diffusion_datetime";
        	
        	TableRowIterator tri =  null;
        	List<DiffusionCollection> collections = new ArrayList<DiffusionCollection> ();
        	
        	
        	try {
				tri =  DatabaseAccess.query(context, myQuery);
				while (tri.hasNext()) {
					TableRow row = tri.next();
					collections.add(new DiffusionCollection(
							row.getStringColumn("diffusion_path")
							, row.getIntColumn("community_id")
							, row.getIntColumn("collection_id")
							, row.getIntColumn("item_id")
							, row.getStringColumn("date_event")
							, row.getStringColumn("date_diffusion")
							, row.getStringColumn("channel")
					));
				}
			} finally {
				if (tri != null) { tri.close(); }
			}
        	
        	DiffusionCollection[] dct = new DiffusionCollection[collections.size()];
        	return collections.toArray(dct);
        	
        }
    }

    public static class SupportCollection extends Support {

    	public SupportCollection(TableRow row) {
    		super(row);
    	}

    	public static SupportCollection[] findById(Context context, int collection_id)
    			throws SQLException
    			{
    		String myQuery = "SELECT distinct"
    				+ " t.code_origine"
    				+ " , t.support_type"
    				+ " , t.set_of_support_type"
    				+ " , t.support_place"
    				+ " , t.key_frame_offset"
    				+ " , t.tc_in, t.tc_out, t.htc_in, t.htc_out, t.duration"
    				+ " , t.tc_in_string, t.tc_out_string, t.htc_in_string, t.htc_out_string, t.duration_string"
    				+ " , t.origine"
    				+ " , t.category"
    				+ " , t.support_role"
    				+ " , t.sound_format"
    				+ " , t.image_format"
    				+ " , t.image_ratio"
    				+ " , t.image_color"
    				+ " FROM t_support2resource t"
    				+ " WHERE resource_type_id = " + Constants.COLLECTION
    				+ " AND resource_id = " + collection_id
        	        + " ORDER BY t.support_role, t.set_of_support_type";

    		TableRowIterator tri =  null;
    		List<SupportCollection> supports = new ArrayList<SupportCollection> ();


    		try {
    			tri =  DatabaseAccess.query(context, myQuery);
    			while (tri.hasNext()) {
    				supports.add(new SupportCollection(tri.next()));
    			}
    		} finally {
    			if (tri != null) { tri.close(); }
    		}

    		SupportCollection[] arr = new SupportCollection[supports.size()];
    		return supports.toArray(arr);

    	}
    }
    
    public static class CodeOrigineCollection extends CodeOrigine {

    	public CodeOrigineCollection(TableRow row) {
    		super(row);
    	}

    	public static CodeOrigineCollection[] findById(Context context, int collection_id)
    			throws SQLException
    			{
    		// 01.07.2016 Lan : support_place also identified a support (exemple DAL...)    		
    		String myQuery = "SELECT distinct co.id, co.code_origine, co.topcommunity_id"
	    		+ " FROM t_codeorigine co, "
	    		+ " ( "
	    		+ " SELECT s.code_origine sameAs_code_origine, s.* FROM t_support2resource s"
	    		+ " UNION SELECT s.support_place sameAs_code_origine, s.* FROM t_support2resource s WHERE s.support_place is not null"
	    		+ " )  s2r"
	    		+ " WHERE s2r.resource_type_id = " + Constants.COLLECTION 
	    		+ " AND s2r.resource_id = " + collection_id
	    		+ " AND co.code_origine = s2r.sameAs_code_origine"
	    		+ " AND co.topcommunity_id = ("
	    		+ "    SELECT top.topcommunity_id"
	    		+ "    FROM v_topcommunity top"
	    		+ "    WHERE top.resource_type_id = s2r.resource_type_id"
	    		+ "    AND top.resource_id = s2r.resource_id"
	    		+ " )"
	    		;    		

    		TableRowIterator tri =  null;
    		List<CodeOrigineCollection> codes = new ArrayList<CodeOrigineCollection> ();


    		try {
    			tri =  DatabaseAccess.query(context, myQuery);
    			while (tri.hasNext()) {
    				codes.add(new CodeOrigineCollection(tri.next()));
    			}
    		} finally {
    			if (tri != null) { tri.close(); }
    		}

    		CodeOrigineCollection[] arr = new CodeOrigineCollection[codes.size()];
    		return codes.toArray(arr);

    	}
    }
    



}
