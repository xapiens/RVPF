/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Text.java 4078 2019-06-11 20:55:00Z SFB $
 */

package org.rvpf.metadata;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;

/**
 * Text.
 */
public final class Text
{
    /**
     * Constructs an instance.
     *
     * @param lang The text language.
     */
    public Text(@Nonnull final Optional<String> lang)
    {
        _lang = (lang
            .isPresent())? lang.get().trim().toLowerCase(Locale.ROOT): "";
    }

    /**
     * Adds some notes.
     *
     * @param notes The notes.
     */
    public void addNotes(@Nonnull final Optional<String> notes)
    {
        if (notes.isPresent()) {
            if (_notes == null) {
                _notes = new LinkedList<String>();
            }

            _notes.add(notes.get());
        }
    }

    /**
     * Adds some other informations.
     *
     * @param element The element identification.
     * @param text The text.
     */
    public void addOther(
            @Nonnull final String element,
            @Nonnull final String text)
    {
        if (_others == null) {
            _others = new LinkedList<Other>();
        }

        _others.add(new Other(element, text));
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (this == other) {
            return true;
        }

        if (other instanceof Text) {
            final Text otherText = (Text) other;

            if ((_ident == null)? (otherText._ident != null): (!_ident
                .equals(otherText._ident))) {
                return false;
            }

            if ((_title == null)? (otherText._title != null): (!_title
                .equals(otherText._title))) {
                return false;
            }

            if ((_description == null)
                    ? (otherText._description != null): (!_description
                        .equals(otherText._description))) {
                return false;
            }

            if (!getNotes().equals(otherText.getNotes())) {
                return false;
            }

            return getOthers().equals(otherText.getOthers());
        }

        return false;
    }

    /**
     * Gets the description.
     *
     * @return The optional description.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<String> getDescription()
    {
        return Optional.ofNullable(_description);
    }

    /**
     * Gets the ident.
     *
     * @return The optional ident.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<String> getIdent()
    {
        return Optional.ofNullable(_ident);
    }

    /**
     * Gets the lang.
     *
     * @return The lang.
     */
    @Nonnull
    @CheckReturnValue
    public String getLang()
    {
        return _lang;
    }

    /**
     * Gets the notes.
     *
     * @return The notes.
     */
    @Nonnull
    @CheckReturnValue
    public List<String> getNotes()
    {
        return (_notes != null)? _notes: Collections.<String>emptyList();
    }

    /**
     * Gets the other informations.
     *
     * @return The other informations.
     */
    @Nonnull
    @CheckReturnValue
    public List<Other> getOthers()
    {
        return (_others != null)? _others: Collections.<Other>emptyList();
    }

    /**
     * Gets the title.
     *
     * @return The optional title.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<String> getTitle()
    {
        return Optional.ofNullable(_title);
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Asks if all the text fields are empty.
     *
     * @return True if all the text fields are empty.
     */
    @CheckReturnValue
    public boolean isEmpty()
    {
        return (_ident == null)
               && (_title == null)
               && (_description == null)
               && (_notes == null)
               && (_others == null);
    }

    /**
     * Merges the informations from a previous instance.
     *
     * @param previous The previous instance.
     */
    public void merge(@Nonnull final Text previous)
    {
        if (_ident == null) {
            _ident = previous._ident;
        }

        if (_title == null) {
            _title = previous._title;
        }

        if (_description == null) {
            _description = previous._description;
        }

        if (previous._notes != null) {
            if (_notes == null) {
                _notes = previous._notes;
            } else {
                _notes.addAll(0, previous._notes);
            }
        }

        if (previous._others != null) {
            if (_others == null) {
                _others = previous._others;
            } else {
                _others.addAll(0, previous._others);
            }
        }
    }

    /**
     * Sets the title.
     *
     * @param description The optional title.
     */
    public void setDescription(final Optional<String> description)
    {
        if (description.isPresent()) {
            _description = description.get();
        }
    }

    /**
     * Sets the ident.
     *
     * @param ident The optional ident.
     */
    public void setIdent(final Optional<String> ident)
    {
        if (ident.isPresent()) {
            _ident = ident.get().trim();
        }
    }

    /**
     * Sets the title.
     *
     * @param title The optional title.
     */
    public void setTitle(final Optional<String> title)
    {
        if (title.isPresent()) {
            _title = title.get().trim();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return getClass()
            .getSimpleName() + "@" + Integer.toHexString(
                System.identityHashCode(this));
    }

    private String _description;
    private String _ident;
    private final String _lang;
    private List<String> _notes;
    private List<Other> _others;
    private String _title;

    /**
     * Other.
     */
    public static final class Other
    {
        /**
         * Constructs an instance.
         *
         * @param element The element identification.
         * @param text The optional text.
         */
        Other(@Nonnull final String element, @Nonnull final String text)
        {
            _element = Require.notNull(element);
            _text = Require.notNull(text);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean equals(final Object other)
        {
            if (this == other) {
                return true;
            }

            if (other instanceof Other) {
                final Other otherOther = (Other) other;

                if ((_element == null)
                        ? (otherOther._element != null): (!_element
                            .equals(otherOther._element))) {
                    return false;
                }

                return (_text != null)? _text
                    .equals(otherOther._text): (otherOther._text == null);
            }

            return false;
        }

        /**
         * Gets the element identification.
         *
         * @return Returns the element identification.
         */
        public String getElement()
        {
            return _element;
        }

        /**
         * Gets the text.
         *
         * @return Returns the text.
         */
        public String getText()
        {
            return _text;
        }

        /** {@inheritDoc}
         */
        @Override
        public int hashCode()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return getClass()
                .getSimpleName() + "@" + Integer.toHexString(
                    System.identityHashCode(this));
        }

        private final String _element;
        private final String _text;
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
