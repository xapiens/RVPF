/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: FilesSender.java 4102 2019-06-30 15:41:17Z SFB $
 */

package org.rvpf.som.queue;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

import java.nio.charset.StandardCharsets;

import java.util.Optional;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.TimeoutMonitor;
import org.rvpf.base.xml.streamer.Streamer;
import org.rvpf.service.ServiceMessages;

/**
 * Sender implementation.
 */
final class FilesSender
    implements Queue.Sender, TimeoutMonitor.Client
{
    /**
     * Constructs an instance.
     *
     * @param queue The queue served.
     */
    FilesSender(@Nonnull final FilesQueue queue)
    {
        _queue = queue;
    }

    /** {@inheritDoc}
     */
    @Override
    public void close()
    {
        synchronized (_senderMutex) {
            if (!_closed) {
                final Optional<TimeoutMonitor> timeoutMonitor = _queue
                    .getTimeoutMonitor();

                if (timeoutMonitor.isPresent()) {
                    timeoutMonitor.get().removeClient(this);
                }

                try {
                    if (_queue.isAutocommit()) {
                        commit();
                    } else {
                        rollback();
                    }
                } catch (final ServiceClosedException exception) {
                    throw new InternalError(exception);
                }

                _queue.onSenderClosed(this);
                _closed = true;
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void commit()
        throws ServiceClosedException
    {
        synchronized (_senderMutex) {
            if (_closed) {
                throw new ServiceClosedException();
            }

            if (_entry != null) {
                _closeOutput();

                _queue.releaseEntry(_entry, _length);

                _entry = null;
                _length = 0;
            }

            _monitorTimeout(false);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void onTimeoutMonitoring()
    {
        synchronized (_senderMutex) {
            if (_idle) {
                try {
                    commit();
                    _idle = true;
                } catch (final ServiceClosedException exception) {
                    final Optional<TimeoutMonitor> timeoutMonitor = _queue
                        .getTimeoutMonitor();

                    if (timeoutMonitor.isPresent()) {
                        timeoutMonitor.get().removeClient(this);
                    }
                }
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void rollback()
        throws ServiceClosedException
    {
        synchronized (_senderMutex) {
            if (_closed) {
                throw new ServiceClosedException();
            }

            if (_entry != null) {
                _closeOutput();

                if (_entry.getTransFile().delete()) {
                    _LOGGER
                        .trace(
                            ServiceMessages.QUEUE_FILE_DELETED,
                            _queue.getDirectoryFile(),
                            _entry.getTransFile().getName());
                } else {
                    throw new RuntimeException(
                        Message.format(
                            BaseMessages.FILE_DELETE_FAILED,
                            _entry.getTransFile()));
                }

                _entry = null;
                _length = 0;
            }

            _monitorTimeout(false);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void send(
            final Serializable[] messages,
            final boolean commit)
        throws ServiceClosedException
    {
        if (messages.length < 1) {
            throw new IllegalArgumentException();
        }

        synchronized (_senderMutex) {
            if (_closed) {
                throw new ServiceClosedException();
            }

            for (final Serializable message: messages) {
                _doSend(message);
            }

            if (commit) {
                commit();
            } else {
                _flush();
            }
        }
    }

    @GuardedBy("_senderMutex")
    private void _closeOutput()
    {
        if (_streamerOutput != null) {
            _streamerOutput.close();

            try {
                _outputStream.close();
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
            }

            _streamerOutput = null;
        }
    }

    @GuardedBy("_senderMutex")
    private void _doSend(
            final Serializable message)
        throws ServiceClosedException
    {
        Require.notNull(message);

        if (_entry == null) {
            _entry = _queue.newEntry();

            try {
                _outputStream = new FileOutputStream(_entry.getTransFile());

                if (_queue.isCompressed()) {
                    _outputStream = new GZIPOutputStream(_outputStream);
                }
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
            }

            _streamerOutput = _queue
                .getStreamer()
                .newOutput(_outputStream, Optional.of(StandardCharsets.UTF_8));
            _LOGGER
                .trace(
                    ServiceMessages.QUEUE_FILE_CREATED,
                    _queue.getDirectoryFile(),
                    _entry.getTransFile().getName());

            _monitorTimeout(true);
        }

        if (!_streamerOutput.add(message)) {
            throw new RuntimeException(
                Message.format(ServiceMessages.MESSAGE_VALIDATION_FAILED));
        }

        ++_length;

        if (_queue.isAutocommit()
                && (_length >= _queue.getAutocommitThreshold())) {
            commit();
        }
    }

    @GuardedBy("_senderMutex")
    private void _flush()
        throws ServiceClosedException
    {
        if (_closed) {
            throw new ServiceClosedException();
        }

        if (_streamerOutput != null) {
            _streamerOutput.flush();
        }

        _idle = false;
    }

    @GuardedBy("_senderMutex")
    private void _monitorTimeout(final boolean monitor)
    {
        final Optional<TimeoutMonitor> timeoutMonitor = _queue
            .getTimeoutMonitor();

        if (timeoutMonitor.isPresent()) {
            if (monitor) {
                timeoutMonitor.get().addClient(this);
            } else {
                timeoutMonitor.get().removeClient(this);
            }

            _idle = false;
        }
    }

    private static final Logger _LOGGER = Logger.getInstance(FilesSender.class);

    private boolean _closed;
    private QueueEntry _entry;
    private boolean _idle;
    private int _length;
    private OutputStream _outputStream;
    private final FilesQueue _queue;
    private final Object _senderMutex = new Object();
    private Streamer.Output _streamerOutput;
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
