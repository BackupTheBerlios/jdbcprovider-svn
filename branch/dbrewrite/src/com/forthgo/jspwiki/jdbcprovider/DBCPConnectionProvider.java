/*
 * DBCPConnectionProvider.java
 *
 * Created on 20. marts 2006, 15:33
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.forthgo.jspwiki.jdbcprovider;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDriver;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.InternalWikiException;
import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiEngine;

/*
 * History:
 * 	 2007-02-13 MT Got rid of 'factory' property since it's not used anyway
 *                 Changed logging to log4j.Logger in stead of deprecateded log4j.Category
 */

/**
 * @author Mikkel Troest
 * @author glasius
 */
public class DBCPConnectionProvider extends ConnectionProvider {
    protected static final Logger log = Logger.getLogger( DBCPConnectionProvider.class );

    private String driver;
    private String url;
    private String username;
    private String password;
    
    /** Creates a new instance of DBCPConnectionProvider */
    public DBCPConnectionProvider()  {
    }

    public void initialize(WikiEngine engine, final Properties config) throws NoRequiredPropertyException {
        log.debug("Initializing DBCPConnectionProvider");

        driver = WikiEngine.getRequiredProperty(config, "dbcp.driverClassName");
        url = WikiEngine.getRequiredProperty(config, "dbcp.url");
        username = WikiEngine.getRequiredProperty(config, "dbcp.username");
        password = WikiEngine.getRequiredProperty(config, "dbcp.password");
        log.debug("driver: "+driver+", url: "+url+", username: "+username);
        
        GenericObjectPool connectionPool = new GenericObjectPool(null);

        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(url, username, password);
        
        new PoolableConnectionFactory(connectionFactory,connectionPool,null,null,false,true);
		
        PoolingDriver drv;
        
        try {
            Class.forName(driver);
            Class.forName("org.apache.commons.dbcp.PoolingDriver");
            drv = (PoolingDriver) DriverManager.getDriver("jdbc:apache:commons:dbcp:");

            drv.registerPool(engine.getApplicationName(),connectionPool);
            
        } catch (SQLException ex) {
            log.error("Failed to create ConnectionPool",ex);
            throw new InternalWikiException("SQLException during connection pool creation: "+ex.getMessage());
        } catch (ClassNotFoundException ex) {
            log.error("Failed to create ConnectionPool",ex);
            throw new InternalWikiException("ClassNotFoundException during connection pool creation: "+ex.getMessage());
        }
    }

    
    public Connection getConnection(WikiEngine engine) throws SQLException {
            return DriverManager.getConnection("jdbc:apache:commons:dbcp:"+engine.getApplicationName());
    }
    
}
