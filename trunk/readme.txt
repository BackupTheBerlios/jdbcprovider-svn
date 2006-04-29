    JDBCProvider - a JDBC-based page provider for JSPWiki.

    Copyright (C) 2004-2006 JDBCProvider team at berlios.de
    http://developer.berlios.de/projects/jdbcprovider/
    http://www.jspwiki.org/wiki/JDBCProviders

SUMMARY

JSPWiki has a pluggable content provider system. This package supplies
providers for page and attachment content backed by a SQL database.

STATUS

Released 2006-04-29
Tested with JSPWiki 2.3.100

RECENT CHANGES

All SQL code has been pulled out into separate properties files for easier
adaption to other databases. Attachment provider bugs related to
moving and listing have been fixed.

INSTALL

Detailed notes are present at http://www.jspwiki.org/wiki/JDBCProviders.
Basically,
 - Copy the enclosed JAR file plus the appropriate JDBC driver
into JSPWiki's lib directory.
 - Add the db.properties files into the jspwiki.properties file.
 - Add DB connect info to jspwiki.properties.
 - Change the provider class names in jspwiki.properties.
 - Run the appropriate create_tables code to create the database tables.
 Note: the page provider and the attachment provider operate independently
 and you may use only one or both.

Example of changes to jspwiki.properties:

jspwiki.pageProvider = com.forthgo.jspwiki.jdbcprovider.JDBCPageProvider
...
#
#  Determines the database information when using JDBCPageProvider.
#
jspwiki-s.JDBCPageProvider.url = jdbc:mysql://localhost:3306/jspwikidb
jspwiki-s.JDBCPageProvider.username = jspwikiuser
jspwiki-s.JDBCPageProvider.password = jspwikipass
jspwiki-s.JDBCPageProvider.driver = org.gjt.mm.mysql.Driver
jspwiki-s.JDBCPageProvider.cachedConnections = 5
jspwiki-s.JDBCPageProvider.continuationEditMinutes = 30
# = Time during which saves by a single user are consolidated in one version.
#   Multiple versions are created if another user saves during that time as well.
...
jspwiki.attachmentProvider = com.forthgo.jspwiki.jdbcprovider.JDBCAttachmentProvider
...
#
#  Determines the database information when using JDBCAttachmentProvider.
#  Can be the same url as for JDBCPageProvider.
#
jspwiki-s.JDBCAttachmentProvider.url = jdbc:mysql://localhost:3306/jspwikidb
jspwiki-s.JDBCAttachmentProvider.username = jspwikiuser
jspwiki-s.JDBCAttachmentProvider.password = jspwikipass
jspwiki-s.JDBCAttachmentProvider.driver = org.gjt.mm.mysql.Driver
jspwiki-s.JDBCAttachmentProvider.cachedConnections = 5
...
...contents of mysql.properties and mysql.attachment.properties...

FUTURE PLANS

We are working on reducing the changes need to jspwiki.properties by
getting the connection info from the servlet container via JNDI and
leaving the SQL statements in separate properties files.

CONTENTS

  readme.txt -- this file
  jdbcprovider.jar -- put this in webapps/.../WEB-INF/lib
  database/
    create_tables_mssql.sql - MS SQL Server code to create the necessary DB tables
    create_tables_mysql.sql - MySQL Server code to create the necessary DB tables
    create_tables_sybase.sql - Sybase code to create the necessary DB tables
    mysql.attachment.properties - SQL statements for MySQL/MSSQL attachment provider
    mysql.properties - SQL statements for MySQL/MSSQL page provider
    sybase.attachment.properties - SQL statements for Sybase attachment provider
    sybase.properties - SQL statements for Sybase page provider

TEAM MEMBERS

Xan Gregg
S¿ren Berg Glasius
Mikkel Troest

