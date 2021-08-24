/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PipeRequest.java 4040 2019-05-31 18:55:08Z SFB $
 */

package org.rvpf.base.pipe;

import java.io.IOException;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.DateTime;
import org.rvpf.base.logger.Message;
import org.rvpf.base.logger.Messages;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.VersionedValue;

/**
 * Pipe request.
 */
@Immutable
public abstract class PipeRequest
{
    /**
     * Constructs an instance.
     *
     * @param requestID The request ID.
     * @param version The request format version.
     */
    protected PipeRequest(@Nonnull final String requestID, final int version)
    {
        _requestID = requestID;
        _version = version;
    }

    /**
     * Cleans a string by replacing control chars with spaces.
     *
     * @param string The string to clean.
     *
     * @return The clean string.
     */
    @Nonnull
    @CheckReturnValue
    public static String cleanString(@Nonnull final String string)
    {
        final Matcher matcher = _CLEAN_PATTERN.matcher(string.trim());

        return matcher.replaceAll(" ");
    }

    /**
     * Logs a message at the DEBUG level.
     *
     * @param entry The messages entry.
     * @param params The message parameters.
     */
    public static void debug(
            @Nonnull final Messages.Entry entry,
            @Nonnull final Object... params)
    {
        log(LogLevel.DEBUG, entry, params);
    }

    /**
     * Logs a message at the DEBUG level.
     *
     * @param format The message format.
     * @param params The message parameters.
     */
    public static void debug(
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        log(LogLevel.DEBUG, format, params);
    }

    /**
     * Logs a message at the ERROR level.
     *
     * @param entry The messages entry.
     * @param params The message parameters.
     *
     * @return A runtime exception.
     */
    @Nonnull
    @CheckReturnValue
    public static RuntimeException error(
            @Nonnull final Messages.Entry entry,
            @Nonnull final Object... params)
    {
        return error(entry.toString(), params);
    }

    /**
     * Logs a message at the ERROR level.
     *
     * @param format The message format.
     * @param params The message parameters.
     *
     * @return A runtime exception.
     */
    @Nonnull
    @CheckReturnValue
    public static RuntimeException error(
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        final String text = Message.format(format, params);

        log(LogLevel.ERROR, BaseMessages.VERBATIM, text);

        return new RuntimeException(text);
    }

    /**
     * Logs a message at the FATAL level.
     *
     * @param entry The messages entry.
     * @param params The message parameters.
     *
     * @return A runtime exception.
     */
    @Nonnull
    @CheckReturnValue
    public static RuntimeException fatal(
            @Nonnull final Messages.Entry entry,
            @Nonnull final Object... params)
    {
        return fatal(entry.toString(), params);
    }

    /**
     * Logs a message at the FATAL level.
     *
     * @param format The message format.
     * @param params The message parameters.
     *
     * @return A runtime exception.
     */
    @Nonnull
    @CheckReturnValue
    public static RuntimeException fatal(
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        final String text = Message.format(format, params);

        log(LogLevel.FATAL, BaseMessages.VERBATIM, text);

        return new RuntimeException(text);
    }

    /**
     * Logs a message at the INFO level.
     *
     * @param entry The messages entry.
     * @param params The message parameters.
     */
    public static void info(
            @Nonnull final Messages.Entry entry,
            @Nonnull final Object... params)
    {
        log(LogLevel.INFO, entry, params);
    }

    /**
     * Logs a message at the INFO level.
     *
     * @param format The message format.
     * @param params The message parameters.
     */
    public static void info(
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        log(LogLevel.INFO, format, params);
    }

    /**
     * Logs a message at the specified level.
     *
     * @param level The log level.
     * @param entry The messages entry.
     * @param params The message parameters.
     */
    public static void log(
            @Nonnull final LogLevel level,
            @Nonnull final Messages.Entry entry,
            @Nonnull final Object... params)
    {
        log(level, entry.toString(), params);
    }

    /**
     * Logs a message at the specified level.
     *
     * @param level The log level.
     * @param format The message format.
     * @param params The message parameters.
     */
    public static void log(
            @Nonnull final LogLevel level,
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        System.err.println(level.name() + ' ' + Message.format(format, params));
        System.err.flush();
    }

