/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.suggest;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.*;
import org.apache.solr.common.util.JavaBinCodec;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.statistics.util.DnsLookup;
import org.dspace.statistics.util.LocationUtils;
import org.dspace.statistics.util.SpiderDetector;
import org.dspace.usage.UsageWorkflowEvent;

import javax.servlet.http.HttpServletRequest;

import java.io.*;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Static holder for a HttpSolrClient connection pool to issue
 * usage logging events to Solr from DSpace libraries, and some static query
 * composers.
 * 
 * @author ben at atmire.com
 * @author kevinvandevelde at atmire.com
 * @author mdiggory at atmire.com
 */
public class SuggestSearch
{
    private static final Logger log = Logger.getLogger(SuggestSearch.class);
	
    private static final HttpSolrServer solr;

    public static final String DATE_FORMAT_8601 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static final String DATE_FORMAT_DCDATE = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private static final LookupService locationService;

    private static final boolean useProxies;

    private static List<String> statisticYearCores = new ArrayList<String>();

    public static enum StatisticsType {
   		VIEW ("view"),
   		SEARCH ("search"),
   		SEARCH_RESULT ("search_result"),
        WORKFLOW("workflow");

   		private final String text;

        StatisticsType(String text) {
   	        this.text = text;
   	    }
   	    public String text()   { return text; }
   	}


    static
    {
        log.info("solr-suggest.spidersfile:" + ConfigurationManager.getProperty("solr-suggest", "spidersfile"));
        log.info("solr-suggest.server:" + ConfigurationManager.getProperty("solr-suggest", "server"));
        log.info("usage-statistics.dbfile:" + ConfigurationManager.getProperty("usage-statistics", "dbfile"));
    	
        HttpSolrServer server = null;
        
        if (ConfigurationManager.getProperty("solr-suggest", "server") != null)
        {
            try
            {
                server = new HttpSolrServer(ConfigurationManager.getProperty("solr-suggest", "server"));
                SolrQuery solrQuery = new SolrQuery()
                        .setQuery("type:2 AND id:1");
                server.query(solrQuery);

                //Attempt to retrieve all the statistic year cores
                File solrDir = new File(ConfigurationManager.getProperty("dspace.dir") + "/solr/");
                File[] solrCoreFiles = solrDir.listFiles(new FileFilter() {

                    @Override
                    public boolean accept(File file) {
                        //Core name example: statistics-2008
                        return file.getName().matches("statistics-\\d\\d\\d\\d");
                    }
                });
                //Base url should like : http://localhost:{port.number}/solr
                String baseSolrUrl = server.getBaseURL().replace("statistics", "");
                for (File solrCoreFile : solrCoreFiles) {
                    log.info("Loading core with name: " + solrCoreFile.getName());

                    createCore(server, solrCoreFile.getName());
                    //Add it to our cores list so we can query it !
                    statisticYearCores.add(baseSolrUrl.replace("http://", "").replace("https://", "") + solrCoreFile.getName());
                }
                //Also add the core containing the current year !
                statisticYearCores.add(server.getBaseURL().replace("http://", "").replace("https://", ""));
            } catch (Exception e) {
            	log.error(e.getMessage(), e);
            }
        }
        solr = server;

        // Read in the file so we don't have to do it all the time
        //spiderIps = SpiderDetector.getSpiderIpAddresses();

        LookupService service = null;
        // Get the db file for the location
        String dbfile = ConfigurationManager.getProperty("usage-statistics", "dbfile");
        if (dbfile != null)
        {
            try
            {
                service = new LookupService(dbfile,
                        LookupService.GEOIP_STANDARD);
            }
            catch (FileNotFoundException fe)
            {
                log.error("The GeoLite Database file is missing (" + dbfile + ")! Solr Statistics cannot generate location based reports! Please see the DSpace installation instructions for instructions to install this file.", fe);
            }
            catch (IOException e)
            {
                log.error("Unable to load GeoLite Database file (" + dbfile + ")! You may need to reinstall it. See the DSpace installation instructions for more details.", e);
            }
        }
        else
        {
            log.error("The required 'dbfile' configuration is missing in solr-suggest.cfg!");
        }
        locationService = service;

        if ("true".equals(ConfigurationManager.getProperty("useProxies")))
        {
            useProxies = true;
        }
        else
        {
            useProxies = false;
        }

        log.info("useProxies=" + useProxies);
    }


