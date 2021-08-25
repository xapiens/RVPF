/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CIPTestsFactory.java 4034 2019-05-28 19:57:11Z SFB $
 */

package org.rvpf.tests.pap.cip;

import java.io.IOException;

import java.net.Socket;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.config.Config;
import org.rvpf.document.loader.ConfigDocumentLoader;
import org.rvpf.pap.cip.CIP;
import org.rvpf.tests.service.ServiceTests;

import org.testng.annotations.Factory;
import org.testng.annotations.Parameters;

/**
 * CIP tests factory.
 */
public final class CIPTestsFactory
    extends ServiceTests
{
    /**
     * No instance.
     */
    private CIPTestsFactory() {}

    /**
     * Creates the tests.
     *
     * @param tests The name of the tests class.
     *
     * @return The tests.
     */
    @Factory
    @Parameters({"tests"})
    public static Object[] createTests(@Nonnull final String tests)
    {
        if (_serverAvailable == null) {
            final Config config;

            setSystemProperty(Config.RVPF_PROPERTIES, _TESTS_PROPERTIES);

            try {
                config = ConfigDocumentLoader
                    .loadConfig("", Optional.empty(), Optional.empty());
            } finally {
                clearSystemProperty(Config.RVPF_PROPERTIES);
            }

            if (config != null) {
                final Optional<String> testsServerHost = config
                    .getStringValue(_TESTS_SERVER_ADDRESS_PROPERTY);

                if (testsServerHost.isPresent()) {
                    try {
                        final Socket socket = new Socket(
                            testsServerHost.get(),
                            CIP.DEFAULT_TCP_PORT);

                        socket.close();
                        _serverAvailable = Boolean.TRUE;
                    } catch (final IOException exception) {
                        _serverAvailable = Boolean.FALSE;
                    }
                } else {
                    _serverAvailable = Boolean.FALSE;
                }
            } else {
                _serverAvailable = Boolean.FALSE;
            }
        }

        if (!_serverAvailable.booleanValue()) {
            return new Object[0];
        }

        final Object testsInstance;

        try {
            testsInstance = Class.forName(tests).newInstance();
        } catch (final Exception exception) {
            throw new IllegalArgumentException(tests, exception);
        }

        return new Object[] {testsInstance, };
    }

    private static final String _TESTS_PROPERTIES = "rvpf-cip.properties";
    private static final String _TESTS_SERVER_ADDRESS_PROPERTY =
        "tests.cip.tcp.address";
    private static Boolean _serverAvailable;
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
