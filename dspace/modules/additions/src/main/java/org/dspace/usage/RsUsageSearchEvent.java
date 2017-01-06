/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.usage;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.statistics.ObjectCount;

import javax.servlet.http.HttpServletRequest;

import java.util.List;

public class RsUsageSearchEvent extends org.dspace.usage.UsageSearchEvent{


    public RsUsageSearchEvent(Action action, HttpServletRequest request,
			Context context, DSpaceObject object, List<String> queries,
			DSpaceObject scope) {
		super(action, request, context, object, queries, scope);
		// TODO Auto-generated constructor stub
	}

	/** Optional search parameters **/
    private long numFound;
    private String query_q;

	public long getNumFound() {
		return numFound;
	}


	public void setNumFound(long numFound) {
		this.numFound = numFound;
	}
	

	public String getQuery_q() {
		return query_q;
	}


	public void setQuery_q(String query_q) {
		this.query_q = query_q;
	}
	

	public ObjectCount getObjectCount() {
		ObjectCount obj = new ObjectCount();
		obj.setCount(numFound);
		obj.setValue(query_q);
		return obj;
	}


}
