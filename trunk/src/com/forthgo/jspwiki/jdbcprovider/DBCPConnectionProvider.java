/*
 * DBCPConnectionProvider.java
 *
 * Created on 20. marts 2006, 15:33
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.forthgo.jspwiki.jdbcprovider;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDriver;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 *
 * @author glasius
 */
public class DBCPConnectionProvider implements ConnectionProvider {

    private String factory;
    private String driver;
    private String url;
    private String username;
    private String password;
    
    /** Creates a new instance of DBCPConnectionProvider */
    public DBCPConnectionProvider(Properties config) throws NoRequiredPropertyException {
        factory = WikiEngine.getRequiredProperty(config, "dbcp.factory");
        driver = WikiEngine.getRequiredProperty(config, "dbcp.driverClassName");
        url = WikiEngine.getRequiredProperty(config, "dbcp.url");
        username = WikiEngine.getRequiredProperty(config, "dbcp.username");
        password = WikiEngine.getRequiredProperty(config, "dbcp.password");

        GenericObjectPool connectionPool = new GenericObjectPool(null);
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(url, username, password);
        PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory,connectionPool,null,null,false,true);
        PoolingDriver driver = new PoolingDriver();
        driver.registerPool("jdbcprovider",connectionPool);

    }

    public Connection getConnection() throws WikiException {
        try {
            return DriverManager.getConnection("jdbc:apache:commons:dbcp:jdbcprovider");
        } catch (SQLException ex) {
            throw new WikiException(ex.getMessage());
        }
    }
    
}
