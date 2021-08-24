/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMPublisher.java 4021 2019-05-24 13:01:15Z SFB $
 */

package org.rvpf.service.som;

import java.io.Serializable;

import java.net.URI;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.rmi.SessionConnectFailedException;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.som.TopicInfo;
import org.rvpf.base.som.TopicProxy;
import org.rvpf.service.ServiceMessages;
import org.rvpf.som.SOMServerImpl;
import org.rvpf.som.topic.PublisherWrapper;
import org.rvpf.som.topic.TopicServerImpl;

/**
 * SOM publisher.
 */
public interface SOMPublisher
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
     * Sends messages.
     *
     * @param messages The messages.
     *
     * @return True value on success.
     */
    @CheckReturnValue
    boolean send(@Nonnull Serializable[] messages);

    /**
     * Client.
     */
    final class Client
        extends SOMEndPoint.Abstract
        implements SOMPublisher
    {
        /**
         * Constructs an instance.
         *
         * @param publisher The publisher proxy.
         */
        Client(@Nonnull final TopicProxy.Publisher publisher)
        {
            _publisher = publisher;
        }

        /** {@inheritDoc}
         */
        @Override
        public void close()
        {
            _publisher.disconnect();
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<TopicInfo> getInfo()
        {
            clearException();

            try {
                return Optional.of(_publisher.getInfo());
            } catch (final SessionConnectFailedException exception) {
                setException(exception);
                Logger
                    .getInstance(getClass())
                    .trace(
                        ServiceMessages.CONNECTION_FAILED,
                        _publisher.getServerName(),
                        exception.getMessage());

                return Optional.empty();
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public String getSOMName()
        {
            return _publisher.getSOMName();
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
            return Optional.of(_publisher.getContext().getServerURI());
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isClosed()
        {
            return _publisher.isClosed();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isOpen()
        {
            return _publisher.isConnected();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isRemote()
        {
            return _publisher.isRemote();
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
                _publisher.connect();
            } catch (final SessionConnectFailedException exception) {
                setException(exception);
                Logger
                    .getInstance(getClass())
                    .trace(
                        ServiceMessages.CONNECTION_FAILED,
                        _publisher.getServerName(),
                        exception.getMessage());

                return false;
            }

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean send(final Serializable[] messages)
        {
            clearException();

            try {
                _publisher.send(messages);
            } catch (final SessionConnectFailedException exception) {
                setException(exception);

                return false;
            } catch (final SessionException exception) {
                setException(exception);

                final Exception exceptionCause = (Exception) exception
                    .getCause();

                if (!(exceptionCause instanceof InterruptedException)) {
                    Logger
                        .getInstance(getClass())
                        .warn(
                            ServiceMessages.PUBLISH_FAILED,
                            (exceptionCause != null)
                            ? exceptionCause: exception);
                }

                return false;
            }

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public void tearDown()
        {
            _publisher.tearDown();
        }

        /** {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return _publisher.getServerName();
        }

        private final TopicProxy.Publisher _publisher;
    }


    /**
     * Server.
     */
    final class Server
        extends SOMEndPoint.Abstract
        implements SOMPublisher
    {
        /**
         * Constructs an instance.
         *
         * @param publisher The publisher wrapper.
         */
        Server(@Nonnull final PublisherWrapper publisher)
        {
            _publisher = publisher;
        }

        /** {@inheritDoc}
         */
        @Override
        public void close()
        {
            _publisher.close();
            _publisher.closeServer();
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<TopicInfo> getInfo()
        {
            return Optional.of(_publisher.getInfo());
        }

        /** {@inheritDoc}
         */
        @Override
        public String getSOMName()
        {
            return _publisher.getServerName();
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<TopicServerImpl> getServer()
        {
            return Optional.of(_publisher.getTopicServer());
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
            return _publisher.isServerClosed();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isOpen()
        {
            return !_publisher.isServerClosed();
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
        public boolean send(final Serializable[] messages)
        {
            clearException();

            try {
                _publisher.send(messages);
            } catch (final ServiceClosedException exception) {
                setException(exception);

                return false;
            }

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public void tearDown()
        {
            close();
            _publisher.tearDownServer();
        }

        /** {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return _publisher.toString();
        }

        private final PublisherWrapper _publisher;
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
