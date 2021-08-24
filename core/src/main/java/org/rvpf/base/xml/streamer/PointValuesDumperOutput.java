/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PointValuesDumperOutput.java 3896 2019-02-16 13:42:30Z SFB $
 */

package org.rvpf.base.xml.streamer;

import java.io.Writer;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.DateTime;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.PointValuesDumper;
import org.rvpf.base.value.VersionedValue;

/**
 * Point values dumper output.
 *
 * <p>Helper class for scripts: it formats {@link VersionedValue}s produced by a
 * {@link PointValuesDumper} to XML text on a {@link Writer}.</p>
 */
@NotThreadSafe
public final class PointValuesDumperOutput
    implements PointValuesDumper.Output
{
    /**
     * Constructs an instance.
     *
     * @param writer A writer for the XML text.
     */
    public PointValuesDumperOutput(@Nonnull final Writer writer)
    {
        _streamer = Streamer.newInstance();

        if (!_streamer.setUp(Optional.empty(), Optional.empty())) {
            throw new RuntimeException();
        }

        _output = _streamer.newOutput(writer);
    }

    /**
     * Closes the output.
     */
    public void close()
    {
        _output.close();
        _streamer.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    public void flush()
    {
        _output.flush();
    }

    /**
     * Gets the first version.
     *
     * @return The optional first version.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<DateTime> getFirstVersion()
    {
        return Optional.ofNullable(_firstVersion);
    }

    /**
     * Gets the last version.
     *
     * @return The optional last version.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<DateTime> getLastVersion()
    {
        return Optional.ofNullable(_lastVersion);
    }

    /** {@inheritDoc}
     */
    @Override
    public void output(final VersionedValue versionedValue)
        throws Exception
    {
        if (!_output.add(Require.notNull(versionedValue))) {
            throw new InternalError();
        }

        if (_firstVersion == null) {
            _firstVersion = versionedValue.getVersion();
        }

        _lastVersion = versionedValue.getVersion();
    }

    private DateTime _firstVersion;
    private DateTime _lastVersion;
    private final Streamer.Output _output;
    private final Streamer _streamer;
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
