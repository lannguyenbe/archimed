package org.dspace.rtbf.rest.search;

import java.util.List;
import java.util.Map;

import org.dspace.discovery.DiscoverQuery.SORT_ORDER;

public interface Request {
		
	public List<Map<String,String>> getParameterFilterQueries();
	public void setParameterFilterQueries(List<Map<String,String>> fqs);

	public String getScope();
	public void setScope(String scope);

	public String[] getScopes();
	public void setScopes(String[] scope);

	public String getQuery();
	public void setQuery(String query);


	public int getLimit();
	public void setLimit(int limit);

	public int getOffset();
	public void setOffset(int offset);

	public int getPage();
	public void setPage(int offset);


	public String getSortField();
	public String getSortField(String requestHandler);


	public SORT_ORDER getSortOrder();


	public boolean isFacet();
	public void setIsFacet(boolean bool);


	public int getFacetLimit();
	public void setFacetLimit(int facetLimit);


	public int getFacetOffset();
	public void setFacetOffset(int facetOffset);

	public int getFacetPage();
	public void setFacetPage(int facetOffset);

	public boolean isSnippet();
	public void setIsSnippet(boolean bool);


	public boolean isHighlight();
	public void setIsHighlight(boolean bool);

	public boolean isCollapse();
	public void setIsCollapse(boolean bool);
	
	public boolean isExactTerm();
	
	public boolean isSpellCheck();




}
