/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MariaDBDialect.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.store.server.the.sql.dialect;

import java.io.File;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.tool.Inet;

/**
 * MariaDB database dialect support.
 */
public final class MariaDBDialect
    extends DialectSupport.Abstract
{
    /**
     * Constructs an instance.
     */
    public MariaDBDialect()
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

        stringBuilder.append("CREATE TABLE IF NOT EXISTS ");
        stringBuilder.append(getCatalogSchemaTable());
        stringBuilder.append(" (");
        stringBuilder.append(getPointColumn());
        stringBuilder.append(" BINARY(16) NOT NULL, ");
        stringBuilder.append(getStampColumn());
        stringBuilder.append(" BIGINT NOT NULL, ");
        stringBuilder.append(getVersionColumn());
        stringBuilder.append(" BIGINT NOT NULL, ");
        stringBuilder.append(getStateColumn());
        stringBuilder.append(" BLOB, ");
        stringBuilder.append(getValueColumn());
        stringBuilder.append(" LONGBLOB,");
        stringBuilder.append(" PRIMARY KEY (");
        stringBuilder.append(getPointColumn());

        if (!isSnapshot()) {
            stringBuilder.append(",");
            stringBuilder.append(getStampColumn());
        }

        stringBuilder.append(")");

        if (!isPullDisabled()) {
            stringBuilder.append(", UNIQUE (");
            stringBuilder.append(getVersionColumn());
            stringBuilder.append(")");
        }

        if (!isSnapshot()) {
            stringBuilder.append(", INDEX (");
            stringBuilder.append(getPointColumn());
            stringBuilder.append(",");
            stringBuilder.append(getStampColumn());
            stringBuilder.append(" DESC)");
        }

        stringBuilder.append(")");
        sql.add(stringBuilder.toString());

        return sql;
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
        return Optional.of(DEFAULT_DRIVER_CLASS_DEF);
    }

    /** Default connection URL. */
    public static final String DEFAULT_CONNECTION_URL = "jdbc:mariadb://"
        + Inet.LOCAL_HOST + "/rvpf";

    /** Default connection user. */
    public static final String DEFAULT_CONNECTION_USER = "rvpf";

    /** Default driver class definition. */
    public static final ClassDefImpl DEFAULT_DRIVER_CLASS_DEF =
        new ClassDefImpl(
            "org.mariadb.jdbc.Driver");

    /** Dialect name. */
    public static final String DIALECT_NAME = "MariaDB";
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
