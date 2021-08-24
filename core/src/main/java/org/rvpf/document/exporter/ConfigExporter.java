/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ConfigExporter.java 4103 2019-07-01 13:31:25Z SFB $
 */

package org.rvpf.document.exporter;

import java.nio.charset.StandardCharsets;

import java.security.SecureRandom;

import java.util.Base64;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.Entity;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.config.Config;
import org.rvpf.config.entity.ClassDefEntity;
import org.rvpf.document.loader.ConfigDocumentLoader;
import org.rvpf.document.loader.ConfigElementLoader;

/**
 * Config exporter.
 */
public class ConfigExporter
    extends XMLExporter
{
    /**
     * Constructs an instance.
     *
     * @param config The config.
     * @param elementName The element name.
     * @param secure True if the destination is secure.
     */
    ConfigExporter(
            @Nonnull final Config config,
            @Nonnull final String elementName,
            final boolean secure)
    {
        super(Optional.empty());

        _config = config;
        _nextId = 1;
        _secure = secure;

        getDocument().setRootElement(Optional.of(createElement(elementName)));
    }

    /** {@inheritDoc}
     */
    @Override
    protected final Config getConfig()
    {
        return _config;
    }

    /** {@inheritDoc}
     */
    @Override
    protected String getElementName()
    {
        return ConfigDocumentLoader.CONFIG_ROOT;
    }

    /** {@inheritDoc}
     */
    @Override
    protected final XMLElement getRootElement()
    {
        return getDocument().getRootElement();
    }

    /** {@inheritDoc}
     */
    @Override
    protected final int nextId()
    {
        return _nextId++;
    }

    /**
     * Adds exporters.
     */
    void addExporters()
    {
        new ClassLibExporter(this);
        new ClassDefExporter(this);
    }

    /**
     * Exports this.
     *
     * @return The document.
     */
    @Nonnull
    @CheckReturnValue
    XMLDocument export()
    {
        registerReferences();

        final Optional<DateTime> configStamp = _config.getStamp();

        if (configStamp.isPresent()) {
            getRootElement()
                .setAttribute(
                    ConfigElementLoader.STAMP_ATTRIBUTE,
                    Optional.of(configStamp.get().toString()));
        }

        final XMLElement saltElement = createElement(SALT_ELEMENT);
        final byte[] saltBytes = new byte[SALT_LENGTH];
        final byte[] base64Bytes;
        final String base64String;

        new SecureRandom().nextBytes(saltBytes);
        base64Bytes = Base64.getEncoder().encode(saltBytes);
        base64String = new String(base64Bytes, StandardCharsets.UTF_8);
        saltElement
            .addText(_EQUAL_PATTERN.matcher(base64String).replaceAll(""));

        getRootElement().addChild(saltElement);

        for (final XMLExporter exporter: _exporters.values()) {
            ((EntityExporter) exporter).export();
        }

        final PropertiesExporter propertiesExporter = new PropertiesExporter(
            this);

        propertiesExporter.export(_config.getProperties(), getRootElement());
        propertiesExporter.exportService(getRootElement());

        return getDocument();
    }

    /**
     * Gets the entities.
     *
     * @return The optional entities.
     */
    @Nonnull
    @CheckReturnValue
    final Optional<? extends Map<String, Entity>> getEntities()
    {
        return _config.getEntities();
    }

    /**
     * Gets the exporters Map.
     *
     * @return The exporters Map.
     */
    @Nonnull
    @CheckReturnValue
    final Map<Class<? extends Entity>, XMLExporter> getExporters()
    {
        return _exporters;
    }

    /**
     * Gets the languages to include for information texts.
     *
     * @return The optional languages to include for information texts.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Set<String>> getLangs()
    {
        return Optional.empty();
    }

    /**
     * Gets the entity references.
     *
     * @return The entity references.
     */
    @Nonnull
    @CheckReturnValue
    final Map<Entity, EntityReference> getReferences()
    {
        return _references;
    }

    /**
     * Gets the attributes to include.
     *
     * @return The optional attributes.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Set<String>> getUsages()
    {
        return Optional.empty();
    }

    /**
     * Gets the secure destination indicator.
     *
     * @return True if the destination is secure.
     */
    @CheckReturnValue
    boolean isSecure()
    {
        return _secure;
    }

    /**
     * Registers references.
     */
    void registerReferences()
    {
        final String serviceName = getConfig().getServiceName();
        final KeyedGroups properties = getConfig()
            .getServiceContext(serviceName)
            .orElse(null);

        _registerReferences(_config.getProperties());

        if (properties != null) {
            _registerReferences(properties);
        }
    }

    private void _registerReferences(final KeyedGroups properties)
    {
        for (final Map.Entry<String, List<Object>> propertyEntry:
                properties.getValuesEntries()) {
            for (final Object propertyValue: propertyEntry.getValue()) {
                if (propertyValue instanceof ClassDefEntity) {
                    registerReference(
                        Optional.of((ClassDefEntity) propertyValue));
                }
            }
        }

        for (final Map.Entry<String, List<Object>> groupEntry:
                properties.getGroupsEntries()) {
            for (final Object group: groupEntry.getValue()) {
                _registerReferences((KeyedGroups) group);
            }
        }
    }

    /** Salt element. */
    public static final String SALT_ELEMENT = "salt";

    /** Salt length (bytes). */
    public static final int SALT_LENGTH = 32;
    private static final Pattern _EQUAL_PATTERN = Pattern.compile("=");

    private final Config _config;
    private final Map<Class<? extends Entity>, XMLExporter> _exporters =
        new LinkedHashMap<Class<? extends Entity>, XMLExporter>();
    private int _nextId;
    private final Map<Entity, EntityReference> _references =
        new IdentityHashMap<>();
    private final boolean _secure;
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
