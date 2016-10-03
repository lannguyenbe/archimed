package org.dspace.rtbf.rest.common;

import java.util.HashMap;
import java.util.Map;

import org.dspace.discovery.DiscoverQuery.SORT_ORDER;

public class Constants extends org.dspace.core.Constants {
    public static final String WEBAPP_NAME = "rtbf-rest";

    // Config sections in the config file rtbf-rest.cfg
    public static final String SORTMETA = "sortMeta";
    public static final String NAMINGMETA = "namingMeta";
    public static final String FILTERMETA = "filterMeta";

    // Use in class RTBObject
    // Type of view determines the choice of metadata to show 
    public static final int EXPANDELEM_VIEW = 1;
    public static final int MIN_VIEW = 2;
    public static final int SEARCH_RESULT_VIEW = 3;
    // public static final int STANDARD_VIEW = 4; // not use
    public static final int PLAYLIST_VIEW = 5;

    // Expandable elements
    public static final String SERIE_EXPAND_OPTIONS = "owningSerie,metadata";
    public static final String EPISODE_EXPAND_OPTIONS = "owningSerie,metadata,diffusions,supports";
    public static final String OWNING_SERIE_EXPAND_OPTIONS = "metadata";
    /*
     * Lan 29.07.2016 : remove diffusions,supports
     * public static final String OWNING_EPISODE_EXPAND_OPTIONS = "metadata,diffusions,supports";
     */
    public static final String OWNING_EPISODE_EXPAND_OPTIONS = "metadata";
    /*
     * Lan 29.07.2016 : remove parentEpisodeList
     * public static final String SEQUENCE_EXPAND_OPTIONS = "owningSerie,owningEpisode,parentEpisodeList,metadata,diffusions,supports";
     */
    public static final String SEQUENCE_EXPAND_OPTIONS = "owningSerie,owningEpisode,metadata,diffusions,supports,linkedDocuments";
    /* 30.09.2016 Lan : comment below because too slow as linkedDocuments expand to a subquery for each result.
     * public static final String SEARCH_RESULT_EXPAND_OPTIONS = "owningParentList,linkedDocuments";
     */ 
    public static final String SEARCH_RESULT_EXPAND_OPTIONS = "owningParentList";

    // Default values
    public static final String[] TYPETEXT = { "none", "none", "SEQUENCE", "EPISODE", "SERIE", "none", "none", "none" };
    public static final int      LIMITMAX = 5000;
    /* 30.09.2016 Lan : scale down DEFAULT_LIMIT because 100 is too slow
     * public static final String   DEFAULT_LIMIT = "100";
     */
    public static final String   DEFAULT_LIMIT = "20";
    public static final int      DEFAULT_RPP = Integer.valueOf(DEFAULT_LIMIT);
    public static final String   DEFAULT_ORDER = "asc";
    public static final int      DEFAULT_FACET_RPP = 10;
    public static final int      DEFAULT_FACET_OFFSET = 0;
    /* 30.09.2016 Lan : scale down 
     * public static final int      DEFAULT_LOV_RPP = 100;
     */
    public static final int      DEFAULT_LOV_RPP = 10;
    public static final String   LOV_ALL = "\\n"; // the literal \n not the LF
    
    // Keys for accessing metadata
    public static final String ATTRIBUTOR = "rtbf.identifier.attributor";
    public static final String ROYALTY = "rtbf.royalty_code";
    public static final String ROYALTY_REMARK = "rtbf.royalty_remark";
    public static final String ABSTRACT = "dc.description.abstract";
    public static final String DATE_ISSUED = "dc.date.issued";
    public static final String CHANNEL_ISSUED = "rtbf.channel_issued";
    public static final String CODE_ORIGINE = "rtbf.code_origine.*";
    public static final String TITLE = "dc.title";
    
    
    // Schema to skip
    public static final String OLD_SCHEMA = "old";
    
}
