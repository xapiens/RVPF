/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StringLogger.java 4109 2019-07-22 23:31:37Z SFB $
 */

package org.rvpf.base.logger;

import java.util.function.Supplier;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.rvpf.base.logger.Logger.LogLevel;
import org.rvpf.base.tool.Require;

/**
 * String logger.
 *
 * <p>Used by scripts to avoid the 'deprecated' warning for format strings.</p>
 */
@Immutable
public final class StringLogger
{
    /**
     * Constructs an instance.
     *
     * @param logger The logger.
     */
    private StringLogger(final Logger logger)
    {
        _logger = Require.notNull(logger);
    }

    /**
     * Gets a logger instance.
     *
     * @param ownerClass The class of the logger's owner.
     *
     * @return The logger instance.
     */
    @Nonnull
    @CheckReturnValue
    public static StringLogger getInstance(@Nonnull final Class<?> ownerClass)
    {
        return new StringLogger(Logger.getInstance(ownerClass));
    }

    /**
     * Gets a logger instance.
     *
     * <p>This is used by scripts to supply their Log4j logger in calls to RVPF.
     * </p>
     *
     * @param log4jLogger A log4j logger.
     *
     * @return The logger instance.
     */
    @Nonnull
    @CheckReturnValue
    public static StringLogger getInstance(
            @Nonnull final org.apache.logging.log4j.Logger log4jLogger)
    {
        return new StringLogger(Logger.getInstance(log4jLogger));
    }

    /**
     * Gets a logger instance.
     *
     * @param name The logger's name.
     *
     * @return The logger instance.
     */
    @Nonnull
    @CheckReturnValue
    public static StringLogger getInstance(@Nonnull final String name)
    {
        return new StringLogger(Logger.getInstance(name));
    }

    /**
     * Logs a debug message.
     *
     * @param messageSupplier A message supplier.
     */
    public void debug(@Nonnull final Supplier<Message> messageSupplier)
    {
        _logger.log(LogLevel.DEBUG, messageSupplier);
    }

    /**
     * Logs a debug message.
     *
     * @param format The message format.
     * @param params The message parameters.
     */
    public void debug(
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        _logger.log(LogLevel.DEBUG, format, params);
    }

    /**
     * Logs a debug message.
     *
     * @param cause The message cause.
     * @param format The message format.
     * @param params The message parameters.
     */
    public void debug(
            @Nonnull final Throwable cause,
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        _logger.log(LogLevel.DEBUG, cause, format, params);
    }

    /**
     * Logs an error message.
     *
     * @param format The message format.
     * @param params The message parameters.
     */
    public void error(
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        _logger.log(LogLevel.ERROR, format, params);
    }

    /**
     * Logs an error message.
     *
     * @param cause The message cause.
     * @param format The message format.
     * @param params The message parameters.
     */
    public void error(
            @Nonnull final Throwable cause,
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        _logger.log(LogLevel.ERROR, cause, format, params);
    }

    /**
     * Logs a fatal message.
     *
     * @param format The message format.
     * @param params The message parameters.
     */
    public void fatal(
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        _logger.log(LogLevel.FATAL, format, params);
    }

    /**
     * Logs a fatal message.
     *
     * @param cause The message cause.
     * @param format The message format.
     * @param params The message parameters.
     */
    public void fatal(
            @Nonnull final Throwable cause,
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        _logger.log(LogLevel.FATAL, cause, format, params);
    }

    /**
     * Gets the log level.
     *
     * @return The log level.
     */
    @Nonnull
    @CheckReturnValue
    public LogLevel getLogLevel()
    {
        return _logger.getLogLevel();
    }

    /**
     * Gets the logger.
     *
     * @return The logger.
     */
    @Nonnull
    @CheckReturnValue
    public Logger getLogger()
    {
        return _logger;
    }

    /**
     * Logs an info message.
     *
     * @param messageSupplier A message supplier.
     */
    public void info(@Nonnull final Supplier<Message> messageSupplier)
    {
        _logger.log(LogLevel.INFO, messageSupplier);
    }

    /**
     * Logs an info message.
     *
     * @param format The message format.
     * @param params The message parameters.
     */
    public void info(
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        _logger.log(LogLevel.INFO, format, params);
    }

    /**
     * Logs an info message.
     *
     * @param cause The message cause.
     * @param format The message format.
     * @param params The message parameters.
     */
    public void info(
            @Nonnull final Throwable cause,
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        _logger.log(LogLevel.INFO, cause, format, params);
    }

    /**
     * Asks if the debug level is enabled.
     *
     * @return True if enabled.
     */
    @CheckReturnValue
    public boolean isDebugEnabled()
    {
        return _logger.isDebugEnabled();
    }

    /**
     * Asks if a log level is enabled.
     *
     * @param logLevel The log level.
     *
     * @return True if enabled.
     */
    @CheckReturnValue
    public boolean isEnabledFor(final LogLevel logLevel)
    {
        return _logger.isEnabledFor(logLevel);
    }

    /**
     * Asks if the info level is enabled.
     *
     * @return True if enabled.
     */
    @CheckReturnValue
    public boolean isInfoEnabled()
    {
        return _logger.isInfoEnabled();
    }

    /**
     * Asks if the trace level is enabled.
     *
     * @return True if enabled.
     */
    @CheckReturnValue
    public boolean isTraceEnabled()
    {
        return _logger.isTraceEnabled();
    }

    /**
     * Logs a trace message.
     *
     * @param messageSupplier A message supplier.
     */
    public void trace(@Nonnull final Supplier<Message> messageSupplier)
    {
        _logger.log(LogLevel.TRACE, messageSupplier);
    }

    /**
     * Logs a trace message.
     *
     * @param format The message format.
     * @param params The message parameters.
     */
    public void trace(
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        _logger.log(LogLevel.TRACE, format, params);
    }

    /**
     * Logs a trace message.
     *
     * @param cause The message cause.
     * @param format The message format.
     * @param params The message parameters.
     */
    public void trace(
            @Nonnull final Throwable cause,
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        _logger.log(LogLevel.TRACE, cause, format, params);
    }

    /**
     * Logs a warning message.
     *
     * @param format The message format.
     * @param params The message parameters.
     */
    public void warn(
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        _logger.log(LogLevel.WARN, format, params);
    }

    /**
     * Logs a warning message.
     *
     * @param cause The message cause.
     * @param format The message format.
     * @param params The message parameters.
     */
    public void warn(
            @Nonnull final Throwable cause,
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        _logger.log(LogLevel.WARN, cause, format, params);
    }

    private final Logger _logger;
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
