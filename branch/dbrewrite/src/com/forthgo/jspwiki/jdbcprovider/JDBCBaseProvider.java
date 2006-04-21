/*
    JSPWiki - a JSP-based WikiWiki clone.
 
    Copyright (C) 2004-2005 Xan Gregg (xan.gregg@forthgo.com)
 
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.
 
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.
 
    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.forthgo.jspwiki.jdbcprovider;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiException;
import com.ecyrd.jspwiki.WikiProvider;
import java.io.File;
import java.io.FileNotFoundException;
import org.apache.log4j.Category;

import java.io.IOException;
import java.sql.*;
import java.util.Properties;

/*
 * History:
 *   2006-02-12 XG Move all SQL statements into property file.
 *                 Expand implementation of Delete.
 *   2006-01-29 XG Update for JSPWiki 2.3.72 (with help from Terry Steichen)
 *                  - added engine member variable since some calls require it now
 *                 Updated check-alive query to work with more DBs (add getter for table name).
 *                 Made get/release connections calls public by user request.
 *   2005-09-28 XG Made default number of cached connections be 0 after seeing resource
 *                 problems with other values on Linux.
 *   2005-08-30 XG Fixed bug: was using wrong property name for cached connection setting.
 */

/**
 * An abstract base class for JDBC-based providers.
 * This class provides three means of connection management (because I don't know
 * JDBC well enough to know which is best) via the cachedConnections
 * property.  A value of 0 means none cached; each connection is created
 * on demand.  A value of 1 means all operations share a single connection,
 * relying on the driver to take care of any synchronization issues. A
 * value greater than 1 indicates the minimum number of connections for
 * a connection pool.
 *
 */
public abstract class JDBCBaseProvider implements WikiProvider {
    
    protected boolean m_migrating = false; // Only used during migration process
    
    private WikiEngine m_engine;
    
    private static JDBCProviderConfiguration config;
    
    /**
     * @throws java.io.FileNotFoundException If the specified page directory does not exist.
     * @throws java.io.IOException           In case the specified page directory is a file, not a directory.
     */
    public void initialize(WikiEngine engine, Properties properties)
    throws NoRequiredPropertyException,
            IOException {
        
        m_engine = engine;
        
        if(config == null) {
            String configPath = WikiEngine.getRequiredProperty(properties,"jspwiki.pageProvider.configuration");
            debug("configPath: "+configPath);
            config = new JDBCProviderConfiguration(configPath);
        }
    }
    
    protected JDBCProviderConfiguration getConfig() {
        return config;
    }
    
    public String getSQL(String key) {
        return config.getSql(key);
    }
    
    public abstract Category getLog();
    
    protected void debug( String message ) {
        getLog().debug( message );
    }
    
    protected void info( String message ) {
        getLog().info( message );
    }
    
    protected void error( String message, Throwable t ) {
        getLog().error( message, t );
    }
    
    
    private void checkConnection() throws SQLException {
        Connection connection = null;
        try {
            connection = getConnection();
        } catch( SQLException e ) {
            error( "Unable to get database connection", e );
            throw e;
        } finally {
            releaseConnection( connection );
        }
    }
    
    public boolean isConnectionOK(Connection connection) {
        try {
            String sql = getSQL("checkConnection");
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            rs.close();
            stmt.close();
        } catch (SQLException se) {
            return false;
        }
        return true;
    }
    
    // public instead of protected by user request
    public Connection getConnection() throws SQLException  {
        return config.getConnection();
    }
    
    // public instead of protected by user request
    public void releaseConnection( Connection connection ) {
        config.releaseConnection(connection);
    }
    
    /**
     * Checks that a query runs without error.
     *
     * @param query the sql query
     * @throws SQLException if the query fails
     */
    protected void checkQuery( String query ) throws SQLException {
        Connection connection = null;
        try {
            connection = getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery( query );
            rs.close();
            stmt.close();
        } catch( SQLException se ) {
            error( "Check failed: " + query, se );
            throw se;
        } finally {
            releaseConnection( connection );
        }
    }
    
    protected WikiEngine getEngine() {
        return m_engine;
    }
}