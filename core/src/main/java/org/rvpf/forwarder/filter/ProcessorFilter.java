/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ProcessorFilter.java 4096 2019-06-24 23:07:39Z SFB $
 */

package org.rvpf.forwarder.filter;

import java.io.Serializable;

import java.util.Iterator;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.Point;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.exception.ValidationException;
import org.rvpf.base.logger.Messages;
import org.rvpf.base.store.Store;
import org.rvpf.base.sync.Sync;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.ValueConverter;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.RecalcTrigger;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.document.loader.MetadataFilter;
import org.rvpf.forwarder.ForwarderMessages;
import org.rvpf.forwarder.ForwarderModule;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.OriginEntity;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.metadata.entity.StoreEntity;
import org.rvpf.service.Service;
import org.rvpf.service.ServiceMessages;

/**
 * Processor filter.
 */
public final class ProcessorFilter
    extends ForwarderFilter.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    public Serializable[] filter(
            final Serializable message)
        throws InterruptedException, ServiceNotAvailableException
    {
        final PointValue pointValue = knownPointValue(message).orElse(null);

        if (pointValue == null) {
            return NO_MESSAGES;
        }

        if (pointValue instanceof RecalcTrigger) {
            final Point point = pointValue.getPoint().get();

            return point
                .getOrigin()
                .isPresent()? new Serializable[] {pointValue, }: NO_MESSAGES;
        }

        final Point point = pointValue.getPoint().get();

        if (point.getResults().isEmpty()) {
            return NO_MESSAGES;
        }

        final Optional<Sync> sync = point.getSync();
        final boolean inSync = !sync.isPresent()
                || sync.get().isInSync(pointValue.getStamp());
        final Store store = point.getStore().orElse(null);
        boolean dropped = !inSync;
        boolean mayConfirm = false;
        boolean mustConfirm = false;
        boolean confirmed = false;
        boolean rejected = false;

        if ((store != null) && store.canConfirm()) {
            mayConfirm = store
                .getParams()
                .getBoolean(Store.CONFIRM_PARAM, true);

            if (mayConfirm) {
                mayConfirm = point
                    .getParams()
                    .getBoolean(Point.CONFIRM_PARAM, true);
            }
        }

        if (mayConfirm) {
            if (_confirm == CONFIRM_ALWAYS) {
                mustConfirm = true;
            } else if (_confirm == CONFIRM_MARKED) {
                mustConfirm = store.getParams().getBoolean(Store.CONFIRM_PARAM);

                if (!mustConfirm) {
                    mustConfirm = point
                        .getParams()
                        .getBoolean(Point.CONFIRM_PARAM);
                }
            }
        }

        if (mustConfirm) {
            rejected = !pointValue.confirm(_confirmValue);
            confirmed = true;
        }

        if (!rejected) {
            if (!confirmed
                    && (_confirm == CONFIRM_REPLICATED)
                    && _replicates
                    && mayConfirm) {
                rejected = !pointValue.confirm(_confirmValue);
            }

            if (!rejected || !_replicates) {
                if (_resynchronizes) {
                    if (!inSync || _replicates) {
                        dropped = false;
                    }
                }
            }
        }

        if (rejected) {
            logDropped(ForwarderMessages.NOTICE_REJECTED, pointValue);
        }

        if (dropped) {
            if (point.getParams().getBoolean(Point.RESYNCHRONIZED_PARAM)) {
                getThisLogger()
                    .trace(ForwarderMessages.NOTICE_DROPPED, pointValue);
            } else {
                logDropped(ForwarderMessages.NOTICE_DROPPED, pointValue);
            }
        }

        return (rejected
                || dropped)? NO_MESSAGES: new Serializable[] {message, };
    }

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
    public boolean setUp(
            final ForwarderModule forwarderModule,
            final KeyedGroups filterProperties)
    {
        if (!super.setUp(forwarderModule, filterProperties)) {
            return false;
        }

        // Sets the confirm mode.

        final String confirm = filterProperties
            .getString(CONFIRM_PROPERTY, Optional.of(CONFIRM_NEVER_KEYWORD))
            .get();

        if (confirm.equalsIgnoreCase(CONFIRM_NEVER_KEYWORD)) {
            _confirm = CONFIRM_NEVER;
        } else if (confirm.equalsIgnoreCase(CONFIRM_REPLICATED_KEYWORD)) {
            _confirm = CONFIRM_REPLICATED;
        } else if (confirm.equalsIgnoreCase(CONFIRM_MARKED_KEYWORD)) {
            _confirm = CONFIRM_MARKED;
        } else if (confirm.equalsIgnoreCase(CONFIRM_ALWAYS_KEYWORD)) {
            _confirm = CONFIRM_ALWAYS;
        } else {
            _confirm = ValueConverter
                .convertToBoolean(
                    ServiceMessages.PROPERTY_TYPE.toString(),
                    CONFIRM_PROPERTY,
                    Optional.of(confirm),
                    false)? CONFIRM_ALWAYS: CONFIRM_NEVER;
        }

        if (_confirm != CONFIRM_NEVER) {
            _confirmValue = filterProperties.getBoolean(CONFIRM_VALUE_PROPERTY);
        }

        getThisLogger()
            .debug(
                ForwarderMessages.CONFIRM,
                _confirm,
                ValueConverter.toInteger(_confirmValue));

        // Gets the processor name.

        _processorName = filterProperties
            .getString(PROCESSOR_PROPERTY)
            .orElse(null);

        if (_processorName == null) {
            getThisLogger()
                .error(BaseMessages.MISSING_PROPERTY, PROCESSOR_PROPERTY);

            return false;
        }

        getThisLogger().debug(ServiceMessages.PROCESSOR_NAME, _processorName);

        // Gets the remaining properties.

        _resynchronizes = filterProperties.getBoolean(RESYNCHRONIZES_PROPERTY);
        getThisLogger()
            .debug(
                ForwarderMessages.RESYNCHRONIZES,
                Boolean.valueOf(_resynchronizes));
        _replicates = filterProperties.getBoolean(REPLICATES_PROPERTY);
        getThisLogger()
            .debug(ForwarderMessages.REPLICATES, Boolean.valueOf(_replicates));

        // Loads the metadata document.

        if (!loadMetadata(new _MetadataFilter())) {
            return false;
        }

        // Monitors stores.

        if (_confirm != CONFIRM_NEVER) {
            final Service service = getModule().getConfig().getService();

            for (final StoreEntity storeEntity:
                    getMetadata().getStoreEntities()) {
                service
                    .monitorService(
                        Optional.empty(),
                        storeEntity.getUUID(),
                        storeEntity.getName());
            }
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean onNewMetadata(final Metadata metadata)
    {
        // Makes sure the processor is known.

        if (!metadata
            .getOriginEntity(Optional.of(_processorName))
            .isPresent()) {
            getThisLogger()
                .error(ServiceMessages.PROCESSOR_NOT_FOUND, _processorName);

            return false;
        }

        // Sets up or prunes points.

        for (final Iterator<Point> i =
                metadata.getPointsCollection().iterator();
                i.hasNext(); ) {
            final PointEntity pointEntity = (PointEntity) i.next();

            if (pointEntity.getOriginEntity().isPresent()
                    || !pointEntity.getResults().isEmpty()) {
                if (!pointEntity.setUp(metadata)) {
                    return false;
                }
            } else {
                i.remove();
            }
        }

        metadata.cleanUp();

        return super.onNewMetadata(metadata);
    }

    /**
     * Gets the confirmation mode.
     *
     * @return A reference to the mode string.
     */
    @Nonnull
    @CheckReturnValue
    Messages.Entry _getConfirm()
    {
        return Require.notNull(_confirm);
    }

    /**
     * Gets the processor name.
     *
     * @return The processor name.
     */
    @Nonnull
    @CheckReturnValue
    String _getProcessorName()
    {
        return Require.notNull(_processorName);
    }

    public static final String CONFIRM_ALWAYS_KEYWORD = "always";
    public static final String CONFIRM_MARKED_KEYWORD = "marked";
    public static final String CONFIRM_NEVER_KEYWORD = "never";

    /** Specifies when a value existence is confirmed. */
    public static final String CONFIRM_PROPERTY = "confirm";

    /** Values are never confirmed. */
    public static final Messages.Entry CONFIRM_NEVER =
        ForwarderMessages.CONFIRM_NEVER;

    /** Values are confirmed when marked. */
    public static final Messages.Entry CONFIRM_MARKED =
        ForwarderMessages.CONFIRM_MARKED;

    /** Values are always confirmed. */
    public static final Messages.Entry CONFIRM_ALWAYS =
        ForwarderMessages.CONFIRM_ALWAYS;

    /** Values are confirmed when replicated. */
    public static final Messages.Entry CONFIRM_REPLICATED =
        ForwarderMessages.CONFIRM_REPLICATED;
    public static final String CONFIRM_REPLICATED_KEYWORD = "replicated";

    /** Specifies if a value is verified when confirmed. */
    public static final String CONFIRM_VALUE_PROPERTY = "confirm.value";

    /** Processor property. */
    public static final String PROCESSOR_PROPERTY = "processor";

    /** Replicates property. */
    public static final String REPLICATES_PROPERTY = "replicates";

    /** Resynchronizes property. */
    public static final String RESYNCHRONIZES_PROPERTY = "resynchronizes";

    private Messages.Entry _confirm;
    private boolean _confirmValue;
    private String _processorName;
    private boolean _replicates;
    private boolean _resynchronizes;

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
        public boolean areOriginsRequired()
        {
            return false;
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
        public boolean arePointInputsNeeded(
                final PointEntity pointEntity)
            throws ValidationException
        {
            final Optional<OriginEntity> originEntity = pointEntity
                .getOriginEntity();
            final boolean needed;

            if (originEntity.isPresent()) {
                needed = originEntity.get() == _processor;
            } else {
                needed = false;
            }

            return needed;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean areStoresNeeded()
        {
            return _getConfirm() != CONFIRM_NEVER;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isOriginNeeded(final OriginEntity originEntity)
        {
            final boolean needed = (_processor == null)
                    && _getProcessorName().equalsIgnoreCase(
                        originEntity.getName().orElse(null));

            if (needed) {
                _processor = originEntity;
            }

            return needed;
        }

        /** {@inheritDoc}
         */
        @Override
        public void reset()
        {
            _processor = null;

            super.reset();
        }

        /** {@inheritDoc}
         */
        @Override
        protected void includeOriginsXML(final XMLElement root)
        {
            root
                .addChild(ORIGINS_ELEMENT)
                .setAttribute(ORIGINS_ELEMENT, _getProcessorName());
        }

        private OriginEntity _processor;
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
