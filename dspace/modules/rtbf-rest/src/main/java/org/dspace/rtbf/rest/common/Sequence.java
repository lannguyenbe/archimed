package org.dspace.rtbf.rest.common;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.ItemAdd;
import org.dspace.content.ItemAdd.DiffusionItem;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverExpandedItems;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.DiscoverResult.SearchDocument;
import org.dspace.discovery.configuration.DiscoveryHitHighlightFieldConfiguration;
import org.dspace.rtbf.rest.util.RsDiscoveryConfiguration;
import org.dspace.utils.DSpace;

import javax.ws.rs.WebApplicationException;
import javax.xml.bind.annotation.XmlRootElement;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@XmlRootElement(name = "sequence")
public class Sequence extends RTBObject{
    private static Logger log = Logger.getLogger(Sequence.class);

    public Sequence() {}
    
    public Sequence(int viewType, org.dspace.content.Item item) {
    	super(viewType, item);
    }
    
    public Sequence(int viewType, org.dspace.content.Item item, String expand, Context context) throws SQLException, WebApplicationException {
    	this(viewType, item);
    	setup(viewType, item, expand, context);    	
    }

    public Sequence(int viewType, org.dspace.content.Item item, String expand, Context context
    		, String fromIndexValue) throws SQLException, WebApplicationException {
    	this(viewType, item);
    	setupDupid(viewType, item, expand, context, fromIndexValue);
    	
    }

    private void setup(int viewType, org.dspace.content.Item item, String expand, Context context) throws SQLException {
    	int innerViewType = 0;
    	
    	switch(viewType) {
    	case Constants.PLAYLIST_VIEW:
    	case Constants.SEARCH_RESULT_VIEW:
    		this.setDateIssued(getMetadataEntry(Constants.DATE_ISSUED,item));
    		// set channels issued related to the date issued whatever the owning episode
    		this.setChannelIssued(getMetadataEntries(Constants.CHANNEL_ISSUED,item));
    		// set channels issued related to the diffusion of the owning episode on the date issued
    		this.setChannelIssuedList(findChannelsIssued(item));
    		innerViewType = Constants.MIN_VIEW;
    		break;
    	default:
    		innerViewType = viewType;
    	}

    	List<String> expandFields = new ArrayList<String>();
    	if (expand != null) {
    		expandFields = Arrays.asList(expand.split(","));
    	}

    	if(expandFields.contains("owningSerie") || expandFields.contains("all")) {
            org.dspace.content.Community parentCommunity = (org.dspace.content.Community) item.getOwningCollection().getParentObject();
            /* 
             * Lan 22.06.2016 : use Constants.OWNING_SERIE_EXPAND_OPTIONS to return more metadata of the owningSerie
             */
            // this.setOwningSerie(new Serie(innerViewType, parentCommunity, null, context));
            this.setOwningSerie(new Serie(innerViewType, parentCommunity, Constants.OWNING_SERIE_EXPAND_OPTIONS, context));
        } else {
            this.addExpand("owningSerie");
        }
    	
        if(expandFields.contains("owningParentList") || expandFields.contains("all")) {
            this.setOwningParentList(findOwningParentList(context, item.getOwningCollection()));
        } else {
            this.addExpand("owningParentList");
        }

        if(expandFields.contains("owningEpisode") || expandFields.contains("all")) {
            /* 
             * Lan 22.06.2016 : use Constants.OWNING_EPISODE_EXPAND_OPTIONSreturn to return more metadata of the owningEpisode
        	 */
        	// this.setOwningEpisode(new Episode(innerViewType, item.getOwningCollection(), null, context));
        	this.setOwningEpisode(new Episode(innerViewType, item.getOwningCollection(), Constants.OWNING_EPISODE_EXPAND_OPTIONS, context));
        } else {
            this.addExpand("owningEpisode");
        }
    	
        if(expandFields.contains("parentEpisodeList") || expandFields.contains("all")) {
        	List<Episode> entries = new ArrayList<Episode>();
            org.dspace.content.Collection[] collections = item.getCollections();
            for(org.dspace.content.Collection collection : collections) {
                entries.add(new Episode(innerViewType, collection, null, context));
            }
            this.setParentEpisodeList(entries);
        } else {
            this.addExpand("parentEpisodeList");
        }
                
        if(expandFields.contains("metadata") || expandFields.contains("all")) {
        	List<MetadataEntry> entries = new ArrayList<MetadataEntry>();
            Metadatum[] dcvs = item.getMetadata(org.dspace.content.Item.ANY, org.dspace.content.Item.ANY, org.dspace.content.Item.ANY, org.dspace.content.Item.ANY);
            for (Metadatum dcv : dcvs) {
            	entries.add(new MetadataEntry(dcv.getField(), dcv.value, dcv.language));
            }
            this.setMetadataEntries(entries);
     	} else {
     		this.addExpand("metadata");
     	}
    	
        if(expandFields.contains("diffusions") || expandFields.contains("all")) {
        	List<RTBObjectParts.Diffusion> diffusionList = new ArrayList<RTBObjectParts.Diffusion>();
        	org.dspace.content.Diffusion[] diffs = ItemAdd.DiffusionItem.findById(context, item.getID());
        	for (org.dspace.content.Diffusion diff : diffs) {

        		org.dspace.content.Collection owningCollection = Collection.find(context, diff.getCollection_id());            
        		diffusionList.add(new RTBObjectParts.Diffusion(diff.getChannel(), diff.getDate_diffusion()
        				, findOwningParentList(context, owningCollection)));
            }
            
            this.setDiffusions(diffusionList);
     	} else {
     		this.addExpand("diffusions");
     	}

        if(expandFields.contains("supports") || expandFields.contains("all")) {
        	List<RTBObjectParts.Support> supportList = new ArrayList<RTBObjectParts.Support>();
        	org.dspace.content.Support[] supports = ItemAdd.SupportItem.findById(context, item.getID());
        	for (org.dspace.content.Support supp : supports) {
        		supportList.add(new RTBObjectParts.Support(supp));
            }
            
            this.setSupports(supportList);
     	} else {
     		this.addExpand("supports");
     	}

        if(expandFields.contains("linkedDocuments") || expandFields.contains("all")) {
            // Add linked Documents 
            // a new search will be performed on solr to retrieve ALL linked documents to this item 
            // their id,type,handle,title,attributor will be available
            List<RTBObject> linkedDocuments = new ArrayList<RTBObject>();	                    
            DiscoverExpandedItems expandedItems = new DiscoverExpandedItems(context, item);
            List<DiscoverExpandedItems.ExpandedItem> entries = expandedItems.getItems();
    		for (DiscoverExpandedItems.ExpandedItem entry : entries) {
    			linkedDocuments.add(new RTBObject(entry));
    		}
    		
    		this.setLinkedDocuments(linkedDocuments);
        
        } else {
     		this.addExpand("linkedDocuments");
     	}

        if(!expandFields.contains("all")) {
            this.addExpand("all");
        }
        
    }
    
