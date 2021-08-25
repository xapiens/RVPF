/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreTarget.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.store.server.pap.modbus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Point;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.pap.PAPProxy;
import org.rvpf.pap.modbus.ModbusServer;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;
import org.rvpf.tests.pap.modbus.ModbusTestsSupport;

/**
 * Store target.
 */
final class StoreTarget
    implements PAPProxy.Responder, ServiceThread.Target
{
    /**
     * Constructs an instance.
     *
     * @param metadata The metadata.
     */
    StoreTarget(@Nonnull final Metadata metadata)
    {
        _metadata = metadata;
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
        throws InterruptedException
    {
        for (;;) {
            putPointValue(_server.nextUpdate(-1).orElse(null));
            _server.onUpdatesCommit();

            if (!_server.isUpdating()) {
                synchronized (this) {
                    notifyAll();
                }
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public PointValue[] select(final Point[] points)
        throws InterruptedException
    {
        final PointValue[] pointValues = new PointValue[points.length];

        for (int i = 0; i < points.length; ++i) {
            pointValues[i] = _pointValues.get(points[i]);
        }

        return pointValues;
    }

    /**
     * Gets a point value for a point.
     *
     * @param point The point.
     *
     * @return The point value.
     *
     * @throws InterruptedException When interrupted.
     */
    @Nonnull
    @CheckReturnValue
    PointValue getPointValue(
            @Nonnull final Point point)
        throws InterruptedException
    {
        synchronized (this) {
            while (_server.isUpdating()) {
                wait();
            }
        }

        return Require.notNull(_pointValues.get(point));
    }

    /**
     * Puts a point value.
     *
     * @param pointValue The point value.
     */
    void putPointValue(@Nonnull final PointValue pointValue)
    {
        final Point point = pointValue.getPoint().get();

        _pointValues.put(point, pointValue);
    }

    /**
     * Starts.
     */
    void start()
    {
        for (final Point point: _metadata.getPointsCollection()) {
            Require.success(((PointEntity) point).setUp(_metadata));
        }

        final ModbusTestsSupport support = new ModbusTestsSupport(_metadata);

        _server = support.getServer();
        _server.setResponder(this);
        _server.start();

        final ServiceThread thread = new ServiceThread(this, "Modbus target");

        if (_thread.compareAndSet(null, thread)) {
            _LOGGER.debug(ServiceMessages.STARTING_THREAD, thread.getName());
            thread.start();
        }
    }

    /**
     * Stops.
     */
    void stop()
    {
        final ServiceThread thread = _thread.getAndSet(null);

        if (thread != null) {
            _LOGGER.debug(ServiceMessages.STOPPING_THREAD, thread.getName());
            _server.stop();
            Require.ignored(thread.interruptAndJoin(_LOGGER, 0));
        }
    }

    private static final Logger _LOGGER = Logger.getInstance(StoreTarget.class);

    private final Metadata _metadata;
    private Map<Point, PointValue> _pointValues = new ConcurrentHashMap<Point,
        PointValue>();
    private ModbusServer _server;
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
