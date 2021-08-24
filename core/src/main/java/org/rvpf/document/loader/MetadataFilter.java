/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MetadataFilter.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.document.loader;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.exception.ValidationException;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.EngineEntity;
import org.rvpf.metadata.entity.OriginEntity;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.metadata.entity.StoreEntity;

/**
 * Metadata filter.
 *
 * <p>A subclass of this class is used to filter the content of the metadata for
 * a process. When a metadata document is read, the appropriate element loader
 * calls these methods to decide if the entity just built should be included in
 * the process metadata.</p>
 */
@ThreadSafe
public class MetadataFilter
    implements Cloneable
{
    /**
     * Constructs an instance.
     * @param keepEntities True to keep entities.
     */
    public MetadataFilter(final boolean keepEntities)
    {
        _keepEntities = keepEntities;
        _defaults = (this instanceof _Defaults)? null: new _Defaults(this);
    }

    /**
     * Asks if the attributes are needed.
     *
     * @return True if the attributes are needed.
     */
    @CheckReturnValue
    public boolean areAttributesNeeded()
    {
        return _defaults.areAttributesNeeded();
    }

    /**
     * Asks if the attributes are needed.
     *
     * @param usage The usage.
     *
     * @return True if the attributes are needed.
     */
    @CheckReturnValue
    public boolean areAttributesNeeded(@Nonnull final String usage)
    {
        return _defaults.areAttributesNeeded(usage);
    }

    /**
     * Asks if the behaviors are needed.
     *
     * @return True if the behaviors are needed.
     */
    @CheckReturnValue
    public boolean areBehaviorsNeeded()
    {
        return _defaults.areBehaviorsNeeded();
    }

    /**
     * Asks if the contents are needed.
     *
     * @return True if the contents are needed.
     */
    @CheckReturnValue
    public boolean areContentsNeeded()
    {
        return _defaults.areContentsNeeded();
    }

    /**
     * Asks if the contents are required.
     *
     * @return True if the contents are required.
     */
    @CheckReturnValue
    public boolean areContentsRequired()
    {
        return _defaults.areContentsRequired();
    }

    /**
     * Asks if the engines are filtered.
     *
     * <p>This would be the case if {@link #isEngineNeeded} does not always
     * return true.</p>
     *
     * @return True if the engines are filtered.
     */
    @CheckReturnValue
    public boolean areEnginesFiltered()
    {
        return _defaults.areEnginesFiltered();
    }

    /**
     * Asks if the engines are needed.
     *
     * @return True if the engines are needed.
     */
    @CheckReturnValue
    public boolean areEnginesNeeded()
    {
        return _defaults.areEnginesNeeded();
    }

    /**
     * Asks if the entities should be kept.
     *
     * @return True if the entities should be kept.
     */
    @CheckReturnValue
    public boolean areEntitiesKept()
    {
        return _keepEntities;
    }

    /**
     * Asks if the groups are needed.
     *
     * @return True if the groups are needed.
     */
    @CheckReturnValue
    public boolean areGroupsNeeded()
    {
        return _defaults.areGroupsNeeded();
    }

    /**
     * Asks if the origins are filtered.
     *
     * <p>This would be the case if {@link #isOriginNeeded} does not always
     * return true.</p>
     *
     * @return True if the origins are filtered.
     */
    @CheckReturnValue
    public boolean areOriginsFiltered()
    {
        return _defaults.areOriginsFiltered();
    }

    /**
     * Asks if the origins are needed.
     *
     * @return True if the origins are needed.
     */
    @CheckReturnValue
    public boolean areOriginsNeeded()
    {
        return _defaults.areOriginsNeeded();
    }

    /**
     * Asks if the origins are required.
     *
     * @return True if the origins are required.
     */
    @CheckReturnValue
    public boolean areOriginsRequired()
    {
        return _defaults.areOriginsRequired();
    }

    /**
     * Asks if the permissions are needed.
     *
     * @return True if the permissions are needed.
     */
    @CheckReturnValue
    public boolean arePermissionsNeeded()
    {
        return _defaults.arePermissionsNeeded();
    }

    /**
     * Asks if the point's inputs should be flagged as having results.
     *
     * @return True if point's inputs should be flagged.
     */
    @CheckReturnValue
    public boolean arePointInputsFlagged()
    {
        return _defaults.arePointInputsFlagged();
    }

    /**
     * Asks if any point's inputs are needed.
     *
     * @return True if any point's inputs are needed.
     */
    @CheckReturnValue
    public boolean arePointInputsNeeded()
    {
        return _defaults.arePointInputsNeeded();
    }

    /**
     * Asks if a point's inputs are needed.
     *
     * @param pointEntity The point Entity.
     *
     * @return True if the point's inputs are needed.
     *
     * @throws ValidationException When appropriate.
     */
    @CheckReturnValue
    public boolean arePointInputsNeeded(
            @Nonnull final PointEntity pointEntity)
        throws ValidationException
    {
        return _defaults.arePointInputsNeeded(pointEntity);
    }

    /**
     * Asks if point's replicates are needed.
     *
     * @return True if any point's replicates are needed.
     */
    @CheckReturnValue
    public boolean arePointReplicatesNeeded()
    {
        return _defaults.arePointReplicatesNeeded();
    }

    /**
     * Asks if any point is needed.
     *
     * @return True if any point is needed.
     */
    @CheckReturnValue
    public boolean arePointsNeeded()
    {
        return _defaults.arePointsNeeded();
    }

    /**
     * Asks if the stores are filtered.
     *
     * <p>This would be the case if {@link #isStoreNeeded} does not always
     * return true.</p>
     *
     * @return True if the stores are filtered.
     */
    @CheckReturnValue
    public boolean areStoresFiltered()
    {
        return _defaults.areStoresFiltered();
    }

    /**
     * Asks if the stores are needed.
     *
     * @return True if the stores are needed.
     */
    @CheckReturnValue
    public boolean areStoresNeeded()
    {
        return _defaults.areStoresNeeded();
    }

    /**
     * Asks if the stores are required.
     *
     * @return True if the stores are required.
     */
    @CheckReturnValue
    public boolean areStoresRequired()
    {
        return _defaults.areStoresRequired();
    }

    /**
     * Asks if the syncs are needed.
     *
     * @return True if the syncs are needed.
     */
    @CheckReturnValue
    public boolean areSyncsNeeded()
    {
        return _defaults.areSyncsNeeded();
    }

    /**
     * Asks if the texts are needed.
     *
     * @return True if the texts are needed.
     */
    @CheckReturnValue
    public boolean areTextsNeeded()
    {
        return _defaults.areTextsNeeded();
    }

    /**
     * Asks if the transforms are needed.
     *
     * @return True if the transforms are needed.
     */
    @CheckReturnValue
    public boolean areTransformsNeeded()
    {
        return _defaults.areTransformsNeeded();
    }

    /**
     * Asks if the transforms are required.
     *
     * @return True if the transforms are required.
     */
    @CheckReturnValue
    public boolean areTransformsRequired()
    {
        return _defaults.areTransformsRequired();
    }

    /** {@inheritDoc}
     */
    @Override
    public final MetadataFilter clone()
    {
        final MetadataFilter clone;

        try {
            clone = (MetadataFilter) super.clone();
        } catch (final CloneNotSupportedException exception) {
            throw new InternalError(exception);
        }

        clone.reset();

        return clone;
    }

    /**
     * Gets a client identification.
     *
     * @return The optional client ident.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<String> getClientIdent()
    {
        return _defaults.getClientIdent();
    }

    /**
     * Gets a XML String to send to a metadata server.
     *
     * @param domain The optional application domain.
     * @param after The optional last update time.
     *
     * @return The XML String.
     */
    @Nonnull
    @CheckReturnValue
    public String getXML(
            @Nonnull final Optional<String> domain,
            @Nonnull final Optional<DateTime> after)
    {
        final XMLDocument document = new XMLDocument(GET_METADATA_ROOT);
        final XMLElement root = document.getRootElement();

        if (domain.isPresent()) {
            root.setAttribute(DOMAIN_ATTRIBUTE, domain.get());
        }

        if (after.isPresent()) {
            root.setAttribute(AFTER_ATTRIBUTE, after.get().toString());
        }

        root.addChild(PROPERTIES_ELEMENT);

        includePointsXML(root);
        includeInputsXML(root);
        includeResultsXML(root);
        includeReplicatesXML(root);
        includeContentsXML(root);
        includeOriginsXML(root);
        includeStoresXML(root);
        includeSyncsXML(root);
        includeEnginesXML(root);
        includeTransformsXML(root);
        includeGroupsXML(root);
        includeAttributesXML(root);
        includeTextsXML(root);

        return document.toString();
    }

    /**
     * Asks if an engine is needed.
     *
     * @param engineEntity The engine entity.
     *
     * @return True if the engine is needed.
     *
     * @throws ValidationException When appropriate.
     */
    @CheckReturnValue
    public boolean isEngineNeeded(
            @Nonnull final EngineEntity engineEntity)
        throws ValidationException
    {
        return _defaults.isEngineNeeded(engineEntity);
    }

    /**
     * Asks if an origin is needed.
     *
     * @param originEntity The origin entity.
     *
     * @return True if the origin is needed.
     *
     * @throws ValidationException When appropriate.
     */
    @CheckReturnValue
    public boolean isOriginNeeded(
            @Nonnull final OriginEntity originEntity)
        throws ValidationException
    {
        return _defaults.isOriginNeeded(originEntity);
    }

    /**
     * Asks if a point is needed.
     *
     * @param pointEntity The point entity.
     *
     * @return True if the point is needed.
     *
     * @throws ValidationException When appropriate.
     */
    @CheckReturnValue
    public boolean isPointNeeded(
            @Nonnull final PointEntity pointEntity)
        throws ValidationException
    {
        return _defaults.isPointNeeded(pointEntity);
    }

    /**
     * Asks if a point's transform is needed.
     *
     * @param pointEntity The point entity.
     *
     * @return True if the point's transform is needed.
     *
     * @throws ValidationException When appropriate.
     */
    @CheckReturnValue
    public boolean isPointTransformNeeded(
            @Nonnull final PointEntity pointEntity)
        throws ValidationException
    {
        return _defaults.isPointTransformNeeded(pointEntity);
    }

    /**
     * Asks if a store is needed.
     *
     * @param storeEntity The store entity.
     *
     * @return True if the store is needed.
     *
     * @throws ValidationException When appropriate.
     */
    @CheckReturnValue
    public boolean isStoreNeeded(
            @Nonnull final StoreEntity storeEntity)
        throws ValidationException
    {
        return _defaults.isStoreNeeded(storeEntity);
    }

    /**
     * Tidies the metadata.
     *
     * <p>This is called after the document is loaded.</p>
     *
     * @param metadata The metadata.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public boolean tidy(@Nonnull final Metadata metadata)
    {
        boolean success = true;

        for (final Point point: metadata.getPointsCollection()) {
            success &= ((PointEntity) point).tidy();
        }

        return success && _defaults.tidy(metadata);
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return getXML(Optional.empty(), Optional.empty());
    }

    /**
     * Returns a child or this to call back.
     *
     * @return The child or this.
     */
    @CheckReturnValue
    protected MetadataFilter callBack()
    {
        return this;
    }

    /**
     * Includes attributes request XML if needed.
     *
     * @param root The root of the XML being built.
     */
    protected void includeAttributesXML(@Nonnull final XMLElement root)
    {
        if (callBack().areAttributesNeeded()) {
            root.addChild(ATTRIBUTES_ELEMENT);
        }
    }

    /**
     * Includes contents request XML if needed.
     *
     * @param root The root of the XML being built.
     */
    protected void includeContentsXML(@Nonnull final XMLElement root)
    {
        if (callBack().areContentsNeeded()) {
            root.addChild(CONTENTS_ELEMENT);
        }
    }

    /**
     * Includes engines request XML if needed.
     *
     * @param root The root of the XML being built.
     */
    protected void includeEnginesXML(@Nonnull final XMLElement root)
    {
        if (callBack().areEnginesNeeded()) {
            root.addChild(ENGINES_ELEMENT);
        }
    }

    /**
     * Includes groups request XML if needed.
     *
     * @param root The root of the XML being built.
     */
    protected void includeGroupsXML(@Nonnull final XMLElement root)
    {
        if (callBack().areGroupsNeeded()) {
            root.addChild(GROUPS_ELEMENT);
        }
    }

    /**
     * Includes inputs request XML if needed.
     *
     * @param root The root of the XML being built.
     */
    protected void includeInputsXML(@Nonnull final XMLElement root)
    {
        final MetadataFilter callBack = callBack();

        if (callBack.arePointInputsNeeded()
                || callBack.arePointInputsFlagged()) {
            root.addChild(INPUTS_ELEMENT);
        }
    }

    /**
     * Includes origins request XML if needed.
     *
     * @param root The root of the XML being built.
     */
    protected void includeOriginsXML(@Nonnull final XMLElement root)
    {
        if (callBack().areOriginsNeeded()) {
            root.addChild(ORIGINS_ELEMENT);
        }
    }

    /**
     * Includes points request XML if needed.
     *
     * @param root The root of the XML being built.
     */
    protected void includePointsXML(@Nonnull final XMLElement root)
    {
        if (callBack().arePointsNeeded()) {
            root.addChild(POINTS_ELEMENT);
        }
    }

    /**
     * Includes replicates request XML if needed.
     *
     * @param root The root of the XML being built.
     */
    protected void includeReplicatesXML(@Nonnull final XMLElement root)
    {
        if (callBack().arePointReplicatesNeeded()) {
            root.addChild(REPLICATES_ELEMENT);
        }
    }

    /**
     * Includes results request XML if needed.
     *
     * @param root The root of the XML being built.
     */
    protected void includeResultsXML(@Nonnull final XMLElement root)
    {
        if (callBack().arePointInputsNeeded()) {
            root.addChild(RESULTS_ELEMENT);
        }
    }

    /**
     * Includes stores request XML if needed.
     *
     * @param root The root of the XML being built.
     */
    protected void includeStoresXML(@Nonnull final XMLElement root)
    {
        if (callBack().areStoresNeeded()) {
            root.addChild(STORES_ELEMENT);
        }
    }

    /**
     * Includes syncs request XML if needed.
     *
     * @param root The root of the XML being built.
     */
    protected void includeSyncsXML(@Nonnull final XMLElement root)
    {
        if (callBack().areSyncsNeeded()) {
            root.addChild(SYNCS_ELEMENT);
        }
    }

    /**
     * Includes texts request XML if needed.
     *
     * @param root The root of the XML being built.
     */
    protected void includeTextsXML(@Nonnull final XMLElement root)
    {
        if (callBack().areTextsNeeded()) {
            root.addChild(TEXTS_ELEMENT);
        }
    }

    /**
     * Includes transforms request XML if needed.
     *
     * @param root The root of the XML being built.
     */
    protected void includeTransformsXML(@Nonnull final XMLElement root)
    {
        if (callBack().areTransformsNeeded()) {
            root.addChild(TRANSFORMS_ELEMENT);
        }
    }

    /**
     * Resets to pristine state.
     *
     * <p>Note: when overriding, call super last to complete reset.</p>
     */
    protected void reset() {}

    /** After attribute. */
    public static final String AFTER_ATTRIBUTE = "after";

    /** Attributes element. */
    public static final String ATTRIBUTES_ELEMENT = "attributes";

    /** Contents element. */
    public static final String CONTENTS_ELEMENT = "contents";

    /** Domain attribute. */
    public static final String DOMAIN_ATTRIBUTE = "domain";

    /** Engines element. */
    public static final String ENGINES_ELEMENT = "engines";

    /** Engine attribute. */
    public static final String ENGINE_ATTRIBUTE = "engine";

    /** Engine element. */
    public static final String ENGINE_ELEMENT = "engine";

    /** Root element. */
    public static final String GET_METADATA_ROOT = "get-metadata";

    /** Groups element. */
    public static final String GROUPS_ELEMENT = "groups";

    /** Group element. */
    public static final String GROUP_ELEMENT = "group";

    /** Inputs element. */
    public static final String INPUTS_ELEMENT = "inputs";

    /** Origins element. */
    public static final String ORIGINS_ELEMENT = "origins";

    /** Origin attribute. */
    public static final String ORIGIN_ATTRIBUTE = "origin";

    /** Origin element. */
    public static final String ORIGIN_ELEMENT = "origin";

    /** Points element. */
    public static final String POINTS_ELEMENT = "points";

    /** Properties element. */
    public static final String PROPERTIES_ELEMENT = "properties";

    /** Replicates element. */
    public static final String REPLICATES_ELEMENT = "replicates";

    /** Results element. */
    public static final String RESULTS_ELEMENT = "results";

    /** Stores element. */
    public static final String STORES_ELEMENT = "stores";

    /** Store attribute. */
    public static final String STORE_ATTRIBUTE = "store";

    /** Store element. */
    public static final String STORE_ELEMENT = "store";

    /** Syncs element. */
    public static final String SYNCS_ELEMENT = "syncs";

    /** Texts element. */
    public static final String TEXTS_ELEMENT = "texts";

    /** Transforms element. */
    public static final String TRANSFORMS_ELEMENT = "transforms";

    private MetadataFilter _defaults;
    private final boolean _keepEntities;

    /**
     * Defaults.
     */
    private static final class _Defaults
        extends MetadataFilter
    {
        /**
         * Constructs an instance.
         *
         * @param child The parent's child.
         */
        _Defaults(final MetadataFilter child)
        {
            super(false);

            _child = child;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean areAttributesNeeded()
        {
            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean areAttributesNeeded(final String usage)
        {
            return callBack().areAttributesNeeded();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean areBehaviorsNeeded()
        {
            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean areContentsNeeded()
        {
            return callBack().arePointInputsNeeded();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean areContentsRequired()
        {
            return callBack().areContentsNeeded();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean areEnginesFiltered()
        {
            return !callBack().areEnginesNeeded();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean areEnginesNeeded()
        {
            return callBack().areTransformsNeeded();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean areEntitiesKept()
        {
            throw new InternalError();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean areGroupsNeeded()
        {
            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean areOriginsFiltered()
        {
            return !callBack().areOriginsNeeded();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean areOriginsNeeded()
        {
            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean areOriginsRequired()
        {
            return callBack().areOriginsNeeded();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean arePermissionsNeeded()
        {
            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean arePointInputsFlagged()
        {
            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean arePointInputsNeeded()
        {
            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean arePointInputsNeeded(final PointEntity pointEntity)
        {
            return callBack().arePointInputsNeeded();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean arePointReplicatesNeeded()
        {
            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean arePointsNeeded()
        {
            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean areStoresFiltered()
        {
            return !callBack().areStoresNeeded();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean areStoresNeeded()
        {
            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean areStoresRequired()
        {
            return callBack().areStoresNeeded();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean areSyncsNeeded()
        {
            return callBack().arePointsNeeded();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean areTextsNeeded()
        {
            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean areTransformsNeeded()
        {
            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean areTransformsRequired()
        {
            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<String> getClientIdent()
        {
            return Optional.empty();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isEngineNeeded(final EngineEntity engineEntity)
        {
            return callBack().areEnginesNeeded();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isOriginNeeded(final OriginEntity originEntity)
        {
            return callBack().areOriginsNeeded();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isPointNeeded(final PointEntity pointEntity)
        {
            return callBack().arePointsNeeded();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isPointTransformNeeded(final PointEntity pointEntity)
        {
            return callBack().areTransformsNeeded();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isStoreNeeded(
                final StoreEntity storeEntity)
            throws ValidationException
        {
            return callBack().areStoresNeeded();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean tidy(final Metadata metadata)
        {
            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        protected MetadataFilter callBack()
        {
            return _child.callBack();
        }

        private MetadataFilter _child;
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
