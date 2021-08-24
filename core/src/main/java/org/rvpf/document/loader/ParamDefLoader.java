/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ParamDefLoader.java 3942 2019-04-30 19:47:54Z SFB $
 */

package org.rvpf.document.loader;

import org.rvpf.base.exception.ValidationException;
import org.rvpf.metadata.entity.ParamDefEntity;

/**
 * ParamDef loader.
 */
final class ParamDefLoader
    extends MetadataElementLoader
{
    /** {@inheritDoc}
     */
    @Override
    protected void process()
        throws ValidationException
    {
        final DocumentElement paramDefElement = getElement();
        String holder = paramDefElement.getAttributeValue(HOLDER_ATTRIBUTE);

        if (PROCESSOR_ENTITY.equals(holder)) {
            holder = ORIGIN_ENTITY;
        }

        putEntity(
            ParamDefEntity
                .newBuilder()
                .setHolder(holder)
                .setHidden(
                    paramDefElement.getAttributeValue(HIDDEN_ATTRIBUTE, false))
                .setMultiple(
                    paramDefElement
                            .getAttributeValue(MULTIPLE_ATTRIBUTE, false))
                .setUUID(paramDefElement.getUUID().orElse(null))
                .setName(paramDefElement.getNameAttribute().orElse(null))
                .build());
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
