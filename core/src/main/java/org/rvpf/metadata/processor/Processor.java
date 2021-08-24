/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Processor.java 3959 2019-05-06 19:41:43Z SFB $
 */

package org.rvpf.metadata.processor;

import java.util.Collection;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.value.PointValue;

/**
 * Processor.
 */
public interface Processor
{
    /**
     * Processes point values.
     *
     * @param pointValues The input point values.
     *
     * @return The output point values (empty to retry smaller).
     *
     * @throws InterruptedException When the service is stopped.
     * @throws ServiceNotAvailableException When the service is not available.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Collection<PointValue>> process(
            @Nonnull Collection<PointValue> pointValues)
        throws InterruptedException, ServiceNotAvailableException;
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
