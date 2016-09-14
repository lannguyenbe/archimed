package org.dspace.rtbf.rest.search;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.annotation.XmlRootElement;

import org.dspace.discovery.DiscoverQuery.SORT_ORDER;
import org.dspace.rtbf.rest.common.Constants;
import org.dspace.rtbf.rest.common.MetadataEntry;

import com.fasterxml.jackson.annotation.JsonProperty;

@XmlRootElement(name = "searchparameters")
public class SearchParameters implements Request {

	private String scope;
	private String[] scopes;
	private String q;
	private int limit = Constants.DEFAULT_RPP;
	private int offset = 0;
	@JsonProperty("sort_by")
	private String sortBy;
	private String order = Constants.DEFAULT_ORDER;
	@JsonProperty("facet")
	private boolean isFacet = false;
	@JsonProperty("facet_limit")
	private int facetLimit = Constants.DEFAULT_FACET_RPP;
	@JsonProperty("facet_offset")
	private int facetOffset = Constants.DEFAULT_FACET_OFFSET;
	@JsonProperty("highlight")
	private boolean isHighlight = true;
	@JsonProperty("snippet")
	private boolean isSnippet = false;
	private String expand;
	
	private List<Map<String, String>> filters = new ArrayList<Map<String, String>> ();

	public String getScope() {
		if (scope != null) {
			return scope;
		} else if (scopes != null && scopes.length > 0) {
			StringBuffer sb = new StringBuffer();;
			for (String str : scopes) {
				sb.append(str);
				sb.append(' ');
			}
			return(sb.toString());
		}
		return(null);
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public String[] getScopes() {
		return scopes;
	}

	public void setScopes(String[] scopes) {
		this.scopes = scopes;
	}

	public String getQ() {
		return q;
	}

	public void setQ(String q) {
		this.q = q;
	}

	public int getLimit() {
		if (limit < 0) {
			return Constants.DEFAULT_RPP; 
		}
		return limit;
	}
	
	public void setLimit(int limit) {
		this.limit = limit;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public String getSortBy() {
		return sortBy;
	}

	public void setSortBy(String sortBy) {
		this.sortBy = sortBy;
	}

	public String getOrder() {
		return order;
	}

	public void setOrder(String order) {
		this.order = order;
	}

	public boolean getIsFacet() {
		return isFacet;
	}

	public void setIsFacet(boolean isFacet) {
		this.isFacet = isFacet;
	}

	public int getFacetLimit() {
		return facetLimit;
	}

	public void setFacetLimit(int facetLimit) {
		this.facetLimit = facetLimit;
	}

	public int getFacetOffset() {
		return facetOffset;
	}

	public void setFacetOffset(int facetOffset) {
		this.facetOffset = facetOffset;
	}

	public boolean getIsHighlight() {
		return isHighlight;
	}

	public void setIsHighlight(boolean isHighlight) {
		this.isHighlight = isHighlight;
	}

	public boolean getIsSnippet() {
		return isSnippet;
	}

	public void setIsSnippet(boolean isSnippet) {
		this.isSnippet = isSnippet;
	}

	public String getExpand() {
		return expand;
	}

	public void setExpand(String expand) {
		this.expand = expand;
	}

	public List<Map<String, String>> getFilters() {
		return filters;
	}

	public void setFilters(List<Map<String, String>> filters) {
		this.filters = filters;
	}

	
	public SearchParameters supersedeBy(MultivaluedMap<String, String> mvm) {
		
		String str;
		
		if ((str = mvm.getFirst("scope")) != null && str.length() > 0) { this.scope = str;}
		if ((str = mvm.getFirst("q")) != null && str.length() > 0) { this.q = str;}
		if ((str = mvm.getFirst("limit")) != null && str.length() > 0) { this.limit = Integer.parseInt(str);}
		if ((str = mvm.getFirst("offset")) != null && str.length() > 0) { this.offset = Integer.parseInt(str);}
		if ((str = mvm.getFirst("sort_by")) != null && str.length() > 0) { this.sortBy = str;}
		if ((str = mvm.getFirst("order")) != null && str.length() > 0) { this.order = str;}
		if ((str = mvm.getFirst("facet")) != null && str.length() > 0) { this.isFacet = Boolean.parseBoolean(str);}
		if ((str = mvm.getFirst("facet_limit")) != null && str.length() > 0) { this.facetLimit = Integer.parseInt(str);}
		if ((str = mvm.getFirst("facet_offset")) != null && str.length() > 0) { this.facetOffset = Integer.parseInt(str);}
		if ((str = mvm.getFirst("highlight")) != null && str.length() > 0) { this.isHighlight = Boolean.parseBoolean(str);}
		if ((str = mvm.getFirst("snippet")) != null && str.length() > 0) { this.isSnippet = Boolean.parseBoolean(str);}
		if ((str = mvm.getFirst("expand")) != null && str.length() > 0) { this.expand = str;}
		

		List<Map<String, String>> fqs = new ArrayList<Map<String, String>> ();
		
        List<String> filterTypes = getRepeatableParameters(mvm, "filtertype");
        List<String> filterOperators = getRepeatableParameters(mvm, "filter_relational_operator");
        List<String> filterValues = getRepeatableParameters(mvm, "filter");
        
        for (int i = 0, len = filterTypes.size(); i < len; i++) {
        	String filterType = filterTypes.get(i);
            String filterValue = filterValues.get(i);
            String filterOperator = filterOperators.get(i);

            Map<String, String> fq = new Hashtable<String, String>();
            fq.put("filtertype", new String(filterType));
            fq.put("filter_relational_operator", new String(filterOperator));
            fq.put("filter", new String(filterValue));
            
            fqs.add(i, fq);
        }
        if (!fqs.isEmpty()) {
        	this.filters = fqs;
        }
		
		return this;		
	}

	
	protected List<String> getRepeatableParameters(MultivaluedMap<String, String> params, String prefix) {
        TreeMap<String, String> result = new TreeMap<String, String>();
         
        for (String key : params.keySet()) {
        	if (key.startsWith(prefix)) {
        		result.put(key, params.getFirst(key));
        	}
        }
        return new ArrayList<String>(result.values());		
	}

	/* 
	 * implements Request
	 */
	@Override
	public List<Map<String, String>> getParameterFilterQueries() {
		return getFilters();
	}

	@Override
	public void setParameterFilterQueries(List<Map<String, String>> fqs) {
		setFilters(fqs);
		
	}

	@Override
	public String getQuery() {
		return getQ();
	}

	@Override
	public void setQuery(String query) {
		setQ(query);
		
	}

	@Override
	public String getSortField() {
		String sortField = getSortBy();
		
		if (sortField != null) { 
			String sf = MetadataEntry.getSortLabel(sortField);
			if (sf != null) { sortField = sf; }
		}

		return sortField;
	}

	@Override
	public SORT_ORDER getSortOrder() {
		return SORT_ORDER.valueOf(getOrder());
	}

	@Override
	public boolean isFacet() {
		return getIsFacet();
	}

	@Override
	public boolean isSnippet() {
		return getIsSnippet();
	}

	@Override
	public boolean isHighlight() {
		return getIsHighlight();
	}
}
