/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MetadataDocumentLoader.java 4076 2019-06-11 17:09:30Z SFB $
 */

package org.rvpf.document.loader;

import java.io.Reader;

import java.net.URL;

import java.util.Locale;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.Attributes;
import org.rvpf.base.Params;
import org.rvpf.base.UUID;
import org.rvpf.base.exception.ValidationException;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.Config;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.AttributeDefEntity;
import org.rvpf.metadata.entity.AttributesDefEntity;
import org.rvpf.metadata.entity.BehaviorEntity;
import org.rvpf.metadata.entity.ContentEntity;
import org.rvpf.metadata.entity.EngineEntity;
import org.rvpf.metadata.entity.GroupEntity;
import org.rvpf.metadata.entity.OriginEntity;
import org.rvpf.metadata.entity.ParamDefEntity;
import org.rvpf.metadata.entity.PermissionsEntity;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.metadata.entity.StoreEntity;
import org.rvpf.metadata.entity.SyncEntity;
import org.rvpf.metadata.entity.TransformEntity;
import org.rvpf.service.ServiceMessages;

/**
 * Metadata document.
 */
public final class MetadataDocumentLoader
    extends ConfigDocumentLoader
{
    /**
     * Constructs an instance.
     *
     * @param metadata The metadata instance to use as destination.
     */
    MetadataDocumentLoader(@Nonnull final Metadata metadata)
    {
        super(metadata, METADATA_ROOT);

        _metadata = metadata;

        addPrefixNames(_PREFIX_NAMES);

        handle(
            MetadataElementLoader.ATTRIBUTES_DEF_ENTITY,
            new AttributesDefLoader());
        handle(MetadataElementLoader.BEHAVIOR_ENTITY, new BehaviorLoader());
        handle(MetadataElementLoader.CONTENT_ENTITY, new ContentLoader());
        handle(MetadataElementLoader.ENGINE_ENTITY, new EngineLoader());
        handle(MetadataElementLoader.GROUP_ENTITY, new GroupLoader());
        handle(MetadataElementLoader.ORIGIN_ENTITY, new OriginLoader());
        handle(MetadataElementLoader.PARAM_DEF_ENTITY, new ParamDefLoader());
        handle(
            MetadataElementLoader.PERMISSIONS_ENTITY,
            new PermissionsLoader());
        handle(MetadataElementLoader.POINT_ENTITY, new PointLoader());
        handle(MetadataElementLoader.PROCESSOR_ENTITY, new OriginLoader());
        handle(MetadataElementLoader.STORE_ENTITY, new StoreLoader());
        handle(MetadataElementLoader.SYNC_ENTITY, new SyncLoader());
        handle(MetadataElementLoader.TRANSFORM_ENTITY, new TransformLoader());
        handle(
            MetadataElementLoader.ATTRIBUTES_ELEMENT,
            new AttributesLoader());
    }

    /**
     * Fetches the Metadata.
     *
     * @param filter A metadata filter.
     * @param config The optional config.
     * @param uuid The  optional UUID for the metadata cache.
     * @param from The optional location to load from.
     *
     * @return The metadata (null on failure).
     */
    @Nullable
    @CheckReturnValue
    public static Metadata fetchMetadata(
            @Nonnull final MetadataFilter filter,
            @Nonnull Optional<Config> config,
            @Nonnull final Optional<UUID> uuid,
            @Nonnull Optional<String> from)
    {
        final Metadata metadata;
        final MetadataDocumentLoader document;
        Reader reader = null;

        if (!config.isPresent()) {
            config = Optional
                .ofNullable(
                    ConfigDocumentLoader
                        .loadConfig("", Optional.empty(), Optional.empty()));

            if (!config.isPresent()) {
                return null;
            }
        } else if (!config.get().getProperties().getOverrider().isPresent()) {
            config.get().fetchSystemProperties();
        }

        metadata = new Metadata(config.get());
        metadata.setFilter(filter);

        if (!from.isPresent()) {
            final MetadataCache metadataCache = new MetadataCache();

            if (metadataCache.setUp(config.get(), uuid, filter)) {
                if (metadataCache.refresh()) {
                    from = Optional.ofNullable(metadataCache.getFrom());
                    reader = metadataCache.getReader();
                }
            }

            metadataCache.tearDown();
        }

        if (!from.isPresent()) {
            final KeyedGroups metadataProperties = config
                .get()
                .getPropertiesGroup(METADATA_PROPERTIES);

            from = metadataProperties
                .getString(PATH_PROPERTY, Optional.of(DEFAULT_PATH));
        }

        document = new MetadataDocumentLoader(metadata);

        if (!document.loadFrom(from.get(), Optional.ofNullable(reader))) {
            return null;
        }

        if (!filter.tidy(metadata)) {
            return null;
        }

        if (!filter.areEntitiesKept()) {
            metadata.returnEntities();
        }

        return metadata;
    }

    /** {@inheritDoc}
     */
    @Override
    protected RootHandler getRootHandler()
    {
        return new MetadataHandler();
    }

    /** {@inheritDoc}
     */
    @Override
    protected URL getURL()
    {
        return _metadata.getURL();
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean loadFrom(final String from, final Optional<Reader> reader)
    {
        final boolean success;

        _reloading = !getEntities().isPresent();
        success = super.loadFrom(from, reader);

        if (getMetadata().getDomain().length() > 0) {
            getThisLogger()
                .debug(
                    ServiceMessages.METADATA_DOMAIN,
                    getMetadata().getDomain());
        }

        return success;
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean read(final String xml)
    {
        final MetadataFilter filter;

        if (!super.read(xml)) {
            return false;
        }

        filter = _metadata.getFilter();

        if (!filter.tidy(_metadata)) {
            return false;
        }

        if (!filter.areEntitiesKept()) {
            _metadata.returnEntities();
        }

        return true;
    }

    /**
     * Gets the attributes for an XML element.
     *
     * @param loader The current loader.
     * @param element The XML element containing the Params.
     *
     * @return The optional attributes.
     *
     * @throws ValidationException When appropriate.
     */
    @Nonnull
    @CheckReturnValue
    Optional<KeyedGroups> getAttributes(
            @Nonnull final MetadataElementLoader loader,
            @Nonnull final DocumentElement element)
        throws ValidationException
    {
        KeyedGroups attributesMap = null;

        if (getFilter().areAttributesNeeded()) {
            for (final DocumentElement attributesElement:
                    element
                        .getChildren(
                                MetadataElementLoader.ATTRIBUTES_ELEMENT)) {
                if (attributesElement.isEnabled()) {
                    final String usage = attributesElement
                        .getAttributeValue(
                            MetadataElementLoader.USAGE_ATTRIBUTE,
                            Optional.of(KeyedGroups.ANONYMOUS))
                        .orElse(null);
                    final String upperUsage = usage.toUpperCase(Locale.ROOT);

                    if (getFilter().areAttributesNeeded(upperUsage)) {
                        @SuppressWarnings("unchecked")
                        final Optional<AttributesDefEntity> attributesDef =
                            isValidationEnabled()
                            ? (Optional<AttributesDefEntity>) getEntity(
                                upperUsage,
                                AttributesDefEntity.ENTITY_PREFIX): Optional
                                    .empty();

                        if (attributesMap == null) {
                            attributesMap = new KeyedGroups();
                        }

                        if (attributesDef.isPresent()
                                && attributesDef.get().isHidden()) {
                            attributesMap.setValuesHidden(upperUsage);
                        }

                        Attributes attributes = (Attributes) attributesMap
                            .getObject(upperUsage);

                        if (attributes == null) {
                            attributes = new Attributes(usage);
                            attributesMap.setValue(upperUsage, attributes);
                        }

                        if (attributesElement
                            .getAttributeValue(
                                MetadataElementLoader.HIDDEN_ATTRIBUTE,
                                false)) {
                            attributes.setHidden(true);
                        }

                        _fetchAttributes(
                            loader,
                            attributesElement,
                            attributes,
                            attributesDef);
                        attributes.freeze();
                    }
                }
            }

            if (attributesMap != null) {
                attributesMap.freeze();
            }
        }

        return Optional.ofNullable(attributesMap);
    }

    /**
     * Gets the filter.
     *
     * @return The filter.
     */
    @Nonnull
    @CheckReturnValue
    MetadataFilter getFilter()
    {
        return _metadata.getFilter();
    }

    /**
     * Gets the metadata instance.
     *
     * @return The Metadata instance.
     */
    @Nonnull
    @CheckReturnValue
    Metadata getMetadata()
    {
        return _metadata;
    }

    /**
     * Gets the params of an XML element.
     *
     * @param loader The current loader.
     * @param element The XML element containing the Params.
     * @param holder The holder of Params to look for.
     *
     * @return The optional params.
     *
     * @throws ValidationException For Params not matched by ParamDef.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Params> getParams(
            @Nonnull final MetadataElementLoader loader,
            @Nonnull final DocumentElement element,
            @Nonnull final String holder)
        throws ValidationException
    {
        final Params params = new Params();
        final String prefix = ParamDefEntity.ENTITY_PREFIX + holder;

        for (final DocumentElement paramElement:
                element.getChildren(MetadataElementLoader.PARAM_ELEMENT)) {
            if (paramElement.isEnabled()) {
                String paramName = paramElement
                    .getAttributeValue(
                        DocumentElement.NAME_ATTRIBUTE,
                        Optional.empty())
                    .orElse(null);
                ParamDefEntity paramDef = null;

                if ((paramName == null) || isValidationEnabled()) {
                    final String def = paramElement
                        .getAttributeValue(
                            MetadataElementLoader.DEF_ATTRIBUTE,
                            Optional.empty())
                        .orElse(null);

                    if (def != null) {
                        if (paramName != null) {
                            getThisLogger()
                                .warn(
                                    ServiceMessages.IGNORED_ATTRIBUTE,
                                    DocumentElement.NAME_ATTRIBUTE);
                        }

                        paramDef = (ParamDefEntity) getEntity(def, prefix)
                            .orElse(null);

                        if (paramDef == null) {
                            getThisLogger()
                                .warn(
                                    ServiceMessages.PARAM_DEF_NOT_FOUND,
                                    def,
                                    holder);

                            continue;
                        }

                        if (!holder.equals(paramDef.getHolder())) {
                            getThisLogger()
                                .warn(
                                    ServiceMessages.UNEXPECTED_PARAM_DEF_HOLDER,
                                    holder,
                                    def,
                                    paramDef.getHolder());

                            continue;
                        }

                        paramName = paramDef.getName().get();
                    } else if (paramName != null) {
                        paramDef = (ParamDefEntity) getEntity(paramName, prefix)
                            .orElse(null);

                        if (paramDef == null) {
                            getThisLogger()
                                .warn(
                                    ServiceMessages.PARAM_DEF_NOT_FOUND,
                                    paramName,
                                    holder);
                        }
                    } else {
                        getThisLogger()
                            .warn(
                                ServiceMessages.PARAM_DEF_NOT_NAMED,
                                DocumentElement.NAME_ATTRIBUTE,
                                MetadataElementLoader.DEF_ATTRIBUTE);

                        continue;
                    }
                }

                loader
                    .putValues(
                        paramName,
                        paramElement,
                        _metadata.getProperties(),
                        Optional.ofNullable(paramDef),
                        params);
            }
        }

        if (params.isEmpty()) {
            return Optional.empty();
        }

        params.freeze();

        return Optional.of(params);
    }

    /** {@inheritDoc}
     */
    @Override
    boolean isValidationEnabled()
    {
        return !_reloading && super.isValidationEnabled();
    }

    private void _fetchAttributes(
            final MetadataElementLoader loader,
            final DocumentElement attributesElement,
            final Attributes attributes,
            final Optional<AttributesDefEntity> attributesDef)
        throws ValidationException
    {
        for (final DocumentElement attributeElement:
                attributesElement
                    .getChildren(MetadataElementLoader.ATTRIBUTE_ELEMENT)) {
            if (attributeElement.isEnabled()) {
                final String attributeName = attributeElement
                    .getAttributeValue(DocumentElement.NAME_ATTRIBUTE);
                final String upperAttributeName = attributeName
                    .toUpperCase(Locale.ROOT);
                final Optional<AttributeDefEntity> attributeDef = attributesDef
                    .isPresent()? attributesDef
                        .get()
                        .getAttributeDef(upperAttributeName): Optional.empty();

                if (attributesDef.isPresent()
                        && !attributeDef.isPresent()
                        && attributeElement.getAttributeValue(
                            ConfigElementLoader.VALIDATED_ATTRIBUTE,
                            true)) {
                    getThisLogger()
                        .warn(
                            ServiceMessages.ATTRIBUTE_DEF_MISSING,
                            attributeName,
                            attributesDef.get().getUsage());
                }

                loader
                    .putValues(
                        upperAttributeName,
                        attributeElement,
                        _metadata.getProperties(),
                        attributeDef,
                        attributes);
            }
        }
    }

    /** Default source of metadata XML text. */
    public static final String DEFAULT_PATH = "resource:rvpf-metadata.xml";

    /** Properties holding the metadata configuration. */
    public static final String METADATA_PROPERTIES = "metadata";

    /** Root element of metadata XML text. */
    public static final String METADATA_ROOT = "metadata";

    /** Property holding the path to the metadata XML text. */
    public static final String PATH_PROPERTY = "path";
    private static final String[][] _PREFIX_NAMES = {
        {AttributesDefEntity.ENTITY_PREFIX,
         MetadataElementLoader.ATTRIBUTES_DEF_ENTITY, },
        {BehaviorEntity.ENTITY_PREFIX, MetadataElementLoader.BEHAVIOR_ENTITY},
        {ContentEntity.ENTITY_PREFIX, MetadataElementLoader.CONTENT_ENTITY},
        {EngineEntity.ENTITY_PREFIX, MetadataElementLoader.ENGINE_ENTITY},
        {GroupEntity.ENTITY_PREFIX, MetadataElementLoader.GROUP_ENTITY},
        {OriginEntity.ENTITY_PREFIX, MetadataElementLoader.ORIGIN_ENTITY},
        {ParamDefEntity.ENTITY_PREFIX, MetadataElementLoader.PARAM_DEF_ENTITY},
        {PermissionsEntity.ENTITY_PREFIX,
         MetadataElementLoader.PERMISSIONS_ENTITY, },
        {PointEntity.ENTITY_PREFIX, MetadataElementLoader.POINT_ENTITY},
        {StoreEntity.ENTITY_PREFIX, MetadataElementLoader.STORE_ENTITY},
        {SyncEntity.ENTITY_PREFIX, MetadataElementLoader.SYNC_ENTITY},
        {TransformEntity.ENTITY_PREFIX, MetadataElementLoader.TRANSFORM_ENTITY},
    };

    private final Metadata _metadata;
    private boolean _reloading;

    /**
     * MetadataHandler.
     */
    protected class MetadataHandler
        extends MetadataDocumentLoader.ConfigHandler
    {
        /** {@inheritDoc}
         */
        @Override
        public void onRootEnd()
        {
            super.onRootEnd();

            if (isEnabled()) {
                final Optional<String> domain = getAttribute(
                    MetadataElementLoader.DOMAIN_ATTRIBUTE);

                if (domain.isPresent()) {
                    getMetadata().setDomain(domain.get());
                }
            }
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
