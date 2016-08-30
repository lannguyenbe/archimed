package org.dspace.content;

import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

public class CommunityAdd extends Community {

    public CommunityAdd(Community community) throws SQLException {
        super(community.ourContext, community.getCommunityRow());
    }
    
    public static CommunityIterator findAllCursor(Context context) throws SQLException
    {
        String myQuery = "SELECT * FROM community";

        TableRowIterator rows = DatabaseManager.queryTable(context, "community", myQuery);

        return new CommunityIterator(context, rows);
    }
    
    public static CommunityIterator findSubcommunities(Context context, int id) throws SQLException
    {
        String myQuery = "SELECT community.* FROM community, community2community WHERE community_id = child_comm_id AND parent_comm_id = "
                + id;

        TableRowIterator rows = DatabaseManager.queryTable(context, "community", myQuery);

        return new CommunityIterator(context, rows);
    }
        

}
