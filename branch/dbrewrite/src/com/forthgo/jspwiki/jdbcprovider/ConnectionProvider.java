/*
 * ConnectionProvider.java
 *
 * Created on 20. marts 2006, 15:15
 *
 */


package com.forthgo.jspwiki.jdbcprovider;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiEngine;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/*
 * History:
 *   2006-04-26 MT  Added connection = null to releaseConnection() to make sure
 *                  the connection is not accidentially closed again while being
 *                  re-used from the pool.
 */

/**
 * @author Mikkel Troest
 * @author glasius
 */
public abstract class ConnectionProvider {
    public abstract void initialize(WikiEngine engine, Properties wikiProps) throws NoRequiredPropertyException;
    
    public abstract Connection getConnection(WikiEngine engine) throws SQLException;
    
    public void releaseConnection(Connection connection)  {
        try {
            if(connection != null) {
                connection.close();
                connection = null;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
