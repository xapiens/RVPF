/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DNP3SerialConnectionsTestsFactory.java 4057 2019-06-04 18:44:38Z SFB $
 */

package org.rvpf.tests.pap.dnp3;

import java.util.Optional;

import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.Config;
import org.rvpf.document.loader.ConfigDocumentLoader;
import org.rvpf.tests.service.ServiceTests;

import org.testng.annotations.Factory;

/**
 * DNP3 serial connections tests factory.
 */
public final class DNP3SerialConnectionsTestsFactory
    extends ServiceTests
{
    /**
     * No instance.
     */
    private DNP3SerialConnectionsTestsFactory() {}

    /**
     * Creates the tests.
     *
     * @return The tests.
     */
    @Factory
    public static Object[] createTests()
    {
        final Config config;

        setSystemProperty(Config.RVPF_PROPERTIES, _TESTS_PROPERTIES);

        try {
            config = ConfigDocumentLoader
                .loadConfig("", Optional.empty(), Optional.empty());
        } finally {
            clearSystemProperty(Config.RVPF_PROPERTIES);
        }

        final KeyedGroups configProperties = config.getProperties();
        final Optional<String> masterPort = configProperties
            .getString(MASTER_SERIAL_PORT_PROPERTY);
        final Optional<String> outstationPort = configProperties
            .getString(OUTSTATION_SERIAL_PORT_PROPERTY);

        return (masterPort.isPresent()
                && outstationPort.isPresent())? new Object[] {
                    new DNP3SerialConnectionsTests(
                            masterPort.get(),
                                    outstationPort.get()), }: new Object[0];
    }

    /** Master serial port property. */
    static final String MASTER_SERIAL_PORT_PROPERTY =
        "tests.master.serial.port";

    /** Outstation serial port property. */
    static final String OUTSTATION_SERIAL_PORT_PROPERTY =
        "tests.outstation.serial.port";
    private static final String _TESTS_PROPERTIES = "rvpf-dnp3.properties";
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
