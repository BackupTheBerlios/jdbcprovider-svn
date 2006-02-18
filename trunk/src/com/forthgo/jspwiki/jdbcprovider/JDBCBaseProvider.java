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
import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiEngine;
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
public abstract class JDBCBaseProvider
{

    private Connection m_sharedConnection = null;
    private CoreConnectionPool m_connectionPool = null;
    protected String m_url = "";
    protected String m_driver = "";
    protected String m_username = "";
    protected String m_password = "";
    protected int m_cachedConnections;

    /** Name of the property that defines the url for the database connection to the pages. */
    public static final String PROP_URL = "url";

    /** Name of the property that defines the username for the database connection to the pages. */
    public static final String PROP_USERNAME = "username";

    /** Name of the property that defines the password for the database connection to the pages. */
    public static final String PROP_PASSWORD = "password";

    /** Name of the property that defines the driver for the database connection to the pages. */
    public static final String PROP_DRIVER = "driver";

    /**
     * Name of the property that defines the approx number of cached database connections.
     * 0 means to create a new connection for every operation;
     * 1 means to cache and share a single connection (let driver handle synchronization);
     * 2+ means use a ConnectionPool with that many min connections.
     */
    public static final String PROP_CACHED_CONNECTIONS = "cachedConnections";
    private WikiEngine m_engine;
    private Properties m_queries = new Properties();    // our copy of query strings with short keys

    /**
     * @throws java.io.FileNotFoundException If the specified page directory does not exist.
     * @throws java.io.IOException           In case the specified page directory is a file, not a directory.
     */
    public void initialize(WikiEngine engine, Properties properties)
            throws NoRequiredPropertyException,
            IOException
    {
        m_engine = engine;
        m_username = getRequired( properties, PROP_USERNAME );
        m_password = getRequired( properties, PROP_PASSWORD );
        m_driver = getRequired( properties, PROP_DRIVER );
        m_url = getRequired( properties, PROP_URL );
        m_cachedConnections = TextUtil.getIntegerProperty( properties, getPropertyBase() + PROP_CACHED_CONNECTIONS, 0 );

        checkDriver();

        // make sure the queries we need are in the properties
        loadQueries(properties, new String[]{"checkConnection"});
        loadQueries(properties, getQueryKeys());   // from subclass

        m_sharedConnection = null;
        m_connectionPool = null;
        try
        {
            if( m_cachedConnections == 0 )
                ;// do nothing
            else if( m_cachedConnections == 1 )
                createSharedConnection();
            else
                createConnectionPool();

            checkConnection();

            // check that required tables already exist
            int n = 1;
            while (true) {
                // SELECT COUNT(*) FROM tableName;
                String query = getOptional(properties, "check." + n);
                if (query == null)
                    break;
                checkQuery(query);
            }
        }
        catch( SQLException e )
        {
            throw new IOException( "SQL Exception: " + e.getMessage() );
        }
    }

    protected abstract String[] getQueryKeys();

    public String getQuery(String key) {
        return m_queries.getProperty(key);
    }

    /**
     * Loads query strings from wiki properties into our local properties list.
     * Unnecessary to keep a separate list, but we have to check them for existence,
     * anyway.
     * @throws NoRequiredPropertyException
     */
    protected void loadQueries(Properties properties, String [] queryKeys) throws NoRequiredPropertyException {
        for (int i = 0; i < queryKeys.length; i++) {
            String queryKey = queryKeys[i];
            m_queries.setProperty(queryKey, getRequired(properties, queryKey));
        }
    }

    private String getRequired(Properties properties, String name)
            throws NoRequiredPropertyException {
        return WikiEngine.getRequiredProperty(properties, getPropertyBase() + name);
    }

    private String getOptional(Properties properties, String name) {
        return properties.getProperty(name);
    }

    /**
     * Returns a string to use as a prefix for property names.
     * 
     * @return property name prefix
     */
    protected abstract String getPropertyBase();

    public abstract Category getLog();

    protected void debug( String message )
    {
        getLog().debug( message );
    }

    protected void info( String message )
    {
        getLog().info( message );
    }

    protected void error( String message, Throwable t )
    {
        getLog().error( message, t );
    }

    private void createConnectionPool() throws SQLException
    {
        try
        {
            m_connectionPool = new CoreConnectionPool(
                    m_driver, m_url, m_username, m_password,
                    m_cachedConnections, m_cachedConnections * 4, true );
        }
        catch( SQLException e )
        {
            error( "Unable to get database connection pool: " + m_url, e );
            throw e;
        }
    }

    private void createSharedConnection() throws SQLException
    {
        try
        {
            m_sharedConnection = DriverManager.getConnection( m_url, m_username, m_password );
        }
        catch( SQLException e )
        {
            error( "Unable to get database connection: " + m_url, e );
            throw e;
        }
    }

    private void checkDriver() throws IOException
    {
        try
        {
            Class.forName( m_driver );
        }
        catch( ClassNotFoundException e )
        {
            throw new IOException( "Unable to find database driver: " + m_driver );
        }
    }

    private void checkConnection() throws SQLException
    {
        Connection connection = null;
        try
        {
            connection = getConnection();
        }
        catch( SQLException e )
        {
            error( "Unable to get database connection: " + m_url, e );
            throw e;
        }
        finally
        {
            releaseConnection( connection );
        }
    }

    public boolean isConnectionOK(Connection connection)
    {
		try {
			String sql = getQuery("checkAlive");
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			rs.close();
			stmt.close();
		}
		catch (SQLException se) {
			return false;
		}
		return true;
    }

    // public instead of protected by user request
    public Connection getConnection() throws SQLException
    {
//        showMemory("getConnection");
        if( m_sharedConnection != null )
        {
            if( !isConnectionOK(m_sharedConnection) )
                createSharedConnection();
            return m_sharedConnection;
        }
        else if( m_connectionPool != null )
        {
            return m_connectionPool.getConnection(this);
        }
        else
        {
            return DriverManager.getConnection( m_url, m_username, m_password );
        }
    }

    // public instead of protected by user request
    public void releaseConnection( Connection connection )
    {
        if( connection == null )
            return; // happens if we are called in a finally clause of a failed getConnection
        if( m_sharedConnection != null )
        {
            // do nothing, shared connection is kept alive
        }
        else if( m_connectionPool != null )
        {
            m_connectionPool.free( connection );
        }
        else
        {
            try
            {
                connection.close();
            }
            catch( SQLException e )
            {
                error( "connection failed to close", e );
            }
        }
//        showMemory("releaseConnection");
    }

    /**
     * Checks that a table by the given name exists.
     * 
     * @param query the sql squery
     * @throws SQLException if the table query fails
     */
    protected void checkQuery( String query ) throws SQLException
    {
        Connection connection = null;
        try
        {
            connection = getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery( query );
            rs.close();
            stmt.close();
        }
        catch( SQLException se )
        {
            error( "Check failed: " + query, se );
            throw se;
        }
        finally
        {
            releaseConnection( connection );
        }
    }

    protected WikiEngine getEngine() {
        return m_engine;
    }
}