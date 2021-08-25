/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ValveStats.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.valve;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.Stats;
import org.rvpf.base.StatsOwner;
import org.rvpf.service.ServiceStats;

/**
 * Valve Stats.
 */
@ThreadSafe
public final class ValveStats
    extends ServiceStats
{
    /**
     * Constructs an instance.
     *
     * @param statsOwner The stats owner.
     */
    ValveStats(@Nonnull final StatsOwner statsOwner)
    {
        super(statsOwner);
    }

    /** {@inheritDoc}
     */
    @Override
    public void buildText()
    {
        addLine(
            ValveMessages.CONNECTIONS_STATS,
            String.valueOf(getConnectionsAccepted()),
            String.valueOf(getConnectionsRefused()),
            String.valueOf(getConnectionsFailed()),
            String.valueOf(getConnectionsClosed()));
        addLine(
            ValveMessages.BYTES_TRANSFERED,
            String.valueOf(getBytesTransfered()));

        if ((getResumes() + getPauses()) > 0) {
            addLine(
                ValveMessages.PAUSES_RESUMES,
                String.valueOf(getPauses()),
                String.valueOf(getResumes()));
        }

        super.buildText();
    }

    /** {@inheritDoc}
     */
    @Override
    public Stats clone()
    {
        final ValveStats clone = (ValveStats) super.clone();

        clone._bytesTransfered = new AtomicLong(getBytesTransfered());
        clone._connectionsAccepted = new AtomicInteger(
            getConnectionsAccepted());
        clone._connectionsClosed = new AtomicInteger(getConnectionsClosed());
        clone._connectionsFailed = new AtomicInteger(getConnectionsFailed());
        clone._connectionsRefused = new AtomicInteger(getConnectionsRefused());
        clone._pauses = new AtomicInteger(getPauses());
        clone._resumes = new AtomicInteger(getResumes());

        return clone;
    }

    /**
     * Gets the number of bytes transfered.
     *
     * @return The number of bytes transfered.
     */
    @CheckReturnValue
    public long getBytesTransfered()
    {
        return _bytesTransfered.get();
    }

    /**
     * Gets the number of connections accepted.
     *
     * @return The number of connections accepted.
     */
    @CheckReturnValue
    public int getConnectionsAccepted()
    {
        return _connectionsAccepted.get();
    }

    /**
     * Gets the number of connections closed.
     *
     * @return The number of connections closed.
     */
    @CheckReturnValue
    public int getConnectionsClosed()
    {
        return _connectionsClosed.get();
    }

    /**
     * Gets the number of failed connections.
     *
     * @return The number of failed connections.
     */
    @CheckReturnValue
    public int getConnectionsFailed()
    {
        return _connectionsFailed.get();
    }

    /**
     * Gets the number of connections refused.
     *
     * @return The number of connections refused.
     */
    @CheckReturnValue
    public int getConnectionsRefused()
    {
        return _connectionsRefused.get();
    }

    /**
     * Gets the number of pauses.
     *
     * @return The number of pauses.
     */
    @CheckReturnValue
    public int getPauses()
    {
        return _pauses.get();
    }

    /**
     * Gets the number of resumes.
     *
     * @return The number of resumes.
     */
    @CheckReturnValue
    public int getResumes()
    {
        return _resumes.get();
    }

    /** {@inheritDoc}
     */
    @Override
    protected void substract(final Stats snapshot)
    {
        final ValveStats stats = (ValveStats) snapshot;

        _connectionsAccepted.addAndGet(-stats.getConnectionsAccepted());
        _connectionsRefused.addAndGet(-stats.getConnectionsRefused());
        _connectionsFailed.addAndGet(-stats.getConnectionsFailed());
        _resumes.addAndGet(-stats.getResumes());
        _pauses.addAndGet(-stats.getPauses());
        _bytesTransfered.addAndGet(-stats.getBytesTransfered());
        _connectionsClosed.addAndGet(-stats.getConnectionsClosed());

        super.substract(snapshot);
    }

    /**
     * Updates connection counters.
     *
     * @param bytesTransfered The transfered byte count.
     */
    void updateConnectionStats(final long bytesTransfered)
    {
        _bytesTransfered.addAndGet(bytesTransfered);
    }

    /**
     * Updates connections counters.
     *
     * @param connectionsFailed Failed connection count.
     */
    void updateConnectionsFailed(final int connectionsFailed)
    {
        _connectionsFailed.addAndGet(connectionsFailed);
    }

    /**
     * Updates connections counters.
     *
     * @param connectionsClosed Closed connection count.
     */
    void updateConnectionsStats(final int connectionsClosed)
    {
        _connectionsClosed.addAndGet(connectionsClosed);
    }

    /**
     * Updates control counters.
     *
     * @param resumes Resume actions count.
     * @param pauses Pause actions count
     */
    void updateControlStats(final int resumes, final int pauses)
    {
        _resumes.addAndGet(resumes);
        _pauses.addAndGet(pauses);
    }

    /**
     * Updates connections counters.
     *
     * @param connectionsAccepted Accepted connections count.
     * @param connectionsRefused Refused connections count.
     */
    void updatePortsStats(
            final int connectionsAccepted,
            final int connectionsRefused)
    {
        _connectionsAccepted.addAndGet(connectionsAccepted);
        _connectionsRefused.addAndGet(connectionsRefused);
    }

    private static final long serialVersionUID = 1L;

    private AtomicLong _bytesTransfered = new AtomicLong();
    private AtomicInteger _connectionsAccepted = new AtomicInteger();
    private AtomicInteger _connectionsClosed = new AtomicInteger();
    private AtomicInteger _connectionsFailed = new AtomicInteger();
    private AtomicInteger _connectionsRefused = new AtomicInteger();
    private AtomicInteger _pauses = new AtomicInteger();
    private AtomicInteger _resumes = new AtomicInteger();
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
