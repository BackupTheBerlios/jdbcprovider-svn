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
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.attachment.AttachmentManager;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.providers.WikiAttachmentProvider;
import com.ecyrd.jspwiki.util.ClassUtil;
import org.apache.log4j.Category;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;

/*
 * History:
 *   2006-04-24 MTR listAttachments now gets latest attachments in stead of only
 *                  version 1. database/*.attachments.properties:
 *                  jspwiki-s.JDBCAttachmentProvider.getList changed accordingly.
 *   2006-02-21 SBG When migrating the attachment orignal date is preserved.
 *                  Database creation code example now to be found in database/
 *   2005-09-28 XG  Use jspwiki-s as property prefix for security.
 *   2005-09-07 XG  Always use java.util.Date for LastModifield field to friendlier comparisons.
 */

/**
 * Provides a database-based repository for Wiki attachments.
 * MySQL commands to create the tables are provided in the code comments.
 * <p/>
 * Based on Thierry Lach's DatabaseProvider, which supported Wiki pages
 * but not attachments.
 *
 * @authoe Mikkel Troest
 * @author Thierry Lach
 * @author Xan Gregg
 * @author Søren Berg Glasius
 * @see JDBCPageProvider
 */
public class JDBCAttachmentProvider extends JDBCBaseProvider
        implements WikiAttachmentProvider {
    
    
    protected static final Category log = Category.getInstance( JDBCAttachmentProvider.class );
    
    public String getProviderInfo() {
        return "JDBC attachment provider";
    }
    
    public void initialize(WikiEngine engine, Properties properties) throws NoRequiredPropertyException, IOException {
        debug( "Initializing JDBCAttachmentProvider" );
        super.initialize( engine, properties);
        int count = getAttachmentCount();
        log.debug("Attachment count at startup: "+count);
        if( count == 0 && getConfig().hasDesireToMigrate()) {
            migratePages( engine );
        }
        
    }
    
    public int getAttachmentCount() {
        int count = 0;
        Connection connection = null;
        try {
            connection = getConnection();
            String sql = getSQL("getCount");
            // SELECT COUNT(*) FROM WIKI_ATT
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery( sql );
            rs.next();
            count = rs.getInt( 1 );
            rs.close();
            stmt.close();
        } catch( SQLException se ) {
            error( "unable to get attachment count ", se );
        } finally {
            releaseConnection( connection );
        }
        return count;
        
    }
    
    // apparently version number and size should not be relied upon at this point
    public void putAttachmentData( Attachment att, InputStream dataStream )
    throws ProviderException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileUtil.copyContents( dataStream, baos );
        byte data[] = baos.toByteArray();
        int version = findLatestVersion( att ) + 1;
        Connection connection = null;
        try {
            connection = getConnection();
            String sql = getSQL("insert");
            // INSERT INTO WIKI_ATT
            // (ATT_PAGENAME, ATT_FILENAME, ATT_VERSION, ATT_MODIFIED, ATT_MODIFIED_BY, ATT_DATA, ATT_LENGTH)
            // VALUES (?, ?, ?, ?, ?, ?,?)
            
            PreparedStatement psPage = connection.prepareStatement( sql );
            psPage.setString( 1, att.getParentName() );
            psPage.setString( 2, att.getFileName() );
            psPage.setInt( 3, version );
            
            Timestamp d;
            if( m_migrating && att.getLastModified() != null ) {
                d = new Timestamp( att.getLastModified().getTime() );
            } else {
                d = new Timestamp( System.currentTimeMillis() );
            }
            
            psPage.setTimestamp( 4, d );
            psPage.setString( 5, att.getAuthor() );
            psPage.setBytes( 6, data );
            psPage.setInt( 7, data.length);
            psPage.execute();
            psPage.close();
        } catch( SQLException se ) {
            error( "Saving attachment failed " + att, se );
        } finally {
            releaseConnection( connection );
        }
    }
    
    public InputStream getAttachmentData( Attachment att ) throws ProviderException, IOException {
        InputStream result = null;
        Connection connection = null;
        try {
            connection = getConnection();
            String sql = getSQL("getData");
            // SELECT ATT_DATA FROM WIKI_ATT WHERE ATT_PAGENAME = ? AND ATT_FILENAME = ? AND ATT_VERSION = ?
            
            PreparedStatement ps = connection.prepareStatement( sql );
            ps.setString( 1, att.getParentName() );
            ps.setString( 2, att.getFileName() );
            ps.setInt( 3, att.getVersion() );
            ResultSet rs = ps.executeQuery();
            
            if( rs.next() ) {
                byte[] bytes = rs.getBytes( 1 );
                result = new ByteArrayInputStream( bytes );
            } else {
                error( "No attachments to read; '" + att + "'", new SQLException( "empty attachment set" ) );
            }
            rs.close();
            ps.close();
            
        } catch( SQLException se ) {
            error( "Unable to read attachment '" + att + "'", se );
        } finally {
            releaseConnection( connection );
        }
        return result;
    }
    
    // latest versions only
    public Collection listAttachments( WikiPage page ) throws ProviderException {
        Collection result = new ArrayList();
        Connection connection = null;
        try {
            connection = getConnection();
            String sql = getSQL("getList");
            // SELECT ATT_LENGTH, ATT_FILENAME, ATT_MODIFIED, ATT_MODIFIED_BY, ATT_VERSION FROM WIKI_ATT WHERE ATT_PAGENAME = ? GROUP BY ATT_FILENAME ORDER BY ATT_VERSION DESC
            
            PreparedStatement ps = connection.prepareStatement( sql );
            ps.setString( 1, page.getName() );
            ResultSet rs = ps.executeQuery();
            
            String previousFileName = "";
            while( rs.next() ) {
                int size = rs.getInt( 1 );
                String fileName = rs.getString( 2 );
                if( fileName.equals( previousFileName ) )
                    continue;   // only add latest version
                Attachment att = new Attachment( getEngine(), page.getName(), fileName );
                att.setSize( size );
                // use Java Date for friendlier comparisons with other dates
                att.setLastModified(new java.util.Date(rs.getTimestamp(3).getTime()));
                att.setAuthor( rs.getString( 4 ) );
                att.setVersion( rs.getInt( 5 ) );
                result.add( att );
                previousFileName = fileName.toString();
            }
            rs.close();
            ps.close();
            
        } catch( SQLException se ) {
            error( "Unable to list attachments", se );
        } finally {
            releaseConnection( connection );
        }
        return result;
    }
    
    public Collection findAttachments( QueryItem[] query ) {
        return new ArrayList(); // fixme
    }
    
    public List listAllChanged( Date timestamp ) throws ProviderException {
        List changedList = new ArrayList();
        
        Connection connection = null;
        try {
            connection = getConnection();
            String sql = getSQL("getChanged");
            // SELECT ATT_PAGENAME, ATT_FILENAME, LENGTH(ATT_DATA), ATT_MODIFIED, ATT_MODIFIED_BY, ATT_VERSION
            // FROM WIKI_ATT WHERE ATT_MODIFIED > ? ORDER BY ATT_MODIFIED DESC
            
            PreparedStatement ps = connection.prepareStatement( sql );
            ps.setTimestamp( 1, new Timestamp( timestamp.getTime() ) );
            ResultSet rs = ps.executeQuery();
            while( rs.next() ) {
                Attachment att = new Attachment( getEngine(), rs.getString( 1 ), rs.getString( 2 ) );
                att.setSize( rs.getInt( 3 ) );
                // use Java Date for friendlier comparisons with other dates
                att.setLastModified(new java.util.Date(rs.getTimestamp(4).getTime()));
                att.setAuthor( rs.getString( 5 ) );
                att.setVersion( rs.getInt( 6 ) );
                changedList.add( att );
            }
            rs.close();
            ps.close();
        } catch( SQLException se ) {
            error( "Error getting changed list, since " + timestamp, se );
        } finally {
            releaseConnection( connection );
        }
        
        return changedList;
    }
    
    public Attachment getAttachmentInfo( WikiPage page, String name, int version ) throws ProviderException {
        Connection connection = null;
        try {
            connection = getConnection();
            String sql = getSQL("getInfo");   // latest version is first
            // SELECT ATT_LENGTH, ATT_MODIFIED, ATT_MODIFIED_BY FROM WIKI_ATT WHERE ATT_PAGENAME = ? AND ATT_FILENAME = ? AND ATT_VERSION = ?
            
            PreparedStatement ps = connection.prepareStatement( sql );
            ps.setString( 1, page.getName() );
            ps.setString( 2, name );
            ps.setInt( 3, version );
            ResultSet rs = ps.executeQuery();
            
            Attachment att = null;
            if( rs.next() ) {
                att = new Attachment( getEngine(), page.getName(), name );
                att.setSize( rs.getInt( 1 ) );
                // use Java Date for friendlier comparisons with other dates
                att.setLastModified(new java.util.Date(rs.getTimestamp(2).getTime()));
                att.setAuthor( rs.getString( 3 ) );
                att.setVersion( version );
            } else {
                debug( "No attachment info for " + page + "/" + name + ":" + version );
            }
            rs.close();
            ps.close();
            return att;
        } catch( SQLException se ) {
            error( "Unable to get attachment info for " + page + "/" + name + ":" + version, se );
            return null;
        } finally {
            releaseConnection( connection );
        }
    }
    
    /**
     * Goes through the repository and decides which version is
     * the newest one in that directory.
     *
     * @return Latest version number in the repository, or 0, if
     *         there is no page in the repository.
     */
    private int findLatestVersion( Attachment att ) {
        int version = 0;
        Connection connection = null;
        try {
            connection = getConnection();
            String sql = getSQL("getLatestVersion");
            // SELECT ATT_VERSION FROM WIKI_ATT WHERE ATT_PAGENAME = ? AND ATT_FILENAME = ? ORDER BY ATT_VERSION DESC LIMIT 1
            
            PreparedStatement ps = connection.prepareStatement( sql );
            ps.setString( 1, att.getParentName() );
            ps.setString( 2, att.getFileName() );
            ResultSet rs = ps.executeQuery();
            
            if( rs.next() )
                version = rs.getInt( 1 );
            rs.close();
            ps.close();
            
        } catch( SQLException se ) {
            error( "Error trying to find latest attachment: " + att, se );
        } finally {
            releaseConnection( connection );
        }
        return version;
    }
    
    public List getVersionHistory( Attachment att ) {
        List list = new ArrayList();
        Connection connection = null;
        try {
            connection = getConnection();
            String sql = getSQL("getVersions");   // latest version is first
            // SELECT ATT_LENGTH, ATT_MODIFIED, ATT_MODIFIED_BY, ATT_VERSION FROM WIKI_ATT WHERE ATT_PAGENAME = ? AND ATT_FILENAME = ? ORDER BY ATT_VERSION DESC
            
            PreparedStatement ps = connection.prepareStatement( sql );
            ps.setString( 1, att.getParentName() );
            ps.setString( 2, att.getFileName() );
            ResultSet rs = ps.executeQuery();
            
            while( rs.next() ) {
                Attachment vAtt = new Attachment( getEngine(), att.getParentName(), att.getFileName() );
                vAtt.setSize( rs.getInt( 1 ) );
                // use Java Date for friendlier comparisons with other dates
                vAtt.setLastModified(new java.util.Date(rs.getTimestamp(2).getTime()));
                vAtt.setAuthor( rs.getString(3) );
                vAtt.setVersion( rs.getInt(4) );
                list.add( vAtt );
            }
            rs.close();
            ps.close();
            
        } catch( SQLException se ) {
            error( "Unable to list attachment version history for " + att, se );
        } finally {
            releaseConnection( connection );
        }
        return list;
    }
    
    public void deleteVersion( Attachment att ) throws ProviderException {
        Connection connection = null;
        try {
            connection = getConnection();
            String sql = getSQL("deleteVersion");
            // DELETE FROM WIKI_ATT WHERE ATT_PAGENAME = ? AND ATT_FILENAME = ? AND ATT_VERSION = ?
            
            PreparedStatement ps = connection.prepareStatement( sql );
            ps.setString( 1, att.getParentName() );
            ps.setString( 2, att.getFileName() );
            ps.setInt( 3, att.getVersion() );
            ps.execute();
            ps.close();
        } catch( SQLException se ) {
            error( "Delete attachment version failed " + att, se );
        } finally {
            releaseConnection( connection );
        }
    }
    
    public void deleteAttachment( Attachment att ) throws ProviderException {
        Connection connection = null;
        try {
            connection = getConnection();
            String sql = getSQL("delete");
            // DELETE FROM WIKI_ATT WHERE ATT_PAGENAME = ? AND ATT_FILENAME = ?
            
            PreparedStatement ps = connection.prepareStatement( sql );
            ps.setString( 1, att.getParentName() );
            ps.setString( 2, att.getFileName() );
            ps.execute();
            ps.close();
        } catch( SQLException se ) {
            error( "Delete attachment failed " + att, se );
        } finally {
            releaseConnection( connection );
        }
    }
    
    /**
     * Move all the attachments for a given page so that they are attached to a
     * new page.
     *
     * @param oldParent Name of the page we are to move the attachments from.
     * @param newParent Name of the page we are to move the attachments to.
     * @throws com.ecyrd.jspwiki.providers.ProviderException
     *          If the attachments could not be moved for some
     *          reason.
     */
    public void moveAttachmentsForPage(String oldParent, String newParent) throws ProviderException {
        Connection connection = null;
        try {
            connection = getConnection();
            String sql = getSQL("move");
            // UPDATE WIKI_ATT SET ATT_PAGE_NAME = ? WHERE ATT_PAGE_NAME = ?
            
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, newParent);
            ps.setString(2, oldParent);
            ps.execute();
            ps.close();
        } catch (SQLException se) {
            String message = "Moving attachment '" + oldParent + "' to '" + newParent + "' failed";
            error(message, se);
            throw new ProviderException(message + ": " + se.getMessage());
        } finally {
            releaseConnection(connection);
        }
    }
    
    /**
     * Copies pages from one provider to this provider.  The source,
     * "import" provider is specified by the properties file at
     * the given path.
     */
    private void migratePages( WikiEngine engine )
    throws IOException {
        Properties importProps = new Properties();
        log.info("Migrating attachments from: "+getConfig().getMigrateFrom());
        importProps.load( new FileInputStream( getConfig().getMigrateFrom() ) );
        String classname = importProps.getProperty( AttachmentManager.PROP_PROVIDER );
        
        WikiAttachmentProvider importProvider;
        try {
            Class providerclass = ClassUtil.findClass( "com.ecyrd.jspwiki.providers",
                    classname );
            importProvider = ( WikiAttachmentProvider ) providerclass.newInstance();
        } catch( Exception e ) {
            log.error( "Unable to locate/instantiate import provider class " + classname, e );
            return;
        }
        try {
            m_migrating = true;
            importProvider.initialize( engine, importProps );
            
            List attachments = importProvider.listAllChanged( new Date( 0 ) );
            for( Iterator i = attachments.iterator(); i.hasNext(); ) {
                Attachment att = ( Attachment ) i.next();
                InputStream data = importProvider.getAttachmentData( att );
                putAttachmentData( att, data );
                info("Migrated Attachment: "+att);
            }
        } catch( ProviderException e ) {
            throw new IOException( e.getMessage() );
        } catch( NoRequiredPropertyException e ) {
            throw new IOException( e.getMessage() );
        } finally {
            m_migrating = false;
        }
    }
    public Category getLog() {
        return log;
    }

    public String getSQL(String key) {
        return super.getSQL("attachment."+key);
    }
    
}
