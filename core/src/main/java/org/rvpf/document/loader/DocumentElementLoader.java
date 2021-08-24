/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DocumentElementLoader.java 4073 2019-06-10 18:47:04Z SFB $
 */

package org.rvpf.document.loader;

import java.net.URL;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Entity;
import org.rvpf.base.exception.ValidationException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.config.Config;
import org.rvpf.service.ServiceMessages;

/**
 * Document element loader.
 */
abstract class DocumentElementLoader
{
    /**
     * Gets the config.
     *
     * @return The config.
     */
    @Nonnull
    @CheckReturnValue
    protected final Config getConfig()
    {
        return getDocumentLoader().getConfig();
    }

    /**
     * Gets the URL for the current context.
     *
     * @return The URL.
     */
    @Nonnull
    @CheckReturnValue
    protected final URL getContextURL()
    {
        return getDocumentLoader().getContextURL();
    }

    /**
     * Gets the document loader.
     *
     * @return The document loader.
     */
    @Nonnull
    @CheckReturnValue
    protected final DocumentLoader getDocumentLoader()
    {
        return Require.notNull(_documentLoader);
    }

    /**
     * Gets the XML element.
     *
     * @return The XML element.
     */
    @Nonnull
    @CheckReturnValue
    protected final DocumentElement getElement()
    {
        return Require.notNull(_element);
    }

    /**
     * Gets an entity registered to the owner.
     *
     * <p>Produces a warning if the entity is not present.</p>
     *
     * @param key The entity key.
     * @param prefix The prefix associated with the entity class.
     *
     * @return The entity.
     *
     * @throws ValidationException When appropriate.
     */
    @Nonnull
    @CheckReturnValue
    protected final Entity getEntity(
            @Nonnull final String key,
            @Nonnull final String prefix)
        throws ValidationException
    {
        return getEntity(key, prefix, false).get();
    }

    /**
     * Gets an entity registered to the owner.
     *
     * @param key The entity name.
     * @param prefix The prefix associated with the entity class.
     * @param optional True if the entity is optional.
     *
     * @return The entity or empty if the entity is unknown and not optional.
     *
     * @throws ValidationException When appropriate.
     */
    @Nonnull
    @CheckReturnValue
    protected final Optional<? extends Entity> getEntity(
            @Nonnull final String key,
            @Nonnull final String prefix,
            final boolean optional)
        throws ValidationException
    {
        final Optional<? extends Entity> entity = getDocumentLoader()
            .getEntity(key, prefix);

        if (!entity.isPresent()) {
            if (!optional) {
                throw new ValidationException(
                    ServiceMessages.ENTITY_UNKNOWN,
                    getDocumentLoader().getPrefixName(prefix),
                    key);
            }

            return Optional.empty();
        }

        return entity;
    }

    /**
     * Gets the information texts language.
     *
     * @return The optional information texts language.
     */
    @Nonnull
    @CheckReturnValue
    protected Optional<String> getLang()
    {
        return getDocumentLoader().getLang();
    }

    /**
     * Gets the logger.
     *
     * @return The logger.
     */
    @Nonnull
    @CheckReturnValue
    protected final Logger getLogger()
    {
        return getDocumentLoader().getThisLogger();
    }

    /**
     * Processes the entry XML informations.
     *
     * @throws ValidationException When appropriate.
     */
    protected abstract void process()
        throws ValidationException;

    /**
     * Registers an entity to the owner XML document.
     *
     * @param entity The entity to be registered.
     */
    protected final void putEntity(@Nonnull final Entity entity)
    {
        putEntity(entity, false);
    }

    /**
     * Registers an entity to the owner XML document.
     *
     * @param entity The entity to be registered.
     * @param supersede Allows supersede by name.
     */
    protected final void putEntity(
            @Nonnull final Entity entity,
            final boolean supersede)
    {
        final DocumentElement element = getElement();
        final Optional<String> elemendId = element.getId();

        if (elemendId.isPresent()) {
            getDocumentLoader().putEntity(elemendId.get(), entity, false);
        }

        final Optional<String> nameAttribute = element
            .getAttributeValue(
                DocumentElement.NAME_ATTRIBUTE,
                Optional.empty());

        if (nameAttribute.isPresent()) {
            getDocumentLoader()
                .putEntity(nameAttribute.get(), entity, supersede);
        }

        final Optional<String> uuidAttribute = element
            .getAttributeValue(
                DocumentElement.UUID_ATTRIBUTE,
                Optional.empty());

        if (uuidAttribute.isPresent()) {
            getDocumentLoader().putEntity(uuidAttribute.get(), entity, false);
        }
    }

    /**
     * Registers an entity to the owner XML document.
     *
     * @param key The key for the registration.
     * @param entity The entity to be registered.
     */
    protected final void putEntity(
            @Nonnull final String key,
            @Nonnull final Entity entity)
    {
        getDocumentLoader().putEntity(key, entity, false);
    }

    /**
     * Removes an entity.
     *
     * @param key The entity definition key.
     * @param prefix The key prefix for the entity.
     */
    protected final void removeEntity(
            @Nonnull final String key,
            @Nonnull final String prefix)
    {
        getDocumentLoader().removeEntity(key, prefix);
    }

    /**
     * Updates information texts.
     */
    protected void updateTexts() {}

    /**
     * Processes an element.
     *
     * @param element The element.
     *
     * @throws ValidationException When appropriate.
     */
    final void process(
            @Nonnull final DocumentElement element)
        throws ValidationException
    {
        _setElement(element);

        if (element.isEnabled()) {
            process();
        }
    }

    /**
     * Sets the owner of this element.
     *
     * @param documentLoader The owner.
     */
    final void setDocument(@Nonnull final DocumentLoader documentLoader)
    {
        _documentLoader = documentLoader;
    }

    /**
     * Updates texts from an element.
     *
     * @param element The element.
     */
    final void updateTexts(@Nonnull final DocumentElement element)
    {
        _setElement(element);

        if (element.isEnabled()) {
            updateTexts();
        }
    }

    /**
     * Sets the wrapped element reference.
     *
     * @param element The element to wrap.
     */
    private void _setElement(@Nonnull final DocumentElement element)
    {
        _element = element;
    }

    private DocumentLoader _documentLoader;
    private DocumentElement _element;
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
