/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.document.loader;

import java.util.Optional;

import org.rvpf.base.exception.ValidationException;
import org.rvpf.metadata.entity.OriginEntity;

/**
 * Attributes Entry.
 */
final class AttributesLoader
    extends MetadataElementLoader
{
    /** {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void process()
        throws ValidationException
    {
        final Optional<? extends DocumentElement> metadataElement =
            (Optional<? extends DocumentElement>) getElement()
                .getParent();

        if (metadataElement.isPresent()) {
            getMetadata()
                .getAttributes()
                .addAll(getAttributes(metadataElement.get()).orElse(null));
        }
    }

    /** {@inheritDoc}
     */
    @Override
    String getEntityPrefix()
    {
        return OriginEntity.ENTITY_PREFIX;
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
