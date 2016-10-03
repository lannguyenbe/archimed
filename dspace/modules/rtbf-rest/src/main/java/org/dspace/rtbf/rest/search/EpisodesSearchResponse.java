package org.dspace.rtbf.rest.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverResult;
import org.dspace.rtbf.rest.search.SearchResponseParts;

/*
 * 18.12.2015 Lan : actuellement episodesSearchResponse = sequencesSearchResponse 
 * mais pourrait different dans le futur
 * 
 */

@XmlRootElement(name = "episodesSearchResponse")
public class EpisodesSearchResponse extends SearchResponse {

    private static Logger log = Logger.getLogger(EpisodesSearchResponse.class);
	
	public EpisodesSearchResponse() {}

	public EpisodesSearchResponse(DiscoverResult queryResults, String expand, Context context, Integer limit, Integer offset) {
		super(queryResults, expand);
		setup(queryResults, expand, context, limit, offset);
		
	}
	
	private void setup(DiscoverResult queryResults, String expand,
			Context context, Integer limit, Integer offset) 
	{
        List<String> expandFields = new ArrayList<String>();
		if (expand != null) {
			expandFields = Arrays.asList(expand.split(","));
		}

        if(expandFields.contains("results") || expandFields.contains("all")) {
        	SearchResponseParts.Result resultsWrapper = new SearchResponseParts.Result(queryResults, context);
        	setResults(resultsWrapper.getLst());
        } else {
            this.addExpand("results");
        }
		
	}

}
