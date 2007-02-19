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
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiEngine;

/*
 * History:
 *   2007-02-19 MT  Total rewrite. Now uses BasicDataSourceFactory.
 *   2007-02-18 MT  Added support for additional driver properties.
 *                  - moved username/password members to properties.
 * 	 2007-02-13 MT  Got rid of 'factory' property since it's not used anyway
 *                  Changed logging to log4j.Logger in stead of deprecateded log4j.Category
 */

/**
 * @author Mikkel Troest
 * @author glasius
 */
public class DBCPConnectionProvider extends ConnectionProvider {
    protected static final Logger log = Logger.getLogger( DBCPConnectionProvider.class );

    private static final String PREFIX = "dbcp";
    
    private DataSource ds = null;
    
    /** Creates a new instance of DBCPConnectionProvider */
    public DBCPConnectionProvider()  {
    }

    public void initialize(WikiEngine engine, final Properties config) throws NoRequiredPropertyException {
        log.debug("Initializing DBCPConnectionProvider");

        Properties connectionProperties = parseAdditionalProperties(config, DRIVER_PROP_PREFIX);
        
        Properties dbcpProps = parseAdditionalProperties(config, PREFIX);
        dbcpProps.put("driverClassName", WikiEngine.getRequiredProperty(config, PREFIX+".driverClassName"));
        dbcpProps.put("url", WikiEngine.getRequiredProperty(config, PREFIX+".url"));
        dbcpProps.put("username", WikiEngine.getRequiredProperty(config, PREFIX+".username"));
        dbcpProps.put("password", WikiEngine.getRequiredProperty(config, PREFIX+".password"));
        dbcpProps.put("connectionProperties", stringifyProps(connectionProperties, ";"));
        log.debug("driver: "+dbcpProps.getProperty("user")+", url: "+dbcpProps.getProperty("url")+", username: "+ dbcpProps.getProperty("user"));
        
        try{
        	ds = BasicDataSourceFactory.createDataSource(dbcpProps);
        }catch(Exception e){
        	throw new NoRequiredPropertyException(e.getMessage(), null);
        }
        
    }

    
    public Connection getConnection(WikiEngine engine) throws SQLException {
            return ds.getConnection();
    }
    
    private String stringifyProps(Properties props, String separator){
    	String s;
    	StringBuffer sb = new StringBuffer();
    	Enumeration e = props.propertyNames();
      while(e.hasMoreElements()){
        s = (String) e.nextElement();
        sb.append(s + "=" + props.getProperty(s)); 
        if(e.hasMoreElements()) sb.append(separator);
      }
      return sb.toString();
    }
    
}
