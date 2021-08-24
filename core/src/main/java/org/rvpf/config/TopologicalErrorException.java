/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TopologicalErrorException.java 3902 2019-02-20 22:30:12Z SFB $
 */

package org.rvpf.config;

import javax.annotation.Nonnull;

import org.rvpf.base.Entity;
import org.rvpf.base.logger.Message;
import org.rvpf.service.ServiceMessages;

/**
 * Topological error exception.
 *
 * <p>Instances of this class are thrown when a definition loop is detected in
 * the configuration or the metadata. Most occurences of this problem are
 * expected to be located in the relations between points.</p>
 */
public final class TopologicalErrorException
    extends Exception
{
    /**
     * Creates an instance.
     *
     * @param entity The exception entity.
     */
    public TopologicalErrorException(@Nonnull final Entity entity)
    {
        super(
            Message
                .format(
                    ServiceMessages.ENTITY_RECURSES,
                    entity.getElementName(),
                    entity.getName()));
    }

    private static final long serialVersionUID = 1L;
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
