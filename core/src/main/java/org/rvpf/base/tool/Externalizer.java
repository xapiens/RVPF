/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Externalizer.java 4101 2019-06-30 14:56:50Z SFB $
 */

package org.rvpf.base.tool;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.util.container.KeyedValues;
import org.rvpf.base.value.BigRational;
import org.rvpf.base.value.Complex;
import org.rvpf.base.value.Dict;
import org.rvpf.base.value.Rational;
import org.rvpf.base.value.State;
import org.rvpf.base.value.Tuple;

/**
 * Externalizer.
 *
 * <p>Provides utility functions to help with values externalization.</p>
 */
public final class Externalizer
{
    /**
     * No instances.
     */
    private Externalizer() {}

    /**
     * Externalizes a serializable object to a byte array.
     *
     * <p>Note: returns null if the serializable is null.</p>
     *
     * @param serializable The serializable object.
     *
     * @return The byte array or null
     */
    @Nullable
    @CheckReturnValue
    public static byte[] externalize(@Nullable final Serializable serializable)
    {
        return externalize(serializable, Optional.empty());
    }

    /**
     * Externalizes a serializable object to a byte array.
     *
     * <p>Note: returns null if the serializable is null.</p>
     *
     * @param serializable The serializable object.
     * @param coder An optional coder.
     *
     * @return The byte array or null.
     */
    @Nullable
    @CheckReturnValue
    public static byte[] externalize(
            @Nullable final Serializable serializable,
            @Nonnull final Optional<Coder> coder)
    {
        final byte[] byteArray;

        if (serializable == null) {
            byteArray = null;
        } else {
            try {
                final ByteArrayOutputStream bytesStream =
                    new ByteArrayOutputStream();

                _write(serializable, bytesStream, coder.orElse(null));
                byteArray = bytesStream.toByteArray();
                bytesStream.close();
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
            }
        }

        return byteArray;
    }

    /**
     * Gets the type code of a byte array.
     *
     * @param byteArray The byte array.
     *
     * @return The type code of the byte array.
     */
    @CheckReturnValue
    public static byte getTypeCode(@Nullable final byte[] byteArray)
    {
        return ((byteArray != null)
                && (byteArray.length > 0))? byteArray[0]: ValueType.NULL
                    .getCode();
    }

    /**
     * Internalizes a serializable object from a byte array.
     *
     * <p>Note: returns null if the byte array is null or is empty.</p>
     *
     * @param byteArray The byte array.
     *
     * @return The serializable object or null.
     */
    @Nullable
    @CheckReturnValue
    public static Serializable internalize(@Nullable final byte[] byteArray)
    {
        return internalize(byteArray, Optional.empty());
    }

    /**
     * Internalizes a serializable object from a byte array.
     *
     * <p>Note: returns null if the byte array is null or is empty.</p>
     *
     * @param byteArray The byte array.
     * @param coder An optional coder.
     *
     * @return The serializable object or null.
     */
    @Nullable
    @CheckReturnValue
    public static Serializable internalize(
            @Nullable final byte[] byteArray,
            @Nonnull final Optional<Coder> coder)
    {
        final Serializable serializable;

        if ((byteArray == null) || (byteArray.length == 0)) {
            serializable = null;
        } else {
            final ByteArrayInputStream bytesStream = new ByteArrayInputStream(
                byteArray);

            try {
                serializable = _read(bytesStream, coder.orElse(null));
                Require.success(bytesStream.available() == 0);
                bytesStream.close();
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
            } catch (final ClassNotFoundException exception) {
                throw new RuntimeException(exception);
            }
        }

        return serializable;
    }

    /**
     * Reads a serializable from an object input.
     *
     * @param input The object input.
     *
     * @return The serializable or null.
     *
     * @throws IOException When appropriate.
     */
    @Nullable
    @CheckReturnValue
    public static Serializable readSerializable(
            @Nonnull final ObjectInput input)
        throws IOException
    {
        final int length = input.readInt();
        final Serializable serializable;

        if (length > 0) {
            final byte[] bytes = new byte[length];

            input.readFully(bytes);
            serializable = internalize(bytes);
        } else {
            serializable = null;
        }

        return serializable;
    }

