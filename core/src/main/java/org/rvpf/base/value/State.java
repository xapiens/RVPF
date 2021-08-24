/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: State.java 3995 2019-05-16 17:11:26Z SFB $
 */

package org.rvpf.base.value;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Logger;

/**
 * State.
 */
public final class State
    extends Number
    implements Externalizable
{
    /**
     * Constructs an instance.
     *
     * <p>This is needed for an Externalizable implementation.</p>
     */
    public State() {}

    /**
     * Constructs an instance.
     *
     * @param code The state code.
     * @param name The state name.
     */
    public State(
            @Nonnull final Optional<Integer> code,
            @Nonnull final Optional<String> name)
    {
        _code = code.orElse(null);
        _name = name.orElse(null);
    }

    /**
     * Returns a state from a string representation.
     *
     * @param string The string representation.
     *
     * @return A state.
     */
    @Nonnull
    @CheckReturnValue
    public static State fromString(String string)
    {
        final int split;
        final String codeString;
        final String name;
        Integer code;

        string = string.trim();
        split = string.indexOf(':');

        codeString = (split < 0)? string: ((split > 0)? string
            .substring(0, split): null);

        if (codeString != null) {
            try {
                code = Integer.valueOf(codeString);
            } catch (final NumberFormatException exceptione) {
                code = null;
            }
        } else {
            code = null;
            string = string.substring(1);
        }

        name = (code == null)? string: ((split >= 0)? string
            .substring(split + 1): "");

        return new State(
            Optional.ofNullable(code),
            (name.length() > 0)? Optional.of(name): Optional.empty());
    }

    /** {@inheritDoc}
     */
    @Override
    public double doubleValue()
    {
        return (_code != null)? _code.doubleValue(): 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (other instanceof State) {
            if (_code != null) {
                return _code.equals(((State) other)._code);
            } else if (_name != null) {
                return _name.equals(((State) other)._name);
            } else {
                return (((State) other)._code == null)
                       && (((State) other)._name == null);
            }
        }

        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public float floatValue()
    {
        return (_code != null)? _code.floatValue(): 0;
    }

    /**
     * Gets this state's code.
     *
     * @return This state's optional code.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Integer> getCode()
    {
        return Optional.ofNullable(_code);
    }

    /**
     * Gets this state's name.
     *
     * @return This state's optional name.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<String> getName()
    {
        return Optional.ofNullable(_name);
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return (_code != null)? _code
            .hashCode(): ((_name != null)? _name.hashCode(): 0);
    }

    /** {@inheritDoc}
     */
    @Override
    public int intValue()
    {
        return (_code != null)? _code.intValue(): 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public long longValue()
    {
        return (_code != null)? _code.longValue(): 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public void readExternal(final ObjectInput input)
        throws IOException
    {
        final State state = fromString(input.readUTF());

        _code = state.getCode().orElse(null);
        _name = state.getName().orElse(null);
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        final StringBuilder stringBuilder = new StringBuilder();

        if (_code != null) {
            stringBuilder.append(_code);
        }

        stringBuilder.append(':');

        if (_name != null) {
            stringBuilder.append(_name);
        }

        return stringBuilder.toString();
    }

    /** {@inheritDoc}
     */
    @Override
    public void writeExternal(final ObjectOutput output)
        throws IOException
    {
        output.writeUTF(toString());
    }

    private Integer _code;
    private String _name;

    /**
     * Group.
     */
    public static final class Group
        implements Externalizable
    {
        /**
         * Constructs an instance.
         *
         * <p>This is needed for an Externalizable implementation.</p>
         */
        public Group() {}

        /**
         * Constructs an instance.
         *
         * @param name The name of the state group.
         */
        public Group(@Nonnull final String name)
        {
            _name = name;
        }

        /**
         * Gets a state by its code.
         *
         * @param code The code.
         *
         * @return The optional state.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<State> get(final int code)
        {
            return get(Integer.valueOf(code));
        }

        /**
         * Gets a state by its code.
         *
         * @param code The code.
         *
         * @return The optional state.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<State> get(@Nonnull final Integer code)
        {
            return Optional.ofNullable(_statesByCode.get(code));
        }

        /**
         * Gets a state by its name.
         *
         * @param name The name.
         *
         * @return The optional state.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<State> get(@Nonnull final String name)
        {
            return Optional
                .ofNullable(
                    _statesByName.get(name.trim().toUpperCase(Locale.ROOT)));
        }

        /**
         * Gets the name of this group.
         *
         * @return The name of this group.
         */
        @Nonnull
        @CheckReturnValue
        public String getName()
        {
            return _name;
        }

        /**
         * Gets the states by code.
         *
         * @return The states by code.
         */
        @Nonnull
        @CheckReturnValue
        public List<State> getStatesByCode()
        {
            return new ArrayList<State>(_statesByCode.values());
        }

        /**
         * Gets the states by name.
         *
         * @return The states by name.
         */
        @Nonnull
        @CheckReturnValue
        public List<State> getStatesByName()
        {
            return new ArrayList<State>(_statesByName.values());
        }

        /**
         * Puts a state in this group.
         *
         * @param state The state.
         */
        public void put(@Nonnull final State state)
        {
            if (state.getName().isPresent()
                    && (_statesByName.put(
                        state.getName().get().trim().toUpperCase(Locale.ROOT),
                        state) != null)) {
                Logger
                    .getInstance(getClass())
                    .warn(
                        BaseMessages.DUPLICATE_STATE_NAME,
                        state.getName().get(),
                        getName());
            }

            if (state.getCode().isPresent()
                    && (_statesByCode.put(
                        state.getCode().get(),
                        state) != null)) {
                Logger
                    .getInstance(getClass())
                    .warn(
                        BaseMessages.DUPLICATE_STATE_CODE,
                        state.getCode().get(),
                        getName());
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public void readExternal(final ObjectInput input)
            throws IOException
        {
            _name = input.readUTF();

            for (int count = input.readInt(); count > 0; --count) {
                put(State.fromString(input.readUTF()));
            }
        }

        /**
         * Returns the number of states in this group.
         *
         * @return The number of states.
         */
        @CheckReturnValue
        public int size()
        {
            return _statesByName.size();
        }

        /** {@inheritDoc}
         */
        @Override
        public void writeExternal(final ObjectOutput output)
            throws IOException
        {
            output.writeUTF(_name);
            output.writeInt(_statesByName.size());

            for (final State state: _statesByName.values()) {
                output.writeUTF(state.toString());
            }
        }

        private String _name;
        private final Map<Integer, State> _statesByCode = new TreeMap<Integer,
            State>();
        private final Map<String, State> _statesByName = new TreeMap<String,
            State>();
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
