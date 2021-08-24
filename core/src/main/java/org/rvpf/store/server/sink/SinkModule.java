/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SinkModule.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.store.server.sink;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.exception.ServiceNotReadyException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.store.server.StoreMessages;

/**
 * Sink module.
 */
public interface SinkModule
{
    /**
     * Closes this sink.
     */
    void close();

    /**
     * Deletes a value.
     *
     * @param versionedValue The point value to delete.
     *
     * @return True if notification may proceed.
     */
    @CheckReturnValue
    boolean delete(@Nonnull VersionedValue versionedValue);

    /**
     * Opens this Sink.
     *
     * @throws ServiceNotReadyException When the service is not ready.
     */
    void open()
        throws ServiceNotReadyException;

    /**
     * Sets up the sink for action.
     *
     * @param sinkAppImpl The sink application implementation.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUp(@Nonnull SinkServiceAppImpl sinkAppImpl);

    /**
     * Tears down what has been set up.
     */
    void tearDown();

    /**
     * Updates a value.
     *
     * @param versionedValue The new point value.
     *
     * @return True if notification may proceed.
     */
    @CheckReturnValue
    boolean update(@Nonnull VersionedValue versionedValue);

    /**
     * Abstract sink module.
     */
    @NotThreadSafe
    abstract class Abstract
        implements SinkModule
    {
        /** {@inheritDoc}
         */
        @Override
        public void close() {}

        /** {@inheritDoc}
         */
        @Override
        public boolean delete(final VersionedValue versionedValue)
        {
            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public void open()
            throws ServiceNotReadyException {}

        /** {@inheritDoc}
         */
        @Override
        public boolean setUp(final SinkServiceAppImpl sinkAppImpl)
        {
            _sinkAppImpl = sinkAppImpl;

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public void tearDown()
        {
            _sinkAppImpl = null;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean update(final VersionedValue versionedValue)
        {
            return true;
        }

        /**
         * Aborts.
         */
        protected final void abort()
        {
            getThisLogger().warn(StoreMessages.ABORTING);
            getSinkAppImpl().getService().fail();
        }

        /**
         * Gets the metadata.
         *
         * @return The metadata.
         */
        protected final Metadata getMetadata()
        {
            return getSinkAppImpl().getService().getMetadata();
        }

        /**
         * Gets the sink application implementation.
         *
         * @return The sink application implementation.
         */
        protected final SinkServiceAppImpl getSinkAppImpl()
        {
            return _sinkAppImpl;
        }

        /**
         * Gets the logger.
         *
         * @return The logger.
         */
        protected final Logger getThisLogger()
        {
            if (_logger == null) {
                _logger = Logger.getInstance(getClass());
            }

            return _logger;
        }

        private Logger _logger;
        private SinkServiceAppImpl _sinkAppImpl;
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
