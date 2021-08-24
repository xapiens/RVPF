/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PointValuesLoaderInput.java 3984 2019-05-14 12:28:33Z SFB $
 */

package org.rvpf.base.xml.streamer;

import java.io.Reader;

import java.util.NoSuchElementException;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.DateTime;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.NoCloseReader;
import org.rvpf.base.util.PointValuesLoader;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.VersionedValue;

/**
 * Point values loader input.
 *
 * <p>Helper class for scripts: it produces {@link PointValue}s for a
 * {@link PointValuesLoader} from the XML text of a {@link Reader}.</p>
 */
@NotThreadSafe
public final class PointValuesLoaderInput
    implements PointValuesLoader.Input
{
    /**
     * Constructs an instance.
     *
     * @param reader A reader for the XML text.
     */
    public PointValuesLoaderInput(@Nonnull final Reader reader)
    {
        _streamer = Streamer.newInstance();

        if (!_streamer.setUp(Optional.empty(), Optional.empty())) {
            throw new RuntimeException();
        }

        _input = _streamer.newInput(new NoCloseReader(Require.notNull(reader)));
    }

    /**
     * Closes the input.
     */
    public void close()
    {
        _input.close();
        _streamer.tearDown();
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
    public Optional<PointValue> input()
        throws Exception
    {
        Optional<PointValue> pointValue;

        try {
            pointValue = Optional.of((PointValue) _input.next());
        } catch (final NoSuchElementException exception) {
            pointValue = Optional.empty();
        }

        if (pointValue.isPresent()) {
            if (pointValue.get() instanceof VersionedValue) {
                final DateTime version = ((VersionedValue) pointValue.get())
                    .getVersion();

                if (version != null) {
                    if (_firstVersion == null) {
                        _firstVersion = version;
                    }

                    _lastVersion = version;
                }
            }
        }

        return pointValue;
    }

    private DateTime _firstVersion;
    private final Streamer.Input _input;
    private DateTime _lastVersion;
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
