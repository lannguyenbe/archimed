package org.dspace.content;

import org.apache.log4j.Logger;
import org.dspace.storage.rdbms.TableRow;

public abstract class CodeOrigine {

	/** log4j category */
    private static final Logger log = Logger.getLogger(CodeOrigine.class);
    
    /** Type of code origine objects in index */
    public static final int RESOURCE_ID = 1001;
    public static final String INDEX_FIELD_NAME = "code_origine";

    /** The table row corresponding to this item */
    private final TableRow codeOrigineRow;
    
    
    public CodeOrigine(TableRow row) {
    	codeOrigineRow = row;
    }
    
    public int getID() 
    {
        return codeOrigineRow.getIntColumn("id");
    }

    public String getCode() 
    {
        return codeOrigineRow.getStringColumn("code_origine");
    }

    public int getTopCommunityID() 
    {
        return codeOrigineRow.getIntColumn("topcommunity_id");
    }

}