    /**
     * Reads a string from an object input.
     *
     * @param input The object input.
     *
     * @return The string or null.
     *
     * @throws IOException When appropriate.
     */
    @Nullable
    @CheckReturnValue
    public static String readString(
            @Nonnull final ObjectInput input)
        throws IOException
    {
        int length = input.readInt();

        if (length < 0) {
            return null;
        }

        final StringBuilder stringBuilder = new StringBuilder(length);

        while (--length >= 0) {
            stringBuilder.append(input.readChar());
        }

        return stringBuilder.toString();
    }

    /**
     * Writes a serializable to an object output.
     *
     * @param serializable The serializable or null.
     * @param output The object output.
     *
     * @throws IOException When appropriate.
     */
    public static void writeSerializable(
            @Nullable final Serializable serializable,
            @Nonnull final ObjectOutput output)
        throws IOException
    {
        final byte[] bytes = externalize(serializable);

        if (bytes != null) {
            output.writeInt(bytes.length);
            output.write(bytes);
        } else {
            output.writeInt(0);
        }
    }

    /**
     * Writes a string to an object output.
     *
     * @param string The string or null.
     * @param output The object output.
     *
     * @throws IOException When appropriate.
     */
    public static void writeString(
            @Nullable final String string,
            @Nonnull final ObjectOutput output)
        throws IOException
    {
        if (string == null) {
            output.writeInt(-1);
        } else {
            output.writeInt(string.length());
            output.writeChars(string);
        }
    }

    private static Serializable _read(
            final InputStream stream,
            final Coder coder)
        throws IOException, ClassNotFoundException
    {
        final int typeCode = stream.read();

        Require.success(typeCode >= 0);

        final ValueType type = ValueType.get((byte) typeCode);
        final Serializable serializable;

        if (type == ValueType.OBJECT) {
            final ObjectInputStream objectStream = new ObjectInputStream(
                stream);

            serializable = (Serializable) objectStream.readObject();
            objectStream.close();
        } else {
            final DataInputStream dataStream = new DataInputStream(stream);

            switch (type) {
                case BIG_DECIMAL: {
                    final int scale = dataStream.readInt();

                    serializable = new BigDecimal(
                        new BigInteger(_readByteArray(dataStream)),
                        scale);

                    break;
                }
                case BIG_INTEGER: {
                    serializable = new BigInteger(_readByteArray(dataStream));

                    break;
                }
                case BIG_RATIONAL: {
                    final BigInteger numerator = new BigInteger(
                        _readByteArray(dataStream));
                    final BigInteger denominator = new BigInteger(
                        _readByteArray(dataStream));

                    serializable = BigRational.valueOf(numerator, denominator);

                    break;
                }
                case BOOLEAN: {
                    serializable = Boolean.valueOf(dataStream.readBoolean());

                    break;
                }
                case BYTE: {
                    serializable = Byte.valueOf(dataStream.readByte());

                    break;
                }
                case BYTE_ARRAY: {
                    serializable = _readByteArray(dataStream);

                    break;
                }
                case CHARACTER: {
                    serializable = Character.valueOf(dataStream.readChar());

                    break;
                }
                case TUPLE: {
                    serializable = _readCollection(dataStream, coder);

                    break;
                }
                case COMPLEX: {
                    final double real = dataStream.readDouble();
                    final double imaginary = dataStream.readDouble();

                    serializable = Complex.cartesian(real, imaginary);

                    break;
                }
                case DOUBLE: {
                    serializable = Double.valueOf(dataStream.readDouble());

                    break;
                }
                case FLOAT: {
                    serializable = Float.valueOf(dataStream.readFloat());

                    break;
                }
                case INTEGER: {
                    serializable = Integer.valueOf(dataStream.readInt());

                    break;
                }
                case LONG: {
                    serializable = Long.valueOf(dataStream.readLong());

                    break;
                }
                case DICT: {
                    serializable = _readMap(dataStream, coder);

                    break;
                }
                case RATIONAL: {
                    final long numerator = dataStream.readLong();
                    final long denominator = dataStream.readLong();

                    serializable = Rational.valueOf(numerator, denominator);

                    break;
                }
                case SHORT: {
                    serializable = Short.valueOf(dataStream.readShort());

                    break;
                }
                case STATE: {
                    serializable = State
                        .fromString(_readString(dataStream, coder));

                    break;
                }
                case STRING: {
                    serializable = _readString(dataStream, coder);

                    break;
                }
                default: {
                    throw new ClassCastException(
                        "Unrecognized type code '" + typeCode + "'");
                }
            }

            dataStream.close();
        }

        return serializable;
    }

