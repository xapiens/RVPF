/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Info.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.base.alert;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.UUID;

/**
 * Service info.
 */
@ThreadSafe
public class Info
    extends Alert
{
    /**
     * Constructs an insgtance.
     *
     * <p>This is needed for an Externalizable implementation.</p>
     */
    public Info() {}

    /**
     * Constructs an instance.
     *
     * @param name The name of this alert.
     * @param serviceName The service generating this alert.
     * @param entityName The name of the entity associated with that service.
     * @param sourceUUID The UUID of the source of this alert.
     * @param info Additional informations.
     */
    public Info(
            @Nonnull final String name,
            @Nonnull final Optional<String> serviceName,
            @Nonnull final Optional<String> entityName,
            @Nonnull final Optional<UUID> sourceUUID,
            @Nonnull final Optional<Object> info)
    {
        super(name, serviceName, entityName, sourceUUID, info);
    }

    /** {@inheritDoc}
     */
    @Override
    protected String getTypeString()
    {
        return BaseMessages.INFO_TYPE.toString();
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
