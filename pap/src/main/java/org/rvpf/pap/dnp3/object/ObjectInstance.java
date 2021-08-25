/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ObjectInstance.java 4085 2019-06-16 15:17:12Z SFB $
 */

package org.rvpf.pap.dnp3.object;

import java.io.Serializable;

import java.nio.ByteBuffer;

import java.util.Arrays;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.pap.dnp3.object.content.ControlStatusCode;
import org.rvpf.pap.dnp3.object.content.DoubleBitState;
import org.rvpf.pap.dnp3.object.content.ObjectFlags;

/**
 * Object instance.
 */
public interface ObjectInstance
{
    /**
     * Dumps itself into a buffer.
     *
     * @param buffer The buffer.
     */
    void dumpToBuffer(@Nonnull final ByteBuffer buffer);

    /**
     * Gets the group category.
     *
     * @return The group category.
     */
    @Nonnull
    @CheckReturnValue
    GroupCategory getGroupCategory();

    /**
     * Gets the group code.
     *
     * @return The group code.
     */
    @CheckReturnValue
    int getGroupCode();

    /**
     * Gets the group name.
     *
     * @return The group name.
     */
    @Nonnull
    @CheckReturnValue
    String getGroupName();

    /**
     * Gets the group title.
     *
     * @return The group title.
     */
    @Nonnull
    @CheckReturnValue
    String getGroupTitle();

    /**
     * Gets the object group.
     *
     * @return The object group.
     */
    @Nonnull
    @CheckReturnValue
    ObjectGroup getObjectGroup();

    /**
     * Gets the object index.
     *
     * @return The object index.
     */
    @CheckReturnValue
    int getObjectIndex();

    /**
     * Gets the object length in bytes.
     *
     * @return The object length in bytes.
     */
    @CheckReturnValue
    int getObjectLength();

    /**
     * Gets the object variation.
     *
     * @return The object variation.
     */
    @Nonnull
    @CheckReturnValue
    ObjectVariation getObjectVariation();

    /**
     * Gets the point type.
     *
     * @return The optional point type.
     */
    @Nonnull
    @CheckReturnValue
    Optional<PointType> getPointType();

    /**
     * Gets the number of values represented.
     *
     * @return The number of values represented.
     */
    @CheckReturnValue
    int getValueCount();

    /**
     * Gets the variation code.
     *
     * @return The variation code.
     */
    @CheckReturnValue
    int getVariationCode();

    /**
     * Gets the variation name.
     *
     * @return The variation name.
     */
    @Nonnull
    @CheckReturnValue
    String getVariationName();

    /**
     * Gets the variation title.
     *
     * @return The variation title.
     */
    @Nonnull
    @CheckReturnValue
    String getVariationTitle();

    /**
     * Loads values from a buffer.
     *
     * @param buffer The buffer.
     */
    void loadFromBuffer(@Nonnull final ByteBuffer buffer);

    /**
     * Sets the object index.
     *
     * @param index The object index.
     */
    void setObjectIndex(final int index);

    /**
     * Sets up this
     *
     * @param objectIndex The object index (or address).
     * @param indexLimit The index limit.
     */
    void setUp(final int objectIndex, final int indexLimit);

    /**
     * Sets the number of values represented.
     *
     * @param count The number of values represented.
     */
    void setValueCount(final int count);

    /**
     * Object instance packed.
     */
    interface Packed
        extends ObjectInstance
    {
        /**
         * Gets a value from a position.
         *
         * @param position The position.
         *
         * @return The value.
         */
        @CheckReturnValue
        int get(int position);

        /**
         * Puts a value at a position.
         *
         * @param position The position.
         * @param value The value.
         */
        void put(int position, int value);
    }


    /**
     * Object instance with flags.
     */
    interface WithFlags
        extends ObjectInstance
    {
        /**
         * Gets the object flags.
         *
         * @return The object flags.
         */
        @Nonnull
        @CheckReturnValue
        ObjectFlags getFlags();

        /**
         * Sets the object flags.
         *
         * @param objectFlags The object flags (empty resets).
         */
        void setFlags(@Nonnull Optional<ObjectFlags> objectFlags);
    }


    /**
     * Object instance with status.
     */
    interface WithStatus
        extends ObjectInstance
    {
        /**
         * Gets the object status.
         *
         * @return The object status.
         */
        @Nonnull
        @CheckReturnValue
        ControlStatusCode getStatus();

        /**
         * Sets the object status.
         *
         * @param objectStatus The object status (empty resets).
         */
        void setStatus(@Nonnull Optional<ControlStatusCode> objectStatus);
    }


