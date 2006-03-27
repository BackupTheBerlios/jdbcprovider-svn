/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2004-2005 Xan Gregg (xan.gregg@forthgo.com)

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

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.providers.WikiPageProvider;
import com.ecyrd.jspwiki.util.ClassUtil;
import org.apache.log4j.Category;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.Date;

/*
 * History:
 *   2006-02-21 SBG When migrating the page orignal date is preserved. 
 *                  Database creation code example now to be found in database/
 *   2006-02-12 XG  Move all SQL statements into property file.
 *                  Expand implementation of Delete.
 *   2006-01-29 XG  Update for JSPWiki 2.3.72 (with help from Terry Steichen)
 *                   - added engine as argument to some methods
 *                   - added movePage method now required by WikiPageProvider
 *   2005-09-28 XG  Use jspwiki-s as property prefix for security.
 *   2005-09-07 XG  Always use java.util.Date for LastModifield field to friendlier comparisons.
 *   2005-08-31 XG  Remove legacy comment.
 *   2005-08-30 XG  Added changes suggested by Gregor Hagedorn:
 *                   - removed dependence on auto-incrementing ID fields
 *                   - added "continuation edit" time-out
 *                   - added notes for SQL Server DB creation
 *   2005-08-24 XG  Fixed possible resource leak with conditionally closed statements
 */

/**
 * Provides a database-based repository for Wiki pages.
 * MySQL commands to create the tables are provided in the code comments.
 * <p/>
 * Based on Thierry Lach's DatabaseProvider, which supported Wiki pages
 * but not attachments.
 *
 * @author Thierry Lach
 * @author Xan Gregg
 * @author Søren Berg Glasius
 */
