/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SystemExit.java 4107 2019-07-13 13:18:26Z SFB $
 */

package org.rvpf.base.tool;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * System exit.
 *
 * Used to exit the JVM.
 */
@Immutable
public final class SystemExit
{
    /**
     * No instances.
     */
    private SystemExit() {}

    /**
     * Called to exit the JVM.
     *
     * @param args The program arguments.
     */
    public static void main(@Nonnull final String[] args)
    {
        System.exit(0);
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
