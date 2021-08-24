/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DefEntityExporter.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.document.exporter;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.Entity;
import org.rvpf.base.UUID;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.document.loader.DocumentElement;

/**
 * Definition entity exporter.
 */
public abstract class DefEntityExporter
    extends EntityExporter
{
    /**
     * Creates an instance.
     *
     * @param owner The exporter owning this.
     * @param entityClass The Class of the entities exported by this.
     */
    protected DefEntityExporter(
            @Nonnull final ConfigExporter owner,
            @Nonnull final Class<? extends Entity> entityClass)
    {
        super(owner, entityClass);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void export(final Entity entity, final XMLElement element)
    {
        element.setAttribute(DocumentElement.NAME_ATTRIBUTE, entity.getName());

        final Optional<UUID> uuid = entity.getUUID();

        if (uuid.isPresent()) {
            element
                .setAttribute(
                    DocumentElement.UUID_ATTRIBUTE,
                    Optional.of(uuid.get().toRawString()));
        }
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
