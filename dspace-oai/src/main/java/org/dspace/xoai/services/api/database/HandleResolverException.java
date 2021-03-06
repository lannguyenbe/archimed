/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.api.database;


public class HandleResolverException extends Exception {
    public HandleResolverException() {
    }

    public HandleResolverException(String message) {
        super(message);
    }

    public HandleResolverException(String message, Throwable cause) {
        super(message, cause);
    }

    public HandleResolverException(Throwable cause) {
        super(cause);
    }
}
