/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DocumentVersionTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.document.version;

import java.util.Optional;

import org.rvpf.base.tool.Require;
import org.rvpf.document.version.DocumentVersionControlActivator;
import org.rvpf.document.version.VersionControl;
import org.rvpf.service.ServiceActivator;
import org.rvpf.tests.service.ServiceTests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Document version tests.
 */
public final class DocumentVersionTests
    extends ServiceTests
{
    /**
     * Sets up the tests.
     *
     * @throws Exception On failure.
     */
    @BeforeClass
    public void setUp()
        throws Exception
    {
        Require.success(getConfig().registerClassLib(_EXT_LIB));

        setUpAlerter();

        quell(_EXT_MESSAGES, _WORKSPACE_MODIFIED);
        _service = startService(
            DocumentVersionControlActivator.class,
            Optional.empty());
    }

    /**
     * Tears down what has been set up.
     *
     * @throws Exception On Failure.
     */
    @AfterClass(alwaysRun = true)
    public void tearDown()
        throws Exception
    {
        if (_service != null) {
            stopService(_service);
            _service = null;

            tearDownAlerter();
            checkAlerts();
        }
    }

    /**
     * Tests the service.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testService()
        throws Exception
    {
        expectEvents(VersionControl.DOCUMENT_UPDATED_EVENT);
        sendSignal(VersionControl.UPDATE_DOCUMENT_SIGNAL, Optional.empty());
        waitForEvent(VersionControl.DOCUMENT_UPDATED_EVENT);
    }

    private static final String _EXT_LIB = "Ext";
    private static final String _EXT_MESSAGES = "org.rvpf.ext.ExtMessages";
    private static final String _WORKSPACE_MODIFIED = "WORKSPACE_MODIFIED";

    private ServiceActivator _service;
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
