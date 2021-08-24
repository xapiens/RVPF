/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MetadataEntity.java 4076 2019-06-11 17:09:30Z SFB $
 */

package org.rvpf.metadata.entity;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Attributes;
import org.rvpf.base.Entity;
import org.rvpf.base.UUID;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.entity.AbstractEntity;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.Text;

/**
 * Metadata entity.
 */
public abstract class MetadataEntity
    extends AbstractEntity
{
    /**
     * Constructs an instance.
     */
    protected MetadataEntity()
    {
        _attributes = Optional.empty();
        _texts = Optional.empty();
    }

    /**
     * Constructs an instance from an other.
     *
     * @param other The other instance.
     */
    protected MetadataEntity(@Nonnull final MetadataEntity other)
    {
        super(other);

        if (other._attributes.isPresent()) {
            _attributes = Optional.of(other._attributes.get().copy());
        } else {
            _attributes = Optional.empty();
        }

        if (other._texts.isPresent()) {
            _texts = Optional.of(new TreeMap<>(other._texts.get()));
        } else {
            _texts = Optional.empty();
        }
    }

    /**
     * Constructs an instance.
     *
     * @param name The optional entity name.
     * @param uuid The optional entity UUID.
     * @param attributes The optional attributes.
     * @param texts The optional texts.
     */
    protected MetadataEntity(
            @Nonnull final Optional<String> name,
            @Nonnull final Optional<UUID> uuid,
            @Nonnull final Optional<KeyedGroups> attributes,
            @Nonnull final Optional<Map<String, Text>> texts)
    {
        super(name, uuid);

        _attributes = Require.notNull(attributes);
        _texts = Require.notNull(texts);
    }

    /**
     * Adds text.
     *
     * @param text The text.
     */
    public final void addText(@Nonnull Text text)
    {
        if (!_texts.isPresent()) {
            _texts = Optional.of(new TreeMap<String, Text>());
        }

        text = _texts.get().put(text.getLang(), text);

        if (text != null) {
            _texts.get().get(text.getLang()).merge(text);
        }
    }

    /** {@inheritDoc}
     *
     * <p>For two {@link Metadata} {@link Entity} to be equal, they must at
     * least be instances of the same class, have the same {@link UUID},
     * {@link Attributes}, name and descriptive texts.</p>
     */
    @Override
    public boolean equals(final Object other)
    {
        if (super.equals(other)) {
            final MetadataEntity otherEntity = (MetadataEntity) other;

            if (!Objects.equals(_attributes, otherEntity._attributes)) {
                return false;
            }

            return Objects.equals(_texts, otherEntity._texts);
        }

        return false;
    }

    /**
     * Gets the attributes.
     *
     * @return The optional attributes.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<KeyedGroups> getAttributes()
    {
        return _attributes;
    }

    /** {@inheritDoc}
     */
    @Override
    public final Optional<Attributes> getAttributes(final String usage)
    {
        final Optional<Attributes> attributes;

        if (_attributes.isPresent()) {
            attributes = Optional
                .ofNullable(
                    (Attributes) _attributes
                        .get()
                        .getObject(usage.toUpperCase(Locale.ROOT)));
        } else {
            attributes = Optional.empty();
        }

        return attributes;
    }

    /**
     * Gets the texts.
     *
     * @return The texts.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<Map<String, Text>> getTexts()
    {
        return _texts;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    /**
     * Sets attributes.
     *
     * @param attributes The optional attributes.
     */
    public final void setAttributes(
            @Nonnull final Optional<KeyedGroups> attributes)
    {
        _attributes = Require.notNull(attributes);
    }

    private Optional<KeyedGroups> _attributes;
    private Optional<Map<String, Text>> _texts;

    /**
     * Builder.
     */
    public abstract static class Builder
        extends AbstractEntity.Builder
    {
        /**
         * Constructs an instance.
         */
        protected Builder() {}

        /**
         * Adds text.
         *
         * @param text The text.
         *
         * @return This.
         */
        @Nonnull
        public final Builder addText(@Nonnull Text text)
        {
            if (!_texts.isPresent()) {
                _texts = Optional.of(new TreeMap<String, Text>());
            }

            text = _texts.get().put(text.getLang(), text);

            if (text != null) {
                _texts.get().get(text.getLang()).merge(text);
            }

            return this;
        }

        /**
         * Sets attributes.
         *
         * @param attributes The optional attributes.
         *
         * @return This.
         */
        @Nonnull
        public final Builder setAttributes(
                @Nonnull final Optional<KeyedGroups> attributes)
        {
            _attributes = attributes;

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
        protected Builder copyFrom(@Nonnull final MetadataEntity entity)
        {
            super.copyFrom(entity);

            _attributes = entity.getAttributes();
            _texts = entity.getTexts();

            return this;
        }

        /**
         * Gets the attributes.
         *
         * @return The attributes.
         */
        @Nonnull
        @CheckReturnValue
        protected final Optional<KeyedGroups> getAttributes()
        {
            return _attributes;
        }

        /**
         * Gets the text.
         *
         * @return The text.
         */
        @Nonnull
        @CheckReturnValue
        protected final Optional<Map<String, Text>> getTexts()
        {
            return _texts;
        }

        private Optional<KeyedGroups> _attributes = Optional.empty();
        private Optional<Map<String, Text>> _texts = Optional.empty();
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
