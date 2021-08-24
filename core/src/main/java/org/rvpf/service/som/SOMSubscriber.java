/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMSubscriber.java 4021 2019-05-24 13:01:15Z SFB $
 */

package org.rvpf.service.som;

import java.io.Serializable;

import java.net.URI;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.rmi.SessionConnectFailedException;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.som.TopicInfo;
import org.rvpf.base.som.TopicProxy;
import org.rvpf.service.ServiceMessages;
import org.rvpf.som.SOMServerImpl;
import org.rvpf.som.topic.SubscriberWrapper;
import org.rvpf.som.topic.TopicServerImpl;

/**
 * SOM subscriber.
 */
public interface SOMSubscriber
    extends SOMEndPoint
{
    /**
     * Gets the topic info.
     *
     * @return The topic info (empty on failure).
     */
    @Nonnull
    @CheckReturnValue
    Optional<TopicInfo> getInfo();

    /**
     * Receives messages.
     *
     * @param limit The maximum number of messages.
     * @param timeout A time limit in millis to wait for the first message
     *                (negative for infinite).
     *
     * @return The messages (null on failure).
     */
    @Nullable
    @CheckReturnValue
    Serializable[] receive(int limit, long timeout);

    /**
     * Client.
     */
    final class Client
        extends SOMEndPoint.Abstract
        implements SOMSubscriber
    {
        /**
         * Constructs an instance.
         *
         * @param subscriber The subscriber proxy.
         */
        Client(@Nonnull final TopicProxy.Subscriber subscriber)
        {
            _subscriber = subscriber;
        }

        /** {@inheritDoc}
         */
        @Override
        public void close()
        {
            _subscriber.disconnect();
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<TopicInfo> getInfo()
        {
            clearException();

            try {
                return Optional.of(_subscriber.getInfo());
            } catch (final SessionConnectFailedException exception) {
                setException(exception);
                Logger
                    .getInstance(getClass())
                    .trace(
                        ServiceMessages.CONNECTION_FAILED,
                        _subscriber.getServerName(),
                        exception.getMessage());

                return Optional.empty();
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public String getSOMName()
        {
            return _subscriber.getSOMName();
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<SOMServerImpl> getServer()
        {
            return Optional.empty();
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<URI> getServerURI()
        {
            return Optional.of(_subscriber.getContext().getServerURI());
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isClosed()
        {
            return _subscriber.isClosed();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isOpen()
        {
            return _subscriber.isConnected();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isRemote()
        {
            return _subscriber.isRemote();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isServer()
        {
            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean open()
        {
            clearException();

            try {
                _subscriber.connect();
            } catch (final SessionConnectFailedException exception) {
                setException(exception);
                Logger
                    .getInstance(getClass())
                    .trace(
                        ServiceMessages.CONNECTION_FAILED,
                        _subscriber.getServerName(),
                        exception.getMessage());

                return false;
            }

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public Serializable[] receive(final int limit, final long timeout)
        {
            clearException();

            try {
                return _subscriber.receive(limit, timeout);
            } catch (final SessionConnectFailedException
                     |ServiceClosedException exception) {
                setException(exception);

                return null;
            } catch (final SessionException exception) {
                setException(exception);

                final Exception exceptionCause = (Exception) exception
                    .getCause();

                Logger
                    .getInstance(getClass())
                    .warn(
                        ServiceMessages.RECEIVE_FAILED,
                        (exceptionCause != null)? exceptionCause: exception);

                return null;
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public void tearDown()
        {
            _subscriber.tearDown();
        }

        /** {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return _subscriber.getServerName();
        }

        private final TopicProxy.Subscriber _subscriber;
    }


    /**
     * Server.
     */
    final class Server
        extends SOMEndPoint.Abstract
        implements SOMSubscriber
    {
        /**
         * Constructs an instance.
         *
         * @param subscriber The subscriber wrapper.
         */
        Server(@Nonnull final SubscriberWrapper subscriber)
        {
            _subscriber = subscriber;
        }

        /** {@inheritDoc}
         */
        @Override
        public void close()
        {
            _subscriber.close();
            _subscriber.closeServer();
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<TopicInfo> getInfo()
        {
            return Optional.of(_subscriber.getInfo());
        }

        /** {@inheritDoc}
         */
        @Override
        public String getSOMName()
        {
            return _subscriber.getServerName();
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<TopicServerImpl> getServer()
        {
            return Optional.of(_subscriber.getTopicServer());
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<URI> getServerURI()
        {
            return Optional.empty();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isClosed()
        {
            return _subscriber.isServerClosed();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isOpen()
        {
            return !_subscriber.isServerClosed();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isRemote()
        {
            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isServer()
        {
            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean open()
        {
            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public Serializable[] receive(final int limit, final long timeout)
        {
            clearException();

            try {
                return _subscriber.receive(limit, timeout);
            } catch (final ServiceClosedException exception) {
                setException(exception);

                return null;
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public void tearDown()
        {
            close();
            _subscriber.tearDownServer();
        }

        /** {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return _subscriber.toString();
        }

        private final SubscriberWrapper _subscriber;
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
