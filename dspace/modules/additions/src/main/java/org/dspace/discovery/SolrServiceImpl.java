/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.dspace.util.MultiFormatDateParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.Group;
import org.apache.solr.client.solrj.response.GroupCommand;
import org.apache.solr.client.solrj.response.PivotField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SpellCheckResponse.Collation;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.*;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.extraction.ExtractingParams;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.CodeOrigine;
import org.dspace.content.Collection;
import org.dspace.content.CollectionAdd;
import org.dspace.content.CollectionIterator;
import org.dspace.content.Community;
import org.dspace.content.CommunityAdd;
import org.dspace.content.CommunityIterator;
import org.dspace.content.HandleLog;
import org.dspace.content.HandleLogIterator;
import org.dspace.content.ItemAdd;
import org.dspace.content.Metadatum;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.ItemAdd.DiffusionItem;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.discovery.DiscoverResult.GroupFilter;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.discovery.configuration.DiscoveryHitHighlightFieldConfiguration;
import org.dspace.discovery.configuration.DiscoveryHitHighlightingConfiguration;
import org.dspace.discovery.configuration.DiscoveryMoreLikeThisConfiguration;
import org.dspace.discovery.configuration.DiscoveryRecentSubmissionsConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.discovery.configuration.DiscoverySearchFilterRegex;
import org.dspace.discovery.configuration.DiscoverySortConfiguration;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.dspace.discovery.configuration.HierarchicalSidebarFacetConfiguration;
import org.dspace.handle.HandleManager;
import org.dspace.sort.OrderFormat;
import org.dspace.storage.rdbms.DatabaseUtils;
import org.dspace.utils.DSpace;
import org.springframework.stereotype.Service;

