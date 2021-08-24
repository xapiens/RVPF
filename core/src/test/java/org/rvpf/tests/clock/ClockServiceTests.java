/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ClockServiceTests.java 4068 2019-06-09 14:51:30Z SFB $
 */

package org.rvpf.tests.clock;

import java.util.Optional;

import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.clock.ClockServiceActivator;
import org.rvpf.service.ServiceActivator;
import org.rvpf.tests.store.StoreClientTests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Clock service tests.
 */
public final class ClockServiceTests
    extends StoreClientTests
{
    /**
     * Sets up this.
     *
     * @throws Exception On failure.
     */
    @BeforeClass
    public void setUp()
        throws Exception
    {
        setUpAlerter();

        setProperty(NULL_NOTIFIER_PROPERTY, "!");
        _storeService = startStoreService(true);
        setUpExpectedUpdates(
            _storeService,
            _CLOCK_POINT_NAME_A,
            _CLOCK_POINT_NAME_B);
        expectUpdates(_CLOCK_POINT_NAME_A, _CLOCK_POINT_NAME_B);

        _clockService = createService(
            ClockServiceActivator.class,
            Optional.empty());
        _clockService.start(false);
    }

    /**
     * Tests the service.
     *
     * @throws Exception On failure.
     */
    @Test
    public void shouldGenerateClockPointValues()
        throws Exception
    {
        final PointValue pointValueA;
        final PointValue pointValueB;

        // Given a started clock service
        // and points configured to need values when the service starts,

        // when waiting for updates of these point values,
        pointValueA = waitForUpdate(_CLOCK_POINT_NAME_A);
        pointValueB = waitForUpdate(_CLOCK_POINT_NAME_B);

        // then the wait should complete successfully.
        Require.notNull(pointValueA, "Clock update notice A");
        Require.notNull(pointValueB, "Clock update notice AB");
    }

    /**
     * Tears down what has been set up.
     *
     * @throws Exception On failure.
     */
    @AfterClass(alwaysRun = true)
    public void tearDown()
        throws Exception
    {
        if (_clockService != null) {
            stopService(_clockService);
            _clockService = null;
        }

        tearDownExpectedUpdates();

        if (_storeService != null) {
            stopService(_storeService);
            _storeService = null;
        }

        tearDownAlerter();
    }

    private static final String _CLOCK_POINT_NAME_A = "TESTS.CLOCK.01";
    private static final String _CLOCK_POINT_NAME_B = "TESTS.CLOCK.04";

    private ServiceActivator _clockService;
    private ServiceActivator _storeService;
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
