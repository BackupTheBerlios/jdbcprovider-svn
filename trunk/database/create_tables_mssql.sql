CREATE TABLE [WIKI_ATT] (
    [PAGENAME] [nvarchar] (100)  NOT NULL ,
    [FILENAME] [nvarchar] (100)  NOT NULL ,
    [VERSION] [int] NOT NULL ,
    [CHANGE_TIME] [datetime] NULL ,
    [CHANGE_BY] [nvarchar] (50)  NULL ,
    [CHANGE_NOTE] [nvarchar] (100)  NULL ,
    [DATA] [image] NULL ,
    [LENGTH] [int] NULL ,
    CONSTRAINT [PK_WIKI_ATT] PRIMARY KEY  CLUSTERED 
    (
        [PAGENAME],
        [FILENAME],
        [VERSION]
    )  ON [PRIMARY] 
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO

 CREATE  INDEX [WIKI_ATT_CHANGE_TIME_IX] ON [WIKI_ATT]([CHANGE_TIME]) ON [PRIMARY]
GO

CREATE TABLE [WIKI_PAGE] (
    [NAME] [nvarchar] (100)  NOT NULL ,
    [VERSION] [int] NOT NULL ,
    [CHANGE_TIME] [datetime] NULL ,
    [CHANGE_BY] [nvarchar] (50)  NULL ,
    [CHANGE_NOTE] [nvarchar] (100)  NULL ,
    [CONTENT] [text]  NULL ,
    CONSTRAINT [PK_WIKI_PAGE] PRIMARY KEY  CLUSTERED 
    (
        [NAME],
        [VERSION]
    )  ON [PRIMARY] 
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO

 CREATE  INDEX [WIKI_PAGE_CHANGE_TIME_IX] ON [WIKI_PAGE]([CHANGE_TIME]) ON [PRIMARY]
GO
