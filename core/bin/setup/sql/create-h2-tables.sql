// $Id: create-h2-tables.sql 2037 2013-12-08 16:21:05Z SFB $

CREATE TABLE Archive (
	Point BINARY(16) NOT NULL,
	Stamp BIGINT NOT NULL,
	Version BIGINT NOT NULL,
	State VARBINARY,
	Value LONGVARBINARY,
	PRIMARY KEY (Point, Stamp));
CREATE UNIQUE INDEX Archive_Version_IX1 ON Archive (Version);
CREATE INDEX Archive_Point_IX1 ON Archive (Point, Stamp DESC);

CREATE TABLE Snapshot (
	Point BINARY(16) NOT NULL,
	Stamp BIGINT NOT NULL,
	Version BIGINT NOT NULL,
	State VARBINARY,
	Value LONGVARBINARY,
	PRIMARY KEY (Point));

CREATE TABLE TestsArchive (
	Point BINARY(16) NOT NULL,
	Stamp BIGINT NOT NULL,
	Version BIGINT NOT NULL,
	State VARBINARY,
	Value LONGVARBINARY,
	PRIMARY KEY (Point, Stamp));
CREATE UNIQUE INDEX Tests_Version_IX1 ON Tests (Version);
CREATE INDEX Tests_Point_IX1 ON Tests (Point, Stamp DESC);

CREATE TABLE TestsSnapshot (
	Point BINARY(16) NOT NULL,
	Stamp BIGINT NOT NULL,
	Version BIGINT NOT NULL,
	State VARBINARY,
	Value LONGVARBINARY,
	PRIMARY KEY (Point));

// End.
