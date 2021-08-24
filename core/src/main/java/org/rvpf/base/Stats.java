/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Stats.java 4109 2019-07-22 23:31:37Z SFB $
 */

package org.rvpf.base;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.logger.Message;
import org.rvpf.base.logger.Messages;
import org.rvpf.base.tool.Require;

/**
 * Stats.
 *
 * <p>Used to hold service statistics.</p>
 */
@ThreadSafe
public abstract class Stats
    implements Cloneable, Serializable
{
    /**
     * Constructs an instance.
     */
    protected Stats()
    {
        setMarkTime(DateTime.now());
        _initialize();
    }

    /**
     * Builds the text.
     */
    public synchronized void buildText()
    {
        DateTime logTime = getLogTime().orElse(null);

        if (logTime == null) {
            logTime = DateTime.now();
            setLogTime(logTime);
        }

        addLine(
            BaseMessages.STATS_SINCE,
            getMarkTime(),
            ElapsedTime
                .fromMillis(logTime.toMillis() - getMarkTime().toMillis())
                .toString());

        _clearLogTime();
    }

    /**
     * Clears the snapshot.
     */
    public final void clearSnapshot()
    {
        _snapshot = null;
    }

    /** {@inheritDoc}
     */
    @Override
    public Stats clone()
    {
        final Stats clone;

        try {
            clone = (Stats) super.clone();
        } catch (final CloneNotSupportedException exception) {
            throw new InternalError(exception);
        }

        return clone;
    }

    /**
     * Gets intermediate stats.
     *
     * @return Intermediate stats.
     */
    @Nonnull
    @CheckReturnValue
    public final Stats getIntermediate()
    {
        final Optional<Stats> snapshot = getSnapshot();
        final Stats stats;

        if (snapshot.isPresent()) {
            stats = clone();
            stats.clearSnapshot();
            stats.substract(snapshot.get());
        } else {
            stats = this;
        }

        return stats;
    }

    /**
     * Gets the mark time.
     *
     * @return The mark time.
     */
    @Nonnull
    @CheckReturnValue
    public final DateTime getMarkTime()
    {
        return _markTime;
    }

    /**
     * Gets the snapshot.
     *
     * @return The optional snapshot.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<Stats> getSnapshot()
    {
        return Optional.ofNullable(_snapshot);
    }

    /**
     * Gets the text.
     *
     * @return The text.
     */
    @Nonnull
    @CheckReturnValue
    public final StringBuilder getText()
    {
        return _text;
    }

    /**
     * Sets the snapshot.
     *
     * @param snapshot The snapshot.
     */
    public final void setSnapshot(@Nonnull final Stats snapshot)
    {
        snapshot.setMarkTime(DateTime.now());

        _snapshot = snapshot;
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        buildText();

        final String string = getText().toString();

        clearText();

        return string;
    }

    /**
     * Converts elapsed time in nanoseconds to a string.
     *
     * @param elapsed The elapsed time in nanoseconds.
     *
     * @return The elapsed time formatted as "day-hour:minute:second.millis".
     */
    @Nonnull
    @CheckReturnValue
    protected static String nanosToString(final long elapsed)
    {
        return ElapsedTime.fromNanos(elapsed).toString();
    }

    /**
     * Adds a line of text.
     *
     * @param entry The messages entry.
     * @param params The text parameters.
     */
    protected final void addLine(
            @Nonnull final Messages.Entry entry,
            @Nonnull final Object... params)
    {
        _addLine(entry.toString(), params);
    }

    /**
     * Adds a line of text.
     *
     * @param format The text.
     * @param params The text parameters.
     *
     * @deprecated For temporary use.
     */
    @Deprecated
    protected final void addLine(
            final String format,
            @Nonnull final Object... params)
    {
        _addLine(format, params);
    }

    /**
     * Adds text.
     *
     * @param text The text.
     */
    protected final void addText(@Nonnull final String text)
    {
        getText().append(text);
    }

    /**
     * Clears the margin.
     */
    protected final void clearMargin()
    {
        _margin = null;
    }

    /**
     * Clears the text.
     */
    protected final void clearText()
    {
        getText().setLength(0);
    }

    /**
     * Gets the log time.
     *
     * @return The optional log time.
     */
    @Nonnull
    @CheckReturnValue
    protected final Optional<DateTime> getLogTime()
    {
        return Optional.ofNullable(_logTime);
    }

    /**
     * Sets the log time.
     *
     * @param logTime The log time.
     */
    protected final void setLogTime(@Nonnull final DateTime logTime)
    {
        _logTime = Require.notNull(logTime);
    }

    /**
     * Sets the margin.
     *
     * @param margin The margin.
     */
    protected final void setMargin(@Nonnull final String margin)
    {
        _margin = Require.notNull(margin);
    }

    /**
     * Sets the mark time.
     *
     * @param markTime The mark time.
     */
    protected final void setMarkTime(@Nonnull final DateTime markTime)
    {
        _markTime = markTime;
    }

    /**
     * Substract a snapshot to get relative values.
     *
     * @param snapshot The stats snapshot.
     */
    protected void substract(@Nonnull final Stats snapshot)
    {
        setMarkTime(snapshot.getMarkTime());
    }

    private void _addLine(final String format, final Object... params)
    {
        final String margin = _margin;

        if (margin != null) {
            addText(margin);
        }

        addText(Message.format(format, params));

        if (margin == null) {
            addText("\n");
        }
    }

    private final void _clearLogTime()
    {
        _logTime = null;
    }

    private void _initialize()
    {
        _text = new StringBuilder();
    }

    private void readObject(
            final ObjectInputStream stream)
        throws IOException, ClassNotFoundException
    {
        stream.defaultReadObject();
        _initialize();
    }

    private static final long serialVersionUID = 1L;

    private transient volatile DateTime _logTime;
    private transient volatile String _margin;
    private volatile DateTime _markTime;
    private volatile Stats _snapshot;
    private transient StringBuilder _text;
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
