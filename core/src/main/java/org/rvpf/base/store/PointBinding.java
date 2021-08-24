/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PointBinding.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.base.store;

import java.io.Serializable;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.UUID;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.ValueConverter;

/**
 * Point binding.
 *
 * <p>Instances of this class are created by a store to hold point binding
 * informations sent as response to a query encapsulated by the {@link Request}
 * nested class.</p>
 *
 * <p>Note: the 'server UUID' field is meaningful only to the server who has
 * created the instance and does not need to survive serialization.</p>
 */
@ThreadSafe
public final class PointBinding
    implements Serializable, Comparable<PointBinding>
{
    /**
     * Constructs a point binding.
     *
     * @param name The point's name.
     * @param clientUUID The point's UUID (client).
     * @param serverUUID The optional point's UUID (server).
     */
    public PointBinding(
            @Nonnull final String name,
            @Nonnull final UUID clientUUID,
            @Nonnull final Optional<UUID> serverUUID)
    {
        _name = Require.notNull(name);
        _clientUUID = clientUUID.toBytes();
        _serverUUID = serverUUID.isPresent()? serverUUID.get().toBytes(): null;
    }

    /** {@inheritDoc}
     */
    @Override
    public int compareTo(final PointBinding other)
    {
        int comparison;

        comparison = compareBytes(_serverUUID, other._serverUUID);

        if (comparison == 0) {
            comparison = compareBytes(_clientUUID, other._clientUUID);
        }

        return comparison;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (this == other) {
            return true;
        }

        if (other instanceof PointBinding) {
            return Arrays.equals(
                _serverUUID,
                ((PointBinding) other)._serverUUID)
                   && Arrays.equals(
                       _clientUUID,
                       ((PointBinding) other)._clientUUID);
        }

        return false;
    }

    /**
     * Gets the point's name.
     *
     * @return The point's name.
     */
    @Nonnull
    @CheckReturnValue
    public String getName()
    {
        return _name;
    }

    /**
     * Gets the point's UUID (server).
     *
     * @return The point's UUID.
     */
    @Nonnull
    @CheckReturnValue
    public UUID getServerUUID()
    {
        return UUID.fromBytes((_serverUUID != null)? _serverUUID: _clientUUID);
    }

    /**
     * Gets the point's UUID (client).
     *
     * @return The point's UUID.
     */
    @Nonnull
    @CheckReturnValue
    public UUID getUUID()
    {
        return UUID.fromBytes(_clientUUID);
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return Arrays.hashCode(_serverUUID) ^ Arrays.hashCode(_clientUUID);
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return getName() + "[" + getUUID() + ":" + getServerUUID() + "]";
    }

    private static int compareBytes(final byte[] left, final byte[] right)
    {
        if (left == null) {
            return (right == null)? 0: -1;
        } else if (right == null) {
            return 1;
        }

        int comparison = 0;

        for (int i = 0; i < left.length; ++i) {
            comparison = left[i] - right[i];

            if (comparison != 0) {
                break;
            }
        }

        return comparison;
    }

    private static final long serialVersionUID = 1L;

    private final byte[] _clientUUID;
    private final String _name;
    private final transient byte[] _serverUUID;

    /**
     * Request.
     *
     * <p>This class encapsulates either a query for point definitions
     * informations or a bindings request.</p>
     */
    @NotThreadSafe
    public static final class Request
        implements Serializable
    {
        /**
         * Constructs an instance.
         *
         * @param selection The selection object.
         * @param uuid The UUID.
         */
        Request(final Object selection, final UUID uuid)
        {
            _selection = selection;
            _uuid = uuid;
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

        /**
         * Gets the client UUID.
         *
         * @return The  optional client UUID.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<UUID> getClientUUID()
        {
            return Optional.ofNullable(_uuid);
        }

        /**
         * Gets the selection name.
         *
         * @return The optional selection name.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<String> getSelectionName()
        {
            return (_selection instanceof String)? Optional
                .of((String) _selection): Optional.empty();
        }

        /**
         * Gets the selection pattern.
         *
         * <p>If none has been assigned, defaults to a match all pattern.</p>
         *
         * @return The optional selection pattern.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<Pattern> getSelectionPattern()
        {
            if (_selection == null) {
                return Optional.of(Pattern.compile(".*"));
            }

            return (_selection instanceof Pattern)? Optional
                .of((Pattern) _selection): Optional.empty();
        }

        /**
         * Gets the selection UUID.
         *
         * @return The optional selection UUID.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<UUID> getSelectionUUID()
        {
            return (_selection instanceof UUID)? Optional
                .of((UUID) _selection): Optional.empty();
        }

        /**
         * Gets the selection.
         *
         * @return The selection.
         */
        Object _getSelection()
        {
            return _selection;
        }

        /**
         * Gets the uuid.
         *
         * @return The uuid.
         */
        UUID _getUUID()
        {
            return _uuid;
        }

        private static final long serialVersionUID = 1L;

        private final Object _selection;
        private final UUID _uuid;

        /**
         * Point binding request builder.
         */
        public static final class Builder
        {
            /**
             * Constructs an instance.
             */
            Builder() {}

            /**
             * Asks to bind the name to a UUID.
             *
             * @param uuid The UUID.
             *
             * @return This.
             */
            public Builder bindTo(@Nonnull final UUID uuid)
            {
                Require.success(_selection instanceof String);

                _uuid = uuid;

                return this;
            }

            /**
             * Builds a request.
             *
             * @return The request.
             */
            public Request build()
            {
                return new Request(_selection, _uuid);
            }

            /**
             * Copies the values from a request.
             *
             * @param request The request.
             *
             * @return This
             */
            @Nonnull
            public Builder copyFrom(@Nonnull final Request request)
            {
                _selection = request._getSelection();
                _uuid = request._getUUID();

                return this;
            }

            /**
             * Selects the point.
             *
             * @param name A point name.
             *
             * @return This.
             */
            public Builder selectName(@Nonnull final String name)
            {
                _selection = name;

                return this;
            }

            /**
             * Selects the points.
             *
             * @param pattern A compiled regular expression.
             *
             * @return This.
             */
            public Builder selectPattern(@Nonnull final Pattern pattern)
            {
                Require.success(_uuid == null);

                _selection = pattern;

                return this;
            }

            /**
             * Selects the point.
             *
             * @param uuid A point UUID.
             *
             * @return This.
             */
            public Builder selectUUID(@Nonnull final UUID uuid)
            {
                Require.success(_uuid == null);

                _selection = uuid;

                return this;
            }

            /**
             * Selects the points.
             *
             * @param wild A simple wild match pattern.
             *
             * @return This.
             */
            public Builder selectWild(@Nonnull final String wild)
            {
                selectPattern(ValueConverter.wildToPattern(wild));

                return this;
            }

            private Object _selection;
            private UUID _uuid;
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
