/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.document.exporter;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.config.entity.ClassDefEntity;
import org.rvpf.document.loader.ConfigElementLoader;
import org.rvpf.document.loader.DocumentElement;
import org.rvpf.document.loader.MetadataElementLoader;

/**
 * Attributes exporter.
 */
final class AttributesExporter
    extends XMLExporter
{
    /**
     * Creates an instance.
     *
     * @param owner The exporter owning this.
     */
    AttributesExporter(@Nonnull final MetadataExporter owner)
    {
        super(Optional.of(owner));
    }

    /** {@inheritDoc}
     */
    @Override
    protected String getElementName()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Exports attributes into a parent element.
     *
     * @param attributes The attributes.
     * @param parent The parent element.
     */
    void export(
            @Nonnull final KeyedGroups attributes,
            @Nonnull final XMLElement parent)
    {
        final boolean secure = getOwner().isSecure();

        for (final String usage: attributes.getGroupsKeys()) {
            final KeyedGroups group = attributes.getGroup(usage);

            if (secure || !group.isHidden()) {
                _exportUsage(usage, group, parent);
            }
        }
    }

    private void _exportAttributes(
            final KeyedGroups attributes,
            final XMLElement parent)
    {
        final boolean secure = getOwner().isSecure();

        for (final String propertyName: attributes.getValuesKeys()) {
            final boolean hidden = attributes.areValuesHidden(propertyName);
            final Object[] propertyValues = attributes.getValues(propertyName);

            if (hidden
                    && !secure
                    && !areDeferredSubstitutions(propertyValues)) {
                continue;
            }

            final XMLElement attributeElement = createElement(
                MetadataElementLoader.ATTRIBUTE_ELEMENT);

            attributeElement
                .setAttribute(
                    DocumentElement.NAME_ATTRIBUTE,
                    Optional.of(propertyName));
            attributeElement
                .setAttribute(ConfigElementLoader.VALIDATED_ATTRIBUTE, false);

            if (hidden) {
                attributeElement
                    .setAttribute(MetadataElementLoader.HIDDEN_ATTRIBUTE, true);
            }

            if (propertyValues.length > 1) {
                for (final Object propertyValue: propertyValues) {
                    final XMLElement valueElement = createElement(
                        ConfigElementLoader.VALUE_ELEMENT);

                    valueElement
                        .setAttribute(
                            ConfigElementLoader.VALUE_ATTRIBUTE,
                            Optional.of((String) propertyValue));
                    attributeElement.addChild(valueElement);
                }
            } else {
                final Object propertyValue = propertyValues[0];

                if (propertyValue instanceof ClassDefEntity) {
                    attributeElement
                        .setAttribute(
                            ConfigElementLoader.CLASS_DEF_REFERENCE,
                            reference(
                                Optional.of((ClassDefEntity) propertyValue)));
                } else {
                    attributeElement
                        .setAttribute(
                            ConfigElementLoader.VALUE_ATTRIBUTE,
                            Optional.of((String) propertyValue));
                }
            }

            parent.addChild(attributeElement);
        }
    }

    private void _exportUsage(
            final String usage,
            final KeyedGroups group,
            final XMLElement parent)
    {
        final XMLElement attributesElement = createElement(
            MetadataElementLoader.ATTRIBUTES_ELEMENT);

        attributesElement
            .setAttribute(
                MetadataElementLoader.USAGE_ATTRIBUTE,
                Optional.of(usage));
        attributesElement
            .setAttribute(ConfigElementLoader.VALIDATED_ATTRIBUTE, false);

        if (group.isHidden()) {
            attributesElement
                .setAttribute(ConfigElementLoader.HIDDEN_ATTRIBUTE, true);
        }

        _exportAttributes(group, parent);
        parent.addChild(attributesElement);
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
