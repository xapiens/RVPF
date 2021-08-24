/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Queue.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.som.queue;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.som.QueueInfo;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.TimeoutMonitor;
import org.rvpf.base.util.container.ConcurrentIdentityHashSet;
import org.rvpf.base.util.container.KeyedValues;
import org.rvpf.service.ServiceMessages;

/**
 * Queue.
 */
interface Queue
{
    /**
     * Gets the info.
     *
     * @return The info.
     */
    @Nonnull
    @CheckReturnValue
    QueueInfo getInfo();

    /**
     * Returns a new receiver.
     *
     * @return The new receiver.
     */
    @Nonnull
    @CheckReturnValue
    Receiver newReceiver();

    /**
     * Returns a new sender.
     *
     * @return The new sender.
     */
    @Nonnull
    @CheckReturnValue
    Sender newSender();

    /**
     * Sets up this queue.
     *
     * @param somProperties The SOM properties.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUp(@Nonnull KeyedValues somProperties);

    /**
     * Tears down what has been set up.
     */
    void tearDown();

    /**
     * Receiver.
     */
    interface Receiver
    {
        /**
         * Closes this receiver.
         */
        void close();

        /**
         * Commits.
         *
         * @throws ServiceClosedException When the service is closed.
         */
        void commit()
            throws ServiceClosedException;

        /**
         * Purges the queue.
         *
         * @return The number of messages purged.
         *
         * @throws ServiceClosedException When the service is closed.
         */
        @CheckReturnValue
        long purge()
            throws ServiceClosedException;

        /**
         * Receives messages.
         *
         * @param limit The maximum number of messages.
         * @param timeout A time limit in millis to wait for the first message
         *                (negative for infinite).
         *
         * @return The messages.
         *
         * @throws ServiceClosedException When the service is closed.
         */
        @Nonnull
        @CheckReturnValue
        Serializable[] receive(
                int limit,
                long timeout)
            throws ServiceClosedException;

        /**
         * Rollbacks.
         *
         * @throws ServiceClosedException When the service is closed.
         */
        void rollback()
            throws ServiceClosedException;
    }


    /**
     * Sender.
     */
    interface Sender
    {
        /**
         * Closes.
         */
        void close();

        /**
         * Commits.
         *
         * @throws ServiceClosedException When the service is closed.
         */
        void commit()
            throws ServiceClosedException;

        /**
         * Rollbacks.
         *
         * @throws ServiceClosedException When the service is closed.
         */
        void rollback()
            throws ServiceClosedException;

        /**
         * Sends messages.
         *
         * @param messages The messages.
         * @param commit Commits if true.
         *
         * @throws ServiceClosedException When the service is closed.
         */
        void send(
                @Nonnull Serializable[] messages,
                boolean commit)
            throws ServiceClosedException;
    }


