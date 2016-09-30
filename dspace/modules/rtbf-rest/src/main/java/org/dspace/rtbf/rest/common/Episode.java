/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rtbf.rest.common;

import org.apache.log4j.Logger;
import org.dspace.content.CollectionAdd;
import org.dspace.content.ItemIterator;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.DiscoverSubItems;
import org.dspace.discovery.configuration.DiscoveryHitHighlightFieldConfiguration;
import org.dspace.rtbf.rest.search.SearchResponseParts;
import org.dspace.rtbf.rest.util.RsDiscoveryConfiguration;

import javax.ws.rs.WebApplicationException;
import javax.xml.bind.annotation.XmlRootElement;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@XmlRootElement(name = "episode")
public class Episode extends RTBObject {
	private static Logger log = Logger.getLogger(Episode.class);

	public Episode(){}
	
    public Episode(int viewType, org.dspace.content.Collection collection, String expand, Context context, Integer limit, Integer offset) 
    		throws SQLException, WebApplicationException
    {
        super(viewType, collection);
        setup(viewType, collection, expand, context, limit, offset);
    }

    public Episode(int viewType, org.dspace.content.Collection collection, String expand, Context context)
    		throws SQLException, WebApplicationException
    {
		this(viewType, collection, expand, context, null, null);
	}

    private void setup(int viewType, org.dspace.content.Collection collection, String expand, Context context, Integer limit, Integer offset) throws SQLException{
    	int innerViewType = 0;
    	
    	switch (viewType) {
    	case Constants.SEARCH_RESULT_VIEW:
    		this.setDateIssued(getMetadataEntry(Constants.DATE_ISSUED,collection));
    		// set channels issued related to the date issued whatever the owning serie
    		this.setChannelIssued(getMetadataEntries(Constants.CHANNEL_ISSUED,collection));
    		// set channels issued related to the diffusion of this episode on the date issued
    		this.setChannelIssuedList(findChannelsIssued(collection));
            // this.setCountSequences(collection.countItems());
    		innerViewType = Constants.MIN_VIEW;
            break;
        default:
    		innerViewType = viewType;
        	break;
    	}

        List<String> expandFields = new ArrayList<String>();
        if(expand != null) {
            expandFields = Arrays.asList(expand.split(","));
        }
        
        if(expandFields.contains("owningSerie") || expandFields.contains("all")) {
            org.dspace.content.Community parentCommunity = (org.dspace.content.Community) collection.getParentObject();
            /* 
             * Lan 22.06.2016 : use Constants.OWNING_SERIE_EXPAND_OPTIONS to return more metadata of the owningSerie
             */
             // this.setOwningSerie(new Serie(innerViewType, parentCommunity, null, context));
             this.setOwningSerie(new Serie(innerViewType, parentCommunity, Constants.OWNING_SERIE_EXPAND_OPTIONS, context));
        } else {
            this.addExpand("owningSerie");
        }

        if(expandFields.contains("owningParentList") || expandFields.contains("all")) {
            this.setOwningParentList(findOwningParentList(context, collection));
        } else {
            this.addExpand("owningParentList");
        }

        // Item paging : limit, offset/page
        if(expandFields.contains("sequences") || expandFields.contains("all")) {
        	addSequences(context, collection);
        } else {
            this.addExpand("sequences");
        }

        if(expandFields.contains("metadata") || expandFields.contains("all")) {
        	List<MetadataEntry> entries = new ArrayList<MetadataEntry>();
        	Metadatum[] dcvs = collection.getMetadataByMetadataString("*.*.*");
            for (Metadatum dcv : dcvs) {
                entries.add(new MetadataEntry(dcv.getField(), dcv.value, dcv.language));
            }
            this.setMetadataEntries(entries);
     	} else {
     		this.addExpand("metadata");
     	}

        if(expandFields.contains("diffusions") || expandFields.contains("all")) {
        	List<RTBObjectParts.Diffusion> diffusionList = new ArrayList<RTBObjectParts.Diffusion>();
        	org.dspace.content.Diffusion[] diffs = CollectionAdd.DiffusionCollection.findById(context, collection.getID());
        	for (org.dspace.content.Diffusion diff : diffs) {
        		diffusionList.add(new RTBObjectParts.Diffusion(diff.getChannel(), diff.getDate_diffusion()
        				, findOwningParentList(context, collection)));
            }
            
            this.setDiffusions(diffusionList);
     	} else {
     		this.addExpand("metadata");
     	}

        if(expandFields.contains("supports") || expandFields.contains("all")) {
        	List<RTBObjectParts.Support> supportList = new ArrayList<RTBObjectParts.Support>();
        	org.dspace.content.Support[] supports = CollectionAdd.SupportCollection.findById(context, collection.getID());
        	for (org.dspace.content.Support supp : supports) {
        		supportList.add(new RTBObjectParts.Support(supp));
            }
            
            this.setSupports(supportList);
     	} else {
     		this.addExpand("supports");
     	}

        if(!expandFields.contains("all")) {
            this.addExpand("all");
        }
    }

