/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ObjectHeader.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.pap.dnp3.object;

import java.io.IOException;

import java.nio.ByteBuffer;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;
import org.rvpf.pap.dnp3.DNP3Messages;
import org.rvpf.pap.dnp3.DNP3ProtocolException;

/**
 * Object header.
 */
public final class ObjectHeader
{
    /**
     * Constructs an instance.
     */
    private ObjectHeader() {}

    /**
     * Constructs an instance.
     *
     * @param objectVariation The object variation.
     * @param prefixCode The prefix code.
     * @param rangeCode The range code.
     * @param range The optional range.
     */
    private ObjectHeader(
            final ObjectVariation objectVariation,
            final PrefixCode prefixCode,
            final RangeCode rangeCode,
            final Optional<Object> range)
    {
        _group = (byte) objectVariation.getObjectGroup().getCode();
        _variation = (byte) objectVariation.getCode();
        _qualifier = (byte) ((prefixCode
            .ordinal() << _PREFIX_CODE_SHIFT) | (rangeCode.ordinal()
                << _RANGE_CODE_SHIFT));
        _range = range.orElse(null);
    }

    /**
     * Returns a new instance from a buffer.
     *
     * @param buffer The buffer.
     *
     * @return The new instance.
     *
     * @throws IOException On I/O exception.
     */
    @Nonnull
    @CheckReturnValue
    public static ObjectHeader newInstance(
            @Nonnull final ByteBuffer buffer)
        throws IOException
    {
        final ObjectHeader objectHeader = new ObjectHeader();

        objectHeader._loadFromBuffer(buffer);

        return objectHeader;
    }

    /**
     * Returns a new instance from an other object header.
     *
     * @param objectHeader The other object header.
     *
     * @return The new instance.
     *
     * @throws DNP3ProtocolException On protocol exception.
     */
    @Nonnull
    @CheckReturnValue
    public static ObjectHeader newInstance(
            @Nonnull final ObjectHeader objectHeader)
        throws DNP3ProtocolException
    {
        return new ObjectHeader(
            objectHeader.getObjectVariation(),
            objectHeader.getPrefixCode(),
            objectHeader.getRangeCode(),
            objectHeader.getRange());
    }

    /**
     * Returns a new instance.
     *
     * @param objectVariation The object Variation.
     * @param objectRange The object range.
     *
     * @return The new instance.
     */
    @Nonnull
    @CheckReturnValue
    public static ObjectHeader newInstance(
            @Nonnull final ObjectVariation objectVariation,
            @Nonnull final ObjectRange objectRange)
    {
        return new ObjectHeader(
            objectVariation,
            PrefixCode.NONE,
            objectRange.getRangeCode(),
            Optional.of(objectRange.getRange()));
    }

    /**
     * Returns a new instance.
     *
     * @param objectVariation The object variation.
     * @param prefixCode The prefix code.
     * @param rangeCode The range code.
     * @param range The optional range.
     *
     * @return The new instance.
     */
    @Nonnull
    @CheckReturnValue
    public static ObjectHeader newInstance(
            @Nonnull final ObjectVariation objectVariation,
            @Nonnull final PrefixCode prefixCode,
            @Nonnull final RangeCode rangeCode,
            @Nonnull final Optional<Object> range)
    {
        return new ObjectHeader(objectVariation, prefixCode, rangeCode, range);
    }

