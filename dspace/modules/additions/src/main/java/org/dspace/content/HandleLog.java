package org.dspace.content;

import java.sql.SQLException;
import java.util.Date;

import org.apache.log4j.Logger;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.storage.rdbms.DatabaseAccess;
import org.dspace.storage.rdbms.TableRow;

public class HandleLog {

    /** Our context */
    protected Context ourContext;

    /** log4j category */
    private static final Logger log = Logger.getLogger(HandleLog.class);

    private final TableRow handleLogRow;
	
	public static final String RESOURCE_TYPE_ID = "resource_type_id";
	public static final String RESOURCE_ID = "resource_id";
	public static final String OPER = "oper";
	public static final String DATEOPER = "dateoper";
	public static final String LOGID = "logid";
	public static final String HANDLE_ID = "handle_id";
	
	private static String prefix = HandleManager.getPrefix();
	
	HandleLog(Context context, TableRow row) throws SQLException {
        ourContext = context;

        
/*        if (null == row.getTable())
        	row.setTable("t_handle_log");
*/        
        handleLogRow = row;
	}
	        
    public int getID() {
    	return handleLogRow.getIntColumn(RESOURCE_ID);
    }

    public int getType() {
    	return handleLogRow.getIntColumn(RESOURCE_TYPE_ID);
    }

    public int getLogID() {
    	return handleLogRow.getIntColumn(LOGID);
    }

    public String getOper() {
    	return handleLogRow.getStringColumn(OPER);
    }

    public Date getDateOper() {
    	return handleLogRow.getDateColumn(DATEOPER);
    }

    public int getHandleID() {
    	return handleLogRow.getIntColumn(HANDLE_ID);
    }

    public String getHandle() {
    	return prefix+"/"+handleLogRow.getIntColumn(HANDLE_ID);
    }

    public static HandleLogIterator findAll(Context context) throws SQLException
    {
        String myQuery = "SELECT l.* "
        				+ " FROM t_handle_log l";

        return new HandleLogIterator(context, myQuery);
    }

    public static HandleLogIterator findAllDel(Context context) throws SQLException
    {
        String myQuery = "SELECT l.* "
        				+ " FROM t_handle_log l"
                		+ " WHERE oper = 'DEL'"
                		+ " ORDER BY resource_type_id ASC"; // do not remove this order

        return new HandleLogIterator(context, myQuery);
    }
    
    public static HandleLogIterator findCommunities2Sync(Context context) throws SQLException
    {
    	/* Retrieve the last operation among INS or UPD on each resource_id */
    	String myQuery = "SELECT lastlog.*"
		        	    + " FROM ("
		        	    + "    SELECT l.*"
		        	    + "     , MAX(dateoper) OVER (PARTITION BY l.resource_id) lastdateoper"
		        	    + "    FROM t_handle_log l"
		        	    + "    WHERE l.resource_type_id = " + Constants.COMMUNITY
		        	    + "    AND l.oper IN ('INS','UPD')) lastlog"
		        	    + " WHERE lastlog.dateoper = lastlog.lastdateoper";

        return new HandleLogIterator(context, myQuery);
    }

    public static HandleLogIterator findCollections2Sync(Context context) throws SQLException
    {
    	/* Retrieve the last operation among INS or UPD on each resource_id */
    	String myQuery = "SELECT lastlog.*"
		        	    + " FROM ("
		        	    + "    SELECT l.*"
		        	    + "     , MAX(dateoper) OVER (PARTITION BY l.resource_id) lastdateoper"
		        	    + "    FROM t_handle_log l"
		        	    + "    WHERE l.resource_type_id = " + + Constants.COLLECTION
		        	    + "    AND l.oper IN ('INS','UPD')) lastlog"
		        	    + " WHERE lastlog.dateoper = lastlog.lastdateoper";

        return new HandleLogIterator(context, myQuery);
    }

    public static HandleLogIterator findItems2Sync(Context context) throws SQLException
    {
    	/* Retrieve the last operation among INS or UPD on each resource_id */
    	String myQuery = "SELECT lastlog.*"
		        	    + " FROM ("
		        	    + "    SELECT l.*"
		        	    + "     , MAX(dateoper) OVER (PARTITION BY l.resource_id) lastdateoper"
		        	    + "    FROM t_handle_log l"
		        	    + "    WHERE l.resource_type_id = " + + Constants.ITEM
		        	    + "    AND l.oper IN ('INS','UPD')) lastlog"
		        	    + " WHERE lastlog.dateoper = lastlog.lastdateoper";

        return new HandleLogIterator(context, myQuery);
    }
    
    public void populateLogDone() throws SQLException {
    	String myInsert = "INSERT INTO t_handle_log_done("
    					+ "     resource_type_id, resource_id, oper, dateoper, logid, handle_id, datedone)"
    					+ " WITH done AS ("
    					+ "     SELECT t.*"
    					+ "	    FROM t_handle_log t"
    					+ "     WHERE t.logid = " + this.getLogID()
    					+ " )"
    					+ " SELECT l.resource_type_id, l.resource_id, l.oper, l.dateoper, l.logid, l.handle_id, SYSTIMESTAMP"
    					+ " FROM t_handle_log l, done"
    					+ " WHERE l.resource_type_id = done.resource_type_id"
    					+ " AND l.resource_id = done.resource_id"
    					+ " AND l.dateoper <= done.dateoper";

    	String myDelete = "DELETE FROM t_handle_log h"
						+ " WHERE h.logid  in ("
    					+ " 	WITH done AS ("
    					+ "     	SELECT t.*"
    					+ "	    	FROM t_handle_log t"
    					+ "     	WHERE t.logid = " + this.getLogID()
    					+ " 	)"
    					+ " 	SELECT l.logid"
    					+ " 	FROM t_handle_log l, done"
    					+ " 	WHERE l.resource_type_id = done.resource_type_id"
    					+ " 	AND l.resource_id = done.resource_id"
    					+ " 	AND l.dateoper <= done.dateoper"
						+ ")" ;

    	DatabaseAccess.executeTransaction(ourContext, myInsert, myDelete);
    	
    }

}
