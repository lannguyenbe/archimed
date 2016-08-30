package org.dspace.storage.rdbms;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.dspace.content.HandleLogIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

/**
 * Use for batch execution instead of the original DataBaseManager 
 * which raise Exception ORA-01000 [Number of open cursors exceeds ...
 * because of bug DBCP-372 in Apache commons dbcp
 * *
 * 25.09.2015 Lan: create
 * 
 * @author nln
 *
 */
public class DatabaseAccess {
    /** log4j category */
    private static final Logger log = Logger.getLogger(DatabaseAccess.class);

    private static Connection connection;

	public static TableRowIterator query(Context context, String query) throws SQLException {
		Connection conn = getConnection(context);			
        PreparedStatement statement = null;
        
        try
        {
        	statement = conn.prepareStatement(query);

            TableRowIterator retTRI = new TableRowIterator(statement.executeQuery());

            retTRI.setStatement(statement);
            return retTRI;
        }
        catch (SQLException sqle) 
        {
            if (statement != null)
            {
                try
                {
                    statement.close();
                }
                catch (SQLException s)
                {
                    log.error("SQL query close Error - ",s);
                    throw s;
                }
            }
            log.error("SQL query Error - ",sqle);
            throw sqle;
        }
    }

	public static void execute(Context context, String sql) throws SQLException {
		Connection conn = getConnection(context);			
        PreparedStatement statement = null;

        try
        {
            statement = connection.prepareStatement(sql);
            statement.execute();
        }
        catch (SQLException sqle) {
            log.error("SQL execute Error - ", sqle);
            throw sqle;
        }
        finally {
            if (statement != null) {
                try {
                    statement.close();
                }
                catch (SQLException sqle) {
                    log.error("SQL execute close Error - ", sqle);
                    throw sqle;
                }
            }
        }
    }

	public static void executeTransaction(Context context, String... sqls) throws SQLException {
		Connection conn = getConnection(context);			
        List<PreparedStatement> statements = null;

        try {
        	conn.setAutoCommit(false);
        	statements = new ArrayList<PreparedStatement>();
        	for (int i = 0, len = sqls.length; i < len; i++) {
        		PreparedStatement stmt = connection.prepareStatement(sqls[i]);
        		stmt.executeUpdate();
	            statements.add(stmt);
        	}
        	conn.commit();
        }
        catch (SQLException sqle) {
        	conn.rollback();      	
            log.error("SQL executeTransaction Error - ", sqle);
            throw sqle;
        }
        finally  {
            if (statements != null) {
                try  {
                	for (PreparedStatement stmt : statements) {
                		stmt.close();
                	}
                }
                catch (SQLException sqle) {
                    log.error("SQL executeTransaction close Error - ", sqle);
                    throw sqle;
                }
            }
            if (conn != null) {
            	conn.setAutoCommit(true);
            }
        }
    }


	public static Connection getConnection(Context context) throws SQLException {
		if (connection == null) {
//            connection = context.getDBConnection();
        	connection = getMyConnection();
		}
		return connection;
	}
	
    public static Connection getMyConnection() throws SQLException {
		try {
			Class.forName("oracle.jdbc.OracleDriver");
		} catch(ClassNotFoundException e) {
            log.error("getConnection - ",e);
			return null;
		} 

	    Connection conn = null;
	    
		Properties connectionProps = new Properties();
		connectionProps.put("user", ConfigurationManager.getProperty("db.username"));
		connectionProps.put("password", ConfigurationManager.getProperty("db.password"));

		String currentUrlString = ConfigurationManager.getProperty("db.url");

		conn = DriverManager.getConnection(currentUrlString, connectionProps);

		return conn;
	}


    public static Connection getMyConnection2() throws SQLException {
		try {
			Class.forName("oracle.jdbc.OracleDriver");
		} catch(ClassNotFoundException e) {
            log.error("getConnection - ",e);
			return null;
		} 

		Connection conn = null;
		String currentUrlString = null;

		currentUrlString= "jdbc:oracle:thin:user/password@oda11:1521:utf";
		conn = DriverManager.getConnection(currentUrlString);

		return conn;
	}

}
