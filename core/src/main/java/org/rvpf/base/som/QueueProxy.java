/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: QueueProxy.java 4043 2019-06-02 15:05:37Z SFB $
 */

package org.rvpf.base.som;

import java.io.Serializable;

import java.rmi.RemoteException;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.rmi.Session;
import org.rvpf.base.rmi.SessionClientContext;
import org.rvpf.base.rmi.SessionConnectFailedException;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.util.LoginInfo;
import org.rvpf.base.util.container.KeyedGroups;

/**
 * Queue proxy.
 */
@ThreadSafe
public class QueueProxy
    extends SOMProxy
{
    /**
     * Constructs an instance.
     *
     * @param clientName A descriptive name for the client.
     * @param loginInfo The optional login informations.
     * @param context The session client context.
     * @param listener The optional listener.
     * @param autoconnect The autoconnect indicator.
     * @param timeout The optional timeout.
     */
    QueueProxy(
            @Nonnull final String clientName,
            @Nonnull final Optional<LoginInfo> loginInfo,
            @Nonnull final SessionClientContext context,
            @Nonnull final Optional<Listener> listener,
            final boolean autoconnect,
            @Nonnull final Optional<ElapsedTime> timeout)
    {
        super(clientName, loginInfo, context, listener, autoconnect, timeout);
    }

    /**
     * Returns a new builder.
     *
     * @return The new builder.
     */
    @Nonnull
    @CheckReturnValue
    public static Builder newBuilder()
    {
        return new Builder();
    }

    /** {@inheritDoc}
     */
    @Override
    public final void connect()
        throws SessionConnectFailedException
    {
        super.connect();

        _inTransaction.set(false);
    }

    /**
     * Gets the queue info.
     *
     * @return The queue info.
     *
     * @throws SessionConnectFailedException When connect fails.
     */
    @Nonnull
    @CheckReturnValue
    public final QueueInfo getInfo()
        throws SessionConnectFailedException
    {
        try {
            return ((QueueServer) getFactory()).getInfo();
        } catch (final RemoteException exception) {
            throw new RuntimeException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected Session createSession()
        throws RemoteException, SessionException
    {
        throw new UnsupportedOperationException();
    }

    final AtomicBoolean _inTransaction = new AtomicBoolean();

    /**
     * Builder.
     */
    public static class Builder
        extends SOMProxy.Builder
    {
        /** {@inheritDoc}
         */
        @Override
        public QueueProxy build()
        {
            if (!setUp()) {
                return null;
            }

            return new QueueProxy(
                getClientName(),
                getLoginInfo(),
                getContext(),
                getListener(),
                isAutoconnect(),
                Optional.empty());
        }

        /** {@inheritDoc}
         */
        @Override
        public Builder prepare(
                final KeyedGroups configProperties,
                final KeyedGroups somProperties,
                final String clientName,
                final Logger clientLogger)
        {
            return (Builder) super
                .prepare(
                    configProperties,
                    somProperties,
                    clientName,
                    clientLogger);
        }

        /** {@inheritDoc}
         */
        @Override
        protected String getDefaultBindPrefix()
        {
            return SOMServer.DEFAULT_QUEUE_BINDING_PREFIX;
        }
    }


    /**
     * Receiver.
     */
    public static final class Receiver
        extends QueueProxy
        implements ReceiverSession
    {
        /**
         * Constructs an instance.
         *
         * @param clientName A descriptive name for the client.
         * @param loginInfo The optional login informations.
         * @param context The session client context.
         * @param listener The optional listener.
         * @param autoconnect The autoconnect indicator.
         * @param timeout The optional timeout.
         */
        Receiver(
                @Nonnull final String clientName,
                @Nonnull final Optional<LoginInfo> loginInfo,
                @Nonnull final SessionClientContext context,
                @Nonnull final Optional<Listener> listener,
                final boolean autoconnect,
                @Nonnull final Optional<ElapsedTime> timeout)
        {
            super(
                clientName,
                loginInfo,
                context,
                listener,
                autoconnect,
                timeout);
        }

        /**
         * Returns a new builder.
         *
         * @return The new builder.
         */
        @Nonnull
        @CheckReturnValue
        public static Builder newBuilder()
        {
            return new Builder();
        }

        /** {@inheritDoc}
         */
        @Override
        public void commit()
            throws SessionException
        {
            if (_inTransaction.compareAndSet(true, false)) {
                try {
                    _getReceiverSession().commit();
                } catch (final Exception exception) {
                    disconnect();

                    throw sessionException(exception);
                }
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public long purge()
            throws SessionException
        {
            final long purged;

            try {
                purged = _getReceiverSession().purge();
            } catch (final Exception exception) {
                disconnect();

                throw sessionException(exception);
            }

            _inTransaction.set(false);

            return purged;
        }

        /** {@inheritDoc}
         */
        @Override
        public Serializable[] receive(
                final int limit,
                final long timeout)
            throws SessionException
        {
            Serializable[] messages;

            try {
                do {    // Expects possible keep-alive timeouts.
                    messages = _getReceiverSession().receive(limit, timeout);
                } while ((messages.length == 0) && (timeout < 0));
            } catch (final Exception exception) {
                throw sessionException(exception);
            }

            _inTransaction.set(true);

            return messages;
        }

        /** {@inheritDoc}
         */
        @Override
        public void rollback()
            throws SessionException
        {
            if (_inTransaction.compareAndSet(true, false)) {
                try {
                    _getReceiverSession().rollback();
                } catch (final Exception exception) {
                    disconnect();

                    throw sessionException(exception);
                }
            }
        }

        /** {@inheritDoc}
         */
        @Override
        protected Session createSession()
            throws RemoteException, SessionException
        {
            confirmTimeout();

            return ((QueueServer) getFactory())
                .createReceiverSession(getContextUUID(), getClientName());
        }

        /** {@inheritDoc}
         */
        @Override
        protected String sessionMode()
        {
            return BaseMessages.RECEIVE_MODE.toString();
        }

        private ReceiverSession _getReceiverSession()
            throws SessionException
        {
            return (ReceiverSession) getSession();
        }

        /**
         * Builder.
         */
        public static final class Builder
            extends QueueProxy.Builder
        {
            /**
             * Constructs an instance.
             */
            Builder() {}

            /** {@inheritDoc}
             */
            @Override
            public Receiver build()
            {
                if (!setUp()) {
                    return null;
                }

                return new Receiver(
                    getClientName(),
                    getLoginInfo(),
                    getContext(),
                    getListener(),
                    isAutoconnect(),
                    getTimeout());
            }

            /** {@inheritDoc}
             */
            @Override
            public Builder prepare(
                    final KeyedGroups configProperties,
                    final KeyedGroups queueProperties,
                    final String clientName,
                    final Logger clientLogger)
            {
                final Optional<ElapsedTime> timeout = queueProperties
                    .getElapsed(
                        TIMEOUT_PROPERTY,
                        Optional.empty(),
                        Optional.of(ElapsedTime.EMPTY));

                return (Builder) super
                    .prepare(
                        configProperties,
                        queueProperties,
                        clientName,
                        clientLogger)
                    .setTimeout(timeout);
            }

            /** {@inheritDoc}
             */
            @Override
            public Builder setAutoconnect(final boolean autoconnect)
            {
                super.setAutoconnect(autoconnect);

                return this;
            }
        }
    }


    /**
     * Sender.
     */
    public static final class Sender
        extends QueueProxy
        implements SenderSession
    {
        /**
         * Constructs an instance.
         *
         * @param clientName A descriptive name for the client.
         * @param loginInfo The optional login informations.
         * @param context The session client context.
         * @param listener The optional listener.
         * @param autoconnect The autoconnect indicator.
         */
        Sender(
                @Nonnull final String clientName,
                @Nonnull final Optional<LoginInfo> loginInfo,
                @Nonnull final SessionClientContext context,
                @Nonnull final Optional<Listener> listener,
                final boolean autoconnect)
        {
            super(
                clientName,
                loginInfo,
                context,
                listener,
                autoconnect,
                Optional.empty());
        }

        /**
         * Returns a new builder.
         *
         * @return The new builder.
         */
        @Nonnull
        @CheckReturnValue
        public static Builder newBuilder()
        {
            return new Builder();
        }

        /** {@inheritDoc}
         */
        @Override
        public void commit()
            throws SessionException
        {
            if (_inTransaction.compareAndSet(true, false)) {
                try {
                    _getSenderSession().commit();
                } catch (final Exception exception) {
                    disconnect();

                    throw sessionException(exception);
                }
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public void rollback()
            throws SessionException
        {
            if (_inTransaction.compareAndSet(true, false)) {
                try {
                    _getSenderSession().rollback();
                } catch (final Exception exception) {
                    disconnect();

                    throw sessionException(exception);
                }
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public void send(
                @Nonnull final Serializable[] messages,
                final boolean commit)
            throws SessionException
        {
            try {
                _getSenderSession().send(messages, commit);
            } catch (final Exception exception) {
                disconnect();

                throw sessionException(exception);
            }

            _inTransaction.set(true);
        }

        /** {@inheritDoc}
         */
        @Override
        protected Session createSession()
            throws RemoteException, SessionException
        {
            return ((QueueServer) getFactory())
                .createSenderSession(getContextUUID(), getClientName());
        }

        /** {@inheritDoc}
         */
        @Override
        protected String sessionMode()
        {
            return BaseMessages.SEND_MODE.toString();
        }

        private SenderSession _getSenderSession()
            throws SessionException
        {
            return (SenderSession) getSession();
        }

        /**
         * Builder.
         */
        public static final class Builder
            extends QueueProxy.Builder
        {
            /** {@inheritDoc}
             */
            @Override
            public Sender build()
            {
                if (!setUp()) {
                    return null;
                }

                return new Sender(
                    getClientName(),
                    getLoginInfo(),
                    getContext(),
                    getListener(),
                    isAutoconnect());
            }

            /** {@inheritDoc}
             */
            @Override
            public Builder prepare(
                    final KeyedGroups configProperties,
                    final KeyedGroups somProperties,
                    final String clientName,
                    final Logger clientLogger)
            {
                return (Builder) super
                    .prepare(
                        configProperties,
                        somProperties,
                        clientName,
                        clientLogger);
            }

            /** {@inheritDoc}
             */
            @Override
            public Builder setAutoconnect(final boolean autoconnect)
            {
                super.setAutoconnect(autoconnect);

                return this;
            }
        }
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
