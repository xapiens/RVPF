/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Archiver.java 4043 2019-06-02 15:05:37Z SFB $
 */

package org.rvpf.store.server.archiver;

import java.io.File;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.ClassDef;
import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Params;
import org.rvpf.base.Point;
import org.rvpf.base.UUID;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.store.Store;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.config.Config;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.StoreEntity;
import org.rvpf.service.ServiceMessages;
import org.rvpf.store.server.StoreMessages;
import org.rvpf.store.server.StoreServer;
import org.rvpf.store.server.StoreServiceAppImpl;
import org.rvpf.store.server.som.SOMAttic;

/**
 * Archiver.
 */
public interface Archiver
{
    /**
     * Asks if the archiver supports a store server.
     *
     * @param storeServer The store server.
     *
     * @return True if the store server is supported.
     */
    @CheckReturnValue
    static boolean supports(@Nonnull final StoreServer storeServer)
    {
        return storeServer.supportsDelete() && storeServer.supportsCount();
    }

    /**
     * Archives a point.
     *
     * @param point The point.
     *
     * @throws ServiceNotAvailableException When service not available.
     */
    void archive(Point point)
        throws ServiceNotAvailableException;

    /**
     * Commits.
     */
    void commit();

    /**
     * Gets the attic.
     *
     * @return The optional attic.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Attic> getAttic();

    /**
     * Sets up this.
     *
     * @param storeAppImpl The calling store application implementation.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUp(@Nonnull final StoreServiceAppImpl storeAppImpl);

    /**
     * Sets up points.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUpPoints();

    /**
     * Starts.
     */
    void start();

    /**
     * Stops.
     */
    void stop();

    /**
     * Tears down what has been set up.
     */
    void tearDown();

    /** Archiver properties. */
    String ARCHIVER_PROPERTIES = "archiver";

    /** Attic implementation class property. */
    String ATTIC_IMPLEMENTATION_CLASS_PROPERTY = "implementation.class";

    /** Attic properties. */
    String ATTIC_PROPERTIES = "attic";

    /** Default attic implementation class. */
    ClassDef DEFAULT_ATTIC_IMPLEMENTATION_CLASS = new ClassDefImpl(
        SOMAttic.class);

    /** Disables the attic. */
    String DISABLED_PROPERTY = "disabled";

    /** Store removed event. */
    String STORE_REMOVED_EVENT = "StoreRemoved";

    /**
     * Attic.
     */
    public interface Attic
    {
        /**
         * Commits.
         */
        void commit();

        /**
         * Puts archived point values into the attic.
         *
         * @param archivedValues The archived point values.
         */
        void put(@Nonnull Collection<PointValue> archivedValues);

        /**
         * Sets up the attic.
         *
         * @param atticProperties The attic properties.
         * @param config The config.
         * @param dataDir The data directory.
         *
         * @return True on success.
         */
        @CheckReturnValue
        boolean setUp(
                @Nonnull KeyedGroups atticProperties,
                @Nonnull Config config,
                @Nonnull File dataDir);

        /**
         * Tears down what has been set up.
         */
        void tearDown();
    }


