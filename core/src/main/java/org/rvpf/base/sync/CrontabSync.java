/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CrontabSync.java 4055 2019-06-04 13:05:05Z SFB $
 */

package org.rvpf.base.sync;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.time.ZoneId;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.DateTime;
import org.rvpf.base.Params;

/**
 * Crontab sync.
 */
@NotThreadSafe
public final class CrontabSync
    extends Sync.Abstract
{
    /**
     * Constructs an instance.
     *
     * <p>Needed for dynamic instantiation.</p>
     */
    public CrontabSync() {}

    /**
     * Constructs an instance.
     *
     * @param entry A 'crontab' entry.
     */
    public CrontabSync(@Nonnull final String entry)
    {
        this(entry, DateTime.getZoneId());
    }

    /**
     * Constructs an instance.
     *
     * @param entry An entry in crontab format.
     * @param zoneId The zone id.
     */
    public CrontabSync(
            @Nonnull final String entry,
            @Nonnull final ZoneId zoneId)
    {
        super(zoneId);

        try {
            _setCrontab(Crontab.parse(entry));
        } catch (final Crontab.BadItemException exception) {
            throw new IllegalArgumentException(exception);
        }
    }

    private CrontabSync(final CrontabSync other)
    {
        super(other);

        _crontab = other._crontab;
    }

    /** {@inheritDoc}
     */
    @Override
    public Sync copy()
    {
        return new CrontabSync(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object)
    {
        if (!(object instanceof CrontabSync)) {
            return false;
        }

        return (_crontab != null)? _crontab
            .equals(
                ((CrontabSync) object)._crontab): (((CrontabSync) object)._crontab
                == null);
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<DateTime> getNextStamp()
    {
        if (_crontab == null) {
            return Optional.empty();
        }

        setCurrentStamp(
            _crontab.change(getCurrentStamp(), getZoneId(), true),
            +1);

        return nextStamp();
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<DateTime> getPreviousStamp()
    {
        if (_crontab == null) {
            return Optional.empty();
        }

        setCurrentStamp(
            _crontab.change(getCurrentStamp(), getZoneId(), false),
            -1);

        return previousStamp();
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isInSync()
    {
        return (_crontab != null)? _crontab
            .isInSchedule(getCurrentStamp(), getZoneId()): false;
    }

    /** {@inheritDoc}
     */
    @Override
    public void readExternal(final ObjectInput input)
        throws IOException
    {
        super.readExternal(input);

        _setCrontab(Crontab.readExternal(input));
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Params params)
    {
        if (!super.setUp(params)) {
            return false;
        }

        final Optional<String> entry = params.getString(CRONTAB_PARAM);

        if (!entry.isPresent()) {
            getThisLogger()
                .error(BaseMessages.MISSING_PARAMETER, CRONTAB_PARAM);

            return false;
        }

        return setUp(entry.get());
    }

    /**
     * Sets up this.
     *
     * @param entry The 'crontab' entry.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public boolean setUp(final String entry)
    {
        try {
            _setCrontab(Crontab.parse(entry));
        } catch (final Crontab.BadItemException exception) {
            getThisLogger().error(BaseMessages.BAD_CRONTAB_ENTRY, entry);

            return false;
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        _crontab = null;

        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return getClass().getSimpleName() + "(" + _crontab.getEntry() + ")";
    }

    /** {@inheritDoc}
     */
    @Override
    public void writeExternal(final ObjectOutput output)
        throws IOException
    {
        super.writeExternal(output);

        Crontab.writeExternal(_crontab, output);
    }

    private void _setCrontab(final Crontab crontab)
    {
        _crontab = crontab;

        freeze();
    }

    private static final long serialVersionUID = 1L;

    private Crontab _crontab;
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
