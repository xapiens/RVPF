/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Proxied.java 4096 2019-06-24 23:07:39Z SFB $
 */

package org.rvpf.metadata;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.Params;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.metadata.entity.ProxyEntity;

/**
 * Proxied.
 *
 * <p>This interface is implemented by proxied metadata objects. These objects
 * are defined in the Metadata by reference to {@link org.rvpf.base.ClassDef}
 * entries. They contain the logic to be associated with the referring entity.
 * </p>
 */
public interface Proxied
{
    /**
     * Gets the proxy entity Name.
     *
     * @return The proxy entity Name.
     */
    @Nonnull
    @CheckReturnValue
    String getName();

    /**
     * Gets the proxy entity Params.
     *
     * @return The Params.
     */
    @Nonnull
    @CheckReturnValue
    Params getParams();

    /**
     * Gets the proxy entity.
     *
     * @return The proxy entity.
     */
    @Nonnull
    @CheckReturnValue
    ProxyEntity getProxyEntity();

    /**
     * Sets up the instance for action.
     *
     * @param metadata The metadata available to the current process.
     * @param proxyEntity The proxy entity refering to the proxied.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUp(@Nonnull Metadata metadata, @Nonnull ProxyEntity proxyEntity);

    /**
     * Tears down what has been set up.
     */
    void tearDown();

    /**
     * Abstract proxied.
     */
    @NotThreadSafe
    abstract class Abstract
        implements Proxied
    {
        /**
         * Gets the metadata.
         *
         * @return The metadata.
         */
        @Nonnull
        @CheckReturnValue
        public final Metadata getMetadata()
        {
            return Require.notNull(_metadata);
        }

        /** {@inheritDoc}
         */
        @Override
        public final String getName()
        {
            return _proxyEntity.getName().orElseGet(() -> getClass().getName());
        }

        /** {@inheritDoc}
         */
        @Override
        public final Params getParams()
        {
            return _proxyEntity.getParams();
        }

        /** {@inheritDoc}
         */
        @Override
        public final ProxyEntity getProxyEntity()
        {
            return _proxyEntity;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean setUp(
                final Metadata metadata,
                final ProxyEntity proxyEntity)
        {
            _metadata = metadata;
            _proxyEntity = proxyEntity;

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public void tearDown()
        {
            final ProxyEntity proxyEntity = _proxyEntity;

            if (proxyEntity != null) {
                proxyEntity.clearInstance();
                _proxyEntity = null;
            }

            _metadata = null;
        }

        /**
         * Gets the logger.
         *
         * @return The logger.
         */
        @Nonnull
        @CheckReturnValue
        protected Logger getThisLogger()
        {
            Logger logger = _logger;

            if (logger == null) {
                logger = Logger.getInstance(getClass());
                _logger = logger;
            }

            return logger;
        }

        private volatile Logger _logger;
        private Metadata _metadata;
        private ProxyEntity _proxyEntity;
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
