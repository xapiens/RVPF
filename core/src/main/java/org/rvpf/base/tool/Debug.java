/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Debug.java 4110 2019-07-24 18:17:42Z SFB $
 */

package org.rvpf.base.tool;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.nio.charset.StandardCharsets;

import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;

/**
 * Debug. Provides convenience methods to help debugging.
 *
 * <p>Used for temporary logging; it is easy to clean up a project by getting a
 * list of all references to this class.</p>
 */
@Immutable
public final class Debug
{
    /**
     * Constructs an instance.
     */
    private Debug()
    {
        _stackTrace = Thread.currentThread().getStackTrace();

        final int callerIndex = Profiler
            .stackTraceStartIndex(_stackTrace, Optional.of(_CLASS_NAME));

        if (callerIndex >= 0) {
            _logger = Logger
                .getInstance(_stackTrace[callerIndex].getClassName());
        } else {
            _logger = Logger.getInstance(_CLASS_NAME);
        }
    }

    /**
     * Exits the current program with a failure status.
     */
    public static void exit()
    {
        System.exit(-1);
    }

    /**
     * Logs a throwable.
     *
     * @param throwable The throwable.
     */
    public static void log(@Nonnull final Throwable throwable)
    {
        new Debug()._log(throwable, "{0}", throwable.getMessage());
    }

    /**
     * Logs a message.
     *
     * @param format The message format.
     * @param params The message parameters.
     */
    public static void log(
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        new Debug()._log(format, params);
    }

    /**
     * Logs a message with a throwable.
     *
     * @param throwable The throwable.
     * @param format The message format.
     * @param params The message parameters (optional).
     */
    public static void log(
            @Nonnull final Throwable throwable,
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        new Debug()._log(throwable, format, params);
    }

    /**
     * Logs a stack trace of the current thread.
     */
    public static void logStackTrace()
    {
        new Debug()._logStackTrace();
    }

    /**
     * Logs the stack trace of the current thread with a throwable.
     *
     * @param throwable The throwable.
     */
    public static void logStackTrace(@Nonnull final Throwable throwable)
    {
        new Debug()._logStackTrace(throwable);
    }

    /**
     * Logs a stack trace of the current thread with a message.
     *
     * @param format The message format.
     * @param params The message parameters.
     */
    public static void logStackTrace(
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        new Debug()._logStackTrace(format, params);
    }

    /**
     * Logs a stack trace of the current thread with a message and a throwable.
     *
     * @param throwable The throwable.
     * @param format The message format.
     * @param params The message parameters.
     */
    public static void logStackTrace(
            @Nonnull final Throwable throwable,
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        new Debug()._logStackTrace(throwable, format, params);
    }

    /**
     * Logs a stack trace of each thread.
     */
    public static void logStackTraces()
    {
        new Debug()._logStackTraces();
    }

    /**
     * Logs the stack trace of each thread with a throwable.
     *
     * @param throwable The throwable.
     */
    public static void logStackTraces(@Nonnull final Throwable throwable)
    {
        new Debug()._logStackTraces(throwable);
    }

    /**
     * Logs a stack trace of each thread with a message.
     *
     * @param format The message format.
     * @param params The message parameters.
     */
    public static void logStackTraces(
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        new Debug()._logStackTraces(format, params);
    }

    /**
     * Logs a stack trace of each thread with a message and a throwable.
     *
     * @param throwable The throwable.
     * @param format The message format.
     * @param params The message parameters.
     */
    public static void logStackTraces(
            @Nonnull final Throwable throwable,
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        new Debug()._logStackTraces(throwable, format, params);
    }

    /** The 'print' methods output directly to the error stream. */

    /**
     * Prints a message.
     *
     * @param format The message format.
     * @param params The message parameters.
     */
    public static void print(
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        System.err.println(Message.format(format, params));
    }

    /**
     * Prints a stack trace of the current thread.
     */
    public static void printStackTrace()
    {
        printStackTrace(System.err);
    }

