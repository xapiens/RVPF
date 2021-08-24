/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ValidatorDefEntity.java 3902 2019-02-20 22:30:12Z SFB $
 */

package org.rvpf.config.entity;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.UUID;

/**
 * ValidatorDef entity.
 */
public abstract class ValidatorDefEntity
    extends AbstractEntity
{
    /**
     * Constructs an instance.
     */
    protected ValidatorDefEntity() {}

    /**
     * Constructs an instance from an other.
     *
     * @param other The other instance.
     */
    protected ValidatorDefEntity(@Nonnull final ValidatorDefEntity other)
    {
        super(other);

        _hidden = other._hidden;
        _multiple = other._multiple;
    }

    /**
     * Constructs an instance.
     *
     * @param name The optional entity name.
     * @param uuid The optional entity UUID.
     * @param hidden True if hidden.
     * @param multiple True if multiple.
     */
    protected ValidatorDefEntity(
            @Nonnull final Optional<String> name,
            @Nonnull final Optional<UUID> uuid,
            final boolean hidden,
            final boolean multiple)
    {
        super(name, uuid);

        _hidden = hidden;
        _multiple = multiple;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (super.equals(other)) {
            final ValidatorDefEntity otherEntity = (ValidatorDefEntity) other;

            if (isHidden() != otherEntity.isHidden()) {
                return false;
            }

            return isMultiple() == otherEntity.isMultiple();
        }

        return false;
    }

    /**
     * Gets this validator's target.
     *
     * @return The validator's target.
     */
    @Nonnull
    @CheckReturnValue
    public abstract String getTarget();

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    /**
     * Gets the hidden indicator.
     *
     * @return The hidden indicator.
     */
    @CheckReturnValue
    public final boolean isHidden()
    {
        return _hidden;
    }

    /**
     * Asks if this validator allows multiple values.
     *
     * @return True when a refering item can have more than one value.
     */
    @CheckReturnValue
    public final boolean isMultiple()
    {
        return _multiple;
    }

    /**
     * Sets the hidden indicator.
     *
     * @param hidden The hidden indicator.
     */
    public final void setHidden(final boolean hidden)
    {
        _hidden = hidden;
    }

    /**
     * Remembers if this ValidatorDef allows multiple values.
     *
     * @param multiple True when a refering item can have more than one value.
     */
    public final void setMultiple(final boolean multiple)
    {
        _multiple = multiple;
    }

    private boolean _hidden;
    private boolean _multiple;

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
         * Asks if hidden.
         *
         * @return True if hidden.
         */
        @CheckReturnValue
        public final boolean isHidden()
        {
            return _hidden;
        }

        /**
         * Asks if multiple.
         *
         * @return True if multiple.
         */
        @CheckReturnValue
        public final boolean isMultiple()
        {
            return _multiple;
        }

        /**
         * Sets the hidden indicator.
         *
         * @param hidden The hidden indicator.
         *
         * @return This.
         */
        @Nonnull
        public final Builder setHidden(final boolean hidden)
        {
            _hidden = hidden;

            return this;
        }

        /**
         * Remembers if this ValidatorDef allows multiple values.
         *
         * @param multiple True when a refering item can have more than one value.
         *
         * @return This.
         */
        @Nonnull
        public final Builder setMultiple(final boolean multiple)
        {
            _multiple = multiple;

            return this;
        }

        /**
         * Copies the values from an other ValidatorDefEntity.
         *
         * @param validatorDefEntity The other ValidatorDefEntity.
         *
         * @return This.
         */
        @Nonnull
        protected Builder copyFrom(
                @Nonnull final ValidatorDefEntity validatorDefEntity)
        {
            super.copyFrom(validatorDefEntity);

            _hidden = validatorDefEntity.isHidden();
            _multiple = validatorDefEntity.isMultiple();

            return this;
        }

        private boolean _hidden;
        private boolean _multiple;
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
