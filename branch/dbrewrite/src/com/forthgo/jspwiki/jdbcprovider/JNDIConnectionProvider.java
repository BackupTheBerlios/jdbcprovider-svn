/*
 * JNDIConnectionProvider.java
 *
 * Created on 20. marts 2006, 15:38
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.forthgo.jspwiki.jdbcprovider;

import com.ecyrd.jspwiki.InternalWikiException;
import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 *
 * @author glasius
 */
public class JNDIConnectionProvider extends ConnectionProvider {
    
    private String jndiDatasource;
    
    private DataSource ds;
    
    /** Creates a new instance of JNDIConnectionProvider */
    public JNDIConnectionProvider() {
    }

    public void initialize(final Properties config) throws NoRequiredPropertyException {
        jndiDatasource = WikiEngine.getRequiredProperty(config, "jndi.datasource");
        try {
            Context ctx = new InitialContext();
            if(ctx == null ) {
                throw new InternalWikiException("Ouch - no initial context");
            }
            
            ds =(DataSource)ctx.lookup("java:comp/env/jdbc/TestDB");
        } catch (NamingException ex) {
            throw new InternalWikiException("NamingException caught: "+ex.getMessage());
        }
    }
    
    public Connection getConnection() throws SQLException {
        return ds.getConnection();
    }
    
    
}
