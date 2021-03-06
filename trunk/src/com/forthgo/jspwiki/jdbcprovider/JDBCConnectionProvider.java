/*
    JDBCProvider - an RDBMS backed page- and attachment provider for
    JSPWiki.
 
    Copyright (C) 2006-2007 The JDBCProvider development team.
    
    The JDBCProvider developer team members are:
      Xan Gregg
      Soeren Berg Glasius
      Mikkel Troest
      Milt Taylor
 
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
