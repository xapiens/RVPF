/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Converter.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.ext.bdb;

import java.io.IOException;
import java.io.Serializable;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.UUID;
import org.rvpf.base.tool.Coder;
import org.rvpf.base.tool.Externalizer;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.VersionedValue;

import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryKeyCreator;

/**
 * Converter.
 */
final class Converter
{
    /**
     * Constructs an instance.
     *
     * @param snapshot True if in snapshot mode.
     */
    Converter(final boolean snapshot)
    {
        _snapshot = snapshot;
    }

    /**
     * Gets the UUID from the database key.
     *
     * @param key The database key.
     *
     * @return The UUID.
     */
    @Nonnull
    @CheckReturnValue
    static UUID getUUID(@Nonnull final DatabaseEntry key)
    {
        final TupleInput input = new TupleInput(key.getData());
        final byte[] bytes = new byte[UUID.BYTES_LENGTH];
        final int read = input.readFast(bytes);

        Require.success(read == UUID.BYTES_LENGTH);

        final UUID uuid = UUID.fromBytes(bytes);

        try {
            input.close();
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        return uuid;
    }

    /**
     * Gets the database key for a point value.
     *
     * @param pointValue The point value.
     *
     * @return The database key.
     */
    @Nonnull
    @CheckReturnValue
    DatabaseEntry getData(@Nonnull final VersionedValue pointValue)
    {
        _output.reset();

        if (_snapshot) {
            _output.writeLong(pointValue.getStamp().toRaw());
        }

        _output.writeLong(pointValue.getVersion().toRaw());

        synchronized (_CODER) {
            _write(pointValue.getState());
            _write(pointValue.getValue());
        }

        return new DatabaseEntry(_output.toByteArray());
    }

    /**
     * Gets the stamp from the data.
     *
     * @param data The data.
     *
     * @return The stamp.
     */
    @Nonnull
    @CheckReturnValue
    DateTime getDataStamp(@Nonnull final DatabaseEntry data)
    {
        Require.success(_snapshot);

        final TupleInput input = new TupleInput(data.getData());
        final DateTime stamp = DateTime.fromRaw(input.readLong());

        try {
            input.close();
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        return stamp;
    }

    /**
     * Gets the database key for a point value.
     *
     * @param pointValue The point value.
     *
     * @return The database key.
     */
    @Nonnull
    @CheckReturnValue
    DatabaseEntry getKey(@Nonnull final VersionedValue pointValue)
    {
        final UUID uuid = pointValue.getPointUUID();

        _output.reset();
        _output
            .writeFast(
                (pointValue.isDeleted()? uuid.deleted(): uuid).toBytes());

        if (!_snapshot) {
            _output.writeLong(pointValue.getStamp().toRaw());
        }

        return new DatabaseEntry(_output.toByteArray());
    }

    /**
     * Gets the time stamp from a key.
     *
     * @param key The key.
     *
     * @return The time stamp.
     */
    @Nonnull
    @CheckReturnValue
    DateTime getKeyStamp(@Nonnull final DatabaseEntry key)
    {
        Require.failure(_snapshot);

        final TupleInput input = new TupleInput(key.getData());

        input.skipFast(UUID.BYTES_LENGTH);

        final DateTime stamp = DateTime.fromRaw(input.readLong());

        try {
            input.close();
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        return stamp;
    }

    /**
     * Gets the point value from the database key and data.
     *
     * @param key The database key.
     * @param data The database data.
     *
     * @return The point value.
     */
    @Nonnull
    @CheckReturnValue
    VersionedValue getPointValue(
            @Nonnull final DatabaseEntry key,
            @Nonnull final DatabaseEntry data)
    {
        TupleInput input;

        input = new TupleInput(key.getData());

        final byte[] bytes = new byte[UUID.BYTES_LENGTH];
        final int read = input.readFast(bytes);

        Require.success(read == UUID.BYTES_LENGTH);

        final UUID uuid = UUID.fromBytes(bytes);
        DateTime stamp = null;

        if (!_snapshot) {
            stamp = DateTime.fromRaw(input.readLong());
        }

        try {
            input.close();
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        input = new TupleInput(data.getData());

        if (_snapshot) {
            stamp = DateTime.fromRaw(input.readLong());
        }

        final DateTime version = DateTime.fromRaw(input.readLong());
        final Serializable state = _read(input);
        final Serializable value = _read(input);

        try {
            input.close();
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        return VersionedValue.Factory
            .restore(uuid, Optional.of(stamp), version, state, value);
    }

    /**
     * Gets the version from the data.
     *
     * @param data The data.
     *
     * @return The version.
     */
    @Nonnull
    @CheckReturnValue
    DateTime getVersion(@Nonnull final DatabaseEntry data)
    {
        Require.failure(_snapshot);

        final TupleInput input = new TupleInput(data.getData());
        final DateTime version = DateTime.fromRaw(input.readLong());

        try {
            input.close();
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        return version;
    }

    /**
     * Gets a version key creator.
     *
     * @return The version key creator.
     */
    @Nonnull
    @CheckReturnValue
    SecondaryKeyCreator getVersionKeyCreator()
    {
        Require.failure(_snapshot);

        return new SecondaryKeyCreator()
        {
            @Override
            public boolean createSecondaryKey(
                    final SecondaryDatabase database,
                    final DatabaseEntry key,
                    final DatabaseEntry data,
                    final DatabaseEntry result)
            {
                final byte[] bytes = new byte[LONG_BYTES_LENGTH];

                System
                    .arraycopy(data.getData(), 0, bytes, 0, LONG_BYTES_LENGTH);
                result.setData(bytes);

                return true;
            }
        };
    }

    /**
     * Asks if a database key has the same UUID as a reference.
     *
     * @param key The database key.
     * @param uuid The reference UUID.
     *
     * @return True if it has the same UUID.
     */
    @CheckReturnValue
    boolean hasSameUUID(
            @Nonnull final DatabaseEntry key,
            @Nonnull final UUID uuid)
    {
        Require.failure(_snapshot);

        final byte[] pointBytes = uuid.toBytes();
        final byte[] entryBytes = key.getData();

        for (int i = 0; i < UUID.BYTES_LENGTH; ++i) {
            if (pointBytes[i] != entryBytes[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Asks if the value from the data is null.
     *
     * @param data The data.
     *
     * @return True if the value is null.
     */
    @CheckReturnValue
    boolean isValueNull(@Nonnull final DatabaseEntry data)
    {
        final TupleInput input = new TupleInput(data.getData());

        if (!_snapshot) {
            input.skipFast(LONG_BYTES_LENGTH);    // Version.
            input.skipFast(input.readInt());    // State.
        }

        final boolean valueIsNull = input.readInt() == 0;

        try {
            input.close();
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        return valueIsNull;
    }

    /**
     * Returns a new database key.
     *
     * @param stamp A time stamp.
     *
     * @return The database key.
     */
    @Nonnull
    @CheckReturnValue
    DatabaseEntry newKey(@Nonnull final DateTime stamp)
    {
        Require.failure(_snapshot);

        final TupleOutput output = new TupleOutput(new byte[LONG_BYTES_LENGTH]);

        output.writeLong(stamp.toRaw());

        final DatabaseEntry key = new DatabaseEntry(output.getBufferBytes());

        try {
            output.close();
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        return key;
    }

    /**
     * Returns a new database key.
     *
     * @param uuid A UUID.
     *
     * @return The database key.
     */
    @Nonnull
    @CheckReturnValue
    DatabaseEntry newKey(@Nonnull final UUID uuid)
    {
        Require.success(_snapshot);

        final TupleOutput output = new TupleOutput(new byte[UUID.BYTES_LENGTH]);

        output.writeFast(uuid.toBytes());

        final DatabaseEntry key = new DatabaseEntry(output.getBufferBytes());

        try {
            output.close();
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        return key;
    }

    /**
     * Returns a new database key.
     *
     * @param uuid A UUID.
     * @param stamp A time stamp.
     *
     * @return The database key.
     */
    @Nonnull
    @CheckReturnValue
    DatabaseEntry newKey(
            @Nonnull final UUID uuid,
            @Nonnull final DateTime stamp)
    {
        Require.failure(_snapshot);

        final TupleOutput output = new TupleOutput(
            new byte[UUID.BYTES_LENGTH + LONG_BYTES_LENGTH]);

        output.writeFast(uuid.toBytes());
        output.writeLong(stamp.toRaw());

        final DatabaseEntry key = new DatabaseEntry(output.getBufferBytes());

        try {
            output.close();
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        return key;
    }

    private static Serializable _read(final TupleInput input)
    {
        final int length = input.readInt();
        final Serializable serializable;

        if (length > 0) {
            final byte[] bytes = new byte[length];
            final int read = input.readFast(bytes);

            Require.success(read == length);
            serializable = Externalizer.internalize(bytes, Optional.of(_CODER));
        } else {
            serializable = null;
        }

        return serializable;
    }

    private void _write(final Serializable serializable)
    {
        final byte[] bytes = Externalizer
            .externalize(serializable, Optional.of(_CODER));

        if (bytes == null) {
            _output.writeInt(0);
        } else {
            _output.writeInt(bytes.length);
            _output.writeFast(bytes);
        }
    }

    static final int LONG_BYTES_LENGTH = 8;
    private static final Coder _CODER = new Coder();

    private final TupleOutput _output = new TupleOutput();
    private final boolean _snapshot;
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
