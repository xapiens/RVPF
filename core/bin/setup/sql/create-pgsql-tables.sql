-- $Id: create-pgsql-tables.sql 2037 2013-12-08 16:21:05Z SFB $

CREATE TABLE Archive (
	Point BYTEA NOT NULL,
	Stamp BIGINT NOT NULL,
	Version BIGINT NOT NULL,
	State BYTEA,
	Value BYTEA,
	PRIMARY KEY (Point, Stamp));
CREATE UNIQUE INDEX Archive_Version_IX1 ON Archive (Version);
CREATE INDEX Archive_Point_IX1 ON Archive (Point, Stamp DESC);

CREATE TABLE Snapshot (
	Point BYTEA NOT NULL,
	Stamp BIGINT NOT NULL,
	Version BIGINT NOT NULL,
	State BYTEA,
	Value BYTEA,
	PRIMARY KEY (Point));

CREATE TABLE TestsArchive (
	Point BYTEA NOT NULL,
	Stamp BIGINT NOT NULL,
	Version BIGINT NOT NULL,
	State BYTEA,
	Value BYTEA,
	PRIMARY KEY (Point, Stamp));
CREATE UNIQUE INDEX Tests_Version_IX1 ON Tests (Version);
CREATE INDEX Tests_Point_IX1 ON Tests (Point, Stamp DESC);

CREATE TABLE TestsSnapshot (
	Point BYTEA NOT NULL,
	Stamp BIGINT NOT NULL,
	Version BIGINT NOT NULL,
	State BYTEA,
	Value BYTEA,
	PRIMARY KEY (Point));

-- End.
