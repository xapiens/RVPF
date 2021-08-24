/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StorageMonitorTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.base.util;

import java.io.File;

import java.util.Optional;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.StorageMonitor;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.tests.Tests;

import org.testng.annotations.Test;

/**
 * Storage monitor tests.
 */
public final class StorageMonitorTests
    extends Tests
{
    /**
     * Should allow get available or free without a previous check.
     */
    @Test(priority = 10)
    public static void shouldAllowGetAvailable()
    {
        final StorageMonitor storageMonitor = new StorageMonitor(
            Logger.getInstance(StorageMonitor.class));

        Require
            .success(storageMonitor.setUp(new KeyedGroups(), new File(".")));
        Require.success(storageMonitor.getAvailable() != 0);

        storageMonitor.setUsableSpaceSupplier(() -> 123_456_789L);

        Require.success(storageMonitor.getAvailable() == 123_456_789L);
        Require.success(storageMonitor.getFree() == 123);
    }

    /**
     * Should allow quelled log.
     */
    @Test(priority = 50)
    public static void shouldAllowQuelledLog()
    {
        final StorageMonitor storageMonitor = new StorageMonitor(
            Logger.getInstance(StorageMonitor.class),
            Optional.of(on -> true),
            Optional.of(on -> true));
        final KeyedGroups storageProperties = new KeyedGroups();

        storageProperties.setValue(StorageMonitor.FREE_ALERT_PROPERTY, "100");
        storageProperties.setValue(StorageMonitor.FREE_WARN_PROPERTY, "500");
        Require
            .success(storageMonitor.setUp(storageProperties, new File(".")));

        storageMonitor.setUsableSpaceSupplier(() -> 1_000_000_000L);
        Require.success(storageMonitor.check());

        storageMonitor.setUsableSpaceSupplier(() -> 99_999_999L);
        Require.failure(storageMonitor.check());

        storageMonitor.setUsableSpaceSupplier(() -> 1_000_000_000L);
        Require.success(storageMonitor.check());
    }

    /**
     * Should allow an initial alert.
     *
     * @throws Exception On failure.
     */
    @Test(priority = 20)
    public void shouldAllowInitialAlert()
        throws Exception
    {
        final StorageMonitor storageMonitor = new StorageMonitor(
            getThisLogger());
        final KeyedGroups storageProperties = new KeyedGroups();

        storageProperties.setValue(StorageMonitor.FREE_ALERT_PROPERTY, "100");
        storageProperties.setValue(StorageMonitor.FREE_WARN_PROPERTY, "500");
        Require
            .success(storageMonitor.setUp(storageProperties, new File(".")));

        storageMonitor.setUsableSpaceSupplier(() -> 99_999_999L);
        expectLogs(
            BaseMessages.STORAGE_UNDER_WARN,
            BaseMessages.STORAGE_UNDER_ALERT);
        Require.failure(storageMonitor.check());
        waitForLogs(
            BaseMessages.STORAGE_UNDER_WARN,
            BaseMessages.STORAGE_UNDER_ALERT);
    }

    /**
     * Should follow an abrupt sequence.
     *
     * @throws Exception On failure.
     */
    @Test(priority = 40)
    public void shouldFollowAbruptSequence()
        throws Exception
    {
        final StorageMonitor storageMonitor = new StorageMonitor(
            Logger.getInstance(StorageMonitor.class),
            Optional.of(on -> false),
            Optional.of(on -> false));
        final KeyedGroups storageProperties = new KeyedGroups();

        storageProperties.setValue(StorageMonitor.FREE_ALERT_PROPERTY, "100");
        storageProperties.setValue(StorageMonitor.FREE_WARN_PROPERTY, "500");
        Require
            .success(storageMonitor.setUp(storageProperties, new File(".")));

        storageMonitor.setUsableSpaceSupplier(() -> 1_000_000_000L);
        Require.success(storageMonitor.check());

        storageMonitor.setUsableSpaceSupplier(() -> 99_999_999L);
        expectLogs(
            BaseMessages.STORAGE_UNDER_WARN,
            BaseMessages.STORAGE_UNDER_ALERT);
        Require.failure(storageMonitor.check());
        waitForLogs(
            BaseMessages.STORAGE_UNDER_WARN,
            BaseMessages.STORAGE_UNDER_ALERT);

        storageMonitor.setUsableSpaceSupplier(() -> 1_000_000_000L);
        expectLogs(
            BaseMessages.STORAGE_NOT_UNDER_ALERT,
            BaseMessages.STORAGE_NOT_UNDER_WARN);
        Require.success(storageMonitor.check());
        waitForLogs(
            BaseMessages.STORAGE_NOT_UNDER_ALERT,
            BaseMessages.STORAGE_NOT_UNDER_WARN);
    }

    /**
     * Should follow a gradual sequence.
     *
     * @throws Exception On failure.
     */
    @Test(priority = 30)
    public void shouldFollowGradualSequence()
        throws Exception
    {
        final StorageMonitor storageMonitor = new StorageMonitor(
            getThisLogger());
        final KeyedGroups storageProperties = new KeyedGroups();

        storageProperties.setValue(StorageMonitor.FREE_ALERT_PROPERTY, "100");
        storageProperties.setValue(StorageMonitor.FREE_WARN_PROPERTY, "500");
        Require
            .success(storageMonitor.setUp(storageProperties, new File(".")));

        storageMonitor.setUsableSpaceSupplier(() -> 500_000_000L);
        Require.success(storageMonitor.check());

        storageMonitor.setUsableSpaceSupplier(() -> 499_999_999L);
        expectLogs(BaseMessages.STORAGE_UNDER_WARN);
        Require.success(storageMonitor.check());
        waitForLogs(BaseMessages.STORAGE_UNDER_WARN);

        storageMonitor.setUsableSpaceSupplier(() -> 99_999_999L);
        expectLogs(BaseMessages.STORAGE_UNDER_ALERT);
        Require.failure(storageMonitor.check());
        waitForLogs(BaseMessages.STORAGE_UNDER_ALERT);

        storageMonitor.setUsableSpaceSupplier(() -> 100_000_000L);
        expectLogs(BaseMessages.STORAGE_NOT_UNDER_ALERT);
        Require.success(storageMonitor.check());
        waitForLogs(BaseMessages.STORAGE_NOT_UNDER_ALERT);

        storageMonitor.setUsableSpaceSupplier(() -> 500_000_000L);
        expectLogs(BaseMessages.STORAGE_NOT_UNDER_WARN);
        Require.success(storageMonitor.check());
        waitForLogs(BaseMessages.STORAGE_NOT_UNDER_WARN);
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
