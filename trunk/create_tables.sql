CREATE TABLE WIKI_PAGE 
(
    PAGE_NAME           nvarchar(100)      NOT NULL,
    PAGE_VERSION        int                DEFAULT 0 NOT NULL,
    PAGE_MODIFIED       datetime           NULL,
    PAGE_MODIFIED_BY    nvarchar(50)       NULL,
    PAGE_TEXT           text               NULL,
    CONSTRAINT PK_WIKI_PAGE
        PRIMARY KEY CLUSTERED (PAGE_NAME)
) LOCK DATAROWS
CREATE INDEX IX_PAGE_MODIFIED ON WIKI_PAGE (PAGE_MODIFIED)

CREATE TABLE WIKI_PAGE_VERSIONS (
    VERSION_NAME        nvarchar(100)       NOT NULL ,
    VERSION_NUM         int                 DEFAULT 0 NOT NULL ,
    VERSION_MODIFIED    datetime            NULL ,
    VERSION_MODIFIED_BY nvarchar(50)        NULL ,
    VERSION_TEXT        text                NULL ,
    CONSTRAINT PK_WIKI_PAGE_VERSIONS 
        PRIMARY KEY CLUSTERED (VERSION_NAME, VERSION_NUM) ,
    CONSTRAINT FK_WIKI_VERSIONS_WIKI_PAGE 
        FOREIGN KEY(VERSION_NAME) REFERENCES WIKI_PAGE(PAGE_NAME)
)LOCK DATAROWS

CREATE TABLE WIKI_ATT
(
    ATT_PAGENAME        nvarchar(100)       NOT NULL,
    ATT_FILENAME        nvarchar(100)       NOT NULL,
    ATT_VERSION         int                 DEFAULT 0 NOT NULL ,
    ATT_MODIFIED        datetime            NULL,
    ATT_MODIFIED_BY     nvarchar(50)        NULL,
    ATT_DATA            image               NULL,
    ATT_LENGTH          int                 NULL,
    CONSTRAINT PK_ATT_PAGE
        PRIMARY KEY CLUSTERED (ATT_PAGENAME, ATT_FILENAME, ATT_VERSION),
)LOCK DATAROWS
CREATE INDEX WIKI_ATT_MODIFIED_IX ON WIKI_ATT (ATT_MODIFIED)
