CREATE TABLE "WIKI_PAGE"
    (
         "NAME" character varying(100) NOT NULL,
         "VERSION" integer NOT NULL,
         "CHANGE_TIME" timestamp without time zone,
         "CHANGE_BY" character varying(50),
         "CHANGE_NOTE" character varying(100),
         "CONTENT" text
    );

ALTER TABLE ONLY "WIKI_PAGE"
    ADD CONSTRAINT "WIKI_PAGE_UNIQUE_KEY" UNIQUE ("NAME", "VERSION");

ALTER TABLE ONLY "WIKI_PAGE"
    ADD CONSTRAINT "WIKI_PAGE_PKEY" PRIMARY KEY ("NAME", "VERSION");
    
CREATE INDEX "WIKI_PAGE_CHANGE_TIME_IX" ON "WIKI_PAGE" USING btree ("CHANGE_TIME");


CREATE TABLE "WIKI_ATT"
    (
         "PAGENAME" character varying(100) NOT NULL,
         "FILENAME" character varying(100) NOT NULL,
         "VERSION" integer NOT NULL,
         "CHANGE_TIME" timestamp without time zone,
         "CHANGE_BY" character varying(50),
         "CHANGE_NOTE" character varying(100),
         "LENGTH" integer,
         "DATA" bytea
    );

ALTER TABLE ONLY "WIKI_ATT"
    ADD CONSTRAINT "WIKI_ATT_UNIQUE_KEY" UNIQUE ("PAGENAME", "FILENAME", "VERSION");

ALTER TABLE ONLY "WIKI_ATT"
    ADD CONSTRAINT "WIKI_ATT_PKEY" PRIMARY KEY ("PAGENAME", "FILENAME", "VERSION");
    
CREATE INDEX "WIKI_ATT_CHANGE_TIME_IX" ON "WIKI_ATT" USING btree ("CHANGE_TIME");
