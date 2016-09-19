package org.dspace.rtbf.rest.common;

import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.log4j.Logger;
import org.dspace.discovery.DiscoverResult.GroupFilter;
import org.dspace.rtbf.rest.common.RTBObject;
import org.dspace.rtbf.rest.search.SearchResponseParts;

public class RTBObjectParts {
    private static Logger log = Logger.getLogger(RTBObjectParts.class);
	
		
		public static class Diffusion {
		
			private String channel;
			private String date_diffusion;
		    protected List<RTBObject> owningParentList;
		    
		    public Diffusion() {}
		    
		    public Diffusion(String channel, String dt, List<RTBObject> list) {
		    	setChannel(channel);
		    	setDate_diffusion(dt);
		    	setOwningParentList(list);
		    }
		    
			public String getChannel() {
				return channel;
			}
			public void setChannel(String channel) {
				this.channel = channel;
			}
			public String getDate_diffusion() {
				return date_diffusion;
			}
			public void setDate_diffusion(String date_diffusion) {
				this.date_diffusion = date_diffusion;
			}
	
			@XmlElementWrapper( name = "owningParentList")
			@XmlElement( name = "owningParent")
			public List<RTBObject> getOwningParentList() {
				return owningParentList;
			}
	
			public void setOwningParentList(List<RTBObject> owningParentList) {
				this.owningParentList = owningParentList;
			}
		}
		
		public static class Support {

		    private String code_origine;
		    private String type;
		    private String set;
		    private String place;
		    private String key_frame_offset;
		    private String tc_in;
		    private String tc_out;
		    private String htc_in;
		    private String htc_out;
		    private String duration;
		    private String origine;
		    private String category;
		    private String role;
		    private String sound_format;
		    private String image_format;
		    private String image_ratio;
		    private String image_color;
		    
		    public Support() {}
		    
		    public Support(org.dspace.content.Support support) {
		    	this.code_origine = support.getCode_origine();
		    	this.type = support.getType();
		    	this.set = support.getSet();
		    	this.place = support.getPlace();
		    	this.key_frame_offset = support.getKey_frame_offset();
		    	this.tc_in = support.getTc_in_string();
		    	this.tc_out = support.getTc_out_string();
		    	this.htc_in = support.getHtc_in_string();
		    	this.htc_out = support.getHtc_out_string();
		    	this.duration = support.getDuration_string();
		    	this.origine = support.getOrigine();
		    	this.category = support.getCategory();
		    	this.role = support.getRole();
		    	this.sound_format = support.getSound_format();
		    	this.image_format  = support.getImage_format();
		    	this.image_ratio = support.getImage_ratio();
		    	this.image_color = support.getImage_color();
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

			public String getTc_in() {
				return tc_in;
			}

			public String getTc_out() {
				return tc_out;
			}

			public String getDuration() {
				return duration;
			}

			public String getOrigine() {
				return origine;
			}

			public String getCategory() {
				return category;
			}

			public void setCode_origine(String code_origine) {
				this.code_origine = code_origine;
			}

			public void setType(String type) {
				this.type = type;
			}

			public void setSet(String set) {
				this.set = set;
			}

			public void setPlace(String place) {
				this.place = place;
			}

			public void setKey_frame_offset(String key_frame_offset) {
				this.key_frame_offset = key_frame_offset;
			}

			public void setTc_in(String tc_in) {
				this.tc_in = tc_in;
			}

			public void setTc_out(String tc_out) {
				this.tc_out = tc_out;
			}

			public void setDuration(String duration) {
				this.duration = duration;
			}

			public void setOrigine(String origine) {
				this.origine = origine;
			}

			public void setCategory(String category) {
				this.category = category;
			}

			public String getHtc_in() {
				return htc_in;
			}

			public String getHtc_out() {
				return htc_out;
			}

			public void setHtc_in(String htc_in) {
				this.htc_in = htc_in;
			}

			public void setHtc_out(String htc_out) {
				this.htc_out = htc_out;
			}

			public String getRole() {
				return role;
			}

			public void setRole(String role) {
				this.role = role;
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

			public void setSound_format(String sound_format) {
				this.sound_format = sound_format;
			}

			public void setImage_format(String image_format) {
				this.image_format = image_format;
			}

			public void setImage_ratio(String image_ratio) {
				this.image_ratio = image_ratio;
			}

			public void setImage_color(String image_color) {
				this.image_color = image_color;
			}

		}
	
		// 19.09.2016 Lan
		public static class GroupCount {
			
			public static class Filter {
				public String filtertype;
				public String filter_relational_operator;
				public String filter;
				
				public Filter() {}
				public Filter(String ft, String fo, String fv) {
					this.filtertype = ft;
					this.filter_relational_operator = fo;
					this.filter = fv;				
				}
			}
			
			private long count;
			private Filter fq;

	 		public GroupCount(GroupFilter f) {
	 			this.count = f.getCount();
	 			this.fq = new Filter(f.getFilterType(), f.getFilterOper(), f.getFilterValue());
	 		}

			public long getCount() {
				return count;
			}

			public void setCount(long count) {
				this.count = count;
			}

			// 19.09.2016 Lan : this adapter is mandatory for jaxb to work but I still donot understand why */
			@XmlJavaTypeAdapter(RTBObjectParts.GroupFilterAdapter.class)
			public GroupCount.Filter getFq() {
				return fq;
			}

			public void setFq(Filter fq) {
				this.fq = fq;
			}

		}
		
		/*
		 * Jaxb Adapters
		 */
		// 19.09.2016 Lan : this adapter is mandatory for jaxb to work but I still donot understand why */
		public static class GroupFilterAdapter extends XmlAdapter<GroupFilterAdapter.AdapteFilter, RTBObjectParts.GroupCount.Filter> {
			public static class AdapteFilter {
				public String filtertype;
				public String filter_relational_operator;
				public String filter;
			}

			@Override
			public RTBObjectParts.GroupCount.Filter unmarshal(
					GroupFilterAdapter.AdapteFilter v) throws Exception {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public GroupFilterAdapter.AdapteFilter marshal(
					RTBObjectParts.GroupCount.Filter v) throws Exception {
				if (v == null) { return null; }
				
				AdapteFilter adapte = new AdapteFilter();
				adapte.filter = v.filter;
				adapte.filtertype = v.filtertype;
				adapte.filter_relational_operator = v.filter_relational_operator;
				return adapte;	
				
			}
		}
				
				
}