    private static byte[] _readByteArray(
            final DataInputStream input)
        throws IOException
    {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        for (;;) {
            final int length = input.readShort() & 0xFFFF;

            if (length == 0) {
                break;
            }

            final byte[] bytes = new byte[length];

            input.readFully(bytes);
            byteStream.write(bytes, 0, bytes.length);
        }

        return byteStream.toByteArray();
    }

    private static Tuple _readCollection(
            final DataInputStream input,
            final Coder coder)
        throws IOException, ClassNotFoundException
    {
        final int size = input.readInt();
        final Tuple tuple = new Tuple(size);

        for (int i = 0; i < size; ++i) {
            tuple.add(_read(input, coder));
        }

        tuple.freeze();

        return tuple;
    }

    private static Dict _readMap(
            final DataInputStream input,
            final Coder coder)
        throws IOException, ClassNotFoundException
    {
        final int size = input.readInt();
        final Dict dict = new Dict(KeyedValues.hashCapacity(size));

        for (int i = 0; i < size; ++i) {
            final String key = _readString(input, coder);

            dict.put(key, _read(input, coder));
        }

        dict.freeze();

        return dict;
    }

    private static String _readString(
            final DataInputStream input,
            final Coder coder)
        throws IOException
    {
        String string;

        if (coder == null) {
            final StringBuilder stringBuilder = new StringBuilder();

            for (;;) {
                string = input.readUTF();

                if (string.isEmpty()) {
                    break;
                }

                stringBuilder.append(string);
            }

            string = stringBuilder.toString();
        } else {
            string = coder.decode(_readByteArray(input));
        }

        return string;
    }