    /**
     * Object instance with time.
     */
    interface WithTime
        extends ObjectInstance
    {
        /**
         * Gets the time.
         *
         * @return The time.
         */
        @Nonnull
        @CheckReturnValue
        DateTime getTime();

        /**
         * Sets the time.
         *
         * @param time The time.
         */
        void setTime(@Nonnull DateTime time);
    }


    /**
     * Object instance with value.
     */
    interface WithValue
        extends ObjectInstance
    {
        /**
         * Returns a 'boolean' value from a serializable.
         *
         * @param value The serializable.
         *
         * @return The 'boolean' value.
         */
        @CheckReturnValue
        static boolean booleanValue(@Nonnull final Serializable value)
        {
            if (value instanceof Boolean) {
                return ((Boolean) value).booleanValue();
            }

            if (value instanceof String) {
                return Boolean.parseBoolean((String) value);
            }

            return ((Number) value).longValue() != 0;
        }

        /**
         * Returns a 'byte' value from a serializable.
         *
         * @param value The serializable.
         *
         * @return The 'byte' value.
         */
        @CheckReturnValue
        static short byteValue(@Nonnull final Serializable value)
        {
            if (value instanceof String) {
                return Byte.parseByte((String) value);
            }

            return ((Number) value).byteValue();
        }

        /**
         * Returns a 'DoubleBitState' value from a serializable.
         *
         * @param value The serializable.
         *
         * @return The 'DoubleBitState' value.
         */
        @Nonnull
        @CheckReturnValue
        static DoubleBitState doubleBitStateValue(
                @Nonnull final Serializable value)
        {
            if (value instanceof DoubleBitState) {
                return (DoubleBitState) value;
            }

            return DoubleBitState.instance(((Number) value).intValue());
        }

        /**
         * Returns a 'double' value from a serializable.
         *
         * @param value The serializable.
         *
         * @return The 'double' value.
         */
        @CheckReturnValue
        static double doubleValue(@Nonnull final Serializable value)
        {
            if (value instanceof String) {
                return Double.parseDouble((String) value);
            }

            return ((Number) value).doubleValue();
        }

        /**
         * Returns a 'float' value from a serializable.
         *
         * @param value The serializable.
         *
         * @return The 'float' value.
         */
        @CheckReturnValue
        static float floatValue(@Nonnull final Serializable value)
        {
            if (value instanceof String) {
                return Float.parseFloat((String) value);
            }

            return ((Number) value).floatValue();
        }

        /**
         * Returns an 'int' value from a serializable.
         *
         * @param value The serializable.
         *
         * @return The 'int' value.
         */
        @CheckReturnValue
        static int intValue(@Nonnull final Serializable value)
        {
            if (value instanceof String) {
                return Integer.parseInt((String) value);
            }

            return ((Number) value).intValue();
        }

        /**
         * Returns a 'short' value from a serializable.
         *
         * @param value The serializable.
         *
         * @return The 'short' value.
         */
        @CheckReturnValue
        static short shortValue(@Nonnull final Serializable value)
        {
            if (value instanceof String) {
                return Short.parseShort((String) value);
            }

            return ((Number) value).shortValue();
        }

        /**
         * Gets the value.
         *
         * @return The value.
         */
        @Nonnull
        @CheckReturnValue
        Serializable getValue();

        /**
         * Sets the value.
         *
         * @param value The value.
         */
        void setValue(@Nonnull final Serializable value);
    }


