/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Entity.java 3900 2019-02-19 20:43:24Z SFB $
 */

package org.rvpf.base;

import java.util.Comparator;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * Entity.
 *
 * <p>An entity holds informations on a defining element in metadata.</p>
 */
public interface Entity
    extends Comparable<Entity>
{
    /**
     * Creates a copy of this entity.
     *
     * @return The copy.
     */
    @Nonnull
    @CheckReturnValue
    Entity copy();

    /**
     * Gets the attributes for an usage.
     *
     * @param usage The usage.
     *
     * @return The optional attributes.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Attributes> getAttributes(@Nonnull String usage);

    /**
     * Gets the entity element name.
     *
     * <p>The entity element is used to in the generation of the entity URI.</p>
     *
     * @return The entity element name.
     */
    @Nonnull
    @CheckReturnValue
    String getElementName();

    /**
     * Gets the name of this.
     *
     * @return The optional name.
     */
    @Nonnull
    @CheckReturnValue
    Optional<String> getName();

    /**
     * Gets the name of this in upper case.
     *
     * @return The optional name in upper case.
     */
    @Nonnull
    @CheckReturnValue
    Optional<String> getNameInUpperCase();

    /**
     * Gets the class prefix.
     *
     * <p>Each concrete entity subclass has its own prefix which will be the
     * same for all its instances. This prefix is used to avoid name collision
     * between different classes in a common registry.</p>
     *
     * @return The class prefix.
     */
    @Nonnull
    @CheckReturnValue
    String getPrefix();

    /**
     * Gets the entity reference name.
     *
     * @return The entity reference name.
     */
    @Nonnull
    @CheckReturnValue
    String getReferenceName();

    /**
     * Gets the UUID of this Entity.
     *
     * @return The optional UUID.
     */
    @Nonnull
    @CheckReturnValue
    Optional<UUID> getUUID();

    /** UUID comparator. */
    Comparator<Entity> UUID_COMPARATOR = Comparator
        .comparing(entity -> entity.getUUID().orElse(null));
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
