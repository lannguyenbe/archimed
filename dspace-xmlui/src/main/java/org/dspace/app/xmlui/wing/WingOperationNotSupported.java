/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing;

/**
 * A class representing an error generated by the Wing framework.
 * 
 * This specific variation is to be thrown when the operation attempting to be
 * performed by the Wing Framework is not supported.
 * 
 * @author Scott Phillips
 */
public class WingOperationNotSupported extends WingException
{
    // Because exception is serializable.
    public static final long serialVersionUID = 1;

    public WingOperationNotSupported(String message)
    {
        super(message, null);
    }

    public WingOperationNotSupported(Throwable t)
    {
        super(t);
    }

    public WingOperationNotSupported(String message, Throwable t)
    {
        super(message, t);
    }

}
