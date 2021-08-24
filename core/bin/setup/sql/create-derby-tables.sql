-- $Id: create-derby-tables.sql 2037 2013-12-08 16:21:05Z SFB $

SET SCHEMA APP;

CREATE TABLE Archive (
	Point CHAR(16) FOR BIT DATA NOT NULL,
	Stamp BIGINT NOT NULL,
	Version BIGINT NOT NULL,
	State LONG VARCHAR FOR BIT DATA,
	Value LONG VARCHAR FOR BIT DATA,
	PRIMARY KEY (Point, Stamp));
CREATE UNIQUE INDEX Archive_Version_IX1 ON Archive (Version);
CREATE INDEX Archive_Point_IX1 ON Archive (Point, Stamp DESC);

CREATE TABLE Snapshot (
	Point CHAR(16) FOR BIT DATA NOT NULL,
	Stamp BIGINT NOT NULL,
	Version BIGINT NOT NULL,
	State LONG VARCHAR FOR BIT DATA,
	Value LONG VARCHAR FOR BIT DATA,
	PRIMARY KEY (Point));

CREATE TABLE TestsArchive (
	Point CHAR(16) FOR BIT DATA NOT NULL,
	Stamp BIGINT NOT NULL,
	Version BIGINT NOT NULL,
	State LONG VARCHAR FOR BIT DATA,
	Value LONG VARCHAR FOR BIT DATA,
	PRIMARY KEY (Point, Stamp));
CREATE UNIQUE INDEX Tests_Version_IX1 ON Tests (Version);
CREATE INDEX Tests_Point_IX1 ON Tests (Point, Stamp DESC);

CREATE TABLE TestsSnapshot (
	Point CHAR(16) FOR BIT DATA NOT NULL,
	Stamp BIGINT NOT NULL,
	Version BIGINT NOT NULL,
	State LONG VARCHAR FOR BIT DATA,
	Value LONG VARCHAR FOR BIT DATA,
	PRIMARY KEY (Point));

-- End.