    private void setupDupid(int viewType, org.dspace.content.Item item, String expand, Context context, String fromIndexValue) throws SQLException {
        DiscoverResult queryResults;
  
    	// 1. Setup from DB metadata
        this.setup(viewType, item, expand, context);
    	
    	// 2. then retouch some metadata with Solr document metatada
    	try {
    		queryResults = searchDupid(context, item.getHandle(), fromIndexValue);
			if (queryResults != null && queryResults.getSearchDocument(item).size() > 0) {
				SearchDocument doc = queryResults.getSearchDocument(item).get(0);
				String dupid = doc.getSearchFieldValues("dup_uniqueid").get(0);
                if (!(dupid.equals(item.getType() +"-"+ item.getID()))) { // is dup item
    				this.setupFromSearchDocument(viewType, doc, expand, context);
            	}
			}
    	} catch (SearchServiceException e) {
			log.error("Unable to setupDupid : "+ e.getMessage());
		}    	    	
    }
    
    public void setupFromSearchDocument(int viewType, SearchDocument doc, String expand, Context context) throws SQLException {
    	
    	// The doc_ metadata is set in SorlServiceImpl.retrieveResult()
    	this.setDupid(doc.getSearchFieldValues("dup_uniqueid").get(0));
    	
    	int innerViewType = 0;
    	
    	switch(viewType) {
    	case Constants.SEARCH_RESULT_VIEW:
        	this.setDateIssued(new MetadataEntry(Constants.DATE_ISSUED, doc.getSearchFieldValues("dup_date_issued").get(0), null));
        	List<MetadataEntry> entries = new ArrayList<MetadataEntry>();
        	for (int i=0, len=doc.getSearchFieldValues("dup_channel_issued").size(); i < len; i++) {
        		entries.add(new MetadataEntry(Constants.CHANNEL_ISSUED, doc.getSearchFieldValues("dup_channel_issued").get(i), null));
        	}
    		this.setChannelIssued(entries);
    		innerViewType = Constants.MIN_VIEW;
    		break;
    	default:
    		innerViewType = viewType;
    	}
    	
        Integer collectionId = Integer.parseInt(doc.getSearchFieldValues("dup_owning_collection").get(0).replaceFirst(String.valueOf(Constants.COLLECTION) + '-', ""));

        if(expand.contains("owningParentList")) {
            org.dspace.content.Collection owningCollection = Collection.find(context, collectionId);
            this.setOwningParentList(findOwningParentList(context, owningCollection));
        }
    	
        if(expand.contains("owningEpisode")) {
            /* 
             * Lan 22.06.2016 : return more metadata of the owningEpisode - see Constants.OWNING_EPISODE_EXPAND_OPTIONS
        	 * this.setOwningEpisode(new Episode(innerViewType, Collection.find(context, collectionId), null, context));
        	 */
        	this.setOwningEpisode(new Episode(innerViewType, Collection.find(context, collectionId), Constants.OWNING_EPISODE_EXPAND_OPTIONS, context));
        }
        
    	if(expand.contains("owningSerie")) {
            org.dspace.content.Collection owningCollection = Collection.find(context, collectionId);
            org.dspace.content.Community parentCommunity = (org.dspace.content.Community) owningCollection.getParentObject();
            /* 
             * Lan 22.06.2016 : return more metadata of the owningSerie - see Constants.OWNING_SERIE_EXPAND_OPTIONS
             * this.setOwningSerie(new Serie(innerViewType, parentCommunity, null, context));
        	 */
            this.setOwningSerie(new Serie(innerViewType, parentCommunity, Constants.OWNING_SERIE_EXPAND_OPTIONS, context));
        }
    }
        
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
    
