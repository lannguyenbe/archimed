package org.dspace.rtbf.rest.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import org.dspace.discovery.DiscoverResult;
import org.dspace.rtbf.rest.common.RTBObject;

@XmlRootElement(name = "searchResponse")
public class SearchResponse {
	
	private Long numFound = 0L;

    private SearchResponseParts.ResponseHeader responseHeader;
    private SearchResponseParts.Meta meta;
    private List<RTBObject> results;
//    private SearchResponseParts.Result results;
    private SearchResponseParts.FacetCounts facets;
	
    private List<String> expand = new ArrayList<String>();
	
	public SearchResponse () {}
	
	public SearchResponse (DiscoverResult queryResults, String expand) {
		this();
		
		disableExpand();
		
		if (queryResults != null) {
			numFound = queryResults.getTotalSearchResults();			
		}
				
        List<String> expandFields = new ArrayList<String>();
		if (expand != null) {
			expandFields = Arrays.asList(expand.split(","));
		}

        if(expandFields.contains("meta") || expandFields.contains("all")) {
        	setMeta(new SearchResponseParts.Meta());
     	} else {
     		this.addExpand("meta");
     	}

        
        if(expandFields.contains("responseHeader") || expandFields.contains("all")) {
        	setResponseHeader(new SearchResponseParts.ResponseHeader());
        } else {
            this.addExpand("responseHeader");
        }

        if(expandFields.contains("facets") || expandFields.contains("all")) {
        	setFacets(new SearchResponseParts.FacetCounts(queryResults));
        } else {
            this.addExpand("facets");
        }
        
	}
		
	public void addExpand(String expandableAttribute) {
    	if (this.expand != null) {
    		this.expand.add(expandableAttribute);
    	}
    }

    public void enableExpand() {
    	if (this.expand == null) {
    		expand = new ArrayList<String>();
    	}
    }

    public void disableExpand() {
    	this.expand = null;    	
    }
	
    public List<String> getExpand() {
        return expand;
    }

    @XmlAttribute
    public Long getNumFound() {
		return numFound;
	}

	public void setNumFound(Long numFound) {
		this.numFound = numFound;
	}

	public SearchResponseParts.ResponseHeader getResponseHeader() {
		return responseHeader;
	}

	public void setResponseHeader(SearchResponseParts.ResponseHeader responseHeader) {
		this.responseHeader = responseHeader;
	}

	public SearchResponseParts.FacetCounts getFacets() {
		return facets;
	}

	public void setFacets(SearchResponseParts.FacetCounts facets) {
		this.facets = facets;
	}

	@XmlElementWrapper( name = "results" )
	@XmlElement( name = "result" )
	public List<RTBObject> getResults() {
		return results;
	}

	public void setResults(List<RTBObject> results) {
		this.results = results;
	}

	public SearchResponseParts.Meta getMeta() {
		return meta;
	}

	public void setMeta(SearchResponseParts.Meta meta) {
		this.meta = meta;
	}

}