    /**
     * Dumps itself and its object instances into a buffer.
     *
     * @param buffer The buffer.
     * @param objectInstances The optional object instances.
     *
     * @throws DNP3ProtocolException On DNP3 protocol exception
     */
    public void dumpToBuffer(
            @Nonnull final ByteBuffer buffer,
            @Nonnull final Optional<ObjectInstance[]> objectInstances)
        throws DNP3ProtocolException
    {
        buffer.put(_group);
        buffer.put(_variation);
        buffer.put(_qualifier);

        switch (getRangeCode()) {
            case START_STOP_INDEX_BYTE:
            case START_STOP_ADDRESS_BYTE:
            case COUNT_BYTE:
            case VARIABLE_COUNT_BYTE: {
                for (final byte octet: (byte[]) _range) {
                    buffer.put(octet);
                }

                break;
            }
            case START_STOP_INDEX_SHORT:
            case START_STOP_ADDRESS_SHORT:
            case COUNT_SHORT: {
                for (final short word: (short[]) _range) {
                    buffer.putShort(word);
                }

                break;
            }
            case START_STOP_INDEX_INT:
            case START_STOP_ADDRESS_INT:
            case COUNT_INT: {
                for (final int doubleWord: (int[]) _range) {
                    buffer.putInt(doubleWord);
                }

                break;
            }
            default: {
                break;
            }
        }

        if (objectInstances.isPresent()) {
            for (final ObjectInstance objectInstance: objectInstances.get()) {
                switch (getPrefixCode()) {
                    case INDEX_BYTE: {
                        buffer.put((byte) objectInstance.getObjectIndex());

                        break;
                    }
                    case INDEX_SHORT: {
                        buffer
                            .putShort((short) objectInstance.getObjectIndex());

                        break;
                    }
                    case INDEX_INT: {
                        buffer.putInt(objectInstance.getObjectIndex());

                        break;
                    }
                    case NONE: {
                        break;
                    }
                    default: {
                        throw new IllegalArgumentException();
                    }

                }

                objectInstance.dumpToBuffer(buffer);
            }
        }
    }

    /**
     * Gets the header total length in bytes.
     *
     * @return The header total length in bytes.
     *
     * @throws DNP3ProtocolException On DNP3 protocol exception
     */
    @CheckReturnValue
    public int getLength()
        throws DNP3ProtocolException
    {
        final int rangeLength;

        switch (getRangeCode()) {
            case COUNT_BYTE: {
                rangeLength = 1;

                break;
            }
            case START_STOP_INDEX_BYTE:
            case START_STOP_ADDRESS_BYTE:
            case COUNT_SHORT: {
                rangeLength = 2;

                break;
            }
            case START_STOP_INDEX_SHORT:
            case START_STOP_ADDRESS_SHORT:
            case COUNT_INT: {
                rangeLength = 4;

                break;
            }
            case START_STOP_INDEX_INT:
            case START_STOP_ADDRESS_INT: {
                rangeLength = 8;

                break;
            }
            default: {
                rangeLength = 0;

                break;
            }
        }

        return 3 + rangeLength;
    }

    /**
     * Gets the length needed to transmit the instances.
     *
     * @param instances The instances.
     *
     * @return The needed length.
     *
     * @throws DNP3ProtocolException On DNP3 protocol exception
     */
    @CheckReturnValue
    public int getLength(
            @Nonnull final ObjectInstance[] instances)
        throws DNP3ProtocolException
    {
        final int prefixLength;

        switch (getPrefixCode()) {
            case INDEX_BYTE:
            case SIZE_BYTE: {
                prefixLength = 1;

                break;
            }
            case INDEX_SHORT:
            case SIZE_SHORT: {
                prefixLength = 2;

                break;
            }
            case INDEX_INT:
            case SIZE_INT: {
                prefixLength = 4;

                break;
            }
            default: {
                prefixLength = 0;

                break;
            }
        }

        int length = 0;

        for (final ObjectInstance instance: instances) {
            length += prefixLength + instance.getObjectLength();
        }

        return length;
    }

    /**
     * Gets the object count.
     *
     * @return The object count.
     */
    @CheckReturnValue
    public int getObjectCount()
    {
        return _count;
    }

    /**
     * Gets the object group.
     *
     * @return The object group.
     *
     * @throws DNP3ProtocolException On unknown group.
     */
    @Nonnull
    @CheckReturnValue
    public ObjectGroup getObjectGroup()
        throws DNP3ProtocolException
    {
        final Optional<ObjectGroup> objectGroup = GroupCategory
            .objectGroup(_group);

        if (!objectGroup.isPresent()) {
            throw new DNP3ProtocolException(
                DNP3Messages.UNKNOWN_GROUP_CODE,
                String.valueOf(_group & 0xFF));
        }

        return objectGroup.get();
    }

