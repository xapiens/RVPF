/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ElapsedSync.java 4043 2019-06-02 15:05:37Z SFB $
 */

package org.rvpf.base.sync;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.time.ZoneId;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Params;
import org.rvpf.base.TimeInterval;
import org.rvpf.base.logger.Message;

/**
 * Elapsed sync.
 *
 * <p>Instances of this class can be used to prepare store queries on a fixed
 * interval starting from an arbitrary time stamp.</p>
 */
@NotThreadSafe
public final class ElapsedSync
    extends Sync.Abstract
{
    /**
     * Constructs an instance.
     *
     * <p>Needed for dynamic instantiation.</p>
     */
    public ElapsedSync() {}

    /**
     * Constructs an instance.
     *
     * @param elapsed An elapsed time.
     * @param offset An optional time offset.
     */
    public ElapsedSync(
            @Nonnull final ElapsedTime elapsed,
            @Nonnull final Optional<ElapsedTime> offset)
    {
        this(DateTime.getZoneId(), elapsed, offset);
    }

    /**
     * Constructs an instance.
     *
     * @param elapsed An elapsed time representation.
     * @param offset An optional time offset representation.
     */
    public ElapsedSync(
            @Nonnull final String elapsed,
            @Nonnull final Optional<String> offset)
    {
        this(DateTime.getZoneId(), elapsed, offset);
    }

    /**
     * Constructs an instance.
     *
     * @param zoneId The zone id.
     * @param elapsed An elapsed time.
     * @param offset An optional time offset.
     */
    public ElapsedSync(
            @Nonnull final ZoneId zoneId,
            @Nonnull final ElapsedTime elapsed,
            @Nonnull final Optional<ElapsedTime> offset)
    {
        super(zoneId);

        _setElapsed(elapsed, offset.orElse(null));
    }

    /**
     * Constructs an instance.
     *
     * @param zoneId The zone id.
     * @param elapsed An elapsed time representation.
     * @param offset A time offset representation.
     */
    public ElapsedSync(
            @Nonnull final ZoneId zoneId,
            @Nonnull final String elapsed,
            @Nonnull final Optional<String> offset)
    {
        this(
            zoneId,
            ElapsedTime.fromString(elapsed),
            ElapsedTime.fromString(offset));
    }

    private ElapsedSync(final ElapsedSync other)
    {
        super(other);

        _elapsed = other._elapsed;
        _offset = other._offset;
        _trim = other._trim;
    }

    /** {@inheritDoc}
     */
    @Override
    public Sync copy()
    {
        return new ElapsedSync(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object)
    {
        if (!(object instanceof ElapsedSync)) {
            return false;
        }

        final ElapsedSync other = (ElapsedSync) object;

        if (!((_elapsed != null)? _elapsed
            .equals(other._elapsed): (other._elapsed == null))) {
            return false;
        }

        if (!((_offset != null)? _offset
            .equals(other._offset): (other._offset == null))) {
            return false;
        }

        if (!((_trim != null)? _trim
            .equals(other._trim): (other._trim == null))) {
            return false;
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<DateTime> getNextStamp()
    {
        return getNextStamp(1);
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<DateTime> getNextStamp(final int intervals)
    {
        if (_elapsed == null) {
            return Optional.empty();
        }

        final long elapsed = _elapsed.toRaw() * intervals;
        final DateTime current = getCurrentStamp();
        final DateTime nextStamp;

        if (current.isBefore(getLimits())) {
            nextStamp = _trim(getLimits().getBeginning(true));
        } else {
            nextStamp = _trim(_floor(current).after(elapsed));
        }

        setCurrentStamp(nextStamp, +1);

        return nextStamp();
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<DateTime> getPreviousStamp()
    {
        return getPreviousStamp(1);
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<DateTime> getPreviousStamp(final int intervals)
    {
        if (_elapsed == null) {
            return Optional.empty();
        }

        final long elapsed = _elapsed.toRaw() * intervals;
        final DateTime current = getCurrentStamp();
        final DateTime previousStamp;

        if (current.isAfter(getLimits())) {
            previousStamp = _trim(_floor(getLimits().getEnd(true)));
        } else {
            final DateTime floor = _floor(current);

            previousStamp = _trim(
                floor.equals(current)? floor.before(elapsed): floor);
        }

        setCurrentStamp(previousStamp, -1);

        return previousStamp();
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(_elapsed, _offset, _trim);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isInSync()
    {
        final DateTime current = getCurrentStamp();
        final TimeInterval limits = getLimits();

        if (current.isBefore(limits) || current.isAfter(limits)) {
            return false;
        }

        return _floor(current).equals(current);
    }

    /** {@inheritDoc}
     */
    @Override
    public void readExternal(final ObjectInput input)
        throws IOException
    {
        super.readExternal(input);

        _elapsed = ElapsedTime.readExternal(input).orElse(null);
        _offset = ElapsedTime.readExternal(input).orElse(null);
        _trim = ElapsedTime.readExternal(input).orElse(null);
    }

    /** {@inheritDoc}
     */
    @Override
    public void setLimits(TimeInterval limits)
    {
        if (_trim != null) {
            final TimeInterval.Builder limitsBuilder = TimeInterval
                .newBuilder()
                .copyFrom(limits);
            DateTime beginning = limits.getBeginning(true);

            if (!beginning.isBeginningOfTime()) {
                beginning = _trim(beginning.after(_trim.toRaw() - 1));

                if (beginning.isBefore(limits)) {
                    beginning = beginning.after(_trim);
                }

                limitsBuilder.setNotBefore(beginning);
            }

            final DateTime end = limits.getEnd(true);

            if (!end.isEndOfTime()) {
                limitsBuilder.setNotAfter(_trim(end));
            }

            limits = limitsBuilder.build();
        }

        super.setLimits(limits);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Params params)
    {
        if (!super.setUp(params)) {
            return false;
        }

        final Optional<ElapsedTime> elapsed = params
            .getElapsed(ELAPSED_PARAM, Optional.empty(), Optional.empty());
        final Optional<ElapsedTime> offset = params
            .getElapsed(OFFSET_PARAM, Optional.empty(), Optional.empty());

        if (!elapsed.isPresent()) {
            getThisLogger()
                .error(BaseMessages.MISSING_PARAMETER, ELAPSED_PARAM);

            return false;
        }

        try {
            _setElapsed(elapsed.get(), offset.orElse(null));
        } catch (final IllegalArgumentException exception) {
            getThisLogger()
                .error(BaseMessages.VERBATIM, exception.getMessage());

            return false;
        }

        _trim = (_offset != null)? _offset: _elapsed;
        super.setLimits(getDefaultLimits());

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        _elapsed = null;
        _trim = null;

        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return getClass()
            .getSimpleName() + "(" + _elapsed
                + ((_offset != null)? (", " + _offset): "") + ")";
    }

    /** {@inheritDoc}
     */
    @Override
    public void writeExternal(final ObjectOutput output)
        throws IOException
    {
        super.writeExternal(output);

        ElapsedTime.writeExternal(Optional.ofNullable(_elapsed), output);
        ElapsedTime.writeExternal(Optional.ofNullable(_offset), output);
        ElapsedTime.writeExternal(Optional.ofNullable(_trim), output);
    }

    private DateTime _floor(DateTime current)
    {
        DateTime floor;

        if (getLimits().getBeginning(true).isBeginningOfTime()) {
            if (getLimits().getEnd(true).isEndOfTime()) {
                if (_offset != null) {
                    current = current.before(_offset);
                }

                floor = current.floored(_elapsed);

                if (_offset != null) {
                    floor = floor.after(_offset);
                }
            } else {
                final long offset = getLimits()
                    .getEnd(true)
                    .sub(current)
                    .toRaw() % _elapsed.toRaw();

                floor = (offset != 0)? current
                    .before(_elapsed.toRaw() - offset): current;
            }
        } else {
            final long offset = current
                .sub(getLimits().getBeginning(true))
                .toRaw() % _elapsed.toRaw();

            floor = current.before(offset);
        }

        return floor;
    }

    private void _setElapsed(
            final ElapsedTime elapsed,
            final ElapsedTime offset)
    {
        if (elapsed.toMillis() < 1) {
            throw new IllegalArgumentException(
                Message.format(BaseMessages.ELAPSED_TOO_SMALL, elapsed));
        }

        if ((offset != null) && (offset.compareTo(elapsed) >= 0)) {
            throw new IllegalArgumentException(
                Message.format(BaseMessages.OFFSET_TOO_LARGE, offset, elapsed));
        }

        _elapsed = elapsed;
        _offset = offset;
    }

    private DateTime _trim(final DateTime stamp)
    {
        return (_trim != null)? stamp.floored(_trim): stamp;
    }

    private static final long serialVersionUID = 1L;

    private ElapsedTime _elapsed;
    private ElapsedTime _offset;
    private ElapsedTime _trim;
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
