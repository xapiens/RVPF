/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Tests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Logger.LogLevel;
import org.rvpf.base.logger.Message;
import org.rvpf.base.logger.Messages;
import org.rvpf.base.tool.Inet;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.Bag;
import org.rvpf.base.util.container.HashCounterBag;
import org.rvpf.base.util.container.ListLinkedHashMap;
import org.rvpf.base.util.container.ListMap;
import org.rvpf.tests.FrameworkTests.Mail;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;

/**
 * Tests.
 */
public abstract class Tests
    implements Thread.UncaughtExceptionHandler, Logger.LogListener
{
    /**
     * Allocates a TCP port.
     *
     * @return A free TCP port.
     */
    @CheckReturnValue
    public static int allocateTCPPort()
    {
        return Inet.allocateTCPPort();
    }

    /**
     * Allocates an UDP port.
     *
     * @return A free UDP port.
     */
    @CheckReturnValue
    public static int allocateUDPPort()
    {
        return Inet.allocateUDPPort();
    }

    /**
     * Forces a garbage collection pass.
     */
    @BeforeTest
    public static void gc()
    {
        System.gc();
    }

    /**
     * Gets the registry port.
     *
     * @return The registry port.
     */
    @CheckReturnValue
    public static int getRegistryPort()
    {
        return Integer
            .parseInt(
                System.getProperty(FrameworkTests.REGISTRY_PORT_PROPERTY));
    }

    /**
     * Restores system properties.
     */
    @AfterTest(alwaysRun = true)
    public static void restoreSystemProperties()
    {
        synchronized (_PROPERTIES) {
            final Iterable<String> keys = new ArrayList<>(_PROPERTIES.keySet());

            for (final String key: keys) {
                restoreSystemProperty(key);
            }
        }
    }

    /**
     * Attaches the logger.
     */
    @BeforeClass
    public final void attachLogger()
    {
        Require.success(Logger.addListener(this));
    }

    /**
     * Expects log entries.
     *
     * @param entries The entries.
     */
    public void expectLogs(@Nonnull final Messages.Entry... entries)
    {
        synchronized (_gate) {
            for (final Messages.Entry entry: entries) {
                quell(entry);
                _expectedEntries.add(entry);
            }
        }
    }

    /**
     * Gets the logger.
     *
     * @return The logger.
     */
    @Nonnull
    @CheckReturnValue
    public final Logger getThisLogger()
    {
        if (_logger == null) {
            _logger = Logger.getInstance(getClass());
        }

        return _logger;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<LogLevel> onLog(
            final Logger logger,
            final LogLevel logLevel,
            final Message message)
    {
        LogLevel newLogLevel = logLevel;

        synchronized (_gate) {
            final Messages.Entry messageEntry = message.getEntry().orElse(null);

            if (LogLevel.INFO.compareTo(logLevel) > 0) {
                if ((messageEntry != null)
                        && _quelledEntries.contains(messageEntry)) {
                    if (_quashedEntries.contains(messageEntry)) {
                        newLogLevel = null;
                    } else {
                        newLogLevel = LogLevel.INFO;
                    }
                } else {
                    ++_loggedProblems;
                }
            }

            if ((messageEntry != null)
                    && _expectedEntries.remove(messageEntry)) {
                _expectedMessages.add(messageEntry, message);
                _gate.notifyAll();

                unquash(messageEntry);
            }
        }

        return Optional.ofNullable(newLogLevel);
    }

    /**
     * Releases the logger.
     */
    @AfterClass(alwaysRun = true)
    public final void releaseLogger()
    {
        Require.success(Logger.removeListener(this));

        synchronized (_gate) {
            Require
                .success(_expectedEntries.isEmpty(), "missing log messages");
            Require
                .success(_expectedMessages.isEmpty(), "missed log messages");
            Require.success(_loggedProblems == 0, "logged problems");

            _quelledEntries.clear();
        }
    }

    /**
     * Requires an expected log message.
     *
     * @param entries The entries for the expected messages.
     */
    public void requireLogs(@Nonnull final Messages.Entry... entries)
    {
        synchronized (_gate) {
            for (final Messages.Entry entry: entries) {
                Require
                    .notNull(
                        _expectedMessages.remove(entry),
                        "log message '" + entry.name() + "' received");
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final void uncaughtException(
            final Thread thread,
            final Throwable throwable)
    {
        getThisLogger().uncaughtException(thread, throwable);

        Require.failure("expected exception not thrown");
    }

    /**
     * Waits for expected log messages.
     *
     * @param entries The entries for the expected messages.
     *
     * @throws InterruptedException When the service is stopped.
     */
    public void waitForLogs(
            @Nonnull final Messages.Entry... entries)
        throws InterruptedException
    {
        for (final Messages.Entry entry: entries) {
            final long millis = System.currentTimeMillis();
            final long timeout = getTimeout(DEFAULT_TIMEOUT);

            synchronized (_gate) {
                while (!_expectedMessages.containsKey(entry)) {
                    _gate.wait(1000);
                    Require
                        .success(
                            (timeout <= 0)
                            || ((System.currentTimeMillis() - millis)
                                <= timeout),
                            "log message '" + entry.name() + "' received");
                }

                _expectedMessages.removeFirst(entry);
            }
        }
    }

    /**
     * Clears a system property.
     *
     * @param key The property key.
     */
    protected static void clearSystemProperty(@Nonnull final String key)
    {
        final String previousValue = System.clearProperty(key);

        Logger
            .getInstance(Tests.class)
            .info(TestsMessages.SYSTEM_PROPERTY_CLEARED, key);

        synchronized (_PROPERTIES) {
            if (!_PROPERTIES.containsKey(key)) {
                _PROPERTIES.put(key, previousValue);
            }
        }
    }

    /**
     * Gets a system property.
     *
     * @param key The property key.
     *
     * @return The property value or empty.
     */
    @Nonnull
    @CheckReturnValue
    protected static Optional<String> getSystemProperty(
            @Nonnull final String key)
    {
        return Optional.ofNullable(System.getProperty(key));
    }

    /**
     * Restores a system property.
     *
     * @param key The property key.
     */
    protected static void restoreSystemProperty(@Nonnull final String key)
    {
        synchronized (_PROPERTIES) {
            if (_PROPERTIES.containsKey(key)) {
                final String value = _PROPERTIES.get(key);

                if (value != null) {
                    System.setProperty(key, value);
                    Logger
                        .getInstance(Tests.class)
                        .info(
                            TestsMessages.SYSTEM_PROPERTY_RESTORED,
                            key,
                            value);
                } else if (System.getProperty(key) != null) {
                    System.clearProperty(key);
                    Logger
                        .getInstance(Tests.class)
                        .info(TestsMessages.SYSTEM_PROPERTY_CLEARED, key);
                }

                _PROPERTIES.remove(key);
            }
        }
    }

    /**
     * Sets a system property.
     *
     * @param key The property key.
     * @param value The property value.
     */
    protected static void setSystemProperty(
            @Nonnull final String key,
            @Nonnull final String value)
    {
        final String previousValue = System.setProperty(key, value);

        Logger
            .getInstance(Tests.class)
            .info(TestsMessages.SYSTEM_PROPERTY_SET, key, value);

        synchronized (_PROPERTIES) {
            if (!_PROPERTIES.containsKey(key)) {
                _PROPERTIES.put(key, previousValue);
            }
        }
    }

    /**
     * Clears the mail.
     */
    protected void clearMail()
    {
        FrameworkTests.getInstance().clearMail();
    }

    /**
     * Gets the timeout in millis.
     *
     * @param defaultTimeout The default timeout (may be empty).
     *
     * @return The timeout in millis.
     */
    @CheckReturnValue
    protected int getTimeout(
            @Nonnull final Optional<ElapsedTime> defaultTimeout)
    {
        if (_timeout == null) {
            synchronized (_gate) {
                String timeoutString = System.getenv(TIMEOUT_ENV_KEY);
                ElapsedTime timeout;

                if (timeoutString == null) {
                    timeoutString = System
                        .getProperty(SYSTEM_PROPERTY_PREFIX + TIMEOUT_PROPERTY);
                }

                if (timeoutString == null) {
                    timeout = defaultTimeout.orElse(null);
                } else {
                    try {
                        timeout = ElapsedTime.fromString(timeoutString);
                    } catch (final IllegalArgumentException exception) {
                        getThisLogger()
                            .warn(
                                BaseMessages.ILLEGAL_ARGUMENT,
                                exception.getMessage());
                        timeout = defaultTimeout.orElse(null);
                    }

                    if (timeout == null) {
                        timeout = ElapsedTime.INFINITY;
                    }

                    getThisLogger().debug(TestsMessages.TESTS_TIMEOUT, timeout);
                }

                _timeout = timeout;
            }
        }

        return (_timeout == null)? 0: (_timeout
            .isInfinity()? -1: (int) _timeout.toMillis());
    }

    /**
     * Ignores an expected logged problem.
     */
    protected void ignoreLoggedProblem()
    {
        Require.success(_loggedProblems > 0);

        --_loggedProblems;
    }

    /**
     * Quashes entries.
     *
     * @param entries The entries.
     */
    protected final void quash(@Nonnull final Messages.Entry... entries)
    {
        synchronized (_gate) {
            for (final Messages.Entry entry: entries) {
                _quelledEntries.add(entry);
                _quashedEntries.add(entry);
            }
        }
    }

    /**
     * Quells entries.
     *
     * @param entries The entries.
     */
    protected final void quell(@Nonnull final Messages.Entry... entries)
    {
        synchronized (_gate) {
            for (final Messages.Entry entry: entries) {
                _quelledEntries.add(entry);
            }
        }
    }

    /**
     * Quells an entry.
     *
     * @param messagesName The name of the class holding the entry.
     * @param entryName The name of the entry.
     *
     * @return The entry.
     */
    @Nonnull
    protected final Messages.Entry quell(
            @Nonnull final String messagesName,
            @Nonnull final String entryName)
    {
        final Messages.Entry entry = _messagesEntry(messagesName, entryName);

        quell(entry);

        return entry;
    }

    /**
     * Unquashes entries.
     *
     * @param entries The entries.
     */
    protected final void unquash(@Nonnull final Messages.Entry... entries)
    {
        synchronized (_gate) {
            for (final Messages.Entry entry: entries) {
                _quashedEntries.remove(entry);
                _quelledEntries.remove(entry);
            }
        }
    }

    /**
     * Unquells entries.
     *
     * @param entries The entries.
     */
    protected final void unquell(@Nonnull final Messages.Entry... entries)
    {
        synchronized (_gate) {
            for (final Messages.Entry entry: entries) {
                _quelledEntries.remove(entry);
            }
        }
    }

    /**
     * Unquells an entry.
     *
     * @param messagesName The name of the class holding the entry.
     * @param entryName The name of the entry.
     *
     * @return The entry.
     */
    @Nonnull
    protected final Messages.Entry unquell(
            @Nonnull final String messagesName,
            @Nonnull final String entryName)
    {
        final Messages.Entry entry = _messagesEntry(messagesName, entryName);

        unquell(entry);

        return entry;
    }

    /**
     * Waits for mail.
     *
     * @return The optional mail.
     *
     * @throws InterruptedException When interrupted.
     */
    @Nonnull
    @CheckReturnValue
    protected Optional<Mail> waitForMail()
        throws InterruptedException
    {
        return FrameworkTests
            .getInstance()
            .waitForMail(getTimeout(DEFAULT_TIMEOUT));
    }

    @SuppressWarnings(
    {
        "unchecked", "rawtypes"
    })
    private static Messages.Entry _messagesEntry(
            final String messages,
            final String entry)
    {
        final Class enumType;

        try {
            enumType = Class
                .forName(
                    messages,
                    true,
                    Thread.currentThread().getContextClassLoader());
        } catch (final ClassNotFoundException exception) {
            throw new RuntimeException(exception);
        }

        return (Messages.Entry) Enum.valueOf(enumType, entry);
    }

    /** Default timeout. */
    public static final Optional<ElapsedTime> DEFAULT_TIMEOUT = Optional
        .of(ElapsedTime.fromMillis(15000));

    /** The system property prefix for RVPF specific properties. */
    public static final String SYSTEM_PROPERTY_PREFIX = "rvpf.";

    /** Timeout environment key. */
    public static final String TIMEOUT_ENV_KEY = "RVPF_TESTS_TIMEOUT";

    /** Timeout property. */
    public static final String TIMEOUT_PROPERTY = "tests.timeout";

    /**  */

    private static final Map<String, String> _PROPERTIES = new HashMap<String,
        String>();

    private final Bag<Messages.Entry> _expectedEntries = new HashCounterBag<>();
    private final ListMap<Messages.Entry, Message> _expectedMessages =
        new ListLinkedHashMap<>();
    private final Object _gate = new Object();
    private int _loggedProblems;
    private volatile Logger _logger;
    private final Bag<Messages.Entry> _quashedEntries = new HashCounterBag<>();
    private final Bag<Messages.Entry> _quelledEntries = new HashCounterBag<>();
    private ElapsedTime _timeout;
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
