package org.dspace.rtbf.rest;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * Root resource (exposed at "/" path)
 */
@Path("/")
public class RsIndex {

/*    @javax.ws.rs.core.Context public static ServletContext servletContext;*/

    /**
     * Return html page with information about RTBF-REST api. It contains methods
     * methods provide by RTBF-REST api.
     * The suffix "REST" is misused, because there is noting rest out there, 
     * the better name would be RTBF-WS for general Web Service
     * 31.03.2017 Lan
     * 
     * @return HTML page which has information about all methods of RTBF-REST api.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String sayHtmlHello(@Context HttpServletRequest request)
            throws WebApplicationException
    { 
        return "<html><title>Archimed Web Services - index</title>" +
        		"<head>" +
        		"<script type=\"text/javascript\">" +
        		"function loadIndex() {" +
        		"	location.href= \"" + request.getContextPath() + "/index.html\";" +
        		"}" +
        		"</script>" +
        		" </head>" +
        		"<body onload=\"loadIndex()\">" +
        		"</body>"
        ;
    }
    

    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @GET
    @Path("test")
    @Produces(MediaType.TEXT_PLAIN)
    public String test() {
        return "RTBF RS api is running";
    }
}
