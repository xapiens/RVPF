/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreStats.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.store.server;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.Stats;
import org.rvpf.base.StatsOwner;
import org.rvpf.base.store.StoreValues;
import org.rvpf.service.ServiceStats;

/**
 * Store stats.
 */
@ThreadSafe
public final class StoreStats
    extends ServiceStats
{
    /**
     * Constructs an instance.
     *
     * @param statsOwner The store stats owner.
     */
    StoreStats(@Nonnull final StatsOwner statsOwner)
    {
        super(statsOwner);
    }

    /**
     * Adds notices stats.
     *
     * @param sent The number of notices sent.
     * @param time The notification time in nanoseconds.
     */
    public void addNotices(final int sent, final long time)
    {
        _noticesSent.addAndGet(sent);

        if (sent > 1) {
            _noticesBatched.addAndGet(sent);
            _noticesBatches.incrementAndGet();
        }

        _notificationTime.addAndGet(time);
    }

    /**
     * Adds query response stats.
     *
     * @param response The optional response.
     * @param time The response time in nanoseconds.
     */
    public void addQueryResponse(
            @Nonnull final Optional<StoreValues> response,
            final long time)
    {
        if (response.isPresent()) {
            _queriesReceived.incrementAndGet();

            if (response.get().isSuccess()) {
                _responseValues.addAndGet(response.get().size());
            } else {
                _queriesIgnored.incrementAndGet();
            }
        }

        _responseTime.addAndGet(time);
    }

    /**
     * Adds removed and archived stats.
     *
     * @param removed The number of values removed.
     * @param archived The number of values archived.
     */
    public void addRemoved(final long removed, final long archived)
    {
        _removed.addAndGet(removed);
        _archived.addAndGet(archived);
    }

    /**
     * Adds replicates stats.
     *
     * @param sent The number of replicates sent.
     * @param time The replication time in nanoseconds.
     */
    public void addReplicates(final long sent, final long time)
    {
        _replicatesSent.addAndGet(sent);

        if (sent > 1) {
            _replicatesBatched.addAndGet(sent);
            _replicatesBatches.incrementAndGet();
        }

        _replicationTime.addAndGet(time);
    }

    /**
     * Adds updates stats.
     *
     * @param updated The number of point values updated.
     * @param deleted The number of point values deleted.
     * @param ignored The number of updates ignored.
     * @param time The update time in nanoseconds.
     */
    public void addUpdates(
            final long updated,
            final long deleted,
            final long ignored,
            final long time)
    {
        final long received = updated + deleted + ignored;

        if (received > 1) {
            _updatesBatched.addAndGet(received);
            _updatesBatches.incrementAndGet();
        }

        _updated.addAndGet(updated);
        _deleted.addAndGet(deleted);
        _updatesIgnored.addAndGet(ignored);

        _updateTime.addAndGet(time);
    }

    /** {@inheritDoc}
     */
    @Override
    public void buildText()
    {
        final long received = getUpdated() + getDeleted() + getUpdatesIgnored();

        addLine(
            StoreMessages.SESSIONS_STATS,
            String.valueOf(getSessionsOpened()),
            String.valueOf(getSessionsClosed()));

        if (received > 0) {
            if (getUpdatesBatches() > 0) {
                addLine(
                    StoreMessages.UPDATES_BATCHES,
                    String.valueOf(getUpdatesBatches()));
                addLine(
                    StoreMessages.UPDATES_STATS,
                    String.valueOf(received),
                    String.valueOf(getUpdatesBatched()));
            } else {
                addLine(
                    StoreMessages.UPDATES_RECEIVED,
                    String.valueOf(received));
            }

            if (getUpdatesIgnored() > 0) {
                addLine(
                    StoreMessages.UPDATES_IGNORED,
                    String.valueOf(getUpdatesIgnored()));
            }

            if (getUpdated() > 0) {
                addLine(
                    StoreMessages.VALUES_UPDATED,
                    String.valueOf(getUpdated()));
            }

            if (getDeleted() > 0) {
                addLine(
                    StoreMessages.VALUES_DELETED,
                    String.valueOf(getDeleted()));
            }

            addLine(StoreMessages.UPDATE_TIME, nanosToString(getUpdateTime()));
        }

        if (getReplicatesSent() > 0) {
            if (getReplicatesBatches() > 0) {
                addLine(
                    StoreMessages.REPLICATES_BATCHES,
                    String.valueOf(getReplicatesBatches()));
                addLine(
                    StoreMessages.REPLICATES_STATS,
                    String.valueOf(getReplicatesSent()),
                    String.valueOf(getReplicatesBatched()));
            } else {
                addLine(
                    StoreMessages.REPLICATES_SENT,
                    String.valueOf(getReplicatesSent()));
            }

            addLine(
                StoreMessages.REPLICATION_TIME,
                nanosToString(getReplicationTime()));
        }

        if (getNoticesSent() > 0) {
            if (getNoticesBatches() > 0) {
                addLine(
                    StoreMessages.NOTICES_BATCHES,
                    String.valueOf(getNoticesBatches()));
                addLine(
                    StoreMessages.NOTICES_STATS,
                    String.valueOf(getNoticesSent()),
                    String.valueOf(getNoticesBatched()));
            } else {
                addLine(
                    StoreMessages.NOTICES_SENT,
                    String.valueOf(getNoticesSent()));
            }

            addLine(
                StoreMessages.NOTIFICATION_TIME,
                nanosToString(getNotificationTime()));
        }

        if (getQueriesReceived() > 0) {
            addLine(
                StoreMessages.QUERIES_RECEIVED,
                String.valueOf(getQueriesReceived()));

            if (getQueriesIgnored() > 0) {
                addLine(
                    StoreMessages.QUERIES_IGNORED,
                    String.valueOf(getQueriesIgnored()));
            }

            addLine(
                StoreMessages.VALUES_SENT,
                String.valueOf(getResponseValues()));
            addLine(
                StoreMessages.RESPONSE_TIME,
                nanosToString(getResponseTime()));
        }

        if (getRemoved() > 0) {
            addLine(
                StoreMessages.VALUES_REMOVED,
                String.valueOf(getRemoved()),
                String.valueOf(getArchived()));
        }

        super.buildText();
    }

    /** {@inheritDoc}
     */
    @Override
    public Stats clone()
    {
        final StoreStats clone = (StoreStats) super.clone();

        clone._archived = new AtomicLong(getArchived());
        clone._deleted = new AtomicLong(getDeleted());
        clone._noticesBatched = new AtomicLong(getNoticesBatched());
        clone._noticesBatches = new AtomicInteger(getNoticesBatches());
        clone._noticesSent = new AtomicLong(getNoticesSent());
        clone._notificationTime = new AtomicLong(getNotificationTime());
        clone._queriesIgnored = new AtomicInteger(getQueriesIgnored());
        clone._queriesReceived = new AtomicInteger(getQueriesReceived());
        clone._removed = new AtomicLong(getRemoved());
        clone._replicatesBatched = new AtomicLong(getReplicatesBatched());
        clone._replicatesBatches = new AtomicInteger(getReplicatesBatches());
        clone._replicatesSent = new AtomicLong(getReplicatesSent());
        clone._replicationTime = new AtomicLong(getReplicationTime());
        clone._responseTime = new AtomicLong(getResponseTime());
        clone._responseValues = new AtomicLong(getResponseValues());
        clone._sessionsClosed = new AtomicInteger(getSessionsClosed());
        clone._sessionsOpened = new AtomicInteger(getSessionsOpened());
        clone._updated = new AtomicLong(getUpdated());
        clone._updatesBatched = new AtomicLong(getUpdatesBatched());
        clone._updatesBatches = new AtomicInteger(getUpdatesBatches());
        clone._updatesIgnored = new AtomicLong(getUpdatesIgnored());
        clone._updateTime = new AtomicLong(getUpdateTime());

        return clone;
    }

    /**
     * Gets the archived count.
     *
     * @return The archived count.
     */
    public long getArchived()
    {
        return _archived.get();
    }

    /**
     * Gets the number of deleted values.
     *
     * @return The number of deleted values.
     */
    @CheckReturnValue
    public long getDeleted()
    {
        return _deleted.get();
    }

    /**
     * Gets the number of notices batched.
     *
     * @return The number of notices batched.
     */
    @CheckReturnValue
    public long getNoticesBatched()
    {
        return _noticesBatched.get();
    }

    /**
     * Gets the number of notices batches.
     *
     * @return The number of notices batches.
     */
    @CheckReturnValue
    public int getNoticesBatches()
    {
        return _noticesBatches.get();
    }

    /**
     * Gets the number of notices sent.
     *
     * @return The number of notices sent.
     */
    @CheckReturnValue
    public long getNoticesSent()
    {
        return _noticesSent.get();
    }

    /**
     * Gets the notification time.
     *
     * @return The notification time.
     */
    @CheckReturnValue
    public long getNotificationTime()
    {
        return _notificationTime.get();
    }

    /**
     * Gets the number of queries ignored.
     *
     * @return The number of queries ignored.
     */
    @CheckReturnValue
    public int getQueriesIgnored()
    {
        return _queriesIgnored.get();
    }

    /**
     * Gets the number of queries received.
     *
     * @return The number of queries received.
     */
    @CheckReturnValue
    public int getQueriesReceived()
    {
        return _queriesReceived.get();
    }

    /**
     * Gets the removed count.
     *
     * @return The removed count.
     */
    public long getRemoved()
    {
        return _removed.get();
    }

    /**
     * Gets the number of replicates batched.
     *
     * @return The number of replicates batched.
     */
    @CheckReturnValue
    public long getReplicatesBatched()
    {
        return _replicatesBatched.get();
    }

    /**
     * Gets the number of replicate batches.
     *
     * @return The number of replicate batches.
     */
    @CheckReturnValue
    public int getReplicatesBatches()
    {
        return _replicatesBatches.get();
    }

    /**
     * Gets the number of replicates sent.
     *
     * @return The number of replicates sent.
     */
    @CheckReturnValue
    public long getReplicatesSent()
    {
        return _replicatesSent.get();
    }

    /**
     * Gets the replication time.
     *
     * @return The replication time.
     */
    @CheckReturnValue
    public long getReplicationTime()
    {
        return _replicationTime.get();
    }

    /**
     * Gets the response time.
     *
     * @return The response time.
     */
    @CheckReturnValue
    public long getResponseTime()
    {
        return _responseTime.get();
    }

    /**
     * Gets the number of response values.
     *
     * @return The number of response values.
     */
    @CheckReturnValue
    public long getResponseValues()
    {
        return _responseValues.get();
    }

    /**
     * Gets the number of sessions closed.
     *
     * @return The number of sessions closed.
     */
    @CheckReturnValue
    public int getSessionsClosed()
    {
        return _sessionsClosed.get();
    }

    /**
     * Gets the number of sessions opened.
     *
     * @return The number of sessions opened.
     */
    @CheckReturnValue
    public int getSessionsOpened()
    {
        return _sessionsOpened.get();
    }

    /**
     * Gets the update time.
     *
     * @return The update time.
     */
    @CheckReturnValue
    public long getUpdateTime()
    {
        return _updateTime.get();
    }

    /**
     * Gets the number of updated values.
     *
     * @return The number of updated values.
     */
    @CheckReturnValue
    public long getUpdated()
    {
        return _updated.get();
    }

    /**
     * Gets the number of updates batched.
     *
     * @return The number of updates batched.
     */
    @CheckReturnValue
    public long getUpdatesBatched()
    {
        return _updatesBatched.get();
    }

    /**
     * Gets the number of update batches.
     *
     * @return The number of update batches.
     */
    @CheckReturnValue
    public int getUpdatesBatches()
    {
        return _updatesBatches.get();
    }

    /**
     * Gets the number of updates ignored.
     *
     * @return The number of updates ignored.
     */
    @CheckReturnValue
    public long getUpdatesIgnored()
    {
        return _updatesIgnored.get();
    }

    /**
     * Called when a session is closed.
     */
    public void sessionClosed()
    {
        _sessionsClosed.incrementAndGet();
    }

    /**
     * Called when a session is opened.
     */
    public void sessionOpened()
    {
        _sessionsOpened.incrementAndGet();
    }

    /** {@inheritDoc}
     */
    @Override
    protected void substract(final Stats snapshot)
    {
        final StoreStats stats = (StoreStats) snapshot;

        _deleted.addAndGet(-stats.getDeleted());
        _noticesBatches.addAndGet(-stats.getNoticesBatches());
        _noticesBatched.addAndGet(-stats.getNoticesBatched());
        _noticesSent.addAndGet(-stats.getNoticesSent());
        _notificationTime.addAndGet(-stats.getNotificationTime());
        _queriesIgnored.addAndGet(-stats.getQueriesIgnored());
        _queriesReceived.addAndGet(-stats.getQueriesReceived());
        _replicatesBatches.addAndGet(-stats.getReplicatesBatches());
        _replicatesBatched.addAndGet(-stats.getReplicatesBatched());
        _replicatesSent.addAndGet(-stats.getReplicatesSent());
        _replicationTime.addAndGet(-stats.getReplicationTime());
        _responseTime.addAndGet(-stats.getResponseTime());
        _responseValues.addAndGet(-stats.getResponseValues());
        _sessionsClosed.addAndGet(-stats.getSessionsClosed());
        _sessionsOpened.addAndGet(-stats.getSessionsOpened());
        _updatesBatches.addAndGet(-stats.getUpdatesBatches());
        _updated.addAndGet(-stats.getUpdated());
        _updatesBatched.addAndGet(-stats.getUpdatesBatched());
        _updatesIgnored.addAndGet(-stats.getUpdatesIgnored());
        _updateTime.addAndGet(-stats.getUpdateTime());

        super.substract(snapshot);
    }

    private static final long serialVersionUID = 1L;

    private AtomicLong _archived = new AtomicLong();
    private AtomicLong _deleted = new AtomicLong();
    private AtomicLong _noticesBatched = new AtomicLong();
    private AtomicInteger _noticesBatches = new AtomicInteger();
    private AtomicLong _noticesSent = new AtomicLong();
    private AtomicLong _notificationTime = new AtomicLong();
    private AtomicInteger _queriesIgnored = new AtomicInteger();
    private AtomicInteger _queriesReceived = new AtomicInteger();
    private AtomicLong _removed = new AtomicLong();
    private AtomicLong _replicatesBatched = new AtomicLong();
    private AtomicInteger _replicatesBatches = new AtomicInteger();
    private AtomicLong _replicatesSent = new AtomicLong();
    private AtomicLong _replicationTime = new AtomicLong();
    private AtomicLong _responseTime = new AtomicLong();
    private AtomicLong _responseValues = new AtomicLong();
    private AtomicInteger _sessionsClosed = new AtomicInteger();
    private AtomicInteger _sessionsOpened = new AtomicInteger();
    private AtomicLong _updated = new AtomicLong();
    private AtomicLong _updatesBatched = new AtomicLong();
    private AtomicInteger _updatesBatches = new AtomicInteger();
    private AtomicLong _updatesIgnored = new AtomicLong();
    private AtomicLong _updateTime = new AtomicLong();
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
