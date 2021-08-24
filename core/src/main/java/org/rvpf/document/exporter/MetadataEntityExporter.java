/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MetadataEntityExporter.java 4078 2019-06-11 20:55:00Z SFB $
 */

package org.rvpf.document.exporter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Attributes;
import org.rvpf.base.Entity;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.document.loader.DocumentElement;
import org.rvpf.document.loader.MetadataElementLoader;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.Text;
import org.rvpf.metadata.entity.MetadataEntity;

/**
 * Metadata entity exporter.
 */
public abstract class MetadataEntityExporter
    extends DefEntityExporter
{
    /**
     * Constructs an instance.
     *
     * @param owner The exporter owning this.
     * @param entityClass The class of the entities exported by this.
     */
    protected MetadataEntityExporter(
            final ConfigExporter owner,
            final Class<? extends Entity> entityClass)
    {
        super(owner, entityClass);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void export(final Entity entity, final XMLElement element)
    {
        super.export(entity, element);

        final MetadataEntity metadataEntity = (MetadataEntity) entity;

        if (isWithAttributes()) {
            final Optional<KeyedGroups> entityAttributes = metadataEntity
                .getAttributes();

            if (entityAttributes.isPresent()) {
                for (final String usage:
                        entityAttributes.get().getValuesKeys()) {
                    final Attributes attributes = (Attributes) entityAttributes
                        .get()
                        .getObject(usage);

                    if (isWithAttributes(usage)) {
                        final XMLElement attributesElement = createElement(
                            MetadataElementLoader.ATTRIBUTES_ELEMENT);

                        element.addChild(attributesElement);

                        if (usage.length() > 0) {
                            attributesElement
                                .setAttribute(
                                    DocumentElement.USAGE_ATTRIBUTE,
                                    Optional.of(usage));
                        }

                        if (attributes.isHidden()) {
                            attributesElement
                                .setAttribute(
                                    MetadataElementLoader.HIDDEN_ATTRIBUTE,
                                    true);
                        }

                        for (Map.Entry<String, List<Object>> attributeEntry:
                                attributes.getValuesEntries()) {
                            final String attributeName = attributeEntry
                                .getKey();
                            final List<Object> attributeValues = attributeEntry
                                .getValue();
                            final XMLElement attributeElement = createElement(
                                MetadataElementLoader.ATTRIBUTE_ELEMENT);

                            attributeElement
                                .setAttribute(
                                    DocumentElement.NAME_ATTRIBUTE,
                                    Optional.of(attributeName));

                            if (attributeValues.size() > 1) {
                                for (final Object attributeValue:
                                        attributeValues) {
                                    final XMLElement valueElement =
                                        createElement(
                                            MetadataElementLoader.VALUE_ELEMENT);

                                    valueElement
                                        .addText((String) attributeValue);
                                    attributeElement.addChild(valueElement);
                                }
                            } else {
                                attributeElement
                                    .addText((String) attributeValues.get(0));
                            }

                            if (attributes.areValuesHidden(attributeName)) {
                                attributeElement
                                    .setAttribute(
                                        MetadataElementLoader.HIDDEN_ATTRIBUTE,
                                        true);
                            }

                            attributesElement.addChild(attributeElement);
                        }
                    }
                }
            }
        }

        if (isWithTexts()) {
            for (final Map.Entry<String, Text> entry:
                    metadataEntity
                        .getTexts()
                        .orElse(Collections.emptyMap())
                        .entrySet()) {
                final String lang = entry.getKey();

                if (isWithTexts(lang)) {
                    final XMLElement textElement = createElement(
                        MetadataElementLoader.TEXT_ELEMENT);

                    element.addChild(textElement);

                    if (lang.length() > 0) {
                        textElement
                            .setAttribute(
                                DocumentElement.LANG_ATTRIBUTE,
                                Optional.of(lang));
                    }

                    final Text text = entry.getValue();

                    textElement
                        .setAttribute(
                            MetadataElementLoader.IDENT_ATTRIBUTE,
                            text.getIdent());

                    if (text.getTitle().isPresent()) {
                        final XMLElement titleElement = createElement(
                            MetadataElementLoader.TITLE_ATTRIBUTE);

                        textElement.addChild(titleElement);

                        titleElement.addText(text.getTitle().get());
                    }

                    if (text.getDescription().isPresent()) {
                        final XMLElement descriptionElement = createElement(
                            MetadataElementLoader.DESCRIPTION_ATTRIBUTE);

                        textElement.addChild(descriptionElement);

                        descriptionElement.addText(text.getDescription().get());
                    }

                    for (final String notes: text.getNotes()) {
                        final XMLElement notesElement = createElement(
                            MetadataElementLoader.NOTES_ELEMENT);

                        textElement.addChild(notesElement);

                        notesElement.addText(notes);
                    }

                    for (final Text.Other other: text.getOthers()) {
                        final XMLElement otherElement = createElement(
                            MetadataElementLoader.OTHER_ELEMENT);

                        textElement.addChild(otherElement);

                        otherElement
                            .setAttribute(
                                MetadataElementLoader.ELEMENT_ATTRIBUTE,
                                Optional.ofNullable(other.getElement()));
                        otherElement.addText(other.getText());
                    }
                }
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected String getElementName()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the metadata.
     *
     * @return The metadata.
     */
    @Nonnull
    @CheckReturnValue
    protected final Metadata getMetadata()
    {
        return (Metadata) getConfig();
    }

    /**
     * Sets the anchored attribute if the entity is anchored.
     *
     * @param entity The entity.
     * @param element The element.
     */
    protected void setAnchored(
            @Nonnull final Entity entity,
            @Nonnull final XMLElement element)
    {
        if (getMetadata().isAnchored(entity)) {
            element
                .setAttribute(MetadataElementLoader.ANCHORED_ATTRIBUTE, true);
        }
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
