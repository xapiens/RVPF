/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DNP3MasterOutstationTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.pap.dnp3;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.rvpf.base.Point;
import org.rvpf.base.UUID;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.config.Config;
import org.rvpf.pap.PAPConnectionListener;
import org.rvpf.pap.PAPProxy;
import org.rvpf.pap.dnp3.DNP3Master;
import org.rvpf.pap.dnp3.DNP3Messages;
import org.rvpf.pap.dnp3.DNP3Outstation;
import org.rvpf.tests.service.MetadataServiceTests;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Master-outstation tests.
 */
public final class DNP3MasterOutstationTests
    extends MetadataServiceTests
    implements PAPProxy.Responder, PAPConnectionListener
{
    /** {@inheritDoc}
     */
    @Override
    public boolean onLostConnection(
            final PAPProxy remoteProxy,
            final Optional<Exception> cause)
    {
        Require.success(_disconnected.getCount() > 0);

        _disconnected.countDown();

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onNewConnection(final PAPProxy remoteProxy)
    {
        Require.failure(_connected);

        _connected = true;

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public PointValue[] select(
            final Point[] points)
        throws InterruptedException, ServiceNotAvailableException
    {
        final PointValue[] pointValues = new PointValue[points.length];

        for (int i = 0; i < pointValues.length; ++i) {
            pointValues[i] = _pointValues.get(points[i].getUUID().get());
        }

        return pointValues;
    }

    /**
     * Sets up this.
     */
    @BeforeClass
    public void setUp()
    {
        DNP3TestsSupport.setPortProperties();

        setSystemProperty(Config.RVPF_PROPERTIES, _TESTS_PROPERTIES);

        _support = new DNP3TestsSupport(Optional.of(getMetadata(true)));
    }

    /**
     * Test high level.
     *
     * @throws Exception On failure.
     */
    @Test(priority = 20)
    public void test()
        throws Exception
    {
        _disconnected = new CountDownLatch(1);
        _connected = false;

        final DNP3Outstation outstation = _support.getOutstation();

        outstation.setNeedTime();
        outstation.setResponder(this);
        outstation.start();

        final DNP3Master master = _support.getMaster();

        Require.success(master.addConnectionListener(this));
        master.open();
        Require.failure(_connected);
        expectLogs(DNP3Messages.MASTER_CONNECTION_ACCEPTED);
        Require
            .success(
                master
                    .connect(
                            _support.getOutstationOrigin(),
                                    DNP3TestsSupport.TESTS_OUTSTATION_DEVICE_ADDRESS));
        Require.success(_connected);
        waitForLogs(DNP3Messages.MASTER_CONNECTION_ACCEPTED);

        new _Tests(_support, this).test();

        Require.success(_disconnected.getCount() > 0);
        _support.clearMaster();
        Require
            .success(
                _disconnected.await(getTimeout(), TimeUnit.MILLISECONDS));

        _support.clearOutstation();
    }

    /**
     * Puts a value in the point values map.
     *
     * @param pointValue The point value.
     */
    void _putValue(@Nonnull final PointValue pointValue)
    {
        _pointValues.put(pointValue.getPointUUID(), pointValue);
    }

    private static final String _TESTS_PROPERTIES = "rvpf-dnp3.properties";

    private boolean _connected;
    private CountDownLatch _disconnected;
    private final Map<UUID, PointValue> _pointValues =
        new ConcurrentHashMap<>();
    private DNP3TestsSupport _support;

    /**
     * Tests.
     */
    private static final class _Tests
        extends DNP3Tests
    {
        /**
         * Constructs an instance.
         *
         * @param suppport The DNP3 tests support.
         * @param owner The owner.
         */
        protected _Tests(
                @Nonnull final DNP3TestsSupport suppport,
                @Nonnull final DNP3MasterOutstationTests owner)
        {
            super(suppport, owner);
        }

        /** {@inheritDoc}
         */
        @Override
        protected Optional<PointValue> getNextPointValue()
        {
            final DNP3Outstation outstation = getSupport().getOutstation();

            try {
                return outstation.nextUpdate(getTimeout());
            } catch (final InterruptedException exception) {
                throw new RuntimeException(exception);
            }
        }

        /** {@inheritDoc}
         */
        @Override
        protected void putPointValue(final PointValue pointValue)
        {
            ((DNP3MasterOutstationTests) getOwner())._putValue(pointValue);
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
