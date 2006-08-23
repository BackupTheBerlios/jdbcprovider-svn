--Script written for MS SQL Server 2000 (from Gregor Hagedorn).
-- - NVARCHAR, NTEXT support unicode, VARCHAR, TEXT do not
-- - The INDEX syntax used for MySQL does not work here, separate statement added
-- - A foreign key relation to versions has been added

CREATE DATABASE YourChoiceOfName
GO
Use YourChoiceOfName

CREATE TABLE WIKI_PAGE (
  PAGE_NAME        NVARCHAR(100) NOT NULL ,
  PAGE_VERSION     INTEGER NOT NULL DEFAULT 0,
  PAGE_MODIFIED    DATETIME NULL ,
  PAGE_MODIFIED_BY NVARCHAR(50) NULL ,
  PAGE_TEXT        NTEXT NULL ,
  CONSTRAINT PK_WIKI_PAGE PRIMARY KEY CLUSTERED (PAGE_NAME, PAGE_VERSION)
)
CREATE INDEX IX_PAGE_MODIFIED ON WIKI_PAGE (PAGE_MODIFIED)

-- This has not been verified - just copied from create_tables_sybase.sql
CREATE TABLE WIKI_PAGE_VERSIONS (
    ATT_PAGENAME        NVARCHAR(100)       NOT NULL,
    ATT_FILENAME        NVARCHAR(100)       NOT NULL,
    ATT_VERSION         INTEGER             DEFAULT 0 NOT NULL ,
    ATT_MODIFIED        datetime            NULL,
    ATT_MODIFIED_BY     NVARCHAR(50)        NULL,
    ATT_DATA            IMAGE               NULL,
    ATT_LENGTH          INTEGER             NULL,
    CONSTRAINT PK_ATT_PAGE
        PRIMARY KEY CLUSTERED (ATT_PAGENAME, ATT_FILENAME, ATT_VERSION),
)
CREATE INDEX WIKI_ATT_MODIFIED_IX ON WIKI_ATT (ATT_MODIFIED)
GO
