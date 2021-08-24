/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: NoCloseReader.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.base.util;

import java.io.FilterReader;
import java.io.Reader;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/** No close reader.
 *
 * <p>This is used to avoid closing a reader at the completion of some
 * processing. To finally close the original reader, either keep a reference to
 * it or call {@link #getReader}.</p>
 */
@ThreadSafe
public class NoCloseReader
    extends FilterReader
{
    /** Constructs an instance.
     *
     * @param reader The wrapped reader.
     */
    public NoCloseReader(@Nonnull final Reader reader)
    {
        super(reader);
    }

    /** {@inheritDoc}
     *
     * <p>Ignores any close request.</p>
     */
    @Override
    public void close() {}

    /** Gets the original reader.
     *
     * @return The original reader.
     */
    @Nonnull
    @CheckReturnValue
    public Reader getReader()
    {
        return in;
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
