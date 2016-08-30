/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;

import org.springframework.beans.factory.annotation.Required;

import java.util.ArrayList;
import java.util.List;

public class DiscoverySearchFilterRegex extends DiscoverySearchFilter {

    /* Lan 25.07.2016 : add metadataValues to support expressions */
    protected List<String> metadataValues;
    public static final String FILTER_TYPE_REGEX = "regex";


    public List<String> getMetadataValues() {
    	if (metadataValues == null) {
    		return (new ArrayList<String>());
    	}
        return metadataValues;
    }

    public void setMetadataValues(List<String> metadataValues) {
        this.metadataValues = metadataValues;
    }

    @Override
    public String getFilterType()
    {
        return FILTER_TYPE_REGEX;
    }
}
