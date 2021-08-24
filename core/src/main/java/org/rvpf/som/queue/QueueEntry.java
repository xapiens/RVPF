/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: QueueEntry.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.som.queue;

import java.io.File;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

/**
 * Queue entry.
 *
 * <p>A queue entry represents a file holding a transaction sent to the
 * queue.</p>
 *
 * <p>Since the length of the transactions for a receiver does not need to match
 * the length of the original transaction, the queue entry may be consumed in
 * multiple steps, each step being a receiver transaction. To support this, an
 * auxilliary file may be created to hold a byte offset for the next
 * transaction.</p>
 */
final class QueueEntry
{
    /**
     * Constructs an instance.
     *
     * @param queue The owner of this entry.
     * @param name The entry name.
     */
    QueueEntry(@Nonnull final FilesQueue queue, @Nonnull final String name)
    {
        _queue = queue;
        setName(name);
    }

    /**
     * Gets the 'backup' file.
     *
     * @return The optional 'backup' file.
     */
    @GuardedBy("_queue")
    @Nonnull
    @CheckReturnValue
    Optional<File> getBackupFile()
    {
        final Optional<String> backupType = _queue.getBackupSuffix();

        return backupType
            .isPresent()? Optional
                .of(_getFile(backupType.get())): Optional.empty();
    }

    /**
     * Gets the 'bad' file.
     *
     * @return The 'bad' file.
     */
    @GuardedBy("_queue")
    @Nonnull
    @CheckReturnValue
    File getBadFile()
    {
        return _getFile(_queue.getBadSuffix());
    }

    /**
     * Gets the 'data' file.
     *
     * @return The 'data' file.
     */
    @GuardedBy("_queue")
    @Nonnull
    @CheckReturnValue
    File getDataFile()
    {
        return _getFile(_queue.getDataSuffix());
    }

    /**
     * Gets the file size.
     *
     * @return The file size.
     */
    @GuardedBy("_queue")
    @CheckReturnValue
    long getFileSize()
    {
        return _fileSize;
    }

    /**
     * Gets the message count.
     *
     * @return The message count.
     */
    @GuardedBy("_queue")
    @CheckReturnValue
    long getMessageCount()
    {
        return _messageCount;
    }

    /**
     * Gets this entry's name.
     *
     * @return This entry's name.
     */
    @GuardedBy("_queue")
    @Nonnull
    @CheckReturnValue
    String getName()
    {
        return _name;
    }

    /**
     * Gets the 'next' file.
     *
     * @return The 'next' file.
     */
    @GuardedBy("_queue")
    @Nonnull
    @CheckReturnValue
    File getNextFile()
    {
        return _getFile(_queue.getNextSuffix());
    }

    /**
     * Gets the next position.
     *
     * @return The next position.
     */
    @GuardedBy("_queue")
    @CheckReturnValue
    long getNextPosition()
    {
        return _nextPosition;
    }

    /**
     * Gets the 'trans' file.
     *
     * @return The 'trans' file.
     */
    @GuardedBy("_queue")
    @Nonnull
    @CheckReturnValue
    File getTransFile()
    {
        return _getFile(_queue.getTransSuffix());
    }

    /**
     * Asks if this queue entry is busy.
     *
     * @return True if busy in a receiver transaction.
     */
    @GuardedBy("_queue")
    @CheckReturnValue
    boolean isBusy()
    {
        return _busy;
    }

    /**
     * Asks if this queue entry is ready.
     *
     * @return True if ready.
     */
    @GuardedBy("_queue")
    @CheckReturnValue
    boolean isReady()
    {
        return _ready;
    }

    /**
     * Sets the 'busy' indicator.
     *
     * @param busy The 'busy' indicator.
     */
    @GuardedBy("_queue")
    void setBusy(final boolean busy)
    {
        _busy = busy;
    }

    /**
     * Sets the file size.
     *
     * @param fileSize The file size.
     */
    @GuardedBy("_queue")
    void setFileSize(final long fileSize)
    {
        _fileSize = fileSize;
    }

    /**
     * Sets the message count.
     *
     * @param messageCount The message count.
     */
    @GuardedBy("_queue")
    void setMesssageCount(final long messageCount)
    {
        _messageCount = messageCount;
    }

    /**
     * Sets this entry's name.
     *
     * @param name The name.
     */
    @GuardedBy("_queue")
    void setName(@Nonnull final String name)
    {
        _name = name;
    }

    /**
     * Sets the next position.
     *
     * @param nextPosition The next position.
     */
    @GuardedBy("_queue")
    void setNextPosition(final long nextPosition)
    {
        _nextPosition = nextPosition;
    }

    /**
     * Sets the 'ready' indicator.
     *
     * @param ready The 'ready' indicator.
     */
    @GuardedBy("_queue")
    void setReady(final boolean ready)
    {
        _ready = ready;
    }

    @GuardedBy("_queue")
    @Nonnull
    @CheckReturnValue
    private File _getFile(@Nonnull final String type)
    {
        return new File(
            _queue.getDirectoryFile(),
            _queue.getEntryPrefix() + getName() + type);
    }

    private boolean _busy;
    private long _fileSize;
    private long _messageCount;
    private String _name;
    private long _nextPosition;
    private final FilesQueue _queue;
    private boolean _ready;
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
