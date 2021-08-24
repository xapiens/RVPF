/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMReceiver.java 4081 2019-06-14 14:54:30Z SFB $
 */

package org.rvpf.service.som;

import java.io.Serializable;

import java.net.URI;

import java.rmi.RemoteException;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.rmi.SessionConnectFailedException;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.som.QueueInfo;
import org.rvpf.base.som.QueueProxy;
import org.rvpf.service.ServiceMessages;
import org.rvpf.som.SOMServerImpl;
import org.rvpf.som.queue.QueueServerImpl;
import org.rvpf.som.queue.ReceiverWrapper;

/**
 * SOM receiver.
 */
public interface SOMReceiver
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
     * Purges the queue.
     *
     * @return The number of messages purged.
     */
    @CheckReturnValue
    long purge();

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
     * Rolls back uncommitted messages
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean rollback();

    /**
     * Client.
     */
    final class Client
        extends SOMEndPoint.Abstract
        implements SOMReceiver
    {
        /**
         * Constructs an instance.
         *
         * @param receiver The receiver proxy.
         */
        Client(@Nonnull final QueueProxy.Receiver receiver)
        {
            _receiver = receiver;
        }

        /** {@inheritDoc}
         */
        @Override
        public void close()
        {
            _receiver.disconnect();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean commit()
        {
            clearException();

            try {
                _receiver.commit();
            } catch (final SessionException exception) {
                setException(exception);
                _LOGGER
                    .warn(
                        ServiceMessages.COMMIT_FAILED,
                        exception.getMessage());

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
                return Optional.of(_receiver.getInfo());
            } catch (final SessionConnectFailedException exception) {
                setException(exception);
                _LOGGER
                    .trace(
                        ServiceMessages.CONNECTION_FAILED,
                        _receiver.getServerName(),
                        exception.getMessage());

                return Optional.empty();
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public String getSOMName()
        {
            return _receiver.getSOMName();
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
            return Optional.of(_receiver.getContext().getServerURI());
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isClosed()
        {
            return _receiver.isClosed();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isOpen()
        {
            return _receiver.isConnected();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isRemote()
        {
            return _receiver.isRemote();
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
                _receiver.connect();
            } catch (final SessionConnectFailedException exception) {
                setException(exception);
                _LOGGER
                    .trace(
                        ServiceMessages.CONNECTION_FAILED,
                        _receiver.getServerName(),
                        exception.getMessage());

                return false;
            }

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public long purge()
        {
            clearException();

            try {
                return _receiver.purge();
            } catch (final SessionException exception) {
                setException(exception);

                throw new RuntimeException(exception);
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public Serializable[] receive(final int limit, final long timeout)
        {
            clearException();

            try {
                return _receiver.receive(limit, timeout);
            } catch (final SessionConnectFailedException
                     |ServiceClosedException exception) {
                setException(exception);

                return null;
            } catch (final SessionException exception) {
                setException(exception);

                final Exception exceptionCause = (Exception) exception
                    .getCause();

                _LOGGER
                    .warn(
                        ServiceMessages.RECEIVE_FAILED,
                        (exceptionCause != null)? exceptionCause: exception);

                return null;
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean rollback()
        {
            clearException();

            try {
                _receiver.rollback();
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
        public void tearDown()
        {
            _receiver.tearDown();
        }

        /** {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return _receiver.getServerName();
        }

        private static final Logger _LOGGER = Logger.getInstance(Client.class);

        private final QueueProxy.Receiver _receiver;
    }


    /**
     * Server.
     */
    final class Server
        extends SOMEndPoint.Abstract
        implements SOMReceiver
    {
        /**
         * Constructs an instance.
         *
         * @param receiver The receiver wrapper.
         */
        Server(@Nonnull final ReceiverWrapper receiver)
        {
            _receiver = receiver;
        }

        /** {@inheritDoc}
         */
        @Override
        public void close()
        {
            _receiver.close();
            _receiver.closeServer();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean commit()
        {
            clearException();

            try {
                _receiver.commit();
            } catch (final ServiceClosedException exception) {
                setException(exception);
                Logger
                    .getInstance(getClass())
                    .info(ServiceMessages.SERVICE_CLOSED);

                return false;
            }

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<QueueInfo> getInfo()
        {
            return Optional.of(_receiver.getInfo());
        }

        /** {@inheritDoc}
         */
        @Override
        public String getSOMName()
        {
            return _receiver.getServerName();
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<QueueServerImpl> getServer()
        {
            return Optional.of(_receiver.getQueueServer());
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
            return _receiver.isServerClosed();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isOpen()
        {
            return !_receiver.isServerClosed();
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
        public long purge()
        {
            clearException();

            try {
                return _receiver.purge();
            } catch (final ServiceClosedException exception) {
                setException(exception);
                Logger
                    .getInstance(getClass())
                    .info(ServiceMessages.SERVICE_CLOSED);

                return 0;
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public Serializable[] receive(final int limit, final long timeout)
        {
            clearException();

            try {
                return _receiver.receive(limit, timeout);
            } catch (final ServiceClosedException exception) {
                setException(exception);

                return null;
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean rollback()
        {
            clearException();

            try {
                _receiver.rollback();
            } catch (final ServiceClosedException exception) {
                setException(exception);
                Logger
                    .getInstance(getClass())
                    .info(ServiceMessages.SERVICE_CLOSED);

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
            _receiver.tearDownServer();
        }

        /** {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return _receiver.toString();
        }

        private final ReceiverWrapper _receiver;
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
