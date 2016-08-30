package org.dspace.content;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.content.CollectionAdd.CodeOrigineCollection;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.storage.rdbms.DatabaseAccess;

public class ItemAdd extends Item {
	private static Logger log = Logger.getLogger(ItemAdd.class);

    
    public ItemAdd(Item item) throws SQLException
    {
        super(item.ourContext, item.getItemRow());
    }
    
    public static ItemDup duplicate(Item item, DiffusionItem dit) throws SQLException {
    	return new ItemDup(item, dit);
    }
    
    public static ItemIterator findAllUnfiltered(Context context) throws SQLException
    {
        String myQuery = "SELECT * FROM item WHERE in_archive='1' or withdrawn='1'"
                + " ORDER BY item.item_id";

        TableRowIterator rows = DatabaseManager.queryTable(context, "item", myQuery);

        return new ItemIterator(context, rows);
    }

    public static ItemIterator findAllUnfiltered(Context context, Integer limit, Integer offset) throws SQLException
    {
        List<Serializable> params = new ArrayList<Serializable>();
        StringBuffer myQuery = new StringBuffer(
            "SELECT item.*"
            + " FROM item"
        );

        DatabaseManager.applyOffsetAndLimit(myQuery, params, offset, limit);

        TableRowIterator rows = DatabaseManager.query(context,
                myQuery.toString(), params.toArray());

        return new ItemIterator(context, rows);
    }

    public static ItemIterator findGeId(Context context, int id) throws SQLException
    {
        String myQuery = "SELECT * FROM item WHERE in_archive='1' AND item_id >="+id
                + " ORDER BY item.item_id";

        TableRowIterator rows = DatabaseManager.queryTable(context, "item", myQuery);

        return new ItemIterator(context, rows);
    }
    
    public static ItemIterator findBetweenId(Context context, int id, int idto) throws SQLException
    {
        String myQuery = "SELECT * FROM item WHERE in_archive='1' AND item_id between "+id
        		+ " and "+idto
                + " ORDER BY item.item_id";

        TableRowIterator rows = DatabaseManager.queryTable(context, "item", myQuery);

        return new ItemIterator(context, rows);
    }
    
    public static ItemIterator findByCollection(Context context, int collection_id)
            throws SQLException
    {
        String myQuery = "SELECT item.* FROM item, collection2item"
                + " WHERE in_archive='1'"
                + " AND item.item_id = collection2item.item_id"
                + " AND collection2item.collection_id = " + collection_id
                + " ORDER BY item.item_id";

        TableRowIterator rows = DatabaseManager.queryTable(context, "item", myQuery);

        return new ItemIterator(context, rows);
    }
    
    public static ItemIterator findByCommunity(Context context, int community_id)
            throws SQLException
    {
        String myQuery = "SELECT item.*"
                + " FROM item, topcommunity2item"
                + " WHERE topcommunity2item.parent_comm_id = " + community_id
                + " AND item.item_id = topcommunity2item.item_id"
                + " ORDER BY item.item_id";

        TableRowIterator rows = DatabaseManager.queryTable(context, "item", myQuery);

        return new ItemIterator(context, rows);
    }

    public static ItemIterator findGeIdByCommunity(Context context, int community_id, int item_id)
            throws SQLException
    {
        String myQuery = "SELECT item.*"
                + " FROM item, "
        		+ " (select community_id, item_id from community2item"
        		+ " union"
        		+ " select  cc.parent_comm_id community_id ,c2i.item_id"
        		+ " from community2item c2i, community2community cc"
        		+ " where cc.parent_comm_id = " + community_id
        		+ " and cc.child_comm_id = c2i.community_id"
                + " ) topcommunity2item"
                + " WHERE topcommunity2item.community_id = " + community_id
                + " AND item.item_id = topcommunity2item.item_id"
                + " AND item.item_id >= " + item_id
                + " ORDER BY item.item_id";

        TableRowIterator rows = DatabaseManager.queryTable(context, "item", myQuery);

        return new ItemIterator(context, rows);
    }

