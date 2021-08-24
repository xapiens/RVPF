/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: FilesReceiver.java 3896 2019-02-16 13:42:30Z SFB $
 */

package org.rvpf.som.queue;

import java.io.Serializable;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.xml.streamer.Streamer;

/**
 * Receiver implementation.
 */
final class FilesReceiver
    implements Queue.Receiver
{
    /**
     * Constructs an instance.
     *
     * @param queue The queue served.
     */
    FilesReceiver(@Nonnull final FilesQueue queue)
    {
        _queue = queue;
    }

    /** {@inheritDoc}
     */
    @Override
    public void close()
    {
        synchronized (_receiverMutex) {
            if (_closed) {
                return;
            }

            try {
                rollback();
            } catch (final ServiceClosedException exception) {
                throw new InternalError(exception);
            }

            _closed = true;
        }

        _queue.onReceiverClosed(this);
    }

    /**
     * Commits.
     */

    /** {@inheritDoc}
     */
    @Override
    public void commit()
        throws ServiceClosedException
    {
        synchronized (_receiverMutex) {
            if (_closed) {
                throw new ServiceClosedException();
            }

            if (_input != null) {
                final long next = _reader.getNext();

                if (_input.hasNext()) {
                    _entry.setNextPosition(next);
                } else if (_entry != null) {
                    _entries.addLast(_entry);
                    _entry = null;
                }

                _input.close();
                _input = null;
                _reader = null;
            }

            _queue.dropEntries(_entries, Optional.ofNullable(_entry), _length);

            _entries.clear();
            _entry = null;
            _length = 0;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public long purge()
        throws ServiceClosedException
    {
        rollback();

        return _queue.purge();
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable[] receive(
            int limit,
            long timeout)
        throws ServiceClosedException
    {
        final List<Serializable> messages = new LinkedList<Serializable>();
        boolean insist = true;

        do {
            final Serializable message = _receive(timeout, insist);

            if (message == null) {
                break;
            }

            messages.add(message);
            timeout = 0;
            insist = false;
        } while (--limit > 0);

        return messages.toArray(new Serializable[messages.size()]);
    }

    /** {@inheritDoc}
     */
    @Override
    public void rollback()
        throws ServiceClosedException
    {
        synchronized (_receiverMutex) {
            if (_closed) {
                throw new ServiceClosedException();
            }

            if (_input != null) {
                _input.close();
                _input = null;
                _reader = null;
            }

            if (_entry != null) {
                _entry.setBusy(false);
                _entry = null;
            }

            for (final QueueEntry entry: _entries) {
                entry.setBusy(false);
            }

            _entries.clear();
            _length = 0;
        }
    }

    private Serializable _receive(
            final long timeout,
            final boolean insist)
        throws ServiceClosedException
    {
        final long startMillis = (timeout > 0)? System.currentTimeMillis(): 0;
        Serializable message;

        try {
            for (;;) {
                final boolean wait;

                synchronized (_receiverMutex) {
                    if (_closed) {
                        throw new ServiceClosedException();
                    }

                    if (_entry == null) {
                        final Optional<QueueEntry> previousEntry = (_entries
                            .isEmpty()? Optional
                                .empty(): Optional.of(_entries.getLast()));

                        _entry = _queue.nextEntry(previousEntry).orElse(null);

                        if (_entry != null) {
                            _entry.setBusy(true);
                            _reader = new QueueReader(
                                _entry.getDataFile(),
                                _entry.getNextPosition(),
                                _queue.isCompressed());
                            _input = _queue.getStreamer().newInput(_reader);
                            wait = false;
                        } else {
                            wait = true;
                        }
                    } else {
                        wait = false;
                    }
                }

                if (wait) {
                    if (timeout == 0) {
                        return null;
                    }

                    final long waitMillis;

                    if (timeout > 0) {
                        final long elapsedMillis = System
                            .currentTimeMillis() - startMillis;

                        if ((elapsedMillis < 0) || (elapsedMillis >= timeout)) {
                            return null;
                        }

                        waitMillis = timeout - elapsedMillis;
                    } else {
                        waitMillis = 0;
                    }

                    _queue.waitForMessages(waitMillis);

                    continue;
                }

                synchronized (_queue) {
                    try {
                        message = _input.next();
                        ++_length;

                        break;
                    } catch (final NoSuchElementException exception) {
                        _input.close();
                        _input = null;
                        _reader = null;
                        _entries.add(_entry);
                        _entry = null;
                        message = null;
                    }
                }

                if (!insist) {
                    break;
                }
            }
        } catch (final RuntimeException exception) {
            throw exception;
        } catch (final InterruptedException exception) {
            throw new ServiceClosedException(exception);
        } catch (final ServiceClosedException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new RuntimeException(exception);
        }

        return message;
    }

    private boolean _closed;
    private final LinkedList<QueueEntry> _entries =
        new LinkedList<QueueEntry>();
    private QueueEntry _entry;
    private Streamer.Input _input;
    private int _length;
    private final FilesQueue _queue;
    private QueueReader _reader;
    private final Object _receiverMutex = new Object();
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
