/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PostgreSQLDialect.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.store.server.the.sql.dialect;

import java.io.File;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.rvpf.base.ClassDefImpl;

/**
 * PostgreSQL database dialect support.
 */
public final class PostgreSQLDialect
    extends DialectSupport.Abstract
{
    /**
     * Constructs an instance.
     */
    public PostgreSQLDialect()
    {
        super(DIALECT_NAME);
    }

    /** {@inheritDoc}
     */
    @Override
    public List<String> getCreateTableSQL()
    {
        final List<String> sql = new ArrayList<>();
        final StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("CREATE TABLE ");
        stringBuilder.append(getCatalogSchemaTable());
        stringBuilder.append(" (");
        stringBuilder.append(getPointColumn());
        stringBuilder.append(" BYTEA NOT NULL, ");
        stringBuilder.append(getStampColumn());
        stringBuilder.append(" BIGINT NOT NULL, ");
        stringBuilder.append(getVersionColumn());
        stringBuilder.append(" BIGINT NOT NULL, ");
        stringBuilder.append(getStateColumn());
        stringBuilder.append(" BYTEA, ");
        stringBuilder.append(getValueColumn());
        stringBuilder.append(" BYTEA,");
        stringBuilder.append(" PRIMARY KEY (");
        stringBuilder.append(getPointColumn());

        if (!isSnapshot()) {
            stringBuilder.append(",");
            stringBuilder.append(getStampColumn());
        }

        stringBuilder.append("))");
        sql.add(stringBuilder.toString());

        stringBuilder
            .append(
                "CREATE TABLE " + getCatalogSchemaTable() + " ("
                + getPointColumn() + " BYTEA NOT NULL, " + getStampColumn()
                + " BIGINT NOT NULL, " + getVersionColumn()
                + " BIGINT NOT NULL, " + getStateColumn() + " BYTEA, "
                + getValueColumn() + " BYTEA, PRIMARY KEY ("
                + getPointColumn());

        if (!isSnapshot()) {
            stringBuilder.append("," + getStampColumn());
        }

        stringBuilder.append("))");

        if (!isPullDisabled()) {
            stringBuilder.setLength(0);
            stringBuilder.append("CREATE UNIQUE INDEX ");
            stringBuilder.append(getCatalogSchemaTable());
            stringBuilder.append("_");
            stringBuilder.append(getVersionColumn());
            stringBuilder.append("_IX1 ON ");
            stringBuilder.append(getCatalogSchemaTable());
            stringBuilder.append(" (");
            stringBuilder.append(getVersionColumn());
            stringBuilder.append(")");
            sql.add(stringBuilder.toString());
        }

        if (!isSnapshot()) {
            stringBuilder.setLength(0);
            stringBuilder.append("CREATE INDEX ");
            stringBuilder.append(getCatalogSchemaTable());
            stringBuilder.append("_");
            stringBuilder.append(getPointColumn());
            stringBuilder.append("_IX1 ON ");
            stringBuilder.append(getCatalogSchemaTable());
            stringBuilder.append(" (");
            stringBuilder.append(getPointColumn());
            stringBuilder.append(",");
            stringBuilder.append(getStampColumn());
            stringBuilder.append(" DESC)");
            sql.add(stringBuilder.toString());
        }

        return sql;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<String> getDefaultConnectionPassword()
    {
        return Optional.of(DEFAULT_CONNECTION_PASSWORD);
    }

    /** {@inheritDoc}
     */
    @Override
    public String getDefaultConnectionURL(
            final File storeDataDir,
            final String storeEntityName)
    {
        return DEFAULT_CONNECTION_URL;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getDefaultConnectionUser()
    {
        return DEFAULT_CONNECTION_USER;
    }

    /** {@inheritDoc}
     */
    @Override
    protected Optional<ClassDefImpl> getDefaultDriverClassDef()
    {
        return Optional.ofNullable(DEFAULT_DRIVER_CLASS_DEF);
    }

    /** Default connection password. */
    public static final String DEFAULT_CONNECTION_PASSWORD = " ";

    /** Default connection URL. */
    public static final String DEFAULT_CONNECTION_URL = "jdbc:postgresql:rvpf";

    /** Default connection user. */
    public static final String DEFAULT_CONNECTION_USER = "rvpf";

    /** Default driver class definition. */
    public static final ClassDefImpl DEFAULT_DRIVER_CLASS_DEF =
        new ClassDefImpl(
            "org.postgresql.Driver");

    /** Dialect name. */
    public static final String DIALECT_NAME = "PostgreSQL";
}

/* This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * version 2.1 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA
 */
