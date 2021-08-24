/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DocumentElement.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.document.loader;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.UUID;
import org.rvpf.base.exception.ValidationException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.ValueConverter;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.service.ServiceMessages;

/**
 * Element.
 */
public final class DocumentElement
    extends XMLElement
{
    /**
     * Constructs an instance.
     *
     * @param name The element name.
     * @param document The document.
     */
    DocumentElement(
            @Nonnull final String name,
            @Nonnull final DocumentLoader document)
    {
        super(name);

        _document = document;
    }

    /** {@inheritDoc}
     */
    @Override
    public XMLElement copy()
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        return super.equals(other);
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<String> getAttributeValue(
            final String name,
            final Optional<String> defaultValue)
    {
        final Optional<String> attributeValue = super
            .getAttributeValue(name, defaultValue);

        return attributeValue
            .isPresent()? Optional
                .of(
                    _document
                            .substitute(
                                    attributeValue.get())): Optional.empty();
    }

    /** {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<DocumentElement> getChildren()
    {
        return (List<DocumentElement>) super.getChildren();
    }

    /** {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<DocumentElement> getChildren(final String name)
    {
        return (List<DocumentElement>) super.getChildren(name);
    }

    /** {@inheritDoc}
     */
    @Override
    public Factory getFactory()
    {
        return _document;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getText()
    {
        return _document.substitute(super.getText());
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    /**
     * Gets an attribute by name or fails.
     *
     * @param name The name of the attribute.
     *
     * @return The value of the attribute.
     *
     * @throws ValidationException When appropriate.
     */
    @Nonnull
    @CheckReturnValue
    String getAttributeValue(
            @Nonnull final String name)
        throws ValidationException
    {
        final Optional<String> attributeValue = getAttributeValue(
            name,
            Optional.empty());

        if (!attributeValue.isPresent()) {
            throw new ValidationException(
                ServiceMessages.MISSING_ATTRIBUTE_IN,
                name,
                getName());
        }

        return attributeValue.get();
    }

    /**
     * Gets a boolean attribute value by its name, providing a default.
     *
     * <p>If the attribute is not found or its value is unrecognized the default
     * will be returned.</p>
     *
     * @param name Name of the attribute.
     * @param defaultValue The value to return if the attribute is not present.
     *
     * @return The attribute boolean value or the provided default.
     */
    @CheckReturnValue
    boolean getAttributeValue(
            @Nonnull final String name,
            final boolean defaultValue)
    {
        final Optional<String> value = getAttributeValue(
            name,
            Optional.empty());

        return ValueConverter
            .convertToBoolean(
                ServiceMessages.ATTRIBUTE_TYPE.toString(),
                name,
                value,
                defaultValue);
    }

    /**
     * Gets the document's logger.
     *
     * @return The document's logger.
     */
    @Nonnull
    @CheckReturnValue
    Logger getDocumentLogger()
    {
        return _document.getThisLogger();
    }

    /**
     * Gets the value of the 'id' attribute.
     *
     * @return The value of the optional 'id' attribute.
     */
    @Nonnull
    @CheckReturnValue
    Optional<String> getId()
    {
        return getAttributeValue(ID_ATTRIBUTE, Optional.empty());
    }

    /**
     * Gets the value of the 'name' attribute.
     *
     * @return The value of the optional 'name' attribute.
     */
    @Nonnull
    @CheckReturnValue
    Optional<String> getNameAttribute()
    {
        Optional<String> name = getAttributeValue(
            NAME_ATTRIBUTE,
            Optional.empty());

        if (name.isPresent()) {
            if (ILLEGAL_PATTERN.matcher(name.get()).find()) {
                getDocumentLogger().warn(ServiceMessages.NAME_CHARACTERS, name);
                name = Optional.empty();
            } else if (DocumentLoader.ID_PATTERN
                .matcher(name.get())
                .matches()) {
                getDocumentLogger().warn(ServiceMessages.NAME_LIKE_ID, name);
                name = Optional.empty();
            } else if (UUID.isUUID(name.get())) {
                getDocumentLogger()
                    .warn(ServiceMessages.NAME_LIKE_UUID, name.get());
            }

            if (!name.isPresent()) {
                removeAttribute(NAME_ATTRIBUTE);
            }
        }

        return name;
    }

    /**
     * Gets a UUID from the value of the 'uuid' attribute.
     *
     * @return The optional UUID.
     */
    @Nonnull
    @CheckReturnValue
    Optional<UUID> getUUID()
    {
        final String uuidString = getAttributeValue(
            UUID_ATTRIBUTE,
            Optional.empty())
            .orElse(null);

        if (uuidString == null) {
            return Optional.empty();
        }

        if (UUID.isUUID(uuidString)) {
            return UUID.fromString(uuidString);
        }

        try {
            return Optional.of(UUID.fromName(uuidString));
        } catch (final IllegalArgumentException exception) {
            getDocumentLogger()
                .warn(ServiceMessages.INVALID_UUID_ATTRIBUTE, uuidString);
            removeAttribute(UUID_ATTRIBUTE);

            return Optional.empty();
        }
    }

    /**
     * Asks if this element is enabled.
     *
     * @return True if enabled.
     */
    @CheckReturnValue
    boolean isEnabled()
    {
        boolean enabled = true;
        Optional<String> condition;

        condition = getAttributeValue(IF_ATTRIBUTE, Optional.empty());

        if ((condition.isPresent())
                && !_document.getConfig().getStringValue(
                    condition.get()).isPresent()) {
            enabled = false;
        }

        condition = getAttributeValue(UNLESS_ATTRIBUTE, Optional.empty());

        if ((condition.isPresent())
                && _document.getConfig().getStringValue(
                    condition.get()).isPresent()) {
            enabled = false;
        }

        condition = getAttributeValue(IF_TRUE_ATTRIBUTE, Optional.empty());

        if ((condition.isPresent())
                && !_document.getConfig().getBooleanValue(condition.get())) {
            enabled = false;
        }

        condition = getAttributeValue(IF_FALSE_ATTRIBUTE, Optional.empty());

        if ((condition.isPresent())
                && _document.getConfig().getBooleanValue(condition.get())) {
            enabled = false;
        }

        return enabled;
    }

    /** ID attribute */
    public static final String ID_ATTRIBUTE = "id";

    /** If attribute. */
    public static final String IF_ATTRIBUTE = "if";

    /** If attribute. */
    public static final String IF_FALSE_ATTRIBUTE = "ifFalse";

    /** If attribute. */
    public static final String IF_TRUE_ATTRIBUTE = "ifTrue";

    /** Illegal characters pattern. */
    public static final Pattern ILLEGAL_PATTERN = Pattern
        .compile("\\x00-\\x20\\x7f");

    /** Lang attribute. */
    public static final String LANG_ATTRIBUTE = "lang";

    /** Name attribute. */
    public static final String NAME_ATTRIBUTE = "name";

    /** Self reference. */
    public static final String SELF_REFERENCE = ".";

    /** Unless attribute. */
    public static final String UNLESS_ATTRIBUTE = "unless";

    /** Usage attribute. */
    public static final String USAGE_ATTRIBUTE = "usage";

    /** UUID attribute. */
    public static final String UUID_ATTRIBUTE = "uuid";

    private final DocumentLoader _document;
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
