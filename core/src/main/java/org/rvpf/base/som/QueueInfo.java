/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: QueueInfo.java 4067 2019-06-08 13:39:16Z SFB $
 */

package org.rvpf.base.som;

import java.io.Serializable;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.DateTime;

/**
 * Queue info.
 */
@ThreadSafe
public final class QueueInfo
    implements Serializable
{
    /**
     * Clears files size info.
     */
    public void clearFilesSize()
    {
        _filesSize.set(0);
    }

    /**
     * Clears message count info.
     */
    public void clearMessageCount()
    {
        _messageCount.set(0);
    }

    /**
     * Gets the file count info.
     *
     * @return The file count info.
     */
    @Nonnegative
    @CheckReturnValue
    public int getFileCount()
    {
        return _fileCount.get();
    }

    /**
     * Gets files size info.
     *
     * @return The files size.
     */
    @Nonnegative
    @CheckReturnValue
    public long getFilesSize()
    {
        return _filesSize.get();
    }

    /**
     * Gets the last receiver commit time.
     *
     * @return The optional last receiver commit time.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<DateTime> getLastReceiverCommit()
    {
        return Optional.ofNullable(_lastReceiverCommit);
    }

    /**
     * Gets the last sender commit time.
     *
     * @return The optional last sender commit time.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<DateTime> getLastSenderCommit()
    {
        return Optional.ofNullable(_lastSenderCommit);
    }

    /**
     * Gets message count info.
     *
     * @return The message count.
     */
    @Nonnegative
    @CheckReturnValue
    public long getMessageCount()
    {
        return _messageCount.get();
    }

    /**
     * Gets messages dropped info.
     *
     * @return The messages dropped.
     */
    @Nonnegative
    @CheckReturnValue
    public long getMessagesDropped()
    {
        return _messagesDropped.get();
    }

    /**
     * Gets the receiver connect time.
     *
     * @return The optional receiver connect time.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<DateTime> getReceiverConnectTime()
    {
        return Optional.ofNullable(_receiverConnectTime);
    }

    /**
     * Gets the sender count.
     *
     * @return The sender count.
     */
    @Nonnegative
    @CheckReturnValue
    public int getSenderCount()
    {
        return _senderCount.get();
    }

    /**
     * Sets the last receiver commit time.
     *
     * @param lastReceiverCommit The last receiver commit time.
     */
    public void setLastReceiverCommit(
            @Nonnull final DateTime lastReceiverCommit)
    {
        _lastReceiverCommit = lastReceiverCommit;
    }

    /**
     * Sets the last sender commit time.
     *
     * @param lastSenderCommit The last sender commit time.
     */
    public void setLastSenderCommit(@Nonnull final DateTime lastSenderCommit)
    {
        _lastSenderCommit = lastSenderCommit;
    }

    /**
     * Sets the receiver connect time.
     *
     * @param receiverConnecTime The optional receiver connect time.
     */
    public void setReceiverConnectTime(
            @Nonnull final Optional<DateTime> receiverConnecTime)
    {
        _receiverConnectTime = receiverConnecTime.orElse(null);
    }

    /**
     * Updates the file count.
     *
     * @param delta The count delta.
     */
    public void updateFileCount(final int delta)
    {
        _fileCount.addAndGet(delta);
    }

    /**
     * Updates the files size.
     *
     * @param delta The size delta.
     */
    public void updateFilesSize(final long delta)
    {
        _filesSize.addAndGet(delta);
    }

    /**
     * Updates the message count.
     *
     * @param delta The count delta.
     */
    public void updateMessageCount(final long delta)
    {
        _messageCount.addAndGet(delta);
    }

    /**
     * Updates the count of messages dropped.
     *
     * @param delta The count delta.
     */
    public void updateMessagesDropped(final long delta)
    {
        _messagesDropped.addAndGet(delta);
    }

    /**
     * Updates the sender count.
     *
     * @param delta The count delta.
     */
    public void updateSenderCount(final int delta)
    {
        _senderCount.addAndGet(delta);
    }

    private static final long serialVersionUID = 1L;

    private final AtomicInteger _fileCount = new AtomicInteger();
    private final AtomicLong _filesSize = new AtomicLong();
    private volatile DateTime _lastReceiverCommit;
    private volatile DateTime _lastSenderCommit;
    private final AtomicLong _messageCount = new AtomicLong();
    private final AtomicLong _messagesDropped = new AtomicLong();
    private volatile DateTime _receiverConnectTime;
    private final AtomicInteger _senderCount = new AtomicInteger();
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
