/*
 * JNDIConnectionProvider.java
 *
 * Created on 20. marts 2006, 15:38
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.forthgo.jspwiki.jdbcprovider;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiException;
import java.sql.Connection;
import java.util.Properties;

/**
 *
 * @author glasius
 */
public class JNDIConnectionProvider implements ConnectionProvider {

    private String jndiDatasource;
    
    /** Creates a new instance of JNDIConnectionProvider */
    public JNDIConnectionProvider(Properties config) throws NoRequiredPropertyException {
        jndiDatasource = WikiEngine.getRequiredProperty(config, "jndi.datasource");
    }

    public Connection getConnection() throws WikiException {
        return null;
    }
    
}
