/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: EntityExporter.java 3936 2019-04-28 19:25:00Z SFB $
 */

package org.rvpf.document.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.Entity;
import org.rvpf.base.util.container.IdentityHashSet;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.document.loader.DocumentElement;

/**
 * Entity exporter.
 */
public abstract class EntityExporter
    extends XMLExporter
{
    /**
     * Creates an instance.
     *
     * @param owner The exporter owning this.
     * @param entityClass The Class of the entities exported by this.
     */
    protected EntityExporter(
            @Nonnull final ConfigExporter owner,
            @Nonnull final Class<? extends Entity> entityClass)
    {
        super(Optional.of(owner));

        addExporter(entityClass);
    }

    /**
     * Exports its entities as elements of the root.
     */
    protected final void export()
    {
        final XMLElement rootElement = getRootElement();
        final List<Entity> entities = new ArrayList<Entity>(_entities);

        entities.sort(null);

        for (final Entity entity: entities) {
            final XMLElement element = createElement(getElementName());
            final EntityReference reference = getReference(entity);

            if (reference.getId() == 0) {
                reference.setId(nextId());
            }

            element
                .setAttribute(
                    DocumentElement.ID_ATTRIBUTE,
                    Optional.of(reference.toString()));

            export(entity, element);
            rootElement.addChild(element);
        }
    }

    /**
     * Exports an entity into the supplied element.
     *
     * @param entity The entity to export.
     * @param element The target element.
     */
    protected abstract void export(
            @Nonnull Entity entity,
            @Nonnull XMLElement element);

    /**
     * Registers the entity's references.
     *
     * @param entity The entity holding the references.
     */
    protected void registerReferences(@Nonnull final Entity entity) {}

    /**
     * Adds an entity.
     *
     * @param entity The entity.
     */
    final void addEntity(@Nonnull final Entity entity)
    {
        _entities.add(entity);
        registerReferences(entity);
    }

    private final Collection<Entity> _entities = new IdentityHashSet<>();
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
