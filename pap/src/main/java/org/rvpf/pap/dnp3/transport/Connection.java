/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Connection.java 4082 2019-06-14 18:38:55Z SFB $
 */

package org.rvpf.pap.dnp3.transport;

import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;
import org.rvpf.pap.PAPConnection;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;

/**
 * Connection.
 */
public abstract class Connection
    extends PAPConnection.Abstract
    implements ServiceThread.Target
{
    /**
     * Constructs an instance.
     *
     * @param localEndPoint The local end point.
     * @param remoteEndPoint The remote end point.
     */
    protected Connection(
            @Nonnull final LocalEndPoint localEndPoint,
            @Nonnull final RemoteEndPoint remoteEndPoint)
    {
        _localEndPoint = localEndPoint;
        _remoteEndPoint = remoteEndPoint;
    }

    /**
     * Activates.
     */
    public void activate()
    {
        final ServiceThread thread = new ServiceThread(
            this,
            "Connection " + this);

        if (_thread.compareAndSet(null, thread)) {
            getThisLogger()
                .debug(ServiceMessages.STARTING_THREAD, thread.getName());
            thread.start();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void doClose()
        throws IOException
    {
        final ServiceThread thread = _thread.getAndSet(null);

        if (thread != null) {
            if (thread != Thread.currentThread()) {
                getThisLogger()
                    .debug(ServiceMessages.STOPPING_THREAD, thread.getName());
                Require.ignored(thread.interruptAndJoin(getThisLogger(), 0));
            }

            final ConnectionManager connectionManager = _localEndPoint
                .getConnectionManager();

            if (connectionManager != null) {
                connectionManager.onClosedConnection(this);
            }
        }
    }

    /**
     * Gets the exception.
     *
     * @return The exception (may be empty).
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Exception> getException()
    {
        return Optional.ofNullable(_exception);
    }

    /**
     * Gets the local end point.
     *
     * @return The local end point.
     */
    @Nonnull
    @CheckReturnValue
    public LocalEndPoint getLocalEndPoint()
    {
        return _localEndPoint;
    }

    /**
     * Gets the remote end point.
     *
     * @return The remote end point.
     */
    @Nonnull
    @CheckReturnValue
    public RemoteEndPoint getRemoteEndPoint()
    {
        return _remoteEndPoint;
    }

    /**
     * Asks if the local end point is on a master.
     *
     * @return True if the local end point is on a master.
     */
    @CheckReturnValue
    public boolean isOnMaster()
    {
        return _localEndPoint.isOnMaster();
    }

    /**
     * Asks if the local end point is on an outstation
     *
     * @return True if the local end point is on an outstation
     */
    @CheckReturnValue
    public boolean isOnOutstation()
    {
        return _localEndPoint.isOnOutstation();
    }

    /**
     * Receives.
     *
     * @param buffer A buffer to receive data.
     *
     * @throws IOException On I/O exception.
     */
    public final void receive(
            @Nonnull final ByteBuffer buffer)
        throws IOException
    {
        try {
            doReceive(buffer);
        } catch (final Exception exception) {
            _exception = exception;
            close();

            throw exception;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
        throws Exception
    {
        final Frame.Receiver frameReceiver = new Frame.Receiver(
            getRemoteEndPoint());

        try {
            for (;;) {
                final Frame frame = frameReceiver.receive(this);

                _remoteEndPoint.onFrameReceived(frame);
            }
        } catch (final ClosedChannelException exception) {
            // Returns.
        } catch (final Exception exception) {
            _exception = exception;

            throw exception;
        } finally {
            frameReceiver.close();
        }
    }

    /**
     * Sends.
     *
     * @param buffer A buffer containing the data.
     *
     * @throws IOException On I/O exception.
     */
    public final void send(@Nonnull final ByteBuffer buffer)
        throws IOException
    {
        try {
            doSend(buffer);
        } catch (final Exception exception) {
            _exception = exception;
            close();

            throw exception;
        }
    }

    /**
     * Do receives.
     *
     * @param buffer A buffer to receive data.
     *
     * @throws IOException On I/O exception.
     */
    protected abstract void doReceive(
            @Nonnull ByteBuffer buffer)
        throws IOException;

    /**
     * Do sends.
     *
     * @param buffer A buffer containing the data.
     *
     * @throws IOException On I/O exception.
     */
    protected abstract void doSend(
            @Nonnull final ByteBuffer buffer)
        throws IOException;

    private volatile Exception _exception;
    private final LocalEndPoint _localEndPoint;
    private final RemoteEndPoint _remoteEndPoint;
    private final AtomicReference<ServiceThread> _thread =
        new AtomicReference<>();
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
