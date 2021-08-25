/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TimeDateCommonTooVariation.java 3978 2019-05-12 10:53:03Z SFB $
 */

package org.rvpf.pap.dnp3.object.groupCategory.times;

import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;
import org.rvpf.pap.dnp3.object.ObjectGroup;
import org.rvpf.pap.dnp3.object.ObjectInstance;
import org.rvpf.pap.dnp3.object.ObjectVariation;
import org.rvpf.pap.dnp3.object.content.DataType;

/**
 * Time and Date Common Time-of-Occurrence variation enumeration.
 */
public enum TimeDateCommonTooVariation
    implements ObjectVariation
{
    ANY(0, "Any", G51V0.class),
    ABSOLUTE_TIME_SYNCHRONIZED(1, "Absolute time synchronized", G51V1.class),
    ABSOLUTE_TIME_UNSYNCHRONIZED(2, "Absolute time unsynchronized",
            G51V2.class);

    /**
     * Constructs an instance.
     *
     * @param code The variation code.
     * @param title The variation title.
     * @param objectClass The object class.
     */
    TimeDateCommonTooVariation(
            final int code,
            @Nonnull final String title,
            @Nonnull final Class<? extends ObjectInstance> objectClass)
    {
        _code = code;
        _title = title;
        _objectClass = objectClass;
    }

    /**
     * Returns the instance for a variation code.
     *
     * @param variationCode The variation code.
     *
     * @return The instance.
     */
    @Nonnull
    @CheckReturnValue
    public static TimeDateCommonTooVariation instance(final int variationCode)
    {
        return Require
            .notNull(_VARIATION_MAP.get(Integer.valueOf(variationCode)));
    }

    /** {@inheritDoc}
     */
    @Override
    public int getCode()
    {
        return _code;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<DataType> getDataType()
    {
        return Optional.empty();
    }

    /** {@inheritDoc}
     */
    @Override
    public Class<? extends ObjectInstance> getObjectClass()
    {
        return _objectClass;
    }

    /** {@inheritDoc}
     */
    @Override
    public ObjectGroup getObjectGroup()
    {
        return TimesGroup.TIME_DATE_COMMON_TOO;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getTitle()
    {
        return _title;
    }

    private static final Map<Integer, TimeDateCommonTooVariation> _VARIATION_MAP =
        MapFactory
            .codeMap(values());

    private final int _code;
    private final Class<? extends ObjectInstance> _objectClass;
    private final String _title;
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
