/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Replicator.java 4039 2019-05-31 17:53:15Z SFB $
 */

package org.rvpf.store.server;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Point;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.Traces;
import org.rvpf.base.util.container.IdentityHashSet;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.util.container.ListHashMap;
import org.rvpf.base.util.container.ListMap;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ReplicatedValue;
import org.rvpf.base.value.filter.ValueFilter;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.metadata.entity.StoreEntity;
import org.rvpf.service.Service;
import org.rvpf.service.ServiceMessages;

/**
 * Replicator.
 */
public interface Replicator
{
    /**
     * Closes.
     */
    void close();

    /**
     * Commits the pending replicates.
     *
     * @throws InterruptedException When the service is stopped.
     */
    void commit()
        throws InterruptedException;

    /**
     * Gets the partner names.
     *
     * @return The partner names.
     */
    @Nonnull
    @CheckReturnValue
    Collection<String> getPartnerNames();

    /**
     * Joins whatever thread set up by this replicator.
     *
     * @return True value on success.
     */
    boolean join();

    /**
     * Replicates of a point value.
     *
     * @param pointValue The point value.
     *
     * @throws InterruptedException When the Service is stopped.
     */
    void replicate(@Nonnull PointValue pointValue)
        throws InterruptedException;

    /**
     * Sets up for processing.
     *
     * @param storeAppImpl The store application implementation.
     *
     * @return True if successful.
     */
    @CheckReturnValue
    boolean setUp(@Nonnull StoreServiceAppImpl storeAppImpl);

    /**
     * Sets up the implementation for processing.
     *
     * @return True if successful.
     */
    @CheckReturnValue
    boolean setUpImpl();

    /**
     * Starts this replicator.
     */
    void start();

    /**
     * Tears down what has been set up.
     */
    void tearDown();

    /**
     * Uses new metadata.
     */
    void useNewMetadata();

