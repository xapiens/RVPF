/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: LoggerTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.base.logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Logger.LogLevel;
import org.rvpf.base.tool.Require;

import org.testng.annotations.Test;

/**
 * Logger tests.
 */
public final class LoggerTests
{
    private LoggerTests() {}

    /**
     * Tests the serialization.
     *
     * @throws Exception On failure.
     */
    @Test
    public static void testSerialization()
        throws Exception
    {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final ObjectOutputStream objectOutputStream = new ObjectOutputStream(
            outputStream);
        final Logger localLogger = Logger.getInstance(LoggerTests.class);

        objectOutputStream.writeObject(localLogger);
        objectOutputStream.close();

        final InputStream inputStream = new ByteArrayInputStream(
            outputStream.toByteArray());
        final ObjectInputStream objectInputStream = new ObjectInputStream(
            inputStream);
        final Logger remoteLogger = (Logger) objectInputStream.readObject();

        Require.success(remoteLogger == localLogger);
        Require.failure(remoteLogger.hasLogged(LogLevel.ALL));
        objectInputStream.close();
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
