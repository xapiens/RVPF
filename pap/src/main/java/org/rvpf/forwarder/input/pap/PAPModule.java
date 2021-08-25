/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.forwarder.input.pap;

import java.io.Serializable;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.document.loader.MetadataFilter;
import org.rvpf.forwarder.BatchControl;
import org.rvpf.forwarder.input.InputModule;
import org.rvpf.metadata.entity.OriginEntity;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.pap.PAP;
import org.rvpf.pap.PAPContext;
import org.rvpf.pap.PAPServer;
import org.rvpf.pap.PAPSupport;

/**
 * PAP module.
 */
public abstract class PAPModule
    extends InputModule
{
    /** {@inheritDoc}
     */
    @Override
    public boolean needsMetadata()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onMetadataRefreshed()
    {
        if (!super.onMetadataRefreshed() || !loadMetadata()) {
            return false;
        }

        final PAPSupport support = newSupport();
        final PAPContext serverContext = support
            .newServerContext(
                getMetadata(),
                _originNames,
                Optional.of(getTraces()));

        ((PAPInput) getInput()).replaceServer(support.newServer(serverContext));

        return true;
    }

    /**
     * Loads the metadata.
     *
     * @return True on success.
     */
    @CheckReturnValue
    protected boolean loadMetadata()
    {
        return loadMetadata(new _MetadataFilter());
    }

    /**
     * Returns a new PAP input for a PAP server.
     *
     * @param server The PAP server.
     *
     * @return The new PAP input.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract PAPInput newInput(@Nonnull PAPServer server);

    /**
     * Returns a new support instance.
     *
     * @return The new support instance.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract PAPSupport newSupport();

    /**
     * Returns protocol properties.
     *
     * @param moduleProperties The module properties.
     *
     * @return The protocol properties.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract KeyedGroups protocolProperties(
            @Nonnull KeyedGroups moduleProperties);

    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp(final KeyedGroups moduleProperties)
    {
        if (!loadMetadata()) {
            return false;
        }

        final KeyedGroups protocolProperties = protocolProperties(
            moduleProperties);

        _originNames = protocolProperties.getStrings(PAP.ORIGIN_PROPERTY);

        final PAPSupport support = newSupport();
        final PAPContext serverContext = support
            .newServerContext(
                getMetadata(),
                _originNames,
                Optional.of(getTraces()));
        final PAPServer server = (serverContext != null)? support
            .newServer(serverContext): null;

        if ((server == null) || !server.setUp(protocolProperties)) {
            return false;
        }

        setInput(newInput(server));

        return super.setUp(moduleProperties);
    }

    /**
     * Returns the attributes usage.
     *
     * @return The attributes usage.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract String usage();

    private String[] _originNames;

    /**
     * PAP input.
     */
    protected abstract class PAPInput
        extends AbstractInput
    {
        /**
         * Constructs an instance.
         *
         * @param server The server.
         */
        protected PAPInput(@Nonnull final PAPServer server)
        {
            _server = server;
        }

        /** {@inheritDoc}
         */
        @Override
        public void close()
        {
            if (_closed.compareAndSet(false, true)) {
                _server.stop();
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<Serializable[]> input(
                final BatchControl batchControl)
            throws InterruptedException
        {
            final int limit = batchControl.getLimit();
            final List<PointValue> pointValues = new LinkedList<>();
            long timeout = -1;

            batchControl.reset();

            for (;;) {
                final PointValue pointValue = getServer()
                    .nextUpdate(timeout)
                    .orElse(null);

                if (pointValue == null) {
                    break;
                }

                pointValues.add(pointValue);

                if (pointValues.size() >= limit) {
                    break;
                }

                final Optional<ElapsedTime> wait = batchControl.getWait();

                if (wait.isPresent()) {
                    timeout = wait.get().toMillis();
                }
            }

            return Optional
                .of(pointValues.toArray(new PointValue[pointValues.size()]));
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isClosed()
        {
            return _closed.get();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean open()
        {
            _closed.set(false);
            _server.start();

            return true;
        }

        /**
         * Replaces the server.
         *
         * @param server The new server.
         */
        public void replaceServer(@Nonnull final PAPServer server)
        {
            close();
            _server = server;
            open();
        }

        /** {@inheritDoc}
         */
        @Override
        public void tearDown()
        {
            close();

            super.tearDown();
        }

        /**
         * Gets the server.
         *
         * @return The server.
         */
        @Nonnull
        @CheckReturnValue
        protected PAPServer getServer()
        {
            return _server;
        }

        private final AtomicBoolean _closed = new AtomicBoolean();
        private volatile PAPServer _server;
    }


    /**
     * Metadata filter.
     */
    private final class _MetadataFilter
        extends MetadataFilter
    {
        /**
         * Constructs an instance.
         */
        _MetadataFilter()
        {
            super(false);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean areAttributesNeeded()
        {
            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean areAttributesNeeded(final String usage)
        {
            return usage().equalsIgnoreCase(usage);
        }

        /** {@inheritDoc}
         */
        @Override
        public final boolean areContentsNeeded()
        {
            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean areOriginsFiltered()
        {
            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean areOriginsNeeded()
        {
            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean arePointsNeeded()
        {
            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isOriginNeeded(final OriginEntity originEntity)
        {
            return originEntity.getAttributes(usage()).isPresent();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isPointNeeded(final PointEntity pointEntity)
        {
            return pointEntity.getAttributes(usage()).isPresent();
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