    /**
     * Abstract replicator.
     */
    public abstract class Abstract
        implements Replicator
    {
        /** {@inheritDoc}
         */
        @Override
        public final synchronized void close()
        {
            if (!_closed) {
                for (final Partner partner: getPartners()) {
                    close(partner);
                }

                _closed = true;
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public final synchronized void commit()
            throws InterruptedException
        {
            if (isEnabled()) {
                final long mark = System.nanoTime();

                if (_closed) {
                    throw new InterruptedException();
                }

                for (final Partner partner: getPartners()) {
                    commit(partner);
                }

                _traces.commit();
                _time += System.nanoTime() - mark;

                if (_replicates > 0) {
                    getThisLogger()
                        .trace(
                            StoreMessages.REPLICATES_SENT,
                            String.valueOf(_replicates));
                    getStats().addReplicates(_replicates, _time);
                }

                _replicates = 0;
                _time = 0;
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public final Collection<String> getPartnerNames()
        {
            return _namedPartners.keySet();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean join()
        {
            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public final synchronized void replicate(
                final PointValue pointValue)
            throws InterruptedException
        {
            if (_closed) {
                throw new InterruptedException();
            }

            if (pointValue instanceof ReplicatedValue) {
                return;
            }

            if (isEnabled()) {
                final long mark = System.nanoTime();
                final boolean replicated = isReplicated(pointValue);
                final UUID pointUUID = Require
                    .notNull(pointValue.getPointUUID());
                final Collection<Target> targets = getTargets(pointUUID);

                if (!replicated && targets.isEmpty()) {
                    return;
                }

                for (PointValue filteredValue: filter(pointValue)) {
                    if (replicated) {
                        for (final Partner partner: getAnonymousPartners()) {
                            replicate(
                                new ReplicatedValue(filteredValue),
                                partner);
                            ++_replicates;

                            getThisLogger()
                                .trace(
                                    StoreMessages.SENT_REPLICATE,
                                    filteredValue.getPoint().get(),
                                    partner);
                        }
                    }

                    for (final Target target: targets) {
                        final Partner partner = target.getPartner();
                        final ReplicatedValue replicatedValue;

                        if (filteredValue.isDeleted()) {
                            replicatedValue = new ReplicatedValue(
                                filteredValue.morph(
                                    Optional.of(target.getPoint()),
                                    Optional.empty()));
                            replicatedValue.setState(null);
                            replicatedValue.setValue(null);
                        } else if (target.shouldConvert(_replicateConverts)) {
                            filteredValue = filteredValue.normalized();
                            replicatedValue = new ReplicatedValue(
                                filteredValue.morph(
                                    Optional.of(target.getPoint()),
                                    Optional.empty()).denormalized());
                        } else {
                            replicatedValue = new ReplicatedValue(
                                filteredValue.morph(
                                    Optional.of(target.getPoint()),
                                    Optional.empty()));
                        }

                        replicate(replicatedValue, partner);
                        ++_replicates;

                        getThisLogger()
                            .trace(
                                StoreMessages.SENT_REPLICATE_AS,
                                filteredValue.getPoint().get(),
                                replicatedValue.getPoint().get(),
                                partner);
                    }

                    _traces.add(filteredValue);
                    _time += System.nanoTime() - mark;
                }
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public final boolean setUp(final StoreServiceAppImpl storeAppImpl)
        {
            _storeAppImpl = storeAppImpl;

            final KeyedGroups replicatorProperties = storeAppImpl
                .getServerProperties()
                .getGroup(REPLICATOR_PROPERTIES);

            if (replicatorProperties.isMissing()) {
                return true;
            }

            final KeyedGroups[] partnersProperties = replicatorProperties
                .getGroups(PARTNER_PROPERTIES);

            for (final KeyedGroups partnerProperties: partnersProperties) {
                final Partner partner = newPartner(partnerProperties);
                final Optional<String> partnerStoreName = partner
                    .getStoreName();

                if (partnerStoreName.isPresent()) {
                    final String partnerName = partnerStoreName
                        .get()
                        .toUpperCase(Locale.ROOT);

                    if (_namedPartners.put(partnerName, partner) != null) {
                        getThisLogger()
                            .error(
                                StoreMessages.DUPLICATE_PARTNER_NAME,
                                partnerStoreName.get());

                        return false;
                    }
                } else {
                    _anonymousPartners.add(partner);
                }

                _partners.add(partner);
            }

            if (_partners.isEmpty()) {
                return true;
            }

            getThisLogger()
                .debug(
                    StoreMessages.WILL_REPLICATE,
                    Integer.valueOf(_partners.size()));

            _replicatedDefaults = replicatorProperties
                .getBoolean(REPLICATED_DEFAULTS_PROPERTY);
            _replicateConverts = replicatorProperties
                .getBoolean(REPLICATE_CONVERTS_PROPERTY);

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean setUpImpl()
        {
            if (!_traces
                .setUp(
                    _storeAppImpl.getDataDir(),
                    _storeAppImpl
                        .getConfigProperties()
                        .getGroup(Traces.TRACES_PROPERTIES),
                    _storeAppImpl.getSourceUUID(),
                    Optional.of(REPLICATED_TRACES))) {
                return false;
            }

            getThisLogger().debug(ServiceMessages.SET_UP_COMPLETED);

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public void start()
        {
            for (final Partner partner: getPartners()) {
                if (!open(partner)) {
                    _storeAppImpl.fail();
                }
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public final void tearDown()
        {
            final boolean wasEnabled = isEnabled();

            close();

            _traces.tearDown();
            _replicatedPoints.clear();
            _targets.clear();
            _partners.clear();
            _anonymousPartners.clear();
            _namedPartners.clear();

            if (wasEnabled) {
                getThisLogger().debug(ServiceMessages.TEAR_DOWN_COMPLETED);
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public final void useNewMetadata()
        {
            _replicatedPoints.clear();
            _targets.clear();
            _valueFilters.clear();

            for (final Point point: getMetadata().getPointsCollection()) {
                final PointEntity pointEntity = (PointEntity) point;

                if (pointEntity
                    .getStoreEntity()
                    .orElse(
                        null) != _storeAppImpl.getStoreEntity().orElse(null)) {
                    continue;
                }

                boolean replicated = point
                    .getParams()
                    .getBoolean(Point.REPLICATED_PARAM, _replicatedDefaults);

                if (replicated) {
                    _replicatedPoints.add(point);
                }

                for (final Point.Replicate replicate: point.getReplicates()) {
                    final Point replicatePoint = replicate.getPoint();
                    final Optional<StoreEntity> replicateStore =
                        ((PointEntity) replicatePoint)
                            .getStoreEntity();
                    final Partner partner = replicateStore
                        .isPresent()? _namedPartners
                            .get(replicateStore.get().getNameInUpperCase().get()): null;

                    if (partner != null) {
                        final UUID pointUUID = point.getUUID().get();

                        _targets.add(pointUUID, new Target(replicate, partner));
                        replicated = true;
                    } else {
                        getThisLogger()
                            .warn(
                                StoreMessages.PARTNER_NOT_SPECIFIED,
                                point,
                                replicate);
                    }
                }

                if (replicated) {
                    final ValueFilter valueFilter = point.filter();

                    if (!valueFilter.isDisabled()) {
                        _valueFilters.put(point.getUUID().get(), valueFilter);
                    }
                }
            }
        }

        /**
         * Closes the connection with a partner.
         *
         * @param partner The partner.
         */
        protected abstract void close(@Nonnull Partner partner);

        /**
         * Commits.
         *
         * @param partner The partner.
         *
         * @throws InterruptedException When interrupted.
         */
        protected abstract void commit(
                @Nonnull Partner partner)
            throws InterruptedException;

        /**
         * Filters a point value.
         *
         * @param pointValue The point value.
         *
         * @return The point value (empty when filtered).
         */
        @Nonnull
        @CheckReturnValue
        protected final PointValue[] filter(
                @Nonnull final PointValue pointValue)
        {
            final ValueFilter valueFilter = _valueFilters
                .get(pointValue.getPointUUID());

            return (valueFilter != null)? Require
                .notNull(
                    valueFilter
                        .filter(
                                Optional
                                        .of(pointValue))): new PointValue[] {
                                        pointValue, };
        }

        /**
         * Gets the anonymous partners.
         *
         * @return The anonymous partners.
         */
        @Nonnull
        @CheckReturnValue
        protected final Collection<Partner> getAnonymousPartners()
        {
            return _anonymousPartners;
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
            return _storeAppImpl.getService().getMetadata();
        }

        /**
         * Gets the partners.
         *
         * @return The partners.
         */
        @Nonnull
        @CheckReturnValue
        protected final Collection<Partner> getPartners()
        {
            return _partners;
        }

        /**
         * Gets the service.
         *
         * @return The service.
         */
        @Nonnull
        @CheckReturnValue
        protected final Service getService()
        {
            return _storeAppImpl.getService();
        }

        /**
         * Gets the stats.
         *
         * @return The stats.
         */
        @Nonnull
        @CheckReturnValue
        protected final StoreStats getStats()
        {
            return _storeAppImpl.getStoreStats();
        }

        /**
         * Gets the targets for a point.
         *
         * @param pointUUID The point's UUID.
         *
         * @return The targets.
         */
        @Nonnull
        @CheckReturnValue
        protected final Collection<Target> getTargets(
                @Nonnull final UUID pointUUID)
        {
            return _targets.getAll(pointUUID);
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
         * Asks if replication is enabled.
         *
         * @return True if enabled.
         */
        @CheckReturnValue
        protected final boolean isEnabled()
        {
            return !_partners.isEmpty();
        }

        /**
         * Asks if a point value is replicated.
         *
         * @param pointValue The point value.
         *
         * @return True if replicated.
         */
        @CheckReturnValue
        protected final boolean isReplicated(
                @Nonnull final PointValue pointValue)
        {
            return _replicatedPoints
                .contains(pointValue.getPoint().orElse(null));
        }

        /**
         * Returns a new partner informations object.
         *
         * @param partnerProperties The partner configuration properties.
         *
         * @return The new partner informations object.
         */
        @Nonnull
        @CheckReturnValue
        protected abstract Partner newPartner(
                @Nonnull final KeyedGroups partnerProperties);

        /**
         * Opens a connection with a partner.
         *
         * @param partner The partner.
         *
         * @return True on success.
         */
        @CheckReturnValue
        protected abstract boolean open(@Nonnull Partner partner);

        /**
         * Replicates a point value to a partner.
         *
         * @param pointValue The point value.
         * @param partner The partner.
         *
         * @throws InterruptedException When interrupted.
         */
        protected abstract void replicate(
                @Nonnull ReplicatedValue pointValue,
                @Nonnull Partner partner)
            throws InterruptedException;

        /** Partner properties. */
        public static final String PARTNER_PROPERTIES = "partner";

        /** Replicated defaults property. */
        public static final String REPLICATED_DEFAULTS_PROPERTY =
            "replicated.defaults";

        /** Traces subdirectory for replicated values. */
        public static final String REPLICATED_TRACES = "replicated";

        /** Replicate converts property. */
        public static final String REPLICATE_CONVERTS_PROPERTY =
            "replicate.converts";

        /** Replicator group properties. */
        public static final String REPLICATOR_PROPERTIES = "replicator";

        /** The store name of a partner. */
        public static final String STORE_NAME_PROPERTY = "store.name";

        private final Collection<Partner> _anonymousPartners =
            new LinkedList<>();
        private boolean _closed;
        private final Logger _logger = Logger.getInstance(getClass());
        private final Map<String, Partner> _namedPartners = new HashMap<>();
        private final Collection<Partner> _partners = new LinkedList<>();
        private boolean _replicateConverts;
        private boolean _replicatedDefaults;
        private final Set<Point> _replicatedPoints = new IdentityHashSet<>();
        private long _replicates;
        private StoreServiceAppImpl _storeAppImpl;
        private final ListMap<UUID, Target> _targets = new ListHashMap<>();
        private long _time;
        private final Traces _traces = new Traces();
        private final Map<UUID, ValueFilter> _valueFilters = new HashMap<>();
    }


    /**
     * Partner.
     */
    abstract class Partner
    {
        /**
         * Constructs an instance.
         *
         * @param storeName The store name.
         */
        protected Partner(@Nonnull final Optional<String> storeName)
        {
            _storeName = storeName;
        }

        /** {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return _storeName.isPresent()? _storeName.get(): "?";
        }

        /**
         * Gets the store name.
         *
         * @return The optional store name.
         */
        @Nonnull
        @CheckReturnValue
        protected final Optional<String> getStoreName()
        {
            return _storeName;
        }

        private final Optional<String> _storeName;
    }


    /**
     * Target.
     */
    class Target
    {
        /**
         * Constructs an instance.
         *
         * @param replicate The point replicate in the target.
         * @param partner The target partner.
         */
        Target(
                @Nonnull final Point.Replicate replicate,
                @Nonnull final Partner partner)
        {
            _replicate = replicate;
            _partner = partner;
        }

        /**
         * Gets the partner.
         *
         * @return The partner.
         */
        @Nonnull
        @CheckReturnValue
        Partner getPartner()
        {
            return _partner;
        }

        /**
         * Gets the point.
         *
         * @return The point.
         */
        @Nonnull
        @CheckReturnValue
        Point getPoint()
        {
            return _replicate.getPoint();
        }

        /**
         * Asks if point values should be converted.
         *
         * @param defaultConvert The default answer.
         *
         * @return True if point values should be converted.
         */
        @CheckReturnValue
        boolean shouldConvert(final boolean defaultConvert)
        {
            final Optional<Boolean> convert = _replicate.getConvert();

            return convert
                .isPresent()? convert.get().booleanValue(): defaultConvert;
        }

        private final Partner _partner;
        private final Point.Replicate _replicate;
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
