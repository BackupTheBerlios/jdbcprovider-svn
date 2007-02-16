-------------------------------------------------
--   Create tables
-------------------------------------------------


CREATE TABLE "DBA"."WIKI_PAGE"
(
    "NAME"              varchar(100) NOT NULL,
    "VERSION"           integer NOT NULL,
    "CHANGE_TIME"       datetime NULL,
    "CHANGE_BY"         varchar(50) NULL,
    "CHANGE_NOTE"       varchar(100) NULL,
    "CONTENT"           text NULL,
    PRIMARY KEY         ("NAME", "VERSION")
)
go
CREATE TABLE "DBA"."WIKI_ATT"
(
    "PAGENAME"          varchar(100) NOT NULL,
    "FILENAME"          varchar(100) NOT NULL,
    "VERSION"           integer NOT NULL,
    "CHANGE_TIME"       datetime NULL,
    "CHANGE_BY"         varchar(50) NULL,
    "CHANGE_NOTE"       varchar(100) NULL,
    "DATA"              image NULL,
    "LENGTH"            integer NULL,
    PRIMARY KEY         ("PAGENAME", "FILENAME", "VERSION")
)
go
commit work
go

-------------------------------------------------
--   Add indexes
-------------------------------------------------

CREATE INDEX "WIKI_PAGE_CHANGE_TIME_IX" ON "DBA"."WIKI_PAGE"
(
    "CHANGE_TIME" ASC
)
go
commit work
go

CREATE INDEX "WIKI_ATT_CHANGE_TIME_IX" ON "DBA"."WIKI_ATT"
(
    "CHANGE_TIME" ASC
)
go
commit work
go