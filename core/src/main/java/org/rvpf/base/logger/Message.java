/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Message.java 4109 2019-07-22 23:31:37Z SFB $
 */

package org.rvpf.base.logger;

import java.text.MessageFormat;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.logger.log4j.AbstractMessage;
import org.rvpf.base.tool.Require;

/**
 * Provides message formatting and supports messages resources provided by
 * the {@link Messages} class.
 *
 * <p>Note: the formatting is done only when at least one parameter is supplied;
 * otherwise, the string is returned unmodified (single quotes do not have to be
 * doubled and curly braces are ordinary characters).</p>
 */
@ThreadSafe
public final class Message
    extends AbstractMessage
{
    /**
     * Constructs an instance.
     *
     * @param entry The messages entry.
     * @param params The message parameters.
     */
    public Message(
            @Nonnull final Messages.Entry entry,
            @Nonnull final Object... params)
    {
        this(null, null, Require.notNull(entry), params);
    }

    /**
     * Constructs an instance.
     *
     * @param format The message format.
     * @param params The message parameters.
     */
    public Message(
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        this(null, Require.notNull(format), null, params);
    }

    /**
     * Constructs an instance.
     *
     * @param cause The message cause.
     * @param entry The messages entry.
     * @param params The message parameters.
     */
    public Message(
            @Nonnull final Throwable cause,
            @Nonnull final Messages.Entry entry,
            @Nonnull final Object... params)
    {
        this(Require.notNull(cause), null, Require.notNull(entry), params);
    }

    /**
     * Constructs an instance.
     *
     * @param cause The message cause.
     * @param format The message format.
     * @param params The message parameters.
     */
    public Message(
            @Nonnull final Throwable cause,
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        this(Require.notNull(cause), Require.notNull(format), null, params);
    }

    private Message(
            final Throwable cause,
            final String format,
            final Messages.Entry entry,
            final Object... params)
    {
        _cause = cause;
        _pattern = format;
        _entry = entry;
        _params = params;
    }

    /**
     * Formats a message.
     *
     * @param entry The messages entry.
     * @param params The message parameters.
     *
     * @return The formatted message.
     */
    @Nonnull
    @CheckReturnValue
    public static String format(
            @Nonnull final Messages.Entry entry,
            @Nonnull final Object... params)
    {
        return format(entry.toString(), params);
    }

    /**
     * Formats a message.
     *
     * @param format The message format.
     * @param params The message parameters.
     *
     * @return The formatted message.
     */
    @Nonnull
    @CheckReturnValue
    public static String format(
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        return (params.length > 0)? MessageFormat
            .format(format, params): format;
    }

    /**
     * Marks a message format for conversion to resource.
     *
     * <p>The message formats needing conversion can be found by performing a
     * search for all calls to this method.</p>
     *
     * @param format The message format.
     * @param params The message parameters.
     *
     * @return The message format.
     */
    @Nonnull
    @CheckReturnValue
    public static String mark(
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        return format(format, params);
    }

    /**
     * Formats this.
     *
     * @param params The message parameters.
     *
     * @return The formatted text.
     */
    @Nonnull
    @CheckReturnValue
    public synchronized String format(@Nonnull final Object... params)
    {
        if (_format == null) {
            final String pattern = (_pattern != null)? _pattern: _entry
                .toString();

            if (params.length == 0) {
                return pattern;
            }

            _format = new MessageFormat(pattern);
        }

        return _format.format(params);
    }

    /**
     * Gets the cause of this message.
     *
     * @return The optional cause.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Throwable> getCause()
    {
        return Optional.ofNullable(_cause);
    }

    /**
     * Gets the entry.
     *
     * @return The optional entry.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Messages.Entry> getEntry()
    {
        return Optional.ofNullable(_entry);
    }

    /**
     * Gets the params.
     *
     * @return The params.
     */
    @Nonnull
    @CheckReturnValue
    public Object[] getParams()
    {
        return _params;
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return format(_params);
    }

    private static final long serialVersionUID = 1L;

    private final Throwable _cause;
    private final Messages.Entry _entry;
    private MessageFormat _format;
    private final Object[] _params;
    private final String _pattern;
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
