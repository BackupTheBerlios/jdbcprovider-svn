/*
 * JDBCProviderConfiguration.java
 *
 * Created on 20. marts 2006, 15:04
 *
 */

package com.forthgo.jspwiki.jdbcprovider;

import com.ecyrd.jspwiki.InternalWikiException;
import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.util.ClassUtil;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import org.apache.commons.jocl.ConstructorUtil;
import org.apache.log4j.Category;

/**
 *
 * @author glasius
 */
public class JDBCProviderConfiguration {
    
    protected static final Category log = Category.getInstance( JDBCProviderConfiguration.class );
    
    private Properties config ;
    private Properties sql;
    
    private ConnectionProvider connectionProvider;
    
    /** Creates a new instance of JDBCProviderConfiguration */
    public JDBCProviderConfiguration(String configPath) throws IOException, NoRequiredPropertyException {
        
        String pageProperties[] = new String[] {"pageExists", "getCurrent", "getVersion",
        "insertCurrent", "insertVersion", "getCurrentInfo", "getVersionInfo",
        "updateCurrent", "getAllPages", "getAllPagesSince", "getPageCount",
        "getVersions", "deleteCurrent", "deleteVersion", "deleteVersions",
        "renameCurrent", "renameVersions", "updateVersion"};
        String attachmentProperties[] = new String[] {"getCount", "insert", "getData",
        "getList", "getChanged", "getInfo", "getLatestVersion",
        "getVersions", "deleteVersion", "delete", "move"};
        
        config = loadProperties(configPath);
        
        
        setupDbProvider(WikiEngine.getRequiredProperty(config, "connectionProvider"));
        
        
        setupSqlQueries(WikiEngine.getRequiredProperty(config, "database.flavour"));
        
    }
    
    
    
    
    public Connection getConnection() throws SQLException {
        return connectionProvider.getConnection();
    }
    
    public void releaseConnection(Connection connection ) {
        connectionProvider.releaseConnection(connection);
    }
    
    public String getSql(String key) {
        return sql.getProperty(key);
    }
    
    public int getContinuationEditTimeout() {
        return TextUtil.getIntegerProperty(config,"continuationEditMinutes",0) * 60 * 1000;
    }
    
    public String getMigrateFrom() {
        return config.getProperty("migrateFromConfiguration");
    }
    
    public boolean hasDesireToMigrate() {
        log.debug("Has desire to migrate: "+config.contains("migrateFromConfiguration"));
        return config.getProperty("migrateFromConfiguration") != null;
    }
    
    
    private void setupDbProvider(final String cpClass) throws InternalWikiException, NoRequiredPropertyException {
        try {
            Class clazz = getClass().forName(cpClass);
            log.debug("dataconnectionProvider: "+clazz.getName());
            
            connectionProvider = (ConnectionProvider) clazz.newInstance();
            connectionProvider.initialize(config);
        } catch (InstantiationException ex) {
            log.error("Error instantiating connectionProvider: ",ex);
            throw new InternalWikiException("Error instantiating connectionProvider: "+ex.getMessage());
        } catch (ClassNotFoundException ex) {
            log.error("connectionProvider class not found: ",ex);
            throw new InternalWikiException("connectionProvider class not found: "+ex.getMessage());
        } catch (IllegalAccessException ex) {
            log.error("IllegalAccessException on connectionProvider: ",ex);
            throw new InternalWikiException("IllegalAccessException on connectionProvider: "+ex.getMessage());
        }
    }
    
    private void setupSqlQueries(final String dbFlavour) throws IOException {
        sql = loadProperties("jdbcprovider."+dbFlavour+".properties");
        log.debug("queries: "+sql.toString());
    }
    
    private Properties loadProperties(String path) throws IOException {
        Properties p = new Properties();
        if(getClass().getResource(path) == null) {
            path = "/WEB-INF/"+path;
        }
        if(getClass().getResource(path) != null) {
            p.load(getClass().getResourceAsStream(path));
        } else {
            throw new IOException("JDBCProvider configuration not found: "+path);
        }
        return p;
    }
    
}
