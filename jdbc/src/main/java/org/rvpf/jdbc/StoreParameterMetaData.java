/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreParameterMetaData.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.jdbc;

import java.sql.ParameterMetaData;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Store parameter metadata.
 */
final class StoreParameterMetaData
    implements ParameterMetaData
{
    /**
     * Constructs an instance.
     *
     * @param parameterCount The parameter count.
     */
    StoreParameterMetaData(final int parameterCount)
    {
        _parameterCount = parameterCount;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getParameterClassName(final int param)
        throws SQLException
    {
        return String.class.getName();
    }

    /** {@inheritDoc}
     */
    @Override
    public int getParameterCount()
        throws SQLException
    {
        return _parameterCount;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getParameterMode(final int param)
        throws SQLException
    {
        return parameterModeUnknown;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getParameterType(final int param)
        throws SQLException
    {
        return Types.VARCHAR;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getParameterTypeName(final int param)
        throws SQLException
    {
        return "VARCHAR";
    }

    /** {@inheritDoc}
     */
    @Override
    public int getPrecision(final int param)
        throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getScale(final int param)
        throws SQLException
    {
        return 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public int isNullable(final int param)
        throws SQLException
    {
        return parameterNullableUnknown;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isSigned(final int param)
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isWrapperFor(final Class<?> iface)
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public <T> T unwrap(final Class<T> iface)
        throws SQLException
    {
        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    private final int _parameterCount;
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
