/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: VersionedValue.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.base.value;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.DateTime;
import org.rvpf.base.UUID;
import org.rvpf.base.tool.Require;

/**
 * Versioned value.
 *
 * <p>This class extends {@link PointValue} by adding a version for the value:
 * the time stamp in {@link DateTime} format of the last modification time
 * (including creation). This is the representation used by TheStore to allow
 * "pull" requests.</p>
 */
@NotThreadSafe
public class VersionedValue
    extends PointValue
{
    /**
     * Constructs an instance.
     *
     * <p>This is needed for an externalizable implementation.</p>
     */
    public VersionedValue() {}

    /**
     * Constructs an instance from a point value.
     *
     * @param pointValue The original point value.
     */
    public VersionedValue(final PointValue pointValue)
    {
        super(pointValue);

        _version = newVersion();
    }

    /**
     * Constructs an instance.
     *
     * @param uuid The UUID of the Point definition.
     * @param stamp The optional time stamp of the value.
     * @param version The version of the value.
     * @param state The state.
     * @param value The value.
     */
    public VersionedValue(
            @Nonnull final UUID uuid,
            @Nonnull final Optional<DateTime> stamp,
            @Nonnull final DateTime version,
            @Nullable final Serializable state,
            @Nullable final Serializable value)
    {
        super(uuid, stamp, state, value);

        _version = Require.notNull(version);
    }

    /**
     * Constructs an instance.
     *
     * @param point A string identifying the point.
     * @param stamp The optional time stamp of the value.
     */
    protected VersionedValue(
            @Nonnull final String point,
            @Nonnull final Optional<DateTime> stamp)
    {
        super(point, stamp, null, null);

        _version = newVersion();
    }

    /**
     * Constructs an instance.
     *
     * @param uuid The UUID of the Point definition.
     * @param stamp The optional time stamp of the value.
     */
    protected VersionedValue(
            @Nonnull final UUID uuid,
            @Nonnull final Optional<DateTime> stamp)
    {
        super(uuid, stamp, null, null);

        _version = newVersion();
    }

    /**
     * Returns a new version stamp.
     *
     * @return The new version stamp.
     */
    @Nonnull
    @CheckReturnValue
    public static DateTime newVersion()
    {
        final DateTime now = DateTime.now();
        long version = now.toRaw();

        for (;;) {
            final long reference = _reference.get();

            if (version <= reference) {
                version = reference + 10;
            }

            if (_reference.compareAndSet(reference, version)) {
                break;
            }
        }

        return (version == now.toRaw())? now: DateTime.fromRaw(version);
    }

    /** {@inheritDoc}
     */
    @Override
    public VersionedValue copy()
    {
        return new VersionedValue(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        return super.equals(other);
    }

    /** {@inheritDoc}
     */
    @Override
    public VersionedValue frozen()
    {
        final VersionedValue frozen;

        if (isFrozen()) {
            frozen = this;
        } else {
            frozen = copy();
            frozen.freeze();
        }

        return frozen;
    }

    /** {@inheritDoc}
     */
    @Override
    public DateTime getVersion()
    {
        return _version;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    /**
     * Reads in an external representation of this.
     *
     * @param input The external representation.
     *
     * @throws IOException When an I/O error occurs.
     */
    @Override
    public void readExternal(final ObjectInput input)
        throws IOException
    {
        super.readExternal(input);

        final DateTime version = DateTime.readExternal(input).get();

        _version = (version != null)? version: newVersion();
    }

    /** {@inheritDoc}
     */
    @Override
    public void readMap(final Map<String, Serializable> map)
    {
        final String versionString;

        super.readMap(map);

        versionString = (String) map.get(VERSION_FIELD);

        _version = DateTime
            .fromString(Optional.ofNullable(versionString))
            .orElse(newVersion());
    }

    /** {@inheritDoc}
     */
    @Override
    public VersionedValue thawed()
    {
        return isFrozen()? copy(): this;
    }

    /**
     * Writes out an external representation of this.
     *
     * @param output The external representation.
     *
     * @throws IOException When an I/O error occurs.
     */
    @Override
    public void writeExternal(final ObjectOutput output)
        throws IOException
    {
        super.writeExternal(output);

        DateTime.writeExternal(Optional.ofNullable(_version), output);
    }

    /** {@inheritDoc}
     */
    @Override
    public void writeMap(final Map<String, Serializable> map)
    {
        map.put(SIMPLE_STRING_MODE, null);
        map.put(VERSION_FIELD, _version.toString());

        super.writeMap(map);
    }

    /** {@inheritDoc}
     */
    @Override
    protected String getStampString(final DateTime.Context dateTimeContext)
    {
        return super
            .getStampString(
                dateTimeContext) + "/" + dateTimeContext.toString(_version);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void setVersion(final DateTime version)
    {
        _version = version;
    }

    /** Version field key. */
    public static final String VERSION_FIELD = "version";
    private static final long serialVersionUID = 1L;
    private static final AtomicLong _reference = new AtomicLong();

    /**
     * Version of the value.
     *
     * @serial
     */
    private DateTime _version;

    /**
     * Deleted.
     */
    @NotThreadSafe
    public static final class Deleted
        extends VersionedValue
    {
        /**
         * Constructs an instance.
         *
         * <p>This is needed for an Externalizable implementation.</p>
         */
        public Deleted() {}

        /**
         * Constructs an instance.
         *
         * @param pointValue The original point value.
         */
        public Deleted(@Nonnull final PointValue pointValue)
        {
            super(pointValue);
        }

        /**
         * Constructs an instance.
         *
         * @param point A string identifying the point.
         * @param stamp The optional time stamp of the value.
         */
        public Deleted(
                @Nonnull final String point,
                @Nonnull final Optional<DateTime> stamp)
        {
            super(point, stamp);
        }

        /**
         * Constructs an instance.
         *
         * @param uuid The UUID of the point definition.
         * @param stamp The optional time stamp of the value.
         */
        public Deleted(
                @Nonnull final UUID uuid,
                @Nonnull final Optional<DateTime> stamp)
        {
            super(uuid, stamp);
        }

        /**
         * Constructs an instance.
         *
         * @param uuid The UUID of the point definition.
         * @param stamp The optional time stamp of the value.
         * @param version The version of the value.
         */
        Deleted(
                @Nonnull final UUID uuid,
                @Nonnull final Optional<DateTime> stamp,
                @Nonnull final DateTime version)
        {
            super(uuid, stamp, version, null, null);
        }

        /** {@inheritDoc}
         */
        @Override
        public Deleted copy()
        {
            return new Deleted(this);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isDeleted()
        {
            return true;
        }

        private static final long serialVersionUID = 1L;
    }


    /**
     * Factory.
     *
     * <p>This factory class should only be used to restore values from a
     * store.</p>
     */
    public static final class Factory
    {
        private Factory() {}

        /**
         * Restores a versioned value.
         *
         * @param uuid The UUID of the Point definition.
         * @param stamp The optional time stamp of the value.
         * @param version The version of the value.
         * @param state The state.
         * @param value The value.
         *
         * @return The restored versioned value.
         */
        @Nonnull
        @CheckReturnValue
        public static VersionedValue restore(
                @Nonnull final UUID uuid,
                @Nonnull final Optional<DateTime> stamp,
                @Nonnull final DateTime version,
                @Nullable final Serializable state,
                @Nullable final Serializable value)
        {
            final VersionedValue restoredValue = uuid
                .isDeleted()? new VersionedValue.Deleted(
                    uuid.undeleted(),
                    stamp,
                    version): new VersionedValue(
                        uuid,
                        stamp,
                        version,
                        state,
                        value);

            restoredValue.freeze();

            return restoredValue;
        }
    }


    /**
     * Purged.
     *
     * <p>Like {@link Deleted} but used to purge values.</p>
     */
    @NotThreadSafe
    public static final class Purged
        extends VersionedValue
    {
        /**
         * Constructs an instance.
         *
         * <p>This is needed for an externalizable implementation.</p>
         */
        public Purged() {}

        /**
         * Constructs an instance.
         *
         * @param pointValue The original point value.
         */
        public Purged(@Nonnull final PointValue pointValue)
        {
            super(pointValue);
        }

        /**
         * Constructs an instance.
         *
         * @param point A string identifying the point.
         * @param stamp The optional time stamp of the value.
         */
        public Purged(
                @Nonnull final String point,
                @Nonnull final Optional<DateTime> stamp)
        {
            super(point, stamp);
        }

        /**
         * Constructs an instance.
         *
         * @param uuid The UUID of the point definition.
         * @param stamp The optional time stamp of the value.
         */
        public Purged(
                @Nonnull final UUID uuid,
                @Nonnull final Optional<DateTime> stamp)
        {
            super(uuid, stamp);
        }

        /** {@inheritDoc}
         */
        @Override
        public Purged copy()
        {
            return new Purged(this);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isDeleted()
        {
            return true;
        }

        private static final long serialVersionUID = 1L;
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
