/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StampsSync.java 3982 2019-05-13 16:23:23Z SFB $
 */

package org.rvpf.base.sync;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.time.ZoneId;

import java.util.Arrays;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.DateTime;
import org.rvpf.base.Params;
import org.rvpf.base.TimeInterval;

/**
 * Stamps sync.
 *
 * <p>Instances of this class can be used to prepare store queries on arbitrary
 * time stamps.</p>
 */
@NotThreadSafe
public final class StampsSync
    extends Sync.Abstract
{
    /**
     * Constructs an instance.
     *
     * <p>Needed for dynamic instantiation.</p>
     */
    public StampsSync() {}

    /**
     * Constructs an instance.
     *
     * @param stamps The time stamps.
     */
    public StampsSync(final DateTime[] stamps)
    {
        this(stamps, DateTime.getZoneId());
    }

    /**
     * Constructs an instance.
     *
     * @param stampStrings Time stamp strings (ISO 8601).
     */
    public StampsSync(final String[] stampStrings)
    {
        this(stampStrings, DateTime.getZoneId());
    }

    /**
     * Constructs an instance.
     *
     * @param stamps The time stamps.
     * @param zoneId The zone id.
     */
    public StampsSync(
            @Nonnull final DateTime[] stamps,
            @Nonnull final ZoneId zoneId)
    {
        super(zoneId);

        _setStamps(stamps);
    }

    /**
     * Constructs an instance.
     *
     * @param stamps The time stamps (ISO 8601).
     * @param zoneId The zone id.
     */
    public StampsSync(
            @Nonnull final String[] stamps,
            @Nonnull final ZoneId zoneId)
    {
        super(zoneId);

        _setStamps(stamps);
    }

    private StampsSync(final StampsSync other)
    {
        super(other);

        _rawStamps = other._rawStamps;
    }

    /** {@inheritDoc}
     */
    @Override
    public Sync copy()
    {
        return new StampsSync(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof StampsSync)) {
            return false;
        }

        return Arrays.equals(_rawStamps, ((StampsSync) other)._rawStamps);
    }

    /** {@inheritDoc}
     */
    @Override
    public TimeInterval getDefaultLimits()
    {
        final TimeInterval defaultLimits;

        if (_rawStamps.length > 0) {
            final TimeInterval.Builder limitsBuilder = TimeInterval
                .newBuilder();

            limitsBuilder.setNotBefore(DateTime.fromRaw(_rawStamps[0]));
            limitsBuilder
                .setNotAfter(
                    DateTime.fromRaw(_rawStamps[_rawStamps.length - 1]));
            defaultLimits = limitsBuilder.build();
        } else {
            defaultLimits = TimeInterval.UNLIMITED;
        }

        return defaultLimits;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<DateTime> getNextStamp()
    {
        int index = Arrays.binarySearch(_rawStamps, getCurrentStamp().toRaw());

        index = (index >= 0)? (index + 1): (-index - 1);

        if (index >= _rawStamps.length) {
            return Optional.empty();
        }

        setCurrentStamp(DateTime.fromRaw(_rawStamps[index]), +1);

        return nextStamp();
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<DateTime> getPreviousStamp()
    {
        int index = Arrays.binarySearch(_rawStamps, getCurrentStamp().toRaw());

        if (index < 0) {
            index = -index - 1;
        }

        --index;

        if (index < 0) {
            return Optional.empty();
        }

        setCurrentStamp(DateTime.fromRaw(_rawStamps[index]), -1);

        return previousStamp();
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final long[] rawStamps = _rawStamps;

        return (rawStamps != null)? Arrays.hashCode(_rawStamps): 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isInSync()
    {
        return Arrays.binarySearch(_rawStamps, getCurrentStamp().toRaw()) >= 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public void readExternal(final ObjectInput input)
        throws IOException
    {
        super.readExternal(input);

        final long[] stamps = new long[input.readInt()];

        for (int i = 0; i < stamps.length; ++i) {
            stamps[i] = input.readLong();
        }

        _setStamps(stamps);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Params params)
    {
        if (!super.setUp(params)) {
            return false;
        }

        final String[] stamps = params.getStrings(STAMP_PARAM);

        if (stamps.length == 0) {
            getThisLogger().error(BaseMessages.MISSING_PARAMETER, STAMP_PARAM);

            return false;
        }

        try {
            _setStamps(stamps);
        } catch (final IllegalArgumentException exception) {
            getThisLogger()
                .error(BaseMessages.VERBATIM, exception.getMessage());

            return false;
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        _rawStamps = null;

        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return getClass().getSimpleName() + "(" + _rawStamps.length + ")";
    }

    /** {@inheritDoc}
     */
    @Override
    public void writeExternal(final ObjectOutput output)
        throws IOException
    {
        super.writeExternal(output);

        output.writeInt(_rawStamps.length);

        for (final long stamp: _rawStamps) {
            output.writeLong(stamp);
        }
    }

    private void _setStamps(final DateTime[] stamps)
    {
        final long[] raws = new long[stamps.length];

        for (int i = 0; i < raws.length; ++i) {
            raws[i] = stamps[i].toRaw();
        }

        _setStamps(raws);
    }

    private void _setStamps(final long[] rawStamps)
    {
        Arrays.sort(rawStamps);
        _rawStamps = rawStamps;

        freeze();
    }

    private void _setStamps(final String[] stampStrings)
    {
        final DateTime.Context dateTimeContext = new DateTime.Context(
            getZoneId());
        final DateTime[] stamps = new DateTime[stampStrings.length];

        for (int i = 0; i < stamps.length; ++i) {
            stamps[i] = dateTimeContext.fromString(stampStrings[i]);
        }

        _setStamps(stamps);
    }

    private static final long serialVersionUID = 1L;

    private long[] _rawStamps;
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
