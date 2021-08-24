# $Id: create-mariadb-tables.sql 4114 2019-08-03 15:53:43Z SFB $

CREATE TABLE IF NOT EXISTS Archive (
	Point BINARY(16) NOT NULL,
	Stamp BIGINT NOT NULL,
	Version BIGINT NOT NULL,
	State BLOB,
	Value LONGBLOB,
	PRIMARY KEY (Point, Stamp),
	UNIQUE (Version),
	INDEX (Point, Stamp DESC));

CREATE TABLE IF NOT EXISTS Snapshot (
	Point BINARY(16) NOT NULL,
	Stamp BIGINT NOT NULL,
	Version BIGINT NOT NULL,
	State BLOB,
	Value LONGBLOB,
	PRIMARY KEY (Point));

CREATE TABLE IF NOT EXISTS TestsArchive (
	Point BINARY(16) NOT NULL,
	Stamp BIGINT NOT NULL,
	Version BIGINT NOT NULL,
	State BLOB,
	Value LONGBLOB,
	PRIMARY KEY (Point, Stamp),
	UNIQUE (Version),
	INDEX (Point, Stamp DESC));

CREATE TABLE IF NOT EXISTS TestsSnapshot (
	Point BINARY(16) NOT NULL,
	Stamp BIGINT NOT NULL,
	Version BIGINT NOT NULL,
	State BLOB,
	Value LONGBLOB,
	PRIMARY KEY (Point));

# End.
