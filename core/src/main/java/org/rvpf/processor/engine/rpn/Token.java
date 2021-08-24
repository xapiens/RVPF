/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Token.java 4017 2019-05-22 20:22:12Z SFB $
 */

package org.rvpf.processor.engine.rpn;

import java.io.Serializable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * Token.
 */
abstract class Token
{
    /**
     * Constructs an instance.
     *
     * @param token The token's text.
     */
    Token(@Nonnull final String token)
    {
        _token = token;
    }

    /** {@inheritDoc}
     */
    @Override
    public final String toString()
    {
        return _token;
    }

    /**
     * Asks if this represents a constant.
     *
     * @return True if this represents a constant.
     */
    boolean isConstant()
    {
        return false;
    }

    /**
     * Asks if this represents an other name.
     *
     * @return True if this represents an other name.
     */
    boolean isOtherName()
    {
        return false;
    }

    /**
     * Asks if this represents a variable action name.
     *
     * @return True if this represents a variable action name.
     */
    boolean isVariableActionName()
    {
        return false;
    }

    static final OtherName COMMA = new OtherName(",");
    static final OtherName RIGHT_PAREN = new OtherName(")");

    private final String _token;

    /**
     * Constant.
     */
    abstract static class Constant
        extends Token
    {
        /**
         * Constructs an instance.
         *
         * @param token The constant text.
         * @param value The constant value.
         */
        Constant(@Nonnull final String token, @Nonnull final Serializable value)
        {
            super(token);

            _value = value;
        }

        /**
         * Gets the value.
         *
         * @return The value.
         */
        @Nonnull
        @CheckReturnValue
        Serializable getValue()
        {
            return _value;
        }

        /** {@inheritDoc}
         */
        @Override
        boolean isConstant()
        {
            return true;
        }

        private final Serializable _value;
    }


    /**
     * Numeric constant.
     */
    static final class NumericConstant
        extends Constant
    {
        /**
         * Constructs an instance.
         *
         * @param token The numeric constant text.
         * @param number The converted number.
         */
        NumericConstant(
                @Nonnull final String token,
                @Nonnull final Number number)
        {
            super(token, number);
        }
    }


    /**
     * Other name.
     */
    static final class OtherName
        extends Token
    {
        /**
         * Constructs an insrtance.
         *
         * @param token The token text.
         */
        OtherName(@Nonnull final String token)
        {
            super(token);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean equals(final Object other)
        {
            return (other instanceof OtherName)
                   && toString().equals(other.toString());
        }

        /** {@inheritDoc}
         */
        @Override
        public int hashCode()
        {
            return toString().hashCode();
        }

        /** {@inheritDoc}
         */
        @Override
        boolean isOtherName()
        {
            return true;
        }
    }


    /**
     * Quoted.
     */
    static final class TextConstant
        extends Constant
    {
        /**
         * Constructs an instance.
         *
         * @param token The quoted text.
         * @param unquoted The unquoted text.
         */
        TextConstant(
                @Nonnull final String token,
                @Nonnull final String unquoted)
        {
            super(token, unquoted);
        }
    }


    /**
     * Variable action name.
     */
    static final class VariableActionName
        extends Token
    {
        /**
         * Constructs an instance.
         *
         * @param token The token text.
         * @param dup The 'dup' indicator.
         * @param type The variable type.
         * @param identifier The variable identifier.
         * @param action The action.
         */
        VariableActionName(
                @Nonnull final String token,
                final boolean dup,
                final char type,
                @Nonnull final Integer identifier,
                final char action)
        {
            super(token);

            _dup = dup;
            _type = type;
            _identifier = identifier;
            _action = action;
        }

        /**
         * Gets the action.
         *
         * @return The action.
         */
        char getAction()
        {
            return _action;
        }

        /**
         * Gets the 'dup' indicator.
         *
         * @return The 'dup' indicator.
         */
        boolean getDup()
        {
            return _dup;
        }

        /**
         * Gets the identifier.
         *
         * @return The identifier.
         */
        Integer getIdentifier()
        {
            return _identifier;
        }

        /**
         * Gets the type.
         *
         * @return The type.
         */
        char getType()
        {
            return _type;
        }

        /** {@inheritDoc}
         */
        @Override
        boolean isVariableActionName()
        {
            return true;
        }

        private final char _action;
        private final boolean _dup;
        private final Integer _identifier;
        private final char _type;
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
