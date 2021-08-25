/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Token.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.jdbc;

import java.io.Serializable;

import java.sql.SQLException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.DateTime;
import org.rvpf.base.tool.Require;

/**
 * Abstract Token.
 */
abstract class Token
{
    /**
     * Constructs an instance.
     *
     * @param token The token text.
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
     * Returns the word for a supplied token.
     *
     * @param token The token text.
     *
     * @return The word.
     */
    @Nonnull
    @CheckReturnValue
    static Word wordFor(final String token)
    {
        final Word word;

        word = RESERVED_REGISTRY.get(token.toUpperCase(Locale.ROOT));

        return (word != null)? word: new Word(token);
    }

    /**
     * Gets the value.
     *
     * @return The value.
     */
    @Nullable
    @CheckReturnValue
    Serializable getValue()
    {
        return null;
    }

    /**
     * Asks if this represents a numeric value.
     *
     * @return True if this represents a numeric value.
     */
    @CheckReturnValue
    boolean isNumeric()
    {
        return false;
    }

    /**
     * Asks if this represents a parameter value.
     *
     * @return True if this represents a parameter value.
     */
    @CheckReturnValue
    boolean isParameter()
    {
        return false;
    }

    /**
     * Asks if this represents a quoted text.
     *
     * @return True if this represents a quoted text.
     */
    @CheckReturnValue
    boolean isQuoted()
    {
        return false;
    }

    /**
     * Asks if this represents a reserved token.
     *
     * @return True if this represents a reserved token.
     */
    @CheckReturnValue
    boolean isReserved()
    {
        return false;
    }

    /**
     * Asks if this represents a value.
     *
     * @return True if this represents a value.
     */
    @CheckReturnValue
    boolean isValue()
    {
        return false;
    }

    /**
     * Asks if this represents a word.
     *
     * @return True if this represents a word.
     */
    @CheckReturnValue
    boolean isWord()
    {
        return false;
    }

    /**
     * Gets the unquoted token text.
     *
     * @return The unquoted token text.
     */
    @Nonnull
    @CheckReturnValue
    String unquoted()
    {
        return _token;
    }

    static final Map<String, Word> RESERVED_REGISTRY = new HashMap<String,
        Word>();
    static final Token REGEXP = new Word.Reserved("REGEXP");
    static final Token PERIOD = new Word.Reserved(".");
    static final Token OR = new Word.Reserved("OR");
    static final Token NULL = new Word.Reserved("NULL");
    static final Token NOW = new Word.Reserved("NOW");
    static final Token NOT = new Word.Reserved("NOT");
    static final Token LT = new Word.Reserved("<");
    static final Token LIMIT = new Word.Reserved("LIMIT");
    static final Token LIKE = new Word.Reserved("LIKE");
    static final Token LEFT = new Word.Reserved("(");
    static final Token LE = new Word.Reserved("<=");
    static final Token IS = new Word.Reserved("IS");
    static final Token INTO = new Word.Reserved("INTO");
    static final Token INSERT = new Word.Reserved("INSERT");
    static final Token GT = new Word.Reserved(">");
    static final Token GE = new Word.Reserved(">=");
    static final Token FROM = new Word.Reserved("FROM");
    static final Token EQ = new Word.Reserved("=");
    static final Token EOT = new Word.Reserved("EOT");
    static final Token DELETE = new Word.Reserved("DELETE");
    static final Token COMMA = new Word.Reserved(",");
    static final Token BOT = new Word.Reserved("BOT");
    static final Token ASTERISK = new Word.Reserved("*");
    static final Token AS = new Word.Reserved("AS");
    static final Token AND = new Word.Reserved("AND");
    static final Token ALL = new Word.Reserved("ALL");
    static final Token RIGHT = new Word.Reserved(")");
    static final Token SELECT = new Word.Reserved("SELECT");
    static final Token SET = new Word.Reserved("SET");
    static final Token TODAY = new Word.Reserved("TODAY");
    static final Token UPDATE = new Word.Reserved("UPDATE");
    static final Token VALUES = new Word.Reserved("VALUES");
    static final Token WHERE = new Word.Reserved("WHERE");
    static final Token YESTERDAY = new Word.Reserved("YESTERDAY");

    private final String _token;

