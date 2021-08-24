/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ForwarderModule.java 4096 2019-06-24 23:07:39Z SFB $
 */

package org.rvpf.forwarder;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.ClassDef;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.UUID;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.Traces;
import org.rvpf.base.util.SnoozeAlarm;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.Config;
import org.rvpf.document.loader.MetadataDocumentLoader;
import org.rvpf.document.loader.MetadataFilter;
import org.rvpf.forwarder.filter.ForwarderFilter;
import org.rvpf.metadata.Metadata;
import org.rvpf.service.Service;
import org.rvpf.service.ServiceMessages;

/**
 * Forwarder module.
 */
public interface ForwarderModule
{
    /**
     * Gets the batch control.
     *
     * @return The batch control.
     */
    @Nonnull
    @CheckReturnValue
    BatchControl getBatchControl();

    /**
     * Gets the configuration.
     *
     * @return The configuration.
     */
    @Nonnull
    @CheckReturnValue
    Config getConfig();

    /**
     * Gets the owner of this module.
     *
     * @return The owner of this module.
     */
    @Nonnull
    @CheckReturnValue
    Service getService();

    /**
     * Asks if the module is reliable.
     *
     * @return True if the module is reliable.
     */
    @CheckReturnValue
    boolean isReliable();

    /**
     * Asks if this module needs metadata.
     *
     * @return True if it does.
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
     * Sets up this module.
     *
     * @param forwarderApp The owner of this module.
     * @param moduleProperties The module properties.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUp(
            @Nonnull ForwarderServiceAppImpl forwarderApp,
            @Nonnull KeyedGroups moduleProperties);

    /**
     * Starts this module.
     *
     * @throws InterruptedException When appropriate.
     * @throws ServiceNotAvailableException When the service is not available.
     */
    void start()
        throws InterruptedException, ServiceNotAvailableException;

    /**
     * Stops this module.
     */
    void stop();

    /**
     * Tears down what has been set up.
     */
    void tearDown();

