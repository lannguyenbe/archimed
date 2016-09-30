package org.dspace.rtbf.rest.search;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverExpandedItems;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.DiscoverResult.FacetResult;
import org.dspace.discovery.DiscoverResult.SearchDocument;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryHitHighlightFieldConfiguration;
import org.dspace.rtbf.rest.common.Constants;
import org.dspace.rtbf.rest.common.Episode;
import org.dspace.rtbf.rest.common.MetadataEntry;
import org.dspace.rtbf.rest.common.MetadataWrapper;
import org.dspace.rtbf.rest.common.RTBObject;
import org.dspace.rtbf.rest.common.Sequence;
import org.dspace.rtbf.rest.common.Serie;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class SearchResponseParts {
    private static Logger log = Logger.getLogger(SearchResponseParts.class);
	
	public static class ResponseHeader {

	}

	public static class Result {
		
	    private List<org.dspace.rtbf.rest.common.RTBObject> lst;

 		public Result(DiscoverResult queryResults, Context context) {
 			this(Constants.SEARCH_RESULT_VIEW, queryResults, context);
 		}

 		public Result(int viewType, DiscoverResult queryResults, Context context) {
			int resultType = 0;
					
			if (queryResults != null && queryResults.getDspaceObjects().size() > 0) {
				
				lst = new ArrayList<org.dspace.rtbf.rest.common.RTBObject>();
				List<org.dspace.content.DSpaceObject> dsoList = queryResults.getDspaceObjects();
				
				if (! dsoList.isEmpty()) {
					resultType = dsoList.get(0).getType();
				}
				
				String viewExpandOptions = (viewType == Constants.SEARCH_RESULT_VIEW) ? Constants.SEARCH_SEQUENCE_EXPAND_OPTIONS : "";
				
	            for (org.dspace.content.DSpaceObject result : dsoList) {
	            	DiscoverResult.DSpaceObjectHighlightResult highlightedResults;
	            	
					try {
						switch (resultType) {
						case Constants.ITEM:
		                    Sequence sequence = new Sequence(viewType, (Item) result, viewExpandOptions, context);
		                    
		                    // 18.04.2016 Lan : if queryResults from Solr return an dup item, then replace some metadata in the default sequence
		                    SearchDocument doc = queryResults.getSearchDocument(result).get(0);
		                    String dupid = doc.getSearchFieldValues("dup_uniqueid").get(0);
		                    if (!(dupid.equals( result.getType() +"-"+ result.getID()))) { // is dup item
		                    	sequence.setupFromSearchDocument(viewType, doc, viewExpandOptions, context);
	                    	}
		                    
/*
 * 29.09.2016 Lan : expand section from query result is not used any more; for each item we use a solr subquery to retrieve linkedDocuments		                    
		                    // Add linked Documents 
		                    // the linked documents to this dso were already retrieved by the same search - in the expanded section of solr response - 
		                    // only their handle are available
		                    List<RTBObject> linkedDocuments = new ArrayList<RTBObject>();	                    
		                    List<DiscoverResult.SearchDocument> entries = queryResults.getExpandDocuments(result);
		            		for (SearchDocument entry : entries) {
		            			Set<String> uniques = new HashSet<String>();
		            			// 19.04.2016 Lan : eliminate dup item in expanded items
		            			String entryHandle = entry.getSearchFieldValues("handle").get(0);
		            			if (!(result.getHandle().equals(entryHandle))) { // is not dup item
		            				if (uniques.add(entryHandle)) { // is unique among expanded item     		            				
		            					linkedDocuments.add(new RTBObject(new DiscoverExpandedItems.ExpandedItem(entry)));
		            				}
		            			}
		            		}
		            		if (linkedDocuments.size() > 0) {
		            			sequence.setLinkedDocuments(linkedDocuments);
		            		}
*/		            		
		            		// Set highlighted snippets
		                    highlightedResults = queryResults.getHighlightedResults(result);
		                    if (highlightedResults != null) {
		                    	sequence.render(highlightedResults);
		                    }
		            		
							lst.add(sequence);
							break;
						case Constants.COLLECTION:
							Episode episode = new Episode(viewType, (Collection) result, viewExpandOptions, null);
							
							// Lan 19.09.2016 : setGroupCount
							episode.setGroupCount(queryResults.getGroupFilter(result));

							// Set highlighted snippets
		                    highlightedResults = queryResults.getHighlightedResults(result);
		                    if (highlightedResults != null) {
		                    	episode.render(highlightedResults);
		                    }
							
							lst.add(episode);
							break;
						case Constants.COMMUNITY:
							Serie serie = new Serie(viewType, (Community) result, null, null);

							// Lan 19.09.2016 : setGroupCount
							serie.setGroupCount(queryResults.getGroupFilter(result));
							
							lst.add(serie);
							break;
						}
					} catch (WebApplicationException e) {
						log.error("Unable to add to result list: "+ e);
					} catch (SQLException e) {  //ignore 
						log.error("Unable to add to result list: sqlexception:"+ e);
					}
				}
			}
			
		}

		public List<org.dspace.rtbf.rest.common.RTBObject> getLst() {
			return lst;
		}

		public void setLst(List<org.dspace.rtbf.rest.common.RTBObject> lst) {
			this.lst = lst;
		}
		
	}

	public static class Meta {

		private List<MetadataEntry> sortEntries;
		private MetadataWrapper sortMeta;

	    public Meta() {
	    	int idx = 1;
	    	String definition;
	    	
	    	sortEntries = new ArrayList<MetadataEntry>();
		    while ((definition = ConfigurationManager.getProperty(Constants.WEBAPP_NAME, Constants.SORTMETA+".field." + idx)) != null) {
		        List<String> fields = new ArrayList<String>();
		        fields = Arrays.asList(definition.split(":"));
	            sortEntries.add(new MetadataEntry(fields.get(0), fields.get(1), null));
		    	
		    	idx++;
		    }
	    }

	    @JsonIgnore
	    @XmlTransient
	    public List<MetadataEntry> getSortEntries() {
			return sortEntries;
		}

		public void setSortEntriest(List<MetadataEntry> entries) { // neither jaxb nor jackson
			this.sortEntries = entries;
		}

	    @JsonIgnore
		public MetadataWrapper getSortMeta() { // jaxb only
			if (sortEntries != null ) {
				sortMeta = new MetadataWrapper(sortEntries);
			}
			return sortMeta;
		}

		public void setSortMeta(MetadataWrapper wrapper) {
			this.sortMeta = wrapper;
		}

		@JsonGetter("sortMeta")
		@XmlTransient
		protected Map<String, Object> getMetadataEntriesAsMap() { // Jackson only
			return MetadataEntry.listAsMap(this.sortEntries);
		}
	}


	// Lan 05.01.2015 : The following json serializer is not use, the conventional Map serialization is preferred for ng-repeat
	// @JsonSerialize(using = SearchResponseParts.FacetCountsSerializer.class)
	public static class FacetCounts {
		
		public static class Filter {
			public String filtertype;
			public String filter_relational_operator;
			public String filter;
			

			public Filter() {}
			public Filter(String ft, String fo, String fv) {
				this.filtertype = ft;
				this.filter_relational_operator = fo;
				this.filter = fv;				
			}
		}
		
		public static class Entry {

			public String key;
			public long count;
			
			public Filter fq;
		}
		
		private Map<String, List<Entry>> facetEntries;
		
 		public FacetCounts(DiscoverResult queryResults) {
 			this.facetEntries = new LinkedHashMap<String, List<Entry>>();
 			
 			Map<String, List<FacetResult>> m = queryResults.getFacetResults();
			for (Map.Entry<String, List<FacetResult>> mEntry : m.entrySet()) {
				List<Entry> entries = new ArrayList<Entry>();
				String[] keyParts = mEntry.getKey().split(":");
				String facetKey = keyParts[0];
				String fqField = keyParts[keyParts.length-1];
				for (FacetResult f : mEntry.getValue()) {
					
					Entry e = new Entry();
					e.key = f.getDisplayedValue();
					e.count = f.getCount();
					e.fq = new Filter(fqField, f.getFilterType(), f.getAsFilterQuery());
					if (facetKey.endsWith(Resource._DT)) { /* is a date math filter, then filter value is a preformatted range */
						String[] dtMath = Resource._DTMATH.get(f.getAsFilterQuery()); 
						if (dtMath != null) { /* dtMath[1] is the preformatted range */
							e.fq = new Filter(fqField, f.getFilterType(), dtMath[1]);
						}
					}
										
					entries.add(e);
				}
				this.facetEntries.put(facetKey, entries);
			}					
 		}

 		@XmlJavaTypeAdapter(SearchResponseParts.FacetCountsAdapter.class)
 		public Map<String, List<Entry>> getFacetEntries() {
			return facetEntries;
		}

		public void setFacetEntries(Map<String, List<Entry>> facetEntries) {
			this.facetEntries = facetEntries;
		}
		
	}


	/*
	 * Jaxb Adapters
	 */
	// FacetCounsAdapter transform Map to List
	public static class FacetCountsAdapter extends XmlAdapter<FacetCountsAdapter.AdapteFacetCounts,  Map<String, List<FacetCounts.Entry>>> {
		
		public static class AdapteFacetCounts {
			public List<ListEntry> fieldEntry = new ArrayList<ListEntry>();
		}
		
		public static class ListEntry {
			public String field;
			public List<FacetCounts.Entry> entry;
		}

		@Override
		public Map<String, List<FacetCounts.Entry>> unmarshal(AdapteFacetCounts v)
				throws Exception {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public AdapteFacetCounts marshal(Map<String, List<FacetCounts.Entry>> v)
				throws Exception {
			AdapteFacetCounts adapte = new AdapteFacetCounts();
			for (Map.Entry<String, List<FacetCounts.Entry>> mEntry : v.entrySet()) {
				ListEntry e = new ListEntry();
				e.field = mEntry.getKey();
				e.entry = mEntry.getValue();
				adapte.fieldEntry.add(e);
			}					
			return adapte;
		}
	}
	

	/*
	 * Jackson custom serializers
	 */
	// FacetCountSerializer (actually not use)
	public static class FacetCountsSerializer extends JsonSerializer<FacetCounts> {

		@Override
		public void serialize(FacetCounts fc, JsonGenerator jgen,
				SerializerProvider provider) throws IOException,
				JsonProcessingException {
			
			Map<String, List<FacetCounts.Entry>> v = fc.getFacetEntries();
			
			jgen.writeStartObject();
			for (Map.Entry<String, List<FacetCounts.Entry>> me : v.entrySet()) {
				jgen.writeArrayFieldStart(me.getKey());
				for (FacetCounts.Entry e : me.getValue()) {
					jgen.writeString(e.key);					
					jgen.writeNumber(e.count);					
				}
				jgen.writeEndArray();			
			}					
			jgen.writeEndObject();
		}
		
	}

		
}
