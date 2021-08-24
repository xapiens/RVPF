/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MetadataElementLoader.java 4078 2019-06-11 20:55:00Z SFB $
 */

package org.rvpf.document.loader;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.Entity;
import org.rvpf.base.Params;
import org.rvpf.base.UUID;
import org.rvpf.base.exception.ValidationException;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.TopologicalErrorException;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.Text;
import org.rvpf.metadata.entity.BehaviorEntity;
import org.rvpf.metadata.entity.ContentEntity;
import org.rvpf.metadata.entity.EngineEntity;
import org.rvpf.metadata.entity.GroupEntity;
import org.rvpf.metadata.entity.MetadataEntity;
import org.rvpf.metadata.entity.OriginEntity;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.metadata.entity.StoreEntity;
import org.rvpf.metadata.entity.SyncEntity;
import org.rvpf.metadata.entity.TransformEntity;
import org.rvpf.service.ServiceMessages;

/**
 * Metadata document element loader.
 */
public abstract class MetadataElementLoader
    extends ConfigElementLoader
{
    /**
     * Anchors an entity if requested.
     *
     * @param entity The entity.
     */
    protected void anchorEntity(@Nonnull final Entity entity)
    {
        if (getElement().getAttributeValue(ANCHORED_ATTRIBUTE, false)) {
            getMetadata().anchor(entity);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected final Optional<String> getLang()
    {
        return getElement()
            .getAttributeValue(DocumentElement.LANG_ATTRIBUTE, super.getLang());
    }

    /** {@inheritDoc}
     */
    @Override
    protected final void updateTexts()
    {
        final String entityPrefix = getEntityPrefix();
        final MetadataEntity entity;
        String key;

        if ((entityPrefix.isEmpty()) || !getMetadataFilter().areTextsNeeded()) {
            return;
        }

        key = getElement().getId().orElse(null);

        if (key == null) {
            final Optional<UUID> uuid = getElement().getUUID();

            if (uuid.isPresent()) {
                key = uuid.get().toString();
            }
        }

        if (key == null) {
            key = getElement().getNameAttribute().orElse(null);
        }

        if (key == null) {
            getLogger()
                .warn(
                    ServiceMessages.ENTITY_FOR_LANG,
                    getElement().getName(),
                    getLang().orElse(null));

            return;
        }

        try {
            entity = (MetadataEntity) getEntity(key, entityPrefix);
        } catch (final ValidationException exception) {
            getLogger().warn(BaseMessages.VERBATIM, exception.getMessage());

            return;
        }

        _updateTexts(entity);
    }

    /**
     * Adds a member to the referenced group.
     *
     * @param member The group member.
     *
     * @throws ValidationException When appropriate.
     */
    final void addGroupMember(
            @Nonnull final Entity member)
        throws ValidationException
    {
        if (getMetadataFilter().areGroupsNeeded()) {
            Optional<String> groupReference = getElement()
                .getAttributeValue(GROUP_REFERENCE, Optional.empty());

            if (groupReference.isPresent()) {
                _addGroupMember(member, groupReference.get());
            }

            for (final DocumentElement groupReferenceElement:
                    getElement().getChildren(GROUP_REFERENCE)) {
                groupReference = groupReferenceElement
                    .getAttributeValue(GROUP_REFERENCE, Optional.empty());

                if (!groupReference.isPresent()) {
                    throw new ValidationException(
                        ServiceMessages.MISSING_ATTRIBUTE_IN,
                        GROUP_REFERENCE,
                        groupReferenceElement.getName());

                }

                _addGroupMember(member, groupReference.get());
            }
        }
    }

    /**
     * Gets the attributes for an element.
     *
     * @param element The element.
     *
     * @return The optional attributes.
     *
     * @throws ValidationException When appropriate.
     */
    @Nonnull
    @CheckReturnValue
    final Optional<KeyedGroups> getAttributes(
            @Nonnull final DocumentElement element)
        throws ValidationException
    {
        return ((MetadataDocumentLoader) getDocumentLoader())
            .getAttributes(this, element);
    }

    /**
     * Gets the entity prefix.
     *
     * @return The entity prefix or null.
     */
    @Nonnull
    @CheckReturnValue
    String getEntityPrefix()
    {
        return "";
    }

    /**
     * Gets the metadata.
     *
     * @return The metadata.
     */
    @Nonnull
    @CheckReturnValue
    final Metadata getMetadata()
    {
        return ((MetadataDocumentLoader) getDocumentLoader()).getMetadata();
    }

    /**
     * Gets the metadata filter.
     *
     * @return The metadata filter.
     */
    @Nonnull
    @CheckReturnValue
    final MetadataFilter getMetadataFilter()
    {
        return ((MetadataDocumentLoader) getDocumentLoader()).getFilter();
    }

    /**
     * Gets the params for an element.
     *
     * @param element The params owner.
     * @param holder A name for the type of params holder.
     *
     * @return The optional params.
     *
     * @throws ValidationException When the param validation fails.
     */
    @Nonnull
    @CheckReturnValue
    final Optional<Params> getParams(
            @Nonnull final DocumentElement element,
            @Nonnull final String holder)
        throws ValidationException
    {
        return ((MetadataDocumentLoader) getDocumentLoader())
            .getParams(this, element, holder);
    }

    /**
     * Sets up an entity.
     *
     * @param entity The entity.
     */
    final void setUpEntity(@Nonnull final MetadataEntity entity)
    {
        entity.setUUID(getElement().getUUID());
        entity.setName(getElement().getNameAttribute());

        _updateTexts(entity);
    }

    /**
     * Sets up an entity builder.
     *
     * @param metadataEntityBuilder The entity builder.
     *
     * @return The entity builder.
     */
    final MetadataEntity.Builder setUpEntityBuilder(
            @Nonnull final MetadataEntity.Builder metadataEntityBuilder)
    {
        metadataEntityBuilder
            .setUUID(getElement().getUUID().orElse(null))
            .setName(getElement().getNameAttribute().orElse(null));

        _updateTexts(metadataEntityBuilder);

        return metadataEntityBuilder;
    }

    private static void _addTexts(
            final DocumentElement element,
            final String defaultLang,
            final MetadataEntity entity)
    {
        Text text;

        text = new Text(Optional.ofNullable(defaultLang));
        text
            .setIdent(
                element.getAttributeValue(IDENT_ATTRIBUTE, Optional.empty()));
        text
            .setTitle(
                element.getAttributeValue(TITLE_ATTRIBUTE, Optional.empty()));
        text
            .setDescription(
                element
                    .getAttributeValue(
                            DESCRIPTION_ATTRIBUTE,
                                    Optional.empty()));
        text
            .addNotes(
                element.getAttributeValue(NOTES_ATTRIBUTE, Optional.empty()));

        if (!text.isEmpty()) {
            entity.addText(text);
        }

        for (final DocumentElement titleElement:
                element.getChildren(TITLE_ELEMENT)) {
            text = new Text(
                titleElement.getAttributeValue(
                    DocumentElement.LANG_ATTRIBUTE,
                    Optional.ofNullable(defaultLang)));
            text.setTitle(Optional.of(titleElement.getText()));
            entity.addText(text);
        }

        for (final DocumentElement descriptionElement:
                element.getChildren(DESCRIPTION_ELEMENT)) {
            text = new Text(
                descriptionElement.getAttributeValue(
                    DocumentElement.LANG_ATTRIBUTE,
                    Optional.ofNullable(defaultLang)));
            text.setDescription(Optional.of(descriptionElement.getText()));
            entity.addText(text);
        }

        for (final DocumentElement notesElement:
                element.getChildren(NOTES_ELEMENT)) {
            text = new Text(
                notesElement.getAttributeValue(
                    DocumentElement.LANG_ATTRIBUTE,
                    Optional.ofNullable(defaultLang)));
            text.addNotes(Optional.of(notesElement.getText()));
            entity.addText(text);
        }

        for (final DocumentElement otherElement:
                element.getChildren(OTHER_ELEMENT)) {
            text = new Text(
                otherElement.getAttributeValue(
                    DocumentElement.LANG_ATTRIBUTE,
                    Optional.ofNullable(defaultLang)));
            text
                .addOther(
                    otherElement
                        .getAttributeValue(ELEMENT_ATTRIBUTE, Optional.empty())
                        .orElse(null),
                    otherElement.getText());
            entity.addText(text);
        }
    }

    private static void _addTexts(
            final DocumentElement element,
            final String defaultLang,
            final MetadataEntity.Builder entityBuilder)
    {
        Text text;

        text = new Text(Optional.ofNullable(defaultLang));
        text
            .setIdent(
                element.getAttributeValue(IDENT_ATTRIBUTE, Optional.empty()));
        text
            .setTitle(
                element.getAttributeValue(TITLE_ATTRIBUTE, Optional.empty()));
        text
            .setDescription(
                element
                    .getAttributeValue(
                            DESCRIPTION_ATTRIBUTE,
                                    Optional.empty()));
        text
            .addNotes(
                element.getAttributeValue(NOTES_ATTRIBUTE, Optional.empty()));

        if (!text.isEmpty()) {
            entityBuilder.addText(text);
        }

        for (final DocumentElement titleElement:
                element.getChildren(TITLE_ELEMENT)) {
            text = new Text(
                titleElement.getAttributeValue(
                    DocumentElement.LANG_ATTRIBUTE,
                    Optional.ofNullable(defaultLang)));
            text.setTitle(Optional.of(titleElement.getText()));
            entityBuilder.addText(text);
        }

        for (final DocumentElement descriptionElement:
                element.getChildren(DESCRIPTION_ELEMENT)) {
            text = new Text(
                descriptionElement.getAttributeValue(
                    DocumentElement.LANG_ATTRIBUTE,
                    Optional.ofNullable(defaultLang)));
            text.setDescription(Optional.of(descriptionElement.getText()));
            entityBuilder.addText(text);
        }

        for (final DocumentElement notesElement:
                element.getChildren(NOTES_ELEMENT)) {
            text = new Text(
                notesElement.getAttributeValue(
                    DocumentElement.LANG_ATTRIBUTE,
                    Optional.ofNullable(defaultLang)));
            text.addNotes(Optional.of(notesElement.getText()));
            entityBuilder.addText(text);
        }

        for (final DocumentElement otherElement:
                element.getChildren(OTHER_ELEMENT)) {
            text = new Text(
                otherElement.getAttributeValue(
                    DocumentElement.LANG_ATTRIBUTE,
                    Optional.ofNullable(defaultLang)));
            text
                .addOther(
                    otherElement
                        .getAttributeValue(ELEMENT_ATTRIBUTE, Optional.empty())
                        .orElse(null),
                    otherElement.getText());
            entityBuilder.addText(text);
        }
    }

    private void _addGroupMember(
            final Entity groupMember,
            final String groupReference)
        throws ValidationException
    {
        final GroupEntity groupEntity = (GroupEntity) getEntity(
            groupReference,
            GroupEntity.ENTITY_PREFIX);
        final boolean addedToGroup;

        try {
            addedToGroup = groupEntity.addMember(groupMember);
        } catch (final TopologicalErrorException exception) {
            throw new RuntimeException(exception);    // Should not happen.
        }

        if (!addedToGroup) {
            getLogger()
                .warn(
                    ServiceMessages.DUPLICATE_GROUP_ENTRY,
                    getElement().getName(),
                    groupMember.getName().orElse(null),
                    groupEntity.getName().orElse(null));
        }
    }

    private void _updateTexts(final MetadataEntity entity)
    {
        if (getMetadataFilter().areTextsNeeded()) {
            final DocumentElement element = getElement();
            final Optional<String> defaultLang = getLang();

            _addTexts(element, defaultLang.orElse(null), entity);

            for (final DocumentElement textElement:
                    element.getChildren(TEXT_ELEMENT)) {
                final String lang = textElement
                    .getAttributeValue(
                        DocumentElement.LANG_ATTRIBUTE,
                        defaultLang)
                    .orElse(null);

                _addTexts(textElement, lang, entity);
            }
        }
    }

    private void _updateTexts(final MetadataEntity.Builder entityBuilder)
    {
        if (getMetadataFilter().areTextsNeeded()) {
            final DocumentElement element = getElement();
            final Optional<String> defaultLang = getLang();

            _addTexts(element, defaultLang.orElse(null), entityBuilder);

            for (final DocumentElement textElement:
                    element.getChildren(TEXT_ELEMENT)) {
                final String lang = textElement
                    .getAttributeValue(
                        DocumentElement.LANG_ATTRIBUTE,
                        defaultLang)
                    .orElse(null);

                _addTexts(textElement, lang, entityBuilder);
            }
        }
    }

    /** Anchored attribute. */
    public static final String ANCHORED_ATTRIBUTE = "anchored";

    /** Arg element. */
    public static final String ARG_ELEMENT = "arg";

    /** AttributesDef entity element. */
    public static final String ATTRIBUTES_DEF_ENTITY = "AttributesDef";

    /** Attributes element. */
    public static final String ATTRIBUTES_ELEMENT = "attributes";

    /** AttributeDef element. */
    public static final String ATTRIBUTE_DEF_ELEMENT = "AttributeDef";

    /** Attribute element. */
    public static final String ATTRIBUTE_ELEMENT = "attribute";

    /** Behavior entity element. */
    public static final String BEHAVIOR_ENTITY = BehaviorEntity.ELEMENT_NAME;

    /** Behavior reference. */
    public static final String BEHAVIOR_REFERENCE =
        BehaviorEntity.ENTITY_REFERENCE_NAME;

    /** Clone attribute. */
    public static final String CLONE_ATTRIBUTE = "clone";

    /** Content entity element. */
    public static final String CONTENT_ENTITY = ContentEntity.ELEMENT_NAME;

    /** Content reference. */
    public static final String CONTENT_REFERENCE =
        ContentEntity.ENTITY_REFERENCE_NAME;

    /** Control attribute. */
    public static final String CONTROL_ATTRIBUTE = "control";

    /** Convert attribute. */
    public static final String CONVERT_ATTRIBUTE = "convert";

    /** Description attribute. */
    public static final String DESCRIPTION_ATTRIBUTE = "description";

    /** Description element. */
    public static final String DESCRIPTION_ELEMENT = "description";

    /** Domain attribute. */
    public static final String DOMAIN_ATTRIBUTE = "domain";

    /** Element attribute. */
    public static final String ELEMENT_ATTRIBUTE = "element";

    /** Engine entity element. */
    public static final String ENGINE_ENTITY = EngineEntity.ELEMENT_NAME;

    /** Engine reference. */
    public static final String ENGINE_REFERENCE =
        EngineEntity.ENTITY_REFERENCE_NAME;

    /** Group entity element. */
    public static final String GROUP_ENTITY = GroupEntity.ELEMENT_NAME;

    /** Group reference. */
    public static final String GROUP_REFERENCE =
        GroupEntity.ENTITY_REFERENCE_NAME;

    /** Holder attribute. */
    public static final String HOLDER_ATTRIBUTE = "holder";

    /** Ident attribute. */
    public static final String IDENT_ATTRIBUTE = "ident";

    /** Input element. */
    public static final String INPUT_ELEMENT = "input";

    /** Member element. */
    public static final String MEMBER_ELEMENT = "member";

    /** Notes attribute. */
    public static final String NOTES_ATTRIBUTE = "notes";

    /** Notes element. */
    public static final String NOTES_ELEMENT = "notes";

    /** Origin entity element. */
    public static final String ORIGIN_ENTITY = OriginEntity.ELEMENT_NAME;

    /** Origin reference. */
    public static final String ORIGIN_REFERENCE =
        OriginEntity.ENTITY_REFERENCE_NAME;

    /** Other element. */
    public static final String OTHER_ELEMENT = "other";

    /** ParamDef entity element. */
    public static final String PARAM_DEF_ENTITY = "ParamDef";

    /** Param element. */
    public static final String PARAM_ELEMENT = "param";

    /** Permissions element. */
    public static final String PERMISSIONS_ELEMENT = "permissions";

    /** Permissions entity element. */
    public static final String PERMISSIONS_ENTITY = "Permissions";

    /** Permissions reference. */
    public static final String PERMISSIONS_REFERENCE = "permissions";

    /** Point entity element. */
    public static final String POINT_ENTITY = PointEntity.ELEMENT_NAME;

    /** Point reference. */
    public static final String POINT_REFERENCE =
        PointEntity.ENTITY_REFERENCE_NAME;

    /** Processor entity element. */
    public static final String PROCESSOR_ENTITY = "Processor";

    /** Processor reference. */
    public static final String PROCESSOR_REFERENCE = "processor";

    /** Replicate element. */
    public static final String REPLICATE_ELEMENT = "replicate";

    /** Store entity element. */
    public static final String STORE_ENTITY = StoreEntity.ELEMENT_NAME;

    /** Store reference. */
    public static final String STORE_REFERENCE =
        StoreEntity.ENTITY_REFERENCE_NAME;

    /** Sync entity element. */
    public static final String SYNC_ENTITY = SyncEntity.ELEMENT_NAME;

    /** Sync reference. */
    public static final String SYNC_REFERENCE =
        SyncEntity.ENTITY_REFERENCE_NAME;

    /** Text element. */
    public static final String TEXT_ELEMENT = "text";

    /** Title attribute. */
    public static final String TITLE_ATTRIBUTE = "title";

    /** Title element. */
    public static final String TITLE_ELEMENT = "title";

    /** Transform entity element. */
    public static final String TRANSFORM_ENTITY = TransformEntity.ELEMENT_NAME;

    /** Transform reference. */
    public static final String TRANSFORM_REFERENCE =
        TransformEntity.ENTITY_REFERENCE_NAME;

    /** Unspecified content. */
    public static final String UNSPECIFIED_CONTENT = "Unspecified";

    /** Unspecified origin. */
    public static final String UNSPECIFIED_ORIGIN = "Unspecified";

    /** Unspecified store. */
    public static final String UNSPECIFIED_STORE = "Unspecified";

    /** Unspecified transform. */
    public static final String UNSPECIFIED_TRANSFORM = "Unspecified";

    /** Usage attribute. */
    public static final String USAGE_ATTRIBUTE = "usage";
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