    /**
     * Prints a stack trace of the current thread.
     *
     * @param outputStream The destination output stream.
     */
    public static void printStackTrace(@Nonnull final OutputStream outputStream)
    {
        final PrintWriter printWriter = new PrintWriter(
            new OutputStreamWriter(outputStream, StandardCharsets.UTF_8),
            true);

        printStackTrace(printWriter);
        printWriter.flush();
    }

    /**
     * Prints a stack trace of the current thread.
     *
     * @param printWriter The destination print writer.
     */
    public static void printStackTrace(@Nonnull final PrintWriter printWriter)
    {
        Profiler
            .printStackTrace(
                Thread.currentThread(),
                Thread.currentThread().getStackTrace(),
                Optional.of(_CLASS_NAME),
                Integer.MAX_VALUE,
                printWriter);
    }

    /**
     * Prints a stack trace of the current thread with a message.
     *
     * @param format The message format.
     * @param params The message parameters.
     */
    public static void printStackTrace(
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        print(format, params);
        printStackTrace();
    }

    /**
     * Prints a stack trace of each thread.
     */
    public static void printStackTraces()
    {
        printStackTraces(System.err);
    }

    /**
     * Prints a stack traces of each thread.
     *
     * @param outputStream The destination output stream.
     */
    public static void printStackTraces(
            @Nonnull final OutputStream outputStream)
    {
        final PrintWriter printWriter = new PrintWriter(
            new OutputStreamWriter(outputStream, StandardCharsets.UTF_8),
            true);

        printStackTraces(printWriter);
        printWriter.flush();
    }

    /**
     * Prints a stack traces of each thread.
     *
     * @param printWriter The destination print writer.
     */
    public static void printStackTraces(@Nonnull final PrintWriter printWriter)
    {
        final Map<Thread, StackTraceElement[]> stackTraces = Thread
            .getAllStackTraces();

        for (final Map.Entry<Thread, StackTraceElement[]> entry:
                stackTraces.entrySet()) {
            Profiler
                .printStackTrace(
                    entry.getKey(),
                    entry.getValue(),
                    Optional.of(_CLASS_NAME),
                    Integer.MAX_VALUE,
                    printWriter);
        }
    }

    /**
     * Prints a stack trace of each thread with a message.
     *
     * @param format The message format.
     * @param params The message parameters.
     */
    public static void printStackTraces(
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        print(format, params);
        printStackTraces();
    }

    private void _log(final String format, final Object... params)
    {
        _logger.log(Logger.LogLevel.DEBUG, "#" + format, params);
    }

    private void _log(
            final Throwable throwable,
            final String format,
            final Object... params)
    {
        _logger.log(Logger.LogLevel.DEBUG, throwable, "#" + format, params);
    }

    private void _logStackTrace()
    {
        Profiler
            .printStackTrace(
                Thread.currentThread(),
                _stackTrace,
                Optional.of(_CLASS_NAME),
                Integer.MAX_VALUE,
                _logger.getPrintWriter(Logger.LogLevel.DEBUG));
    }

    private void _logStackTrace(final Throwable throwable)
    {
        _logStackTrace(throwable, "{0}", throwable);
    }

    private void _logStackTrace(final String format, final Object... params)
    {
        _log(format, params);
        _logStackTrace();
    }

    private void _logStackTrace(
            final Throwable throwable,
            final String format,
            final Object... params)
    {
        _log(throwable, format, params);
        _logStackTrace();
    }

    private void _logStackTraces()
    {
        printStackTraces(_logger.getPrintWriter(Logger.LogLevel.DEBUG));
    }

    private void _logStackTraces(final Throwable throwable)
    {
        _logStackTraces(throwable, "{0}", throwable);
    }

    private void _logStackTraces(final String format, final Object... params)
    {
        _log(format, params);
        _logStackTraces();
    }

    private void _logStackTraces(
            final Throwable throwable,
            final String format,
            final Object... params)
    {
        _log(throwable, format, params);
        _logStackTraces();
    }

    private static final String _CLASS_NAME = Debug.class.getName();

    private final Logger _logger;
    private final StackTraceElement[] _stackTrace;
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
