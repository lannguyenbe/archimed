/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rtbf.rest.common;

import org.apache.log4j.Logger;
import org.dspace.content.CollectionIterator;
import org.dspace.content.CommunityIterator;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverResult;

import javax.ws.rs.WebApplicationException;
import javax.xml.bind.annotation.XmlRootElement;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@XmlRootElement(name = "serie")
public class Serie extends RTBObject{
    private static Logger log = Logger.getLogger(Serie.class);

    public Serie(){}
    
    public Serie(int viewType, org.dspace.content.Community community, String expand, Context context, Integer limit, Integer offset) throws SQLException, WebApplicationException{
        super(viewType, community);
        setup(viewType, community, expand, context, limit, offset);
    }

    public Serie(int viewType, org.dspace.content.Community community, String expand, Context context) throws SQLException, WebApplicationException{
    	this(viewType, community, expand, context, null, null);
    }

    private void setup(int viewType, org.dspace.content.Community community, String expand, Context context, Integer limit, Integer offset) throws SQLException{
    	
    	switch (viewType) {
    	case Constants.SEARCH_RESULT_VIEW:
/*    		TODO : This way of counting is NOT optimal : to optimize
	        int[] counts = {0,0,0};
	    	community.getCounts(counts);
	        this.setCountSequences(counts[0]);
	        this.setCountEpisodes(counts[1]);
	        this.setCountSubSeries(counts[2]);
*/	        
    		break;
    	}
	        
        List<String> expandFields = new ArrayList<String>();
        if(expand != null) {
            expandFields = Arrays.asList(expand.split(","));
        }

        if(expandFields.contains("owningSerie") || expandFields.contains("all")) {
            org.dspace.content.Community parentCommunity = community.getParentCommunity();
            if(parentCommunity != null) {
                this.setOwningSerie(new Serie(viewType, parentCommunity, null, context));
            }
        } else {
            this.addExpand("owningSerie");
        }

        if(expandFields.contains("owningParentList") || expandFields.contains("all")) {
            org.dspace.content.Community parentCommunity = community.getParentCommunity();
            if(parentCommunity != null) {
                this.setOwningSerie(new Serie(viewType, parentCommunity, null, context));
            }
        } else {
            this.addExpand("owningParentList");
        }

        // Episodes pagination
        if(expandFields.contains("episodes") || expandFields.contains("all")) {
            List<Episode> entries = new ArrayList<Episode>();
            if (!((limit != null) && (limit >= 0) && (offset != null) && (offset >= 0))) {
                log.warn("Pagging was badly set, using default values.");
                limit = Constants.LIMITMAX;
                offset = 0;
            }
            CollectionIterator childCollections = community.getCollections(limit, offset);
            while(childCollections.hasNext()) {
                org.dspace.content.Collection collection = childCollections.next();
                	entries.add(new Episode(viewType, collection, null, context));
            }
            this.setEpisodes(entries);
        
        } else {
            this.addExpand("episodes");
        }

        if(expandFields.contains("subSeries") || expandFields.contains("all")) {
            List<Serie> entries = new ArrayList<Serie>();

            if (!((limit != null) && (limit >= 0) && (offset != null) && (offset >= 0))) {
                log.warn("Paging was badly set, using default values.");
                limit = Constants.LIMITMAX;
                offset = 0;
            }

    		CommunityIterator childCommunities = community.getSubCommunities(limit, offset);
            while(childCommunities.hasNext()) {
                org.dspace.content.Community subCommunity = childCommunities.next();
                	entries.add(new Serie(viewType, subCommunity, null, context));
            }
            this.setSubSeries(entries);
            
        } else {
            this.addExpand("subSeries");
        }

        if(expandFields.contains("metadata") || expandFields.contains("all")) {
    		List<MetadataEntry> entries = new ArrayList<MetadataEntry>();
            Metadatum[] dcvs = getAllMetadata(community);
            for (Metadatum dcv : dcvs) {
                entries.add(new MetadataEntry(dcv.getField(), dcv.value, dcv.language));
           }
            this.setMetadataEntries(entries);
     	} else {
     		this.addExpand("metadata");
     	}

        if(!expandFields.contains("all")) {
            this.addExpand("all");
        }
    }
    
    // 20.09.2016 Lan
    public void setGroupCount(DiscoverResult.GroupFilter filter) {
    	if (filter == null) return;

    	RTBObjectParts.GroupCount g = new RTBObjectParts.GroupCount(filter);
    	this.setGroupCount(g);}
}
