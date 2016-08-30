package org.dspace.rtbf.rest.util.jackson;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

public class NameMapper {
    // The class for which the mappings need to take place.
    public Class<?> classToFilter;
    // The mappings property names. Key would be the existing property name
    // value would be name you want in the ouput.
    public Properties nameMappings;

    public NameMapper(Class<?> classToFilter, Properties nameMappings) {
        this.classToFilter = classToFilter;
        this.nameMappings = nameMappings;
    }
}