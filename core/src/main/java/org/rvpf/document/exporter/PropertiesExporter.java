/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PropertiesExporter.java 4103 2019-07-01 13:31:25Z SFB $
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
 * Properties exporter.
 */
final class PropertiesExporter
    extends XMLExporter
{
    /**
     * Creates an instance.
     *
     * @param owner The exporter owning this.
     */
    PropertiesExporter(@Nonnull final ConfigExporter owner)
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
     * Exports properties into a parent element.
     *
     * @param properties The properties.
     * @param parent The parent element.
     */
    void export(
            @Nonnull final KeyedGroups properties,
            @Nonnull final XMLElement parent)
    {
        if (properties.isEmpty()
                || (properties.isHidden() && !getOwner().isSecure())) {
            return;
        }

        _exportProperties(properties, parent);
        _exportGroups(properties, parent);
    }

    /**
     * Exports the service into a parent Element.
     *
     * @param parent The parent Element.
     */
    void exportService(@Nonnull final XMLElement parent)
    {
        final String serviceName = getConfig().getServiceName();
        final KeyedGroups properties = getConfig()
            .getServiceContext(serviceName)
            .orElse(null);

        if (properties != null) {
            final XMLElement serviceElement = createElement(
                ConfigElementLoader.SERVICE_ELEMENT);

            serviceElement
                .setAttribute(
                    DocumentElement.NAME_ATTRIBUTE,
                    Optional.of(serviceName));
            export(properties, serviceElement);

            parent.addChild(serviceElement);
        }
    }

    private void _exportGroup(
            final String name,
            final KeyedGroups group,
            final XMLElement parent)
    {
        final XMLElement propertiesElement = createElement(
            ConfigElementLoader.PROPERTIES_ELEMENT);

        propertiesElement
            .setAttribute(DocumentElement.NAME_ATTRIBUTE, Optional.of(name));
        propertiesElement
            .setAttribute(ConfigElementLoader.VALIDATED_ATTRIBUTE, false);

        if (group.isHidden()) {
            propertiesElement
                .setAttribute(ConfigElementLoader.HIDDEN_ATTRIBUTE, true);
        }

        export(group, propertiesElement);
        parent.addChild(propertiesElement);
    }

    private void _exportGroups(
            final KeyedGroups properties,
            final XMLElement parent)
    {
        final boolean secure = getOwner().isSecure();

        for (final String groupName: properties.getGroupsKeys()) {
            for (final KeyedGroups group: properties.getGroups(groupName)) {
                if (secure || !group.isHidden()) {
                    _exportGroup(groupName, group, parent);
                }
            }
        }
    }

    private void _exportProperties(
            final KeyedGroups properties,
            final XMLElement parent)
    {
        final boolean secure = getOwner().isSecure();

        for (final String propertyName: properties.getValuesKeys()) {
            final boolean hidden = properties.areValuesHidden(propertyName);
            final Object[] propertyValues = properties.getValues(propertyName);

            if (hidden
                    && !secure
                    && !areDeferredSubstitutions(propertyValues)) {
                continue;
            }

            final XMLElement propertyElement = createElement(
                ConfigElementLoader.PROPERTY_ELEMENT);

            propertyElement
                .setAttribute(
                    DocumentElement.NAME_ATTRIBUTE,
                    Optional.of(propertyName));
            propertyElement
                .setAttribute(ConfigElementLoader.VALIDATED_ATTRIBUTE, false);

            if (hidden) {
                propertyElement
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
                    propertyElement.addChild(valueElement);
                }
            } else {
                final Object propertyValue = propertyValues[0];

                if (propertyValue instanceof ClassDefEntity) {
                    propertyElement
                        .setAttribute(
                            ConfigElementLoader.CLASS_DEF_REFERENCE,
                            reference(
                                Optional.of((ClassDefEntity) propertyValue)));
                } else {
                    propertyElement
                        .setAttribute(
                            ConfigElementLoader.VALUE_ATTRIBUTE,
                            Optional.of((String) propertyValue));
                }
            }

            parent.addChild(propertyElement);
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