    /**
     * Abstract forwarder module.
     */
    @NotThreadSafe
    abstract class Abstract
        implements ForwarderModule
    {
        /** {@inheritDoc}
         */
        @Override
        public final BatchControl getBatchControl()
        {
            return Require.notNull(_batchControl);
        }

        /** {@inheritDoc}
         */
        @Override
        public final Config getConfig()
        {
            return _forwarderAppImpl.getConfig();
        }

        /**
         * Gets the configuration properties.
         *
         * @return The configuration properties.
         */
        @Nonnull
        @CheckReturnValue
        public final KeyedGroups getConfigProperties()
        {
            return _forwarderAppImpl.getConfigProperties();
        }

        /** {@inheritDoc}
         */
        @Override
        public final Service getService()
        {
            return _forwarderAppImpl.getService();
        }

        /**
         * Gets the logger.
         *
         * @return The logger.
         */
        @Nonnull
        @CheckReturnValue
        public final Logger getThisLogger()
        {
            return _logger;
        }

        /**
         * Gets the traces.
         *
         * @return The traces.
         */
        @Nonnull
        @CheckReturnValue
        public final Traces getTraces()
        {
            return _traces;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isReliable()
        {
            return _reliable.isPresent()? _reliable.get().booleanValue(): false;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean needsMetadata()
        {
            for (final ForwarderFilter filter: _filters) {
                if (filter.needsMetadata()) {
                    return true;
                }
            }

            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean onMetadataRefreshed()
        {
            for (final ForwarderFilter filter: _filters) {
                if (!filter.onMetadataRefreshed()) {
                    return false;
                }
            }

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public final boolean setUp(
                final ForwarderServiceAppImpl forwarderApp,
                final KeyedGroups moduleProperties)
        {
            _forwarderAppImpl = forwarderApp;

            final Optional<String> uuid = moduleProperties
                .getString(MODULE_UUID_PROPERTY);

            if (uuid.isPresent()) {
                if (!UUID.isUUID(uuid.get())) {
                    getThisLogger()
                        .error(ForwarderMessages.BAD_MODULE_UUID, uuid.get());

                    return false;
                }

                _uuid = UUID.fromString(uuid.get()).get();
                getThisLogger().debug(ForwarderMessages.MODULE_UUID, _uuid);
            }

            _batchControl = BatchControl
                .newBuilder()
                .setDefaultLimit(getDefaultBatchLimit())
                .setDefaultWait(getDefaultBatchWait())
                .setLogger(getThisLogger())
                .applyProperties(moduleProperties)
                .build();

            _connectionRetryDelay = moduleProperties
                .getElapsed(
                    CONNECTION_RETRY_DELAY_PROPERTY,
                    Optional.of(DEFAULT_CONNECTION_RETRY_DELAY),
                    Optional.of(DEFAULT_CONNECTION_RETRY_DELAY))
                .orElse(null);
            getThisLogger()
                .debug(
                    ServiceMessages.CONNECTION_RETRY_DELAY,
                    _connectionRetryDelay);

            if (!SnoozeAlarm
                .validate(
                    _connectionRetryDelay,
                    this,
                    ServiceMessages.CONNECTION_RETRY_DELAY_TEXT)) {
                return false;
            }

            if (!_traces
                .setUp(
                    _forwarderAppImpl.getDataDir(),
                    getConfigProperties().getGroup(Traces.TRACES_PROPERTIES),
                    _forwarderAppImpl.getSourceUUID(),
                    moduleProperties.getString(TRACES_PROPERTY))) {
                return false;
            }

            for (final KeyedGroups filterProperties:
                    moduleProperties.getGroups(FILTER_PROPERTIES)) {
                final Optional<ClassDef> classDef = filterProperties
                    .getClassDef(FILTER_CLASS_PROPERTY, Optional.empty());

                if (!classDef.isPresent()) {
                    getThisLogger()
                        .error(
                            ForwarderMessages.FILTER_PROPERTY_MISSING,
                            FILTER_CLASS_PROPERTY);

                    return false;
                }

                final ForwarderFilter filter = classDef
                    .get()
                    .createInstance(ForwarderFilter.class);

                if (filter == null) {
                    return false;
                }

                _filters.add(filter);

                if (!filter.setUp(this, filterProperties)) {
                    return false;
                }
            }

            _reliable = moduleProperties
                .getBoolean(RELIABLE_PROPERTY, Optional.empty());

            return setUp(moduleProperties);
        }

        /** {@inheritDoc}
         */
        @Override
        public void tearDown()
        {
            for (final ForwarderFilter filter: _filters) {
                filter.tearDown();
            }

            _traces.tearDown();

            _forwarderAppImpl = null;
        }

        /**
         * Filters messages.
         *
         * @param messages The messages to be filtered.
         *
         * @return The filtered messages.
         *
         * @throws InterruptedException When the service is stopped.
         * @throws ServiceNotAvailableException When the service is not
         *                                      available.
         */
        @Nonnull
        @CheckReturnValue
        protected final Serializable[] filter(
                @Nonnull final Serializable[] messages)
            throws InterruptedException, ServiceNotAvailableException
        {
            if (_filters.isEmpty() || (messages.length == 0)) {
                return messages;
            }

            final List<Serializable> messageList = new ArrayList<Serializable>(
                messages.length);

            for (Serializable message: messages) {
                if (message == null) {
                    continue;
                }

                final List<Serializable> filteredMessages = new LinkedList<>();

                filteredMessages.add(message);

                for (final ForwarderFilter filter: _filters) {
                    final ListIterator<Serializable> iterator = filteredMessages
                        .listIterator();

                    while (iterator.hasNext()) {
                        final Serializable[] filtered = filter
                            .filter(iterator.next());

                        iterator.remove();

                        for (final Serializable serializable: filtered) {
                            iterator.add(serializable);
                        }
                    }
                }

                messageList.addAll(filteredMessages);
            }

            return messageList.toArray(new Serializable[messageList.size()]);
        }

        /**
         * Gets the service connection retry delay.
         *
         * @return The service connection retry delay.
         */
        @Nonnull
        @CheckReturnValue
        protected final ElapsedTime getConnectionRetryDelay()
        {
            return Require.notNull(_connectionRetryDelay);
        }

        /**
         * Gets the default batch limit.
         *
         * @return The default batch limit.
         */
        @CheckReturnValue
        protected abstract int getDefaultBatchLimit();

        /**
         * Gets the default batch wait.
         *
         * @return The optional default batch wait.
         */
        @Nonnull
        @CheckReturnValue
        protected Optional<ElapsedTime> getDefaultBatchWait()
        {
            return _DEFAULT_BATCH_WAIT;
        }

        /**
         * Gets the owner of this module.
         *
         * @return The owner of this module.
         */
        @Nonnull
        @CheckReturnValue
        protected final ForwarderServiceAppImpl getForwarderAppImpl()
        {
            return Require.notNull(_forwarderAppImpl);
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
         * Gets the reliable indicator.
         *
         * @return The optional reliable indicator.
         */
        @Nonnull
        @CheckReturnValue
        protected Optional<Boolean> getReliable()
        {
            return _reliable;
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

            _metadata = MetadataDocumentLoader
                .fetchMetadata(
                    metadataFilter,
                    Optional.of(getConfig()),
                    Optional.ofNullable(_uuid),
                    Optional.empty());

            if (_metadata != null) {
                _metadata.setService(_forwarderAppImpl.getService());
            }

            return _metadata != null;
        }

        /**
         * Sets the reliable indicator.
         *
         * @param reliable The reliable indicator.
         */
        protected void setReliable(@Nonnull final Boolean reliable)
        {
            _reliable = Optional.of(reliable);
        }

        /**
         * Sets up this module.
         *
         * @param moduleProperties The module properties.
         *
         * @return True on success.
         */
        @CheckReturnValue
        protected abstract boolean setUp(@Nonnull KeyedGroups moduleProperties);

        /** Specifies the maximum number of messages in a transaction. */
        public static final String BATCH_LIMIT_PROPERTY = "batch.limit";

        /** The time in millis before trying to reconnect to the service. */
        public static final String CONNECTION_RETRY_DELAY_PROPERTY =
            "connection.retry.delay";

        /** Default connection retry delay. */
        public static final ElapsedTime DEFAULT_CONNECTION_RETRY_DELAY =
            ElapsedTime
                .fromMillis(60000);

        /** The filter class property. */
        public static final String FILTER_CLASS_PROPERTY = "filter.class";

        /** The filter properties group. */
        public static final String FILTER_PROPERTIES = "filter";

        /**
         * A UUID for the module. This is needed when the module is a client of
         * the metadata server.
         */
        public static final String MODULE_UUID_PROPERTY = "module.uuid";

        /** Password property. */
        public static final String PASSWORD_PROPERTY = "password";

        /** Reliable property. */
        public static final String RELIABLE_PROPERTY = "reliable";

        /** The names of the subdirectory holding traces for this module. */
        public static final String TRACES_PROPERTY = "traces";

        /** User property. */
        public static final String USER_PROPERTY = "user";

        /**  */

        private static final Optional<ElapsedTime> _DEFAULT_BATCH_WAIT =
            Optional
                .empty();

        private BatchControl _batchControl;
        private ElapsedTime _connectionRetryDelay;
        private final List<ForwarderFilter> _filters = new LinkedList<>();
        private ForwarderServiceAppImpl _forwarderAppImpl;
        private final Logger _logger = Logger.getInstance(getClass());
        private Metadata _metadata;
        private Optional<Boolean> _reliable = Optional.empty();
        private final Traces _traces = new Traces();
        private UUID _uuid;

        /**
         * Module input/output.
         */
        protected interface ModuleInputOutput
        {
            /**
             * Closes this module.
             */
            void close();

            /**
             * Confirms the processing of the messages.
             *
             * @return True on success.
             */
            @CheckReturnValue
            boolean commit();

            /**
             * Gets a display name for this module.
             *
             * @return The display name.
             */
            @Nonnull
            @CheckReturnValue
            String getDisplayName();

            /**
             * Asks if this module is closed.
             *
             * @return True when this module is closed.
             */
            @CheckReturnValue
            boolean isClosed();

            /**
             * Asks if this module is reliable.
             *
             * @return True when this module is reliable.
             */
            @CheckReturnValue
            boolean isReliable();

            /**
             * Opens this module.
             *
             * @return True on success.
             *
             * @throws InterruptedException When the service is stopped.
             */
            @CheckReturnValue
            boolean open()
                throws InterruptedException;

            /**
             * Sets up this module.
             *
             * @param moduleProperties The module properties.
             *
             * @return True on success.
             */
            @CheckReturnValue
            boolean setUp(@Nonnull KeyedGroups moduleProperties);

            /**
             * Tears down what has been set up.
             */
            void tearDown();
        }


        /**
         * Abstract input/output.
         */
        protected abstract static class AbstractInputOutput
            implements ModuleInputOutput
        {
            /** {@inheritDoc}
             */
            @Override
            public boolean commit()
            {
                return true;
            }

            /** {@inheritDoc}
             */
            @Override
            public boolean isReliable()
            {
                return false;
            }

            /** {@inheritDoc}
             */
            @Override
            public boolean setUp(final KeyedGroups moduleProperties)
            {
                return true;
            }

            /** {@inheritDoc}
             */
            @Override
            public void tearDown() {}
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