/**
 * SolrIndexer contains the methods that index Items and their metadata,
 * collections, communities, etc. It is meant to either be invoked from the
 * command line (see dspace/bin/index-all) or via the indexContent() methods
 * within DSpace.
 * <p/>
 * The Administrator can choose to run SolrIndexer in a cron that repeats
 * regularly, a failed attempt to index from the UI will be "caught" up on in
 * that cron.
 *
 * The SolrServiceImpl is registered as a Service in the ServiceManager via
 * a spring configuration file located under
 * classpath://spring/spring-dspace-applicationContext.xml
 *
 * Its configuration is Autowired by the ApplicationContext
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
@Service
public class SolrServiceImpl implements SearchService, IndexingService {

    private static final Logger log = Logger.getLogger(SolrServiceImpl.class);

    protected static final String LAST_INDEXED_FIELD = "SolrIndexer.lastIndexed";

    public static final String FILTER_SEPARATOR = "\n|||\n";

    public static final String AUTHORITY_SEPARATOR = "###";

    public static final String STORE_SEPARATOR = "\n|||\n";

    public static final String VARIANTS_STORE_SEPARATOR = "###";

    /**
     * Non-Static CommonsHttpSolrServer for processing indexing events.
     */
    private HttpSolrServer solr = null;


    protected HttpSolrServer getSolr()
    {
        if ( solr == null)
        {
            String solrService = new DSpace().getConfigurationService().getProperty("discovery.search.server");

            UrlValidator urlValidator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);
            if (urlValidator.isValid(solrService)||ConfigurationManager.getBooleanProperty("discovery","solr.url.validation.enabled",true))
            {
                try {
                    log.debug("Solr URL: " + solrService);
                    solr = new HttpSolrServer(solrService);

                    solr.setBaseURL(solrService);
                    solr.setUseMultiPartPost(true);
                    SolrQuery solrQuery = new SolrQuery()
                            .setQuery("search.resourcetype:2 AND search.resourceid:1");

                    solr.query(solrQuery);

                    // As long as Solr initialized, check with DatabaseUtils to see
                    // if a reindex is in order. If so, reindex everything
                    DatabaseUtils.checkReindexDiscovery(this);
                } catch (SolrServerException e) {
                    log.error("Error while initializing solr server", e);
                }
            }
            else
            {
                log.error("Error while initializing solr, invalid url: " + solrService);
            }
        }

        return solr;
    }

    /**
     * If the handle for the "dso" already exists in the index, and the "dso"
     * has a lastModified timestamp that is newer than the document in the index
     * then it is updated, otherwise a new document is added.
     *
     * @param context Users Context
     * @param dso     DSpace Object (Item, Collection or Community
     * @throws SQLException
     * @throws IOException
     */
    @Override
    public void indexContent(Context context, DSpaceObject dso)
            throws SQLException {
        indexContent(context, dso, false);
    }

    /**
     * If the handle for the "dso" already exists in the index, and the "dso"
     * has a lastModified timestamp that is newer than the document in the index
     * then it is updated, otherwise a new document is added.
     *
     * @param context Users Context
     * @param dso     DSpace Object (Item, Collection or Community
     * @param force   Force update even if not stale.
     * @throws SQLException
     * @throws IOException
     * 
     * 01.08.2016 Lan : ajouter la duplication des items dans l'index
     */
    @Override
    public void indexContent(Context context, DSpaceObject dso,
                             boolean force) throws SQLException {

        String handle = dso.getHandle();

        if (handle == null)
        {
            handle = HandleManager.findHandle(context, dso);
        }

        try {
            switch (dso.getType())
            {
                case Constants.ITEM:
                    Item item = (Item) dso;
                    if (item.isArchived() || item.isWithdrawn())
                    {
                        /**
                         * If the item is in the repository now, add it to the index
                         */
                    	// Lan 06.04.2016 - force at the left of || for optimization purpose
                        if (force || requiresIndexing(handle, ((Item) dso).getLastModified()))
                        {
                            unIndexContent(context, handle);
                            
                            ItemAdd.DiffusionItem[] dItems = ItemAdd.DiffusionItem.findDupById(context, item.getID());
                            // duplicate the item as many as diffusion_path
                            if (dItems.length == 0) { // there is no dup
                                buildDocument(context, (Item) dso);                           	
                            } else {
                            	ItemAdd.ItemDup defaultItemDup = ItemAdd.duplicate(item, null);
                                buildDocument(context, defaultItemDup);
                                for (int i = 0, len = dItems.length; i < len; i++) {
                                	ItemAdd.ItemDup itemDup = ItemAdd.duplicate(item, dItems[i]);
                                    buildDocument(context, itemDup);
    							}
                            }

                        }
                    } else {
                        /**
                         * Make sure the item is not in the index if it is not in
                         * archive or withwrawn.
                         */
                        unIndexContent(context, item);
                        log.info("Removed Item: " + handle + " from Index");
                    }
                    break;

                case Constants.COLLECTION:
                    buildDocument(context, (Collection) dso);
                    log.info("Wrote Collection: " + handle + " to Index");
                    break;

                case Constants.COMMUNITY:
                    buildDocument(context, (Community) dso);
                    log.info("Wrote Community: " + handle + " to Index");
                    break;

                default:
                    log
                            .error("Only Items, Collections and Communities can be Indexed");
            }

        } catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * unIndex removes an Item, Collection, or Community
     *
     * @param context
     * @param dso     DSpace Object, can be Community, Item, or Collection
     * @throws SQLException
     * @throws IOException
     */
    @Override
    public void unIndexContent(Context context, DSpaceObject dso)
            throws SQLException, IOException {
        unIndexContent(context, dso, false);
    }

    /**
     * unIndex removes an Item, Collection, or Community
     *
     * @param context
     * @param dso     DSpace Object, can be Community, Item, or Collection
     * @param commit if <code>true</code> force an immediate commit on SOLR
     * @throws SQLException
     * @throws IOException
     */
    @Override
    public void unIndexContent(Context context, DSpaceObject dso, boolean commit)
            throws SQLException, IOException {
        try {
            if (dso == null)
            {
                return;
            }
            String uniqueID = dso.getType()+"-"+dso.getID();
            getSolr().deleteById(uniqueID);
            if(commit)
            {
                getSolr().commit();
            }
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            emailException(exception);
        }
    }

    /**
     * Unindex a Document in the Lucene index.
     * @param context the dspace context
     * @param handle the handle of the object to be deleted
     * @throws IOException
     * @throws SQLException
     */
    @Override
    public void unIndexContent(Context context, String handle) throws IOException, SQLException {
        unIndexContent(context, handle, false);
    }

    public void unIndexContentR(Context context, String handle) throws IOException, SQLException {
    	unIndexContentR(context, handle, true);
    }
    

    /**
     * Lan 14.09.2015 
     * Unindex recursively documents from the index, the corresponding dso of the given handle should exist in the database,
     * the main usage is to delete all documents of a top community from the index BEFORE deleting data from the DB
     * for item = same result as calling unIndexContent(context, handle) without R
     * for collection = unindex all the items having this collection as parent; then unindex the collection
     * Careful !!! even if the item belongs to multiple collections, the item is unindexed anyway
     * for community = unindex all the items, collections, communities having this community as parent; then unindex the community
     */
    public void unIndexContentR(Context context, String handle, boolean commit) throws IOException, SQLException {
    	
    	DSpaceObject dso = HandleManager.resolveToObject(context, handle);
    	
    	if (dso == null) {
    		return;
    	}
    	
    	try {
    		if (getSolr() != null) {
    			int resourceID = dso.getID();
                String uniqueID = dso.getType()+"-"+dso.getID();
	    		switch (dso.getType()) {
		    		case Constants.ITEM:
		                getSolr().deleteById(uniqueID);
		    			break;
		    		case Constants.COLLECTION:
		                getSolr().deleteByQuery("location.coll:" + resourceID);
		                getSolr().deleteById(uniqueID);
		    			break;
		    		case Constants.COMMUNITY:
		    			// unindex items and collections
		                getSolr().deleteByQuery("location.comm:" + resourceID);
		                
		                // unindex sub-communitites -- consult the database because community document don't have location.comm value
		                CommunityIterator subCommunities = null;
		                try {
		                	for (subCommunities = CommunityAdd.findSubcommunities(context, resourceID); subCommunities.hasNext();) {
		                        Community subComm = subCommunities.next();
		                        String subCommID = String.valueOf(Constants.COMMUNITY)+"-"+subComm.getID();
				                getSolr().deleteById(subCommID);
		                	}
		                	
		                } finally {
		                	if (subCommunities != null) {
		                		subCommunities.close();
		                	}
		                }
		                
		                // unindex the given community
		                getSolr().deleteById(uniqueID);
		    			break;
		    		default:
		    			log.error("Only Items, Collections and Communities can be Indexed");
	    		}
                if(commit)
                {
                    getSolr().commit();
                }
    		}    		
    	} catch (SolrServerException e) {
    		log.error(e.getMessage(),e);
    	}
    }
    
    
    
    /**
     * Unindex a Document in the Lucene Index.
     * @param context the dspace context
     * @param handle the handle of the object to be deleted
     * @throws SQLException
     * @throws IOException
     */
    @Override
    public void unIndexContent(Context context, String handle, boolean commit)
            throws SQLException, IOException {

        try {
            if(getSolr() != null){
                getSolr().deleteByQuery("handle:\"" + handle + "\"");
                if(commit)
                {
                    getSolr().commit();
                }
            }
        } catch (SolrServerException e)
        {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * reIndexContent removes something from the index, then re-indexes it
     *
     * @param context context object
     * @param dso     object to re-index
     */
    @Override
    public void reIndexContent(Context context, DSpaceObject dso)
            throws SQLException, IOException {
        try {
            indexContent(context, dso);
        } catch (Exception exception)
        {
            log.error(exception.getMessage(), exception);
            emailException(exception);
        }
    }

    /**
     * create full index - wiping old index
     *
     * @param c context to use
     */
    @Override
    public void createIndex(Context c) throws SQLException, IOException {

        /* Reindex all content preemptively. */
        updateIndex(c, true);

    }


    /**
     * Iterates over all Items, Collections and Communities. And updates them in
     * the index. Uses decaching to control memory footprint. Uses indexContent
     * and isStale to check state of item in index.
     *
     * @param context the dspace context
     */
    @Override
    public void updateIndex(Context context)
    {
        updateIndex(context, false);
    }

    /**
     * Iterates over all Items, Collections and Communities. And updates them in
     * the index. Uses decaching to control memory footprint. Uses indexContent
     * and isStale to check state of item in index.
     * <p/>
     * At first it may appear counterintuitive to have an IndexWriter/Reader
     * opened and closed on each DSO. But this allows the UI processes to step
     * in and attain a lock and write to the index even if other processes/jvms
     * are running a reindex.
     *
     * @param context the dspace context
     * @param force whether or not to force the reindexing
     */
    @Override
    public void updateIndex(Context context, boolean force)
    {
        try {
            ItemIterator items = null;
            try {
                for (items = Item.findAllUnfiltered(context); items.hasNext();)
                {
                    Item item = items.next();
                    indexContent(context, item, force);
                    item.decache();
                }
            } finally {
                if (items != null)
                {
                    items.close();
                }
            }

            Collection[] collections = Collection.findAll(context);
            for (Collection collection : collections)
            {
                indexContent(context, collection, force);
                context.removeCached(collection, collection.getID());

            }

            Community[] communities = Community.findAll(context);
            for (Community community : communities)
            {
                indexContent(context, community, force);
                context.removeCached(community, community.getID());
            }

            if(getSolr() != null)
            {
                getSolr().commit();
            }

        } catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
    }
    
    /**
     * Lan add update options
     */
    public void updateIndexBig(Context context, boolean force)
    {
        try {
            CommunityIterator communities = null;
            try {
                for (communities = CommunityAdd.findAllCursor(context); communities.hasNext();)
                {
                    Community community = communities.next();
                    indexContent(context, community, force);
                    context.removeCached(community, community.getID());
                }
            } finally {
                if (communities != null)
                {
                    communities.close();
                }
            }

            CollectionIterator collections = null;
            try {
                for (collections = CollectionAdd.findAllCursor(context); collections.hasNext();)
                {
                    Collection collection = collections.next();
                    indexContent(context, collection, force);
                    context.removeCached(collection, collection.getID());
                }
            } finally {
                if (collections != null)
                {
                    collections.close();
                }
            }
            
            ItemIterator items = null;
            try {
                for (items = Item.findAllUnfiltered(context); items.hasNext();)
                {
                    Item item = items.next();
                    indexContent(context, item, force);
                    item.decache();
                }
            } finally {
                if (items != null)
                {
                    items.close();
                }
            }

            if(getSolr() != null)
            {
                getSolr().commit();
            }

        } catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
    }

    /*
     * Index items greater than <id>
     */
    public void updateIndexI(Context contextRO, int id, boolean force)
    {
        try {
            ItemIterator items = null;
            try {
                for (items = ItemAdd.findGeId(contextRO, id); items.hasNext();)
                {
                    Item item = items.next();
                    indexContent(contextRO, item, force);
                }
            } finally {
                if (items != null)
                {
                    items.close();
                }
            }

            if(getSolr() != null)
            {
                getSolr().commit();
            }

        } catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
    }
    
    /*
     * Index items greater than <itemId> for the given community <commId>
     */
    public void updateIndexCI(Context contextRO, int commId, int itemId, boolean force)
    {
        try {
            ItemIterator items = null;
            try {
                for (items = ItemAdd.findGeIdByCommunity(contextRO, commId, itemId); items.hasNext();)
                {
                    Item item = items.next();
                    indexContent(contextRO, item, force);
                }
            } finally {
                if (items != null)
                {
                    items.close();
                }
            }

            if(getSolr() != null)
            {
                getSolr().commit();
            }

        } catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
    }
    

    /*
     * Index items greater than <itemId> for the given community <commId>
     */
    public void updateIndexIto(Context contextRO, int id, int idto, boolean force)
    {
        try {
            ItemIterator items = null;
            try {
                for (items = ItemAdd.findBetweenId(contextRO, id, idto); items.hasNext();)
                {
                    Item item = items.next();
                    indexContent(contextRO, item, force);
                }
            } finally {
                if (items != null)
                {
                    items.close();
                }
            }

            if(getSolr() != null)
            {
                getSolr().commit();
            }

        } catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
    }
    
    /*
     * Index the items between <itemId> and <itemIdto> for the given community <commId>
     */
    public void updateIndexCIto(Context contextRO, int commId, int itemId, int itemIdto, boolean force)
    {
        try {
            ItemIterator items = null;
            try {
                for (items = ItemAdd.findBetweenIdByCommunity(contextRO, commId, itemId, itemIdto); items.hasNext();)
                {
                    Item item = items.next();
                    indexContent(contextRO, item, force);
                }
            } finally {
                if (items != null)
                {
                    items.close();
                }
            }

            if(getSolr() != null)
            {
                getSolr().commit();
            }

        } catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
    }
    
    /*
     * Index all the communitites and collections (not items)
     */
    public void updateIndexCC(Context contextRO, boolean force)
    {
        try {
            CommunityIterator communities = null;
            try {
                for (communities = CommunityAdd.findAllCursor(contextRO); communities.hasNext();)
                {
                    Community community = communities.next();
                    indexContent(contextRO, community, force);
                }
            } finally {
                if (communities != null)
                {
                    communities.close();
                }
            }

            CollectionIterator collections = null;
            try {
                for (collections = CollectionAdd.findAllCursor(contextRO); collections.hasNext();)
                {
                    Collection collection = collections.next();
                    indexContent(contextRO, collection, force);
                }
            } finally {
                if (collections != null)
                {
                    collections.close();
                }
            }
            
            if(getSolr() != null)
            {
                getSolr().commit();
            }

        } catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
    }

    /*
     * Index the community and all its collections (not items)
     */
    public void updateIndexCC(Context contextRO, int commId, boolean force)
    {
        try {
            CommunityIterator communities = null;
            try {
                for (communities = CommunityAdd.findSubcommunities(contextRO, commId); communities.hasNext();)
                {
                    Community community = communities.next();
                    indexContent(contextRO, community, force);
                    
                }
            } finally {
                if (communities != null)
                {
                    communities.close();
                }
            }

            CollectionIterator collections = null;
            try {
                for (collections = CollectionAdd.findAllByCommunity(contextRO, commId); collections.hasNext();)
                {
                    Collection collection = collections.next();
                    indexContent(contextRO, collection, force);
                }
            } finally {
                if (collections != null)
                {
                    collections.close();
                }
            }

            if(getSolr() != null)
            {
                getSolr().commit();
            }

        } catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
    }
    
    /*
     * Index all communitites
     */
    public void updateIndexCM(Context contextRO, boolean force)
    {
        try {
            CommunityIterator communities = null;
            try {
                for (communities = CommunityAdd.findAllCursor(contextRO); communities.hasNext();)
                {
                    Community community = communities.next();
                    indexContent(contextRO, community, force);
                }
            } finally {
                if (communities != null)
                {
                    communities.close();
                }
            }

            if(getSolr() != null)
            {
                getSolr().commit();
            }

        } catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
    }

    /*
     * Index collections between <id> and <idto>
     */
    public void updateIndexCL(Context contextRO, int id, boolean force) {
    	updateIndexCLto(contextRO, id, -1, force);
    }

    public void updateIndexCLto(Context contextRO, int id, int idto, boolean force)
    {
        try {
            CollectionIterator collections = null;
            try {
            	if (idto < 0) {
            		collections = CollectionAdd.findGeId(contextRO, id);
            	} else {
            		collections = CollectionAdd.findBetweenId(contextRO, id, idto);
            	}
                while(collections.hasNext())
                {
                    Collection collection = collections.next();
                    indexContent(contextRO, collection, force);
                }
            } finally {
                if (collections != null)
                {
                    collections.close();
                }
            }

            if(getSolr() != null)
            {
                getSolr().commit();
            }

        } catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
    }

    /*
     * Index t_handle_log - synchronization
     */
    public void updateIndexS(Context contextRO, boolean commit)
    {
        try {
            HandleLogIterator hlogs = null;
            int numlogs = 0;

            // 1. Sync DEL on item, collection, community in this order
            try {
                for (hlogs = HandleLog.findAllDel(contextRO), numlogs = 0; hlogs.hasNext(); numlogs++)
                {
                    HandleLog handleLog = hlogs.next();
                    
                    String handle = handleLog.getHandle();
                    // 11.04.2016 Lan : because of duplication of items on their diffusions
                    // Multiple documents may correspond to the handle (same handle, different search.uniqueid)
                    getSolr().deleteByQuery("handle:\"" + handle + "\"");

                    handleLog.populateLogDone();
                    log.info("Sync delete handle: " + handle + " from Index");
                    
                }
                System.out.println(numlogs + " DEL");
                if (commit) { getSolr().commit(); }
            } finally {
                if (hlogs != null) { 
                	hlogs.close(); 
                	hlogs = null; 
                }
            }
    
            // 2. Sync INS/UPD on community
            try {
            	Community community;
                for (hlogs = HandleLog.findCommunities2Sync(contextRO), numlogs = 0; hlogs.hasNext(); numlogs++)
                {
                    HandleLog handleLog = hlogs.next();
                    
                    community = (Community) Community.find(contextRO, handleLog.getType(), handleLog.getID());
                    if (community != null) {
//                        System.out.println(handleLog.getOper() + ", " + handleLog.getType() + "-" + handleLog.getID() + ", " + handleLog.getHandleID());
                        indexContent(contextRO, community, true); // force is true 
                        handleLog.populateLogDone();
                    }
                }
                System.out.println(numlogs + " INS/UPD on Community");
                if (commit) { getSolr().commit(); }
            } finally {
                if (hlogs != null) { 
                	hlogs.close(); 
                	hlogs = null; 
                }
            }

            // 3. Sync INS/UPD on collection
            try {
            	Collection collection;
                for (hlogs = HandleLog.findCollections2Sync(contextRO), numlogs = 0; hlogs.hasNext(); numlogs++)
                {
                    HandleLog handleLog = hlogs.next();
                    
                    collection = (Collection) Collection.find(contextRO, handleLog.getType(), handleLog.getID());
                    if (collection != null ) {
//                        System.out.println(handleLog.getOper() + ", " + handleLog.getType() + "-" + handleLog.getID() + ", " + handleLog.getHandleID());
                        indexContent(contextRO, collection, true); // force is true
                        handleLog.populateLogDone();
                    }
                }
                System.out.println(numlogs + " INS/UPD on Collection");
                if (commit) { getSolr().commit(); }
            } finally {
                if (hlogs != null) { 
                	hlogs.close(); 
                	hlogs = null; 
                }
            }

            // 4. Sync INS/UPD on item
            try {
            	Item item;
                for (hlogs = HandleLog.findItems2Sync(contextRO), numlogs = 0; hlogs.hasNext(); numlogs++)
                {
                    HandleLog handleLog = hlogs.next();
                    
                    item = (Item) Item.find(contextRO, handleLog.getType(), handleLog.getID());
                    if (item != null) {
//                        System.out.println(handleLog.getOper() + ", " + handleLog.getType() + "-" + handleLog.getID() + ", " + handleLog.getHandleID());
                        indexContent(contextRO, item, true); // force is true
                        handleLog.populateLogDone();
                    }
                }
                System.out.println(numlogs + " INS/UPD on Item");
                if (commit) { getSolr().commit(); }
            } finally {
                if (hlogs != null) { 
                	hlogs.close(); 
                	hlogs = null; 
                }
            }

        } catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
    }


    protected void indexCommunity(Context context, Community community, boolean force) 
            throws SQLException 
    {
        try {
            
            // Index the given community
            int id = community.getID();
            indexContent(context, community, force);
            context.removeCached(community, id);
    
            CollectionIterator collections = null;
            try {
                for (collections = CollectionAdd.findByCommunity(context, id); collections.hasNext();)
                {
                    Collection collection = collections.next();
                    indexContent(context, collection, force);
                    context.removeCached(collection, collection.getID());
                }
            } finally {
                if (collections != null)
                {
                    collections.close();
                }
            }
            
            ItemIterator items = null;
            try {
                for (items = ItemAdd.findByCommunity(context, id); items.hasNext();)
                {
                    Item item = items.next();
                    indexContent(context, item, force);
                    item.decache();
                }
            } finally {
                if (items != null)
                {
                    items.close();
                }
            }
    
            if(getSolr() != null)
            {
                getSolr().commit();
            }
    
        } catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
    }    
        
    
    /*
     * Index the whole community top-down : the community, its collections, its items
     */
    public void updateIndexC(Context contextRO, int id, boolean force)
    {
        try {
            Community community = Community.find(contextRO, id);
            
            indexCommunity(contextRO, community, force);
            
            if(getSolr() != null)
            {
                getSolr().commit();
            }

        } catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
    }    

    /**
     * Iterates over all documents in the Lucene index and verifies they are in
     * database, if not, they are removed.
     *
     * @param force whether or not to force a clean index
     * @throws IOException IO exception
     * @throws SQLException sql exception
     * @throws SearchServiceException occurs when something went wrong with querying the solr server
     */
    @Override
    public void cleanIndex(boolean force) throws IOException,
            SQLException, SearchServiceException {

        Context context = new Context();
        context.turnOffAuthorisationSystem();

        try
        {
            if(getSolr() == null)
            {
                return;
            }
            if (force)
            {
                getSolr().deleteByQuery("search.resourcetype:[2 TO 4]");
            } else {
                SolrQuery query = new SolrQuery();
                query.setQuery("search.resourcetype:[2 TO 4]");
                QueryResponse rsp = getSolr().query(query);
                SolrDocumentList docs = rsp.getResults();

                Iterator iter = docs.iterator();
                while (iter.hasNext())
                {

                 SolrDocument doc = (SolrDocument) iter.next();

                String handle = (String) doc.getFieldValue("handle");

                DSpaceObject o = HandleManager.resolveToObject(context, handle);

                if (o == null)
                {
                    log.info("Deleting: " + handle);
                    /*
                          * Use IndexWriter to delete, its easier to manage
                          * write.lock
                          */
                    unIndexContent(context, handle);
                } else {
                    context.removeCached(o, o.getID());
                    log.debug("Keeping: " + handle);
                }
            }
            }
        } catch(Exception e)
        {

            throw new SearchServiceException(e.getMessage(), e);
        } finally
        {
            context.abort();
        }




    }

    /**
     * Maintenance to keep a SOLR index efficient.
     * Note: This might take a long time.
     */
    @Override
    public void optimize()
    {
        try {
            if(getSolr() == null)
            {
                return;
            }
            long start = System.currentTimeMillis();
            System.out.println("SOLR Search Optimize -- Process Started:" + start);
            getSolr().optimize();
            long finish = System.currentTimeMillis();
            System.out.println("SOLR Search Optimize -- Process Finished:" + finish);
            System.out.println("SOLR Search Optimize -- Total time taken:" + (finish - start) + " (ms).");
        } catch (SolrServerException sse)
        {
            System.err.println(sse.getMessage());
        } catch (IOException ioe)
        {
            System.err.println(ioe.getMessage());
        }
    }

    @Override
    public void buildSpellCheck() throws SearchServiceException {
        try {
            if (getSolr() == null) {
                return;
            }
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.set("spellcheck", true);
            solrQuery.set(SpellingParams.SPELLCHECK_BUILD, true);
            getSolr().query(solrQuery);
        }catch (SolrServerException e)
        {
            //Make sure to also log the exception since this command is usually run from a crontab.
            log.error(e, e);
            throw new SearchServiceException(e);
        }
    }

    // //////////////////////////////////
    // Private
    // //////////////////////////////////

    protected void emailException(Exception exception)
    {
        // Also email an alert, system admin may need to check for stale lock
        try {
            String recipient = ConfigurationManager
                    .getProperty("alert.recipient");

            if (StringUtils.isNotBlank(recipient))
            {
                Email email = Email
                        .getEmail(I18nUtil.getEmailFilename(
                                Locale.getDefault(), "internal_error"));
                email.addRecipient(recipient);
                email.addArgument(ConfigurationManager
                        .getProperty("dspace.url"));
                email.addArgument(new Date());

                String stackTrace;

                if (exception != null)
                {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    exception.printStackTrace(pw);
                    pw.flush();
                    stackTrace = sw.toString();
                } else {
                    stackTrace = "No exception";
                }

                email.addArgument(stackTrace);
                email.send();
            }
        } catch (Exception e)
        {
            // Not much we can do here!
            log.warn("Unable to send email alert", e);
        }

    }


    /**
     * Is stale checks the lastModified time stamp in the database and the index
     * to determine if the index is stale.
     *
     * @param handle the handle of the dso
     * @param lastModified the last modified date of the DSpace object
     * @return a boolean indicating if the dso should be re indexed again
     * @throws SQLException sql exception
     * @throws IOException io exception
     * @throws SearchServiceException if something went wrong with querying the solr server
     */
    protected boolean requiresIndexing(String handle, Date lastModified)
            throws SQLException, IOException, SearchServiceException {

        boolean reindexItem = false;
        boolean inIndex = false;

        SolrQuery query = new SolrQuery();
        query.setQuery("handle:" + handle);
        QueryResponse rsp;

        try {
            if(getSolr() == null)
            {
                return false;
            }
            rsp = getSolr().query(query);
        } catch (SolrServerException e)
        {
            throw new SearchServiceException(e.getMessage(),e);
        }

        for (SolrDocument doc : rsp.getResults())
        {

            inIndex = true;

            Object value = doc.getFieldValue(LAST_INDEXED_FIELD);

            if(value instanceof Date)
            {
                Date lastIndexed = (Date) value;

                if (lastIndexed.before(lastModified))
                {

                    reindexItem = true;
                }
            }
        }

        return reindexItem || !inIndex;
    }


    /**
     * @param myitem the item for which our locations are to be retrieved
     * @return a list containing the identifiers of the communities & collections
     * @throws SQLException sql exception
     */

    protected List<String> getItemLocations(Item myitem)
            throws SQLException {
        List<String> locations = new Vector<String>();

        // build list of community ids
        Community[] communities = myitem.getCommunities();

        // build list of collection ids
        Collection[] collections = myitem.getCollections();
        
        // Lan 02.12.2015 : add owning_collection and owning_community
        Collection owningCollection = myitem.getOwningCollection();
        Community owningCommunity = (Community) owningCollection.getParentObject();
        locations.add("om" + owningCommunity.getID());
        locations.add("ol" + owningCollection.getID());

        // now put those into strings
        int i = 0;

        for (i = 0; i < communities.length; i++)
        {
            locations.add("m" + communities[i].getID());
        }

        for (i = 0; i < collections.length; i++)
        {
            locations.add("l" + collections[i].getID());
        }

        return locations;
    }

    protected List<String> getCollectionLocations(Collection target) throws SQLException {
        List<String> locations = new Vector<String>();

        // build list of community ids
        Community[] communities = target.getCommunities();
        
        // 06.09.2016 Lan : add owning_community and owning_collection
        // the collection owns itself
        Collection owningCollection = target;
        Community owningCommunity = (Community) owningCollection.getParentObject();
        locations.add("om" + owningCommunity.getID());
        locations.add("ol" + owningCollection.getID());

        // now put those into strings
        for (Community community : communities)
        {
            locations.add("m" + community.getID());
        }
        
        return locations;
    }
    
    protected List<String> getCommunityLocations(Community target) throws SQLException {
        List<String> locations = new Vector<String>();

        // build list of community ids
        Community parentCommunity = target.getParentCommunity();
        
        // 07.06.2016 Lan : add owning_community
        // the community owns itself
        locations.add("om" + target.getID());

        // now put those into strings
        if (parentCommunity != null) {
        	locations.add("m" + parentCommunity.getID());
        }
                
        return locations;
    }

    protected List<String> getCodeOrigineLocations(CodeOrigine target) throws SQLException {
        List<String> locations = new Vector<String>();
        
        locations.add("m" + target.getTopCommunityID());
        
        return locations;
    }

    /**
     * Write the document to the index under the appropriate handle.
     *
     * @param doc the solr document to be written to the server
     * @param streams
     * @throws IOException IO exception
     */
    protected void writeDocument(SolrInputDocument doc, List<BitstreamContentStream> streams) throws IOException {

        try {
            if(getSolr() != null)
            {
                if(CollectionUtils.isNotEmpty(streams))
                {
                    ContentStreamUpdateRequest req = new ContentStreamUpdateRequest("/update/extract");

                    for(BitstreamContentStream bce : streams)
                    {
                        req.addContentStream(bce);
                    }

                    ModifiableSolrParams params = new ModifiableSolrParams();

                    //req.setParam(ExtractingParams.EXTRACT_ONLY, "true");
                    for(String name : doc.getFieldNames())
                    {
                        for(Object val : doc.getFieldValues(name))
                        {
                             params.add(ExtractingParams.LITERALS_PREFIX + name,val.toString());
                        }
                    }

                    req.setParams(params);
                    req.setParam(ExtractingParams.UNKNOWN_FIELD_PREFIX, "attr_");
                    req.setParam(ExtractingParams.MAP_PREFIX + "content", "fulltext");
                    req.setParam(ExtractingParams.EXTRACT_FORMAT, "text");
                    req.setAction(AbstractUpdateRequest.ACTION.COMMIT, true, true);
                    req.process(getSolr());
                }
                else
                {
                    getSolr().add(doc);
                }
            }
        } catch (SolrServerException e)
        {
            log.error(e.getMessage(), e);
        }
    }


    /* 24.06.2016 Lan : write document using update.chain=UCstr */
    protected void writeDocumentUC(SolrInputDocument doc) throws IOException {
    	writeDocumentUC(doc, null);
    }

    protected void writeDocumentUC(SolrInputDocument doc, String UCstr) throws IOException {

        try {
            if(getSolr() != null)
            {
            	String updateChain = (UCstr == null || UCstr.isEmpty()) ? "defaultchain" : UCstr;
            	UpdateRequest req = new UpdateRequest();
            	req.setCommitWithin(-1);  
            	req.setParam("update.chain", updateChain);  
            	req.add(doc);
            	req.process(getSolr());
           }
        } catch (SolrServerException e)
        {
            log.error(e.getMessage(), e);
        }
    }

    protected void buildDocument(Context context, Community community)
    throws SQLException, IOException {
        List<String> locations = getCommunityLocations(community);

        // Create Document
        SolrInputDocument doc = buildDocument(Constants.COMMUNITY, community.getID(),
                community.getHandle(), locations);

        //Keep a list of our sort values which we added, sort values can only be added once
        List<String> sortFieldsAdded = new ArrayList<String>();

        List<DiscoveryConfiguration> discoveryConfigurations = Arrays.asList(SearchUtils.getDiscoveryConfiguration(Constants.COMMUNITY));

        //A map used to save each sidebarFacet config by the metadata fields
        Map<String, List<DiscoverySearchFilter>> searchFilters = new HashMap<String, List<DiscoverySearchFilter>>();
        Map<String, DiscoverySortFieldConfiguration> sortFields = new HashMap<String, DiscoverySortFieldConfiguration>();

        for (DiscoveryConfiguration discoveryConfiguration : discoveryConfigurations) {
        	for (int i = 0; i < discoveryConfiguration.getSearchFilters().size(); i++)
        	{
        		DiscoverySearchFilter discoverySearchFilter = discoveryConfiguration.getSearchFilters().get(i);
        		for (int j = 0; j < discoverySearchFilter.getMetadataFields().size(); j++)
        		{
        			String metadataField = discoverySearchFilter.getMetadataFields().get(j);
        			List<DiscoverySearchFilter> resultingList;
        			if(searchFilters.get(metadataField) != null)
        			{
        				resultingList = searchFilters.get(metadataField);
        			}else{
        				//New metadata field, create a new list for it
        				resultingList = new ArrayList<DiscoverySearchFilter>();
        			}
        			resultingList.add(discoverySearchFilter);

        			searchFilters.put(metadataField, resultingList);
        		}
        	}

        	DiscoverySortConfiguration sortConfiguration = discoveryConfiguration.getSearchSortConfiguration();
        	if(sortConfiguration != null)
        	{
        		for (DiscoverySortFieldConfiguration discoverySortConfiguration : sortConfiguration.getSortFields())
        		{
        			sortFields.put(discoverySortConfiguration.getMetadataField(), discoverySortConfiguration);
        		}
        	}
        }

        List<String> toIgnoreMetadataFields = SearchUtils.getIgnoredMetadataFields(community.getType());
        /*
         * Lan 25.07.2016 : some metedata are catched for full text search, not all of them
         */
        List<String> toCatchAllMetadataFields = SearchUtils.getCatchAllMetadataFields(community.getType());
        Metadatum[] mydc = community.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        for (Metadatum meta : mydc)
        {
            String field = meta.schema + "." + meta.element;
            String unqualifiedField = field;

            String value = meta.value;
            Date value_dt = null;
            
            String indexFieldName;

            if (value == null) { continue; }

            if (meta.qualifier != null && !meta.qualifier.trim().equals("")) { field += "." + meta.qualifier; }
            
            if (toIgnoreMetadataFields != null	&& (toIgnoreMetadataFields.contains(field) || toIgnoreMetadataFields.contains(unqualifiedField + "." + Item.ANY)))
            {
                continue;
            }
                       
            if ((searchFilters.get(field) != null || searchFilters.get(unqualifiedField + "." + Item.ANY) != null))
            {
                List<DiscoverySearchFilter> searchFilterConfigs = searchFilters.get(field);
                if(searchFilterConfigs == null)
                {
                    searchFilterConfigs = searchFilters.get(unqualifiedField + "." + Item.ANY);
                }

                for (DiscoverySearchFilter searchFilter : searchFilterConfigs)
                {
                    String separator = new DSpace().getConfigurationService().getProperty("discovery.solr.facets.split.char");
                    if(separator == null)
                    {
                        separator = FILTER_SEPARATOR;
                    }
                    if(searchFilter.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE))
                    {
                        //For our search filters that are dates we format them properly
                        value_dt = MultiFormatDateParser.parse(value);
                        if(value_dt != null)
                        {
                            //TODO: make this date format configurable !
                            value = DateFormatUtils.formatUTC(value_dt, "yyyy-MM-dd");
                            // 09.03.2015 Lan : add _dt that contains date AND time, not only date
                        	doc.addField(searchFilter.getIndexFieldName() + "_dt", value_dt);
                        }else{
                        	log.warn("Error while indexing search date field, community: " + community.getHandle() + " metadata field: " + field + " date value: " + value);
                        }
                    }
                    

                    doc.addField(searchFilter.getIndexFieldName(), value);
                    doc.addField(searchFilter.getIndexFieldName() + "_keyword", value);
                    // Lan add those following solr fields
                    doc.addField(searchFilter.getIndexFieldName() + "_contain", value);
                    if(!(searchFilter.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE)))
                    {
                    	doc.addField(searchFilter.getIndexFieldName() + "_partial", value);
                    }


                    if(searchFilter.getFilterType().equals(DiscoverySearchFilterFacet.FILTER_TYPE_FACET))
                    {
                        if(searchFilter.getType().equals(DiscoveryConfigurationParameters.TYPE_TEXT)) 
                        {
                           	doc.addField(searchFilter.getIndexFieldName() + "_filter", 
                           			OrderFormat.makeSortString(value, null, OrderFormat.TEXT) // Remove diacritic + lower case
                           			+ separator + value);
                        } else if(searchFilter.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE)) {
                        	if(value_dt != null)
                        	{
                        		String indexField = searchFilter.getIndexFieldName() + ".year";
                        		String yearUTC = DateFormatUtils.formatUTC(value_dt, "yyyy");
                        		doc.addField(searchFilter.getIndexFieldName() + "_keyword", yearUTC);
                        		// add the year to the autocomplete index
                        		doc.addField(searchFilter.getIndexFieldName() + "_ac", yearUTC);
                        		doc.addField(indexField, yearUTC);

                        		if (yearUTC.startsWith("0"))
                        		{
                        			doc.addField(
                        					searchFilter.getIndexFieldName()
                        					+ "_keyword",
                        					yearUTC.replaceFirst("0*", ""));
                        			// add date without starting zeros for autocomplete e filtering
                        			doc.addField(
                        					searchFilter.getIndexFieldName()
                        					+ "_ac",
                        					yearUTC.replaceFirst("0*", ""));
                        			doc.addField(
                        					searchFilter.getIndexFieldName()
                        					+ "_ac",
                        					value.replaceFirst("0*", ""));
                        			doc.addField(
                        					searchFilter.getIndexFieldName()
                        					+ "_keyword",
                        					value.replaceFirst("0*", ""));
                        		}

                        		//Also save a sort value of this year, this is required for determining the upper & lower bound year of our facet
                        		if(doc.getField(indexField + "_sort") == null)
                        		{
                        			//We can only add one year so take the first one
                        			doc.addField(indexField + "_sort", yearUTC);
                        		}
                        	}
                        } else if(searchFilter.getType().equals(DiscoveryConfigurationParameters.TYPE_HIERARCHICAL)) {
                            HierarchicalSidebarFacetConfiguration hierarchicalSidebarFacetConfiguration = (HierarchicalSidebarFacetConfiguration) searchFilter;
                            String[] subValues = value.split(hierarchicalSidebarFacetConfiguration.getSplitter());
                            // Lan 24.02.2015 : skip first node even the following node is null
                            // if(hierarchicalSidebarFacetConfiguration.isSkipFirstNodeLevel() && 1 < subValues.length)
                            if(hierarchicalSidebarFacetConfiguration.isSkipFirstNodeLevel() && 1 <= subValues.length)
                            {
                                //Remove the first element of our array
                                subValues = (String[]) ArrayUtils.subarray(subValues, 1, subValues.length);
                            }
                            for (int i = 0; i < subValues.length; i++)
                            {
                                StringBuilder valueBuilder = new StringBuilder();
                                for(int j = 0; j <= i; j++)
                                {
                                    valueBuilder.append(subValues[j]);
                                    if(j < i)
                                    {
                                        valueBuilder.append(hierarchicalSidebarFacetConfiguration.getSplitter());
                                    }
                                }

                                String indexValue = valueBuilder.toString().trim();
                                doc.addField(searchFilter.getIndexFieldName() + "_tax_" + i + "_filter", indexValue.toLowerCase() + separator + indexValue);
                                //We add the field x times that it has occurred
                                for(int j = i; j < subValues.length; j++)
                                {
                                    doc.addField(searchFilter.getIndexFieldName() + "_filter", indexValue.toLowerCase() + separator + indexValue);
                                    doc.addField(searchFilter.getIndexFieldName() + "_keyword", indexValue);
                                }
                            }
                        }
                    }
                }
            }
            
            if (sortFields.get(field) != null  && !sortFieldsAdded.contains(field))
            {
                //Only add sort value once
                String type;
                type = sortFields.get(field).getType();

                // 09.03.2015 Lan : <field>_sort for date contains date AND time, not only date
                if(type.equals(DiscoveryConfigurationParameters.TYPE_DATE))
                {
                	if (value_dt == null) {
                		value_dt = MultiFormatDateParser.parse(value);
                	}
                    if(value_dt != null)
                    {
                    	String sort_dt = DateFormatUtils.ISO_DATETIME_FORMAT.format(value_dt);
                        doc.addField(field + "_sort", sort_dt);
                    }else{
                        log.warn("Error while indexing sort date field, community: " + community.getHandle() + " metadata field: " + field + " date value: " + value);
                    }
                }else{
                    doc.addField(field + "_sort", value);
                }
                sortFieldsAdded.add(field);
            }
            
            /*
             * Lan 25.07.2016 : add for full text search
             */
            if (toCatchAllMetadataFields != null	&& (toCatchAllMetadataFields.contains(field) || toCatchAllMetadataFields.contains(unqualifiedField + "." + Item.ANY)))
            {
            	doc.addField("catched_" + field, value);
            }
            
            doc.addField(field, value);

        }

        //Do any additional indexing, depends on the plugins
        List<SolrServiceIndexPlugin> solrServiceIndexPlugins = new DSpace().getServiceManager().getServicesByType(SolrServiceIndexPlugin.class);
        for (SolrServiceIndexPlugin solrServiceIndexPlugin : solrServiceIndexPlugins)
        {
            solrServiceIndexPlugin.additionalIndex(context, community, doc);
        }

        writeDocument(doc, null);
    }

    /**
     * Build a Lucene document for a DSpace Item and write the index
     *
     * @param context
     * @param collection
     * @throws SQLException
     * @throws IOException
     * 
     * 01.07.2016 Lan : copy of buildDOcument(Context, Item) where Item is replaced by Collection
     */
    protected void buildDocument(Context context, Collection collection)
    throws SQLException, IOException {
        List<String> locations = getCollectionLocations(collection);

        // Create Lucene Document
        SolrInputDocument doc = buildDocument(Constants.COLLECTION, collection.getID(),
                collection.getHandle(), locations);

        //Keep a list of our sort values which we added, sort values can only be added once
        List<String> sortFieldsAdded = new ArrayList<String>();

        List<DiscoveryConfiguration> discoveryConfigurations = Arrays.asList(SearchUtils.getDiscoveryConfiguration(Constants.COLLECTION));

        //A map used to save each sidebarFacet config by the metadata fields
        Map<String, List<DiscoverySearchFilter>> searchFilters = new HashMap<String, List<DiscoverySearchFilter>>();
        Map<String, DiscoverySortFieldConfiguration> sortFields = new HashMap<String, DiscoverySortFieldConfiguration>();

        for (DiscoveryConfiguration discoveryConfiguration : discoveryConfigurations) {
        	for (int i = 0; i < discoveryConfiguration.getSearchFilters().size(); i++)
        	{
        		DiscoverySearchFilter discoverySearchFilter = discoveryConfiguration.getSearchFilters().get(i);
        		for (int j = 0; j < discoverySearchFilter.getMetadataFields().size(); j++)
        		{
        			String metadataField = discoverySearchFilter.getMetadataFields().get(j);
        			List<DiscoverySearchFilter> resultingList;
        			if(searchFilters.get(metadataField) != null)
        			{
        				resultingList = searchFilters.get(metadataField);
        			}else{
        				//New metadata field, create a new list for it
        				resultingList = new ArrayList<DiscoverySearchFilter>();
        			}
        			resultingList.add(discoverySearchFilter);

        			searchFilters.put(metadataField, resultingList);
        		}
        	}

        	DiscoverySortConfiguration sortConfiguration = discoveryConfiguration.getSearchSortConfiguration();
        	if(sortConfiguration != null)
        	{
        		for (DiscoverySortFieldConfiguration discoverySortConfiguration : sortConfiguration.getSortFields())
        		{
        			sortFields.put(discoverySortConfiguration.getMetadataField(), discoverySortConfiguration);
        		}
        	}
        }

        List<String> toIgnoreMetadataFields = SearchUtils.getIgnoredMetadataFields(collection.getType());
        /*
         * Lan 25.07.2016 : some metedata are catched for full text search, not all of them
         */
        List<String> toCatchAllMetadataFields = SearchUtils.getCatchAllMetadataFields(collection.getType());
        Metadatum[] mydc = collection.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        for (Metadatum meta : mydc)
        {
            String field = meta.schema + "." + meta.element;
            String unqualifiedField = field;

            String value = meta.value;
            Date value_dt = null;
            
            String indexFieldName;

            if (value == null) { continue; }

            if (meta.qualifier != null && !meta.qualifier.trim().equals("")) { field += "." + meta.qualifier; }
            
            if (toIgnoreMetadataFields != null	&& (toIgnoreMetadataFields.contains(field) || toIgnoreMetadataFields.contains(unqualifiedField + "." + Item.ANY)))
            {
                continue;
            }
                       
            if ((searchFilters.get(field) != null || searchFilters.get(unqualifiedField + "." + Item.ANY) != null))
            {
                List<DiscoverySearchFilter> searchFilterConfigs = searchFilters.get(field);
                if(searchFilterConfigs == null)
                {
                    searchFilterConfigs = searchFilters.get(unqualifiedField + "." + Item.ANY);
                }

                for (DiscoverySearchFilter searchFilter : searchFilterConfigs)
                {
                    	
                    String separator = new DSpace().getConfigurationService().getProperty("discovery.solr.facets.split.char");
                    if(separator == null)
                    {
                        separator = FILTER_SEPARATOR;
                    }
                    if(searchFilter.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE))
                    {
                        //For our search filters that are dates we format them properly
                        value_dt = MultiFormatDateParser.parse(value);
                        if(value_dt != null)
                        {
                            //TODO: make this date format configurable !
                            value = DateFormatUtils.formatUTC(value_dt, "yyyy-MM-dd");
                            // 09.03.2015 Lan : add _dt that contains date AND time, not only date
                        	doc.addField(searchFilter.getIndexFieldName() + "_dt", value_dt);
                        }else{
                        	log.warn("Error while indexing search date field, collection: " + collection.getHandle() + " metadata field: " + field + " date value: " + value);
                        }
                    }
                    

                    /* Lan  extract code_origine from support hardcode ! ******************************************/
                    if (field.equals("rtbf.support")) {
                    	/* isolate code_origine part */
                    	String value_p1 = value.replaceAll("^(((?!::).)+)::(((?!::).)+)(::)?(.*)?$", "$3").replaceAll("^(((?!\\\\\\\\).)+)(.*)$", "$1");
                        doc.addField(searchFilter.getIndexFieldName(), value_p1);
                        doc.addField(searchFilter.getIndexFieldName() + "_keyword", value_p1);
                        doc.addField(searchFilter.getIndexFieldName() + "_contain", value_p1);
                        doc.addField(searchFilter.getIndexFieldName() + "_partial", value_p1);
                    	
                    	/* isolate place part */
                    	String value_p2 = value.replaceAll("^Fichier Sonuma.*$", "").replaceAll("^(((?!::).)+)::(((?!::).)+)(::)?(.*)?$", "$6");
                    	if (!value_p2.isEmpty()) {
                    		doc.addField(searchFilter.getIndexFieldName(), value_p2);
                    		doc.addField(searchFilter.getIndexFieldName() + "_keyword", value_p2);
                            doc.addField(searchFilter.getIndexFieldName() + "_contain", value_p2);
                    		doc.addField(searchFilter.getIndexFieldName() + "_partial", value_p2);
                    	}
                    	
                    	continue;
                    }
                    /* Lan code_origine hardcode ! ******************************************/

                    /* Lan rtbf.contributor_plus_role hardcode ! ******************************************/
                    /* create role_contributor_filter solr field for prefix facetting */
                    if (field.equals("rtbf.contributor_plus_role")) {
                    	String splitter = ((HierarchicalSidebarFacetConfiguration) searchFilter).getSplitter();
                    	/* permute role before contributor */
                    	String value_p1 = value.replaceAll("^(.+)"+splitter+"(.+)$", "$2"+splitter+"$1");
                       // Remove diacritic + lower case
                        String value_p2 = OrderFormat.makeSortString(value_p1, null, OrderFormat.TEXT);
                    	doc.addField("role_"+searchFilter.getIndexFieldName() + "_filter", value_p2 + separator + value);

                    }
                    /* Lan rtbf.contributor_plus_role hardcode ! ******************************************/

                    doc.addField(searchFilter.getIndexFieldName(), value);
                    doc.addField(searchFilter.getIndexFieldName() + "_keyword", value);
                    // Lan add those following solr fields
                    doc.addField(searchFilter.getIndexFieldName() + "_contain", value);
                    if(!(searchFilter.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE)))
                    {
                    	doc.addField(searchFilter.getIndexFieldName() + "_partial", value);
                    }


                    if(searchFilter.getFilterType().equals(DiscoverySearchFilterFacet.FILTER_TYPE_FACET))
                    {
                        if(searchFilter.getType().equals(DiscoveryConfigurationParameters.TYPE_TEXT)) 
                        {
                           	doc.addField(searchFilter.getIndexFieldName() + "_filter", 
                           			OrderFormat.makeSortString(value, null, OrderFormat.TEXT) // Remove diacritic + lower case
                           			+ separator + value);
                        } else if(searchFilter.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE)) {
                        	if(value_dt != null)
                        	{
                        		String indexField = searchFilter.getIndexFieldName() + ".year";
                        		String yearUTC = DateFormatUtils.formatUTC(value_dt, "yyyy");
                        		doc.addField(searchFilter.getIndexFieldName() + "_keyword", yearUTC);
                        		// add the year to the autocomplete index
                        		doc.addField(searchFilter.getIndexFieldName() + "_ac", yearUTC);
                        		doc.addField(indexField, yearUTC);

                        		if (yearUTC.startsWith("0"))
                        		{
                        			doc.addField(
                        					searchFilter.getIndexFieldName()
                        					+ "_keyword",
                        					yearUTC.replaceFirst("0*", ""));
                        			// add date without starting zeros for autocomplete e filtering
                        			doc.addField(
                        					searchFilter.getIndexFieldName()
                        					+ "_ac",
                        					yearUTC.replaceFirst("0*", ""));
                        			doc.addField(
                        					searchFilter.getIndexFieldName()
                        					+ "_ac",
                        					value.replaceFirst("0*", ""));
                        			doc.addField(
                        					searchFilter.getIndexFieldName()
                        					+ "_keyword",
                        					value.replaceFirst("0*", ""));
                        		}

                        		//Also save a sort value of this year, this is required for determining the upper & lower bound year of our facet
                        		if(doc.getField(indexField + "_sort") == null)
                        		{
                        			//We can only add one year so take the first one
                        			doc.addField(indexField + "_sort", yearUTC);
                        		}
                        	}
                        } else if(searchFilter.getType().equals(DiscoveryConfigurationParameters.TYPE_HIERARCHICAL)) {
                            HierarchicalSidebarFacetConfiguration hierarchicalSidebarFacetConfiguration = (HierarchicalSidebarFacetConfiguration) searchFilter;
                            String[] subValues = value.split(hierarchicalSidebarFacetConfiguration.getSplitter());
                            // Lan 24.02.2015 : skip first node even the following node is null
                            // if(hierarchicalSidebarFacetConfiguration.isSkipFirstNodeLevel() && 1 < subValues.length)
                            if(hierarchicalSidebarFacetConfiguration.isSkipFirstNodeLevel() && 1 <= subValues.length)
                            {
                                //Remove the first element of our array
                                subValues = (String[]) ArrayUtils.subarray(subValues, 1, subValues.length);
                            }
                            for (int i = 0; i < subValues.length; i++)
                            {
                                StringBuilder valueBuilder = new StringBuilder();
                                for(int j = 0; j <= i; j++)
                                {
                                    valueBuilder.append(subValues[j]);
                                    if(j < i)
                                    {
                                        valueBuilder.append(hierarchicalSidebarFacetConfiguration.getSplitter());
                                    }
                                }

                                String indexValue = valueBuilder.toString().trim();
                                doc.addField(searchFilter.getIndexFieldName() + "_tax_" + i + "_filter", indexValue.toLowerCase() + separator + indexValue);
                                //We add the field x times that it has occurred
                                for(int j = i; j < subValues.length; j++)
                                {
                                    doc.addField(searchFilter.getIndexFieldName() + "_filter", indexValue.toLowerCase() + separator + indexValue);
                                    doc.addField(searchFilter.getIndexFieldName() + "_keyword", indexValue);
                                }
                            }
                        }
                    }
                }
            }
            
            if (sortFields.get(field) != null  && !sortFieldsAdded.contains(field))
            {
                //Only add sort value once
                String type;
                type = sortFields.get(field).getType();

                // 09.03.2015 Lan : <field>_sort for date contains date AND time, not only date
                if(type.equals(DiscoveryConfigurationParameters.TYPE_DATE))
                {
                	if (value_dt == null) {
                		value_dt = MultiFormatDateParser.parse(value);
                	}
                    if(value_dt != null)
                    {
                    	String sort_dt = DateFormatUtils.ISO_DATETIME_FORMAT.format(value_dt);
                        doc.addField(field + "_sort", sort_dt);
                    }else{
                        log.warn("Error while indexing sort date field, collection: " + collection.getHandle() + " metadata field: " + field + " date value: " + value);
                    }
                }else{
                    doc.addField(field + "_sort", value);
                }
                sortFieldsAdded.add(field);
            }
            
            /*
             * Lan 25.07.2016 : add for full text search
             */
            if (toCatchAllMetadataFields != null	&& (toCatchAllMetadataFields.contains(field) || toCatchAllMetadataFields.contains(unqualifiedField + "." + Item.ANY)))
            {
            	doc.addField("catched_" + field, value);
            }
            
            doc.addField(field, value);

            
        }


        /* Lan 20.06.2016 : index related code_origine of supports
         * Lan 26.07.2015 : filter removed
        try {
        	CodeOrigine[] codeOrigines = CollectionAdd.CodeOrigineCollection.findById(context, collection.getID());
        	for (CodeOrigine codeOrigine : codeOrigines) {
        		buildDocument(context, codeOrigine);
                log.info("Wrote CodeOrigine: " + codeOrigine.getCode() + " to Index");
			}

        	log.debug("Index all code_origine of collection " + collection.getID());
        } catch (RuntimeException e)
        {
            log.error(e.getMessage(), e);
        }
        */
        
        
        //Do any additional indexing, depends on the plugins
        List<SolrServiceIndexPlugin> solrServiceIndexPlugins = new DSpace().getServiceManager().getServicesByType(SolrServiceIndexPlugin.class);
        for (SolrServiceIndexPlugin solrServiceIndexPlugin : solrServiceIndexPlugins)
        {
            solrServiceIndexPlugin.additionalIndex(context, collection, doc);
        }

        writeDocument(doc, null);
    }

    protected void buildDocument(Context context, CodeOrigine codeOrigine)
    throws SQLException, IOException {
        List<String> locations = getCodeOrigineLocations(codeOrigine);

        // Create Lucene Document
        SolrInputDocument doc = buildDocument(CodeOrigine.RESOURCE_ID, codeOrigine.getID(), null, locations);

        // TODO remove hardcode
        String indexFieldName = CodeOrigine.INDEX_FIELD_NAME;
        String value = codeOrigine.getCode();
        doc.addField(indexFieldName, value);
        doc.addField(indexFieldName + "_sort", value);
    	doc.addField(indexFieldName + "_partial", value);
    	doc.addField(indexFieldName + "_keyword", value);
    	doc.addField(indexFieldName + "_contain", value);
        

        writeDocumentUC(doc);
    }

    
    /**
     * Add the metadata value of the community/collection to the solr document
     * IF needed highlighting is added !
     * @param doc the solr document
     * @param highlightedMetadataFields the list of metadata fields that CAN be highlighted
     * @param metadataField the metadata field added
     * @param value the value (can be NULL !)
     */
    protected void addContainerMetadataField(SolrInputDocument doc, List<String> highlightedMetadataFields, List<String> toIgnoreMetadataFields, String metadataField, String value)
    {
        if(toIgnoreMetadataFields == null || !toIgnoreMetadataFields.contains(metadataField))
        {
            if(StringUtils.isNotBlank(value))
            {
                doc.addField(metadataField, value);
                if(highlightedMetadataFields.contains(metadataField))
                {
                    doc.addField(metadataField + "_hl", value);
                }
            }
        }
    }

    /**
     * Build a Lucene document for a DSpace Item and write the index
     *
     * @param context Users Context
     * @param item    The DSpace Item to be indexed
     * @throws SQLException
     * @throws IOException
     * 
     * 10.10.2015 Lan : si un field DB de type date fait l'objet d'un searchFilter, ajouter 1 field index de nom getIndexFieldName() + "_dt"
     * 		exemple: pour dc.date.issued, on crée date_issued_dt de type date et contient date ET HEURE
     * 09.03.2016 Lan : si un field DB de type date fait l'objet d'un sortFieldConfiguration, le field index de nom getIndexFieldName() + "_sort" comporte la partie heure
     * 		exemple: pour dc.date.issued (qui n'a plus la partie heure), dc.date.issued_sort est de type text et contient date ET HEURE
     */
    protected void buildDocument(Context context, Item item)
            throws SQLException, IOException {
        String handle = item.getHandle();

        if (handle == null)
        {
            handle = HandleManager.findHandle(context, item);
        }

        // get the location string (for searching by collection & community)
        List<String> locations = getItemLocations(item);

        SolrInputDocument doc = null;
        if (item instanceof ItemAdd.ItemDup) {
        	doc = buildDocument(Constants.ITEM, item.getID(), ((ItemAdd.ItemDup)item).getSearchUniqueID(), handle, locations);
            log.info("Building ItemDup: " + handle + " search.uniqueid:" + ((ItemAdd.ItemDup)item).getSearchUniqueID());
        } else {
        	doc = buildDocument(Constants.ITEM, item.getID(), handle, locations);
            log.debug("Building Item: " + handle);
        }

        doc.addField("withdrawn", item.isWithdrawn());
        doc.addField("discoverable", item.isDiscoverable());
        
        //Keep a list of our sort values which we added, sort values can only be added once
        List<String> sortFieldsAdded = new ArrayList<String>();
        // Set<String> hitHighlightingFields = new HashSet<String>(); // Lan 16.02.2016 : not use anymore
        try {
        	// Lan 02.05.2014 : this list contains only 1 configuration, the one specific to item
        	List<DiscoveryConfiguration> discoveryConfigurations = Arrays.asList(SearchUtils.getDiscoveryConfiguration(item));

            //A map used to save each sidebarFacet config by the metadata fields
            Map<String, List<DiscoverySearchFilter>> searchFilters = new HashMap<String, List<DiscoverySearchFilter>>();
            Map<String, DiscoverySortFieldConfiguration> sortFields = new HashMap<String, DiscoverySortFieldConfiguration>();
            Map<String, DiscoveryRecentSubmissionsConfiguration> recentSubmissionsConfigurationMap = new HashMap<String, DiscoveryRecentSubmissionsConfiguration>();
            Set<String> moreLikeThisFields = new HashSet<String>();
            for (DiscoveryConfiguration discoveryConfiguration : discoveryConfigurations)
            {
                for (int i = 0, ilen = discoveryConfiguration.getSearchFilters().size(); i < ilen; i++)
                {
                    DiscoverySearchFilter discoverySearchFilter = discoveryConfiguration.getSearchFilters().get(i);
                    for (int j = 0, jlen = discoverySearchFilter.getMetadataFields().size(); j < jlen; j++)
                    {
                        String metadataField = discoverySearchFilter.getMetadataFields().get(j);
                        List<DiscoverySearchFilter> resultingList;
                        if(searchFilters.get(metadataField) != null) //if id an entry already exists for this metadataField
                        {
                            resultingList = searchFilters.get(metadataField);
                        } else {
                            //New metadata field, create a new list for it
                            resultingList = new ArrayList<DiscoverySearchFilter>();
                        }
                        resultingList.add(discoverySearchFilter);

                        searchFilters.put(metadataField, resultingList);
                    }
                }

                DiscoverySortConfiguration sortConfiguration = discoveryConfiguration.getSearchSortConfiguration();
                if(sortConfiguration != null)
                {
                    for (DiscoverySortFieldConfiguration discoverySortConfiguration : sortConfiguration.getSortFields())
                    {
                        sortFields.put(discoverySortConfiguration.getMetadataField(), discoverySortConfiguration);
                    }
                }

                DiscoveryRecentSubmissionsConfiguration recentSubmissionConfiguration = discoveryConfiguration.getRecentSubmissionConfiguration();
                if(recentSubmissionConfiguration != null)
                {
                    recentSubmissionsConfigurationMap.put(recentSubmissionConfiguration.getMetadataSortField(), recentSubmissionConfiguration);
                }

                /* Lan 16.02.2016 : not use anymore
                DiscoveryHitHighlightingConfiguration hitHighlightingConfiguration = discoveryConfiguration.getHitHighlightingConfiguration();
                if(hitHighlightingConfiguration != null)
                {
                    List<DiscoveryHitHighlightFieldConfiguration> fieldConfigurations = hitHighlightingConfiguration.getMetadataFields();
                    for (DiscoveryHitHighlightFieldConfiguration fieldConfiguration : fieldConfigurations)
                    {
                        hitHighlightingFields.add(fieldConfiguration.getField());
                    }
            	}
            	*/

                DiscoveryMoreLikeThisConfiguration moreLikeThisConfiguration = discoveryConfiguration.getMoreLikeThisConfiguration();
                if(moreLikeThisConfiguration != null)
                {
                    for(String metadataField : moreLikeThisConfiguration.getSimilarityMetadataFields())
                    {
                        moreLikeThisFields.add(metadataField);
                    }
                }
            }


            List<String> toProjectionFields = new ArrayList<String>();
            String projectionFieldsString = new DSpace().getConfigurationService().getProperty("discovery.index.projection");
            if(projectionFieldsString != null){
                if(projectionFieldsString.indexOf(",") != -1){
                    for (int i = 0; i < projectionFieldsString.split(",").length; i++) {
                        toProjectionFields.add(projectionFieldsString.split(",")[i].trim());
                    }
                } else {
                    toProjectionFields.add(projectionFieldsString);
                }
            }

            List<String> toIgnoreMetadataFields = SearchUtils.getIgnoredMetadataFields(item.getType());
            /*
             * Lan 25.07.2016 : some metadata are catched for full text search, not all of them
             */
            List<String> toCatchAllMetadataFields = SearchUtils.getCatchAllMetadataFields(item.getType());
            Metadatum[] mydc = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
            for (Metadatum meta : mydc)
            {
                String field = meta.schema + "." + meta.element;
                String unqualifiedField = field;

                String value = meta.value;
                Date value_dt = null;

                if (value == null)
                {
                    continue;
                }

                if (meta.qualifier != null && !meta.qualifier.trim().equals(""))
                {
                    field += "." + meta.qualifier;
                }

                //We are not indexing provenance, this is useless
                if (toIgnoreMetadataFields != null && (toIgnoreMetadataFields.contains(field) || toIgnoreMetadataFields.contains(unqualifiedField + "." + Item.ANY)))
                {
                    continue;
                }

                String authority = null;
                String preferedLabel = null;
                List<String> variants = null;
                boolean isAuthorityControlled = MetadataAuthorityManager
                        .getManager().isAuthorityControlled(meta.schema,
                                meta.element,
                                meta.qualifier);

                int minConfidence = isAuthorityControlled?MetadataAuthorityManager
                        .getManager().getMinConfidence(
                                meta.schema,
                                meta.element,
                                meta.qualifier):Choices.CF_ACCEPTED;

                if (isAuthorityControlled && meta.authority != null
                        && meta.confidence >= minConfidence)
                {
                    boolean ignoreAuthority = new DSpace()
                            .getConfigurationService()
                            .getPropertyAsType(
                                    "discovery.index.authority.ignore." + field,
                                    new DSpace()
                                            .getConfigurationService()
                                            .getPropertyAsType(
                                                    "discovery.index.authority.ignore",
                                                    new Boolean(false)), true);
                    if (!ignoreAuthority)
                    {
                        authority = meta.authority;

                        boolean ignorePrefered = new DSpace()
                                .getConfigurationService()
                                .getPropertyAsType(
                                        "discovery.index.authority.ignore-prefered."
                                                + field,
                                        new DSpace()
                                                .getConfigurationService()
                                                .getPropertyAsType(
                                                        "discovery.index.authority.ignore-prefered",
                                                        new Boolean(false)),
                                        true);
                        if (!ignorePrefered)
                        {

                            preferedLabel = ChoiceAuthorityManager.getManager()
                                    .getLabel(meta.schema, meta.element,
                                            meta.qualifier, meta.authority,
                                            meta.language);
                        }

                        boolean ignoreVariants = new DSpace()
                                .getConfigurationService()
                                .getPropertyAsType(
                                        "discovery.index.authority.ignore-variants."
                                                + field,
                                        new DSpace()
                                                .getConfigurationService()
                                                .getPropertyAsType(
                                                        "discovery.index.authority.ignore-variants",
                                                        new Boolean(false)),
                                        true);
                        if (!ignoreVariants)
                        {
                            variants = ChoiceAuthorityManager.getManager()
                                    .getVariants(meta.schema, meta.element,
                                            meta.qualifier, meta.authority,
                                            meta.language);
                        }

                    }
                }

                if ((searchFilters.get(field) != null || searchFilters.get(unqualifiedField + "." + Item.ANY) != null))
                {
                    List<DiscoverySearchFilter> searchFilterConfigs = searchFilters.get(field);
                    if(searchFilterConfigs == null)
                    {
                        searchFilterConfigs = searchFilters.get(unqualifiedField + "." + Item.ANY);
                    }

                    for (DiscoverySearchFilter searchFilter : searchFilterConfigs)
                    {
                    	                    	
                        String separator = new DSpace().getConfigurationService().getProperty("discovery.solr.facets.split.char");
                        if(separator == null)
                        {
                            separator = FILTER_SEPARATOR;
                        }
                        if(searchFilter.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE))
                        {
                            //For our search filters that are dates we format them properly
                            value_dt = MultiFormatDateParser.parse(value);
                            if(value_dt != null)
                            {
                                //TODO: make this date format configurable !
                                value = DateFormatUtils.formatUTC(value_dt, "yyyy-MM-dd");
                                // 09.03.2015 Lan : add _dt that contains date AND time, not only date
                            	doc.addField(searchFilter.getIndexFieldName() + "_dt", value_dt);
                            }else{
                            	log.warn("Error while indexing search date field, item: " + item.getHandle() + " metadata field: " + field + " date value: " + value);
                            }
                        }
                        

                        /* TODO remove extract code_origine from support hardcode ! ******************************************/
                        if (field.equals("rtbf.support")) {
                        	/* isolate code_origine part */
                        	String value_p1 = value.replaceAll("^(((?!::).)+)::(((?!::).)+)(::)?(.*)?$", "$3").replaceAll("^(((?!\\\\\\\\).)+)(.*)$", "$1");
                            doc.addField(searchFilter.getIndexFieldName(), value_p1);
                            doc.addField(searchFilter.getIndexFieldName() + "_keyword", value_p1);
                            doc.addField(searchFilter.getIndexFieldName() + "_contain", value_p1);
                            doc.addField(searchFilter.getIndexFieldName() + "_partial", value_p1);
                        	
                        	/* isolate place part */
                        	String value_p2 = value.replaceAll("^Fichier Sonuma.*$", "").replaceAll("^(((?!::).)+)::(((?!::).)+)(::)?(.*)?$", "$6");
                        	if (!value_p2.isEmpty()) {
                        		doc.addField(searchFilter.getIndexFieldName(), value_p2);
                        		doc.addField(searchFilter.getIndexFieldName() + "_keyword", value_p2);
                                doc.addField(searchFilter.getIndexFieldName() + "_contain", value_p2);
                        		doc.addField(searchFilter.getIndexFieldName() + "_partial", value_p2);
                        	}
                        	
                        	continue;
                        }

                        /* TODO remove rtbf.contributor_plus_role hardcode ! ******************************************/
                        /* create role_contributor_filter solr field for prefix facetting */
                        if (field.equals("rtbf.contributor_plus_role")) {
                        	String splitter = ((HierarchicalSidebarFacetConfiguration) searchFilter).getSplitter();
                        	/* permute role before contributor */
                        	String value_p1 = value.replaceAll("^(.+)"+splitter+"(.+)$", "$2"+splitter+"$1");
                            // Remove diacritic + lower case
                            String value_p2 = OrderFormat.makeSortString(value_p1, null, OrderFormat.TEXT);
                        	doc.addField("role_"+searchFilter.getIndexFieldName() + "_filter", value_p2 + separator + value);

                        }

                        
                        doc.addField(searchFilter.getIndexFieldName(), value);
                        doc.addField(searchFilter.getIndexFieldName() + "_keyword", value);
                        // Lan add those following solr fields
                        doc.addField(searchFilter.getIndexFieldName() + "_contain", value);
                        if(!(searchFilter.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE)))
                        {
                        	doc.addField(searchFilter.getIndexFieldName() + "_partial", value);
                        }

                        if (authority != null && preferedLabel == null)
                        {
                            doc.addField(searchFilter.getIndexFieldName()
                                    + "_keyword", value + AUTHORITY_SEPARATOR
                                    + authority);
                            doc.addField(searchFilter.getIndexFieldName()
                                    + "_authority", authority);
                            doc.addField(searchFilter.getIndexFieldName()
                                    + "_acid", value.toLowerCase()
                                    + separator + value
                                    + AUTHORITY_SEPARATOR + authority);
                        }

                        if (preferedLabel != null)
                        {
                            doc.addField(searchFilter.getIndexFieldName(),
                                    preferedLabel);
                            doc.addField(searchFilter.getIndexFieldName()
                                    + "_keyword", preferedLabel);
                            doc.addField(searchFilter.getIndexFieldName()
                                    + "_keyword", preferedLabel
                                    + AUTHORITY_SEPARATOR + authority);
                            doc.addField(searchFilter.getIndexFieldName()
                                    + "_authority", authority);
                            doc.addField(searchFilter.getIndexFieldName()
                                    + "_acid", preferedLabel.toLowerCase()
                                    + separator + preferedLabel
                                    + AUTHORITY_SEPARATOR + authority);
                        }
                        if (variants != null)
                        {
                            for (String var : variants)
                            {
                                doc.addField(searchFilter.getIndexFieldName() + "_keyword", var);
                                doc.addField(searchFilter.getIndexFieldName()
                                        + "_acid", var.toLowerCase()
                                        + separator + var
                                        + AUTHORITY_SEPARATOR + authority);
                            }
                        }

                        //Add a dynamic fields for auto complete in search
                        /* Lan  - replace by normalize value      
                        doc.addField(searchFilter.getIndexFieldName() + "_ac",
                                value.toLowerCase() + separator + value);
                         */
                        doc.addField(searchFilter.getIndexFieldName() + "_ac",
                                OrderFormat.makeSortString(value, null, OrderFormat.TEXT)
                                + separator + value);                                
                       if (preferedLabel != null)
                        {
                            doc.addField(searchFilter.getIndexFieldName() + "_ac",
                                    // preferedLabel.toLowerCase()
                                    OrderFormat.makeSortString(preferedLabel, null, OrderFormat.TEXT)                                
                                    + separator + preferedLabel);
                        }
                        if (variants != null)
                        {
                            for (String var : variants)
                            {
                                doc.addField(searchFilter.getIndexFieldName() +"_ac",
                                        // var.toLowerCase()
                                        OrderFormat.makeSortString(var, null, OrderFormat.TEXT)                                
                                        + separator + var);
                            }
                        }

                        if(searchFilter.getFilterType().equals(DiscoverySearchFilterFacet.FILTER_TYPE_FACET))
                        {
                            if(searchFilter.getType().equals(DiscoveryConfigurationParameters.TYPE_TEXT))
                            {
                            	//Add a special filter
                           	 	//We use a separator to split up the lowercase and regular case, this is needed to get our filters in regular case
                            	//Solr has issues with facet prefix and cases
                            	if (authority != null)
                            	{
                                	String facetValue = preferedLabel != null?preferedLabel:value;
                                	doc.addField(searchFilter.getIndexFieldName() + "_filter", facetValue.toLowerCase() + separator + facetValue + AUTHORITY_SEPARATOR + authority);
                            	}
                            	else
                            	{
                                	doc.addField(searchFilter.getIndexFieldName() + "_filter", value.toLowerCase() + separator + value);
                            	}
                            }else
                                if(searchFilter.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE))
                                {
                                    if(value_dt != null)
                                    {
                                        String indexField = searchFilter.getIndexFieldName() + ".year";
                                        String yearUTC = DateFormatUtils.formatUTC(value_dt, "yyyy");
										doc.addField(searchFilter.getIndexFieldName() + "_keyword", yearUTC);
										// add the year to the autocomplete index
										doc.addField(searchFilter.getIndexFieldName() + "_ac", yearUTC);
										doc.addField(indexField, yearUTC);

                                    	if (yearUTC.startsWith("0"))
                                        {
        									doc.addField(
        											searchFilter.getIndexFieldName()
        													+ "_keyword",
        													yearUTC.replaceFirst("0*", ""));
        									// add date without starting zeros for autocomplete e filtering
        									doc.addField(
        											searchFilter.getIndexFieldName()
        													+ "_ac",
        													yearUTC.replaceFirst("0*", ""));
        									doc.addField(
        											searchFilter.getIndexFieldName()
        													+ "_ac",
        													value.replaceFirst("0*", ""));
        									doc.addField(
        											searchFilter.getIndexFieldName()
        													+ "_keyword",
        													value.replaceFirst("0*", ""));
                                        }

                                    	//Also save a sort value of this year, this is required for determining the upper & lower bound year of our facet
                                        if(doc.getField(indexField + "_sort") == null)
                                        {
                                        	//We can only add one year so take the first one
                                        	doc.addField(indexField + "_sort", yearUTC);
                                    	}
                                }
                            }else
                            if(searchFilter.getType().equals(DiscoveryConfigurationParameters.TYPE_HIERARCHICAL))
                            {
                                HierarchicalSidebarFacetConfiguration hierarchicalSidebarFacetConfiguration = (HierarchicalSidebarFacetConfiguration) searchFilter;
                                String[] subValues = value.split(hierarchicalSidebarFacetConfiguration.getSplitter());
                                // Lan 24.02.2015 : skip first node even the following node is null
                                // if(hierarchicalSidebarFacetConfiguration.isSkipFirstNodeLevel() && 1 < subValues.length)
                                if(hierarchicalSidebarFacetConfiguration.isSkipFirstNodeLevel() && 1 <= subValues.length)
                                {
                                    //Remove the first element of our array
                                    subValues = (String[]) ArrayUtils.subarray(subValues, 1, subValues.length);
                                }
                                for (int i = 0; i < subValues.length; i++)
                                {
                                    StringBuilder valueBuilder = new StringBuilder();
                                    for(int j = 0; j <= i; j++)
                                    {
                                        valueBuilder.append(subValues[j]);
                                        if(j < i)
                                        {
                                            valueBuilder.append(hierarchicalSidebarFacetConfiguration.getSplitter());
                                        }
                                    }

                                    String indexValue = valueBuilder.toString().trim();
                                    doc.addField(searchFilter.getIndexFieldName() + "_tax_" + i + "_filter", indexValue.toLowerCase() + separator + indexValue);
                                    //We add the field x times that it has occurred
                                    for(int j = i; j < subValues.length; j++)
                                    {
                                        doc.addField(searchFilter.getIndexFieldName() + "_filter", indexValue.toLowerCase() + separator + indexValue);
                                        doc.addField(searchFilter.getIndexFieldName() + "_keyword", indexValue);
                                    }
                                }
                            }
                        }
                    }
                    
                    // Value has been changed inside this block and should be ignored
                    if (value == null)
                    {
                        continue;
                    }
                }

                if ((sortFields.get(field) != null || recentSubmissionsConfigurationMap.get(field) != null) && !sortFieldsAdded.contains(field))
                {
                    //Only add sort value once
                    String type;
                    if(sortFields.get(field) != null)
                    {
                        type = sortFields.get(field).getType();
                    }else{
                        type = recentSubmissionsConfigurationMap.get(field).getType();
                    }

                    // 09.03.2015 Lan : <field>_sort for date contains date AND time, not only date
                    if(type.equals(DiscoveryConfigurationParameters.TYPE_DATE))
                    {
                    	if (value_dt == null) {
                    		value_dt = MultiFormatDateParser.parse(value);
                    	}
                        if(value_dt != null)
                        {
                        	String sort_dt = DateFormatUtils.ISO_DATETIME_FORMAT.format(value_dt);
                            doc.addField(field + "_sort", sort_dt);
                        }else{
                            log.warn("Error while indexing sort date field, item: " + item.getHandle() + " metadata field: " + field + " date value: " + value);
                        }
                    }else{
                        doc.addField(field + "_sort", value);
                    }
                    sortFieldsAdded.add(field);
                }

                /* Lan 08.01.2016 - dont need distinct field *_hl anymore
                if(hitHighlightingFields.contains(field) || hitHighlightingFields.contains("*") || hitHighlightingFields.contains(unqualifiedField + "." + Item.ANY))
                {
                    doc.addField(field + "_hl", value);
                }
                */

                if(moreLikeThisFields.contains(field) || moreLikeThisFields.contains(unqualifiedField + "." + Item.ANY))
                {
                    doc.addField(field + "_mlt", value);
                }

                /*
                 * Lan 25.07.2016 : add for full text search
                 */
                if (toCatchAllMetadataFields != null	&& (toCatchAllMetadataFields.contains(field) || toCatchAllMetadataFields.contains(unqualifiedField + "." + Item.ANY)))
                {
                	doc.addField("catched_" + field, value);
                }
                
                doc.addField(field, value);
                
                if (toProjectionFields.contains(field) || toProjectionFields.contains(unqualifiedField + "." + Item.ANY))
                {
                    StringBuffer variantsToStore = new StringBuffer();
                    if (variants != null)
                    {
                        for (String var : variants)
                        {
                            variantsToStore.append(VARIANTS_STORE_SEPARATOR);
                            variantsToStore.append(var);
                        }
                    }
                    doc.addField(
                            field + "_stored",
                            value + STORE_SEPARATOR + preferedLabel
                                    + STORE_SEPARATOR
                                    + (variantsToStore.length() > VARIANTS_STORE_SEPARATOR
                                            .length() ? variantsToStore
                                            .substring(VARIANTS_STORE_SEPARATOR
                                                    .length()) : "null")
                                    + STORE_SEPARATOR + authority
                                    + STORE_SEPARATOR + meta.language);
                }

                if (meta.language != null && !meta.language.trim().equals(""))
                {
                    String langField = field + "." + meta.language;
                    doc.addField(langField, value);
                }
            }

        } catch (Exception e)  {
            log.error(e.getMessage(), e);
        }


        log.debug("  Added Metadata");

        try {

            Metadatum[] values = item.getMetadataByMetadataString("dc.relation.ispartof");

            if(values != null && values.length > 0 && values[0] != null && values[0].value != null)
            {
                // group on parent
                String handlePrefix = ConfigurationManager.getProperty("handle.canonical.prefix");
                if (handlePrefix == null || handlePrefix.length() == 0)
                {
                    handlePrefix = "http://hdl.handle.net/";
                }

                doc.addField("publication_grp",values[0].value.replaceFirst(handlePrefix,"") );

            }
            else
            {
                // group on self
                doc.addField("publication_grp", item.getHandle());
            }

        } catch (Exception e)
        {
            log.error(e.getMessage(),e);
        }


        log.debug("  Added Grouping");



        List<BitstreamContentStream> streams = new ArrayList<BitstreamContentStream>();

        try {
            // now get full text of any bitstreams in the TEXT bundle
            // trundle through the bundles
            Bundle[] myBundles = item.getBundles();

            for (Bundle myBundle : myBundles)
            {
                if ((myBundle.getName() != null)
                        && myBundle.getName().equals("TEXT"))
                {
                    // a-ha! grab the text out of the bitstreams
                    Bitstream[] myBitstreams = myBundle.getBitstreams();

                    for (Bitstream myBitstream : myBitstreams)
                    {
                        try {

                            streams.add(new BitstreamContentStream(myBitstream));

                            log.debug("  Added BitStream: "
                                    + myBitstream.getStoreNumber() + "	"
                                    + myBitstream.getSequenceID() + "   "
                                    + myBitstream.getName());

                        } catch (Exception e)
                        {
                            // this will never happen, but compiler is now
                            // happy.
                            log.trace(e.getMessage(), e);
                        }
                    }
                }
            }
        } catch (RuntimeException e)
        {
            log.error(e.getMessage(), e);
        }
        
        /* Lan 20.06.2016 : index related code_origine of supports
         * Lan 26.07.2015 : filter removed
        try {
        	CodeOrigine[] codeOrigines = ItemAdd.CodeOrigineItem.findById(context, item.getID());
        	for (CodeOrigine codeOrigine : codeOrigines) {
        		buildDocument(context, codeOrigine);
                log.info("Wrote CodeOrigine: " + codeOrigine.getCode() + " to Index");
			}

        	log.debug("Index all code_origine of item " + item.getID());
        } catch (RuntimeException e)
        {
            log.error(e.getMessage(), e);
        }
        */


        //Do any additional indexing, depends on the plugins
        List<SolrServiceIndexPlugin> solrServiceIndexPlugins = new DSpace().getServiceManager().getServicesByType(SolrServiceIndexPlugin.class);
        for (SolrServiceIndexPlugin solrServiceIndexPlugin : solrServiceIndexPlugins)
        {
            solrServiceIndexPlugin.additionalIndex(context, item, doc);
        }
        
        // write the index and close the inputstreamreaders
        try {
            writeDocument(doc, streams);
            log.info("Wrote Item: " + handle + " to Index");
        } catch (RuntimeException e)
        {
            log.error("Error while writing item to discovery index: " + handle + " message:"+ e.getMessage(), e);
        }
        
        
    }

    /**
     * Create Lucene document with all the shared fields initialized.
     *
     * @param type      Type of DSpace Object
     * @param id
     * @param handle
     * @param locations @return
     */
    protected SolrInputDocument buildDocument(int type, int id, String handle,
            List<String> locations)
    {
    	return buildDocument(type, id, type+"-"+id, handle, locations);    	
    }

    protected SolrInputDocument buildDocument(int type, int id, String uniqueId, String handle,
                                            List<String> locations)
    {
        SolrInputDocument doc = new SolrInputDocument();

        // want to be able to check when last updated
        // (not tokenized, but it is indexed)
        doc.addField(LAST_INDEXED_FIELD, new Date());

        // New fields to weaken the dependence on handles, and allow for faster
        // list display
		doc.addField("search.uniqueid", uniqueId);
        doc.addField("search.resourcetype", Integer.toString(type));

        doc.addField("search.resourceid", Integer.toString(id));

        // want to be able to search for handle, so use keyword
        // (not tokenized, but it is indexed)
        if (handle != null)
        {
            // want to be able to search for handle, so use keyword
            // (not tokenized, but it is indexed)
            doc.addField("handle", handle);
        }

        if (locations != null)
        {
            for (String location : locations)
            {
            	doc.addField("location", location);
            	if (location.startsWith("om")) {
            		doc.addField("owning_community", Constants.COMMUNITY+"-"+location.substring(2));
            	}
            	else if (location.startsWith("ol")) {
            		doc.addField("owning_collection", Constants.COLLECTION+"-"+location.substring(2));
            	}
            	else if (location.startsWith("m")) {
            		doc.addField("location.comm", location.substring(1));
            		doc.addField("location.community", Constants.COMMUNITY+"-"+location.substring(1));
            	}
            	else if (location.startsWith("l")) {
            		doc.addField("location.coll", location.substring(1));
            		doc.addField("location.collection", Constants.COLLECTION+"-"+location.substring(1));
            	}
            }
        }

        return doc;
    }

    /**
     * Helper function to retrieve a date using a best guess of the potential
     * date encodings on a field
     *
     * @param t the string to be transformed to a date
     * @return a date if the formatting was successful, null if not able to transform to a date
     */
    public static Date toDate(String t)
    {
        SimpleDateFormat[] dfArr;

        // Choose the likely date formats based on string length
        switch (t.length())
        {
			// case from 1 to 3 go through adding anyone a single 0. Case 4 define
			// for all the SimpleDateFormat
        	case 1:
        		t = "0" + t;
        	case 2:
        		t = "0" + t;
        	case 3:
        		t = "0" + t;
            case 4:
                dfArr = new SimpleDateFormat[]{new SimpleDateFormat("yyyy")};
                break;
            case 6:
                dfArr = new SimpleDateFormat[]{new SimpleDateFormat("yyyyMM")};
                break;
            case 7:
                dfArr = new SimpleDateFormat[]{new SimpleDateFormat("yyyy-MM")};
                break;
            case 8:
                dfArr = new SimpleDateFormat[]{new SimpleDateFormat("yyyyMMdd"),
                        new SimpleDateFormat("yyyy MMM")};
                break;
            case 10:
                dfArr = new SimpleDateFormat[]{new SimpleDateFormat("yyyy-MM-dd")};
                break;
            case 11:
                dfArr = new SimpleDateFormat[]{new SimpleDateFormat("yyyy MMM dd")};
                break;
            case 20:
                dfArr = new SimpleDateFormat[]{new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss'Z'")};
                break;
            default:
                dfArr = new SimpleDateFormat[]{new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")};
                break;
        }

        for (SimpleDateFormat df : dfArr)
        {
            try {
                // Parse the date
                df.setCalendar(Calendar
                        .getInstance(TimeZone.getTimeZone("UTC")));
                df.setLenient(false);
                return df.parse(t);
            } catch (ParseException pe)
            {
                log.error("Unable to parse date format", pe);
            }
        }

        return null;
    }

    public static String locationToName(Context context, String field, String value) throws SQLException {
        if("location.comm".equals(field) || "location.coll".equals(field))
        {
            int type = field.equals("location.comm") ? Constants.COMMUNITY : Constants.COLLECTION;
            DSpaceObject commColl = DSpaceObject.find(context, type, Integer.parseInt(value));
            if(commColl != null)
            {
                return commColl.getName();
            }

        }
        return value;
    }

    //******** SearchService implementation
    @Override
    public DiscoverResult search(Context context, DiscoverQuery query) throws SearchServiceException
    {
        return search(context, query, false);
    }

    @Override
    public DiscoverResult search(Context context, DSpaceObject dso,
            DiscoverQuery query)
            throws SearchServiceException
    {
        return search(context, dso, query, false);
    }

    public DiscoverResult search(Context context, DSpaceObject dso, DiscoverQuery discoveryQuery, boolean includeUnDiscoverable) throws SearchServiceException {
        if(dso != null)
        {
            if (dso instanceof Community)
            {
                discoveryQuery.addFilterQueries("location:m" + dso.getID());
            } else if (dso instanceof Collection)
            {
                discoveryQuery.addFilterQueries("location:l" + dso.getID());
            } else if (dso instanceof Item)
            {
                discoveryQuery.addFilterQueries("handle:" + dso.getHandle());
            }
        }
        return search(context, discoveryQuery, includeUnDiscoverable);

    }


    public DiscoverResult search(Context context, DiscoverQuery discoveryQuery, boolean includeUnDiscoverable) throws SearchServiceException {
        try {
            if(getSolr() == null){
                return new DiscoverResult();
            }
            SolrQuery solrQuery = resolveToSolrQuery(context, discoveryQuery, includeUnDiscoverable);


            QueryResponse queryResponse = getSolr().query(solrQuery);
            return retrieveResult(context, discoveryQuery, queryResponse);

        } catch (Exception e)
        {
            throw new org.dspace.discovery.SearchServiceException(e.getMessage(),e);
        }
    }

    protected SolrQuery resolveToSolrQuery(Context context, DiscoverQuery discoveryQuery, boolean includeUnDiscoverable)
    {
        SolrQuery solrQuery = new SolrQuery();

        String query = "*:*";
        if(discoveryQuery.getQuery() != null)
        {
        	query = discoveryQuery.getQuery();
		}

        solrQuery.setQuery(query);
        if(discoveryQuery.isSpellCheck())
        {
            solrQuery.setParam(SpellingParams.SPELLCHECK_Q, query);
            solrQuery.setParam(SpellingParams.SPELLCHECK_COLLATE, Boolean.TRUE);
            solrQuery.setParam("spellcheck", Boolean.TRUE);
        }

        if (!includeUnDiscoverable)
        {
        	solrQuery.addFilterQuery("NOT(withdrawn:true)");
        	solrQuery.addFilterQuery("NOT(discoverable:false)");
		}

        for (int i = 0; i < discoveryQuery.getFilterQueries().size(); i++)
        {
            String filterQuery = discoveryQuery.getFilterQueries().get(i);
            solrQuery.addFilterQuery(filterQuery);
        }
        if(discoveryQuery.getDSpaceObjectFilter() != -1)
        {
            solrQuery.addFilterQuery("search.resourcetype:" + discoveryQuery.getDSpaceObjectFilter());
        }

        for (int i = 0; i < discoveryQuery.getFieldPresentQueries().size(); i++)
        {
            String filterQuery = discoveryQuery.getFieldPresentQueries().get(i);
            solrQuery.addFilterQuery(filterQuery + ":[* TO *]");
        }

        if(discoveryQuery.getStart() != -1)
        {
            solrQuery.setStart(discoveryQuery.getStart());
        }

        if(discoveryQuery.getMaxResults() != -1)
        {
            solrQuery.setRows(discoveryQuery.getMaxResults());
        }

        if(discoveryQuery.getSortField() != null)
        {
            SolrQuery.ORDER order = SolrQuery.ORDER.asc;
            if(discoveryQuery.getSortOrder().equals(DiscoverQuery.SORT_ORDER.desc))
                order = SolrQuery.ORDER.desc;

            solrQuery.addSortField(discoveryQuery.getSortField(), order);
        }

        for(String property : discoveryQuery.getProperties().keySet())
        {
            List<String> values = discoveryQuery.getProperties().get(property);
            
            switch (property) {
			case "qt":
	            // Lan 15.12.2015 : process "qt" property especially; set by rtbf-rest
	            // use "qt" to choose another RequestHandler than /select
			case "collapse.field":
	            // Lan 23.09.2016 : process "collapse.field" property especially; set by xmlui
	            // use "collapse_field" to choose another RequestHandler than /select which is the default
            	solrQuery.setRequestHandler(values.get(0));				
				break;
			default:
                solrQuery.add(property, values.toArray(new String[values.size()]));
				break;
			}
            
        }

        List<DiscoverFacetField> facetFields = discoveryQuery.getFacetFields();
        if(0 < facetFields.size())
        {
            //Only add facet information if there are any facets
            for (DiscoverFacetField facetFieldConfig : facetFields)
            {
                String field = transformFacetField(facetFieldConfig, facetFieldConfig.getField(), false);
                solrQuery.addFacetField(field);
                
                // Lan 22.12.2015 : get rid of local parameter in field
                String[] lpField = field.split("\\{.+\\}");
                if (lpField.length > 1) { field = lpField[1]; }

                // Setting the facet limit in this fashion ensures that each facet can have its own max
                solrQuery.add("f." + field + "." + FacetParams.FACET_LIMIT, String.valueOf(facetFieldConfig.getLimit()));
                String facetSort;
                if(DiscoveryConfigurationParameters.SORT.COUNT.equals(facetFieldConfig.getSortOrder()))
                {
                    facetSort = FacetParams.FACET_SORT_COUNT;
                }else{
                    facetSort = FacetParams.FACET_SORT_INDEX;
                }
                solrQuery.add("f." + field + "." + FacetParams.FACET_SORT, facetSort);
                if (facetFieldConfig.getOffset() != -1)
                {
                    solrQuery.setParam("f." + field + "."
                            + FacetParams.FACET_OFFSET,
                            String.valueOf(facetFieldConfig.getOffset()));
                }
                if(facetFieldConfig.getPrefix() != null)
                {
                    solrQuery.setFacetPrefix(field, facetFieldConfig.getPrefix());
                }
            }

            List<String> facetQueries = discoveryQuery.getFacetQueries();
            for (String facetQuery : facetQueries)
            {
                solrQuery.addFacetQuery(facetQuery);
            }

            if(discoveryQuery.getFacetMinCount() != -1)
            {
                solrQuery.setFacetMinCount(discoveryQuery.getFacetMinCount());
            }

            solrQuery.setParam(FacetParams.FACET_OFFSET, String.valueOf(discoveryQuery.getFacetOffset()));
        }
        
        // Lan 21.03.2017 : support facet pivot from rtbf-rest
        List<String> facetPivotFields = discoveryQuery.getFacetPivotFields();
        if(0 < facetPivotFields.size()) {
        	for (String pivot : facetPivotFields) {
            	solrQuery.addFacetPivotField(pivot);
			}
        }
        

/*
        if(0 < discoveryQuery.getHitHighlightingFields().size())
        {
            solrQuery.setHighlight(true);
            solrQuery.add(HighlightParams.USE_PHRASE_HIGHLIGHTER, Boolean.TRUE.toString());
            for (DiscoverHitHighlightingField highlightingField : discoveryQuery.getHitHighlightingFields())
            {
                solrQuery.addHighlightField(highlightingField.getField() + "_hl");
                solrQuery.add("f." + highlightingField.getField() + "_hl." + HighlightParams.FRAGSIZE, String.valueOf(highlightingField.getMaxChars()));
                solrQuery.add("f." + highlightingField.getField() + "_hl." + HighlightParams.SNIPPETS, String.valueOf(highlightingField.getMaxSnippets()));
            }

        }
*/
        // Highlight text fields , donot have distinct field *_hl for highlight anymore
        if ( discoveryQuery.getHitHighlightingFields().size() > 0) {
            solrQuery.setHighlight(true);
            solrQuery.add(HighlightParams.USE_PHRASE_HIGHLIGHTER, Boolean.TRUE.toString());
            for (DiscoverHitHighlightingField highlightingField : discoveryQuery.getHitHighlightingFields())
            {
                solrQuery.addHighlightField(highlightingField.getField());
                solrQuery.add("f." + highlightingField.getField() + "." + HighlightParams.FRAGSIZE, String.valueOf(highlightingField.getMaxChars()));
                solrQuery.add("f." + highlightingField.getField() + "." + HighlightParams.SNIPPETS, String.valueOf(highlightingField.getMaxSnippets()));
            }        	
        }
        
                
        //Add any configured search plugins !
        List<SolrServiceSearchPlugin> solrServiceSearchPlugins = new DSpace().getServiceManager().getServicesByType(SolrServiceSearchPlugin.class);
        for (SolrServiceSearchPlugin searchPlugin : solrServiceSearchPlugins)
        {
            searchPlugin.additionalSearchParameters(context, discoveryQuery, solrQuery);
        }
        return solrQuery;
    }

    @Override
    public InputStream searchJSON(Context context, DiscoverQuery query, DSpaceObject dso, String jsonIdentifier) throws SearchServiceException {
        if(dso != null)
        {
            if (dso instanceof Community)
            {
                query.addFilterQueries("location:m" + dso.getID());
            } else if (dso instanceof Collection)
            {
                query.addFilterQueries("location:l" + dso.getID());
            } else if (dso instanceof Item)
            {
                query.addFilterQueries("handle:" + dso.getHandle());
            }
        }
        return searchJSON(context, query, jsonIdentifier);
    }


    public InputStream searchJSON(Context context, DiscoverQuery discoveryQuery, String jsonIdentifier) throws SearchServiceException {
        if(getSolr() == null)
        {
            return null;
        }

        SolrQuery solrQuery = resolveToSolrQuery(context, discoveryQuery, false);
        //We use json as out output type
        solrQuery.setParam("json.nl", "map");
        solrQuery.setParam("json.wrf", jsonIdentifier);
        solrQuery.setParam(CommonParams.WT, "json");

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(getSolr().getBaseURL()).append("/select?");
        urlBuilder.append(solrQuery.toString());

        try {
            HttpGet get = new HttpGet(urlBuilder.toString());
            HttpResponse response = new DefaultHttpClient().execute(get);
            return response.getEntity().getContent();

        } catch (Exception e)
        {
            log.error("Error while getting json solr result for discovery search recommendation", e);
        }
        return null;
    }

    protected DiscoverResult retrieveResult(Context context, DiscoverQuery query, QueryResponse solrQueryResponse) throws SQLException {
        DiscoverResult result = new DiscoverResult();

        if(solrQueryResponse != null)
        {
            if (solrQueryResponse.getResults() == null) { return retrieveGroup(context, query, solrQueryResponse); }

            result.setSearchTime(solrQueryResponse.getQTime());
            result.setStart(query.getStart());
            result.setMaxResults(query.getMaxResults());
            result.setTotalSearchResults(solrQueryResponse.getResults().getNumFound());

            List<String> searchFields = query.getSearchFields();
            for (SolrDocument doc : solrQueryResponse.getResults())
            {
                DSpaceObject dso = findDSpaceObject(context, doc);

                if(dso != null)
                {
                    result.addDSpaceObject(dso);
                } else {
                    log.error(LogManager.getHeader(context, "Error while retrieving DSpace object from discovery index", "Handle: " + doc.getFirstValue("handle")));
                    continue;
                }

                DiscoverResult.SearchDocument resultDoc = new DiscoverResult.SearchDocument();
                //Lan : Add also handle
                resultDoc.addSearchField("handle", (String) doc.getFieldValue("handle"));

                //18.04.2016 Lan : fiels that are different among dup items
                resultDoc.addSearchField("dup_uniqueid", (String) doc.getFieldValue("search.uniqueid"));
                resultDoc.addSearchField("dup_owning_collection", (String) doc.getFieldValue("owning_collection"));                
                resultDoc.addSearchField("dup_date_issued", (doc.getFieldValue("date_issued_dt") == null) ? null : DateFormatUtils.formatUTC((Date) doc.getFieldValue("date_issued_dt"),"yyyy-MM-dd'T'HH:mm:ss'Z'"));
                // resultDoc.addSearchField("dup_channel_issued", (String) doc.getFieldValue("rtbf.channel_issued")); // singlevalue
                java.util.Collection<Object> manyValues = doc.getFieldValues("rtbf.channel_issued");
                if (manyValues != null) {
                	resultDoc.addSearchField("dup_channel_issued", manyValues.toArray(new String[manyValues.size()])); // multivalued
                }

                //Add information about our search fields
                for (String field : searchFields)
                {
                    List<String> valuesAsString = new ArrayList<String>();
                	java.util.Collection<Object> fieldValues = doc.getFieldValues(field);
                	if (fieldValues != null) {
	                    for (Object o : fieldValues)
	                    {
	                        valuesAsString.add(String.valueOf(o));
	                    }
	                    resultDoc.addSearchField(field, valuesAsString.toArray(new String[valuesAsString.size()]));
                	}
                }
                result.addSearchDocument(dso, resultDoc);

                if(solrQueryResponse.getHighlighting() != null)
                {
                    Map<String, List<String>> highlightedFields = solrQueryResponse.getHighlighting().get(dso.getType() + "-" + dso.getID());
                    if(MapUtils.isNotEmpty(highlightedFields))
                    {
                        //We need to remove all the "_hl" appendix strings from our keys
                        Map<String, List<String>> resultMap = new HashMap<String, List<String>>();
                        for(String key : highlightedFields.keySet())
                        {
                        	/* Lan 08.01.2015 - suffix _hl may or may not exists */
                        	int ihl = key.lastIndexOf("_hl");
                        	if (ihl < 0) { /* no _hl suffix */
                        		resultMap.put(key, highlightedFields.get(key));
                        	} else { /* remove _hl suffix */
                        		resultMap.put(key.substring(0, ihl), highlightedFields.get(key));
                        	}
                        }

                        result.addHighlightedResult(dso, new DiscoverResult.DSpaceObjectHighlightResult(dso, resultMap));
                    }
                }
                
                /* Lan 11.04.2015 - expanded */
                if (solrQueryResponse.getExpandedResults() != null) {
                    String identifier_origin = (String) doc.getFirstValue("identifier_origin");
                    SolrDocumentList expandedResults = solrQueryResponse.getExpandedResults().get(identifier_origin);
                    if (expandedResults != null) {
                        for (SolrDocument docE : expandedResults) {
                            DiscoverResult.SearchDocument resultDocE = new DiscoverResult.SearchDocument();
                            // Add handle
                            resultDocE.addSearchField("handle", String.valueOf(docE.getFieldValue("handle")));
                            // Add other metadata returned from solr
                            for (String fieldName : searchFields) {
	                            resultDocE.addSearchField(fieldName, String.valueOf(docE.getFirstValue(fieldName)));
	                        }
	                        result.addExpandDocument(dso, resultDocE);                        
                        }                        
                    }
                }
                
            }

            //Resolve our facet field values
            List<FacetField> facetFields = solrQueryResponse.getFacetFields();
            if(facetFields != null)
            {
                for (int i = 0; i <  facetFields.size(); i++)
                {
                    FacetField facetField = facetFields.get(i);
                    DiscoverFacetField facetFieldConfig = query.getFacetFields().get(i);
                    List<FacetField.Count> facetValues = facetField.getValues();
                    if (facetValues != null)
                    {
                        if(facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE) && facetFieldConfig.getSortOrder().equals(DiscoveryConfigurationParameters.SORT.VALUE))
                        {
                            //If we have a date & are sorting by value, ensure that the results are flipped for a proper result
                           Collections.reverse(facetValues);
                        }

                        for (FacetField.Count facetValue : facetValues)
                        {
                            String displayedValue = transformDisplayedValue(context, facetField.getName(), facetValue.getName());
                            String field = transformFacetField(facetFieldConfig, facetField.getName(), true);
                            String authorityValue = transformAuthorityValue(context, facetField.getName(), facetValue.getName());
                            String sortValue = transformSortValue(context, facetField.getName(), facetValue.getName());
                            String filterValue = displayedValue;
                            if (StringUtils.isNotBlank(authorityValue))
                            {
                                filterValue = authorityValue;
                            }
                            result.addFacetResult(
                                    field,
                                    new DiscoverResult.FacetResult(filterValue,
                                            displayedValue, authorityValue,
                                            sortValue, facetValue.getCount()));
                        }
                    }
                }
            }

            if(solrQueryResponse.getFacetQuery() != null)
            {
				// just retrieve the facets in the order they where requested!
				// also for the date we ask it in proper (reverse) order
				// At the moment facet queries are only used for dates
                LinkedHashMap<String, Integer> sortedFacetQueries = new LinkedHashMap<String, Integer>(solrQueryResponse.getFacetQuery());
                for(String facetQuery : sortedFacetQueries.keySet())
                {
                    //TODO: do not assume this, people may want to use it for other ends, use a regex to make sure
                    //We have a facet query, the values looks something like: dateissued.year:[1990 TO 2000] AND -2000
                    //Prepare the string from {facet.field.name}:[startyear TO endyear] to startyear - endyear
                    String facetField = facetQuery.substring(0, facetQuery.indexOf(":"));
                    String name = "";
                    String filter = "";
                    if (facetQuery.indexOf('[') > -1 && facetQuery.lastIndexOf(']') > -1)
                    {
                        name = facetQuery.substring(facetQuery.indexOf('[') + 1);
                        name = name.substring(0, name.lastIndexOf(']')).replaceAll("TO", "-");
                        filter = facetQuery.substring(facetQuery.indexOf('['));
                        filter = filter.substring(0, filter.lastIndexOf(']') + 1);
                    }

                    Integer count = sortedFacetQueries.get(facetQuery);

                    //No need to show empty years
                    if(0 < count)
                    {
                        result.addFacetResult(facetField, new DiscoverResult.FacetResult(filter, name, null, name, count));
                    }
                }
            }
            
            // Lan 21.02.2017
            // Get facet.pivot results            
            NamedList<List<PivotField>> pivotFields = solrQueryResponse.getFacetPivot();
            if (pivotFields != null) {
            	for (int i = 0; i <  pivotFields.size(); i++) { // presently size is 1
                String pvfName = pivotFields.getName(i); // channel_tax_0_filter,channel_keyword
            	for (PivotField pv1 : pivotFields.getVal(i)) {
            		String pv1_name = pv1.getField(); // channel_tax_0_filter
            		String pv1_value = pv1.getValue().toString();
            		int pv1_count = pv1.getCount();
            		String pv1_displayed = transformDisplayedValue(context, pv1_name, pv1_value);
            		
        			DiscoverResult.FacetResult pv1facet = new DiscoverResult.FacetResult(pv1_value, pv1_displayed, null, pv1_value, pv1_count);
        			result.addFacetResult(pvfName+":"+pv1_name, pv1facet);

            		List<PivotField> pv2_list = pv1.getPivot();
            		for (int j = 0, jlen = pv2_list.size(); j < jlen; j++) {
            			PivotField pv2 = pv2_list.get(j);
                		String pv2_name = pv2.getField(); // channel_keyword
                		String pv2_value = pv2.getValue().toString();
                		int pv2_count = pv2.getCount();
                		
            			pv1facet.addSubFacet(pv2_name, new DiscoverResult.FacetResult(pv2_value, pv2_value, null, pv2_value, pv2_count));
            		}
            	}
					
				}
            	
            }

            // Get the first collation suggested
            if(solrQueryResponse.getSpellCheckResponse() != null)
            {
                String recommendedQuery = solrQueryResponse.getSpellCheckResponse().getCollatedResult();
                if(StringUtils.isNotBlank(recommendedQuery))
                {
                    result.setSpellCheckQuery(recommendedQuery);
                }
            }
            
            // Get all collations suggested
            if(solrQueryResponse.getSpellCheckResponse() != null)
            {
                if (solrQueryResponse.getSpellCheckResponse().getCollatedResults() != null) {
	                List<String> recommendedQueries = new ArrayList<String>();
	                for (Collation collation : solrQueryResponse.getSpellCheckResponse().getCollatedResults()) {
						recommendedQueries.add(collation.getCollationQueryString());
					}
	
	                if (!recommendedQueries.isEmpty()) {
	                    result.setCollations(recommendedQueries);
	                }
                }
            }

        }

        return result;
    }

    protected DiscoverResult retrieveGroup(Context context, DiscoverQuery query, QueryResponse solrQueryResponse) throws SQLException {
        DiscoverResult result = new DiscoverResult();

        if(solrQueryResponse != null)
        {
            if (solrQueryResponse.getGroupResponse() == null) { return null; }

            result.setSearchTime(solrQueryResponse.getQTime());
            result.setStart(query.getStart());
            result.setMaxResults(query.getMaxResults());

            GroupCommand group1 = solrQueryResponse.getGroupResponse().getValues().get(0);
            String groupName = group1.getName();
            
            result.setTotalSearchResults(group1.getNGroups());
            
            for (Group group : group1.getValues())
            {
            	String groupValue = group.getGroupValue();
            	
            	DSpaceObject dso = findDSpaceObject(context, groupValue);

                if(dso != null)
                {
                    result.addDSpaceObject(dso);
                } else {
                    log.error(LogManager.getHeader(context, "Error while retrieving DSpace object from discovery index", "uniqueid: " + groupValue));
                    continue;
                }
                
            	SolrDocument doc = group.getResult().get(0);
                long numFound = group.getResult().getNumFound();
                if (groupValue.equals(doc.getFieldValue("search.uniqueid"))) {
                	numFound--;
                }
                result.addGroupFilter(dso, new GroupFilter(groupName, groupValue, numFound));

                if(solrQueryResponse.getHighlighting() != null)
                {
                    Map<String, List<String>> highlightedFields = solrQueryResponse.getHighlighting().get(dso.getType() + "-" + dso.getID());
                    if(MapUtils.isNotEmpty(highlightedFields))
                    {
                        //We need to remove all the "_hl" appendix strings from our keys
                        Map<String, List<String>> resultMap = new HashMap<String, List<String>>();
                        for(String key : highlightedFields.keySet())
                        {
                        	/* Lan 08.01.2015 - suffix _hl may or may not exists */
                        	int ihl = key.lastIndexOf("_hl");
                        	if (ihl < 0) { /* no _hl suffix */
                        		resultMap.put(key, highlightedFields.get(key));
                        	} else { /* remove _hl suffix */
                        		resultMap.put(key.substring(0, ihl), highlightedFields.get(key));
                        	}
                        }

                        result.addHighlightedResult(dso, new DiscoverResult.DSpaceObjectHighlightResult(dso, resultMap));
                    }
                }
                                
            }

            //Resolve our facet field values
            List<FacetField> facetFields = solrQueryResponse.getFacetFields();
            if(facetFields != null)
            {
                for (int i = 0; i <  facetFields.size(); i++)
                {
                    FacetField facetField = facetFields.get(i);
                    DiscoverFacetField facetFieldConfig = query.getFacetFields().get(i);
                    List<FacetField.Count> facetValues = facetField.getValues();
                    if (facetValues != null)
                    {
                        if(facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE) && facetFieldConfig.getSortOrder().equals(DiscoveryConfigurationParameters.SORT.VALUE))
                        {
                            //If we have a date & are sorting by value, ensure that the results are flipped for a proper result
                           Collections.reverse(facetValues);
                        }

                        for (FacetField.Count facetValue : facetValues)
                        {
                            String displayedValue = transformDisplayedValue(context, facetField.getName(), facetValue.getName());
                            String field = transformFacetField(facetFieldConfig, facetField.getName(), true);
                            String authorityValue = transformAuthorityValue(context, facetField.getName(), facetValue.getName());
                            String sortValue = transformSortValue(context, facetField.getName(), facetValue.getName());
                            String filterValue = displayedValue;
                            if (StringUtils.isNotBlank(authorityValue))
                            {
                                filterValue = authorityValue;
                            }
                            result.addFacetResult(
                                    field,
                                    new DiscoverResult.FacetResult(filterValue,
                                            displayedValue, authorityValue,
                                            sortValue, facetValue.getCount()));
                        }
                    }
                }
            }

            if(solrQueryResponse.getFacetQuery() != null)
            {
				// just retrieve the facets in the order they where requested!
				// also for the date we ask it in proper (reverse) order
				// At the moment facet queries are only used for dates
                LinkedHashMap<String, Integer> sortedFacetQueries = new LinkedHashMap<String, Integer>(solrQueryResponse.getFacetQuery());
                for(String facetQuery : sortedFacetQueries.keySet())
                {
                    //TODO: do not assume this, people may want to use it for other ends, use a regex to make sure
                    //We have a facet query, the values looks something like: dateissued.year:[1990 TO 2000] AND -2000
                    //Prepare the string from {facet.field.name}:[startyear TO endyear] to startyear - endyear
                    String facetField = facetQuery.substring(0, facetQuery.indexOf(":"));
                    String name = "";
                    String filter = "";
                    if (facetQuery.indexOf('[') > -1 && facetQuery.lastIndexOf(']') > -1)
                    {
                        name = facetQuery.substring(facetQuery.indexOf('[') + 1);
                        name = name.substring(0, name.lastIndexOf(']')).replaceAll("TO", "-");
                        filter = facetQuery.substring(facetQuery.indexOf('['));
                        filter = filter.substring(0, filter.lastIndexOf(']') + 1);
                    }

                    Integer count = sortedFacetQueries.get(facetQuery);

                    //No need to show empty years
                    if(0 < count)
                    {
                        result.addFacetResult(facetField, new DiscoverResult.FacetResult(filter, name, null, name, count));
                    }
                }
            }

            // Get all collations suggested
            if(solrQueryResponse.getSpellCheckResponse() != null)
            {
                String recommendedQuery = solrQueryResponse.getSpellCheckResponse().getCollatedResult();
                if(StringUtils.isNotBlank(recommendedQuery))
                {
                    result.setSpellCheckQuery(recommendedQuery);
                }
            }

            // Get all collations suggested
            if(solrQueryResponse.getSpellCheckResponse() != null)
            {
                if (solrQueryResponse.getSpellCheckResponse().getCollatedResults() != null) {
	                List<String> recommendedQueries = new ArrayList<String>();
	                for (Collation collation : solrQueryResponse.getSpellCheckResponse().getCollatedResults()) {
						recommendedQueries.add(collation.getCollationQueryString());
					}
	
	                if (!recommendedQueries.isEmpty()) {
	                    result.setCollations(recommendedQueries);
	                }
                }
            }
        }

        return result;
    }

    protected static DSpaceObject findDSpaceObject(Context context, String uniqueId) throws SQLException {

        String[] idParts = uniqueId.split("-");
        Integer type = Integer.valueOf(idParts[0]);
        Integer id = Integer.valueOf(idParts[1]);

        if (type != null && id != null)
        {
            return DSpaceObject.find(context, type, id);
        } 

        return null;
    }

    protected static DSpaceObject findDSpaceObject(Context context, SolrDocument doc) throws SQLException {

        Integer type = (Integer) doc.getFirstValue("search.resourcetype");
        Integer id = (Integer) doc.getFirstValue("search.resourceid");
        String handle = (String) doc.getFirstValue("handle");

        if (type != null && id != null)
        {
            return DSpaceObject.find(context, type, id);
        } else if (handle != null)
        {
            return HandleManager.resolveToObject(context, handle);
        }

        return null;
    }


    /** Simple means to return the search result as an InputStream */
    public java.io.InputStream searchAsInputStream(DiscoverQuery query) throws SearchServiceException, java.io.IOException {
        if(getSolr() == null)
        {
            return null;
        }
        HttpHost hostURL = (HttpHost)(getSolr().getHttpClient().getParams().getParameter(ClientPNames.DEFAULT_HOST));

        HttpGet method = new HttpGet(hostURL.toHostString() + "");
        try
        {
            URI uri = new URIBuilder(method.getURI()).addParameter("q",query.toString()).build();
        }
        catch (URISyntaxException e)
        {
            throw new SearchServiceException(e);
        }

        HttpResponse response = getSolr().getHttpClient().execute(method);

        return response.getEntity().getContent();
    }

    public List<DSpaceObject> search(Context context, String query, int offset, int max, String... filterquery)
    {
        return search(context, query, null, true, offset, max, filterquery);
    }

    public List<DSpaceObject> search(Context context, String query, String orderfield, boolean ascending, int offset, int max, String... filterquery)
    {

        try {
            if(getSolr() == null)
            {
                return Collections.emptyList();
            }

            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(query);
            solrQuery.setFields("search.resourceid", "search.resourcetype");
            solrQuery.setStart(offset);
            solrQuery.setRows(max);
            if (orderfield != null)
            {
                solrQuery.setSortField(orderfield, ascending ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
            }
            if (filterquery != null)
            {
                solrQuery.addFilterQuery(filterquery);
            }
            QueryResponse rsp = getSolr().query(solrQuery);
            SolrDocumentList docs = rsp.getResults();

            Iterator iter = docs.iterator();
            List<DSpaceObject> result = new ArrayList<DSpaceObject>();
            while (iter.hasNext())
            {
                SolrDocument doc = (SolrDocument) iter.next();

                DSpaceObject o = DSpaceObject.find(context, (Integer) doc.getFirstValue("search.resourcetype"), (Integer) doc.getFirstValue("search.resourceid"));

                if (o != null)
                {
                    result.add(o);
                }
            }
            return result;
		} catch (Exception e)
        {
			// Any acception that we get ignore it.
			// We do NOT want any crashed to shown by the user
            log.error(LogManager.getHeader(context, "Error while quering solr", "Queyr: " + query), e);
            return new ArrayList<DSpaceObject>(0);
		}
    }

    public DiscoverFilterQuery TODEL_toFilterQuery(Context context, String field, String operator, String value) throws SQLException{
        DiscoverFilterQuery result = new DiscoverFilterQuery();

        StringBuilder filterQuery = new StringBuilder();
        if(StringUtils.isNotBlank(field))
        {
            filterQuery.append(field);
            if("equals".equals(operator))
            {
                //Query the keyword indexed field !
                filterQuery.append("_keyword");
            }
            else if ("authority".equals(operator))
            {
                //Query the authority indexed field !
                filterQuery.append("_authority");
            }
            else if ("notequals".equals(operator)
                    || "notcontains".equals(operator)
                    || "notauthority".equals(operator))
            {
                filterQuery.insert(0, "-");
            }
            filterQuery.append(":");
            if("equals".equals(operator) || "notequals".equals(operator))
            {
                //DO NOT ESCAPE RANGE QUERIES !
                if(!value.matches("\\[.*TO.*\\]"))
                {
                    value = ClientUtils.escapeQueryChars(value);
                    filterQuery.append(value);
                }
                else
                {
                	if (value.matches("\\[\\d{1,4} TO \\d{1,4}\\]"))
                	{
                		int minRange = Integer.parseInt(value.substring(1, value.length()-1).split(" TO ")[0]);
                		int maxRange = Integer.parseInt(value.substring(1, value.length()-1).split(" TO ")[1]);
                		value = "["+String.format("%04d", minRange) + " TO "+ String.format("%04d", maxRange) + "]";
                	}
                	filterQuery.append(value);
                }
            }
            else{
                //DO NOT ESCAPE RANGE QUERIES !
                if(!value.matches("\\[.*TO.*\\]"))
                {
                    value = ClientUtils.escapeQueryChars(value);
                    filterQuery.append("(").append(value).append(")");
                }
                else
                {
                    filterQuery.append(value);
                }
            }


        }

        result.setDisplayedValue(transformDisplayedValue(context, field, value));
        result.setFilterQuery(filterQuery.toString());
        return result;
    }
    
    // Lan
    public DiscoverFilterQuery toFilterQuery(Context context, String field, String operator, String value) throws SQLException{
    	if (field.matches("'(.+)'")) {
    		return toFilterQuery(false /* append no suffix to field */, context, field.substring(1, field.length()-1), operator, value);
    	}
    	return toFilterQuery(true /* append suffix  _keyword or _partial to field */, context, field, operator, value);
    	
    }
    

    // Lan 15.01.2016
    // isIndexed is true when the field is configured as a searchFilterin discovery.xml e.g. exists solr fields <field>_keyword, <field>_contain
    // then append field with _keyword or _contain in the filter query
    private DiscoverFilterQuery toFilterQuery(boolean isIndexed, Context context, String field, String operator, String value) throws SQLException{
        DiscoverFilterQuery result = new DiscoverFilterQuery();

        StringBuilder filterQuery = new StringBuilder();
        if(StringUtils.isNotBlank(field))
        {
            filterQuery.append(field);
            
            switch(operator) {
            case "notequals":
            	filterQuery.insert(0, "-");
            case "equals":
                //Query the keyword indexed field !
            	if (isIndexed) { filterQuery.append("_keyword"); }
            	break;
            case "notcontains":
            	filterQuery.insert(0, "-");
            case "contains":
                //Query the partial n-gram field !
            	if (isIndexed) { filterQuery.append("_contain"); }
            	break;
            case "notauthority":
            	filterQuery.insert(0, "-");
            case "authority":
	            //Query the authority indexed field !
	            if (isIndexed) { filterQuery.append("_authority"); }
            default:
            	break;
            }
            
            filterQuery.append(":");
            if("equals".equals(operator) || "notequals".equals(operator))
            {
                //DO NOT ESCAPE RANGE QUERIES !
                if(!value.matches("\\[.*TO.*\\]"))
                {
                    value = ClientUtils.escapeQueryChars(value);
                    filterQuery.append(value);
                }
                else
                {
                	if (value.matches("\\[\\d{1,4} TO \\d{1,4}\\]"))
                	{
                		int minRange = Integer.parseInt(value.substring(1, value.length()-1).split(" TO ")[0]);
                		int maxRange = Integer.parseInt(value.substring(1, value.length()-1).split(" TO ")[1]);
                		value = "["+String.format("%04d", minRange) + " TO "+ String.format("%04d", maxRange) + "]";
                	}
                	filterQuery.append(value);
                }
            }
            else
            {
                //DO NOT ESCAPE RANGE QUERIES !
                if(!value.matches("\\[.*TO.*\\]"))
                {
                	String[] tokens = value.split("\\W+");
                	value = ClientUtils.escapeQueryChars(value);
                	if (tokens.length > 1) {
                        filterQuery.append("\"").append(value).append("\"~9");                		
                	} else {
                        filterQuery.append(value);                		
                		
                	}
                }
                else
                {
                    filterQuery.append(value);
                }
            }
            
        }

        result.setDisplayedValue(transformDisplayedValue(context, field, value));
        result.setFilterQuery(filterQuery.toString());
        return result;
    }

    private DiscoverFilterQuery TODEL2_toFilterQuery(boolean isIndexed, Context context, String field, String operator, String value) throws SQLException{
        DiscoverFilterQuery result = new DiscoverFilterQuery();

        StringBuilder filterQuery = new StringBuilder();
        if(StringUtils.isNotBlank(field))
        {
            filterQuery.append(field);
            if("equals".equals(operator))
            {
                //Query the keyword indexed field !
                if (isIndexed) { filterQuery.append("_keyword"); }
            }
            else if ("contains".equals(operator))
            {
                //Query the partial n-gram field !
                if (isIndexed) { filterQuery.append("_contain"); }
            }
             else if ("authority".equals(operator))
            {
                //Query the authority indexed field !
                if (isIndexed) { filterQuery.append("_authority"); }
            }
            else if ("notequals".equals(operator)
                    || "notcontains".equals(operator)
                    || "notauthority".equals(operator))
            {
                filterQuery.insert(0, "-");
            }
            filterQuery.append(":");
            if("equals".equals(operator) || "notequals".equals(operator))
            {
                //DO NOT ESCAPE RANGE QUERIES !
                if(!value.matches("\\[.*TO.*\\]"))
                {
                    value = ClientUtils.escapeQueryChars(value);
                    filterQuery.append(value);
                }
                else
                {
                	if (value.matches("\\[\\d{1,4} TO \\d{1,4}\\]"))
                	{
                		int minRange = Integer.parseInt(value.substring(1, value.length()-1).split(" TO ")[0]);
                		int maxRange = Integer.parseInt(value.substring(1, value.length()-1).split(" TO ")[1]);
                		value = "["+String.format("%04d", minRange) + " TO "+ String.format("%04d", maxRange) + "]";
                	}
                	filterQuery.append(value);
                }
            }
            else{
                //DO NOT ESCAPE RANGE QUERIES !
                if(!value.matches("\\[.*TO.*\\]"))
                {
/*                	
                    StringTokenizer tokens = new StringTokenizer(value, " ");
                	value = ClientUtils.escapeQueryChars(value);
                	if (tokens.countTokens() > 1) {
                        filterQuery.append("\"").append(value).append("\"~9");                		
                	} else {
                        filterQuery.append(value);                		
                		
                	}
*/                	
                	String[] tokens = value.split("\\W+");
                	value = ClientUtils.escapeQueryChars(value);
                	if (tokens.length > 1) {
                        filterQuery.append("\"").append(value).append("\"~9");                		
                	} else {
                        filterQuery.append(value);                		
                		
                	}
                }
                else
                {
                    filterQuery.append(value);
                }
            }
            
            /* Lan 26.01.2016 : this is for contains orerator,
             * q.op=AND is mandatory because text_edge creates others sub-tokens using Word Delimiter Filter :
             * 		compare {!q.op=AND}codeorigine_partial:DAL6140382 versus codeorigine_partial:DAL6140382
             * subquery _query_ is mandatory for join request in requestHandler /selectCollection and /selectCommunity
             */
            /*
            if ("contains".equals(operator) || "notcontains".equals(operator)) {
            	// filterQuery.insert(0,"{!q.op=AND}");
            	filterQuery.insert(0,"_query_:\"{!q.op=AND}");
            	filterQuery.append("\"");
            }
            */
        }

        result.setDisplayedValue(transformDisplayedValue(context, field, value));
        result.setFilterQuery(filterQuery.toString());
        return result;
    }


    @Override
    public List<Item> getRelatedItems(Context context, Item item, DiscoveryMoreLikeThisConfiguration mltConfig)
    {
        List<Item> results = new ArrayList<Item>();
        try{
            SolrQuery solrQuery = new SolrQuery();
            //Set the query to handle since this is unique
            solrQuery.setQuery("handle: " + item.getHandle());
            //Add the more like this parameters !
            solrQuery.setParam(MoreLikeThisParams.MLT, true);
            //Add a comma separated list of the similar fields
            @SuppressWarnings("unchecked")
            java.util.Collection<String> similarityMetadataFields = CollectionUtils.collect(mltConfig.getSimilarityMetadataFields(), new Transformer()
            {
                @Override
                public Object transform(Object input)
                {
                    //Add the mlt appendix !
                    return input + "_mlt";
                }
            });

            solrQuery.setParam(MoreLikeThisParams.SIMILARITY_FIELDS, StringUtils.join(similarityMetadataFields, ','));
            solrQuery.setParam(MoreLikeThisParams.MIN_TERM_FREQ, String.valueOf(mltConfig.getMinTermFrequency()));
            solrQuery.setParam(MoreLikeThisParams.DOC_COUNT, String.valueOf(mltConfig.getMax()));
            solrQuery.setParam(MoreLikeThisParams.MIN_WORD_LEN, String.valueOf(mltConfig.getMinWordLength()));

            if(getSolr() == null)
            {
                return Collections.emptyList();
            }
            QueryResponse rsp = getSolr().query(solrQuery);
            NamedList mltResults = (NamedList) rsp.getResponse().get("moreLikeThis");
            if(mltResults != null && mltResults.get(item.getType() + "-" + item.getID()) != null)
            {
                SolrDocumentList relatedDocs = (SolrDocumentList) mltResults.get(item.getType() + "-" + item.getID());
                for (Object relatedDoc : relatedDocs)
                {
                    SolrDocument relatedDocument = (SolrDocument) relatedDoc;
                    DSpaceObject relatedItem = findDSpaceObject(context, relatedDocument);
                    if (relatedItem.getType() == Constants.ITEM)
                    {
                        results.add((Item) relatedItem);
                    }
                }
            }


        } catch (Exception e)
        {
            log.error(LogManager.getHeader(context, "Error while retrieving related items", "Handle: " + item.getHandle()), e);
        }
        return results;
    }

    @Override
    // 10.03.2016 Lan : Use only by xmlui
    public String toSortFieldIndex(String metadataField, String type)
    {
        return metadataField + "_sort";
    }

    // 24.03.2017 Lan
    protected String pivotingFacetField(DiscoverFacetField facetFieldConfig, String field)
    {
    	int lastIndexOf;
    	if(facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_HIERARCHICAL))
        {
    		// pivot string for hierarchical facetting on "channel" is "{!key=channel}channel_tax_0_filter,channel_keyword"
    		return String.format("{!key=%1$s}%1$s_tax_0_filter,%1$s_keyword", field);
        }else{
            return null;
        }
    }

    protected String transformFacetField(DiscoverFacetField facetFieldConfig, String field, boolean removePostfix)
    {
    	int lastIndexOf;
        if(facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_TEXT))
        {
            if(removePostfix)
            {
                return field.substring(0, field.lastIndexOf("_filter"));
            }else{
                return field + "_filter";
            }
        }else if(facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE))
        {
            if(removePostfix)
            {
            	lastIndexOf = field.lastIndexOf(".year");
            	return ((lastIndexOf >= 0) ? field.substring(0, lastIndexOf) : field);
            }else{
                return field + ".year";
            }
        }else if(facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_AC))
        {
            if(removePostfix)
            {
                return field.substring(0, field.lastIndexOf("_ac"));
            }else{
                return field + "_ac";
            }
        }else if(facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_HIERARCHICAL))
        {
            if(removePostfix)
            {
                return StringUtils.substringBeforeLast(field, "_tax_");
            }else{
                //Only display top level filters !
                return field + "_tax_0_filter";
            }
        }else if(facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_AUTHORITY))
        {
            if(removePostfix)
            {
                return field.substring(0, field.lastIndexOf("_acid"));
            }else{
                return field + "_acid";
            }
        }else if(facetFieldConfig.getType().equals(DiscoveryConfigurationParameters.TYPE_STANDARD))
        {
            return field;
        }else{
            return field;
        }
    }

    protected String transformDisplayedValue(Context context, String field, String value) throws SQLException {
        if(field.equals("location.comm") || field.equals("location.coll"))
        {
            value = locationToName(context, field, value);
        }
        else if (field.endsWith("_filter") || field.endsWith("_ac")
          || field.endsWith("_acid"))
        {
            //We have a filter make sure we split !
            String separator = new DSpace().getConfigurationService().getProperty("discovery.solr.facets.split.char");
            if(separator == null)
            {
                separator = FILTER_SEPARATOR;
            }
            //Escape any regex chars
            separator = java.util.regex.Pattern.quote(separator);
            String[] fqParts = value.split(separator);
            StringBuffer valueBuffer = new StringBuffer();
            int start = fqParts.length / 2;
            for(int i = start; i < fqParts.length; i++)
            {
                String[] split = fqParts[i].split(AUTHORITY_SEPARATOR, 2);
                valueBuffer.append(split[0]);
            }
            value = valueBuffer.toString();
        }else if(value.matches("\\((.*?)\\)"))
        {
            //The brackets where added for better solr results, remove the first & last one
            value = value.substring(1, value.length() -1);
        }
        return value;
    }

    protected String transformAuthorityValue(Context context, String field, String value) throws SQLException {
    	if(field.equals("location.comm") || field.equals("location.coll"))
    	{
            return value;
    	}
    	if (field.endsWith("_filter") || field.endsWith("_ac")
                || field.endsWith("_acid"))
        {
            //We have a filter make sure we split !
            String separator = new DSpace().getConfigurationService().getProperty("discovery.solr.facets.split.char");
            if(separator == null)
            {
                separator = FILTER_SEPARATOR;
            }
            //Escape any regex chars
            separator = java.util.regex.Pattern.quote(separator);
            String[] fqParts = value.split(separator);
            StringBuffer authorityBuffer = new StringBuffer();
            int start = fqParts.length / 2;
            for(int i = start; i < fqParts.length; i++)
            {
                String[] split = fqParts[i].split(AUTHORITY_SEPARATOR, 2);
                if (split.length == 2)
                {
                    authorityBuffer.append(split[1]);
                }
            }
            if (authorityBuffer.length() > 0)
            {
                return authorityBuffer.toString();
            }
        }
        return null;
    }

    protected String transformSortValue(Context context, String field, String value) throws SQLException {
        if(field.equals("location.comm") || field.equals("location.coll"))
        {
            value = locationToName(context, field, value);
        }
        else if (field.endsWith("_filter") || field.endsWith("_ac")
                || field.endsWith("_acid"))
        {
            //We have a filter make sure we split !
            String separator = new DSpace().getConfigurationService().getProperty("discovery.solr.facets.split.char");
            if(separator == null)
            {
                separator = FILTER_SEPARATOR;
            }
            //Escape any regex chars
            separator = java.util.regex.Pattern.quote(separator);
            String[] fqParts = value.split(separator);
            StringBuffer valueBuffer = new StringBuffer();
            int end = fqParts.length / 2;
            for(int i = 0; i < end; i++)
            {
                valueBuffer.append(fqParts[i]);
            }
            value = valueBuffer.toString();
        }else if(value.matches("\\((.*?)\\)"))
        {
            //The brackets where added for better solr results, remove the first & last one
            value = value.substring(1, value.length() -1);
        }
        return value;
    }

	@Override
	public void indexContent(Context context, DSpaceObject dso, boolean force,
			boolean commit) throws SearchServiceException, SQLException {
		indexContent(context, dso, force);
		if (commit)
		{
			commit();
		}
	}

	@Override
	public void commit() throws SearchServiceException {
		try {
            if(getSolr() != null)
            {
                getSolr().commit();
            }
		} catch (Exception e) {
			throw new SearchServiceException(e.getMessage(), e);
		}
	}
}