    private static void _write(
            final Serializable serializable,
            final OutputStream stream,
            final Coder coder)
        throws IOException
    {
        final ValueType type = ValueType.get(serializable);

        stream.write(type.getCode());

        if (type == ValueType.OBJECT) {
            final ObjectOutputStream objectStream = new ObjectOutputStream(
                stream);

            objectStream.writeObject(serializable);
            objectStream.close();
        } else {
            final DataOutputStream dataStream = new DataOutputStream(stream);

            switch (type) {
                case BIG_DECIMAL: {
                    dataStream.writeInt(((BigDecimal) serializable).scale());
                    _writeByteArray(
                        ((BigDecimal) serializable)
                            .unscaledValue()
                            .toByteArray(),
                        dataStream);

                    break;
                }
                case BIG_INTEGER: {
                    _writeByteArray(
                        ((BigInteger) serializable).toByteArray(),
                        dataStream);

                    break;
                }
                case BIG_RATIONAL: {
                    _writeByteArray(
                        ((BigRational) serializable)
                            .getNumerator()
                            .toByteArray(),
                        dataStream);
                    _writeByteArray(
                        ((BigRational) serializable)
                            .getDenominator()
                            .toByteArray(),
                        dataStream);

                    break;
                }
                case BOOLEAN: {
                    dataStream
                        .writeBoolean(((Boolean) serializable).booleanValue());

                    break;
                }
                case BYTE: {
                    dataStream.writeByte(((Byte) serializable).byteValue());

                    break;
                }
                case BYTE_ARRAY: {
                    _writeByteArray((byte[]) serializable, dataStream);

                    break;
                }
                case CHARACTER: {
                    dataStream
                        .writeChar(((Character) serializable).charValue());

                    break;
                }
                case TUPLE: {
                    _writeCollection(
                        (Collection<?>) serializable,
                        dataStream,
                        coder);

                    break;
                }
                case COMPLEX: {
                    dataStream.writeDouble(((Complex) serializable).real());
                    dataStream
                        .writeDouble(((Complex) serializable).imaginary());

                    break;
                }
                case DOUBLE: {
                    dataStream
                        .writeDouble(((Double) serializable).doubleValue());

                    break;
                }
                case INTEGER: {
                    dataStream.writeInt(((Integer) serializable).intValue());

                    break;
                }
                case FLOAT: {
                    dataStream.writeFloat(((Float) serializable).floatValue());

                    break;
                }
                case LONG: {
                    dataStream.writeLong(((Long) serializable).longValue());

                    break;
                }
                case DICT: {
                    _writeMap((Map<?, ?>) serializable, dataStream, coder);

                    break;
                }
                case RATIONAL: {
                    dataStream
                        .writeLong(((Rational) serializable).getNumerator());
                    dataStream
                        .writeLong(((Rational) serializable).getDenominator());

                    break;
                }
                case SHORT: {
                    dataStream.writeShort(((Short) serializable).shortValue());

                    break;
                }
                case STATE:
                case STRING: {
                    _writeString(serializable.toString(), dataStream, coder);

                    break;
                }
                default: {
                    Require.failure();
                }
            }

            dataStream.close();
        }
    }

    private static void _writeByteArray(
            final byte[] bytes,
            final DataOutputStream output)
        throws IOException
    {
        int begin = 0;
        int end;

        for (;;) {
            end = begin + _MAX_BYTES_BLOCK;

            if (end > bytes.length) {
                end = bytes.length;
            }

            final int length = end - begin;

            output.writeShort(length);

            if (begin == end) {
                break;
            }

            output.write(bytes, begin, length);

            begin = end;
        }
    }

    private static void _writeCollection(
            final Collection<?> collection,
            final DataOutputStream output,
            final Coder coder)
        throws IOException
    {
        final Iterator<?> iterator = collection.iterator();
        final int size = collection.size();
        int written = 0;

        output.writeInt(size);

        while (iterator.hasNext()) {
            _write((Serializable) iterator.next(), output, coder);
            ++written;
        }

        Require.success(written == size);
    }

    private static void _writeMap(
            final Map<?, ?> map,
            final DataOutputStream output,
            final Coder coder)
        throws IOException
    {
        final Iterator<?> iterator = map.entrySet().iterator();
        final int size = map.size();
        int written = 0;

        output.writeInt(size);

        while (iterator.hasNext()) {
            final Map.Entry<?, ?> entry = (Map.Entry<?, ?>) iterator.next();

            _writeString(String.valueOf(entry.getKey()), output, coder);
            _write((Serializable) entry.getValue(), output, coder);
            ++written;
        }

        Require.success(written == size);
    }

    private static void _writeString(
            final String string,
            final DataOutputStream output,
            final Coder coder)
        throws IOException
    {
        if (coder == null) {
            int begin = 0;
            int end;

            for (;;) {
                end = begin + _MAX_UTF_CHARS;

                if (end > string.length()) {
                    end = string.length();
                }

                output.writeUTF(string.substring(begin, end));

                if (begin == end) {
                    break;
                }

                begin = end;
            }
        } else {
            _writeByteArray(Require.notNull(coder.encode(string)), output);
        }
    }

    static final Map<Class<?>, ValueType> _CLASS_TYPE_MAP =
        new IdentityHashMap<Class<?>, ValueType>();
    static final Map<Byte, ValueType> _CODE_TYPE_MAP = new HashMap<Byte,
        ValueType>();
    private static final int _MAX_BYTES_BLOCK = 65534;
    private static final int _MAX_UTF_CHARS = 65535 / 3;

