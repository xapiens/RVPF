/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PointType.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3.object;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Content;
import org.rvpf.pap.dnp3.object.content.DataType;
import org.rvpf.pap.dnp3.object.groupCategory.analogInputs.AnalogInputSupport;
import org.rvpf.pap.dnp3.object.groupCategory.analogOutputs.AnalogOutputSupport;
import org.rvpf.pap.dnp3.object.groupCategory.binaryInputs
    .DoubleBitInputSupport;
import org.rvpf.pap.dnp3.object.groupCategory.binaryInputs
    .SingleBitInputSupport;
import org.rvpf.pap.dnp3.object.groupCategory.binaryOutputs.BinaryOutputSupport;
import org.rvpf.pap.dnp3.object.groupCategory.counters.CounterSupport;

/**
 * Point type enumeration.
 */
public enum PointType
{
    ANALOG_INPUT("Analog Input", new AnalogInputSupport()),
    ANALOG_OUTPUT("Analog Output", new AnalogOutputSupport()),
    SINGLE_BIT_INPUT("Single-bit Input", new SingleBitInputSupport()),
    DOUBLE_BIT_INPUT("Single-bit Input", new DoubleBitInputSupport()),
    BINARY_OUTPUT("Binary Output", new BinaryOutputSupport()),
    COUNTER("Counter", new CounterSupport());

    /**
     * Constructs an instance.
     *
     * @param title The point type title.
     * @param pointTypeSupport The point type support.
     */
    PointType(
            @Nonnull final String title,
            @Nonnull final Support pointTypeSupport)
    {
        _title = title;
        _support = pointTypeSupport;
    }

    /**
     * Returns a new object instance for an object variation.
     *
     * @param objectVariation The object variation.
     *
     * @return The new object instance.
     */
    @Nonnull
    @CheckReturnValue
    public static ObjectInstance newObjectInstance(
            @Nonnull final ObjectVariation objectVariation)
    {
        final ObjectInstance objectInstance;

        try {
            objectInstance = objectVariation.getObjectClass().newInstance();
        } catch (final ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }

        return objectInstance;
    }

    /**
     * Gets the support instance.
     *
     * @return The support instance.
     */
    @Nonnull
    @CheckReturnValue
    public Support getSupport()
    {
        return _support;
    }

    /**
     * Gets the point type title.
     *
     * @return The point type title.
     */
    @Nonnull
    @CheckReturnValue
    public String getTitle()
    {
        return _title;
    }

    private final Support _support;
    private final String _title;

    /**
     * Point type support.
     */
    public interface Support
    {
        /**
         * Gets the data type for a content.
         *
         * @param content The content.
         *
         * @return The data type.
         */
        @Nonnull
        @CheckReturnValue
        Optional<DataType> getDataType(@Nonnull Content content);

        /**
         * Gets the input variation.
         *
         * @return The input variation.
         */
        @Nonnull
        @CheckReturnValue
        ObjectVariation getInputVariation();

        /**
         * Gets the input variation for a data type.
         *
         * @param dataType The data type.
         *
         * @return The input variation (empty if none).
         */
        @Nonnull
        @CheckReturnValue
        Optional<ObjectVariation> getInputVariation(@Nonnull DataType dataType);

        /**
         * Gets the output variation for a data type.
         *
         * @param dataType The data type.
         *
         * @return The output variation (empty if none).
         */
        @Nonnull
        @CheckReturnValue
        Optional<ObjectVariation> getOutputVariation(
                @Nonnull DataType dataType);

        /**
         * Gets the point type.
         *
         * @return The point type.
         */
        @Nonnull
        @CheckReturnValue
        PointType getPointType();

        /**
         * Asks if this point type is read-only.
         *
         * @return True if this point type is read-only.
         */
        @CheckReturnValue
        boolean isReadOnly();
    }
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
