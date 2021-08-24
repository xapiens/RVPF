/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ProxyEntity.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.metadata.entity;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Params;
import org.rvpf.base.UUID;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.UndefinedEntityException;
import org.rvpf.config.entity.ClassDefEntity;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.Proxied;
import org.rvpf.metadata.Text;

/**
 * Proxy entity.
 *
 * <p>Each instance of a subclass keeps a reference to an implementation of the
 * interface associated with the type of entity that it represents.</p>
 */
public abstract class ProxyEntity
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
     * @param classDef The optional class definition.
     * @param instance The optional proxied instance.
     */
    protected ProxyEntity(
            @Nonnull final Optional<String> name,
            @Nonnull final Optional<UUID> uuid,
            @Nonnull final Optional<KeyedGroups> attributes,
            @Nonnull final Optional<Map<String, Text>> texts,
            @Nonnull final Optional<Params> params,
            @Nonnull final Optional<ClassDefEntity> classDef,
            @Nonnull final Optional<Proxied> instance)
    {
        super(name, uuid, attributes, texts, params);

        _classDef = Require.notNull(classDef);
        _instance = Require.notNull(instance);

    }

    /**
     * Clears the proxied instance.
     */
    public final void clearInstance()
    {
        _instance = null;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (super.equals(other)) {
            final ProxyEntity otherEntity = (ProxyEntity) other;

            return Objects.equals(_classDef, otherEntity._classDef);
        }

        return false;
    }

    /**
     * Gets the class definition.
     *
     * @return The optional class definition.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<ClassDefEntity> getClassDef()
    {
        return _classDef;
    }

    /**
     * Gets a proxy for the supplied instance.
     *
     * @param instance The instance to be proxied.
     *
     * @return The proxy.
     */
    @Nonnull
    @CheckReturnValue
    public final ProxyEntity getProxy(@Nonnull final Proxied instance)
    {
        final ProxyEntity proxy;

        if (_instance.isPresent() && (instance == _instance.get())) {
            proxy = this;
        } else {
            proxy = (ProxyEntity) copy();
            proxy.setInstance(instance);
        }

        return proxy;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    /**
     * Asks if the proxied entity implements an interface.
     *
     * @param classObject A class object.
     *
     * @return True if the interface is implemented.
     *
     * @throws UndefinedEntityException When the interface is undefined.
     */
    @CheckReturnValue
    public final boolean is(
            final Class<?> classObject)
        throws UndefinedEntityException
    {
        if (!_classDef.isPresent()) {
            return false;
        }

        return _classDef.get().is(classObject);
    }

    /**
     * Asks if the proxied entity implements an interface.
     *
     * @param className The name of the class or interface.
     *
     * @return True if the interface is implemented.
     *
     * @throws UndefinedEntityException When the interface is undefined.
     */
    @CheckReturnValue
    public final boolean is(
            final String className)
        throws UndefinedEntityException
    {
        if (!_classDef.isPresent()) {
            return false;
        }

        return _classDef.get().is(className);
    }

    /**
     * Sets the proxied instance.
     *
     * @param instance The proxied instance.
     */
    public final void setInstance(@Nonnull final Proxied instance)
    {
        _instance = Optional.of((instance));
    }

    /**
     * Sets up the proxied object.
     *
     * <p>Note: this method must be prepared to be called redundantly. Such
     * calls happen during the normal setup of the points in a processor.</p>
     *
     * @param metadata The metadata.
     *
     * @return True if successful.
     */
    @CheckReturnValue
    public boolean setUp(@Nonnull final Metadata metadata)
    {
        if (!_instance.isPresent()) {
            final Proxied instance;

            if (_classDef.isPresent()) {
                instance = _classDef.get().createInstance(Proxied.class);
            } else {
                instance = createDefaultInstance();
            }

            if ((instance == null) || !instance.setUp(metadata, this)) {
                return false;
            }

            setInstance(instance);
        }

        return true;
    }

    /**
     * Tears down what has been set up.
     */
    public void tearDown()
    {
        if (_instance.isPresent()) {
            _instance.get().tearDown();
            _instance = Optional.empty();
        }
    }

    /**
     * Creates a default instance.
     *
     * <p>This must be overriden by the subclass when the 'classDef' attribute
     * is optional; otherwise, it will always fail.</p>
     *
     * @return A new instance or fails.
     */
    @Nonnull
    @CheckReturnValue
    protected Proxied createDefaultInstance()
    {
        throw Require.failure();
    }

    /**
     * Gets the proxied instance.
     *
     * @return The optional proxied instance.
     */
    @Nonnull
    @CheckReturnValue
    protected final Optional<? extends Proxied> getInstance()
    {
        return _instance;
    }

    private final Optional<ClassDefEntity> _classDef;
    private Optional<Proxied> _instance;

    /**
     * Builder.
     */
    public abstract static class Builder
        extends ParamsEntity.Builder
    {
        /**
         * Constructs an instance.
         */
        protected Builder() {}

        /**
         * Sets the classDef.
         *
         * @param classDef The optional classDef.
         *
         * @return This.
         */
        @Nonnull
        public Builder setClassDef(
                @Nonnull final Optional<ClassDefEntity> classDef)
        {
            _classDef = classDef;

            return this;
        }

        /**
         * Sets the instance.
         *
         * @param instance The instance.
         *
         * @return This.
         */
        @Nonnull
        public Builder setInstance(@Nonnull final Proxied instance)
        {
            _instance = Optional.of(instance);

            return this;
        }

        /**
         * Copies the values from an entity.
         *
         * @param entity The entity.
         *
         * @return This.
         */
        @Nonnull
        protected Builder copyFrom(@Nonnull final ProxyEntity entity)
        {
            super.copyFrom(entity);

            _classDef = entity.getClassDef();

            return this;
        }

        /**
         * Gets the classDef.
         *
         * @return The optional classDef.
         */
        protected Optional<ClassDefEntity> getClassDef()
        {
            return _classDef;
        }

        /**
         * Gets the instance.
         *
         * @return The optional instance.
         */
        protected Optional<Proxied> getInstance()
        {
            return _instance;
        }

        private Optional<ClassDefEntity> _classDef = Optional.empty();
        private Optional<Proxied> _instance = Optional.empty();
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