public class JDBCPageProvider extends JDBCBaseProvider
        implements WikiPageProvider
{

    protected static final Category log = Category.getInstance( JDBCPageProvider.class );
    protected static final int ONE_MINUTE = 60 * 1000;    // in milliseconds
    protected int m_continuationEditTimeout = 60 * ONE_MINUTE;    // one hour in milliseconds

    public void initialize(WikiEngine engine, Properties properties) throws NoRequiredPropertyException, IOException
    {
        debug( "Initializing JDBCPageProvider" );
        super.initialize( engine, properties );
        
        
        
        m_continuationEditTimeout = TextUtil.getIntegerProperty(properties, getPropertyBase() + "continuationEditMinutes", 0);
        m_continuationEditTimeout *= ONE_MINUTE;
        int n = getPageCount();
        String migrationPath = properties.getProperty( getPropertyBase() + "migrateFrom" );
        if( n == 0 && migrationPath != null )
            migratePages( engine, migrationPath );
    }

    protected String getPropertyBase()
    {
        return "jspwiki-s.JDBCPageProvider.";   // -s prevents display as a wiki variable, for security
    }

    protected String[] getQueryKeys() {
        return new String[]{"pageExists", "getCurrent", "getVersion",
                "insertCurrent", "insertVersion", "getCurrentInfo", "getVersionInfo",
                "updateCurrent", "getAllPages", "getAllPagesSince", "getPageCount",
                "getVersions", "deleteCurrent", "deleteVersion", "deleteVersions",
                "renameCurrent", "renameVersions", "updateVersion"};
    }

    public Category getLog()
    {
        return log;
    }

    public boolean pageExists( String page )
    {
        boolean found = false;
        Connection connection = null;
        try
        {
            connection = getConnection();
            // SELECT PAGE_VERSION FROM WIKI_PAGE WHERE PAGE_NAME = ?
            String sql = getQuery("pageExists");
            PreparedStatement ps = connection.prepareStatement( sql );
            ps.setString( 1, page );
            ResultSet rs = ps.executeQuery();
            if( rs.next() )
            {
                found = true;
            }
            rs.close();
            ps.close();
        }
        catch( SQLException se )
        {
            error( "unable to check existence for " + page, se );
        }
        finally
        {
            releaseConnection( connection );
        }
        return found;
    }

    /**
     * Read the text directly from the correct file.
     */
    private String getCurrentPageText( String page )
    {
        String pageText = null;
        Connection connection = null;
        try
        {
            connection = getConnection();
            String sql = getQuery("getCurrent");
            // SELECT PAGE_TEXT FROM WIKI_PAGE WHERE PAGE_NAME = ?
            PreparedStatement ps = connection.prepareStatement( sql );
            ps.setString( 1, page );
            ResultSet rs = ps.executeQuery();

            if( rs.next() )
            {
                pageText = rs.getString( 1 );
            }
            else
            {
                // This is okay.
                info( "New page '" + page + "'" );
            }
            rs.close();
            ps.close();

        }
        catch( SQLException se )
        {
            error( "unable to get current page text for " + page, se );
        }
        finally
        {
            releaseConnection( connection );
        }

        return pageText;
    }

    public String getPageText( String page, int version )
            throws ProviderException
    {
        if( version == WikiPageProvider.LATEST_VERSION )
        {
            return getCurrentPageText( page );
        }

        String pageText = null;
        Connection connection = null;
        try
        {
            connection = getConnection();
            String sql = getQuery("getVersion");
            // SELECT VERSION_TEXT FROM WIKI_PAGE_VERSIONS WHERE VERSION_NAME = ? AND VERSION_NUM = ?
            PreparedStatement ps = connection.prepareStatement( sql );
            ps.setString( 1, page );
            ps.setInt( 2, version );
            ResultSet rs = ps.executeQuery();

            if( rs.next() )
            {
                pageText = rs.getString( 1 );
            }
            else
            {
                // This is okay.
                info( "New page '" + page + "'" );
            }
            rs.close();
            ps.close();

        }
        catch( SQLException se )
        {
            error( "unable to get page text for " + page + ":" + version, se );
        }
        finally
        {
            releaseConnection( connection );
        }
        return pageText;
    }

    private void insertPageText( WikiPage page, String text )
    {
        Timestamp d;
        Connection connection = null;
        try
        {
            connection = getConnection();
            String sql = getQuery("insertCurrent");
            // INSERT INTO WIKI_PAGE (PAGE_NAME, PAGE_VERSION, PAGE_MODIFIED, PAGE_MODIFIED_BY, PAGE_TEXT) VALUES (?, ?, ?, ?, ?)
            PreparedStatement psPage = connection.prepareStatement( sql );
            psPage.setString( 1, page.getName() );
            psPage.setInt( 2, 1 );
            
            if(page.getLastModified() != null) {
                d = new Timestamp( page.getLastModified().getTime());
            } else {
                d = new Timestamp( System.currentTimeMillis() );
            }
            psPage.setTimestamp( 3, d );
            psPage.setString( 4, page.getAuthor() );
            psPage.setString( 5, text );
            psPage.execute();
            psPage.close();

            sql = getQuery("insertVersion");
            // INSERT INTO WIKI_PAGE_VERSIONS (VERSION_NAME, VERSION_NUM, VERSION_MODIFIED, VERSION_MODIFIED_BY, VERSION_TEXT) VALUES (?, ?, ?, ?, ?)
            PreparedStatement psVer = connection.prepareStatement( sql );
            psVer.setString( 1, page.getName() );
            psVer.setInt( 2, 1 );
            psVer.setTimestamp( 3, d );
            psVer.setString( 4, page.getAuthor() );
            psVer.setString( 5, text );
            psVer.execute();
            psVer.close();
        }
        catch( SQLException se )
        {
            error( "Saving failed", se );
        }
        finally
        {
            releaseConnection( connection );
        }
    }

    private void updatePageText( WikiPage page, String text )
    {
        Connection connection = null;
        try
        {
            connection = getConnection();
            String sql = getQuery("getCurrentInfo");
            // SELECT PAGE_VERSION, PAGE_MODIFIED, PAGE_MODIFIED_BY FROM WIKI_PAGE WHERE PAGE_NAME = ?
            PreparedStatement psQuery = connection.prepareStatement(sql);
            psQuery.setString(1, page.getName());
            ResultSet rs = psQuery.executeQuery();
            rs.next();
            int version = rs.getInt(1);
            Timestamp previousModified = rs.getTimestamp(2);
            String previousAuthor = rs.getString(3);
            rs.close();
            psQuery.close();

            /* If same author and saved again within m_continuationEditTimeout, save by directly overwriting,
               i.e. keep version number and do not create a backup */
            boolean isDifferentAuthor = page.getAuthor() == null || page.getAuthor().equals("") || !page.getAuthor().equals(previousAuthor);
            boolean isContinuationEditTimeExpired = System.currentTimeMillis() > m_continuationEditTimeout + previousModified.getTime();
            boolean createVersion = isDifferentAuthor || isContinuationEditTimeExpired;

            if (createVersion)
                version += 1;

            sql = getQuery("updateCurrent");
            // UPDATE WIKI_PAGE SET PAGE_MODIFIED = ?, PAGE_MODIFIED_BY = ?, PAGE_VERSION = ?, PAGE_TEXT = ? WHERE PAGE_NAME = ?

            PreparedStatement psPage = connection.prepareStatement( sql );
            Timestamp d;
            if(m_migrating && page.getLastModified() != null) {
                d = new Timestamp( page.getLastModified().getTime());
            } else {
                d = new Timestamp( System.currentTimeMillis() );
            }
            psPage.setTimestamp( 1, d );
            psPage.setString( 2, page.getAuthor() );
            psPage.setInt( 3, version );
            psPage.setString( 4, text );
            psPage.setString( 5, page.getName() );
            psPage.execute();
            psPage.close();

            if (createVersion) {
                sql = getQuery("insertVersion");
                // INSERT INTO WIKI_PAGE_VERSIONS (VERSION_NAME, VERSION_NUM, VERSION_MODIFIED, VERSION_MODIFIED_BY, VERSION_TEXT) VALUES (?, ?, ?, ?, ?)
                PreparedStatement psVer = connection.prepareStatement( sql );
                psVer.setString( 1, page.getName() );
                psVer.setInt( 2, version );
                psVer.setTimestamp( 3, d );
                psVer.setString( 4, page.getAuthor() );
                psVer.setString( 5, text );
                psVer.execute();
                psVer.close();
            } else {
                // UPDATE WIKI_PAGE_VERSIONS  SET VERSION_MODIFIED=?, VERSION_MODIFIED_BY=?, VERSION_TEXT=? WHERE VERSION_NAME =?, VERSION_NUM=?
                PreparedStatement psVer = connection.prepareStatement(sql);
                psVer.setTimestamp(1, d);
                psVer.setString(2, page.getAuthor());
                psVer.setString(3, text);
                psVer.setString(4, page.getName());
                psVer.setInt(5, version);
                psVer.execute();
                psVer.close();
            }
        }
        catch( SQLException se )
        {
            error( "Saving failed", se );
        }
        finally
        {
            releaseConnection( connection );
        }
    }

    public void putPageText( WikiPage page, String text )
    {
        if( pageExists( page.getName() ) )
        {
            updatePageText( page, text );
        }
        else
        {
            insertPageText( page, text );
        }
    }

    public Collection getAllPages()
            throws ProviderException
    {
        Collection set = new ArrayList();

        Connection connection = null;
        try
        {
            connection = getConnection();
            String sql = getQuery("getAllPages");
            // SELECT PAGE_NAME, PAGE_VERSION, PAGE_MODIFIED, PAGE_MODIFIED_BY FROM WIKI_PAGE
            PreparedStatement ps = connection.prepareStatement( sql );
            ResultSet rs = ps.executeQuery();
            while( rs.next() )
            {
                WikiPage page = new WikiPage( getEngine(), rs.getString( 1 ) );
                page.setVersion( rs.getInt( 2 ) );
                // use Java Date for friendlier comparisons with other dates
                page.setLastModified( new java.util.Date(rs.getTimestamp( 3 ).getTime()));
                page.setAuthor( rs.getString( 4 ) );
                set.add( page );
            }
            rs.close();
            ps.close();
        }
        catch( SQLException se )
        {
            error( "unable to get all pages", se );
        }
        finally
        {
            releaseConnection( connection );
        }

        return set;
    }

    public Collection getAllChangedSince( Date date )
    {
        Collection set = new ArrayList();

        Connection connection = null;
        try
        {
            connection = getConnection();
            String sql = getQuery("getAllPagesSince");
            // SELECT PAGE_NAME, PAGE_VERSION, PAGE_MODIFIED, PAGE_MODIFIED_BY FROM WIKI_PAGE WHERE PAGE_MODIFIED > ?
            PreparedStatement ps = connection.prepareStatement( sql );
            ps.setTimestamp( 1, new Timestamp( date.getTime() ) );
            ResultSet rs = ps.executeQuery();
            while( rs.next() )
            {
                WikiPage page = new WikiPage(getEngine(), rs.getString( 1 ) );
                page.setVersion( rs.getInt( 2 ) );
                // use Java Date for friendlier comparisons with other dates
                page.setLastModified(new java.util.Date(rs.getTimestamp(3).getTime()));
                page.setAuthor( rs.getString( 4 ) );
                set.add( page );
            }
            rs.close();
            ps.close();
        }
        catch( SQLException se )
        {
            error( "unable to get all pages since " + date, se );
        }
        finally
        {
            releaseConnection( connection );
        }

        return set;
    }

    public int getPageCount()
    {
        int count = 0;
        // Check if the page table exists
        Connection connection = null;
        try
        {
            connection = getConnection();
            String sql = getQuery("getPageCount");
            // SELECT COUNT(*) FROM WIKI_PAGE
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery( sql );
            rs.next();
            count = rs.getInt( 1 );
            rs.close();
            stmt.close();
        }
        catch( SQLException se )
        {
            error( "unable to get page count ", se );
        }
        finally
        {
            releaseConnection( connection );
        }
        return count;

    }

    public Collection findPages( QueryItem[] query )
    {
        Collection res = new TreeSet( new SearchResultComparator() );
        SearchMatcher matcher = new SearchMatcher(getEngine(), query );

        Collection wikipages = null;
        try
        {
            wikipages = getAllPages();
        }
        catch( ProviderException e )
        {
        }

        nextfile:
            for( Iterator i = wikipages.iterator(); i.hasNext(); )
            {
                // debug("Searching page "+wikipages[i].getPath() );
                WikiPage page = ( WikiPage ) i.next();
                try
                {
                    String pagetext = getCurrentPageText( page.getName() );
                    SearchResult comparison = matcher.matchPageContent( page.getName(), pagetext );
                    if( comparison != null )
                    {
                        res.add( comparison );
                    }

                }
                catch( IOException se )
                {
                    error( "Failed to read", se );
                }
            }

        return res;
    }

    /**
     * Always returns the latest version here.
     */
    private WikiPage getCurrentPageInfo( String page )
    {
        WikiPage p = null;
        Connection connection = null;
        try
        {
            connection = getConnection();
            String sql = getQuery("getCurrentInfo");
            // SELECT PAGE_VERSION, PAGE_MODIFIED, PAGE_MODIFIED_BY FROM WIKI_PAGE WHERE PAGE_NAME = ?
            PreparedStatement ps = connection.prepareStatement( sql );
            ps.setString( 1, page );
            ResultSet rs = ps.executeQuery();

            if( rs.next() )
            {
                p = new WikiPage( getEngine(), page );
                p.setVersion( rs.getInt( 1 ) );
                // use Java Date for friendlier comparisons with other dates
                p.setLastModified(new java.util.Date(rs.getTimestamp(2).getTime()));
                p.setAuthor( rs.getString( 3 ) );
            }
            rs.close();
            ps.close();
        }
        catch( SQLException se )
        {
            error( "unable to get current page info for " + page, se );
        }
        finally
        {
            releaseConnection( connection );
        }
        return p;
    }

    /**
     * Return the correct version of the page.
     */
    public WikiPage getPageInfo( String page, int version )
            throws ProviderException
    {
        WikiPage p = null;
        if( version == WikiPageProvider.LATEST_VERSION )
        {
            p = getCurrentPageInfo( page );
        }
        else
        {
            Connection connection = null;
            try
            {
                connection = getConnection();
                String sql = getQuery("getVersionInfo");
                // SELECT VERSION_NUM, VERSION_MODIFIED, VERSION_MODIFIED_BY FROM WIKI_PAGE_VERSIONS WHERE VERSION_NAME = ? AND VERSION_NUM = ?
                PreparedStatement ps = connection.prepareStatement( sql );
                ps.setString( 1, page );
                ps.setInt( 2, version );
                ResultSet rs = ps.executeQuery();

                if( rs.next() )
                {
                    p = new WikiPage( getEngine(), page );
                    p.setVersion( rs.getInt( 1 ) );
                    // use Java Date for friendlier comparisons with other dates
                    p.setLastModified(new java.util.Date(rs.getTimestamp(2).getTime()));
                    p.setAuthor( rs.getString( 3 ) );
                }
                rs.close();
                ps.close();

            }
            catch( SQLException se )
            {
                error( "unable to get page info for " + page + ":" + version, se );
            }
            finally
            {
                releaseConnection( connection );
            }
        }
        return p;
    }

    /**
     * Provide the list of versions.
     */
    public List getVersionHistory( String page )
            throws ProviderException
    {
        List list = new ArrayList();

        Connection connection = null;
        try
        {
            connection = getConnection();
            String sql = getQuery("getVersions");
            // SELECT VERSION_NUM, VERSION_MODIFIED, VERSION_MODIFIED_BY FROM WIKI_PAGE_VERSIONS WHERE VERSION_NAME = ? ORDER BY VERSION_NUM DESC
            PreparedStatement ps = connection.prepareStatement( sql );
            ps.setString( 1, page );
            ResultSet rs = ps.executeQuery();

            while( rs.next() )
            {
                WikiPage p = new WikiPage( getEngine(), page );
                p.setVersion( rs.getInt( 1 ) );
                // use Java Date for friendlier comparisons with other dates
                p.setLastModified(new java.util.Date(rs.getTimestamp(2).getTime()));
                p.setAuthor( rs.getString( 3 ) );
                list.add( p );
            }
            rs.close();
            ps.close();
        }
        catch( SQLException se )
        {
            error( "unable to get version history for " + page, se );
        }
        finally
        {
            releaseConnection( connection );
        }
        return list;
    }

    public String getProviderInfo()
    {
        return "JDBC page provider";
    }

    public void deleteVersion( String pageName, int version ) throws ProviderException {
        WikiPage page = getCurrentPageInfo(pageName);
        if (version == WikiPageProvider.LATEST_VERSION) {
            version = page.getVersion();
        }

        if (version == page.getVersion()) {
            // need to update current-version table
            List versions = getVersionHistory(pageName);    // list of WikiPage
            if (versions.size() <= 1) {
                // no previous version, so just delete page
                deletePage(pageName);
                return;
            }
            int previousVersion = ((WikiPage) versions.get(1)).getVersion();
            WikiPage previousPage = getPageInfo(pageName, previousVersion);
            String previousText = getPageText(pageName, previousVersion);
            Connection connection = null;
            try {
                connection = getConnection();
                String sql = getQuery("updateCurrent");
                // UPDATE WIKI_PAGE SET PAGE_MODIFIED = ? PAGE_MODIFIED_BY = ? PAGE_VERSION = ? PAGE_TEXT = ? WHERE PAGE_NAME = ?

                PreparedStatement psPage = connection.prepareStatement(sql);
                psPage.setTimestamp(1, new Timestamp(previousPage.getLastModified().getTime()));
                psPage.setString(2, previousPage.getAuthor());
                psPage.setInt(3, previousVersion);
                psPage.setString(4, previousText);
                psPage.setString(5, pageName);
                psPage.execute();
                psPage.close();
            }
            catch(SQLException se )
            {
                error("Delete version failed " + pageName + ":" + version, se);
            }
            finally
            {
                releaseConnection(connection);
            }
        }

        Connection connection = null;
        try
        {
            connection = getConnection();
            String sql = getQuery("deleteVersion");
            // DELETE FROM WIKI_PAGE_VERSIONS WHERE VERSION_NAME = ? AND VERSION_NUM = ?
            PreparedStatement psVer = connection.prepareStatement( sql );
            psVer.setString( 1, pageName );
            psVer.setInt( 2, version );
            psVer.execute();
            psVer.close();

        }
        catch( SQLException se )
        {
            error( "Delete version failed " + pageName + ":" + version, se );
        }
        finally
        {
            releaseConnection( connection );
        }
    }

    public void deletePage( String pageName ) throws ProviderException
    {
        Connection connection = null;
        try
        {
            connection = getConnection();
            String sql = getQuery("deleteVersions");
            // DELETE FROM WIKI_PAGE_VERSIONS WHERE VERSION_NAME = ?
            PreparedStatement psVer = connection.prepareStatement( sql );
            psVer.setString( 1, pageName );
            psVer.execute();
            psVer.close();

            sql = getQuery("deleteCurrent");
            // DELETE FROM WIKI_PAGE WHERE PAGE_NAME = ?
            PreparedStatement psPage = connection.prepareStatement( sql );
            psPage.setString( 1, pageName );
            psPage.execute();
            psPage.close();

        }
        catch( SQLException se )
        {
            error( "Delete failed " + pageName, se );
        }
        finally
        {
            releaseConnection( connection );
        }
    }

    /**
     * Move a page
     *
     * @param from Name of the page to move.
     * @param to   New name of the page.
     * @throws com.ecyrd.jspwiki.providers.ProviderException
     *          If the page could not be moved for some reason.
     */
    public void movePage(String from, String to) throws ProviderException
    {
        Connection connection = null;
        try {
            connection = getConnection();
            String sql = getQuery("renameCurrent");
            // UPDATE WIKI_PAGE SET PAGE_NAME = ? WHERE PAGE_NAME = ?
            PreparedStatement psPage = connection.prepareStatement(sql);
            psPage.setString(1, to);
            psPage.setString(2, from);
            psPage.execute();
            psPage.close();

            sql = getQuery("renameVersions");;
            // UPDATE WIKI_PAGE_VERSIONS SET VERSION_NAME = ? WHERE VERSION_NAME = ?
            PreparedStatement psVerPage = connection.prepareStatement(sql);
            psVerPage.setString(1, to);
            psVerPage.setString(2, from);
            psVerPage.execute();
            psVerPage.close();
        }
        catch (SQLException se) {
            String message = "Moving '" + from + "' to '" + to + "' failed";
            error(message, se);
            throw new ProviderException(message + ": " + se.getMessage());
        }
        finally {
            releaseConnection(connection);
        }
    }

    /**
     * Copies pages from one provider to this provider.  The source,
     * "import" provider is specified by the properties file at
     * the given path.
     */
    private void migratePages( WikiEngine engine, String path )
            throws IOException
    {
        Properties importProps = new Properties();
        importProps.load( new FileInputStream( path ) );
        String classname = importProps.getProperty( PageManager.PROP_PAGEPROVIDER );

        WikiPageProvider importProvider;
        try
        {
            Class providerclass = ClassUtil.findClass( "com.ecyrd.jspwiki.providers",
                    classname );
            importProvider = ( WikiPageProvider ) providerclass.newInstance();
        }
        catch( Exception e )
        {
            log.error( "Unable to locate/instantiate import provider class " + classname, e );
            return;
        }
        try
        {
            m_migrating = true; 
            importProvider.initialize( engine, importProps );

            Collection pages = importProvider.getAllPages();
            for( Iterator i = pages.iterator(); i.hasNext(); )
            {
                WikiPage latest = ( WikiPage ) i.next();

                int version = 1;
                boolean done = false;
                while( !done )
                {
                    WikiPage page = importProvider.getPageInfo( latest.getName(), version );
                    if( page == null )
                        done = true;
                    else
                    {
                        String text = importProvider.getPageText( page.getName(), version );
                        putPageText( page, text );
                        if (page.getVersion() >= latest.getVersion())
                            done = true;
                        version += 1;
                    }
                    info("Migrated page: "+page);
                }
            }
        }
        catch( ProviderException e )
        {
            throw new IOException( e.getMessage() );
        }
        catch( NoRequiredPropertyException e )
        {
            throw new IOException( e.getMessage() );
        } finally {
            m_migrating = false;
        }
    }


}

