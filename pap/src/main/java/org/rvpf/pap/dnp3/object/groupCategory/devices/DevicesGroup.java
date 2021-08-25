/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DevicesGroup.java 3978 2019-05-12 10:53:03Z SFB $
 */

package org.rvpf.pap.dnp3.object.groupCategory.devices;

import java.util.Map;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;
import org.rvpf.pap.dnp3.object.GroupCategory;
import org.rvpf.pap.dnp3.object.ObjectGroup;
import org.rvpf.pap.dnp3.object.ObjectVariation;

/**
 * Devices group enumeration.
 */
public enum DevicesGroup
    implements ObjectGroup
{
    INTERNAL_INDICATIONS(80, "Internal Indications",
            InternalIndicationsVariation.class);

    /**
     * Constructs an instance.
     *
     * @param code The group code.
     * @param title The group title.
     * @param objectVariationClass The object variation class.
     */
    DevicesGroup(
            final int code,
            @Nonnull final String title,
            @Nonnull final Class<? extends ObjectVariation> objectVariationClass)
    {
        _code = code;
        _title = title;
        _objectVariationClass = objectVariationClass;
    }

    /**
     * Returns the instance for a group number.
     *
     * @param group The group number.
     *
     * @return The instance.
     */
    @Nonnull
    @CheckReturnValue
    public static DevicesGroup instance(final int group)
    {
        return Require.notNull(_GROUP_MAP.get(Integer.valueOf(group)));
    }

    /** {@inheritDoc}
     */
    @Override
    public GroupCategory getCategory()
    {
        return GroupCategory.DEVICES;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getCode()
    {
        return _code;
    }

    /** {@inheritDoc}
     */
    @Override
    public Class<? extends ObjectVariation> getObjectVariationClass()
    {
        return _objectVariationClass;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getTitle()
    {
        return _title;
    }

    private static final Map<Integer, DevicesGroup> _GROUP_MAP = MapFactory
        .codeMap(values());

    private final int _code;
    private final Class<? extends ObjectVariation> _objectVariationClass;
    private final String _title;
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
