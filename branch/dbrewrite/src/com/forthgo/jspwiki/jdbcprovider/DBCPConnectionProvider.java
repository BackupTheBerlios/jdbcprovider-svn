/*
 * DBCPConnectionProvider.java
 *
 * Created on 20. marts 2006, 15:33
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.forthgo.jspwiki.jdbcprovider;

import com.ecyrd.jspwiki.InternalWikiException;
import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiEngine;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDriver;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Category;

/**
 *
 * @author glasius
 */
public class DBCPConnectionProvider extends ConnectionProvider {
    protected static final Category log = Category.getInstance( DBCPConnectionProvider.class );

    private String factory;
    private String driver;
    private String url;
    private String username;
    private String password;
    
    /** Creates a new instance of DBCPConnectionProvider */
    public DBCPConnectionProvider()  {
    }

    public void initialize(final Properties config) throws NoRequiredPropertyException {
        log.debug("Initializing DBCPConnectionProvider");

        factory = WikiEngine.getRequiredProperty(config, "dbcp.factory");
        driver = WikiEngine.getRequiredProperty(config, "dbcp.driverClassName");
        url = WikiEngine.getRequiredProperty(config, "dbcp.url");
        username = WikiEngine.getRequiredProperty(config, "dbcp.username");
        password = WikiEngine.getRequiredProperty(config, "dbcp.password");
        log.debug("-factory: "+factory+", driver: "+driver+", url: "+url+", username: "+username);
        
        GenericObjectPool connectionPool = new GenericObjectPool(null);
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(url, username, password);
        PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory,connectionPool,null,null,false,true);
        PoolingDriver drv;
        try {
            this.getClass().forName(driver);
            this.getClass().forName("org.apache.commons.dbcp.PoolingDriver");
            drv = (PoolingDriver) DriverManager.getDriver("jdbc:apache:commons:dbcp:");

            drv.registerPool("jdbcprovider",connectionPool);
        } catch (SQLException ex) {
            log.error("Failed to create ConnectionPool",ex);
            throw new InternalWikiException("SQLException during connection pool creation: "+ex.getMessage());
        } catch (ClassNotFoundException ex) {
            log.error("Failed to create ConnectionPool",ex);
            throw new InternalWikiException("ClassNotFoundException during connection pool creation: "+ex.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
            return DriverManager.getConnection("jdbc:apache:commons:dbcp:jdbcprovider");
    }


}
