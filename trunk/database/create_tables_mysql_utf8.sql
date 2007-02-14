CREATE TABLE WIKI_PAGE
    (
        NAME               VARCHAR (100)    CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
        VERSION            INTEGER          NOT NULL,
        CHANGE_TIME        DATETIME,
        CHANGE_BY          VARCHAR (50)     CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
        CHANGE_NOTE        VARCHAR (100)    CHARACTER SET utf8 COLLATE utf8_bin,
        CONTENT            MEDIUMTEXT       CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
        
        PRIMARY KEY        (NAME, VERSION),
        UNIQUE KEY         NAME             (NAME, VERSION),
        KEY                WIKI_PAGE_CHANGE_TIME_IX   (CHANGE_TIME)
    );

CREATE TABLE WIKI_ATT
    (
        PAGENAME           VARCHAR (100)    CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
        FILENAME           VARCHAR (100)    CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
        VERSION            INTEGER          NOT NULL,
        CHANGE_TIME        DATETIME,
        CHANGE_BY          VARCHAR (50)     CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
        CHANGE_NOTE        VARCHAR (100)    CHARACTER SET utf8 COLLATE utf8_bin,
        DATA               MEDIUMBLOB,
        LENGTH             INTEGER,
        
        PRIMARY KEY        (PAGENAME,FILENAME,VERSION),
        UNIQUE KEY         PAGENAME         (PAGENAME,FILENAME,VERSION),
        KEY                WIKI_ATT_CHANGE_TIME_IX   (CHANGE_TIME)
    );
