/*
 * C3P0ConnectionProvider.java
 *
 * Created on february 16. 2007, 15:33
 */
package com.forthgo.jspwiki.jdbcprovider;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.InternalWikiException;
import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiEngine;
import com.mchange.v2.c3p0.DataSources;

/*
 * History:
 * 2007-02-16 MT  Added support for additional driver- and c3p0 properties 
 * 2007-02-16 MT  Initial version
 */

/**
 * @author Mikkel Troest
 */
public class C3P0ConnectionProvider extends ConnectionProvider {
	protected static final Logger log = Logger.getLogger(C3P0ConnectionProvider.class);

	private static final String PREFIX = "c3p0";
	
	private String driver;
	private String url;

	private Properties connectionProperties;
	private Properties c3p0Properties;
	
	private DataSource ds = null;
	
  /** Creates a new instance of C3P0ConnectionProvider */
  public C3P0ConnectionProvider()  {
  }
  
	public void initialize(WikiEngine engine, Properties config)	throws NoRequiredPropertyException {
    log.debug("Initializing C3P0ConnectionProvider");

    driver = WikiEngine.getRequiredProperty(config, PREFIX+".driverClassName");
    url = WikiEngine.getRequiredProperty(config, PREFIX+".url");
    
    connectionProperties = parseAdditionalProperties(config, DRIVER_PROP_PREFIX);
    
    connectionProperties.put("user", WikiEngine.getRequiredProperty(config, PREFIX+".username"));
    connectionProperties.put("password", WikiEngine.getRequiredProperty(config, PREFIX+".password"));
    log.debug("driver: "+driver+", url: "+url+", username: "+ connectionProperties.getProperty("user"));

    c3p0Properties = parseAdditionalProperties(config, PREFIX);
   
    try {
      Class.forName(driver);

      ds = DataSources.pooledDataSource(DataSources.unpooledDataSource(url, connectionProperties), c3p0Properties);
      
  } catch (SQLException ex) {
			log.error("Failed to create ConnectionPool", ex);
			throw new InternalWikiException("SQLException during connection pool creation: " + ex.getMessage());
		} catch (ClassNotFoundException ex) {
			log.error("Failed to create ConnectionPool", ex);
			throw new InternalWikiException("ClassNotFoundException during connection pool creation: " + ex.getMessage());
		}
    
    
	}

	public Connection getConnection(WikiEngine engine) throws SQLException {
		return ds.getConnection();
	}

}
