CREATE TABLE WIKI_PAGE
    (
        PAGE_NAME           VARCHAR (100)   CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
	PAGE_VERSION        INTEGER																						NOT NULL,
	PAGE_MODIFIED       DATETIME,
	PAGE_MODIFIED_BY    VARCHAR (50)    CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
	PAGE_TEXT           MEDIUMTEXT      CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
	PRIMARY KEY  (PAGE_NAME),
	UNIQUE KEY PAGE_NAME (PAGE_NAME),
	KEY PAGE_MODIFIED_IX (PAGE_MODIFIED)
    );

CREATE TABLE WIKI_PAGE_VERSIONS
    (
        VERSION_NAME          VARCHAR (100)   CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
        VERSION_NUM           INTEGER																						NOT NULL,
        VERSION_MODIFIED      DATETIME,
        VERSION_MODIFIED_BY   VARCHAR (50)    CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
        VERSION_TEXT          MEDIUMTEXT      CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
        PRIMARY KEY  (VERSION_NAME,VERSION_NUM),
        UNIQUE KEY VERSION_NAME (VERSION_NAME,VERSION_NUM)
    );

CREATE TABLE WIKI_ATT
    (
        ATT_PAGENAME	VARCHAR	(100)   CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
        ATT_FILENAME	VARCHAR	(100)   CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
        ATT_VERSION	INTEGER 																					NOT NULL,
        ATT_MODIFIED	DATETIME,
        ATT_MODIFIED_BY VARCHAR	(50)    CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
        ATT_DATA	MEDIUMBLOB,
        ATT_LENGTH 	INTEGER,
        PRIMARY KEY  (ATT_PAGENAME,ATT_FILENAME,ATT_VERSION),
        UNIQUE KEY ATT_PAGENAME (ATT_PAGENAME,ATT_FILENAME,ATT_VERSION),
        KEY ATT_MODIFIED_IX (ATT_MODIFIED)
    );