/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SyncEntity.java 4096 2019-06-24 23:07:39Z SFB $
 */

package org.rvpf.metadata.entity;

import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Params;
import org.rvpf.base.UUID;
import org.rvpf.base.sync.Sync;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.entity.ClassDefEntity;
import org.rvpf.metadata.Text;

/**
 * Sync entity.
 *
 * <p>Instances of this class supply an object implementing the {@link Sync}
 * interface.</p>
 */
public final class SyncEntity
    extends ParamsEntity
{
    /**
     * Constructs an instance.
     *
     * @param name The optional entity name.
     * @param uuid The optional entity UUID.
     * @param attributes The optional attributes.
     * @param texts The optional texts.
     * @param params The optional params.
     * @param classDef The class definition.
     * @param sync The optional Sync instance.
     */
    protected SyncEntity(
            @Nonnull final Optional<String> name,
            @Nonnull final Optional<UUID> uuid,
            @Nonnull final Optional<KeyedGroups> attributes,
            @Nonnull final Optional<Map<String, Text>> texts,
            @Nonnull final Optional<Params> params,
            @Nonnull final ClassDefEntity classDef,
            @Nonnull final Optional<Sync> sync)
    {
        super(name, uuid, attributes, texts, params);

        _classDef = Require.notNull(classDef);
        _sync = Require.notNull(sync);
    }

    /**
     * Returns a new builder.
     *
     * @return The new builder.
     */
    @Nonnull
    @CheckReturnValue
    public static Builder newBuilder()
    {
        return new Builder();
    }

    /** {@inheritDoc}
     */
    @Override
    public SyncEntity copy()
    {
        return newBuilder().copyFrom(this).build();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (this == other) {
            return true;
        }

        return super.equals(other);
    }

    /**
     * Gets the classDef.
     *
     * @return The classDef.
     */
    @Nonnull
    @CheckReturnValue
    public ClassDefEntity getClassDef()
    {
        return Require.notNull(_classDef);
    }

    /** {@inheritDoc}
     */
    @Override
    public String getElementName()
    {
        return ELEMENT_NAME;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getPrefix()
    {
        return ENTITY_PREFIX;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getReferenceName()
    {
        return ENTITY_REFERENCE_NAME;
    }

    /**
     * Gets a Sync instance.
     *
     * @return The Sync instance.
     */
    @Nonnull
    @CheckReturnValue
    public Sync getSync()
    {
        return _sync.get().copy();
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    /**
     * Sets up this.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public boolean setUp()
    {
        if (!_sync.isPresent()) {
            _sync = Optional
                .ofNullable(getClassDef().createInstance(Sync.class));

            if (!_sync.isPresent()) {
                return false;
            }

            if (_sync.get() instanceof Sync.Abstract) {
                if (!((Sync.Abstract) _sync.get()).setUp(getParams())) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Tears down what has been set up.
     */
    public void tearDown()
    {
        if (_sync.isPresent()) {
            if (_sync.get() instanceof Sync.Abstract) {
                ((Sync.Abstract) _sync.get()).tearDown();
            }

            _sync = Optional.empty();
        }
    }

    /** Sync element name. */
    public static final String ELEMENT_NAME = "Sync";

    /** Sync entity prefix. */
    public static final String ENTITY_PREFIX = "V";

    /** Sync entity reference name. */
    public static final String ENTITY_REFERENCE_NAME = "sync";

    private ClassDefEntity _classDef;
    private Optional<Sync> _sync;

    /**
     * Builder.
     */
    public static final class Builder
        extends ParamsEntity.Builder
    {
        /**
         * Constructs an instance.
         */
        Builder() {}

        /** {@inheritDoc}
         */
        @Override
        public SyncEntity build()
        {
            return new SyncEntity(
                getName(),
                getUUID(),
                getAttributes(),
                getTexts(),
                getParams(),
                Require.notNull(_classDef),
                Optional.ofNullable(_sync));
        }

        /**
         * Sets the classDef.
         *
         * @param classDef The classDef.
         *
         * @return This.
         */
        @Nonnull
        public Builder setClassDef(@Nonnull final ClassDefEntity classDef)
        {
            _classDef = Require.notNull(classDef);

            return this;
        }

        /**
         * Copies the values from an other sync entity.
         *
         * @param syncEntity The other sync entity.
         *
         * @return This.
         */
        @Nonnull
        protected Builder copyFrom(@Nonnull final SyncEntity syncEntity)
        {
            super.copyFrom(syncEntity);

            _classDef = syncEntity.getClassDef();
            _sync = syncEntity.getSync();

            return this;
        }

        private ClassDefEntity _classDef;
        private Sync _sync;
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