    private DiscoverResult searchDupid(Context context, String handle, String fromIndexValue) throws SearchServiceException, SQLException {

    	DiscoverQuery query = new DiscoverQuery();
        
        String[] searchFields = {"search.uniqueid"};
        for (String sf : searchFields) {
        	query.addSearchField(sf);			
		}
        // filter query
        query.addFilterQueries("handle:"+handle);

        if (fromIndexValue.startsWith(org.dspace.core.Constants.ITEM+"-")) {
        	query.addFilterQueries("search.uniqueid:"+fromIndexValue);    	    		
    	} else if (fromIndexValue.startsWith(org.dspace.core.Constants.COLLECTION+"-")) {
    		query.addFilterQueries("owning_collection:"+fromIndexValue);    	    		    		
    	}
        
        return (getSearchService().search(context, query));
    }


    protected SearchService getSearchService()
    {
        DSpace dspace = new DSpace();
        
        org.dspace.kernel.ServiceManager manager = dspace.getServiceManager() ;

        return manager.getServiceByName(SearchService.class.getName(),SearchService.class);
    }

    private List<String> findChannelsIssued(org.dspace.content.Item item) throws SQLException {
		org.dspace.content.ItemAdd itemA = new org.dspace.content.ItemAdd(item);
		List<String> channels = itemA.findChannelsIssuedById();		

/*		if (channels == null) { // no row in SEGMENT_DIFFUSION for the sequence, look in Metadatavalue Table that has value(s) inherited from Episode
			channels = new ArrayList<String>();
    		List<MetadataEntry> channelsInMetadatavalueTable = getMetadataEntries(Constants.CHANNEL_ISSUED,item);
    		for (MetadataEntry ch : channelsInMetadatavalueTable) {
				channels.add(ch.value);
			}    		
		}
*/
		return channels;    	
    }
    
    private List<RTBObject> findOwningParentList(Context context, org.dspace.content.Collection owningCollection) throws WebApplicationException, SQLException{
    	int innerViewType = Constants.MIN_VIEW;
        List<RTBObject> entries = new ArrayList<RTBObject>();
        // collection level
        entries.add(new Episode(innerViewType, owningCollection, null, context));
        // serie level
        org.dspace.content.Community parentCommunity = (org.dspace.content.Community) owningCollection.getParentObject();
        entries.add(new Serie(innerViewType, parentCommunity, null, context));
        // repository level
        org.dspace.content.Community topparentCommunity = parentCommunity.getParentCommunity();
    	entries.add(new Serie(innerViewType, topparentCommunity, null, context));
		return entries;   	
    }
        		                    
}
