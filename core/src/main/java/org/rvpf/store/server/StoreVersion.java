/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreVersion.java 3900 2019-02-19 20:43:24Z SFB $
 */

package org.rvpf.store.server;

import javax.annotation.concurrent.Immutable;

import org.rvpf.base.util.Version;

/**
 * RVPF-Store version.
 *
 * <p>By providing a concrete subclass of {@link org.rvpf.base.util.Version},
 * this class will supply the implementation version for its package from the
 * manifest of its jar.</p>
 */
@Immutable
public final class StoreVersion
    extends Version
{
    /** Copyright notice. */
    public static final String COPYRIGHT =
        "Copyright (c) 2003-2019 Serge Brisson";
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
