/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PAPTestsSupport.java 3974 2019-05-11 15:34:04Z SFB $
 */

package org.rvpf.tests.pap;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.metadata.Metadata;

/**
 * PAP tests support.
 */
public abstract class PAPTestsSupport
{
    /**
     * Constructs an instance.
     *
     * @param metadata The optional metadata.
     */
    protected PAPTestsSupport(@Nonnull final Optional<Metadata> metadata)
    {
        _metadata = metadata;
    }

    /**
     * Gets the metadata.
     *
     * @return The optional metadata.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Metadata> getMetadata()
    {
        return _metadata;
    }

    private final Optional<Metadata> _metadata;
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