    /**
     * Value type.
     */
    public enum ValueType
    {
        BIG_DECIMAL('D', BigDecimal.class),
        BIG_INTEGER('I', BigInteger.class),
        BIG_RATIONAL('R', BigRational.class),
        BOOLEAN('z', Boolean.class),
        BYTE('b', Byte.class),
        BYTE_ARRAY('a', byte[].class),
        CHARACTER('c', Character.class),
        COMPLEX('x', Complex.class),
        DICT('m', Dict.class),
        DOUBLE('d', Double.class),
        FLOAT('f', Float.class),
        INTEGER('i', Integer.class),
        LONG('j', Long.class),
        NULL('0', null),
        OBJECT('o', null),
        RATIONAL('r', Rational.class),
        SHORT('s', Short.class),
        STATE('q', State.class),
        STRING('t', String.class),
        TUPLE('n', Tuple.class);

        /**
         * Constructs an instance.
         *
         * @param code The type code.
         * @param typeClass The type class.
         */
        ValueType(final char code, @Nullable final Class<?> typeClass)
        {
            ValueType previous;

            _code = (byte) code;
            previous = _CODE_TYPE_MAP.put(Byte.valueOf(_code), this);
            Require.success(previous == null);

            if (typeClass != null) {
                previous = _CLASS_TYPE_MAP.put(typeClass, this);
                Require.success(previous == null);
            }
        }

        /**
         * Returns a string of value type codes for a value type array.
         *
         * @param valueTypes The value type array.
         *
         * @return The string of value type codes.
         */
        @Nonnull
        @CheckReturnValue
        public static String arrayToString(
                @Nonnull final ValueType[] valueTypes)
        {
            final StringBuilder stringBuilder = new StringBuilder();

            for (final ValueType valueType: valueTypes) {
                stringBuilder.append((char) valueType.getCode());
            }

            return stringBuilder.toString();
        }

        /**
         * Gets a value type from a code.
         *
         * @param code The code.
         *
         * @return The value type.
         */
        @Nonnull
        @CheckReturnValue
        public static ValueType get(final byte code)
        {
            final ValueType type = _CODE_TYPE_MAP.get(Byte.valueOf(code));

            return (type != null)? type: NULL;
        }

        /**
         * Gets a value type for an object.
         *
         * @param object The object.
         *
         * @return The value type.
         */
        @Nonnull
        @CheckReturnValue
        public static ValueType get(@Nonnull final Object object)
        {
            final ValueType type = _CLASS_TYPE_MAP.get(object.getClass());

            return (type != null)? type: ValueType.OBJECT;
        }

        /**
         * Returns a string of value type codes for a value type set.
         *
         * @param valueTypes The value type set.
         *
         * @return The string of value type codes.
         */
        @Nonnull
        @CheckReturnValue
        public static String setToString(
                @Nonnull final EnumSet<ValueType> valueTypes)
        {
            final StringBuilder stringBuilder = new StringBuilder();

            for (final ValueType valueType: valueTypes) {
                stringBuilder.append((char) valueType.getCode());
            }

            return stringBuilder.toString();
        }

        /**
         * Returns a value type set for a string of value type codes.
         *
         * @param valueTypeCodes The string of value type codes.
         *
         * @return The value type set.
         */
        @Nonnull
        @CheckReturnValue
        public static EnumSet<ValueType> stringToSet(
                @Nonnull final String valueTypeCodes)
        {
            final EnumSet<ValueType> valueTypeSet = EnumSet
                .noneOf(ValueType.class);

            for (final char code: valueTypeCodes.toCharArray()) {
                final ValueType valueType = get((byte) code);

                if (valueType != null) {
                    valueTypeSet.add(valueType);
                }
            }

            return valueTypeSet;
        }

        /**
         * Gets the code.
         *
         * @return The code.
         */
        @CheckReturnValue
        public byte getCode()
        {
            return _code;
        }

        private final byte _code;
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
