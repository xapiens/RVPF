/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: LoggerTests.java 4061 2019-06-06 16:55:09Z SFB $
 */

package org.rvpf.tests.service.log4j;

import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.SocketAppender;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.StringMap;

import org.rvpf.base.tool.Inet;
import org.rvpf.service.ServiceActivator;
import org.rvpf.service.log4j.LoggerServiceActivator;
import org.rvpf.tests.TestsMessages;
import org.rvpf.tests.service.ServiceTests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Logger tests.
 */
public final class LoggerTests
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
        _loggerPort = allocateTCPPort();
        _loggerAddress = Inet.LOCAL_HOST + ":" + _loggerPort;
        setProperty(_LOGGER_ADDRESS_PROPERTY, _loggerAddress);

        _loggerService = startService(
            LoggerServiceActivator.class,
            Optional.empty());
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
        if (_loggerService != null) {
            stopService(_loggerService);
            _loggerService = null;
        }
    }

    /**
     * Tests the java.util.logging trap.
     */
    @Test
    public void testJULTrap()
    {
        final java.util.logging.Logger logger = java.util.logging.Logger
            .getLogger(getClass().getName());

        logger.info("Test INFO");
        logger.fine("Test FINE");
        logger.finer("Test FINER");
        logger.finest("Test FINEST");
    }

    /**
     * Test log listening.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testLogListener()
        throws Exception
    {
        expectLogs(TestsMessages.TEST);
        getThisLogger().warn(TestsMessages.TEST);
        waitForLogs(TestsMessages.TEST);
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
        //J-
        final Appender socketAppender = SocketAppender.newBuilder()
            .withHost("127.0.0.1")
            .withName("TESTS_SOCKET_APPENDER")
            .withPort(_loggerPort)
            .build();
        //J+
        final Logger logger = LogManager.getLogger(getClass());

        socketAppender.start();

        final Log4jLogEvent.Builder eventBuilder = Log4jLogEvent.newBuilder();

        eventBuilder.setLoggerName(logger.getName());
        eventBuilder.setLoggerFqcn(logger.getClass().getName());
        eventBuilder.setLevel(Level.INFO);
        eventBuilder
            .setMessage(new SimpleMessage("Logger server test message"));

        final StringMap contextData = ContextDataFactory.createContextData();

        for (final Map.Entry<String, String> entry:
                ThreadContext.getImmutableContext().entrySet()) {
            contextData.putValue(entry.getKey(), entry.getValue());
        }

        eventBuilder.setContextData(contextData);
        eventBuilder.setContextStack(ThreadContext.getImmutableStack());
        eventBuilder.setThreadName(Thread.currentThread().getName());
        eventBuilder.setTimeMillis(System.currentTimeMillis());

        socketAppender.append(eventBuilder.build());

        socketAppender.stop();
    }

    private static final String _LOGGER_ADDRESS_PROPERTY =
        "tests.logger.address";

    private String _loggerAddress;
    private int _loggerPort;
    private ServiceActivator _loggerService;
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