    /**
     * Gets the object variation.
     *
     * @return The object variation.
     *
     * @throws DNP3ProtocolException On unknown group or variation.
     */
    @Nonnull
    @CheckReturnValue
    public ObjectVariation getObjectVariation()
        throws DNP3ProtocolException
    {
        final ObjectGroup objectGroup = getObjectGroup();
        final Optional<ObjectVariation> objectVariation = GroupCategory
            .objectVariation(objectGroup, _variation);

        if (!objectVariation.isPresent()) {
            throw new DNP3ProtocolException(
                DNP3Messages.UNKNOWN_VARIATION,
                Byte.valueOf(_variation),
                objectGroup.getTitle());
        }

        return objectVariation.get();
    }

    /**
     * Gets the prefix code.
     *
     * @return The prefix code.
     *
     * @throws DNP3ProtocolException On unknown prefix code.
     */
    @Nonnull
    @CheckReturnValue
    public PrefixCode getPrefixCode()
        throws DNP3ProtocolException
    {
        try {
            return _PREFIX_CODES[_prefixCode()];
        } catch (final ArrayIndexOutOfBoundsException exception) {
            throw new DNP3ProtocolException(
                DNP3Messages.UNKNOWN_PREFIX_CODE,
                Integer.valueOf(_prefixCode()));
        }
    }

    /**
     * Gets the range.
     *
     * @return The range.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Object> getRange()
    {
        return Optional.ofNullable(_range);
    }

    /**
     * Gets the range code.
     *
     * @return The range code.
     *
     * @throws DNP3ProtocolException On unknown range code.
     */
    @Nonnull
    @CheckReturnValue
    public RangeCode getRangeCode()
        throws DNP3ProtocolException
    {
        try {
            return _RANGE_CODES[_rangeCode()];
        } catch (final ArrayIndexOutOfBoundsException exception) {
            throw new DNP3ProtocolException(
                DNP3Messages.UNKNOWN_RANGE_CODE,
                Integer.valueOf(_rangeCode()));
        }
    }

