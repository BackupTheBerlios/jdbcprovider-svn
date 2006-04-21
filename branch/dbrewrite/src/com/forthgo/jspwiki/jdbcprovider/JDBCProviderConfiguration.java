/*
 * JDBCProviderConfiguration.java
 *
 * Created on 20. marts 2006, 15:04
 *
 */

package com.forthgo.jspwiki.jdbcprovider;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiException;
import java.io.IOException;
import java.sql.Connection;
import java.util.Properties;

/**
 *
 * @author glasius
 */
public class JDBCProviderConfiguration {
    Properties config;
    Properties sql;
    private ConnectionProvider connectionProvider;
    /** Creates a new instance of JDBCProviderConfiguration */
    private JDBCProviderConfiguration(String configPath) throws IOException, NoRequiredPropertyException {
        config = new Properties();
        sql = new Properties();

        config.load(getClass().getResourceAsStream(configPath));
        
        String connectionProviderClass = WikiEngine.getRequiredProperty(config, "connectionProvider");
        
        // Instantiate the connectionProvider by dynamic class loading
        
        // Load the sql properties here....
    }
    
    public Connection getConnection() throws WikiException {
        return connectionProvider.getConnection();
    }

    public String getSql(String key) {
        return sql.getProperty(key);
    }
}
