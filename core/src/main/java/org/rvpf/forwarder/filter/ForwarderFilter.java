/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ForwarderFilter.java 4096 2019-06-24 23:07:39Z SFB $
 */

package org.rvpf.forwarder.filter;

import java.io.Serializable;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.UUID;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Messages;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.document.loader.MetadataDocumentLoader;
import org.rvpf.document.loader.MetadataFilter;
import org.rvpf.forwarder.ForwarderMessages;
import org.rvpf.forwarder.ForwarderModule;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.PointEntity;

/**
 * Forwarder filter.
 */
public interface ForwarderFilter
{
    /**
     * Filters a message.
     *
     * @param message The original message.
     *
     * @return The filtered messages.
     *
     * @throws InterruptedException When the service is stopped.
     * @throws ServiceNotAvailableException When the service is not available.
     */
    @Nonnull
    @CheckReturnValue
    Serializable[] filter(
            @Nonnull Serializable message)
        throws InterruptedException, ServiceNotAvailableException;

    /**
     * Asks if this filter needs metadata.
     *
     * @return True if yes.
     */
    @CheckReturnValue
    boolean needsMetadata();

    /**
     * Called when the metadata should be refreshed.
     *
     * @return False if a restart is needed.
     */
    @CheckReturnValue
    boolean onMetadataRefreshed();

    /**
     * Sets up this filter.
     *
     * @param forwarderModule The owner of this filter.
     * @param filterProperties The filter properties.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUp(
            @Nonnull ForwarderModule forwarderModule,
            @Nonnull KeyedGroups filterProperties);

    /**
     * Tears down what has been set up.
     */
    void tearDown();

    /**
     * Abstract filter.
     */
    @NotThreadSafe
    abstract class Abstract
        implements ForwarderFilter
    {
        /** {@inheritDoc}
         */
        @Override
        public boolean needsMetadata()
        {
            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean onMetadataRefreshed()
        {
            return !needsMetadata();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean setUp(
                final ForwarderModule forwarderModule,
                final KeyedGroups filterProperties)
        {
            _module = forwarderModule;

            final Optional<String> uuidString = filterProperties
                .getString(FILTER_UUID_PROPERTY);

            if (uuidString.isPresent()) {
                if (!UUID.isUUID(uuidString.get())) {
                    getThisLogger()
                        .error(
                            ForwarderMessages.BAD_FILTER_UUID,
                            uuidString.get());

                    return false;
                }

                _uuid = UUID.fromString(uuidString.get());
                getThisLogger().debug(ForwarderMessages.FILTER_UUID, _uuid);
            }

            _warnDropped = filterProperties.getBoolean(WARN_DROPPED_PROPERTY);

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public void tearDown()
        {
            _module = null;
        }

        /**
         * Gets the metadata.
         *
         * @return The metadata.
         */
        @Nonnull
        @CheckReturnValue
        protected final Metadata getMetadata()
        {
            return Require.notNull(_metadata);
        }

        /**
         * Gets the owner module;
         *
         * @return The owner module.
         */
        @Nonnull
        @CheckReturnValue
        protected final ForwarderModule getModule()
        {
            return Require.notNull(_module);
        }

        /**
         * Gets the logger.
         *
         * @return The logger.
         */
        @Nonnull
        @CheckReturnValue
        protected final Logger getThisLogger()
        {
            return _logger;
        }

        /**
         * Gets the UUID.
         *
         * @return The optional UUID.
         */
        @Nonnull
        @CheckReturnValue
        protected Optional<UUID> getUUID()
        {
            return _uuid;
        }

        /**
         * Returns the message as a known point value.
         *
         * @param message The message.
         *
         * @return The known point value (empty if unknown).
         */
        @Nonnull
        @CheckReturnValue
        protected final Optional<PointValue> knownPointValue(
                @Nonnull final Serializable message)
        {
            PointValue pointValue;

            if (message instanceof PointValue) {
                pointValue = (PointValue) message;

                if (pointValue
                    .getPoint()
                    .orElse(null) instanceof PointEntity.Definition) {
                    pointValue = pointValue.reset();
                }

                pointValue = pointValue.restore(getMetadata());

                if (!pointValue.getPoint().isPresent()) {
                    logDropped(
                        ForwarderMessages.DROPPED_UNKNOWN_POINT,
                        pointValue);
                    pointValue = null;
                }
            } else {
                logDropped(
                    ForwarderMessages.DROPPED_NOT_POINT_VALUE,
                    message.getClass());
                pointValue = null;
            }

            return Optional.ofNullable(pointValue);
        }

        /**
         * Loads the metadata.
         *
         * @param metadataFilter The metadata filter.
         *
         * @return True on success.
         */
        @CheckReturnValue
        protected final boolean loadMetadata(
                @Nonnull final MetadataFilter metadataFilter)
        {
            Require.success(needsMetadata());

            Metadata metadata = MetadataDocumentLoader
                .fetchMetadata(
                    metadataFilter,
                    Optional.of(getModule().getConfig()),
                    _uuid,
                    Optional.empty());

            if (metadata != null) {
                if (onNewMetadata(metadata)) {
                    metadata.setService(getModule().getService());
                } else {
                    metadata = null;
                }
            }

            _metadata = metadata;

            return _metadata != null;
        }

        /**
         * Logs a dropped message.
         *
         * @param entry The messages entry.
         * @param params The message parameters.
         */
        protected void logDropped(
                @Nonnull final Messages.Entry entry,
                @Nonnull final Object... params)
        {
            if (_warnDropped) {
                getThisLogger().warn(entry, params);
            } else {
                getThisLogger().trace(entry, params);
            }
        }

        /**
         * Called on new metadata.
         *
         * @param metadata The new metadata.
         *
         * @return False to reject the metadata.
         */
        @CheckReturnValue
        protected boolean onNewMetadata(@Nonnull final Metadata metadata)
        {
            return true;
        }

        /**
         * A UUID for the filter. This is needed when the filter is a client of
         * the metadata server. It is also used as a marker by the alert filter.
         */
        public static final String FILTER_UUID_PROPERTY = "filter.uuid";

        /** Warn dropped property. */
        public static final String WARN_DROPPED_PROPERTY = "warn.dropped";

        /** No messages. */
        protected static final Serializable[] NO_MESSAGES = new Serializable[0];

        private final Logger _logger = Logger.getInstance(getClass());
        private Metadata _metadata;
        private ForwarderModule _module;
        private Optional<UUID> _uuid = Optional.empty();
        private boolean _warnDropped;
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
