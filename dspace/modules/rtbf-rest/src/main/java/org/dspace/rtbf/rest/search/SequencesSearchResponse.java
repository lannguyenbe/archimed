package org.dspace.rtbf.rest.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverResult;
import org.dspace.rtbf.rest.search.SearchResponseParts;

@XmlRootElement(name = "sequencesSearchResponse")
public class SequencesSearchResponse extends SearchResponse {

    private static Logger log = Logger.getLogger(SequencesSearchResponse.class);
	
	public SequencesSearchResponse() {}

	public SequencesSearchResponse(DiscoverResult queryResults, String expand, Context context) {
		super(queryResults, expand);
		setup(queryResults, expand, context);
		
	}
	
	private void setup(DiscoverResult queryResults, String expand, Context context) 
	{
        List<String> expandFields = new ArrayList<String>();
		if (expand != null) {
			expandFields = Arrays.asList(expand.split(","));
		}

        if(expandFields.contains("results") || expandFields.contains("all")) {
            if(expandFields.contains("linkedDocuments")) {
            	SearchResponseParts.Result resultsWrapper = new SearchResponseParts.Result(queryResults, "linkedDocuments", context);
            	setResults(resultsWrapper.getLst());
            } else {
            	SearchResponseParts.Result resultsWrapper = new SearchResponseParts.Result(queryResults, context);
            	setResults(resultsWrapper.getLst());            	
            }
        } else {
            this.addExpand("results");
        }

	}

}