    /**
     * Returns a solr input document containing common information about the statistics
     * regardless if we are logging a search or a view of a DSpace object
     * @param dspaceObject the object used.
     * @param request the current request context.
     * @param currentUser the current session's user.
     * @return a solr input document
     * @throws SQLException in case of a database exception
     */
    private static SolrInputDocument getCommonSolrDoc(DSpaceObject dspaceObject, HttpServletRequest request, EPerson currentUser) throws SQLException {
        boolean isSpiderBot = request != null && SpiderDetector.isSpider(request);
        if(isSpiderBot &&
                !ConfigurationManager.getBooleanProperty("usage-statistics", "logBots", true))
        {
            return null;
        }

        SolrInputDocument doc1 = new SolrInputDocument();
        // Save our basic info that we already have

        if(request != null){
            String ip = request.getRemoteAddr();


            doc1.addField("ip", ip);

            //Also store the referrer
            if(request.getHeader("referer") != null){
                doc1.addField("referrer", request.getHeader("referer"));
            }

            try
            {
                String dns = DnsLookup.reverseDns(ip);
                doc1.addField("dns", dns.toLowerCase());
            }
            catch (Exception e)
            {
                log.error("Failed DNS Lookup for IP:" + ip);
                log.debug(e.getMessage(),e);
            }
		    if(request.getHeader("User-Agent") != null)
		    {
		        doc1.addField("userAgent", request.getHeader("User-Agent"));
		    }
            // Save the location information if valid, save the event without
            // location information if not valid
            if(locationService != null)
            {
                Location location = locationService.getLocation(ip);
                if (location != null
                        && !("--".equals(location.countryCode)
                        && location.latitude == -180 && location.longitude == -180))
                {
                    try
                    {
                        doc1.addField("continent", LocationUtils
                                .getContinentCode(location.countryCode));
                    }
                    catch (Exception e)
                    {
                        System.out
                                .println("COUNTRY ERROR: " + location.countryCode);
                    }
                    doc1.addField("countryCode", location.countryCode);
                    doc1.addField("city", location.city);
                    doc1.addField("latitude", location.latitude);
                    doc1.addField("longitude", location.longitude);
                    doc1.addField("isBot",isSpiderBot);


                }
            }
        }

        // Save the current time
        doc1.addField("time", DateFormatUtils.format(new Date(), DATE_FORMAT_8601));
        if (currentUser != null)
        {
            doc1.addField("epersonid", currentUser.getID());
        }

        // Lan 18.11.2016 : store uriInfo = URI+query
        if(request.getQueryString() != null){
            doc1.addField("uriInfo", request.getRequestURI()+"?"+request.getQueryString());
        } else {
        	doc1.addField("uriInfo", request.getRequestURI());
        }
        
        if (request.getParameter("scope") != null) {
        	doc1.addField("scope", request.getParameter("scope"));
        }

        return doc1;
    }

