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
     * Return html page with information about REST api. It contains methods all
     * methods provide by REST api.
     * 
     * @return HTML page which has information about all methods of REST api.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String sayHtmlHello(@Context HttpServletRequest request)
            throws WebApplicationException
    { 
        return "<html><title>Archimed RS - index</title>" +
                "<body>"
                	+ "<h1>Archimed RS API</h1>" +
                	"Server path: " + request.getContextPath() +
                	"<h2>Index</h2>" +
                		"<ul>" +
                			"<li>GET / - Return this page.</li>" +
                			"<li>GET /test - Return the string \"RS api is running\" for testing purposes.</li>" +
                		"</ul>" +
                	"<h2>Handles</h2>" +
                		"<ul>" +
                			"<li>GET /handle/{prefix}/{handleid} - Returns a document with the specified handleID.</li>" +
                		"</ul>" +
                	"<h2>Search</h2>" +
                	"<ul>" +
                  		"<li>GET /search/sequences - Return list of sequences results of the query</li>" +
                  	"</ul>" +
                "</body></html> ";
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
