/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TopicProxy.java 3982 2019-05-13 16:23:23Z SFB $
 */

package org.rvpf.base.som;

import java.io.Serializable;

import java.rmi.RemoteException;

import java.util.Optional;

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
 * Topic proxy.
 */
@ThreadSafe
public class TopicProxy
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
    TopicProxy(
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

    /**
     * Gets the topic info.
     *
     * @return The topic info.
     *
     * @throws SessionConnectFailedException When connect fails.
     */
    @Nonnull
    @CheckReturnValue
    public final TopicInfo getInfo()
        throws SessionConnectFailedException
    {
        try {
            return ((TopicServer) getFactory()).getInfo();
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

    /**
     * Builder.
     */
    public static class Builder
        extends SOMProxy.Builder
    {
        /** {@inheritDoc}
         */
        @Override
        public TopicProxy build()
        {
            if (!setUp()) {
                return null;
            }

            return new TopicProxy(
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
            return SOMServer.DEFAULT_TOPIC_BINDING_PREFIX;
        }
    }


    /**
     * Publisher.
     */
    public static final class Publisher
        extends TopicProxy
        implements PublisherSession
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
        Publisher(
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
        public void send(
                @Nonnull final Serializable[] messages)
            throws SessionException
        {
            try {
                _getPublisherSession().send(messages);
            } catch (final Exception exception) {
                disconnect();

                throw sessionException(exception);
            }
        }

        /** {@inheritDoc}
         */
        @Override
        protected Session createSession()
            throws RemoteException, SessionException
        {
            return ((TopicServer) getFactory())
                .createPublisherSession(getContextUUID(), getClientName());
        }

        /** {@inheritDoc}
         */
        @Override
        protected String sessionMode()
        {
            return BaseMessages.PUBLISH_MODE.toString();
        }

        private PublisherSession _getPublisherSession()
            throws SessionException
        {
            return (PublisherSession) getSession();
        }

        /**
         * Builder.
         */
        public static final class Builder
            extends TopicProxy.Builder
        {
            /** {@inheritDoc}
             */
            @Override
            public Publisher build()
            {
                if (!setUp()) {
                    return null;
                }

                return new Publisher(
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


    /**
     * Subscriber.
     */
    public static final class Subscriber
        extends TopicProxy
        implements SubscriberSession
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
        Subscriber(
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
        public Serializable[] receive(
                final int limit,
                final long timeout)
            throws SessionException
        {
            Serializable[] messages;

            try {
                do {    // Expects possible keep-alive timeouts.
                    messages = _getSubscriber().receive(limit, timeout);
                } while ((messages.length == 0) && (timeout < 0));
            } catch (final Exception exception) {
                disconnect();

                throw sessionException(exception);
            }

            return messages;
        }

        /** {@inheritDoc}
         */
        @Override
        protected Session createSession()
            throws RemoteException, SessionException
        {
            confirmTimeout();

            return ((TopicServer) getFactory())
                .createSubscriberSession(getContextUUID(), getClientName());
        }

        /** {@inheritDoc}
         */
        @Override
        protected String sessionMode()
        {
            return BaseMessages.SUBSCRIBE_MODE.toString();
        }

        private SubscriberSession _getSubscriber()
            throws SessionException
        {
            return (SubscriberSession) getSession();
        }

        /**
         * Builder.
         */
        public static final class Builder
            extends TopicProxy.Builder
        {
            /** {@inheritDoc}
             */
            @Override
            public Subscriber build()
            {
                if (!setUp()) {
                    return null;
                }

                return new Subscriber(
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
