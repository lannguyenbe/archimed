package org.dspace.rtbf.rest.lov;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.dspace.discovery.DiscoverQuery.SORT_ORDER;
import org.dspace.rtbf.rest.common.Constants;

public class LOVParameters implements org.dspace.rtbf.rest.search.Request{
	
	private String scope = null;
	private int limit = -1;
	private int offset = 0;
	
	public LOVParameters() {}
	
	public LOVParameters(MultivaluedMap<String, String> mvm) {

		String str;
		
		if ((str = mvm.getFirst("scope")) != null && str.length() > 0) { this.scope = str;}
		if ((str = mvm.getFirst("limit")) != null && str.length() > 0) { this.limit = Integer.parseInt(str);}
		if ((str = mvm.getFirst("offset")) != null && str.length() > 0) { this.offset = Integer.parseInt(str);}
	}

	@Override
	public List<Map<String, String>> getParameterFilterQueries() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setParameterFilterQueries(List<Map<String, String>> fqs) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getScope() {
		return scope;
	}

	@Override
	public void setScope(String scope) {
		this.scope = scope;
	}

	@Override
	public String[] getScopes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setScopes(String[] scope) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getQuery() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setQuery(String query) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getLimit() {
		return limit;
	}

	@Override
	public void setLimit(int limit) {
		this.limit = limit;
	}

	@Override
	public int getOffset() {
		return offset;
	}

	@Override
	public void setOffset(int offset) {
		this.offset = offset;
	}

	@Override
	public String getSortField() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SORT_ORDER getSortOrder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isFacet() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setIsFacet(boolean bool) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getFacetLimit() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setFacetLimit(int facetLimit) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getFacetOffset() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setFacetOffset(int facetOffset) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isSnippet() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setIsSnippet(boolean bool) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isHighlight() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setIsHighlight(boolean bool) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isCollapse() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setIsCollapse(boolean bool) {
		// TODO Auto-generated method stub
		
	}

}
