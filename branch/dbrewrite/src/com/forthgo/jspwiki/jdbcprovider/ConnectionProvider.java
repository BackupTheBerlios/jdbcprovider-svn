/*
 * ConnectionProvider.java
 *
 * Created on 20. marts 2006, 15:15
 *
 */

package com.forthgo.jspwiki.jdbcprovider;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 *
 * @author glasius
 */
public abstract class ConnectionProvider {
    public abstract void initialize(Properties wikiProps) throws NoRequiredPropertyException;
    
    public abstract Connection getConnection() throws SQLException;
    
    public void releaseConnection(Connection connection)  {
        try {
            if(connection != null) {
                connection.close();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