    private static SolrInputDocument getCommonSolrDoc(DSpaceObject dspaceObject, String ip, String userAgent, String xforwardedfor, EPerson currentUser) throws SQLException {
        boolean isSpiderBot = SpiderDetector.isSpider(ip);
        if(isSpiderBot &&
                !ConfigurationManager.getBooleanProperty("usage-statistics", "logBots", true))
        {
            return null;
        }

        SolrInputDocument doc1 = new SolrInputDocument();
        // Save our basic info that we already have

        if(ip != null){
            doc1.addField("ip", ip);

            try
            {
                String dns = DnsLookup.reverseDns(ip);
                doc1.addField("dns", dns.toLowerCase());
            }
            catch (Exception e)
            {
                log.error("Failed DNS Lookup for IP:" + ip);
                log.debug(e.getMessage(),e);
            }
		    if(userAgent != null)
		    {
		        doc1.addField("userAgent", userAgent);
		    }
            // Save the location information if valid, save the event without
            // location information if not valid
            if(locationService != null)
            {
                Location location = locationService.getLocation(ip);
                if (location != null
                        && !("--".equals(location.countryCode)
                        && location.latitude == -180 && location.longitude == -180))
                {
                    try
                    {
                        doc1.addField("continent", LocationUtils
                                .getContinentCode(location.countryCode));
                    }
                    catch (Exception e)
                    {
                        System.out
                                .println("COUNTRY ERROR: " + location.countryCode);
                    }
                    doc1.addField("countryCode", location.countryCode);
                    doc1.addField("city", location.city);
                    doc1.addField("latitude", location.latitude);
                    doc1.addField("longitude", location.longitude);
                    doc1.addField("isBot",isSpiderBot);


                }
            }
        }

        // Save the current time
        doc1.addField("time", DateFormatUtils.format(new Date(), DATE_FORMAT_8601));
        if (currentUser != null)
        {
            doc1.addField("epersonid", currentUser.getID());
        }

        return doc1;
    }

    
    public static void postSearch(DSpaceObject resultObject, HttpServletRequest request, EPerson currentUser,
                                 List<String> queries, int rpp, String sortBy, String order, int page, DSpaceObject scope) {
        try
        {
            SolrInputDocument solrDoc = getCommonSolrDoc(resultObject, request, currentUser);
            if (solrDoc == null) return;

            for (String query : queries) {
                solrDoc.addField("query", query);
            }

            // Lan 08.12.2016 : isolate q from fq in queries
            // based on a quick rule that q does not contains ':'
            String query_q;
            if (!queries.isEmpty()) {
            	query_q = queries.get(0);
            	if (query_q.contains(":")) { query_q = null; }
            	if (query_q != null) {
                    solrDoc.addField("query_q", query_q);
            	}
            	
            }

            if(resultObject != null){
                //We have a search result
                solrDoc.addField("statistics_type", StatisticsType.SEARCH_RESULT.text());
            }else{
                solrDoc.addField("statistics_type", StatisticsType.SEARCH.text());
            }
            //Store the scope
            if(scope != null){
                solrDoc.addField("scopeId", scope.getID());
                solrDoc.addField("scopeType", scope.getType());
            }

            if(rpp != -1){
                solrDoc.addField("rpp", rpp);
            }

            if(sortBy != null){
                solrDoc.addField("sortBy", sortBy);
                if(order != null){
                    solrDoc.addField("sortOrder", order);
                }
            }

            if(page != -1){
                solrDoc.addField("page", page);
            }

            solr.add(solrDoc);
        }
        catch (RuntimeException re)
        {
            throw re;
        }
        catch (Exception e)
        {
        	log.error(e.getMessage(), e);
        }
    }


    public static class ResultProcessor
    {

        public void execute(String query) throws SolrServerException, IOException {
            Map<String, String> params = new HashMap<String, String>();
            params.put("q", query);
            params.put("rows", "10");
            if(0 < statisticYearCores.size()){
                params.put(ShardParams.SHARDS, StringUtils.join(statisticYearCores.iterator(), ','));
            }
            MapSolrParams solrParams = new MapSolrParams(params);
            QueryResponse response = solr.query(solrParams);
            
            long numbFound = response.getResults().getNumFound();

            // process the first batch
            process(response.getResults());

            // Run over the rest
            for (int i = 10; i < numbFound; i += 10)
            {
                params.put("start", String.valueOf(i));
                solrParams = new MapSolrParams(params);
                response = solr.query(solrParams);
                process(response.getResults());
            }

        }

        public void commit() throws IOException, SolrServerException {
            solr.commit();
        }

        /**
         * Override to manage pages of documents
         * @param docs
         */
        public void process(List<SolrDocument> docs) throws IOException, SolrServerException {
            for(SolrDocument doc : docs){
                process(doc);
            }
        }

        /**
         * Override to manage individual documents
         * @param doc
         */
        public void process(SolrDocument doc) throws IOException, SolrServerException {


        }
    }

