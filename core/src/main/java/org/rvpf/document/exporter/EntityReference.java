/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: EntityReference.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.document.exporter;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import org.rvpf.base.Entity;
import org.rvpf.document.loader.DocumentLoader;

/** Entity reference.
 */
public final class EntityReference
{
    /** Creates an instance.
     *
     * @param entity The referenced Entity.
     */
    public EntityReference(@Nonnull final Entity entity)
    {
        _entity = entity;
    }

    /** Gets the referenced entity.
     *
     * @return The referenced entity.
     */
    @Nonnull
    @CheckReturnValue
    public Entity getEntity()
    {
        return _entity;
    }

    /** Gets the element ID.
     *
     * @return The element ID.
     */
    @CheckReturnValue
    public int getId()
    {
        return _id;
    }

    /** Sets the element ID.
     *
     * @param id The element ID.
     */
    public void setId(final int id)
    {
        _id = id;
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return DocumentLoader.ID_PREFIX
            + Integer.toString(_id, Character.MAX_RADIX);
    }

    private final Entity _entity;
    private int _id;
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