    /**
     * Loads instances from a buffer.
     *
     * @param buffer The buffer.
     * @param objectIndices The optional object indices to supply.
     * @param objectSizes The optional object sizes to supply.
     * @param objectInstances The optional object instances to supply.
     *
     * @throws DNP3ProtocolException On DNP3 protocol exception
     */
    public void loadInstancesFromBuffer(
            @Nonnull final ByteBuffer buffer,
            @Nonnull final Optional<Number[]> objectIndices,
            @Nonnull final Optional<Number[]> objectSizes,
            @Nonnull final Optional<ObjectInstance[]> objectInstances)
        throws DNP3ProtocolException
    {
        final int start;
        final int stop;

        switch (getRangeCode()) {
            case COUNT_BYTE:
            case VARIABLE_COUNT_BYTE: {
                start = 0;
                stop = (((byte[]) _range)[0] & 0xFF) - 1;

                break;
            }
            case COUNT_SHORT: {
                start = 0;
                stop = (((short[]) _range)[0] & 0xFFFF) - 1;

                break;
            }
            case COUNT_INT: {
                start = 0;
                stop = ((int[]) _range)[0] - 1;

                break;
            }
            case START_STOP_INDEX_BYTE:
            case START_STOP_ADDRESS_BYTE: {
                start = ((byte[]) _range)[0] & 0xFF;
                stop = ((byte[]) _range)[1] & 0xFF;

                break;
            }
            case START_STOP_INDEX_SHORT:
            case START_STOP_ADDRESS_SHORT: {
                start = ((short[]) _range)[0] & 0xFFFF;
                stop = ((short[]) _range)[1] & 0xFFFF;

                break;
            }
            case START_STOP_INDEX_INT:
            case START_STOP_ADDRESS_INT: {
                start = ((int[]) _range)[0];
                stop = ((int[]) _range)[1];

                break;
            }
            default: {
                start = 0;
                stop = 0;

                break;
            }
        }

        _count = 1 + stop - start;

        if (objectIndices.isPresent()) {
            Require.success(objectIndices.get().length == _count);
        }

        if (objectSizes.isPresent()) {
            Require.success(objectSizes.get().length == _count);
        }

        if (objectInstances.isPresent()) {
            Require.success(objectInstances.get().length == _count);
        }

        final ObjectVariation objectVariation = getObjectVariation();
        int nextIndex = start;

        for (int i = 0; i < _count; ++i) {
            final int objectIndex;
            final int objectSize;

            switch (getPrefixCode()) {
                case INDEX_BYTE: {
                    objectIndex = buffer.get() & 0xFF;
                    objectIndices.get()[i] = Integer.valueOf(objectIndex);
                    objectSize = 0;

                    break;
                }
                case SIZE_BYTE: {
                    objectIndex = nextIndex;
                    objectSize = buffer.get() & 0xFF;
                    objectSizes.get()[i] = Integer.valueOf(objectSize);

                    break;
                }
                case INDEX_SHORT: {
                    objectIndex = buffer.getShort() & 0xFFFF;
                    objectIndices.get()[i] = Integer.valueOf(objectIndex);
                    objectSize = 0;

                    break;
                }
                case SIZE_SHORT: {
                    objectIndex = nextIndex;
                    objectSize = buffer.getShort() & 0xFFFF;
                    objectSizes.get()[i] = Integer.valueOf(objectSize);

                    break;
                }
                case INDEX_INT: {
                    objectIndex = buffer.getInt();
                    objectIndices.get()[i] = Integer.valueOf(objectIndex);
                    objectSize = 0;

                    break;
                }
                case SIZE_INT: {
                    objectIndex = nextIndex;
                    objectSize = buffer.getInt();
                    objectSizes.get()[i] = Integer.valueOf(objectSize);

                    break;
                }
                default: {
                    objectIndex = nextIndex;
                    objectSize = 0;

                    break;
                }
            }

            if (objectInstances.isPresent()) {
                final ObjectInstance objectInstance = GroupCategory
                    .newObjectInstance(objectVariation);

                objectInstance.setUp(objectIndex, stop);
                objectInstance.loadFromBuffer(buffer);
                objectInstances.get()[i] = objectInstance;
            }

            if (objectVariation.isPacked()) {
                break;
            }

            ++nextIndex;

        }
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        final StringBuilder builder = new StringBuilder();
        final ObjectGroup objectGroup;
        final ObjectVariation objectVariation;

        try {
            objectGroup = getObjectGroup();
            objectVariation = getObjectVariation();
        } catch (final DNP3ProtocolException exception) {
            throw new RuntimeException(exception);
        }

        builder.append("Group ");
        builder.append(objectGroup.getCode());
        builder.append(" (");
        builder.append(objectGroup.getTitle());
        builder.append(") variation ");
        builder.append(objectVariation.getCode());
        builder.append(" (");
        builder.append(objectVariation.getTitle());
        builder.append(") [");

        final int rangeCode = _rangeCode();

        if (rangeCode == RangeCode.START_STOP_INDEX_BYTE.ordinal()) {
            builder.append(((byte[]) _range)[0] & 0xFF);
            builder.append(':');
            builder.append(((byte[]) _range)[1] & 0xFF);
        } else if (rangeCode == RangeCode.START_STOP_INDEX_SHORT.ordinal()) {
            builder.append(((short[]) _range)[0] & 0xFFFF);
            builder.append(':');
            builder.append(((short[]) _range)[1] & 0xFFFF);
        } else if (rangeCode == RangeCode.START_STOP_INDEX_INT.ordinal()) {
            builder.append(((int[]) _range)[0]);
            builder.append(':');
            builder.append(((int[]) _range)[1]);
        } else if (rangeCode == RangeCode.COUNT_BYTE.ordinal()) {
            builder.append(((byte[]) _range)[0] & 0xFF);
        } else if (rangeCode == RangeCode.COUNT_SHORT.ordinal()) {
            builder.append(((short[]) _range)[0] & 0xFFFF);
        } else if (rangeCode == RangeCode.COUNT_INT.ordinal()) {
            builder.append(((int[]) _range)[0]);
        }

        builder.append("]/");

        return builder.toString();
    }

