/*
 * DatabaseConnectionProvider.java
 *
 * Created on 20. marts 2006, 15:20
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

/**
 *
 * @author glasius
 */
public class DatabaseConnectionProvider extends ConnectionProvider {
    
    private String url;
    private String username;
    private String password;
    
    /** Creates a new instance of DatabaseConnectionProvider */
    public DatabaseConnectionProvider() {
    }

    public void initialize(WikiEngine engine, final Properties config) throws NoRequiredPropertyException {
        String driver = WikiEngine.getRequiredProperty(config, "jdbc.driverClassName");
        url = WikiEngine.getRequiredProperty(config, "jdbc.url");
        username = WikiEngine.getRequiredProperty(config, "jdbc.username");
        password = WikiEngine.getRequiredProperty(config, "jdbc.password");
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException ex) {
            throw new InternalWikiException("Database driver could not load. Class not found. Find driver for: "+driver);
        }
    }
    
    public Connection getConnection(WikiEngine engine) throws SQLException {
        return DriverManager.getConnection( url, username, password );
    }
    
}
