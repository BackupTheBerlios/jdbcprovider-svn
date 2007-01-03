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
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import org.apache.log4j.Category;

/*
 * History:
 *   2006-04-26 MT  Added WikiEngine member and changed constructor declaration
 *                  to JDBCProviderConfiguration( WikiEngine, String).
 *                  This was necessary to be able to access the getRootPath()
 *                  method of the WikiEngine.
 *                  Changed the loadProperties() method to use java.io.File and
 *                  java.io.FileInputStream to load properties;
 *                  Class.getResource() do not work properly when classes are
 *                  packed inside a jar file. Also, the root path is seen as
 *                  '${CATALINA_HOME}/bin', so prepending '/WEB-INF/' didn't help.
 */

/**
 *
 * @author Mikkel Troest
 * @author glasius
 */
public class JDBCProviderConfiguration {
    
    protected static final Category log = Category.getInstance( JDBCProviderConfiguration.class );
    
    private Properties config ;
    private Properties sql;
    private WikiEngine m_wikiEngine;
    
    private ConnectionProvider connectionProvider;
    
    /** Creates a new instance of JDBCProviderConfiguration */
    public JDBCProviderConfiguration(WikiEngine engine, String configPath) throws IOException, NoRequiredPropertyException {
        m_wikiEngine = engine;
      config = loadProperties(configPath);
        
        
        setupDbProvider(engine, WikiEngine.getRequiredProperty(config, "connectionProvider"));
        
        
        setupSqlQueries(engine, WikiEngine.getRequiredProperty(config, "database.flavour"));
        
    }
    
    
    
    
    public Connection getConnection() throws SQLException {
        return connectionProvider.getConnection(m_wikiEngine);
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
    
    
    private void setupDbProvider(WikiEngine engine, final String cpClass) throws InternalWikiException, NoRequiredPropertyException {
        try {
            Class clazz = Class.forName(cpClass);
            log.debug("dataconnectionProvider: "+clazz.getName());
            
            connectionProvider = (ConnectionProvider) clazz.newInstance();
            connectionProvider.initialize(engine, config);
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
    
    private void setupSqlQueries(WikiEngine engine, final String dbFlavour) throws IOException {
        sql = loadProperties("jdbcprovider."+dbFlavour+".properties");
        log.debug("queries: "+sql.toString());
    }
    
    private Properties loadProperties(String path) throws IOException {
        Properties p = new Properties();
        java.io.File f = new java.io.File(path);
        if(!f.exists()) {
           log.info("Properties not found in '" + f.getAbsoluteFile() + "'. Looking in <JSPWiki_APP_BASE>/WEB-INF/...");
           path = m_wikiEngine.getRootPath() + "WEB-INF" + System.getProperty("file.separator") + path; 
           f = new java.io.File(path);
        }
        if(f.exists()) {
            java.io.FileInputStream fis = new java.io.FileInputStream(f);
            p.load(fis);
            fis.close();
        }
        else {
            throw new IOException("JDBCProvider configuration not found: "+path);
        }
        return p;
    }
    
}
