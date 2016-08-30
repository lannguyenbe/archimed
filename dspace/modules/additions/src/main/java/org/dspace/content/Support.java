package org.dspace.content;

import org.apache.log4j.Logger;
import org.dspace.storage.rdbms.TableRow;

public abstract class Support {

	/** log4j category */
    private static final Logger log = Logger.getLogger(Support.class);
    
    protected String code_origine;
    protected String type;
    protected String set;
    protected String place;
    protected String key_frame_offset;
    protected double tc_in;
    protected double tc_out;
    protected double htc_in;
    protected double htc_out;
    protected double duration;
    protected String tc_in_string;
    protected String tc_out_string;
    protected String htc_in_string;
    protected String htc_out_string;
    protected String duration_string;
    protected String origine;
    protected String category;
    protected String role;
    protected String sound_format;
    protected String image_format;
    protected String image_ratio;
    protected String image_color;
    
    
    public Support() {}
    
    public Support(TableRow row){
    	this.code_origine = row.getStringColumn("code_origine");
    	this.type = row.getStringColumn("support_type");
    	this.set = row.getStringColumn("set_of_support_type");
    	this.place = row.getStringColumn("support_place");
    	this.key_frame_offset = row.getStringColumn("key_frame_offset");
    	this.tc_in = row.getLongColumn("tc_in");
    	this.tc_out = row.getLongColumn("tc_out");
    	this.htc_in = row.getLongColumn("htc_in");
    	this.htc_out = row.getLongColumn("htc_out");
    	this.duration = row.getLongColumn("duration");
    	this.tc_in_string = row.getStringColumn("tc_in_string");
    	this.tc_out_string = row.getStringColumn("tc_out_string");
    	this.htc_in_string = row.getStringColumn("htc_in_string");
    	this.htc_out_string = row.getStringColumn("htc_out_string");
    	this.duration_string = row.getStringColumn("duration_string");
    	this.origine = row.getStringColumn("origine");
    	this.category = row.getStringColumn("category");
    	this.role = row.getStringColumn("support_role");
    	this.sound_format = row.getStringColumn("sound_format");
    	this.image_format = row.getStringColumn("image_format");
    	this.image_ratio = row.getStringColumn("image_ratio");
    	this.image_color = row.getStringColumn("image_color");
    }

    public String getCode_origine() {
		return code_origine;
	}
	public String getType() {
		return type;
	}
	public String getSet() {
		return set;
	}
	public String getPlace() {
		return place;
	}
	public String getKey_frame_offset() {
		return key_frame_offset;
	}
	public double getTc_in() {
		return tc_in;
	}
	public double getTc_out() {
		return tc_out;
	}
	public double getDuration() {
		return duration;
	}
	public String getTc_in_string() {
		return tc_in_string;
	}
	public String getTc_out_string() {
		return tc_out_string;
	}
	public String getDuration_string() {
		return duration_string;
	}
	public String getOrigine() {
		return origine;
	}
	public String getCategory() {
		return category;
	}
	public double getHtc_in() {
		return htc_in;
	}
	public double getHtc_out() {
		return htc_out;
	}
	public String getHtc_in_string() {
		return htc_in_string;
	}
	public String getHtc_out_string() {
		return htc_out_string;
	}

	public String getRole() {
		return role;
	}

	public String getSound_format() {
		return sound_format;
	}

	public String getImage_format() {
		return image_format;
	}

	public String getImage_ratio() {
		return image_ratio;
	}

	public String getImage_color() {
		return image_color;
	}

}
