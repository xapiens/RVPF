/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreColumn.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.jdbc;

import java.io.Serializable;

import java.sql.Types;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.UUID;
import org.rvpf.base.tool.Require;

/**
 * Store column.
 */
public enum StoreColumn
{
    ALL("*", 0, 0, true, Object.class, false),
    POINT("Point", 38, Types.VARCHAR, false, String.class, false),
    POINT_NAME("Point_name", 32, Types.VARCHAR, false, String.class, true),
    POINT_UUID("Point_UUID", 38, Types.VARCHAR, false, UUID.class, true),
    STAMP("Stamp", 28, Types.VARCHAR, false, DateTime.class, true),
    VERSION("Version", 28, Types.VARCHAR, true, DateTime.class, true),
    STATE("State", 16, Types.VARCHAR, true, Serializable.class, true),
    VALUE("Value", 40, Types.VARCHAR, true, Serializable.class, true),
    EXTRAPOLATED("EXTRAPOLATED", 12, Types.BOOLEAN, false, Boolean.class,
            false),
    INTERPOLATED("INTERPOLATED", 12, Types.BOOLEAN, false, Boolean.class,
            false),
    COUNT("COUNT(*)", 10, Types.INTEGER, true, Integer.class, false),
    CRONTAB("CRONTAB", 16, Types.VARCHAR, true, String.class, false),
    ELAPSED("ELAPSED", 28, Types.VARCHAR, true, String.class, false),
    STAMPS("STAMPS", 40, Types.VARCHAR, true, String.class, false),
    TIME_LIMIT("TIME_LIMIT", 28, Types.VARCHAR, true, String.class, false),;

    /**
     * Constructs an instance.
     *
     * @param label The column label.
     * @param displaySize The column display size.
     * @param type The column type.
     * @param nullable True if the column may be null.
     * @param objectClass The fetched object class.
     * @param writable True if the column is writable.
     */
    StoreColumn(
            @Nonnull final String label,
            final int displaySize,
            final int type,
            final boolean nullable,
            @Nonnull final Class<?> objectClass,
            final boolean writable)
    {
        _label = label;
        _displaySize = displaySize;
        _type = type;
        _nullable = nullable;
        _objectClass = objectClass;
        _writable = writable;
    }

    /**
     * Gets the type name for a type.
     *
     * @param type The type.
     *
     * @return The type name.
     */
    @Nonnull
    @CheckReturnValue
    static String getTypeName(final int type)
    {
        final String typeName;

        switch (type) {
            case Types.BOOLEAN: {
                typeName = "BOOLEAN";

                break;
            }
            case Types.INTEGER: {
                typeName = "INTEGER";

                break;
            }
            case Types.NULL: {
                typeName = "NULL";

                break;
            }
            case Types.JAVA_OBJECT: {
                typeName = "JAVA_OBJECT";

                break;
            }
            case Types.SMALLINT: {
                typeName = "SMALLINT";

                break;
            }
            case Types.VARCHAR: {
                typeName = "VARCHAR";

                break;
            }
            default: {
                throw new InternalError("Unexpected type: " + type);
            }
        }

        return typeName;
    }

    /**
     * Gets the display size.
     *
     * @return The display size.
     */
    @CheckReturnValue
    int getDisplaySize()
    {
        return _displaySize;
    }

    /**
     * Gets the label.
     *
     * @return The label.
     */
    @Nonnull
    @CheckReturnValue
    String getLabel()
    {
        return _label;
    }

    /**
     * Gets the objectClass.
     *
     * @return The objectClass.
     */
    @Nonnull
    @CheckReturnValue
    Class<?> getObjectClass()
    {
        Require.failure(_objectClass == Object.class);

        return _objectClass;
    }

    /**
     * Gets the type.
     *
     * @return The type.
     */
    @CheckReturnValue
    int getType()
    {
        return _type;
    }

    /**
     * Gets the type name.
     *
     * @return The type name.
     */
    @Nonnull
    @CheckReturnValue
    String getTypeName()
    {
        return getTypeName(_type);
    }

    /**
     * Gets the nullable indicator.
     *
     * @return The nullable indicator.
     */
    @CheckReturnValue
    boolean isNullable()
    {
        return _nullable;
    }

    /**
     * Gets the writable indicator.
     *
     * @return The writable indicator.
     */
    @CheckReturnValue
    boolean isWritable()
    {
        return _writable;
    }

    private final int _displaySize;
    private final String _label;
    private final boolean _nullable;
    private final Class<?> _objectClass;
    private final int _type;
    private final boolean _writable;
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
