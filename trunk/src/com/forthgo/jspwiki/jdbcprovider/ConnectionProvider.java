/*
 * ConnectionProvider.java
 *
 * Created on 20. marts 2006, 15:15
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.forthgo.jspwiki.jdbcprovider;

import com.ecyrd.jspwiki.WikiException;
import java.sql.Connection;

/**
 *
 * @author glasius
 */
public interface ConnectionProvider {
    public Connection getConnection() throws WikiException;
}
