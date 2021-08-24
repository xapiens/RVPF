/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PropertyDefLoader.java 3942 2019-04-30 19:47:54Z SFB $
 */

package org.rvpf.document.loader;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.config.entity.PropertyDefEntity;

/**
 * PropertyDef loader.
 */
final class PropertyDefLoader
    extends ConfigElementLoader
{
    /** {@inheritDoc}
     */
    @Override
    protected void process()
    {
        final DocumentElement propertyDefElement = getElement();
        final PropertyDefEntity propertyDef = preparePropertyDef(
            propertyDefElement);

        propertyDef.setUUID(propertyDefElement.getUUID());
        putEntity(propertyDef);
    }

    /**
     * Prepares a PropertyDef.
     *
     * @param element The defining element.
     *
     * @return The PropertyDef.
     */
    @Nonnull
    @CheckReturnValue
    static PropertyDefEntity preparePropertyDef(
            @Nonnull final DocumentElement element)
    {
        final PropertyDefEntity.Builder propertyDefBuilder = PropertyDefEntity
            .newBuilder();

        propertyDefBuilder.setName(element.getNameAttribute().get());

        propertyDefBuilder
            .setHidden(element.getAttributeValue(HIDDEN_ATTRIBUTE, false))
            .setMultiple(element.getAttributeValue(MULTIPLE_ATTRIBUTE, false));

        return propertyDefBuilder.build();
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
