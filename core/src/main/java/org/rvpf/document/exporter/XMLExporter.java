/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: XMLExporter.java 3949 2019-05-03 15:35:40Z SFB $
 */

package org.rvpf.document.exporter;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Entity;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.config.Config;

/**
 * XML exporter.
 */
public abstract class XMLExporter
{
    /**
     * Creates an instance.
     *
     * @param owner The optional exporter owning this.
     */
    protected XMLExporter(@Nonnull final Optional<ConfigExporter> owner)
    {
        _owner = owner.isPresent()? owner.get(): (ConfigExporter) this;
        _elementFactory = _document.getElementFactory();
    }

    /**
     * Asks if all values are deferred substitutions.
     *
     * @param values The values.
     *
     * @return True if all values are deferred substitutions.
     */
    @CheckReturnValue
    protected static boolean areDeferredSubstitutions(
            @Nonnull final Object[] values)
    {
        for (final Object value: values) {
            if (!(value instanceof String)) {
                return false;
            }

            final String string = value.toString().trim();

            if (!string.startsWith("$${")) {
                return false;
            }

            if (string.indexOf('}') != (string.length() - 1)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Adds this as the exporter for an Entity Class.
     *
     * @param entityClass The Entity Class.
     */
    protected final void addExporter(
            @Nonnull final Class<? extends Entity> entityClass)
    {
        _owner.getExporters().put(entityClass, this);
    }

    /**
     * Creates an Element with the specified name.
     *
     * @param name The Element name.
     *
     * @return The Element.
     */
    @Nonnull
    @CheckReturnValue
    protected final XMLElement createElement(@Nonnull final String name)
    {
        return _elementFactory.newXMLElement(name);
    }

    /**
     * Gets the config.
     *
     * @return The config.
     */
    @Nonnull
    @CheckReturnValue
    protected Config getConfig()
    {
        return _owner.getConfig();
    }

    /**
     * Gets the XML document.
     *
     * @return The XML document.
     */
    @Nonnull
    @CheckReturnValue
    protected XMLDocument getDocument()
    {
        return _document;
    }

    /**
     * Gets the XML element name.
     *
     * @return The name.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract String getElementName();

    /**
     * Gets an entity.
     *
     * @param name The name of the entity.
     * @param prefix The prefix associated with the class of the entity.
     *
     * @return The optional entity.
     */
    @Nonnull
    @CheckReturnValue
    protected final Optional<? extends Entity> getEntity(
            @Nonnull final String name,
            @Nonnull final String prefix)
    {
        final Optional<? extends Map<String, Entity>> entities = _owner
            .getEntities();
        final Optional<Entity> entity;

        if ((name != null) && entities.isPresent()) {
            entity = Optional
                .ofNullable(
                    entities.get().get(prefix + name.toUpperCase(Locale.ROOT)));
        } else {
            entity = Optional.empty();
        }

        return entity;
    }

    /**
     * Gets the owner.
     *
     * @return The owner.
     */
    @Nonnull
    @CheckReturnValue
    protected ConfigExporter getOwner()
    {
        return _owner;
    }

    /**
     * Gets the Reference to an Entity.
     *
     * @param entity The referenced Entity.
     *
     * @return The Reference.
     */
    @Nonnull
    @CheckReturnValue
    protected final EntityReference getReference(@Nonnull final Entity entity)
    {
        return _owner.getReferences().get(entity);
    }

    /**
     * Gets the root Element.
     *
     * @return The root Element.
     */
    @Nonnull
    @CheckReturnValue
    protected XMLElement getRootElement()
    {
        return _owner.getRootElement();
    }

    /**
     * Asks if attributes are required.
     *
     * @return True when attributes are required.
     */
    @CheckReturnValue
    protected final boolean isWithAttributes()
    {
        return _owner.getUsages().isPresent();
    }

    /**
     * Asks if attributes for the specified usage are required.
     *
     * @param usage The attributes usage.
     *
     * @return True when they are required.
     */
    @CheckReturnValue
    protected final boolean isWithAttributes(@Nonnull final String usage)
    {
        final Set<String> usages = _owner.getUsages().orElse(null);

        return (usages != null)
               && ((usages.isEmpty()) || usages.contains(usage));
    }

    /**
     * Asks if texts are required.
     *
     * @return True when texts are required.
     */
    @CheckReturnValue
    protected final boolean isWithTexts()
    {
        return _owner.getLangs().isPresent();
    }

    /**
     * Asks if texts in the specified language are required.
     *
     * @param lang The texts language.
     *
     * @return True when they are required.
     */
    @CheckReturnValue
    protected final boolean isWithTexts(final String lang)
    {
        final Set<String> langs = _owner.getLangs().orElse(null);

        return (langs != null) && ((langs.isEmpty()) || langs.contains(lang));
    }

    /**
     * Returns the next ID for attribution to an Entity.
     *
     * @return A new Entity ID.
     */
    @CheckReturnValue
    protected int nextId()
    {
        return _owner.nextId();
    }

    /**
     * Returns a reference string to an entity.
     *
     * @param entity The optional entity.
     *
     * @return The optional reference string.
     */
    @Nonnull
    @CheckReturnValue
    protected Optional<String> reference(
            @Nonnull final Optional<? extends Entity> entity)
    {
        if (!entity.isPresent()) {
            return Optional.empty();
        }

        final EntityReference reference = getReference(entity.orElse(null));

        if (reference.getId() == 0) {
            reference.setId(nextId());
        }

        return Optional.of(reference.toString());
    }

    /**
     * Registers an entity.
     *
     * @param entity The entity to register.
     */
    protected void registerReference(
            @Nonnull final Optional<? extends Entity> entity)
    {
        if (entity.isPresent()) {
            if (_owner
                .getReferences()
                .put(entity.get(), new EntityReference(entity.get())) == null) {
                final EntityExporter exporter = (EntityExporter) _owner
                    .getExporters()
                    .get(entity.get().getClass());

                exporter.addEntity(entity.get());
            }
        }
    }

    private final XMLDocument _document = new XMLDocument();
    private final XMLElement.Factory _elementFactory;
    private final ConfigExporter _owner;
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
