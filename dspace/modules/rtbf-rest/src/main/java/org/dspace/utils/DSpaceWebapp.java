/** Lan 09.11.2015 : this file is copied from dspace-xmlui because we make use of
 *  dspace-api/.../DSpaceContextListener in web.xml to init org.dspace.*.Context
 *  without this class, there is a message when starting the Webapp : 
 *  "2015-11-09 11:41:49.241:INFO:/rs:main: Can't create webapp MBean:  org.dspace.utils.DSpaceWebapp"
 *  and when closing the webapp an excaption is thrown :
 *  NullPointer Exception on DSpaceContextListener.contextDestroyed()
 *  
 */

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.utils;

import org.dspace.app.util.AbstractDSpaceWebapp;

/**
 * An MBean to identify this web application.
 *
 * @author mwood
 */
public class DSpaceWebapp
        extends AbstractDSpaceWebapp
{
    public DSpaceWebapp()
    {
        super("RTBF-REST");
    }

    @Override
    public boolean isUI()
    {
        return false;
    }
}