    private void _loadFromBuffer(final ByteBuffer buffer)
        throws IOException
    {
        _group = buffer.get();
        _variation = buffer.get();
        _qualifier = buffer.get();

        switch (getRangeCode()) {
            case COUNT_BYTE:
            case VARIABLE_COUNT_BYTE: {
                _count = buffer.get();
                _range = new byte[] {(byte) _count};

                break;
            }
            case START_STOP_INDEX_BYTE:
            case START_STOP_ADDRESS_BYTE: {
                final int start = buffer.get() & 0xFF;
                final int stop = buffer.get() & 0xFF;

                _count = 1 + stop - start;
                _range = new byte[] {(byte) start, (byte) stop};

                break;
            }
            case COUNT_SHORT: {
                _count = buffer.getShort() & 0xFFFF;
                _range = new short[] {(short) _count};

                break;
            }
            case START_STOP_INDEX_SHORT:
            case START_STOP_ADDRESS_SHORT: {
                final int start = buffer.getShort() & 0xFFFF;
                final int stop = buffer.getShort() & 0xFFFF;

                _count = 1 + stop - start;
                _range = new short[] {(short) start, (short) stop};

                break;
            }
            case COUNT_INT: {
                _count = buffer.getInt();
                _range = new int[] {_count};

                break;
            }
            case START_STOP_INDEX_INT:
            case START_STOP_ADDRESS_INT: {
                final int start = buffer.getInt();
                final int stop = buffer.getInt();

                _count = 1 + stop - start;
                _range = new int[] {start, stop};

                break;
            }
            default: {
                _count = 0;
                _range = null;

                break;
            }
        }
    }

    private int _prefixCode()
    {
        return (_qualifier & _PREFIX_CODE_MASK) >> _PREFIX_CODE_SHIFT;
    }

    private int _rangeCode()
    {
        return (_qualifier & _RANGE_CODE_MASK) >> _RANGE_CODE_SHIFT;
    }

    private static final byte _PREFIX_CODE_MASK = 0x70;
    private static final int _PREFIX_CODE_SHIFT = 4;
    private static final byte _RANGE_CODE_MASK = 0x0F;
    private static final int _RANGE_CODE_SHIFT = 0;
    private static final RangeCode[] _RANGE_CODES = RangeCode.class
        .getEnumConstants();
    private static final PrefixCode[] _PREFIX_CODES = PrefixCode.class
        .getEnumConstants();

    private int _count;
    private byte _group;
    private byte _qualifier;
    private Object _range;
    private byte _variation;

    /**
     * Prefix code.
     */
    public enum PrefixCode
    {
        NONE,
        INDEX_BYTE,
        INDEX_SHORT,
        INDEX_INT,
        SIZE_BYTE,
        SIZE_SHORT,
        SIZE_INT;
    }

    /**
     * Range code.
     */
    public enum RangeCode
    {
        START_STOP_INDEX_BYTE,
        START_STOP_INDEX_SHORT,
        START_STOP_INDEX_INT,
        START_STOP_ADDRESS_BYTE,
        START_STOP_ADDRESS_SHORT,
        START_STOP_ADDRESS_INT,
        NONE,
        COUNT_BYTE,
        COUNT_SHORT,
        COUNT_INT,
        RESERVED_1,
        VARIABLE_COUNT_BYTE;
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