    /**
     * Abstract object instance.
     */
    abstract class Abstract
        implements ObjectInstance
    {
        /** {@inheritDoc}
         */
        @Override
        public GroupCategory getGroupCategory()
        {
            return getObjectGroup().getCategory();
        }

        /** {@inheritDoc}
         */
        @Override
        public final int getGroupCode()
        {
            return getObjectGroup().getCode();
        }

        /** {@inheritDoc}
         */
        @Override
        public final String getGroupName()
        {
            return getObjectGroup().name();
        }

        /** {@inheritDoc}
         */
        @Override
        public final String getGroupTitle()
        {
            return getObjectGroup().getTitle();
        }

        /** {@inheritDoc}
         */
        @Override
        public final ObjectGroup getObjectGroup()
        {
            return getObjectVariation().getObjectGroup();
        }

        /** {@inheritDoc}
         */
        @Override
        public final int getObjectIndex()
        {
            return _index;
        }

        /** {@inheritDoc}
         */
        @Override
        public final Optional<PointType> getPointType()
        {
            return getObjectGroup().getPointType();
        }

        /** {@inheritDoc}
         */
        @Override
        public int getValueCount()
        {
            return 1;
        }

        /** {@inheritDoc}
         */
        @Override
        public final int getVariationCode()
        {
            return getObjectVariation().getCode();
        }

        /** {@inheritDoc}
         */
        @Override
        public final String getVariationName()
        {
            return getObjectVariation().name();
        }

        /** {@inheritDoc}
         */
        @Override
        public final String getVariationTitle()
        {
            return getObjectVariation().getTitle();
        }

        /** {@inheritDoc}
         */
        @Override
        public final void setObjectIndex(final int index)
        {
            _index = index;
        }

        /** {@inheritDoc}
         */
        @Override
        public final void setUp(final int objectIndex, final int indexLimit)
        {
            setObjectIndex(objectIndex);

            if (getObjectVariation().isPacked()) {
                setValueCount(1 + indexLimit - objectIndex);
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public void setValueCount(final int count)
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return "#";
        }

        /**
         * Gets bits from a packed value at a position.
         *
         * @param values The values.
         * @param position The position.
         * @param bitsPerValue The number of bits per value.
         * @param valueMask A mask matching the number of bits.
         *
         * @return The value.
         */
        protected static int getBits(
                @Nonnull final byte[] values,
                final int position,
                final int bitsPerValue,
                final int valueMask)
        {
            final int bitCount = (position + 1) * bitsPerValue;
            int byteIndex = bitCount % 8;
            final int bitIndex = bitCount - (byteIndex * 8);

            if (bitIndex > 0) {
                ++byteIndex;
            }

            return (values[byteIndex] >> bitIndex) & valueMask;
        }

        /**
         * Gets the time from a buffer.
         *
         * @param buffer The buffer.
         *
         * @return The time in millis since 1970-01-01.
         */
        @CheckReturnValue
        protected static long getTimeFromBuffer(
                @Nonnull final ByteBuffer buffer)
        {
            long time = (long) buffer.getInt() & 0xFFFFFFFF;

            time |= buffer.getShort() & 0xFFFF;

            return time;
        }

        /**
         * Puts bits into a packed value at a position.
         *
         * @param value The value.
         * @param values The values.
         * @param position The position.
         * @param bitsPerValue The number of bits per value.
         * @param valueMask A mask matching the number of bits.
         *
         * @return The potentialy reallocated values.
         */
        protected static byte[] putBits(
                final int value,
                @Nonnull byte[] values,
                final int position,
                final int bitsPerValue,
                final int valueMask)
        {
            final int bitCount = (position + 1) * bitsPerValue;
            int byteIndex = bitCount % 8;
            final int bitIndex = bitCount - (byteIndex * 8);

            if (bitIndex > 0) {
                ++byteIndex;
            }

            if (byteIndex >= values.length) {
                values = Arrays.copyOf(values, byteIndex + 1);
            }

            final int mask = valueMask << bitIndex;
            final int valueByte = values[byteIndex] & ~mask;

            values[byteIndex] = (byte) (valueByte
                    | ((value & valueMask) << bitIndex));

            return values;
        }

        /**
         * Puts time into a buffer.
         *
         * @param time The time.
         * @param buffer The buffer.
         */
        protected static void putTimeToBuffer(
                final long time,
                @Nonnull final ByteBuffer buffer)
        {
            buffer.putInt((int) time);
            buffer.putShort((short) (time >> 32));
        }

        /**
         * Returns the number of bytes needed.
         *
         * @param bitsPerValue The number of bits per value.
         *
         * @return The number of bytes needed.
         */
        @CheckReturnValue
        protected final int byteCount(final int bitsPerValue)
        {
            final int bitCount = getValueCount() * bitsPerValue;
            int byteCount = bitCount % 8;

            if ((bitCount - (byteCount * 8)) > 0) {
                ++byteCount;
            }

            return byteCount;
        }

        /** Number of bytes to store time. */
        protected static final int TIME_BYTES = Integer.BYTES + Short.BYTES;

        private int _index;
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
