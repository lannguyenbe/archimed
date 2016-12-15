package org.dspace.rtbf.rest.application;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.dspace.rtbf.rest.*;
import org.dspace.rtbf.rest.lov.LOVAuthors;
import org.dspace.rtbf.rest.lov.LOVChannels;
import org.dspace.rtbf.rest.lov.LOVCodeOrigines;
import org.dspace.rtbf.rest.lov.LOVEvents;
import org.dspace.rtbf.rest.lov.LOVSerieTitles;
import org.dspace.rtbf.rest.lov.LOVPlaces;
import org.dspace.rtbf.rest.lov.LOVProductionTypes;
import org.dspace.rtbf.rest.lov.LOVPublishers;
import org.dspace.rtbf.rest.lov.LOVRoyaltyCodes;
import org.dspace.rtbf.rest.lov.LOVSubjects;

public class RsApplication extends Application {

	@Override
	public Set<Class<?>> getClasses() {
        HashSet<Class<?>> set = new HashSet<Class<?>>(1);
        
        // Moxy registration
        // set.add(org.glassfish.jersey.moxy.json.MoxyJsonFeature.class);
        // set.add(RsMoxyJsonContextResolver.class);
        
        // Jackson2 registration
        set.add(org.glassfish.jersey.jackson.JacksonFeature.class);
        set.add(RsJacksonContextResolver.class);
        

        set.add(RsIndex.class);
        set.add(SearchResource.class);
        set.add(HandleResource.class);
        set.add(SequencesResource.class);
        set.add(EpisodesResource.class);
        set.add(SeriesResource.class);
        // LOV
        set.add(LOVAuthors.class);
        set.add(LOVSubjects.class);
        set.add(LOVPlaces.class);
        set.add(LOVSerieTitles.class);
        set.add(LOVCodeOrigines.class);
        set.add(LOVPublishers.class);
        set.add(LOVProductionTypes.class);
        set.add(LOVRoyaltyCodes.class);
        set.add(LOVEvents.class);
        set.add(LOVChannels.class);
        return set;
	}

	
}