    private List<String> findChannelsIssued(org.dspace.content.Collection collection) throws SQLException {
		org.dspace.content.CollectionAdd collectionA = new org.dspace.content.CollectionAdd(collection);
		List<String> channels = collectionA.findChannelsIssuedById();		

		return channels;    	
    }

    private List<RTBObject> findOwningParentList(Context context, org.dspace.content.Collection owningCollection) throws WebApplicationException, SQLException{
    	int innerViewType = Constants.MIN_VIEW;
        List<RTBObject> entries = new ArrayList<RTBObject>();
       // serie level
        org.dspace.content.Community parentCommunity = (org.dspace.content.Community) owningCollection.getParentObject();
        entries.add(new Serie(innerViewType, parentCommunity, null, context));
        // repository level
        org.dspace.content.Community topparentCommunity = parentCommunity.getParentCommunity();
        entries.add(new Serie(innerViewType, topparentCommunity, null, context));
		return entries;   	
    }
    
    // copy from Sequence
    public void render(DiscoverResult.DSpaceObjectHighlightResult highlightedResults) {
    	// Cons highlight part for each sequence 
    	Map<String, List<String>> hlEntries = new LinkedHashMap<String, List<String>>();
    	for (DiscoveryHitHighlightFieldConfiguration fieldConfiguration : RsDiscoveryConfiguration.getHighlightFieldConfigurations())
    	{
    		String metadataKey = fieldConfiguration.getField();
    		List<String> hlList = highlightedResults.getHighlightResults(metadataKey);
    		if (hlList == null || hlList.size() == 0) { continue; }
    		if (MetadataEntry.getPreferredLabel(metadataKey).isEmpty()) { continue; }
    		hlEntries.put(MetadataEntry.getPreferredLabel(metadataKey), hlList);
    	}
    	this.setHlEntries(hlEntries);

    	// render title
    	List<String> hlList = highlightedResults.getHighlightResults("dc.title");
    	if (hlList != null) {
    		String title = this.getTitle().getValue();
    		this.getTitle().setValue(title.replace(hlList.get(0).replaceAll("</?em>", ""), hlList.get(0)));
    	}

    	// render description_abstract
    	hlList = highlightedResults.getHighlightResults("dc.description.abstract");
    	if (hlList != null) {
    		for (String hl : hlList) {
    			String str = this.getDescriptionAbstract().getValue();
    			this.getDescriptionAbstract().setValue(str.replace(hl.replaceAll("</?em>", ""), hl));        			
    		}
    	}
    }

    // 19.09.2016 Lan
    public void setGroupCount(DiscoverResult.GroupFilter filter) {
    	List<RTBObjectParts.GroupCount> list = new ArrayList<RTBObjectParts.GroupCount>();
    	RTBObjectParts.GroupCount g = new RTBObjectParts.GroupCount(filter);
    	list.add(g);
    	this.setGroupCount(g);
	}
    
    // 29.09.2016 Lan : sub sequences of the collection must shown the same date and channel of diffusion as of the collection
    protected void addSequences(Context context, org.dspace.content.Collection collection) {
    	
    	DiscoverResult queryResults = new DiscoverSubItems(context, collection).getqueryResults();
    	SearchResponseParts.Result resultsWrapper = new SearchResponseParts.Result(Constants.PLAYLIST_VIEW, queryResults, context);
    	this.setSequences(resultsWrapper.getLst());
    	
    }

}

