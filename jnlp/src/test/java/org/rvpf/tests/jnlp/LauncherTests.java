/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: LauncherTests.java 4061 2019-06-06 16:55:09Z SFB $
 */

package org.rvpf.tests.jnlp;

import java.util.Optional;

import org.rvpf.http.HTTPServerActivator;
import org.rvpf.http.HTTPServerImpl;
import org.rvpf.jnlp.launcher.Launcher;
import org.rvpf.jnlp.loader.CacheManager;
import org.rvpf.jnlp.loader.JNLPProperties;
import org.rvpf.service.Service;
import org.rvpf.service.ServiceActivator;
import org.rvpf.service.ServiceMessages;
import org.rvpf.tests.service.ServiceTests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Launcher Tests.
 */
public final class LauncherTests
    extends ServiceTests
    implements Runnable
{
    /** {@inheritDoc}
     */
    @Override
    public void run()
    {
        final String[] args = {
            "http://localhost:" + _serverPort + "/tests-service.jnlp",
            _FORWARDER_ACTIVATOR, };

        Launcher.main(args);
    }

    /**
     * Sets up this.
     *
     * @throws Exception On failure.
     */
    @BeforeClass
    public void setUp()
        throws Exception
    {
        _server = createService(HTTPServerActivator.class, Optional.empty());
        startService(_server);

        setUpAlerter();

        _serverPort = ((HTTPServerImpl) _server.getService())
            .getListenerPort(0);
        setSystemProperty(
            JNLPProperties.RVPF_JNLP_PREFIX + SERVER_PORT_PROPERTY,
            String.valueOf(_serverPort));
        setSystemProperty(
            JNLPProperties.RVPF_JNLP_PREFIX + CacheManager.CACHE_DIR_PROPERTY,
            CACHE_DIR);
        setSystemProperty(
            JNLPProperties.RVPF_JNLP_PREFIX + CacheManager.PURGE_PROPERTY,
            "true");
        setSystemProperty(_TRUST_STORE_PROPERTY, _TRUST_STORE);
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

        if (_server != null) {
            stopService(_server);
            _server = null;
        }
    }

    /**
     * Tests.
     *
     * @throws Exception On failure.
     */
    @Test
    public void test()
        throws Exception
    {
        expectEvents(Service.STARTED_EVENT);

        final Thread thread = new Thread(this);

        thread.start();
        waitForEvent(Service.STARTED_EVENT);
        quell(
            ServiceMessages.REGISTRY_EXPORT_PURGED,
            ServiceMessages.RMI_NOT_BOUND,
            ServiceMessages.RMI_NOT_REGISTERED);
        sendSignal(Service.STOP_SIGNAL, Optional.empty());
        thread.join();
    }

    /** Cache directory. */
    public static final String CACHE_DIR = "tests/data/cache";

    /** Server port property. */
    public static final String SERVER_PORT_PROPERTY = "server.port";
    private static final String _FORWARDER_ACTIVATOR =
        "org.rvpf.forwarder.ForwarderServiceActivator";
    private static final String _TRUST_STORE = "tests/config/client.truststore";
    private static final String _TRUST_STORE_PROPERTY =
        "javax.net.ssl.trustStore";

    private ServiceActivator _server;
    private int _serverPort;
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