    /*
     * //TODO: below are not used public static void
     * update(String query, boolean addField, String fieldName, Object
     * fieldValue, Object oldFieldValue) throws SolrServerException, IOException
     * { List<Object> vals = new ArrayList<Object>(); vals.add(fieldValue);
     * List<Object> oldvals = new ArrayList<Object>(); oldvals.add(fieldValue);
     * update(query, addField, fieldName, vals, oldvals); }
     */
    public static void update(String query, String action,
            List<String> fieldNames, List<List<Object>> fieldValuesList)
            throws SolrServerException, IOException
    {
        // Since there is NO update
        // We need to get our documents
        // QueryResponse queryResponse = solr.query()//query(query, null, -1,
        // null, null, null);

        final List<SolrDocument> docsToUpdate = new ArrayList<SolrDocument>();

        ResultProcessor processor = new ResultProcessor(){
                public void process(List<SolrDocument> docs) throws IOException, SolrServerException {
                    docsToUpdate.addAll(docs);
                }
            };

        processor.execute(query);

        // We have all the docs delete the ones we don't need
        solr.deleteByQuery(query);

        // Add the new (updated onces
        for (int i = 0; i < docsToUpdate.size(); i++)
        {
            SolrDocument solrDocument = docsToUpdate.get(i);
            // Now loop over our fieldname actions
            for (int j = 0; j < fieldNames.size(); j++)
            {
                String fieldName = fieldNames.get(j);
                List<Object> fieldValues = fieldValuesList.get(j);

                if (action.equals("addOne") || action.equals("replace"))
                {
                    if (action.equals("replace"))
                    {
                        solrDocument.removeFields(fieldName);
                    }

                    for (Object fieldValue : fieldValues)
                    {
                        solrDocument.addField(fieldName, fieldValue);
                    }
                }
                else if (action.equals("remOne"))
                {
                    // Remove the field
                    java.util.Collection<Object> values = solrDocument
                            .getFieldValues(fieldName);
                    solrDocument.removeFields(fieldName);
                    for (Object value : values)
                    {
                        // Keep all the values besides the one we need to remove
                        if (!fieldValues.contains((value)))
                        {
                            solrDocument.addField(fieldName, value);
                        }
                    }
                }
            }
            SolrInputDocument newInput = ClientUtils
                    .toSolrInputDocument(solrDocument);
            solr.add(newInput);
        }
        solr.commit();
        // System.out.println("SolrLogger.update(\""+query+"\"):"+(new
        // Date().getTime() - start)+"ms,"+numbFound+"records");
    }

    public static void query(String query, int max) throws SolrServerException
    {
        query(query, null, null,0, max, null, null, null, null, null, false, null);
    }

    public static QueryResponse query(String query, int max, String requestHandler) throws SolrServerException
    {
        return query(query, null, null, max , max, null, null, null, null, null, false, requestHandler);
    }




    public static Map<String, Integer> queryFacetQuery(String query,
            String filterQuery, List<String> facetQueries)
            throws SolrServerException
    {
        QueryResponse response = query(query, filterQuery, null,0, 1, null, null,
                null, facetQueries, null, false);
        return response.getFacetQuery();
    }


