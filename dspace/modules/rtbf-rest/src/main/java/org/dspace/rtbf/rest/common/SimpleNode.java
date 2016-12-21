package org.dspace.rtbf.rest.common;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SimpleNode {
    
    public enum Attribute {
        TITLE, NAME, KEY
    }
	
	private String title;
    private String name;
    private String key;
	
	public SimpleNode() {}
	
	public SimpleNode(String t) {
		this.title = t;
	}
	
	public String getTitle() {
		return this.title;
	}
	
    public String getName() {
        return this.name;
    }

    public String getKey() {
        return this.key;
    }

    public void setTitle(String s) {
		this.title = s;
	}

    public void setName(String s) {
        this.name = s;
    }
    
    public void setKey(String s) {
        this.key = s;
    }

    public SimpleNode setAttribute(Attribute name, String val) {
        switch (name) {
        case TITLE:
            this.title = val;
            break;
        case NAME:
            this.name = val;
            break;
        case KEY:
            this.key = val;
            break;
        default:
            break;
        }
        
        return this;
        
    }

	
}
