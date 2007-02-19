/*
 * ConnectionProvider.java
 *
 * Created on 20. marts 2006, 15:15
 *
 */


package com.forthgo.jspwiki.jdbcprovider;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiEngine;

/*
 * History:
 *   2007-02-18 MT  Added static DRIVER_PROP_PREFIX.
 *   2007-02-16 MT  Added method parseAdditionalProperties() to support user specified
 *                  connection- and provider properties.
 *   2006-04-26 MT  Added connection = null to releaseConnection() to make sure
 *                  the connection is not accidentially closed again while being
 *                  re-used from a pool.
 */

/**
 * @author Milton Taylor
 * @author Mikkel Troest
 * @author glasius
 */
public abstract class ConnectionProvider {
	
		protected static final String DRIVER_PROP_PREFIX = "driver";
		
    public abstract void initialize(WikiEngine engine, Properties wikiProps) throws NoRequiredPropertyException;
    
    public abstract Connection getConnection(WikiEngine engine) throws SQLException;
    
    public void releaseConnection(Connection connection)  {
        try {
            if(connection != null) {
                connection.close();
                connection = null;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    
    protected Properties parseAdditionalProperties(Properties config, String prefix){

    	int prefixSnip = prefix.length() + 1; /* add 1 for the seperator, '.' */
      Properties props = new Properties();
      for(Enumeration e = config.keys(); e.hasMoreElements(); ){
      	String rawProperty = (String) e.nextElement();
      	if( rawProperty.startsWith(prefix) ){
      		String trimmedProperty = rawProperty.substring(prefixSnip);
      		/*
      		 * Hack. We'd rather get driverClassName/url/user/password through engine.getRequiredProp()
      		 * driverClassName makes no sense as a property for driver- or provider-specific props anyway.
      		 */
      		if(!trimmedProperty.equalsIgnoreCase("driverClassName") & !trimmedProperty.equalsIgnoreCase("url") & !trimmedProperty.equalsIgnoreCase("username") & !trimmedProperty.equalsIgnoreCase("password") ){
      			props.put(trimmedProperty, config.get(rawProperty));
      		}
      	}
      }
    	return props;
    }
    
}
