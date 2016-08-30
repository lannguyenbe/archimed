/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.apache.log4j.Logger;

/**
 * Abstract base class for DSpace objects
 */
public abstract class Diffusion
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(Diffusion.class);

    protected int community_id;
    protected int collection_id;
    protected int item_id;
	protected String diffusion_path;
	protected String date_event;
	protected String date_diffusion;
	protected String channel;

	
	public int getCommunity_id() {
		return community_id;
	}

	public int getCollection_id() {
		return collection_id;
	}

	public int getItem_id() {
		return item_id;
	}

	public String getDiffusion_path() {
		return diffusion_path;
	}

	public String getDate_event() {
		return date_event;
	}

	public String getDate_diffusion() {
		return date_diffusion;
	}

	public String getChannel() {
		return channel;
	}
	
}