    /**
     * Converts a point value to a string.
     *
     * @param pointValue The point value.
     *
     * @return The string.
     */
    @Nonnull
    @CheckReturnValue
    public static String pointValueToString(
            @Nonnull final PointValue pointValue)
    {
        final StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(pointValue.getPointName().orElse(null));

        stringBuilder.append(' ');
        stringBuilder.append(pointValue.getStamp().toFullString());

        if (pointValue.getState() != null) {
            final String value = String.valueOf(pointValue.getState());

            stringBuilder.append(" [");

            for (int i = 0; i < value.length(); ++i) {
                final char c = value.charAt(i);

                if (c == '[') {
                    stringBuilder.append(']');
                } else if (c == ']') {
                    stringBuilder.append('[');
                }

                stringBuilder.append(c);
            }

            stringBuilder.append(']');
        }

        if (pointValue.getValue() != null) {
            final String value = String.valueOf(pointValue.getValue());

            stringBuilder.append(" \"");

            for (int i = 0; i < value.length(); ++i) {
                final char c = value.charAt(i);

                if (c == '"') {
                    stringBuilder.append('"');
                }

                stringBuilder.append(c);
            }

            stringBuilder.append('"');
        } else if (pointValue.isDeleted()) {
            stringBuilder.append(" -");
        }

        return stringBuilder.toString();
    }

    /**
     * Converts a string to a point value.
     *
     * @param string The string.
     *
     * @return The point value (null on failure).
     */
    @Nullable
    @CheckReturnValue
    public static PointValue stringToPointValue(@Nonnull String string)
    {
        final String[] words = SPACE_PATTERN.split(string, 3);

        if (words.length < 1) {
            return null;
        }

        PointValue pointValue = new PointValue(
            words[0],
            Optional.empty(),
            null,
            null);

        if (words.length >= 2) {
            final Optional<DateTime> stamp = DateTime
                .fromString(Optional.of(words[1]));

            if (stamp.isPresent()) {
                pointValue.setStamp(stamp.get());
            }

            if (words.length > 2) {
                final StringBuilder stringBuilder = new StringBuilder();

                string = words[2].trim();

                if ((string.length() > 0) && (string.charAt(0) == '[')) {
                    int index = 0;
                    boolean leftBracketSeen = false;
                    boolean rightBracketSeen = false;

                    for (;;) {
                        if (++index >= string.length()) {
                            if (!rightBracketSeen) {
                                return null;
                            }

                            break;
                        }

                        final char c = string.charAt(index);

                        if (rightBracketSeen) {
                            if (c == '[') {
                                stringBuilder.append(c);
                                rightBracketSeen = false;
                            } else {
                                ++index;

                                break;
                            }
                        } else if (leftBracketSeen) {
                            if (c == ']') {
                                stringBuilder.append(c);
                                leftBracketSeen = false;
                            } else {
                                return null;
                            }
                        } else if (c == '[') {
                            leftBracketSeen = true;
                        } else if (c == ']') {
                            rightBracketSeen = true;
                        } else {
                            stringBuilder.append(c);
                        }
                    }

                    pointValue.setState(stringBuilder.toString());
                    stringBuilder.setLength(0);
                    string = string.substring(index).trim();
                }

                if ((string.length() > 0) && (string.charAt(0) == '"')) {
                    int index = 0;
                    boolean quoteSeen = false;

                    for (;;) {
                        if (++index >= string.length()) {
                            if (!quoteSeen) {
                                return null;
                            }

                            break;
                        }

                        final char c = string.charAt(index);

                        if (quoteSeen) {
                            if (c == '"') {
                                stringBuilder.append(c);
                                quoteSeen = false;
                            } else {
                                return null;
                            }
                        } else if (c == '"') {
                            quoteSeen = true;
                        } else {
                            stringBuilder.append(c);
                        }
                    }

                    pointValue.setValue(stringBuilder.toString());
                } else if (string.length() > 0) {
                    pointValue = (string
                        .charAt(
                            0) == '-')? new VersionedValue.Deleted(
                                pointValue): null;
                }
            }
        }

        return pointValue;
    }

    /**
     * Logs a message at the TRACE level.
     *
     * @param entry The messages entry.
     * @param params The message parameters.
     */
    public static void trace(
            @Nonnull final Messages.Entry entry,
            @Nonnull final Object... params)
    {
        log(LogLevel.TRACE, entry, params);
    }

    /**
     * Logs a message at the TRACE level.
     *
     * @param format The message format.
     * @param params The message parameters.
     */
    public static void trace(
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        log(LogLevel.TRACE, format, params);
    }

    /**
     * Logs a message at the WARN level.
     *
     * @param entry The messages entry.
     * @param params The message parameters.
     */
    public static void warn(
            @Nonnull final Messages.Entry entry,
            @Nonnull final Object... params)
    {
        log(LogLevel.WARN, entry, params);
    }

