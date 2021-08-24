/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MemoryQueue.java 4021 2019-05-24 13:01:15Z SFB $
 */

package org.rvpf.som.queue;

import java.io.Serializable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.som.QueueInfo;
import org.rvpf.base.tool.TimeoutMonitor;
import org.rvpf.base.util.container.KeyedValues;
import org.rvpf.service.ServiceMessages;

/**
 * Memory queue.
 */
public final class MemoryQueue
    extends Queue.Abstract
{
    /**
     * Constructs an instance.
     *
     * @param name The queue name.
     * @param stats The queue stats.
     */
    MemoryQueue(@Nonnull final String name, @Nonnull final QueueStats stats)
    {
        super(name, stats);
    }

    /** {@inheritDoc}
     */
    @Override
    public QueueInfo getInfo()
    {
        return _info;
    }

    /** {@inheritDoc}
     */
    @Override
    public Receiver newReceiver()
    {
        final Receiver receiver = new Receiver();

        onNewReceiver(receiver);

        return receiver;
    }

    /** {@inheritDoc}
     */
    @Override
    public Sender newSender()
    {
        final Sender sender = new Sender();

        onNewSender(sender);

        return sender;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final KeyedValues somProperties)
    {
        if (!super.setUp(somProperties)) {
            return false;
        }

        getThisLogger().info(ServiceMessages.QUEUE_IN_MEMORY, getName());
        _keepLimit = somProperties.getInt(KEEP_LIMIT_PROPERTY, 0);

        if (_keepLimit > 0) {
            getThisLogger()
                .info(
                    ServiceMessages.QUEUE_KEEP_LIMIT,
                    getName(),
                    String.valueOf(_keepLimit));
        }

        _receiverRequired = somProperties
            .getBoolean(RECEIVER_REQUIRED_PROPERTY);

        if (_receiverRequired) {
            getThisLogger().info(ServiceMessages.QUEUE_RECEIVER_REQUIRED);
        }

        getThisLogger().debug(ServiceMessages.SET_UP_COMPLETED);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        super.tearDown();

        getThisLogger().debug(ServiceMessages.TEAR_DOWN_COMPLETED);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void onReceiverClosed(final Queue.Receiver receiver)
    {
        synchronized (_queueMutex) {
            if (_receiverRequired) {
                _purge();
            }

            _queueMutex.notifyAll();
        }

        super.onReceiverClosed(receiver);
    }

    /**
     * Adds messages.
     *
     * @param messages The messages.
     */
    void _addMessages(@Nonnull Collection<Serializable> messages)
    {
        final int keepLimit = ((_keepLimit > 0)
                && !hasReceiver())? _keepLimit: Integer.MAX_VALUE;

        synchronized (_queueMutex) {
            if (!_receiverRequired || hasReceiver()) {
                for (final Serializable message: messages) {
                    while (_queueMessages.size() >= keepLimit) {
                        _queueMessages.removeFirst();

                        if (!_warnedAboutDrop) {
                            getThisLogger()
                                .warn(
                                    ServiceMessages.QUEUE_MESSAGES_DROPPED,
                                    getName(),
                                    String.valueOf(keepLimit));
                            _warnedAboutDrop = true;
                        }

                        getThisLogger()
                            .trace(
                                ServiceMessages.QUEUE_MESSAGE_DROPPED,
                                getName(),
                                message);
                        _info.updateMessagesDropped(+1);
                    }

                    _queueMessages.add(message);
                }

                _queueMutex.notifyAll();
            } else {
                messages = null;
            }
        }

        if (messages != null) {
            _info.updateMessageCount(+messages.size());
            _info.setLastSenderCommit(DateTime.now());
            getStats().transactionReceived(messages.size());
        }
    }

    /**
     * Gets the keep limit.
     *
     * @return The keep limit.
     */
    @CheckReturnValue
    int _getKeepLimit()
    {
        return _keepLimit;
    }

    /**
     * Gets a message.
     *
     * @return The message (empty when no message is available).
     */
    @Nonnull
    @CheckReturnValue
    Optional<Serializable> _getMessage()
    {
        synchronized (_queueMutex) {
            return Optional.ofNullable(_queueMessages.poll());
        }
    }

    /**
     * Called on receiver commit.
     *
     * @param messageCount The number of messages committed.
     */
    void _onReceiverCommit(final int messageCount)
    {
        _info.updateMessageCount(-messageCount);
        _info.setLastReceiverCommit(DateTime.now());
        getStats().transactionsSent(1, messageCount);

        synchronized (_queueMutex) {
            if (_warnedAboutDrop && (_queueMessages.size() == 0)) {
                getThisLogger().info(ServiceMessages.QUEUE_EMPTY, getName());
                _warnedAboutDrop = false;
            }
        }
    }

    /**
     * Purges the queue.
     *
     * @return The number of messages purged.
     */
    int _purge()
    {
        final int purged;

        synchronized (_queueMutex) {
            purged = _queueMessages.size();
            _queueMessages.clear();
        }

        _info.updateMessageCount(-purged);

        return purged;
    }

    /**
     * Puts back messages.
     *
     * @param messages The messages.
     */
    void _putBack(@Nonnull final Collection<Serializable> messages)
    {
        synchronized (_queueMutex) {
            _queueMessages.addAll(0, messages);
        }
    }

    /**
     * Waits for messages.
     *
     * @param waitMillis The maximum wait time in milliseconds.
     *
     * @throws InterruptedException When interrupted.
     */
    void _waitForMessages(final long waitMillis)
        throws InterruptedException
    {
        synchronized (_queueMutex) {
            _queueMutex.wait(waitMillis);
        }
    }

    /** Keep limit property. */
    public static final String KEEP_LIMIT_PROPERTY = "keep.limit";

    /** Receiver required property. */
    public static final String RECEIVER_REQUIRED_PROPERTY = "receiver.required";

    private final QueueInfo _info = new QueueInfo();
    private int _keepLimit;
    private final LinkedList<Serializable> _queueMessages = new LinkedList<>();
    private final Object _queueMutex = new Object();
    private boolean _receiverRequired;
    private boolean _warnedAboutDrop;

    /**
     * Receiver.
     */
    private final class Receiver
        implements Queue.Receiver
    {
        /**
         * Constructs an instance.
         */
        Receiver() {}

        /** {@inheritDoc}
         */
        @Override
        public void close()
        {
            synchronized (_receiverMutex) {
                if (_closed) {
                    return;
                }

                _closed = true;
            }

            onReceiverClosed(this);
        }

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

                _onReceiverCommit(_receivedMessages.size());
                _receivedMessages.clear();
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public long purge()
            throws ServiceClosedException
        {
            rollback();

            getThisLogger()
                .trace(ServiceMessages.QUEUE_PURGE_STARTED, getName());

            final int purged = _purge();

            if (purged > 0) {
                getThisLogger()
                    .debug(
                        ServiceMessages.PURGED_MESSAGES,
                        String.valueOf(purged),
                        getName());
            }

            getThisLogger()
                .trace(ServiceMessages.QUEUE_PURGE_COMPLETED, getName());

            return purged;
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

            do {
                final Optional<Serializable> message = _receive(timeout);

                if (!message.isPresent()) {
                    break;
                }

                messages.add(message.get());
                timeout = 0;
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

                _putBack(_receivedMessages);
                _receivedMessages.clear();
            }
        }

        private Optional<Serializable> _receive(
                final long timeout)
            throws ServiceClosedException
        {
            final long startMillis = (timeout > 0)? System
                .currentTimeMillis(): 0;
            Optional<Serializable> message;

            try {
                for (;;) {
                    synchronized (_receiverMutex) {
                        if (_closed) {
                            throw new ServiceClosedException();
                        }

                        message = _getMessage();

                        if (message.isPresent()) {
                            _receivedMessages.add(message.get());

                            break;
                        }
                    }

                    if (timeout == 0) {
                        break;
                    }

                    final long waitMillis;

                    if (timeout > 0) {
                        final long elapsedMillis = System
                            .currentTimeMillis() - startMillis;

                        if ((elapsedMillis < 0) || (elapsedMillis >= timeout)) {
                            return Optional.empty();
                        }

                        waitMillis = timeout - elapsedMillis;
                    } else {
                        waitMillis = 0;
                    }

                    _waitForMessages(waitMillis);
                }
            } catch (final InterruptedException exception) {
                throw new ServiceClosedException(exception);
            }

            return message;
        }

        private boolean _closed;
        private final List<Serializable> _receivedMessages = new LinkedList<>();
        private final Object _receiverMutex = new Object();
    }


    /**
     * Sender.
     */
    private final class Sender
        implements Queue.Sender, TimeoutMonitor.Client
    {
        /**
         * Constructs an instance.
         */
        Sender() {}

        /** {@inheritDoc}
         */
        @Override
        public void close()
        {
            synchronized (_senderMutex) {
                if (!_closed) {
                    final Optional<TimeoutMonitor> timeoutMonitor =
                        getTimeoutMonitor();

                    if (timeoutMonitor.isPresent()) {
                        timeoutMonitor.get().removeClient(this);
                    }

                    try {
                        if (isAutocommit()) {
                            commit();
                        } else {
                            rollback();
                        }
                    } catch (final ServiceClosedException exception) {
                        throw new InternalError(exception);
                    }

                    onSenderClosed(this);
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

                _addMessages(_messagesToSend);

                _messagesToSend.clear();
                _monitorTimeout(false);
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public void onTimeoutMonitoring()
        {
            synchronized (_senderMutex) {
                if (!_closed) {
                    if (_idle) {
                        try {
                            commit();
                        } catch (final ServiceClosedException exception) {
                            throw new InternalError(exception);
                        }
                    }

                    _idle = true;
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

                _messagesToSend.clear();
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
            synchronized (_senderMutex) {
                final boolean wasEmpty = _messagesToSend.isEmpty();

                Collections.addAll(_messagesToSend, messages);

                if (commit) {
                    commit();
                } else if (isAutocommit()
                           && (_messagesToSend.size()
                           >= getAutocommitThreshold())) {
                    commit();
                } else if (wasEmpty) {
                    _monitorTimeout(true);
                } else {
                    _idle = false;
                }
            }
        }

        private void _monitorTimeout(final boolean monitor)
        {
            final Optional<TimeoutMonitor> timeoutMonitor = getTimeoutMonitor();

            if (timeoutMonitor.isPresent()) {
                if (monitor) {
                    timeoutMonitor.get().addClient(this);
                } else {
                    timeoutMonitor.get().removeClient(this);
                }

                _idle = false;
            }
        }

        private boolean _closed;
        private boolean _idle;
        private final List<Serializable> _messagesToSend = new LinkedList<>();
        private final Object _senderMutex = new Object();
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
