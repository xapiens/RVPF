/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ServiceAppHolderTests.java 4060 2019-06-06 13:49:41Z SFB $
 */

package org.rvpf.tests.service;

import java.util.Optional;

import org.rvpf.service.ServiceActivator;
import org.rvpf.service.metadata.app.MetadataServiceAppHolderActivator;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Service application holder tests.
 */
public class ServiceAppHolderTests
    extends ServiceTests
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
    }

    /**
     * Tests the service.
     *
     * @throws Exception On failure.
     */
    @Test
    public void shouldStartStopAppHolderService()
        throws Exception
    {
        final ServiceActivator serviceAppHolderService;

        // Given the service application holder service activator class,
        // when starting and stopping the service,
        serviceAppHolderService = startService(
            MetadataServiceAppHolderActivator.class,
            Optional.empty());
        stopService(serviceAppHolderService);

        // then no exception should have been thrown.
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
        tearDownAlerter();
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
