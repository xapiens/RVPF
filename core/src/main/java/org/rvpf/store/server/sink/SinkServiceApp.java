/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SinkServiceApp.java 4019 2019-05-23 14:14:01Z SFB $
 */

package org.rvpf.store.server.sink;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

import org.rvpf.metadata.entity.OriginEntity;
import org.rvpf.store.server.StoreServiceApp;

/**
 * Sink application.
 */
public interface SinkServiceApp
    extends StoreServiceApp
{
    /**
     * Gets the processor.
     *
     * @return The processor.
     */
    @Nullable
    @CheckReturnValue
    OriginEntity getProcessor();
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