    /**
     * Abstract.
     */
    abstract class Abstract
        implements Archiver
    {
        /** {@inheritDoc}
         */
        @Override
        public void archive(
                final Point point)
            throws ServiceNotAvailableException {}

        /** {@inheritDoc}
         */
        @Override
        public void commit() {}

        /** {@inheritDoc}
         */
        @Override
        public Optional<Attic> getAttic()
        {
            return Optional.ofNullable(_attic);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean setUp(final StoreServiceAppImpl storeAppImpl)
        {
            _storeAppImpl = storeAppImpl;

            _properties = getStoreAppImpl()
                .getServerProperties()
                .getGroup(ARCHIVER_PROPERTIES);

            if (_properties.isMissing()) {
                return true;    // The archiver will be inactive.
            }

            if (!setUpAttic(
                    _properties.getGroup(ATTIC_PROPERTIES),
                    storeAppImpl.getConfig(),
                    storeAppImpl.getStoreDataDir())) {
                return false;
            }

            if (!setUpPoints()) {
                return false;
            }

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean setUpPoints()
        {
            final Metadata metadata = _storeAppImpl.getService().getMetadata();
            final Optional<StoreEntity> appStoreEntity = _storeAppImpl
                .getStoreEntity();

            if (!appStoreEntity.isPresent()) {
                return true;
            }

            final StoreEntity storeEntity = metadata
                .getStoreEntity(appStoreEntity.get().getName())
                .get();
            final Params storeParams = storeEntity.getParams();
            final Optional<ElapsedTime> storeArchiveTime = storeParams
                .getElapsed(
                    Store.ARCHIVE_TIME_PARAM,
                    Optional.empty(),
                    Optional.empty());
            final Optional<ElapsedTime> storeLifeTime = storeParams
                .getElapsed(
                    Store.LIFE_TIME_PARAM,
                    Optional.empty(),
                    Optional.empty());
            final boolean defaultRespectVersion = storeParams
                .getBoolean(Store.RESPECT_VERSION_PARAM);
            final ElapsedTime defaultLifeTime;
            final boolean defaultIsArchive;

            if (storeArchiveTime.isPresent()) {
                if (storeLifeTime.isPresent()) {
                    getThisLogger()
                        .error(
                            ServiceMessages.PARAM_CONFLICT,
                            Store.LIFE_TIME_PARAM,
                            Store.ARCHIVE_TIME_PARAM);

                    return false;
                }

                defaultLifeTime = storeArchiveTime.get();
                defaultIsArchive = true;
            } else {
                defaultLifeTime = storeLifeTime.orElse(null);
                defaultIsArchive = false;
            }

            for (final Point point: metadata.getPointsCollection()) {
                final Optional<ElapsedTime> pointArchiveTime = point
                    .getParams()
                    .getElapsed(
                        Point.ARCHIVE_TIME_PARAM,
                        Optional.empty(),
                        Optional.empty());
                final Optional<ElapsedTime> pointLifeTime = point
                    .getParams()
                    .getElapsed(
                        Point.LIFE_TIME_PARAM,
                        Optional.empty(),
                        Optional.empty());
                final boolean respectVersion = storeEntity
                    .getParams()
                    .getBoolean(
                        Point.RESPECT_VERSION_PARAM,
                        defaultRespectVersion);
                final ElapsedTime lifeTime;
                final boolean archived;

                if (pointArchiveTime.isPresent()) {
                    if (pointLifeTime.isPresent()) {
                        getThisLogger()
                            .error(
                                ServiceMessages.PARAM_CONFLICT,
                                Point.LIFE_TIME_PARAM,
                                Point.ARCHIVE_TIME_PARAM);

                        return false;
                    }

                    lifeTime = pointArchiveTime.get();
                    archived = true;
                } else if (pointLifeTime.isPresent()) {
                    lifeTime = pointLifeTime.get();
                    archived = false;
                } else {
                    lifeTime = defaultLifeTime;
                    archived = defaultIsArchive;
                }

                final int keepAtLeast = point
                    .getParams()
                    .getInt(Point.KEEP_AT_LEAST_PARAM, 1);
                final int keepAtMost = point
                    .getParams()
                    .getInt(Point.KEEP_AT_MOST_PARAM, -1);

                if ((lifeTime != null) || (keepAtMost > 0)) {
                    _pointReferences
                        .put(
                            point,
                            new PointReference(
                                point,
                                Optional.ofNullable(lifeTime),
                                archived,
                                keepAtLeast,
                                keepAtMost,
                                respectVersion));
                }
            }

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public void tearDown()
        {
            if (_attic != null) {
                _attic.tearDown();
                _attic = null;
            }

            _storeAppImpl = null;
        }

        /**
         * Archives a point.
         *
         * @param pointReference The point reference.
         * @param scanTime The scan time.
         *
         * @throws ServiceNotAvailableException When service is not available.
         */
        protected void archive(
                @Nonnull final PointReference pointReference,
                @Nonnull final DateTime scanTime)
            throws ServiceNotAvailableException
        {
            final DateTime keepTime = scanTime
                .before(pointReference.getLifeTime().orElse(ElapsedTime.EMPTY));
            final Optional<DateTime> nextStamp = pointReference.getNextStamp();

            if (nextStamp.isPresent()
                    && nextStamp.get().isNotBefore(keepTime)) {
                return;
            }

            final Point point = pointReference.getPoint();
            final int keepAtMost = pointReference.getKeepAtMost();
            final StoreValuesQuery.Builder storeQueryBuilder = StoreValuesQuery
                .newBuilder();

            if (keepAtMost > 0) {
                final UUID pointUUID = point.getUUID().get();

                // Removes all deleted values.
                storeQueryBuilder.setPointUUID(pointUUID.deleted());
                storeQueryBuilder.setAll(true);
                _archive(
                    pointReference,
                    storeQueryBuilder,
                    DateTime.END_OF_TIME,
                    false);

                // Gets the total number of values.
                storeQueryBuilder.setPoint(point);

                final long removeCount = _count(storeQueryBuilder) - keepAtMost;

                // Removes the unneeded values.
                if (removeCount > 0) {
                    storeQueryBuilder.setPoint(point).clear();
                    storeQueryBuilder
                        .setRows(
                            (int) Math.min(removeCount, Integer.MAX_VALUE));
                    _archive(
                        pointReference,
                        storeQueryBuilder,
                        DateTime.END_OF_TIME,
                        false);
                }
            }

            if (!pointReference.getLifeTime().isPresent()) {
                return;    // No time related actions.
            }

            final int keepAtLeast = pointReference.getKeepAtLeast();
            final long rows;

            if (keepAtLeast > 0) {
                if (keepAtMost <= 0) {
                    final UUID pointUUID = point.getUUID().get();

                    // Removes all deleted values.

                    storeQueryBuilder.setPointUUID(pointUUID.deleted()).clear();
                    storeQueryBuilder.setBefore(keepTime);
                    storeQueryBuilder.setAll(true);
                    _archive(
                        pointReference,
                        storeQueryBuilder,
                        DateTime.END_OF_TIME,
                        false);
                }

                // Gets the number of rows to remove.
                storeQueryBuilder.setPoint(point);
                rows = _count(storeQueryBuilder) - keepAtLeast;

                if (rows <= 0) {
                    return;
                }
            } else {
                rows = -1;    // Removes all rows, including deleted.
            }

            storeQueryBuilder.setPoint(point).clear();
            storeQueryBuilder.setBefore(keepTime);

            if (rows > 0) {
                storeQueryBuilder
                    .setRows((int) Math.min(rows, Integer.MAX_VALUE));
            } else {
                storeQueryBuilder.setAll(true);
                storeQueryBuilder.setIncludeDeleted(true);
            }

            _archive(
                pointReference,
                storeQueryBuilder,
                keepTime,
                pointReference.isRespectVersion());
        }

        /**
         * Commits the attic.
         */
        protected void commitAttic()
        {
            if (_archived > 0) {
                _attic.commit();
            }
        }

        /**
         * Gets a point reference.
         *
         * @param point The point.
         *
         * @return The point reference (empty if unknown).
         */
        @Nonnull
        @CheckReturnValue
        protected Optional<PointReference> getPointReference(
                @Nonnull final Point point)
        {
            return Optional
                .ofNullable(_pointReferences.get(Require.notNull(point)));
        }

        /**
         * Gets the point references.
         *
         * @return The point references.
         */
        @Nonnull
        @CheckReturnValue
        protected Collection<PointReference> getPointReferences()
        {
            return _pointReferences.values();
        }

        /**
         * Gets the properties.
         *
         * @return The properties.
         */
        @Nonnull
        @CheckReturnValue
        protected KeyedGroups getProperties()
        {
            return Require.notNull(_properties);
        }

        /**
         * Gets the storeAppImpl.
         *
         * @return The storeAppImpl.
         */
        @Nonnull
        @CheckReturnValue
        protected StoreServiceAppImpl getStoreAppImpl()
        {
            return Require.notNull(_storeAppImpl);
        }

        /**
         * Gets this logger.
         *
         * @return The logger.
         */
        @Nonnull
        @CheckReturnValue
        protected Logger getThisLogger()
        {
            return _logger;
        }

        /**
         * Asks if disabled.
         *
         * @return True if disabled.
         */
        @CheckReturnValue
        protected boolean isDisabled()
        {
            return _pointReferences.isEmpty();
        }

        /**
         * Sets up the attic.
         *
         * @param atticProperties The attic properties.
         * @param config The config.
         * @param dataDir The data directory.
         *
         * @return True on success.
         */
        protected boolean setUpAttic(
                @Nonnull final KeyedGroups atticProperties,
                @Nonnull final Config config,
                @Nonnull final File dataDir)
        {
            _atticDisabled = atticProperties.getBoolean(DISABLED_PROPERTY);

            if (_atticDisabled) {
                getThisLogger().info(StoreMessages.ARCHIVER_ATTIC_DISABLED);
            } else {
                final ClassDef atticClassDef = atticProperties
                    .getClassDef(
                        ATTIC_IMPLEMENTATION_CLASS_PROPERTY,
                        DEFAULT_ATTIC_IMPLEMENTATION_CLASS);

                _attic = atticClassDef.createInstance(Attic.class);

                if (_attic == null) {
                    return false;
                }

                if (atticClassDef != DEFAULT_ATTIC_IMPLEMENTATION_CLASS) {
                    getThisLogger()
                        .info(StoreMessages.ATTIC_CLASS, atticClassDef);
                }

                if (!_attic.setUp(atticProperties, config, dataDir)) {
                    return false;
                }
            }

            return true;
        }

        /**
         * Updates the stats.
         */
        protected void updateStats()
        {
            if (_removed == 0) {
                return;
            }

            if (_archived > 0) {
                final String archivedString = Long
                    .valueOf(_archived)
                    .toString();

                getThisLogger()
                    .debug(StoreMessages.ARCHIVED_VALUES, archivedString);
            }

            final long forgotten = _removed - _archived;

            if (forgotten > 0) {
                final String forgottenString = Long
                    .valueOf(forgotten)
                    .toString();

                getThisLogger()
                    .debug(StoreMessages.FORGOTTEN_VALUES, forgottenString);
            }

            final String removedString = Long.valueOf(_removed).toString();

            getStoreAppImpl()
                .getService()
                .sendEvent(STORE_REMOVED_EVENT, Optional.of(removedString));

            _storeAppImpl.getStoreStats().addRemoved(_removed, _archived);
            _removed = 0;
            _archived = 0;
        }

        private void _archive(
                final PointReference reference,
                final StoreValuesQuery.Builder storeQueryBuilder,
                final DateTime keepTime,
                final boolean respectVersion)
            throws ServiceNotAvailableException
        {
            final List<PointValue> pointValues = new LinkedList<PointValue>();
            StoreValuesQuery storeQuery = storeQueryBuilder
                .setReverse(false)
                .build();

            for (;;) {
                final StoreValues response = _select(storeQuery);

                for (final PointValue pointValue: response) {
                    if (respectVersion
                            && (pointValue instanceof VersionedValue)) {
                        final DateTime version = ((VersionedValue) pointValue)
                            .getVersion();

                        if (version.isNotBefore(keepTime)) {
                            reference.setNextStamp(version);

                            continue;
                        }
                    }

                    pointValues.add(pointValue);
                }

                if (!pointValues.isEmpty()) {
                    final Collection<PointValue> archivedValues = _atticDisabled
                        ? null: new LinkedList<PointValue>();
                    final PointValue[] removedValues =
                        new PointValue[pointValues.size()];
                    int i = 0;

                    for (final PointValue pointValue: pointValues) {
                        if (!_atticDisabled
                                && reference.isArchived()
                                && !pointValue.isDeleted()) {
                            archivedValues.add(pointValue);
                            ++_archived;
                        }

                        removedValues[i++] = new VersionedValue.Purged(
                            pointValue);
                        ++_removed;
                    }

                    pointValues.clear();

                    if ((archivedValues != null) && !archivedValues.isEmpty()) {
                        _attic.put(archivedValues);
                    }

                    _update(removedValues);
                }

                if (response.isComplete()) {
                    break;
                }

                storeQuery = response.createQuery();
            }
        }

        private long _count(
                final StoreValuesQuery.Builder storeQueryBuilder)
            throws SessionException
        {
            storeQueryBuilder.setCount(true);

            final StoreValues response = _select(storeQueryBuilder.build());
            final long count = response.getCount();

            if (count < 0) {
                throw new IllegalStateException();
            }

            return count;
        }

        private StoreValues _select(
                final StoreValuesQuery storeQuery)
            throws SessionException
        {
            final StoreValues response = getStoreAppImpl()
                .getServer()
                .select(
                    new StoreValuesQuery[] {storeQuery},
                    Optional.empty())[0];

            if (!response.isSuccess()) {
                throw new RuntimeException(response.getException().get());
            }

            return response;
        }

        private void _update(
                final PointValue[] removedValues)
            throws ServiceNotAvailableException
        {
            final Exception[] response = getStoreAppImpl()
                .getServer()
                .update(removedValues, Optional.empty());

            for (final Exception exception: response) {
                if (exception != null) {
                    throw new RuntimeException(exception);
                }
            }
        }

        private long _archived;
        private Attic _attic;
        private final Logger _logger = Logger.getInstance(getClass());
        private boolean _atticDisabled = true;
        private final Map<Point, PointReference> _pointReferences =
            new LinkedHashMap<>();
        private KeyedGroups _properties;
        private long _removed;
        private StoreServiceAppImpl _storeAppImpl;

        /**
         * Point reference.
         */
        protected static final class PointReference
        {
            /**
             * Constructs an instance.
             *
             * @param point The point.
             * @param lifeTime The optional life time.
             * @param archived True if values are archived.
             * @param keepAtLeast The minimum number of values to keep.
             * @param keepAtMost The maximum number of values to keep.
             * @param respectVersion True to respect the point version.
             */
            PointReference(
                    @Nonnull final Point point,
                    @Nonnull final Optional<ElapsedTime> lifeTime,
                    final boolean archived,
                    final int keepAtLeast,
                    final int keepAtMost,
                    final boolean respectVersion)
            {
                _point = point;
                _lifeTime = lifeTime;
                _archived = archived;
                _keepAtLeast = keepAtLeast;
                _keepAtMost = keepAtMost;
                _respectVersion = respectVersion;
            }

            /**
             * Gets the minimum number of values to keep.
             *
             * @return The minimum number of values to keep.
             */
            @CheckReturnValue
            int getKeepAtLeast()
            {
                return _keepAtLeast;
            }

            /**
             * Gets the keepAtMost.
             *
             * @return The keepAtMost.
             */
            @CheckReturnValue
            int getKeepAtMost()
            {
                return _keepAtMost;
            }

            /**
             * Gets the archive time.
             *
             * @return The optional archive time.
             */
            @Nonnull
            @CheckReturnValue
            Optional<ElapsedTime> getLifeTime()
            {
                return _lifeTime;
            }

            /**
             * Gets the next stamp.
             *
             * @return The optional next stamp.
             */
            @Nonnull
            @CheckReturnValue
            Optional<DateTime> getNextStamp()
            {
                return Optional.ofNullable(_nextStamp);
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
                return _point;
            }

            /**
             * Gets the archived indicator.
             *
             * @return True if archived.
             */
            @CheckReturnValue
            boolean isArchived()
            {
                return _archived;
            }

            /**
             * Gets the respect version indicator.
             *
             * @return The respect version indicator.
             */
            @CheckReturnValue
            boolean isRespectVersion()
            {
                return _respectVersion;
            }

            /**
             * Sets the next stamp.
             *
             * @param nextStamp The next stamp.
             */
            void setNextStamp(@Nonnull final DateTime nextStamp)
            {
                _nextStamp = nextStamp;
            }

            private final boolean _archived;
            private final int _keepAtLeast;
            private final int _keepAtMost;
            private final Optional<ElapsedTime> _lifeTime;
            private DateTime _nextStamp;
            private final Point _point;
            private final boolean _respectVersion;
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
