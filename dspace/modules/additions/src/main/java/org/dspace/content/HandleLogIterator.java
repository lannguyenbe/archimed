package org.dspace.content;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseAccess;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Specialized iterator for HandleLog. Inspired by ItemIterator
 * 
 * @author Lan Nguyen
 */
public class HandleLogIterator {
    /** log4j category */
    private static final Logger log = Logger.getLogger(HandleLogIterator.class);
	
	private Context ourContext;
	private TableRowIterator handleLogRows;
	
	
	public HandleLogIterator(Context context, String query, Object... parameters ) throws SQLException 
	{
		TableRowIterator rows = DatabaseAccess.query(context, query);

		ourContext = context;
		handleLogRows = rows;
		
	}
	
	public boolean hasNext() throws SQLException {
		if (handleLogRows != null) {
			return handleLogRows.hasNext();
		}
		return false;
	}

	public HandleLog next() throws SQLException {
		if (handleLogRows.hasNext()) {
			return new HandleLog(ourContext, handleLogRows.next());
		}
		return null;
	}
	
    public void close() {
		if (handleLogRows != null) {
			handleLogRows.close();
		}
    }

	
	/**
	 * Inner class - not used
	 * @author nln
	 *
	 */
	class TableRowIteratorInner {
	    /**
	     * Results from a query
	     */
	    private ResultSet results;

	    /**
	     * Statement used to submit the query
	     */
	    private Statement statemt = null;

	    /**
	     * Statement used to submit the query
	     */
	    private Connection connection = null;

	    /**
	     * The name of the RDBMS table
	     * not use
	    private String table; 
	     */

	    /**
	     * True if there is a next row
	     */
	    private boolean hasNext = true;

	    /**
	     * True if we have already advanced to the next row.
	     */
	    private boolean hasAdvanced = false;

	    /**
	     * Column names for the results in this query
	     */
	    private int numcols = 0;
	    private List<String> columnNames = null;
	    private List<Integer> columnTypes = null;


		public TableRowIteratorInner(ResultSet results) {
			this.results = results;

			try {
				ResultSetMetaData meta = results.getMetaData();
				
				columnNames = new ArrayList<String>();
				columnTypes = new ArrayList<Integer>();
				numcols = meta.getColumnCount();
				for (int i = 0; i < numcols; i++) {
					columnNames.add(meta.getColumnLabel(i + 1));
					columnTypes.add(meta.getColumnType(i + 1));
				}
				
			} catch (SQLException e) {
				columnNames = null;
				columnTypes = null;
				numcols = 0;
			}
		}
		
	    public boolean hasNext() throws SQLException
	    {
	        if (results == null)
	        {
	            close();
	            return false;
	        }

	        if (hasAdvanced)
	        {
	            return hasNext;
	        }

	        hasAdvanced = true;
	        hasNext = results.next();

	        // No more results
	        if (!hasNext)
	        {
	            close();
	        }

	        return hasNext;
	    }

	    public TableRow next() throws SQLException
	    {
	        if (results == null)
	        {
	            return null;
	        }

	        if (!hasNext())
	        {
	            return null;
	        }

	        hasAdvanced = false;

	        return process(results);
	    }
	    
	    private TableRow process(ResultSet results) throws SQLException {
	    	TableRow row = new TableRow(null, columnNames);

	    	for (int i = 0, j = i+1; i < numcols; i++, j++) {
	    		int jdbctype = columnTypes.get(i);
	    		String name = columnNames.get(i);

	    		switch (jdbctype) {
		    		case Types.BOOLEAN:
		    		case Types.BIT:
		    			row.setColumn(name, results.getBoolean(j));
		    			break;
		    			
		    		case Types.INTEGER: /* isOracle */
		    			long longValue = results.getLong(j);
		    			if (longValue <= (long)Integer.MAX_VALUE) {
		    				row.setColumn(name, (int) longValue);
		    			} else {
		    				row.setColumn(name, longValue);
		    			}
		    			break;
		    				    			
	                case Types.BIGINT:
	                    row.setColumn(name, results.getLong(j));
	                    break;
	
	                case Types.NUMERIC:
	                case Types.DECIMAL:
	                    row.setColumn(name, results.getBigDecimal(j));
	                    break;
	
	                case Types.DOUBLE:
	                    row.setColumn(name, results.getDouble(j));
	                    break;
	
	                case Types.CLOB: /* is Oracle */
	                	row.setColumn(name, results.getString(j));
	                	break;
	                	
	                case Types.VARCHAR:
	                    try
	                    {
	                        byte[] bytes = results.getBytes(j);
	
	                        if (bytes != null)
	                        {
	                            String mystring = new String(results.getBytes(j), "UTF-8");
	                            row.setColumn(name, mystring);
	                        }
	                        else
	                        {
	                            row.setColumn(name, results.getString(j));
	                        }
	                    }
	                    catch (UnsupportedEncodingException e)
	                    {
	                        log.error("Unable to parse text from database", e);
	                    }
	                    break;
	
	                case Types.DATE:
	                    row.setColumn(name, results.getDate(j));
	                    break;
	
	                case Types.TIME:
	                    row.setColumn(name, results.getTime(j));
	                    break;
	
	                case Types.TIMESTAMP:
	                    row.setColumn(name, results.getTimestamp(j));
	                    break;
	
	                default:
	                    throw new IllegalArgumentException("Unsupported JDBC type: " + jdbctype);
	    		}

		    	if (results.wasNull()) {
		    		row.setColumnNull(name);
		    	}
	    	}

	    	// not changed
	    	// row.resetChanged(); // function not visible
	    	return row;
	    }


		public void setStatement(PreparedStatement statement) {
			this.statemt = statement;
		}

		public void setConnection(Connection conn) {
			this.connection = conn;			
		}

	    /**
	     * Close the Iterator and release any associated resources
	     */
	    public void close()
	    {
	        try
	        {
	            if (results != null) { results.close(); results = null; }
	            if (statemt != null) { statemt.close(); statemt = null; }
//	            if (connection != null) { connection.close(); connection = null; }
	        }
	        catch (SQLException sqle)
	        {
	        }

	        columnNames = null;
	        columnTypes = null;
	        numcols = 0;
	    }

	
	} /* class TableRowIterator */

}