    public static ItemIterator findBetweenIdByCommunity(Context context, int community_id, int item_id, int item_id_to)
            throws SQLException
    {
        String myQuery = "SELECT item.*"
                + " FROM item, "
        		+ " (select community_id, item_id from community2item"
        		+ " union"
        		+ " select  cc.parent_comm_id community_id ,c2i.item_id"
        		+ " from community2item c2i, community2community cc"
        		+ " where cc.parent_comm_id = " + community_id
        		+ " and cc.child_comm_id = c2i.community_id"
                + " ) topcommunity2item"
                + " WHERE topcommunity2item.community_id = " + community_id
                + " AND item.item_id = topcommunity2item.item_id"
                + " AND item.item_id between " + item_id + " and " + item_id_to
                + " ORDER BY item.item_id";

        TableRowIterator rows = DatabaseManager.queryTable(context, "item", myQuery);

        return new ItemIterator(context, rows);
    }
    
    public List<String> findChannelsIssuedById()
            throws SQLException
    {
    	int item_id = this.getID();
    	String myQuery = "SELECT distinct diff.channel"
        	+ " from"
        	+ " (SELECT d.resource_type_id, d.resource_id, d.diffusion_path"
        	+ " FROM t_diffusion d"
        	+ " WHERE d.resource_type_id = " + Constants.ITEM
        	+ " AND d.resource_id = " + item_id
        	+ " AND d.is_premdiff = 1) premdiff"
        	+ " , t_diffusion diff"
        	+ " WHERE diff.resource_type_id = premdiff.resource_type_id"
        	+ " AND diff.resource_id = premdiff.resource_id"
        	+ " AND diff.diffusion_path = premdiff.diffusion_path"
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
     

    public static class ItemDup extends Item {
    	
    	private DiffusionItem diffusionItem; // is null then the defaultItemDup
    	
        public ItemDup(Item item, DiffusionItem dit) throws SQLException {
            super(item.ourContext, item.getItemRow());
            diffusionItem = dit;
        }
        
        public String getSearchUniqueID()
        {
        	if (diffusionItem == null) { // return simple search.uniqueid for the defaultItemDup
        		return(this.getType()+"-"+this.getID());
        	}
            return diffusionItem.getDiffusion_path();
        }
        

        @Override
        public Collection getOwningCollection() throws java.sql.SQLException
        {
        	if (diffusionItem == null) { // return simple search.uniqueid for the defaultItemDup
        		return(super.getOwningCollection());
        	}
            return(Collection.find(this.ourContext, diffusionItem.getCollection_id()));
        }
        
        @Override
        public Metadatum[] getMetadata(String schema, String element, String qualifier,
        		String lang)
        {
        	DiffusionItem dit = null;
        	if (diffusionItem == null) {
        		try {
					dit = DiffusionItem.findFirstById(this.ourContext, this.getID());
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}       		
        	} else {
        		dit = diffusionItem;
        	}
        	
            Metadatum[] metadata = super.getMetadata(schema, element, qualifier, lang);
        	if (dit == null) {
        		return (metadata); // return as in table METADATAVALUE
        	}
                        
            // Patch date issued and channel issued with dit
        	List<Metadatum> dcValues = new ArrayList(Arrays.asList(metadata));
            
            Iterator<Metadatum> iterator = dcValues.iterator();
            while (iterator.hasNext()) {
            	Metadatum dcValue = iterator.next();
            	String field = dcValue.schema + "." + dcValue.element;
                if (dcValue.qualifier != null && !dcValue.qualifier.trim().equals(""))
                {
                    field += "." + dcValue.qualifier;
                }
            	switch (field) {
            		case "dcterms.isPartOf.title" :
	            	case "dc.date.issued" :
	            	case "rtbf.channel_issued" : 
	            		iterator.remove();
	            		break;
	            	default:
	            		break;
            	}
            }
            
            // Add serie title for dup item
            Metadatum patch = new Metadatum();
            patch.schema = "dcterms";
            patch.element = "isPartOf";
            patch.qualifier = "title";
            try {
				patch.value = this.getOwningCollection().getParentObject().getName();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            dcValues.add(patch);

            // Add date issued for dup item
            patch = new Metadatum();
            patch.schema = "dc";
            patch.element = "date";
            patch.qualifier = "issued";
            patch.value = dit.getDate_diffusion();
            dcValues.add(patch);

            // Add channel issued for dup item
            patch = new Metadatum();
            patch.schema = "rtbf";
            patch.element = "channel_issued";
            patch.qualifier = null;
            patch.value = dit.getChannel();
            dcValues.add(patch);

            // Create an array of matching values
        	Metadatum[] valueArray = new Metadatum[dcValues.size()];
        	valueArray = (Metadatum[]) dcValues.toArray(valueArray);

        	return valueArray;
        }


    }
        
    public static class DiffusionItem extends Diffusion {

    	public DiffusionItem(String diffusion_path, int community_id, int collection_id, int item_id, String date_event, String date_diffusion, String channel) {
    		this.community_id = community_id;
    		this.collection_id = collection_id;
    		this.item_id = item_id;
    		this.diffusion_path = diffusion_path;
    		this.date_event = date_event;
    		this.date_diffusion = date_diffusion;
    		this.channel = channel;
    	}
    	
        public static DiffusionItem[] findById(Context context, int item_id)
                throws SQLException
        {
        	String myQuery = "SELECT t.diffusion_path, c2c.community_id, t.collection_id, t.resource_id item_id"
    	    	+ " , to_char(t.event_date,'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') date_event"
    	    	+ " , to_char(t.diffusion_datetime,'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') date_diffusion"
    	    	+ " , t.channel"
    	    	+ " FROM t_diffusion t"
    	    	+ " , community2collection c2c"
     	    	+ " WHERE t.resource_type_id = " + Constants.ITEM
    	    	+ " AND t.resource_id = " + item_id
    	    	+ " AND c2c.collection_id = t.collection_id"    	
    	        + " ORDER BY t.diffusion_datetime";
        	
        	TableRowIterator tri =  null;
        	List<DiffusionItem> items = new ArrayList<DiffusionItem> ();
        	
        	
        	try {
				tri =  DatabaseAccess.query(context, myQuery);
				while (tri.hasNext()) {
					TableRow row = tri.next();
					items.add(new DiffusionItem(
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
        	
        	DiffusionItem[] dit = new DiffusionItem[items.size()];
        	return items.toArray(dit);
        	
        }

        public static DiffusionItem findFirstById(Context context, int item_id)
                throws SQLException
        {
        	String myQuery = "SELECT t.diffusion_path, c2c.community_id, t.collection_id, t.resource_id item_id"
    	    	+ " , to_char(t.event_date,'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') date_event"
    	    	+ " , to_char(t.diffusion_datetime,'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') date_diffusion"
    	    	+ " , t.channel"
    	    	+ " FROM t_diffusion t"
    	    	+ " , community2collection c2c"
     	    	+ " WHERE t.resource_type_id = " + Constants.ITEM
    	    	+ " AND t.resource_id = " + item_id
       	    	+ " AND t.is_premdiff = 1 "
    	    	+ " AND c2c.collection_id = t.collection_id"    	
    	        + " AND rownum = 1"; // to be sure to get only 1 row
        	
        	TableRowIterator tri =  null;
        	DiffusionItem item = null;
        	
        	
        	try {
				tri =  DatabaseAccess.query(context, myQuery);
				while (tri.hasNext()) {
					TableRow row = tri.next();
					item = new DiffusionItem(
							row.getStringColumn("diffusion_path")
							, row.getIntColumn("community_id")
							, row.getIntColumn("collection_id")
							, row.getIntColumn("item_id")
							, row.getStringColumn("date_event")
							, row.getStringColumn("date_diffusion")
							, row.getStringColumn("channel")
							);
				}
			} finally {
				if (tri != null) { tri.close(); }
			}
        	
        	return item;
        	
        }

        public static DiffusionItem[] findDupById(Context context, int item_id)
                throws SQLException
        {
        	String myQuery = "SELECT t.diffusion_path, c2c.community_id, t.collection_id, t.resource_id item_id"
    	    	+ " , to_char(t.event_date,'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') date_event"
    	    	+ " , to_char(t.diffusion_dt,'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') date_diffusion"
    	    	+ " , t.min_channel channel"
    	    	+ " FROM ("
    	    	+ "    SELECT diffusion_path, resource_id, collection_id, event_date"
    	    	+ "     , min(diffusion_datetime) diffusion_dt"
    	    	+ "     , sum(is_premdiff) premdiff"
    	    	+ "     , min(channel) keep (dense_rank first order by diffusion_datetime) min_channel" // TODO compute all channels for this diffusion_path, not only the min
    	    	+ "    FROM t_diffusion"
    	    	+ "    WHERE resource_type_id = " + Constants.ITEM
    	    	+ "    and resource_id = " + item_id
    	    	+ "    AND diffusion_path IS NOT NULL"
    	    	+ "    GROUP BY diffusion_path, resource_id, collection_id, event_date"
    	    	+ " ) t,"
    	    	+ " community2collection c2c"
    	    	+ " WHERE t.premdiff = 0"
    	    	+ " AND c2c.collection_id = t.collection_id"    	
    	        + " ORDER BY t.event_date";
        	
        	TableRowIterator tri =  null;
        	List<DiffusionItem> items = new ArrayList<DiffusionItem> ();
        	
        	
        	try {
				tri =  DatabaseAccess.query(context, myQuery);
				while (tri.hasNext()) {
					TableRow row = tri.next();
					items.add(new DiffusionItem(
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
        	
        	DiffusionItem[] dit = new DiffusionItem[items.size()];
        	return items.toArray(dit);
        	
        }
        
    }
    
    public static class SupportItem extends Support {

    	public SupportItem(TableRow row) {
    		super(row);
    	}

    	public static SupportItem[] findById(Context context, int item_id)
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
    				+ " WHERE resource_type_id = " + Constants.ITEM
    				+ " AND resource_id = " + item_id
        	        + " ORDER BY t.support_role, t.set_of_support_type";

    		TableRowIterator tri =  null;
    		List<SupportItem> supports = new ArrayList<SupportItem> ();


    		try {
    			tri =  DatabaseAccess.query(context, myQuery);
    			while (tri.hasNext()) {
    				supports.add(new SupportItem(tri.next()));
    			}
    		} finally {
    			if (tri != null) { tri.close(); }
    		}

    		SupportItem[] arr = new SupportItem[supports.size()];
    		return supports.toArray(arr);

    	}
    }

    public static class CodeOrigineItem extends CodeOrigine {

    	public CodeOrigineItem(TableRow row) {
    		super(row);
    	}

    	public static CodeOrigineItem[] findById(Context context, int item_id)
    			throws SQLException
    			{
    		// 01.07.2016 Lan : support_place also identified a support (exemple DAL...)
    		String myQuery = "SELECT distinct co.id, co.code_origine, co.topcommunity_id"
    	    		+ " FROM t_codeorigine co, "
    	    		+ " ( "
    	    		+ " SELECT s.code_origine sameAs_code_origine, s.* FROM t_support2resource s"
    	    		+ " UNION SELECT s.support_place sameAs_code_origine, s.* FROM t_support2resource s WHERE s.support_place is not null"
    	    		+ " )  s2r"
		    		+ " WHERE s2r.resource_type_id = " + Constants.ITEM
		    		+ " AND s2r.resource_id = "+ item_id
    	    		+ " AND co.code_origine = s2r.sameAs_code_origine"
    	    		+ " AND co.topcommunity_id = ("
    	    		+ "    SELECT top.topcommunity_id"
    	    		+ "    FROM v_topcommunity top"
    	    		+ "    WHERE top.resource_type_id = s2r.resource_type_id"
    	    		+ "    AND top.resource_id = s2r.resource_id"
    	    		+ " )"
    	    		;    		
    		
    		TableRowIterator tri =  null;
    		List<CodeOrigineItem> codes = new ArrayList<CodeOrigineItem> ();


    		try {
    			tri =  DatabaseAccess.query(context, myQuery);
    			while (tri.hasNext()) {
    				codes.add(new CodeOrigineItem(tri.next()));
    			}
    		} finally {
    			if (tri != null) { tri.close(); }
    		}

    		CodeOrigineItem[] arr = new CodeOrigineItem[codes.size()];
    		return codes.toArray(arr);

    	}
    }
    
}
