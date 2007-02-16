    JDBCProvider - a JDBC-based page provider for JSPWiki.

    Copyright (C) 2004-2006 JDBCProvider team at berlios.de
    http://jdbcprovider.berlios.de/
    http://developer.berlios.de/projects/jdbcprovider/
    http://www.jspwiki.org/wiki/JDBCProviders

SUMMARY

JSPWiki has a pluggable content provider system. This package supplies
providers for page and attachment content backed by a SQL database.

STATUS

Dev Build
Released 2007-01-03
Tested with JSPWiki 2.4.65 and 2.5.16

MOST RECENT CHANGES

* Minor bug fixes
* Works with latest jspwiki versions
* Works with multiple wiki instances, i.e. multiple databases
* Added support for PostgreSQL
* Added project files for eclipse


RECENT CHANGES

All SQL code has been pulled out into separate properties files for easier
adaption to other databases. Currently there are four flavours supported: mysql, 
sqlanywhere, postgresql, and sybase.
Adapting it to other databases should be close to trivial :-)

INSTALL

* If you're upgrading from a previous version, please read the section UPGRADE 
  after reading this section

Basically,
 - Copy the dist/JDBCProvider.jar file into JSPWiki's WEB-INF/lib directory.
 - If you are planing to use DBCP copy the three jar files in lib
   to WEB-INF/lib 
 - Copy the appropiate databasedriver to WEB-INF/lib
 - Copy the jdbcprovider.properties into WEB-INF directory
 - Edit the jdbcprovider.properties so that it reflects your favorite database-
   connection option.
 - Copy the jdbcprovider.sybase.properties or jdbcprovider.mysql.properties into
   WEB-INF directory
 - Merge the jspwiki.aditional.properties file into the jspwiki.properties file.
   Remember to remove the current page and attachment providers.
 - Run the appropriate create_tables code to create the database tables (unless
   you are upgrading from a older version of JDBCProvider)
 
Note: the page provider and the attachment provider operate independently
      and you may use only one or both.

Example of changes to jspwiki.properties:

jspwiki.pageProvider = com.forthgo.jspwiki.jdbcprovider.JDBCPageProvider
jspwiki.attachmentProvider = com.forthgo.jspwiki.jdbcprovider.JDBCAttachmentProvider
jspwiki.jdbcprovider.configuration=jdbcprovider.properties

MIGRATING

If you have an old provider in your JSPWiki you can migrate your repository to
use JDBCProvider. 

Note: Both the WIKI_PAGE and WIKI_ATT table must be empty (truncated).

* Make a backup of your current JSPWiki page and attachment repository to a 
  temp directory

* Copy jspwiki.properties in your temp direcory

* Edit the just copied version of jspwiki.properties so that you have

jspwiki.pageProvider =VersioningFileProvider (your old page provider)
jspwiki.fileSystemProvider.pageDir =/data/wiki/pages (the folder with your wiki pages)
jspwiki.attachmentProvider = BasicAttachmentProvider (your old attachment provider)
jspwiki.basicAttachmentProvider.storageDir = /data/wiki/attachments (the folder with your attachments)

* Edit the jdbcprovider.properties:

##
## Migrate from another page repository. If you define this, both pages
## and attachments will be migrated, if you have set your attachment provider
## to JDBCAttachmentProvider. Please provide a full path the the other
## provider.
migrateFromConfiguration = /data/wiki/oldwiki.properties

* When done, comment out the above line.


UPGRADE

Preferably make a copy of your tables / database before proceding.

The new version of the page provider does not use the WIKI_PAGE_VERSIONS any
longer, but all page versions are stored here. Thus, we need to move the pages 
from WIKI_PAGE_VERSIONS to WIKI_PAGE, but first the tables need to be modified. 
It is important to understand that the latest version of a page in the old
version is stored in both WIKI_PAGE and WIKI_PAGE_VERSIONS

On Sybase do this:

* Remove the foreign key restraint on WIKI_PAGE_VERSIONS :
  ALTER TABLE dbo.WIKI_PAGE_VERSIONS DROP CONSTRAINT FK_WIKI_VERSIONS_WIKI_PAGE
  go

* Remove the primary key on WIKI_PAGE and add a new one:

  ALTER TABLE WIKI_PAGE DROP CONSTRAINT PK_WIKI_PAGE
  go
  ALTER TABLE dbo.WIKI_PAGE ADD CONSTRAINT PK_WIKI_PAGE
       PRIMARY KEY NONCLUSTERED (PAGE_NAME,PAGE_VERSION)
  go

* Remove all pages from WIKI_PAGE:

  TRUNCATE TABLE dbo.WIKI_PAGE
  go

* Move all pages from WIKI_PAGE_VERSIONS to WIKI_PAGE:

  INSERT WIKI_PAGE SELECT * FROM WIKI_PAGE_VERSIONS

* Finally remove WIKI_PAGE_VERSIONS

  DROP TABLE dbo.WIKI_PAGE_VERSIONS
  go

On Mysql do this:

INSERT INTO WIKI_PAGE (NAME, VERSION, CHANGE_TIME, CHANGE_BY, CONTENT)
       SELECT VERSION_NAME, VERSION_NUM, VERSION_MODIFIED, VERSION_MODIFIED_BY, VERSION_TEXT
              FROM your_old_db.WIKI_PAGE_VERSIONS;

INSERT INTO WIKI_ATT (PAGENAME, FILENAME, VERSION, CHANGE_TIME, CHANGE_BY, DATA, LENGTH)
       SELECT ATT_PAGENAME, ATT_FILENAME, ATT_VERSION, ATT_MODIFIED, ATT_MODIFIED_BY, ATT_DATA, LENGTH(ATT_DATA)
              FROM your_old_db.WIKI_ATT;

CONTENTS

  readme.txt -- this file
  dist/JDBCProvider.jar -- put this in webapps/.../WEB-INF/lib
  lib/
    commons-dbcp-1.2.1.jar -- these two are needed if you chose to use a
    commons-pool-1.2.jar   -- DBCP pool for your database connections

  database/

    create_tables_mysql.sql -- MySQL Server code to create the necessary DB tables
    create_tables_mysql_utf8.sql -- MySQL Server code to create the necessary 
                                    DB tables with the utf8 charset
    create_tables_sybase.sql -- Sybase code to create the necessary DB tables
    create_tables_sqlany.sql -- SQLAnywhere code to create the necessary DB tables
    create_tables_pgsql.sql -- PostGreSQL code to create the necessary DB tables
    jspwiki.additional.properties -- Properties that must be merged into jspwiki.properties
    jdbcprovider.properties -- JDBCProvider configuration file, where DB is configured
    jdbcprovider.mysql.properties -- SQL statements for MySQL DB
    jdbcprovider.sybase.properties -- SQL statements for Sybase DB
    jdbcprovider.sqlany.properties -- SQL statements for SQLAnywhere DB
    jdbcprovider.pgsql.properties -- SQL statements for PostGreSQL DB
    
TEAM MEMBERS

Xan Gregg
Soeren Berg Glasius
Mikkel Troest

