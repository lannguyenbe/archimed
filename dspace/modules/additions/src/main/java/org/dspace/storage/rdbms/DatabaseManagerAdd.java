package org.dspace.storage.rdbms;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.core.Context;

public class DatabaseManagerAdd extends DatabaseManager {
    
    /** log4j category */
    private static Logger log = Logger.getLogger(DatabaseManager.class);

    /** For bulk query */
    private static Integer BULK_FETCH_SIZE = 1000;
    
    protected DatabaseManagerAdd() {
    }
    
    public static TableRowIterator queryBulk(Context context, String query
            , Object... parameters) throws SQLException    
    {
        if (log.isDebugEnabled())
        {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < parameters.length; i++)
            {
                if (i > 0)
               {
                       sb.append(",");
               }
                sb.append(parameters[i].toString());
            }
            log.debug("Running query \"" + query + "\"  with parameters: " + sb.toString());
        }

        /* Lan 20.11.2014 : use cursor based resultset */
        PreparedStatement statement = context.getDBConnection().prepareStatement(query,
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        statement.setFetchSize(BULK_FETCH_SIZE);
        try
        {
            loadParameters(statement,parameters);

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
                }
            }

            throw sqle;
        }
    }
    

    public static TableRowIterator queryTableBulk(Context context, String table, String query
            , Object... parameters ) throws SQLException
    {
        if (log.isDebugEnabled())
        {
            StringBuilder sb = new StringBuilder("Running query \"").append(query).append("\"  with parameters: ");
            for (int i = 0; i < parameters.length; i++)
            {
                if (i > 0)
               {
                       sb.append(",");
               }
                sb.append(parameters[i].toString());
            }
            log.debug(sb.toString());
        }
        
        /* Lan 20.11.2014 : use cursor based resultset */
        PreparedStatement statement = context.getDBConnection().prepareStatement(query,
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        statement.setFetchSize(BULK_FETCH_SIZE);
        try
        {
            loadParameters(statement, parameters);

            TableRowIterator retTRI = new TableRowIterator(statement.executeQuery(), canonicalize(table));

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
                }
            }

            throw sqle;
        }
    }
    
    
    

}
