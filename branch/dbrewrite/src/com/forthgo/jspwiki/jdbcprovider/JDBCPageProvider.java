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

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.PageManager;
import com.ecyrd.jspwiki.QueryItem;
import com.ecyrd.jspwiki.SearchMatcher;
import com.ecyrd.jspwiki.SearchResult;
import com.ecyrd.jspwiki.SearchResultComparator;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.providers.WikiPageProvider;
import com.ecyrd.jspwiki.util.ClassUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import org.apache.log4j.Category;

/*
 * History:
 *   2006-08-22 SBG Total rewrite of the tables. Droped WIKI_PAGE_VERSIONS
 *                  Now only using WIKI_PAGE for page versions
 *   2006-03-26 XG  Fix a bug reported by Andreas Kohlbecker where the versions
 *                  table was not getting updated during a continuation edit
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
public class JDBCPageProvider extends JDBCBaseProvider implements WikiPageProvider {

    protected static final Category log = Category.getInstance( JDBCPageProvider.class );

    public void initialize(WikiEngine engine, Properties properties) throws NoRequiredPropertyException, IOException
    {
        debug( "Initializing JDBCPageProvider" );
        super.initialize( engine, properties );
        int count = getPageCount();
        log.debug("Page count at startup: "+count);
        if( count == 0 && getConfig().hasDesireToMigrate())
            migratePages( engine);
    }

    public boolean pageExists( String page )
    {
        PreparedStatement pstmt = null;
        boolean found = false;
        Connection con = null;
        try
        {
            con = getConnection();
            // SELECT TOP 1 PAGE_VERSION FROM WIKI_PAGE WHERE PAGE_NAME = ?
            String sql = getSQL("exists");
            pstmt = con.prepareStatement( sql );
            pstmt.setString( 1, page );
            ResultSet rs = pstmt.executeQuery();
            if( rs.next() )
            {
                found = true;
            }
        }
        catch( SQLException se )
        {
            error( "unable to check existence for " + page, se );
        }
        finally
        {
            releaseConnection( pstmt, con );
        }
        return found;
    }

    /**
     * Read the text directly from the correct file.
     */
    private String getCurrentPageText( String page )
    {
        PreparedStatement pstmt = null;
        String pageText = null;
        Connection con = null;
        try
        {
            con = getConnection();
            String sql = getSQL("getCurrent");
            // SELECT TOP 1 PAGE_TEXT FROM WIKI_PAGE WHERE PAGE_NAME = ? ORDER BY PAGE_VERSION DESC
            pstmt = con.prepareStatement( sql );
            pstmt.setString( 1, page );
            ResultSet rs = pstmt.executeQuery();

            if( rs.next() )
            {
                pageText = rs.getString( 1 );
            }
            else
            {
                // This is okay.
                info( "New page '" + page + "'" );
            }

        }
        catch( SQLException se )
        {
            error( "unable to get current page text for " + page, se );
        }
        finally
        {
            releaseConnection(pstmt, con);
        }

        return pageText;
    }

    public String getPageText( String page, int version )
            throws ProviderException
    {
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        if( version == WikiPageProvider.LATEST_VERSION )
        {
            return getCurrentPageText( page );
        }

        String pageText = null;
        Connection con = null;
        try
        {
            con = getConnection();
            String sql = getSQL("getVersion");
            // SELECT VERSION_TEXT FROM WIKI_PAGE_VERSIONS WHERE VERSION_NAME = ? AND VERSION_NUM = ?
            pstmt = con.prepareStatement( sql );
            pstmt.setString( 1, page );
            pstmt.setInt( 2, version );
            rs = pstmt.executeQuery();

            if( rs.next() )
            {
                pageText = rs.getString( 1 );
            }
            else
            {
                // This is okay.
                info( "New page '" + page + "'" );
            }

        }
        catch( SQLException se )
        {
            error( "unable to get page text for " + page + ":" + version, se );
        }
        finally
        {
            releaseConnection( rs, pstmt, con );
        }
        return pageText;
    }

    private void insertPageText( WikiPage page, String text )
    {
        Timestamp d;
        Connection con = null;
        PreparedStatement pstmt = null;
        try
        {
            // Start by determining the previous version
            List versions = getVersionHistory(page.getName());
            int version = 1;
            if(versions.size() > 0) {
                version = ((WikiPage)versions.get(0)).getVersion()+1;
            }
            
            // Insert the version into database
            con = getConnection();
            String sql = getSQL("insertPage");
            // INSERT INTO WIKI_PAGE (PAGE_NAME, PAGE_VERSION, PAGE_MODIFIED, PAGE_MODIFIED_BY, PAGE_TEXT) VALUES (?, ?, ?, ?, ?)
            pstmt = con.prepareStatement( sql );
            pstmt.setString( 1, page.getName() );
            pstmt.setInt( 2, version );
            
            if(m_migrating && page.getLastModified() != null) {
                d = new Timestamp( page.getLastModified().getTime());
            } else {
                d = new Timestamp( System.currentTimeMillis() );
            }
            pstmt.setTimestamp( 3, d );
            pstmt.setString( 4, page.getAuthor() );
            pstmt.setString( 5, text );
            pstmt.execute();
        }
        catch( SQLException se )
        {
            error( "Saving failed", se );
        }
        catch( ProviderException pe) 
        {
            error( "Failed to get version list",pe);
        }
        finally
        {
            releaseConnection(pstmt, con);
        }
    }

    private void updatePageText( WikiPage page, String text )
    {
        PreparedStatement pstmt = null;
        Connection connection = null;
        try
        {
            WikiPage latest = getCurrentPageInfo(page.getName());
            int version = latest.getVersion();
            Date previousModified = latest.getLastModified();
            String previousAuthor = latest.getAuthor();

            /* If same author and saved again within m_continuationEditTimeout, save by directly overwriting,
               i.e. keep version number and do not create a backup */
            boolean isDifferentAuthor = page.getAuthor() == null || page.getAuthor().equals("") || !page.getAuthor().equals(previousAuthor);
            boolean isContinuationEditTimeExpired = System.currentTimeMillis() > getConfig().getContinuationEditTimeout() + previousModified.getTime();
            boolean createVersion = isDifferentAuthor || isContinuationEditTimeExpired;

            if (createVersion) {
                insertPageText(page, text);
            } else {
                String sql = getSQL("updatePage");
                // UPDATE WIKI_PAGE  SET PAGE_MODIFIED=?, PAGE_MODIFIED_BY=?, PAGE_TEXT=? WHERE PAGE_NAME =?, PAGE_VERSION=?
                pstmt = connection.prepareStatement(sql);
                pstmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                pstmt.setString(2, page.getAuthor());
                pstmt.setString(3, text);
                pstmt.setString(4, page.getName());
                pstmt.setInt(5, version);
                pstmt.execute();
                
            }
        }
        catch( SQLException se )
        {
            error( "Saving failed", se );
        }
        finally
        {
            releaseConnection( pstmt, connection );
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
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        Collection set = new ArrayList();

        Connection con = null;
        try
        {
            con = getConnection();
            String sql = getSQL("getAllPages");
            // SELECT  P1.PAGE_NAME, P1.PAGE_VERSION, P1.PAGE_MODIFIED, P1.PAGE_MODIFIED_BY FROM WIKI_PAGE P1 WHERE P1.PAGE_VERSION = (SELECT MAX(PAGE_VERSION) FROM WIKI_PAGE P2 WHERE P2.PAGE_NAME=P1.PAGE_NAME)
            pstmt = con.prepareStatement( sql );
            rs = pstmt.executeQuery();
            while( rs.next() )
            {
                WikiPage page = new WikiPage( getEngine(), rs.getString( 1 ) );
                page.setVersion( rs.getInt( 2 ) );
                // use Java Date for friendlier comparisons with other dates
                page.setLastModified( new java.util.Date(rs.getTimestamp( 3 ).getTime()));
                page.setAuthor( rs.getString( 4 ) );
                set.add( page );
            }
        }
        catch( SQLException se )
        {
            error( "unable to get all pages", se );
        }
        finally
        {
            releaseConnection( rs, pstmt, con );
        }

        return set;
    }

    public Collection getAllChangedSince( Date date )
    {
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        Collection set = new ArrayList();

        Connection con = null;
        try
        {
            con = getConnection();
            String sql = getSQL("getAllChangedSince");
            // SELECT  P1.PAGE_NAME, P1.PAGE_VERSION, P1.PAGE_MODIFIED, P1.PAGE_MODIFIED_BY FROM WIKI_PAGE P1 WHERE P1.PAGE_MODIFIED > ? AND P1.PAGE_VERSION = (SELECT MAX(PAGE_VERSION) FROM WIKI_PAGE P2 WHERE P2.PAGE_NAME=P1.PAGE_NAME)
            pstmt = con.prepareStatement( sql );
            pstmt.setTimestamp( 1, new Timestamp( date.getTime() ) );
            rs = pstmt.executeQuery();
            while( rs.next() )
            {
                WikiPage page = new WikiPage(getEngine(), rs.getString( 1 ) );
                page.setVersion( rs.getInt( 2 ) );
                // use Java Date for friendlier comparisons with other dates
                page.setLastModified(new java.util.Date(rs.getTimestamp(3).getTime()));
                page.setAuthor( rs.getString( 4 ) );
                set.add( page );
            }
        }
        catch( SQLException se )
        {
            error( "unable to get all pages since " + date, se );
        }
        finally
        {
            releaseConnection( rs, pstmt, con );
        }

        return set;
    }

    public int getPageCount()
    {
        ResultSet rs = null;
        Statement stmt = null;
        int count = 0;
        // Check if the page table exists
        Connection connection = null;
        try
        {
            connection = getConnection();
            String sql = getSQL("getPageCount");
            // SELECT COUNT(DISTINCT PAGE_NAME) from WIKI_PAGE
            stmt = connection.createStatement();
            rs = stmt.executeQuery( sql );
            rs.next();
            count = rs.getInt( 1 );
        }
        catch( SQLException se )
        {
            error( "unable to get page count ", se );
        }
        finally
        {
            releaseConnection( rs, stmt, connection );
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
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        WikiPage p = null;
        Connection con = null;
        try
        {
            con = getConnection();
            String sql = getSQL("getCurrentInfo");
            // SELECT P1.PAGE_VERSION, P1.PAGE_MODIFIED, P1.PAGE_MODIFIED_BY FROM WIKI_PAGE P1 WHERE P1.PAGE_NAME=? AND P1.PAGE_VERSION = (SELECT MAX(PAGE_VERSION) FROM WIKI_PAGE P2 WHERE P2.PAGE_NAME=P1.PAGE_NAME)
            pstmt = con.prepareStatement( sql );
            pstmt.setString( 1, page );
            rs = pstmt.executeQuery();

            if( rs.next() )
            {
                p = new WikiPage( getEngine(), page );
                p.setVersion( rs.getInt( 1 ) );
                // use Java Date for friendlier comparisons with other dates
                p.setLastModified(new java.util.Date(rs.getTimestamp(2).getTime()));
                p.setAuthor( rs.getString( 3 ) );
            }
        }
        catch( SQLException se )
        {
            error( "unable to get current page info for " + page, se );
        }
        finally
        {
            releaseConnection(rs, pstmt, con );
        }
        return p;
    }

    /**
     * Return the correct version of the page.
     */
    public WikiPage getPageInfo( String page, int version )
            throws ProviderException
    {
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        Connection con = null;
        WikiPage p = null;
        if( version == WikiPageProvider.LATEST_VERSION )
        {
            p = getCurrentPageInfo( page );
        }
        else
        {
            try
            {
                con = getConnection();
                String sql = getSQL("getVersionInfo");
                // SELECT PAGE_VERSION, PAGE_MODIFIED, PAGE_MODIFIED_BY FROM WIKI_PAGE WHERE PAGE_NAME = ? AND PAGE_VERSION = ?
                pstmt = con.prepareStatement( sql );
                pstmt.setString( 1, page );
                pstmt.setInt( 2, version );
                rs = pstmt.executeQuery();

                if( rs.next() )
                {
                    p = new WikiPage( getEngine(), page );
                    p.setVersion( rs.getInt( 1 ) );
                    // use Java Date for friendlier comparisons with other dates
                    p.setLastModified(new java.util.Date(rs.getTimestamp(2).getTime()));
                    p.setAuthor( rs.getString( 3 ) );
                }

            }
            catch( SQLException se )
            {
                error( "unable to get page info for " + page + ":" + version, se );
            }
            finally
            {
                releaseConnection( rs, pstmt, con );
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
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        List list = new ArrayList();

        Connection con = null;
        try
        {
            con = getConnection();
            String sql = getSQL("getVersions");
            // SELECT PAGE_VERSION, PAGE_MODIFIED, PAGE_MODIFIED_BY FROM WIKI_PAGE WHERE PAGE_NAME = ? ORDER BY PAGE_VERSION DESC
            pstmt = con.prepareStatement( sql );
            pstmt.setString( 1, page );
            rs = pstmt.executeQuery();

            while( rs.next() )
            {
                WikiPage p = new WikiPage( getEngine(), page );
                p.setVersion( rs.getInt( 1 ) );
                // use Java Date for friendlier comparisons with other dates
                p.setLastModified(new java.util.Date(rs.getTimestamp(2).getTime()));
                p.setAuthor( rs.getString( 3 ) );
                list.add( p );
            }
        }
        catch( SQLException se )
        {
            error( "unable to get version history for " + page, se );
        }
        finally
        {
            releaseConnection( rs, pstmt, con );
        }
        return list;
    }

    public String getProviderInfo()
    {
        return "JDBC page provider";
    }

    public void deleteVersion( String pageName, int version ) throws ProviderException {
        Connection con = null;
        PreparedStatement psVer = null;
        try
        {
            con = getConnection();
            String sql = getSQL("deleteVersion");
            // DELETE FROM WIKI_PAGE WHERE PAGE_NAME = ? AND PAGE_VERSION = ?
            psVer = con.prepareStatement( sql );
            psVer.setString( 1, pageName );
            psVer.setInt( 2, version );
            psVer.execute();
        }
        catch( SQLException se )
        {
            error( "Delete version failed " + pageName + ":" + version, se );
        }
        finally
        {
            releaseConnection( psVer, con );
        }
    }

    public void deletePage( String pageName ) throws ProviderException
    {
        Connection connection = null;
        PreparedStatement psVer = null;
        try
        {
            connection = getConnection();
            String sql = getSQL("deleteVersions");
            // DELETE FROM WIKI_PAGE WHERE VERSION_NAME = ?
            psVer = connection.prepareStatement( sql );
            psVer.setString( 1, pageName );
            psVer.execute();
        }
        catch( SQLException se )
        {
            error( "Delete failed " + pageName, se );
        }
        finally
        {
            releaseConnection( psVer, connection );
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
        PreparedStatement pstmt = null;
        Connection con = null;
        try {
            con = getConnection();
            String sql = getSQL("move");
            // UPDATE WIKI_PAGE SET PAGE_NAME = ? WHERE PAGE_NAME = ?
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, to);
            pstmt.setString(2, from);
            pstmt.execute();

        }
        catch (SQLException se) {
            String message = "Moving '" + from + "' to '" + to + "' failed";
            error(message, se);
            throw new ProviderException(message + ": " + se.getMessage());
        }
        finally {
            releaseConnection(pstmt,con);
        }
    }

    /**
     * Copies pages from one provider to this provider.  The source,
     * "import" provider is specified by the properties file at
     * the given path.
     */
    private void migratePages( WikiEngine engine)
            throws IOException
    {
        Properties importProps = new Properties();
        log.info("Migrating pages from: "+getConfig().getMigrateFrom());
        importProps.load( new FileInputStream( getConfig().getMigrateFrom()) );
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

    public Category getLog() {
        return log;
    }

    public String getSQL(String key) {
        return super.getSQL("page."+key);
    }



}