    /**
     * Logs a message at the WARN level.
     *
     * @param format The message format.
     * @param params The message parameters.
     */
    public static void warn(
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        log(LogLevel.WARN, format, params);
    }

    /**
     * Gets the request ID.
     *
     * @return The request ID.
     */
    @Nonnull
    @CheckReturnValue
    public final String getRequestID()
    {
        return _requestID;
    }

    /**
     * Gets the version.
     *
     * @return The version.
     */
    @Nonnull
    @CheckReturnValue
    public final int getVersion()
    {
        return _version;
    }

    /**
     * Returns the first line for a request.
     *
     * <p>Drops lines containing only whitespace characters. Also recognizes
     * synchronization requests and responds.</p>
     *
     * @return The first request line (empty on end of input).
     */
    @Nonnull
    @CheckReturnValue
    protected static Optional<String> firstLine()
    {
        String line;

        for (;;) {
            line = _nextLine(false);

            if ((line == null) || (line.indexOf(' ') >= 0)) {
                break;
            }

            if ("0".equals(line)) {
                line = null;

                break;
            }

            writeLine(line);
        }

        return Optional.ofNullable(line);
    }

    /**
     * Returns the next line for a request.
     *
     * <p>Drops lines containing only whitespace characters.</p>
     *
     * @return The first request line (throws a RuntimeException if none).
     */
    @Nonnull
    @CheckReturnValue
    protected static String nextLine()
    {
        return _nextLine(true);
    }

    /**
     * Returns the point value from the next input line.
     *
     * @param stampRequired True if a stamp is required.
     *
     * @return The point value.
     */
    @Nonnull
    @CheckReturnValue
    protected static PointValue nextPointValue(final boolean stampRequired)
    {
        final String line = _nextLine(true);
        final PointValue pointValue = stringToPointValue(line);

        if ((pointValue == null) || (stampRequired && !pointValue.hasStamp())) {
            throw error("Unexpected point value format: " + line);
        }

        return pointValue;
    }

    /**
     * Parses an int.
     *
     * @param text The text representation.
     *
     * @return The int.
     */
    @CheckReturnValue
    protected static int parseInt(@Nonnull final String text)
    {
        try {
            return Integer.parseInt(text);
        } catch (final NumberFormatException exception) {
            throw error("Bad number value: " + text);
        }
    }

    /**
     * Writes a line.
     *
     * @param line The line.
     */
    protected static void writeLine(@Nonnull final String line)
    {
        System.out.println(line);
        System.out.flush();
        trace("Sent: {" + line + "}");
    }

    /**
     * Writes a point value.
     *
     * @param pointValue The point value.
     */
    protected static void writePointValue(@Nonnull final PointValue pointValue)
    {
        writeLine(pointValueToString(pointValue));
    }

    private static String _nextLine(final boolean required)
    {
        final StringBuilder stringBuilder = new StringBuilder();
        int next;
        String line;

        for (;;) {
            try {
                next = System.in.read();
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
            }

            if (next == -1) {
                if (stringBuilder.length() > 0) {
                    warn("Lost characters at end of input");
                }

                if (required) {
                    throw error("Unexpected end of input");
                }

                line = null;

                break;
            }

            if (LINE_SEPARATOR.endsWith(String.valueOf((char) next))) {
                line = stringBuilder.toString().trim();

                if (line.length() > 0) {
                    break;
                }

                continue;
            }

            if (LINE_SEPARATOR.indexOf(next) < 0) {
                stringBuilder.append((char) next);
            }
        }

        trace("Received: {" + line + "}");

        return line;
    }

    /** Debug level. */
    public static final String DEBUG_LEVEL = "DEBUG";

    /** Error level. */
    public static final String ERROR_LEVEL = "ERROR";

    /** Fatal level. */
    public static final String FATAL_LEVEL = "FATAL";

    /** Info level. */
    public static final String INFO_LEVEL = "INFO";

    /** Line separator. */
    public static final String LINE_SEPARATOR = System
        .getProperty("line.separator");

    /** Space pattern. */
    public static final Pattern SPACE_PATTERN = Pattern.compile(" ");

    /** Trace level. */
    public static final String TRACE_LEVEL = "TRACE";

    /** Warn level. */
    public static final String WARN_LEVEL = "WARN";

    /**  */

    private static final Pattern _CLEAN_PATTERN = Pattern
        .compile("[\\x00-\\x1f]|[\\x7f-\\x9f]");

    private final String _requestID;
    private final int _version;

    /**
     * Log level.
     */
    public enum LogLevel
    {
        FATAL,
        ERROR,
        WARN,
        INFO,
        DEBUG,
        TRACE;
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