    /**
     * Abstract queue.
     */
    abstract class Abstract
        implements Queue
    {
        /**
         * Constructs an instance.
         *
         * @param name The queue name.
         * @param stats The queue stats.
         */
        protected Abstract(
                @Nonnull final String name,
                @Nonnull final QueueStats stats)
        {
            _name = name;
            _stats = stats;
        }

        /**
         * Called when a sender is closed.
         *
         * @param sender The sender.
         */
        public final void onSenderClosed(@Nonnull final Sender sender)
        {
            if (_senders.remove(sender)) {
                getInfo().updateSenderCount(-1);
            }
        }

        /** {@inheritDoc}
         */
        @Override
        @OverridingMethodsMustInvokeSuper
        public boolean setUp(final KeyedValues somProperties)
        {
            ElapsedTime autocommitTimeout = somProperties
                .getElapsed(
                    AUTOCOMMIT_TIMEOUT_PROPERTY,
                    Optional.empty(),
                    Optional.empty())
                .orElse(null);

            _autocommitThreshold = somProperties
                .getInt(AUTOCOMMIT_THRESHOLD_PROPERTY, -1);

            _autocommit = somProperties
                .getBoolean(
                    AUTOCOMMIT_PROPERTY,
                    (_autocommitThreshold > 0) || (autocommitTimeout != null));
            getThisLogger()
                .debug(_autocommit
                       ? ServiceMessages.AUTOCOMMIT_ENABLED
                       : ServiceMessages.AUTOCOMMIT_DISABLED);

            if (_autocommit) {
                if (_autocommitThreshold <= 0) {
                    _autocommitThreshold = Integer.MAX_VALUE;
                }

                if (_autocommitThreshold < Integer.MAX_VALUE) {
                    getThisLogger()
                        .debug(
                            ServiceMessages.AUTOCOMMIT_THRESHOLD,
                            String.valueOf(_autocommitThreshold));
                }

                if (autocommitTimeout != null) {
                    if (autocommitTimeout.toMillis() < 1) {
                        autocommitTimeout = ElapsedTime.fromMillis(1);
                    }

                    getThisLogger()
                        .debug(
                            ServiceMessages.AUTOCOMMIT_TIMEOUT,
                            autocommitTimeout);
                    _timeoutMonitor = new TimeoutMonitor(autocommitTimeout);
                }
            }

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public void tearDown()
        {
            if (_timeoutMonitor != null) {
                _timeoutMonitor = null;
                TimeoutMonitor.shutdown();
            }

            for (final Sender sender: new ArrayList<>(_senders)) {
                sender.close();
            }

            Require.success(_senders.isEmpty());

            _closeReceiver();
        }

        /**
         * Gets the autocommit threshold.
         *
         * @return The autocommit threshold.
         */
        @CheckReturnValue
        protected int getAutocommitThreshold()
        {
            return _autocommitThreshold;
        }

        /**
         * Gets the name.
         *
         * @return The name.
         */
        @Nonnull
        @CheckReturnValue
        protected String getName()
        {
            return _name;
        }

        /**
         * Gets the stats.
         *
         * @return The stats.
         */
        @Nonnull
        @CheckReturnValue
        protected QueueStats getStats()
        {
            return _stats;
        }

        /**
         * Gets the logger.
         *
         * @return The logger.
         */
        @Nonnull
        @CheckReturnValue
        protected final Logger getThisLogger()
        {
            return _logger;
        }

        /**
         * Gets the timeout monitor.
         *
         * @return The optional timeout monitor.
         */
        @Nonnull
        @CheckReturnValue
        protected Optional<TimeoutMonitor> getTimeoutMonitor()
        {
            return Optional.ofNullable(_timeoutMonitor);
        }

        /**
         * Asks if the queue has a receiver.
         *
         * @return True if the queue has a receiver.
         */
        @CheckReturnValue
        protected boolean hasReceiver()
        {
            return _receiver != null;
        }

        /**
         * Asks if 'autocommit' is enabled.
         *
         * @return True if 'autocommit' is enabled.
         */
        @CheckReturnValue
        protected boolean isAutocommit()
        {
            return _autocommit;
        }

        /**
         * Called on a new receiver.
         *
         * @param receiver The new receiver.
         */
        protected final synchronized void onNewReceiver(
                @Nonnull final Receiver receiver)
        {
            _closeReceiver();

            _receiver = receiver;
            getInfo().setReceiverConnectTime(Optional.of(DateTime.now()));
        }

        /**
         * Called on a new sender.
         *
         * @param sender The new sender.
         */
        protected final void onNewSender(@Nonnull final Sender sender)
        {
            _senders.add(sender);
            getInfo().updateSenderCount(+1);
        }

        /**
         * Called when the receiver is closed.
         *
         * @param receiver The receiver.
         */
        protected synchronized void onReceiverClosed(
                @Nonnull final Receiver receiver)
        {
            Require.success(receiver == _receiver);

            _receiver = null;
            getInfo().setReceiverConnectTime(Optional.empty());
        }

        private synchronized void _closeReceiver()
        {
            final Receiver receiver = _receiver;

            if (receiver != null) {
                receiver.close();
                Require.success(_receiver == null);
            }
        }

        /**
         * True will automatically commit partial sends on closed sessions and
         * service restarts.
         */
        public static final String AUTOCOMMIT_PROPERTY = "autocommit";

        /**
         * A positive value will commit messages sent by a sender to the queue
         * as soon as their count reaches the specified number.
         */
        public static final String AUTOCOMMIT_THRESHOLD_PROPERTY =
            "autocommit.threshold";

        /**
         * A positive elapsed time value will commit messages sent by a sender
         * to the queue when the sender has been inactive for that length of
         * time.
         */
        public static final String AUTOCOMMIT_TIMEOUT_PROPERTY =
            "autocommit.timeout";

        private boolean _autocommit;
        private int _autocommitThreshold;
        private final Logger _logger = Logger.getInstance(getClass());
        private final String _name;
        private volatile Receiver _receiver;
        private final Set<Sender> _senders = new ConcurrentIdentityHashSet<>();
        private final QueueStats _stats;
        private volatile TimeoutMonitor _timeoutMonitor;
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