    /**
     * Numeric.
     */
    static final class Numeric
        extends Value
    {
        /**
         * Constructs an instance.
         *
         * @param token The numeric text.
         * @param number The wrapped number.
         */
        Numeric(@Nonnull final String token, @Nonnull final Number number)
        {
            super(token, number);
        }

        /** {@inheritDoc}
         */
        @Override
        Number getValue()
        {
            return (Number) super.getValue();
        }

        /** {@inheritDoc}
         */
        @Override
        boolean isNumeric()
        {
            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        void setValue(final Serializable value)
        {
            Require.failure();
        }
    }


    /**
     * Parameter.
     */
    static final class Parameter
        extends Value
    {
        /**
         * Constructs an instance.
         */
        Parameter()
        {
            super("?", null);
        }

        /**
         * Constructs an instance from an other.
         *
         * @param original The other instance.
         */
        Parameter(@Nonnull final Parameter original)
        {
            super(original.toString(), original.getValue());
        }

        /** {@inheritDoc}
         */
        @Override
        boolean isParameter()
        {
            return true;
        }
    }


    /**
     * Quoted.
     */
    static final class Quoted
        extends Value
    {
        /**
         * Constructs an instance.
         *
         * @param token The quoted text.
         * @param unquoted The quoted value.
         */
        Quoted(@Nonnull final String token, @Nonnull final String unquoted)
        {
            super(token, unquoted);
        }

        /** {@inheritDoc}
         */
        @Override
        String getValue()
        {
            return (String) super.getValue();
        }

        /** {@inheritDoc}
         */
        @Override
        boolean isQuoted()
        {
            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        void setValue(final Serializable value)
        {
            Require.failure();
        }

        /** {@inheritDoc}
         */
        @Override
        String unquoted()
        {
            return (String) super.getValue();
        }
    }


    /**
     * Constant.
     */
    static class Value
        extends Token
    {
        /**
         * Constructs an instance.
         *
         * @param token The constant text.
         * @param value The constant value.
         */
        Value(@Nonnull final String token, @Nullable final Serializable value)
        {
            super(token);

            _value = value;
        }

        /**
         * Gets a date-time from this token.
         *
         * @return The date-time.
         *
         * @throws SQLException On failure.
         */
        final DateTime getDateTime()
            throws SQLException
        {
            if (_value instanceof DateTime) {
                return (DateTime) _value;
            }

            if (_value instanceof String) {
                try {
                    return DateTime
                        .fromString(Optional.of((String) _value))
                        .orElse(null);
                } catch (final IllegalArgumentException exception) {
                    // Falls to throw.
                }
            }

            throw JDBCMessages.NOT_A_STAMP.exception(_value);
        }

        /**
         * Gets a number from this token.
         *
         * @return The number.
         *
         * @throws SQLException On failure.
         */
        final Number getNumber()
            throws SQLException
        {
            if (_value instanceof Number) {
                return (Number) _value;
            }

            if (_value instanceof String) {
                try {
                    return Long.decode((String) _value);
                } catch (final NumberFormatException exception1) {
                    try {
                        return Double.valueOf((String) _value);
                    } catch (final NumberFormatException exception2) {
                        // Falls to throw.
                    }
                }
            }

            throw JDBCMessages.NOT_A_NUMBER.exception(_value);
        }

        /** {@inheritDoc}
         */
        @Override
        Serializable getValue()
        {
            return _value;
        }

        /** {@inheritDoc}
         */
        @Override
        final boolean isValue()
        {
            return true;
        }

        /**
         * Sets the value.
         *
         * @param value The value.
         */
        void setValue(final Serializable value)
        {
            _value = value;
        }

        private Serializable _value;
    }


    /**
     * Operation word.
     */
    static class Word
        extends Token
    {
        /**
         * Constructs an instance.
         *
         * @param token The word text.
         */
        Word(final String token)
        {
            super(token);
        }

        /** {@inheritDoc}
         */
        @Override
        final boolean isWord()
        {
            return true;
        }

        /**
         * Reserved.
         */
        static final class Reserved
            extends Word
        {
            /**
             * Constructs an instance.
             *
             * @param token The reserved token.
             */
            Reserved(final String token)
            {
                super(token);

                RESERVED_REGISTRY.put(toString(), this);
            }

            /** {@inheritDoc}
             */
            @Override
            boolean isReserved()
            {
                return true;
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
