/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: HTTPModule.java 4096 2019-06-24 23:07:39Z SFB $
 */

package org.rvpf.http;

import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import javax.servlet.ServletContext;

import org.rvpf.base.UUID;
import org.rvpf.base.alert.Event;
import org.rvpf.base.alert.Signal;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.Config;
import org.rvpf.document.loader.MetadataDocumentLoader;
import org.rvpf.document.loader.MetadataFilter;
import org.rvpf.metadata.Metadata;
import org.rvpf.service.metadata.MetadataService;

/**
 * Server context module.
 */
public interface HTTPModule
{
    /**
     * Does event actions.
     *
     * @param event The event.
     */
    void doEventActions(@Nonnull Event event);

    /**
     * Does pending actions.
     */
    void doPendingActions();

    /**
     * Does signal actions.
     *
     * @param signal The signal.
     */
    void doSignalActions(@Nonnull Signal signal);

    /**
     * Gets the default path for this module.
     *
     * @return The default path.
     */
    @Nonnull
    @CheckReturnValue
    String getDefaultPath();

    /**
     * Asks if this module needs metadata.
     *
     * @return True if yes.
     */
    @CheckReturnValue
    boolean needsMetadata();

    /**
     * Prepares the servlet context.
     *
     * @param servletContext The servlet context.
     */
    void prepareServletContext(@Nonnull ServletContext servletContext);

    /**
     * Sets up this server module.
     *
     * @param httpApp The HTTP application.
     * @param servlets The servlets map.
     * @param contextProperties The context properties.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUp(
            @Nonnull HTTPServerAppImpl httpApp,
            @Nonnull Map<String, String> servlets,
            @Nonnull KeyedGroups contextProperties);

    /**
     * Tears down what has been set up.
     */
    void tearDown();

    /**
     * Abstract HTTP module.
     */
    @NotThreadSafe
    abstract class Abstract
        implements HTTPModule
    {
        /**
         * Requests a callback for event actions.
         */
        public final void callbackForEventActions()
        {
            _httpApp.callbackForEventActions(this);
        }

        /**
         * Requests a callback for pending actions.
         */
        public final void callbackForPendingActions()
        {
            _httpApp.callbackForPendingActions(this);
        }

        /**
         * Requests a callback for signal actions.
         */
        public final void callbackForSignalActions()
        {
            _httpApp.callbackForSignalActions(this);
        }

        /** {@inheritDoc}
         */
        @Override
        public void doEventActions(final Event event) {}

        /** {@inheritDoc}
         */
        @Override
        public void doPendingActions() {}

        /** {@inheritDoc}
         */
        @Override
        public void doSignalActions(final Signal signal) {}

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
        public void prepareServletContext(
                final ServletContext servletContext) {}

        /**
         * Sets the restart enabled indicator.
         *
         * @param restartEnabled The restart enabled indicator.
         *
         * @return The previous value of the indicator.
         */
        @CheckReturnValue
        public final boolean setRestartEnabled(final boolean restartEnabled)
        {
            return _httpApp.getService().setRestartEnabled(restartEnabled);
        }

        /** {@inheritDoc}
         */
        @Override
        public final boolean setUp(
                final HTTPServerAppImpl httpApp,
                final Map<String, String> servlets,
                final KeyedGroups contextProperties)
        {
            _httpApp = httpApp;

            final Optional<String> uuid = contextProperties
                .getString(MODULE_UUID_PROPERTY);

            if (uuid.isPresent()) {
                if (!UUID.isUUID(uuid.get())) {
                    getThisLogger()
                        .error(HTTPMessages.BAD_MODULE_UUID, uuid.get());

                    return false;
                }

                _uuid = UUID.fromString(uuid.get()).orElse(null);
            }

            if (!setUp(contextProperties)) {
                return false;
            }

            addServlets(servlets);

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public void tearDown()
        {
            _metadata = null;
        }

        /**
         * Adds the servlets to the servlets map.
         *
         * @param servlets The servlets map.
         */
        protected abstract void addServlets(
                @Nonnull final Map<String, String> servlets);

        /**
         * Gets the config.
         *
         * @return The config.
         */
        @Nonnull
        @CheckReturnValue
        protected final Config getConfig()
        {
            return _httpApp.getService().getConfig();
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
         * Gets the service.
         *
         * @return The service.
         */
        @Nonnull
        @CheckReturnValue
        protected final MetadataService getService()
        {
            return _httpApp.getService();
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
         * Loads the metadata.
         *
         * @param metadataFilter The metadata filter.
         *
         * @return True on success.
         */
        @CheckReturnValue
        protected final boolean loadMetadata(
                final MetadataFilter metadataFilter)
        {
            Require.success(needsMetadata());

            _metadata = MetadataDocumentLoader
                .fetchMetadata(
                    metadataFilter,
                    Optional.of(getConfig()),
                    Optional.ofNullable(_uuid),
                    Optional.empty());

            if (_metadata != null) {
                _metadata.setService(getService());
            }

            return _metadata != null;
        }

        /**
         * Sets up the server module.
         *
         * @param contextProperties The context properties.
         *
         * @return True on success.
         */
        @CheckReturnValue
        protected boolean setUp(@Nonnull final KeyedGroups contextProperties)
        {
            return true;
        }

        /**
         * The module UUID is needed when the module is a client of the
         * metadata server.
         */
        public static final String MODULE_UUID_PROPERTY = "module.uuid";

        private HTTPServerAppImpl _httpApp;
        private final Logger _logger = Logger.getInstance(getClass());
        private Metadata _metadata;
        private UUID _uuid;
    }


    /**
     * Context.
     */
    abstract class Context
    {
        /**
         * Gets the log ID.
         *
         * @return The log ID.
         */
        public final Optional<String> getLogID()
        {
            return Optional.ofNullable(_logID);
        }

        private final String _logID = Logger.currentLogID().orElse(null);
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
