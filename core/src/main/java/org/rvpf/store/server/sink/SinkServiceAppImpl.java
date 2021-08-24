/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SinkServiceAppImpl.java 4097 2019-06-25 15:35:48Z SFB $
 */

package org.rvpf.store.server.sink;

import java.util.Collection;
import java.util.Optional;

import org.rvpf.base.ClassDef;
import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.exception.ServiceNotReadyException;
import org.rvpf.base.tool.Require;
import org.rvpf.document.loader.MetadataFilter;
import org.rvpf.metadata.entity.OriginEntity;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.store.server.StoreMessages;
import org.rvpf.store.server.StoreMetadataFilter;
import org.rvpf.store.server.StoreServiceAppImpl;

/**
 * Sink app implementation.
 */
public class SinkServiceAppImpl
    extends StoreServiceAppImpl
    implements SinkServiceApp
{
    /** {@inheritDoc}
     */
    @Override
    public OriginEntity getProcessor()
    {
        final MetadataFilter metadataFilter = getMetadata().getFilter();

        return (metadataFilter instanceof _StoreMetadataFilter)
               ? ((_StoreMetadataFilter) metadataFilter)
                   .getProcessor(): null;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getResponseLimit()
    {
        return 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public SinkServer getServer()
    {
        return Require.notNull(_server);
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
    {
        super.start();

        try {
            _sink.open();
        } catch (final ServiceNotReadyException exception) {
            throw new RuntimeException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        unregisterServer();

        if (_sink != null) {
            _sink.close();
        }

        super.stop();
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        stop();

        if (_server != null) {
            _server.tearDown();
            _server = null;
        }

        if (_sink != null) {
            _sink.tearDown();
            _sink = null;
        }

        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp()
    {
        if (!super.setUp(SERVER_PROPERTIES)) {
            return false;
        }

        // Sets up the notifier.

        if (!setUpNotifier()) {
            return false;
        }

        // Sets up the Sink instance.

        final ClassDef classDef = getServerProperties()
            .getClassDef(SINK_MODULE_CLASS_PROPERTY, DEFAULT_SINK_MODULE);

        _sink = classDef.createInstance(SinkModule.class);

        if (_sink == null) {
            return false;
        }

        getThisLogger().debug(StoreMessages.SINK, _sink.getClass().getName());

        if (!_sink.setUp(this)) {
            return false;
        }

        // Sets up and register the server.

        _server = new SinkServer();

        if (!_server.setUp(this, _sink)) {
            return false;
        }

        return registerServer(_server);
    }

    /** {@inheritDoc}
     */
    @Override
    protected StoreMetadataFilter storeMetadataFilter(
            final String storeName,
            final Collection<String> partnerNames)
    {
        final Optional<String> processorName = getServerProperties()
            .getString(PROCESSOR_PROPERTY);

        return processorName
            .isPresent()? new _StoreMetadataFilter(
                storeName,
                partnerNames,
                processorName.get()): super
                    .storeMetadataFilter(storeName, partnerNames);
    }

    /** Default sink module. */
    public static final ClassDef DEFAULT_SINK_MODULE = new ClassDefImpl(
        NullSink.class);

    /** Processor property. */
    public static final String PROCESSOR_PROPERTY = "processor";

    /** Server properties. */
    public static final String SERVER_PROPERTIES = "store.server.sink";

    /** Specifies a class implementing the sink module interface. */
    public static final String SINK_MODULE_CLASS_PROPERTY = "module.class";

    private SinkServer _server;
    private SinkModule _sink;

    /**
     * Sink store metadata filter.
     */
    private static final class _StoreMetadataFilter
        extends StoreMetadataFilter
    {
        /**
         * Constructs an instance.
         *
         * @param storeName The store name.
         * @param partnerNames The name of the store partners (optional).
         * @param processorName The processor name.
         */
        _StoreMetadataFilter(
                final String storeName,
                final Collection<String> partnerNames,
                final String processorName)
        {
            super(Optional.of(storeName), Optional.of(partnerNames));

            _processorName = processorName;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean arePointInputsNeeded()
        {
            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean arePointInputsNeeded(final PointEntity pointEntity)
        {
            return (_processor != null)
                   && (pointEntity.getOriginEntity().orElse(
                       null) == _processor);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean areStoresRequired()
        {
            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isOriginNeeded(final OriginEntity originEntity)
        {
            if (_processorName
                .equalsIgnoreCase(originEntity.getName().orElse(null))) {
                _processor = originEntity;
            }

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isPointTransformNeeded(final PointEntity pointEntity)
        {
            return (_processor != null)
                   && (pointEntity.getOriginEntity().orElse(
                       null) == _processor);
        }

        /** {@inheritDoc}
         */
        @Override
        public void reset()
        {
            _processor = null;

            super.reset();
        }

        /**
         * Gets the processor entity.
         *
         * @return The processor entity.
         */
        OriginEntity getProcessor()
        {
            return _processor;
        }

        private OriginEntity _processor;
        private final String _processorName;
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