    private static String getDateView(String name, String type, Context context)
    {
        if (name != null && name.matches("^[0-9]{4}\\-[0-9]{2}.*"))
        {
            /*
             * if("YEAR".equalsIgnoreCase(type)) return name.substring(0, 4);
             * else if("MONTH".equalsIgnoreCase(type)) return name.substring(0,
             * 7); else if("DAY".equalsIgnoreCase(type)) return
             * name.substring(0, 10); else if("HOUR".equalsIgnoreCase(type))
             * return name.substring(11, 13);
             */
            // Get our date
            Date date = null;
            try
            {
                SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT_8601, context.getCurrentLocale());
                date = format.parse(name);
            }
            catch (ParseException e)
            {
                try
                {
                    // We should use the dcdate (the dcdate is used when
                    // generating random data)
                    SimpleDateFormat format = new SimpleDateFormat(
                            DATE_FORMAT_DCDATE, context.getCurrentLocale());
                    date = format.parse(name);
                }
                catch (ParseException e1)
                {
                    e1.printStackTrace();
                }
                // e.printStackTrace();
            }
            String dateformatString = "dd-MM-yyyy";
            if ("DAY".equals(type))
            {
                dateformatString = "dd-MM-yyyy";
            }
            else if ("MONTH".equals(type))
            {
                dateformatString = "MMMM yyyy";

            }
            else if ("YEAR".equals(type))
            {
                dateformatString = "yyyy";
            }
            SimpleDateFormat simpleFormat = new SimpleDateFormat(
                    dateformatString, context.getCurrentLocale());
            if (date != null)
            {
                name = simpleFormat.format(date);
            }

        }
        return name;
    }

    public static QueryResponse query(String query, String filterQuery,
            String facetField, int rows, int max, String dateType, String dateStart,
            String dateEnd, List<String> facetQueries, String sort, boolean ascending)
            throws SolrServerException
    {
    	return query(query, filterQuery,
                facetField, rows, max, dateType, dateStart,
                dateEnd, facetQueries, sort, ascending, null);
    }


    // Lan 12.12.2016 : set request handler
    public static QueryResponse query(String query, String filterQuery,
            String facetField, int rows, int max, String dateType, String dateStart,
            String dateEnd, List<String> facetQueries, String sort, boolean ascending
            , String handler)
            throws SolrServerException
    {
        if (solr == null)
        {
            return null;
        }

        // System.out.println("QUERY");
        SolrQuery solrQuery = new SolrQuery().setRows(rows).setQuery(query)
                .setFacetMinCount(1);
        
        // Lan 12.12.2016 : set request handler
        if (handler != null) {
        	solrQuery.setRequestHandler(handler);
        }
        
        // Lan 21.12.2016 : set spellcheck
        solrQuery.setParam(SpellingParams.SPELLCHECK_Q, query);
        solrQuery.setParam(SpellingParams.SPELLCHECK_COLLATE, Boolean.TRUE);
        solrQuery.setParam("spellcheck", Boolean.TRUE);
        

        // Set the date facet if present
        if (dateType != null)
        {
            solrQuery.setParam("facet.date", "time")
                    .
                    // EXAMPLE: NOW/MONTH+1MONTH
                    setParam("facet.date.end",
                            "NOW/" + dateType + dateEnd + dateType).setParam(
                            "facet.date.gap", "+1" + dateType)
                    .
                    // EXAMPLE: NOW/MONTH-" + nbMonths + "MONTHS
                    setParam("facet.date.start",
                            "NOW/" + dateType + dateStart + dateType + "S")
                    .setFacet(true);
        }
        if (facetQueries != null)
        {
            for (int i = 0; i < facetQueries.size(); i++)
            {
                String facetQuery = facetQueries.get(i);
                solrQuery.addFacetQuery(facetQuery);
            }
            if (0 < facetQueries.size())
            {
                solrQuery.setFacet(true);
            }
        }

        if (facetField != null)
        {
            solrQuery.addFacetField(facetField);
        }

        // Set the top x of if present
        if (max != -1)
        {
            solrQuery.setFacetLimit(max);
        }


        if(sort != null){
            solrQuery.setSortField(sort, (ascending ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc));
        }


        if (filterQuery != null)
        {
            solrQuery.addFilterQuery(filterQuery);
            
        }

        QueryResponse response;
        try
        {
            // solr.set
            response = solr.query(solrQuery);
        }
        catch (SolrServerException e)
        {
            System.err.println("Error using query " + query);
            throw e;
        }
        return response;
    }


    /** String of IP and Ranges in IPTable as a Solr Query */
    private static String filterQuery = null;


    public static void shardSolrIndex() throws IOException, SolrServerException {
        /*
        Start by faceting by year so we can include each year in a separate core !
         */
        SolrQuery yearRangeQuery = new SolrQuery();
        yearRangeQuery.setQuery("*:*");
        yearRangeQuery.setRows(0);
        yearRangeQuery.setFacet(true);
        yearRangeQuery.add(FacetParams.FACET_RANGE, "time");
        //We go back to 2000 the year 2000, this is a bit overkill but this way we ensure we have everything
        //The alternative would be to sort but that isn't recommended since it would be a very costly query !
        yearRangeQuery.add(FacetParams.FACET_RANGE_START, "NOW/YEAR-" + (Calendar.getInstance().get(Calendar.YEAR) - 2000) + "YEARS");
        //Add the +0year to ensure that we DO NOT include the current year
        yearRangeQuery.add(FacetParams.FACET_RANGE_END, "NOW/YEAR+0YEARS");
        yearRangeQuery.add(FacetParams.FACET_RANGE_GAP, "+1YEAR");
        yearRangeQuery.add(FacetParams.FACET_MINCOUNT, String.valueOf(1));

        //Create a temp directory to store our files in !
        File tempDirectory = new File(ConfigurationManager.getProperty("dspace.dir") + File.separator + "temp" + File.separator);
        tempDirectory.mkdirs();


        QueryResponse queryResponse = solr.query(yearRangeQuery);
        //We only have one range query !
        List<RangeFacet.Count> yearResults = queryResponse.getFacetRanges().get(0).getCounts();
        for (RangeFacet.Count count : yearResults) {
            long totalRecords = count.getCount();

            //Create a range query from this !
            //We start with out current year
            DCDate dcStart = new DCDate(count.getValue());
            Calendar endDate = Calendar.getInstance();
            //Advance one year for the start of the next one !
            endDate.setTime(dcStart.toDate());
            endDate.add(Calendar.YEAR, 1);
            DCDate dcEndDate = new DCDate(endDate.getTime());


            StringBuilder filterQuery = new StringBuilder();
            filterQuery.append("time:([");
            filterQuery.append(ClientUtils.escapeQueryChars(dcStart.toString()));
            filterQuery.append(" TO ");
            filterQuery.append(ClientUtils.escapeQueryChars(dcEndDate.toString()));
            filterQuery.append("]");
            //The next part of the filter query excludes the content from midnight of the next year !
            filterQuery.append(" NOT ").append(ClientUtils.escapeQueryChars(dcEndDate.toString()));
            filterQuery.append(")");


            Map<String, String> yearQueryParams = new HashMap<String, String>();
            yearQueryParams.put(CommonParams.Q, "*:*");
            yearQueryParams.put(CommonParams.ROWS, String.valueOf(10000));
            yearQueryParams.put(CommonParams.FQ, filterQuery.toString());
            yearQueryParams.put(CommonParams.WT, "csv");

            //Start by creating a new core
            String coreName = "statistics-" + dcStart.getYear();
            HttpSolrServer statisticsYearServer = createCore(solr, coreName);

            System.out.println("Moving: " + totalRecords + " into core " + coreName);
            log.info("Moving: " + totalRecords + " records into core " + coreName);

            List<File> filesToUpload = new ArrayList<File>();
            for(int i = 0; i < totalRecords; i+=10000){
                String solrRequestUrl = solr.getBaseURL() + "/select";
                solrRequestUrl = generateURL(solrRequestUrl, yearQueryParams);

                HttpGet get = new HttpGet(solrRequestUrl);
                HttpResponse response = new DefaultHttpClient().execute(get);
                InputStream csvInputstream = response.getEntity().getContent();
                //Write the csv ouput to a file !
                File csvFile = new File(tempDirectory.getPath() + File.separatorChar + "temp." + dcStart.getYear() + "." + i + ".csv");
                FileUtils.copyInputStreamToFile(csvInputstream, csvFile);
                filesToUpload.add(csvFile);

                //Add 10000 & start over again
                yearQueryParams.put(CommonParams.START, String.valueOf((i + 10000)));
            }

            for (File tempCsv : filesToUpload) {
                //Upload the data in the csv files to our new solr core
                ContentStreamUpdateRequest contentStreamUpdateRequest = new ContentStreamUpdateRequest("/update/csv");
                contentStreamUpdateRequest.setParam("stream.contentType", "text/plain;charset=utf-8");
                contentStreamUpdateRequest.setAction(AbstractUpdateRequest.ACTION.COMMIT, true, true);
                contentStreamUpdateRequest.addFile(tempCsv, "text/plain;charset=utf-8");

                statisticsYearServer.request(contentStreamUpdateRequest);
            }
            statisticsYearServer.commit(true, true);


            //Delete contents of this year from our year query !
            solr.deleteByQuery(filterQuery.toString());
            solr.commit(true, true);

            log.info("Moved " + totalRecords + " records into core: " + coreName);
        }

        FileUtils.deleteDirectory(tempDirectory);
    }

    private static HttpSolrServer createCore(HttpSolrServer solr, String coreName) throws IOException, SolrServerException {
        String solrDir = ConfigurationManager.getProperty("dspace.dir") + File.separator + "solr" +File.separator;
        String baseSolrUrl = solr.getBaseURL().replace("statistics", "");
        CoreAdminRequest.Create create = new CoreAdminRequest.Create();
        create.setCoreName(coreName);
        create.setInstanceDir("statistics");
        create.setDataDir(solrDir + coreName + File.separator + "data");
        HttpSolrServer solrServer = new HttpSolrServer(baseSolrUrl);
        create.process(solrServer);
        log.info("Created core with name: " + coreName);
        return new HttpSolrServer(baseSolrUrl + "/" + coreName);
    }


    private static String generateURL(String baseURL, Map<String, String> parameters) throws UnsupportedEncodingException {
        boolean first = true;
        StringBuilder result = new StringBuilder(baseURL);
        for (String key : parameters.keySet())
        {
            if (first)
            {
                result.append("?");
                first = false;
            }
            else
            {
                result.append("&");
            }

            result.append(key).append("=").append(URLEncoder.encode(parameters.get(key), "UTF-8"));
        }

        return result.toString();
    }

}
