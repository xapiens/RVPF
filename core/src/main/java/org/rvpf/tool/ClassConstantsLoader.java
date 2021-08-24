/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ClassConstantsLoader.java 3885 2019-02-05 20:22:42Z SFB $
 */

package org.rvpf.tool;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;

/**
 * Class constants loader.
 */
public final class ClassConstantsLoader
{
    /**
     * Constructs an instance.
     *
     * @param classFile A class file.
     *
     * @throws IOException From file access.
     */
    public ClassConstantsLoader(
            @Nonnull final File classFile)
        throws IOException
    {
        this(new FileInputStream(classFile));
    }

    /**
     * Constructs an instance.
     *
     * @param classInputStream A class input stream.
     *
     * @throws IOException From stream access.
     */
    public ClassConstantsLoader(
            @Nonnull final InputStream classInputStream)
        throws IOException
    {
        final DataInputStream input = new DataInputStream(
            new BufferedInputStream(classInputStream));
        final int magic = input.readInt();

        if (magic != _MAGIC) {
            input.close();

            throw new ClassFormatError(
                Message.format(
                    ToolsMessages.BAD_MAGIC,
                    "0x" + Integer.toHexString(
                        magic).toUpperCase(Locale.ROOT)));
        }

        input.readShort();    // Minor version.
        input.readShort();    // Major version.

        _constants = new Object[input.readUnsignedShort()];
        _tags = new byte[_constants.length];

        for (int i = 1; i < _constants.length; ++i) {
            _tags[i] = input.readByte();

            switch (_tags[i]) {
                case _CONSTANT_UTF8: {
                    _constants[i] = input.readUTF();

                    break;
                }
                case _CONSTANT_INTEGER: {
                    _constants[i] = Integer.valueOf(input.readInt());

                    break;
                }
                case _CONSTANT_FLOAT: {
                    _constants[i] = new Float(input.readFloat());

                    break;
                }
                case _CONSTANT_LONG: {
                    _constants[i++] = Long.valueOf(input.readLong());

                    break;
                }
                case _CONSTANT_DOUBLE: {
                    _constants[i++] = new Double(input.readDouble());

                    break;
                }
                case _CONSTANT_CLASS:
                case _CONSTANT_STRING: {
                    _constants[i] = Integer.valueOf(input.readUnsignedShort());

                    break;
                }
                case _CONSTANT_FIELDREF:
                case _CONSTANT_METHODREF:
                case _CONSTANT_INTERFACE_METHODREF:
                case _CONSTANT_NAME_AND_TYPE:
                case _CONSTANT_INVOKE_DYNAMIC: {
                    _constants[i] = new int[] {input.readUnsignedShort(),
                            input.readUnsignedShort(), };

                    break;
                }
                case _CONSTANT_METHOD_HANDLE: {
                    _constants[i] = new int[] {input.readByte(),
                            input.readUnsignedShort(), };

                    break;
                }
                case _CONSTANT_METHOD_TYPE: {
                    _constants[i] = Integer.valueOf(input.readShort());

                    break;
                }
                default: {
                    throw new ClassFormatError(
                        Message.format(
                            ToolsMessages.INVALID_CONSTANT_TYPE,
                            Byte.valueOf(_tags[i])));
                }
            }
        }

        input.readUnsignedShort();    // Access flags.
        _classIndex = input.readUnsignedShort();

        input.close();
    }

    /**
     * Main entry.
     *
     * @param args Standard main arguments.
     */
    public static void main(@Nonnull final String[] args)
    {
        if (args.length != 1) {
            _LOGGER.error(ToolsMessages.SINGLE_CLASS);
            System.exit(1);
        }

        final String classArg = args[0].trim();
        final ClassLoader classLoader = Thread
            .currentThread()
            .getContextClassLoader();
        final InputStream classInputStream = classLoader
            .getResourceAsStream(classArg.replace('.', '/') + ".class");

        if (classInputStream == null) {
            _LOGGER.error(ToolsMessages.CLASS_NOT_FOUND, classArg);
            System.exit(2);

            return;    // Quells warnings.
        }

        final ClassConstantsLoader loader;

        try {
            loader = new ClassConstantsLoader(classInputStream);
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        _LOGGER.info(ToolsMessages.CLASS, loader.getClassName());

        for (final String className: loader.getClassNames()) {
            _LOGGER.info(ToolsMessages.CLASS_REFERENCE, className);

            for (final String fieldName: loader.getFieldNames(className)) {
                _LOGGER.info(ToolsMessages.CLASS_FIELD, className, fieldName);
            }
        }
    }

    /**
     * Gets the class name.
     *
     * @return The class name.
     */
    @Nonnull
    @CheckReturnValue
    public String getClassName()
    {
        return _className(_classIndex);
    }

    /**
     * Gets the class names.
     *
     * @return The class names.
     */
    @Nonnull
    @CheckReturnValue
    public Set<String> getClassNames()
    {
        if (_classes == null) {
            _classes = new HashMap<>();

            for (int i = 1; i < _tags.length; ++i) {
                if (_tags[i] == _CONSTANT_CLASS) {
                    _classes.put(_className(i), Integer.valueOf(i));
                }
            }
        }

        return _classes.keySet();
    }

    /**
     * Gets the field names for a class name.
     *
     * <p>The class name must have been supplied by a call to
     * {@link #getClassNames()} on this instance.</p>
     *
     * @param className The class name.
     *
     * @return The field names.
     */
    @Nonnull
    @CheckReturnValue
    public List<String> getFieldNames(@Nonnull final String className)
    {
        final int classIndex = _classes.get(className).intValue();
        final List<String> fields = new LinkedList<>();

        for (int i = 1; i < _tags.length; ++i) {
            if (_tags[i] == _CONSTANT_FIELDREF) {
                final int[] fieldRef = (int[]) _constants[i];

                if (fieldRef[0] == classIndex) {
                    final int[] nameAndType = (int[]) _constants[fieldRef[1]];

                    fields.add((String) _constants[nameAndType[0]]);
                }
            }
        }

        return fields;
    }

    private String _className(final int constantIndex)
    {
        final String className =
            (String) _constants[((Integer) _constants[constantIndex]).intValue()];

        return className.replace('/', '.');
    }

    private static final byte _CONSTANT_CLASS = (byte) 7;
    private static final byte _CONSTANT_DOUBLE = (byte) 6;
    private static final byte _CONSTANT_FIELDREF = (byte) 9;
    private static final byte _CONSTANT_FLOAT = (byte) 4;
    private static final byte _CONSTANT_INTEGER = (byte) 3;
    private static final byte _CONSTANT_INTERFACE_METHODREF = (byte) 11;
    private static final byte _CONSTANT_INVOKE_DYNAMIC = (byte) 18;
    private static final byte _CONSTANT_LONG = (byte) 5;
    private static final byte _CONSTANT_METHODREF = (byte) 10;
    private static final byte _CONSTANT_METHOD_HANDLE = (byte) 15;
    private static final byte _CONSTANT_METHOD_TYPE = (byte) 16;
    private static final byte _CONSTANT_NAME_AND_TYPE = (byte) 12;
    private static final byte _CONSTANT_STRING = (byte) 8;
    private static final byte _CONSTANT_UTF8 = (byte) 1;
    private static final int _MAGIC = 0xCAFEBABE;
    private static final Logger _LOGGER = Logger
        .getInstance(ClassConstantsLoader.class);

    private final int _classIndex;
    private Map<String, Integer> _classes;
    private final Object[] _constants;
    private final byte[] _tags;
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
