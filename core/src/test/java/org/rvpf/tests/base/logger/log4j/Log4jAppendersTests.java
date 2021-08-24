/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Log4jAppendersTests.java 3909 2019-03-21 19:23:41Z SFB $
 */

package org.rvpf.tests.base.logger.log4j;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.tests.FrameworkTests;
import org.rvpf.tests.Tests;
import org.rvpf.tests.core.CoreTestsMessages;

import org.testng.annotations.Test;

/**
 * Log4j appenders tests.
 */
public class Log4jAppendersTests
    extends Tests
{
    /**
     * Tests the AlwaysTrigger evaluator.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testAlwaysTrigger()
        throws Exception
    {
        clearMail();

        Logger
            .getInstance(_SMTP_ALWAYS_LOGGER_NAME)
            .warn(CoreTestsMessages.ALWAYS_TRIGGER_TEST);
        ignoreLoggedProblem();

        final FrameworkTests.Mail mail = waitForMail().get();

        Require.equal(mail.getRecipient().get(), _RECIPIENT_ALWAYS);
    }

    /**
     * Tests JUL handling.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testJUL()
        throws Exception
    {
        final java.util.logging.Logger julLogger = java.util.logging.Logger
            .getLogger(getClass().getName());

        julLogger.info("JUL 'info' test");
    }

    /**
     * Tests the MidnightTrigger evaluator.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testMidnightTrigger()
        throws Exception
    {
        clearMail();

        Logger
            .getInstance(_SMTP_MIDNIGHT_LOGGER_NAME)
            .warn(CoreTestsMessages.MIDNIGHT_TRIGGER_TEST);
        ignoreLoggedProblem();
        Logger.getMidnightLogger().info(CoreTestsMessages.NOT_REALLY_MIDNIGHT);

        final FrameworkTests.Mail mail = waitForMail().get();

        Require.equal(mail.getRecipient().get(), _RECIPIENT_MIDNIGHT);
    }

    private static final String _RECIPIENT_ALWAYS = "rvpf.always@localhost";
    private static final String _RECIPIENT_MIDNIGHT = "rvpf.midnight@localhost";
    private static final String _SMTP_ALWAYS_LOGGER_NAME = "SMTP-ALWAYS";
    private static final String _SMTP_MIDNIGHT_LOGGER_NAME = "SMTP-MIDNIGHT";
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
