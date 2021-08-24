/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMSender.java 4021 2019-05-24 13:01:15Z SFB $
 */

package org.rvpf.service.som;

import java.io.Serializable;

import java.net.URI;

import java.rmi.RemoteException;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.rmi.SessionConnectFailedException;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.som.QueueInfo;
import org.rvpf.base.som.QueueProxy;
import org.rvpf.service.ServiceMessages;
import org.rvpf.som.SOMServerImpl;
import org.rvpf.som.queue.QueueServerImpl;
import org.rvpf.som.queue.SenderWrapper;

/**
 * SOM sender.
 */
public interface SOMSender
    extends SOMEndPoint
{
    /**
     * Commits uncommitted messages.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean commit();

    /**
     * Gets the queue info.
     *
     * @return The queue info (empty on failure).
     */
    @Nonnull
    @CheckReturnValue
    Optional<QueueInfo> getInfo();

    /**
     * Rolls back uncommitted messages
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean rollback();

    /**
     * Sends messages.
     *
     * @param messages The messages.
     * @param commit If true, commits.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean send(@Nonnull Serializable[] messages, boolean commit);

    /**
     * Client.
     */
    final class Client
        extends SOMEndPoint.Abstract
        implements SOMSender
    {
        /**
         * Constructs an instance.
         *
         * @param sender The sender proxy.
         */
        Client(@Nonnull final QueueProxy.Sender sender)
        {
            _sender = sender;
        }

        /** {@inheritDoc}
         */
        @Override
        public void close()
        {
            _sender.disconnect();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean commit()
        {
            clearException();

            try {
                _sender.commit();
            } catch (final SessionException exception) {
                setException(exception);
                _LOGGER
                    .warn(ServiceMessages.COMMIT_FAILED, exception.toString());

                return false;
            }

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<QueueInfo> getInfo()
        {
            clearException();

            try {
                return Optional.of(_sender.getInfo());
            } catch (final SessionConnectFailedException exception) {
                setException(exception);
                _LOGGER
                    .trace(
                        ServiceMessages.CONNECTION_FAILED,
                        _sender.getServerName(),
                        exception.getMessage());

                return Optional.empty();
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public String getSOMName()
        {
            return _sender.getSOMName();
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
            return Optional.of(_sender.getContext().getServerURI());
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isClosed()
        {
            return _sender.isClosed();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isOpen()
        {
            return _sender.isConnected();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isRemote()
        {
            return _sender.isRemote();
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
                _sender.connect();
            } catch (final SessionConnectFailedException exception) {
                setException(exception);
                _LOGGER
                    .trace(
                        ServiceMessages.CONNECTION_FAILED,
                        _sender.getServerName(),
                        exception.getMessage());

                return false;
            }

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean rollback()
        {
            clearException();

            try {
                _sender.rollback();
            } catch (final SessionException exception) {
                setException(exception);

                final Exception exceptionCause = (Exception) exception
                    .getCause();

                if (exceptionCause instanceof RemoteException) {
                    _LOGGER
                        .warn(
                            ServiceMessages.ROLLBACK_FAILED,
                            exceptionCause.getMessage());
                }

                return false;
            }

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean send(final Serializable[] messages, final boolean commit)
        {
            clearException();

            try {
                _sender.send(messages, commit);
            } catch (final SessionConnectFailedException exception) {
                setException(exception);

                return false;
            } catch (final SessionException exception) {
                setException(exception);

                final Exception exceptionCause = (Exception) exception
                    .getCause();

                if (exceptionCause instanceof InterruptedException) {
                    setException(new ServiceClosedException(exceptionCause));
                } else {
                    _LOGGER
                        .warn(
                            ServiceMessages.SEND_FAILED,
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
            _sender.tearDown();
        }

        /** {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return _sender.getServerName();
        }

        private static final Logger _LOGGER = Logger.getInstance(Client.class);

        private final QueueProxy.Sender _sender;
    }


    /**
     * Server.
     */
    final class Server
        extends SOMEndPoint.Abstract
        implements SOMSender
    {
        /**
         * Constructs an instance.
         *
         * @param sender The sender wrapper.
         */
        Server(@Nonnull final SenderWrapper sender)
        {
            _sender = sender;
        }

        /** {@inheritDoc}
         */
        @Override
        public void close()
        {
            _sender.close();
            _sender.closeServer();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean commit()
        {
            clearException();

            try {
                _sender.commit();
            } catch (final ServiceClosedException exception) {
                setException(exception);

                return false;
            }

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<QueueInfo> getInfo()
        {
            return Optional.of(_sender.getInfo());
        }

        /** {@inheritDoc}
         */
        @Override
        public String getSOMName()
        {
            return _sender.getServerName();
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<QueueServerImpl> getServer()
        {
            return Optional.of(_sender.getQueueServer());
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
            return _sender.isServerClosed();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isOpen()
        {
            return !_sender.isServerClosed();
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
        public boolean rollback()
        {
            clearException();

            try {
                _sender.rollback();
            } catch (final ServiceClosedException exception) {
                setException(exception);

                return false;
            }

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean send(final Serializable[] messages, final boolean commit)
        {
            clearException();

            try {
                _sender.send(messages, commit);
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
            _sender.tearDownServer();
        }

        /** {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return _sender.toString();
        }

        private final SenderWrapper _sender;
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
