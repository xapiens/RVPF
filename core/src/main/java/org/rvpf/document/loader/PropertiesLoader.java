/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PropertiesLoader.java 4103 2019-07-01 13:31:25Z SFB $
 */

package org.rvpf.document.loader;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;

import java.net.URL;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.exception.ValidationException;
import org.rvpf.base.logger.Message;
import org.rvpf.base.security.Crypt;
import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.config.ConfigProperties;
import org.rvpf.config.entity.PropertiesDefEntity;
import org.rvpf.service.ServiceMessages;

/**
 * Properties loader.
 */
final class PropertiesLoader
    extends ConfigElementLoader
{
    /** {@inheritDoc}
     */
    @Override
    protected void process()
        throws ValidationException
    {
        final DocumentElement propertiesElement = getElement();
        final boolean system = propertiesElement
            .getAttributeValue(SYSTEM_ATTRIBUTE, false);
        final Optional<String> name = propertiesElement.getNameAttribute();        
        if (system && name.isPresent()) {
            throw new ValidationException(
                ServiceMessages.CONFLICTING_ATTRIBUTES,
                SYSTEM_ATTRIBUTE,
                DocumentElement.NAME_ATTRIBUTE);
        }

        final boolean validate =
            ((ConfigDocumentLoader) getDocumentLoader()).isValidationEnabled()
            && propertiesElement.getAttributeValue(
                VALIDATED_ATTRIBUTE,
                true);
        final String def = propertiesElement
            .getAttributeValue(DEF_ATTRIBUTE, Optional.empty())
            .orElse(null);
        final PropertiesDefEntity propertiesDef;

        if (validate && name.isPresent()) {
            propertiesDef = (PropertiesDefEntity) getEntity(
                (def != null)? def: name.get(),
                PropertiesDefEntity.ENTITY_PREFIX,
                true)
                .orElse(null);

            if (propertiesDef == null) {
                getLogger()
                    .warn(ServiceMessages.PROPERTIES_DEF_MISSING, name.get());
            }
        } else {
            propertiesDef = null;
        }

        final ConfigProperties group = new ConfigProperties(name);
        final ConfigProperties configProperties = getConfig().getProperties();

        prepareGroup(
            group,
            this,
            propertiesElement,
            configProperties,
            Optional.ofNullable(propertiesDef),
            validate && !system);

        final String from = propertiesElement
            .getAttributeValue(FROM_ATTRIBUTE, Optional.empty())
            .orElse(null);

        if (from != null) {
            _fetchFromFile(propertiesElement, from, group);
        }

        if (system) {
            _setSystemProperties(group);
        } else {
            _addGroupProperties(configProperties, name.orElse(null), group);

            if (name.isPresent()) {
                if ((propertiesDef != null)
                        && (configProperties.getGroups(
                            name.get()).length > 1)) {
                    if (!propertiesDef.isMultiple()) {
                        getLogger()
                            .warn(
                                ServiceMessages.PROPERTIES_MULTIPLE,
                                name.get());
                    }
                }
            }
        }
    }

    /**
     * Prepares the group of properties specified in the element.
     *
     * @param group The group of properties.
     * @param loader The current Entry.
     * @param propertiesElement The XML element specifying the properties.
     * @param context The current context.
     * @param groupDef The optional group definition.
     * @param validate True if the group should be validated.
     *
     * @throws ValidationException When appropriate.
     */
    static void prepareGroup(
            @Nonnull final ConfigProperties group,
            @Nonnull final ConfigElementLoader loader,
            @Nonnull final DocumentElement propertiesElement,
            @Nonnull final ConfigProperties context,
            @Nonnull final Optional<PropertiesDefEntity> groupDef,
            boolean validate)
        throws ValidationException
    {
        final boolean hidden = groupDef
            .isPresent()? groupDef.get().isHidden(): false;

        group
            .setHidden(
                hidden
                || propertiesElement.getAttributeValue(
                    HIDDEN_ATTRIBUTE,
                    false));

        validate &= !groupDef.isPresent() || groupDef.get().isValidated();

        final boolean overrides = propertiesElement
            .getAttributeValue(OVERRIDES_ATTRIBUTE, false);

        if (overrides) {
            if (!group.getName().isPresent()) {
                throw new ValidationException(
                    ServiceMessages.MISSING_ATTRIBUTE_IN,
                    DocumentElement.NAME_ATTRIBUTE,
                    propertiesElement.getName());
            }

            context.removeGroup(group.getName().get());
        }

        final String extended = propertiesElement
            .getAttributeValue(EXTENDS_ATTRIBUTE, Optional.empty())
            .orElse(null);

        if (extended != null) {
            if (!group.getName().isPresent()) {
                throw new ValidationException(
                    ServiceMessages.MISSING_ATTRIBUTE_IN,
                    DocumentElement.NAME_ATTRIBUTE,
                    propertiesElement.getName());
            }

            final KeyedGroups extendedProperties = context.getGroup(extended);

            if (extendedProperties.isMissing()) {
                throw new ValidationException(
                    ServiceMessages.PROPERTIES_NOT_FOUND,
                    extended);
            }

            _addExtendedProperties(extendedProperties, group);
        }

        group.setOverriden(context);

        for (final DocumentElement childElement:
                propertiesElement.getChildren()) {
            if (childElement.isEnabled()) {
                if (PROPERTY_ELEMENT.equals(childElement.getName())) {
                    PropertyLoader
                        .addPropertyValues(
                            loader,
                            childElement,
                            group,
                            groupDef,
                            validate);
                } else if (PROPERTIES_ELEMENT.equals(childElement.getName())) {
                    _addChildGroup(
                        group,
                        loader,
                        childElement,
                        groupDef.orElse(null),
                        validate);
                }
            }
        }

        group.clearOverriden();
    }

    private static void _addChildGroup(
            final ConfigProperties group,
            final ConfigElementLoader loader,
            final DocumentElement childElement,
            final PropertiesDefEntity groupDef,
            boolean validate)
        throws ValidationException
    {
        final Optional<String> name = childElement.getNameAttribute();
        final PropertiesDefEntity propertiesDef;

        if (!name.isPresent()) {
            propertiesDef = groupDef;
        } else {
            validate &= childElement
                .getAttributeValue(VALIDATED_ATTRIBUTE, true);

            if (validate) {
                final String def = childElement
                    .getAttributeValue(DEF_ATTRIBUTE, name)
                    .orElse(null);

                if (groupDef == null) {
                    propertiesDef = (PropertiesDefEntity) loader
                        .getDocumentLoader()
                        .getEntity(def, PropertiesDefEntity.ENTITY_PREFIX)
                        .orElse(null);
                } else {
                    propertiesDef = groupDef.getPropertiesDef(def).orElse(null);
                }

                if (propertiesDef == null) {
                    if (groupDef != null) {
                        childElement
                            .getDocumentLogger()
                            .warn(
                                ServiceMessages.PROPERTIES_DEF_MISSING_IN,
                                name.get(),
                                groupDef.getName().orElse(null));
                    } else {
                        childElement
                            .getDocumentLogger()
                            .warn(
                                ServiceMessages.PROPERTIES_DEF_MISSING,
                                name.get());
                    }
                }
            } else {
                propertiesDef = null;
            }
        }

        final ConfigProperties childGroup = new ConfigProperties(name);

        prepareGroup(
            childGroup,
            loader,
            childElement,
            group,
            Optional.ofNullable(propertiesDef),
            validate
            && childElement.getAttributeValue(VALIDATED_ATTRIBUTE, true));
        _addGroupProperties(group, name.orElse(null), childGroup);

        if ((propertiesDef != null)
                && name.isPresent()
                && (group.getGroups(name.get()).length > 1)) {
            if (!propertiesDef.isMultiple()) {
                if (groupDef != null) {
                    childElement
                        .getDocumentLogger()
                        .warn(
                            ServiceMessages.PROPERTIES_MULTIPLE_IN,
                            name.get(),
                            groupDef.getName().get());
                } else {
                    childElement
                        .getDocumentLogger()
                        .warn(ServiceMessages.PROPERTIES_MULTIPLE, name.get());
                }
            }
        }
    }

    private static void _addExtendedProperties(
            final KeyedGroups extendedProperties,
            final KeyedGroups properties)
    {
        for (final Map.Entry<String, List<Object>> entry:
                extendedProperties.getValuesEntries()) {
            for (final Object value: entry.getValue()) {
                properties.add(entry.getKey(), value);
            }
        }

        for (final Map.Entry<String, List<Object>> entry:
                extendedProperties.getGroupsEntries()) {
            final String name = entry.getKey();
            final List<Object> groups = entry.getValue();

            for (final Object object: groups) {
                final ConfigProperties childProperties =
                    (ConfigProperties) object;
                final ConfigProperties groupProperties = new ConfigProperties(
                    childProperties.getName());

                _addGroupProperties(properties, name, groupProperties);
                _addExtendedProperties(childProperties, groupProperties);
            }
        }
    }

    private static void _addGroupProperties(
            final KeyedGroups group,
            final String key,
            final KeyedGroups properties)
    {
        if (key != null) {
            group.addGroup(key, properties);
        } else {
            group.addAll(properties);
        }
    }

    private static String[] getKeyIdents(
            final DocumentElement propertiesElement,
            final String elementName)
    {
        final List<DocumentElement> children = propertiesElement
            .getChildren(elementName);
        final String[] keyIdents = new String[children.size()];
        int childIndex = 0;

        for (final DocumentElement child: children) {
            keyIdents[childIndex++] = child.getText();
        }

        return keyIdents;
    }

    private void _fetchFromFile(
            final DocumentElement propertiesElement,
            final String from,
            final ConfigProperties group)
        throws ValidationException
    {
        try {
            final URL fromURL = new URL(getContextURL(), from);
            final boolean verify = propertiesElement
                .getAttributeValue(VERIFY_ATTRIBUTE, false);
            final String[] verifyKeyIdents = verify? getKeyIdents(
                propertiesElement,
                VERIFY_KEY_ELEMENT): _EMPTY_KEY_IDENTS;
            final boolean decrypt = propertiesElement
                .getAttributeValue(DECRYPT_ATTRIBUTE, false);
            final String[] decryptKeyIdents = decrypt? getKeyIdents(
                propertiesElement,
                DECRYPT_KEY_ELEMENT): _EMPTY_KEY_IDENTS;
            final String security = propertiesElement
                .getAttributeValue(SECURITY_ATTRIBUTE, Optional.empty())
                .orElse(null);
            final DocumentStream documentStream = DocumentStream
                .create(fromURL);
            final Reader reader;

            if (verify || decrypt) {
                final XMLDocument xmlDocument = new XMLDocument();

                try {
                    xmlDocument
                        .parse(documentStream, documentStream.getEncoding());
                } catch (final XMLDocument.ParseException exception) {
                    throw new RuntimeException(exception.getCause());
                }

                try {
                    documentStream.close();
                } catch (final IOException exception) {
                    throw new RuntimeException(exception);
                }

                final SecurityContext securityContext = new SecurityContext(
                    getLogger());
                final KeyedGroups configProperties = getConfig()
                    .getProperties();
                final KeyedGroups securityProperties = (security != null)
                    ? configProperties
                        .getGroup(security): KeyedGroups.MISSING_KEYED_GROUP;

                if (!securityContext
                    .setUp(configProperties, securityProperties)) {
                    throw new ValidationException(
                        ServiceMessages.PROPERTIES_GET_FAILED);
                }

                final Crypt crypt = new Crypt();

                if (!crypt
                    .setUp(
                        securityContext.getCryptProperties(),
                        Optional.empty())) {
                    throw new ValidationException(
                        ServiceMessages.PROPERTIES_GET_FAILED);
                }

                final Serializable serializable = crypt
                    .load(
                        xmlDocument,
                        from,
                        verify,
                        verifyKeyIdents,
                        decrypt,
                        decryptKeyIdents);

                if (serializable == null) {
                    throw new ValidationException(
                        ServiceMessages.PROPERTIES_GET_FAILED);
                }

                reader = new StringReader(String.valueOf(serializable));
            } else {
                reader = null;
            }

            final DocumentPropertiesMap properties = DocumentPropertiesMap
                .fetch(documentStream, Optional.ofNullable(reader));

            properties
                .forEach(
                    (key, value) -> {
                        final String substitute = getDocumentLoader()
                            .substitute(value);

                        if (group.getObject(key) != null) {
                            getLogger()
                                .debug(
                                        ServiceMessages.PREVIOUS_VALUE_OVERRIDDEN,
                                                key,
                                                substitute);
                        }

                        group.setValue(key, substitute);
                    });

            getDocumentLoader().updateStamp(properties.getStamp());

            getLogger()
                .debug(
                    ServiceMessages.GOT_PROPERTIES_FROM,
                    documentStream.getFromURL());
        } catch (final IOException exception) {
            final boolean optional = propertiesElement
                .getAttributeValue(OPTIONAL_ATTRIBUTE, false);
            final Message message = new Message(
                ServiceMessages.PROPERTIES_GET_FAILED_,
                from,
                exception.getMessage());

            if (optional) {
                getLogger().debug(message);
            } else {
                getLogger().error(message);
            }
        }
    }

    private void _setSystemProperties(final ConfigProperties group)
    {
        final DocumentSystemProperties properties = DocumentSystemProperties
            .getInstance();

        for (final Map.Entry<String, List<Object>> entry:
                group.getValuesEntries()) {
            final String key = entry.getKey();

            for (final Object value: entry.getValue()) {
                if (properties.containsValueKey(key)) {
                    getLogger().warn(ServiceMessages.PROPERTY_MULTIPLE, key);
                }

                properties.add(key, value);
            }
        }
    }

    private static final String[] _EMPTY_KEY_IDENTS = new String[0];
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
