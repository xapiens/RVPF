/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AnalogOutputStatusVariation.java 3977 2019-05-11 20:32:50Z SFB $
 */

package org.rvpf.pap.dnp3.object.groupCategory.analogOutputs;

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
 * Analog Output Status variation enumeration.
 */
public enum AnalogOutputStatusVariation
    implements ObjectVariation
{
    ANY(0, "Any", G40V0.class, Optional.empty()),
    INTEGER_WITH_FLAG(1, "32-bit with flag", G40V1.class, Optional
        .of(DataType.INT32)),
    SHORT_WITH_FLAG(2, "16-bit with flag", G40V2.class, Optional
        .of(DataType.INT16)),
    FLOAT_WITH_FLAG(3, "Single-precision with flag", G40V3.class, Optional
        .of(DataType.FLOAT16)),
    DOUBLE_WITH_FLAG(4, "Double-precision with flag", G40V4.class, Optional
        .of(DataType.FLOAT32));

    /**
     * Constructs an instance.
     *
     * @param code The variation code.
     * @param title The variation title.
     * @param objectClass The object class.
     * @param dataType The optional data type.
     */
    AnalogOutputStatusVariation(
            final int code,
            @Nonnull final String title,
            @Nonnull final Class<? extends ObjectInstance> objectClass,
            @Nonnull final Optional<DataType> dataType)
    {
        _code = code;
        _title = title;
        _objectClass = objectClass;
        _dataType = dataType;
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
    public static AnalogOutputStatusVariation instance(final int variationCode)
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
        return _dataType;
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
        return AnalogOutputsGroup.ANALOG_OUTPUT_STATUS;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getTitle()
    {
        return _title;
    }

    private static final Map<Integer, AnalogOutputStatusVariation> _VARIATION_MAP =
        MapFactory
            .codeMap(values());

    private final int _code;
    private final Optional<DataType> _dataType;
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
