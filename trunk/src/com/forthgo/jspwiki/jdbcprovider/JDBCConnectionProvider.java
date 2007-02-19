/*
 * JDBCConnectionProvider.java
 *
 * Created on 20. marts 2006, 15:20
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.forthgo.jspwiki.jdbcprovider;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.InternalWikiException;
import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiEngine;

/*
 * History:
 *   2007-02-18 MT  Removed members username, password - moved to properties
 *   2007-02-17 MT  Added support for additional connection properties
 * 	 2007-02-14 MT  Renamed this class to JDBCConnectionProvider
 */

/**
 * @author Mikkel Troest
 * @author glasius
 */
public class JDBCConnectionProvider extends ConnectionProvider {
  
		protected static final Logger log = Logger.getLogger(JDBCConnectionProvider.class);
	
		private static final String PROP_PREFIX = "jdbc";
	
		private String driver;
    private String url;
    
    private Properties connectionProperties;
    
    /** Creates a new instance of JDBCConnectionProvider */
    public JDBCConnectionProvider() {
    }

    public void initialize(WikiEngine engine, final Properties config) throws NoRequiredPropertyException {
    		log.debug("Initializing JDBCConnectionProvider");
    		
        driver = WikiEngine.getRequiredProperty(config, PROP_PREFIX+".driverClassName");
        url = WikiEngine.getRequiredProperty(config, PROP_PREFIX+".url");

        connectionProperties = parseAdditionalProperties(config, DRIVER_PROP_PREFIX);
        connectionProperties.put("user", WikiEngine.getRequiredProperty(config, PROP_PREFIX+".username"));
        connectionProperties.put("password", WikiEngine.getRequiredProperty(config, PROP_PREFIX+".password"));
        log.debug("driver: "+driver+", url: "+url+", username: "+ connectionProperties.getProperty("user"));
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException ex) {
            throw new InternalWikiException("Database driver could not load. Class not found. Find driver for: "+driver);
        }
    }
    
    public Connection getConnection(WikiEngine engine) throws SQLException {
    	return DriverManager.getConnection( url, connectionProperties );
    }
    

}
