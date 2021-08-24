/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreServer.java 4097 2019-06-25 15:35:48Z SFB $
 */

package org.rvpf.store.server;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Point;
import org.rvpf.base.TimeInterval;
import org.rvpf.base.UUID;
import org.rvpf.base.exception.PointUnknownException;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.rmi.UnauthorizedAccessException;
import org.rvpf.base.security.Identity;
import org.rvpf.base.store.PointBinding;
import org.rvpf.base.store.PointBinding.Request;
import org.rvpf.base.store.Store;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.store.UnresolvedStateException;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.Traces;
import org.rvpf.base.util.SnoozeAlarm;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ReplicatedValue;
import org.rvpf.base.value.State;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.Permissions;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.metadata.entity.StoreEntity;
import org.rvpf.store.server.archiver.Archiver;
import org.rvpf.store.server.polator.Polator;
import org.rvpf.store.server.rmi.StoreSessionImpl;

/**
 * Store server interface.
 */
public interface StoreServer
{
    /**
     * Adds a notice listener.
     *
     * @param noticeListener The notice listener.
     */
    void addNoticeListener(@Nonnull NoticeListener noticeListener);

    /**
     * Binds point names to their UUID.
     *
     * @param bindRequests The bind requests.
     *
     * @return The successful bindings (null on failure).
     */
    @Nullable
    @CheckReturnValue
    PointBinding[] bind(@Nonnull final PointBinding.Request[] bindRequests);

    /**
     * Disables suspend.
     *
     * @throws InterruptedException When interrupted.
     */
    void disableSuspend()
        throws InterruptedException;

    /**
     * Enables suspend.
     */
    void enableSuspend();

    /**
     * Gets the metadata.
     *
     * @return The metadata.
     */
    @Nonnull
    @CheckReturnValue
    Metadata getMetadata();

    /**
     * Gets the response limit.
     *
     * @return The response limit.
     */
    @CheckReturnValue
    int getResponseLimit();

    /**
     * Gets the state groups.
     *
     * @return The state groups.
     */
    @Nonnull
    @CheckReturnValue
    Collection<State.Group> getStateGroups();

    /**
     * Gets the store stats.
     *
     * @return The store stats.
     */
    @Nonnull
    @CheckReturnValue
    StoreStats getStats();

    /**
     * Asks if this is a memory store.
     *
     * @return True if this is a memory store.
     */
    @CheckReturnValue
    boolean isMemory();

    /**
     * Returns a new archiver.
     *
     * @return The optional new archiver.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Archiver> newArchiver();

    /**
     * Probes the state of the server.
     *
     * @return True if the state of the server is fine.
     */
    @CheckReturnValue
    boolean probe();

    /**
     * Pulls points values.
     *
     * @param query The store query.
     * @param timeout A time limit in millis to wait for the first value
     *                (negative for infinite).
     * @param storeSession The store session implementation.
     * @param identity The optional requesting identity.
     *
     * @return The optional point values
     *         (empty store values on timeout, empty on service closed).
     *
     * @throws SessionException On session exception.
     *
     * @see Store#pull(StoreValuesQuery, long)
     */
    @Nonnull
    @CheckReturnValue
    Optional<StoreValues> pull(
            @Nonnull StoreValuesQuery query,
            long timeout,
            @Nonnull StoreSessionImpl storeSession,
            @Nonnull Optional<Identity> identity)
        throws SessionException;

    /**
     * Purges points values.
     *
     * @param pointUUIDs The UUID of the points to purge.
     * @param timeInterval A time interval.
     * @param identity The optional requesting identity.
     *
     * @return The number of values purged.
     *
     * @throws ServiceNotAvailableException On service not available.
     */
    int purge(
            @Nonnull UUID[] pointUUIDs,
            @Nonnull TimeInterval timeInterval,
            @Nonnull Optional<Identity> identity)
        throws ServiceNotAvailableException;

    /**
     * Removes a notice listener.
     *
     * @param noticeListener The notice listener.
     */
    void removeNoticeListener(@Nonnull NoticeListener noticeListener);

    /**
     * Resolves a state for a point UUID.
     *
     * @param state The state.
     * @param pointUUID The optional point UUID.
     *
     * @return The optional resolved state.
     */
    @Nonnull
    @CheckReturnValue
    Optional<State> resolveForPointUUID(
            @Nonnull State state,
            @Nonnull Optional<UUID> pointUUID);

    /**
     * Resume updates.
     */
    void resumeUpdates();

    /**
     * Selects point values.
     *
     * @param queries A store query array.
     * @param identity The optional requesting identity.
     *
     * @return The point values.
     *
     * @throws SessionException On session exception.
     */
    @Nonnull
    @CheckReturnValue
    StoreValues[] select(
            @Nonnull StoreValuesQuery[] queries,
            @Nonnull Optional<Identity> identity)
        throws SessionException;

    /**
     * Starts this server
     */
    void start();

    /**
     * Stops this server
     */
    void stop();

    /**
     * Returns a string of supported value type codes.
     *
     * @return The string of supported value type codes.
     *
     * @throws SessionException On session exception.
     */
    @Nonnull
    @CheckReturnValue
    String supportedValueTypeCodes()
        throws SessionException;

    /**
     * Asks if the server supports count.
     *
     * @return True if count is supported.
     */
    @CheckReturnValue
    boolean supportsCount();

    /**
     * Asks if the server supports delete.
     *
     * @return True if delete is supported.
     */
    @CheckReturnValue
    boolean supportsDelete();

    /**
     * Asks if the server supports deliver.
     *
     * @return True if deliver is supported.
     */
    @CheckReturnValue
    boolean supportsDeliver();

    /**
     * Asks if the server supports pull.
     *
     * @return True if pull is supported.
     */
    @CheckReturnValue
    boolean supportsPull();

    /**
     * Asks if the server supports purge.
     *
     * @return True if purge is supported.
     */
    @CheckReturnValue
    boolean supportsPurge();

    /**
     * Asks if the server supports subscribe.
     *
     * @return True if subscribe is supported.
     */
    @CheckReturnValue
    boolean supportsSubscribe();

    /**
     * Suspends updates.
     */
    void suspendUpdates();

    /**
     * Unbinds.
     *
     * @param bindings The point bindings.
     */
    void unbind(@Nonnull final PointBinding[] bindings);

    /**
     * Updates point values.
     *
     * @param pointValues The point values.
     * @param identity The optional requesting identity.
     *
     * @return An exception array (null on service closed).
     *
     * @throws ServiceNotAvailableException On service not available.
     */
    @Nullable
    @CheckReturnValue
    Exception[] update(
            @Nonnull PointValue[] pointValues,
            @Nonnull Optional<Identity> identity)
        throws ServiceNotAvailableException;

    /**
     * Abstract store server.
     */
    abstract class Abstract
        implements StoreServer
    {
        /**
         * Adds a binding.
         *
         * @param binding The binding.
         */
        public final void addBinding(@Nonnull final PointBinding binding)
        {
            synchronized (_bindings) {
                _bindings.add(binding);
                _bindingsByName.put(binding.getName(), binding);
                _bindingsByUUID.put(binding.getServerUUID(), binding);
            }
        }

        /**
         * Adds a notice.
         *
         * @param notice A point value.
         *
         * @throws InterruptedException When the service is stopped.
         */
        public final void addNotice(
                @Nonnull final PointValue notice)
            throws InterruptedException
        {
            _getStoreAppImpl().addNotice(notice);
        }

        /** {@inheritDoc}
         */
        @Override
        public final void addNoticeListener(final NoticeListener noticeListener)
        {
            _getStoreAppImpl().addNoticeListener(noticeListener);
        }

        /** {@inheritDoc}
         */
        @Override
        public PointBinding[] bind(final Request[] bindRequests)
        {
            return null;
        }

        /** {@inheritDoc}
         */
        @Override
        public final void disableSuspend()
            throws InterruptedException
        {
            _getStoreAppImpl().getService().disableSuspend();
        }

        /** {@inheritDoc}
         */
        @Override
        public final void enableSuspend()
        {
            _getStoreAppImpl().getService().enableSuspend();
        }

        /**
         * Gets the back-end limit.
         *
         * @return The back-end limit.
         */
        @CheckReturnValue
        public final int getBackEndLimit()
        {
            return _getStoreAppImpl().getBackEndLimit();
        }

        /**
         * Gets a point binding by name.
         *
         * @param name The name.
         *
         * @return The optional point binding.
         */
        @Nonnull
        @CheckReturnValue
        public final Optional<PointBinding> getBinding(final String name)
        {
            return Optional.ofNullable(_bindingsByName.get(name));
        }

        /** {@inheritDoc}
         */
        @Override
        public final Metadata getMetadata()
        {
            return _getStoreAppImpl().getService().getMetadata();
        }

        /**
         * Gets the replicator.
         *
         * @return The replicator.
         */
        @Nonnull
        @CheckReturnValue
        public final Replicator getReplicator()
        {
            return _getStoreAppImpl().getReplicator();
        }

        /** {@inheritDoc}
         */
        @Override
        public final int getResponseLimit()
        {
            return _getStoreAppImpl().getResponseLimit();
        }

        /** {@inheritDoc}
         */
        @Override
        public final Collection<State.Group> getStateGroups()
        {
            return _stateGroups.values();
        }

        /** {@inheritDoc}
         */
        @Override
        public final StoreStats getStats()
        {
            return _getStoreAppImpl().getStoreStats();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isMemory()
        {
            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean probe()
        {
            return !isStopped();
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<StoreValues> pull(
                final StoreValuesQuery query,
                long timeout,
                final StoreSessionImpl storeSession,
                final Optional<Identity> identity)
            throws SessionException
        {
            final StoreValuesQuery[] queries = {query, };
            final long startMillis = (timeout > 0)? System
                .currentTimeMillis(): 0;

            if (timeout < 0) {
                timeout = Long.MAX_VALUE;
            }

            for (;;) {
                final StoreValues storeValues = select(queries, identity)[0];

                if (!storeValues.isEmpty()) {
                    return Optional.of(storeValues);
                }

                final long elapsedMillis = System
                    .currentTimeMillis() - startMillis;

                if ((elapsedMillis < 0) || (elapsedMillis >= timeout)) {
                    return Optional.of(storeValues);
                }

                Require.success(_pullSleep > 0);

                if (!storeSession
                    .sleep(Math.min(_pullSleep, timeout - elapsedMillis))) {
                    if (storeSession.interrupted()) {
                        storeSession.interrupt();

                        break;
                    }
                }
            }

            return Optional.empty();
        }

        /** {@inheritDoc}
         */
        @Override
        public int purge(
                final UUID[] pointUUIDs,
                final TimeInterval timeInterval,
                final Optional<Identity> identity)
            throws ServiceNotAvailableException
        {
            throw new UnsupportedOperationException();
        }

        /**
         * Removes a binding.
         *
         * @param binding The binding.
         *
         * @return True if it was alone for the point.
         */
        public final boolean removeBinding(@Nonnull PointBinding binding)
        {
            synchronized (_bindings) {
                _bindings.remove(binding);

                final PointBinding nextBinding = _bindings
                    .ceiling(
                        new PointBinding(
                            binding.getName(),
                            binding.getServerUUID(),
                            Optional.empty()));

                if ((nextBinding != null)
                        && nextBinding.getServerUUID().equals(
                            binding.getServerUUID())) {
                    return false;
                }

                binding = _bindingsByUUID.remove(binding.getServerUUID());
                _bindingsByName.remove(binding.getName());
            }

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public final void removeNoticeListener(
                final NoticeListener noticeListener)
        {
            _getStoreAppImpl().removeNoticeListener(noticeListener);
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<State> resolveForPointUUID(
                final State state,
                final Optional<UUID> pointUUID)
        {
            final Optional<Point> point = pointUUID
                .isPresent()? getMetadata()
                    .getPointByUUID(pointUUID.get()): Optional.empty();

            return resolveForPoint(state, point);
        }

        /** {@inheritDoc}
         */
        @Override
        public final void resumeUpdates()
        {
            _lock.readLock().unlock();
        }

        /**
         * Sends the notices.
         *
         * @throws InterruptedException When the service is stopped.
         */
        public void sendNotices()
            throws InterruptedException
        {
            _getStoreAppImpl().sendNotices();
        }

        /**
         * Sets up for processing.
         *
         * @param storeAppImpl The store application.
         *
         * @return True on success.
         */
        @CheckReturnValue
        public boolean setUp(@Nonnull final StoreServiceAppImpl storeAppImpl)
        {
            _storeAppImpl = storeAppImpl;

            _nullRemoves = storeAppImpl.isNullRemoves();

            // Sets up traces.

            final KeyedGroups tracesProperties = storeAppImpl
                .getConfigProperties()
                .getGroup(Traces.TRACES_PROPERTIES);
            final UUID sourceUUID = storeAppImpl.getSourceUUID();

            if (supportsUpdateTracer()) {
                Require
                    .ignored(
                        _updatedTraces
                            .setUp(
                                    storeAppImpl.getDataDir(),
                                            tracesProperties,
                                            sourceUUID,
                                            Optional.of(UPDATED_TRACES)));
            }

            if (supportsDeleteTracer()) {
                Require
                    .ignored(
                        _deletedTraces
                            .setUp(
                                    storeAppImpl.getDataDir(),
                                            tracesProperties,
                                            sourceUUID,
                                            Optional.of(DELETED_TRACES)));
            }

            // Sets up state groups.

            final String[] stateGroupRefs = storeAppImpl
                .getServerProperties()
                .getStrings(STATES_PROPERTY);

            _stateGroups.put("", new State.Group(""));

            for (final String stateGroupRef: stateGroupRefs) {
                final KeyedGroups groupProperties = getMetadata()
                    .getPropertiesGroup(stateGroupRef);

                if (groupProperties.isMissing()) {
                    getThisLogger()
                        .warn(
                            StoreMessages.STATE_GROUP_LOAD_FAILED,
                            stateGroupRef);

                    continue;
                }

                final String stateGroupName = groupProperties
                    .getString(NAME_PROPERTY, Optional.of(""))
                    .get()
                    .trim();
                final String[] stateGroupDefs = groupProperties
                    .getStrings(STATE_PROPERTY);
                State.Group stateGroup = _stateGroups.get(stateGroupName);
                int previousStateCode = -1;

                if (stateGroup == null) {
                    stateGroup = new State.Group(stateGroupName);
                }

                for (final String stateDef: stateGroupDefs) {
                    State state = State.fromString(stateDef);

                    if (!state.getCode().isPresent()) {
                        state = new State(
                            Optional.of(Integer.valueOf(previousStateCode + 1)),
                            state.getName());
                    }

                    previousStateCode = state.getCode().get().intValue();
                    stateGroup.put(state);
                }

                _stateGroups.put(stateGroup.getName(), stateGroup);
            }

            if (getThisLogger().isDebugEnabled()) {
                final int globalStates = _stateGroups.get("").size();
                final int stateGroups = _stateGroups.size() - 1;

                if (globalStates > 0) {
                    getThisLogger()
                        .debug(
                            StoreMessages.STATE_GROUP_GLOBAL_NAMES,
                            String.valueOf(globalStates));
                }

                if (stateGroups > 0) {
                    getThisLogger()
                        .debug(
                            StoreMessages.STATE_GROUPS,
                            String.valueOf(stateGroups));
                }
            }

            return setUpPullSleep();
        }

        /** {@inheritDoc}
         */
        @Override
        public void start()
        {
            _started = true;
        }

        /** {@inheritDoc}
         */
        @Override
        public void stop()
        {
            _stopped = true;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean supportsCount()
        {
            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean supportsDelete()
        {
            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean supportsDeliver()
        {
            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean supportsPull()
        {
            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean supportsPurge()
        {
            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean supportsSubscribe()
        {
            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public final void suspendUpdates()
        {
            _lock.readLock().lock();
        }

        /**
         * Tears down what has been set up.
         */
        public void tearDown()
        {
            _deletedTraces.tearDown();
            _updatedTraces.tearDown();

            _storeAppImpl = null;
        }

        /** {@inheritDoc}
         */
        @Override
        public void unbind(final PointBinding[] bindings) {}

        /**
         * Activates the service class loader.
         */
        protected void activateServiceClassLoader()
        {
            _getStoreAppImpl().getConfig().registerClassLoader();
        }

        /**
         * Checks if a purge on point values is allowed for the identity.
         *
         * @param pointUUIDs The points UUIDs.
         * @param identity The optional identity.
         *
         * @return True if the purge is allowed for the specified UUIDs.
         */
        @CheckReturnValue
        protected final boolean checkPurge(
                @Nonnull final UUID[] pointUUIDs,
                @Nonnull final Optional<Identity> identity)
        {
            final Optional<StoreEntity> storeEntity = _storeAppImpl
                .getStoreEntity();
            final Optional<? extends Permissions> storePermissions = storeEntity
                .isPresent()? storeEntity
                    .get()
                    .getPermissions(): Optional.empty();

            final boolean allowedByDefault = storePermissions
                .isPresent()? storePermissions
                    .get()
                    .check(Permissions.Action.DELETE, identity): true;
            final Metadata metadata = getMetadata();

            for (final UUID pointUUID: pointUUIDs) {
                final Optional<Point> optionalPoint = metadata
                    .getPointByUUID(pointUUID);

                if (optionalPoint.isPresent()) {
                    final PointEntity point = (PointEntity) optionalPoint.get();
                    final Optional<? extends Permissions> pointPermissions =
                        point
                            .getPermissions();

                    if ((pointPermissions.isPresent())
                            && !pointPermissions.get().check(
                                Permissions.Action.DELETE,
                                identity)) {
                        final Message message = new Message(
                            StoreMessages.POINT_ACTION_UNAUTHORIZED,
                            Permissions.Action.DELETE,
                            point);

                        getThisLogger().warn(message);

                        return false;
                    }
                } else if (!allowedByDefault) {
                    final Message message = new Message(
                        StoreMessages.POINT_ACTION_UNAUTHORIZED,
                        Permissions.Action.DELETE,
                        pointUUID);

                    getThisLogger().warn(message);

                    return false;
                }
            }

            return true;
        }

        /**
         * Checks if an update on a point's values is allowed for the identity.
         *
         * @param pointValue A point value.
         * @param identity The optional identity.
         *
         * @return An exception if not allowed.
         */
        @Nonnull
        @CheckReturnValue
        protected final Optional<Exception> checkUpdate(
                @Nonnull final PointValue pointValue,
                @Nonnull final Optional<Identity> identity)
        {
            final PointEntity pointEntity = (PointEntity) pointValue
                .getPoint()
                .orElse(null);
            final Permissions.Action action;

            if ((pointEntity != null) && pointEntity.hasInputRelations()) {
                action = Permissions.Action.PROCESS;
            } else if (pointValue.isDeleted()) {
                action = Permissions.Action.DELETE;
            } else if (pointEntity != null) {
                action = Permissions.Action.WRITE;
            } else {
                action = Permissions.Action.INJECT;
            }

            final Permissions permissions;

            if (pointEntity != null) {
                permissions = pointEntity.getPermissions().orElse(null);
            } else {
                final Optional<StoreEntity> storeEntity = _storeAppImpl
                    .getStoreEntity();

                permissions = storeEntity
                    .isPresent()? storeEntity
                        .get()
                        .getPermissions()
                        .orElse(null): null;
            }

            if (permissions == null) {
                return Optional.empty();
            }

            if (permissions.check(action, identity)) {
                return Optional.empty();
            }

            final Message message = new Message(
                StoreMessages.POINT_ACTION_UNAUTHORIZED,
                action,
                pointValue.pointString());

            getThisLogger().warn(message);

            return Optional.of(new UnauthorizedAccessException(message));
        }

        /**
         * Enters updates.
         */
        protected final void enterUpdates()
        {
            lock();
        }

        /**
         * Gets the archiver.
         *
         * @return The optional archiver.
         */
        @Nonnull
        @CheckReturnValue
        protected Optional<Archiver> getArchiver()
        {
            return _getStoreAppImpl().getArchiver();
        }

        /**
         * Gets the 'deleted' traces.
         *
         * @return The 'deleted' traces.
         */
        @Nonnull
        @CheckReturnValue
        protected final Traces getDeletedTraces()
        {
            return _deletedTraces;
        }

        /**
         * Gets the polator for a point.
         *
         * @param point The point.
         *
         * @return The polator for the point.
         */
        @Nonnull
        @CheckReturnValue
        protected Polator getPolator(@Nonnull final Point point)
        {
            return _getStoreAppImpl().getPolator(point);
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
         * Gets the 'updated' traces.
         *
         * @return The 'updated' traces.
         */
        @Nonnull
        @CheckReturnValue
        protected final Traces getUpdatedTraces()
        {
            return _updatedTraces;
        }

        /**
         * Asks if started.
         *
         * @return True if started.
         */
        @CheckReturnValue
        protected final boolean isStarted()
        {
            return _started;
        }

        /**
         * Asks if stopped.
         *
         * @return True if stopped.
         */
        @CheckReturnValue
        protected final boolean isStopped()
        {
            return _stopped;
        }

        /**
         * Leaves updates.
         */
        protected final void leaveUpdates()
        {
            unlock();
        }

        /**
         * Locks.
         */
        protected final void lock()
        {
            _lock.writeLock().lock();
        }

        /**
         * Reports updates.
         *
         * @param updated The number of point values updated.
         * @param deleted The number of point values deleted.
         * @param ignored The number of updates ignored.
         * @param time The update time in nanoseconds.
         */
        protected final void reportUpdates(
                final long updated,
                final long deleted,
                final long ignored,
                final long time)
        {
            final Logger logger = getThisLogger();

            if (logger.isTraceEnabled()) {
                if (updated > 0) {
                    logger
                        .trace(StoreMessages.UPDATED, String.valueOf(updated));
                }

                if (deleted > 0) {
                    logger
                        .trace(StoreMessages.DELETED, String.valueOf(deleted));
                }

                if (ignored > 0) {
                    logger
                        .trace(StoreMessages.IGNORED, String.valueOf(ignored));
                }
            }

            getStats().addUpdates(updated, deleted, ignored, time);
        }

        /**
         * Resolves a state for a point.
         *
         * @param state The state.
         * @param point The optional point.
         *
         * @return The resolved state (may be empty).
         */
        @Nonnull
        @CheckReturnValue
        protected Optional<State> resolveForPoint(
                @Nonnull final State state,
                @Nonnull final Optional<Point> point)
        {
            final String groupName = point
                .isPresent()? point
                    .get()
                    .getParams()
                    .getString(Point.STATES_PARAM, Optional.of(""))
                    .get()
                    .trim(): "";
            final State.Group stateGroup = _stateGroups.get(groupName);

            if (stateGroup != null) {
                final Optional<Integer> stateCode = state.getCode();
                final Optional<String> stateName = state.getName();

                if (stateCode.isPresent()) {
                    if (stateName.isPresent()) {
                        return Optional.of(state);
                    }

                    return stateGroup.get(stateCode.get());
                }

                if (stateName.isPresent()) {
                    return stateGroup.get(stateName.get());
                }
            } else {
                getThisLogger()
                    .warn(StoreMessages.STATE_GROUP_UNDEFINED, groupName);
            }

            return (groupName
                .length() > 0)? resolveForPoint(
                    state,
                    Optional.empty()): Optional.empty();
        }

        /**
         * Restore point references and values for updates.
         *
         * @param updates The point values to restore.
         *
         * @return An initialized exception array.
         */
        @Nonnull
        @CheckReturnValue
        protected final Exception[] restoreUpdates(
                @Nonnull final PointValue[] updates)
        {
            final Exception[] exceptions = new Exception[updates.length];

            for (int i = 0; i < updates.length; ++i) {
                PointValue update = updates[i];

                if (update == null) {
                    exceptions[i] = new NullPointerException();

                    continue;
                }

                update = update.restore(getMetadata());

                if (update.hasPointUUID()) {
                    if (update.getPoint().isPresent()) {
                        update = update.encoded();
                    }

                    if (update.getState() instanceof State) {
                        final State state = (State) update.getState();

                        update
                            .setState(
                                resolveForPoint(state, Optional.empty())
                                    .orElse(null));

                        if (update.getState() == null) {
                            exceptions[i] = new UnresolvedStateException(state);

                            continue;
                        }
                    }

                    if (update.getValue() instanceof State) {
                        final State state = (State) update.getValue();

                        if (update == updates[i]) {
                            update = update.copy();
                        }

                        update
                            .setValue(
                                resolveForPoint(state, update.getPoint())
                                    .orElse(null));

                        if (update.getValue() == null) {
                            exceptions[i] = new UnresolvedStateException(
                                state,
                                update.pointString());

                            continue;
                        }
                    }
                } else {
                    final String pointString = update.pointString();

                    if (pointString.length() == 0) {
                        exceptions[i] = new PointUnknownException(
                            update.valueString());
                    } else {
                        getThisLogger()
                            .debug(
                                StoreMessages.POINT_UPDATE_IGNORED,
                                pointString);

                        exceptions[i] = new PointUnknownException(pointString);
                    }
                }

                updates[i] = update;
            }

            return exceptions;
        }

        /**
         * Sets up pull sleep.
         *
         * @return True on success.
         */
        @CheckReturnValue
        protected boolean setUpPullSleep()
        {
            final ElapsedTime pullSleep = _getStoreAppImpl()
                .getServerProperties()
                .getElapsed(
                    PULL_SLEEP_PROPERTY,
                    Optional.of(DEFAULT_PULL_SLEEP),
                    Optional.of(DEFAULT_PULL_SLEEP))
                .get();

            if (!SnoozeAlarm
                .validate(pullSleep, this, StoreMessages.PULL_SLEEP_TEXT)) {
                return false;
            }

            getThisLogger().info(StoreMessages.PULL_SLEEP, pullSleep);
            _pullSleep = pullSleep.toMillis();

            return true;
        }

        /**
         * Asks if the server supports the delete tracer.
         *
         * @return True if the delete tracer is supported.
         */
        @CheckReturnValue
        protected boolean supportsDeleteTracer()
        {
            return false;
        }

        /**
         * Asks if the server supports the update tracer.
         *
         * @return True if the update tracer is supported.
         */
        @CheckReturnValue
        protected boolean supportsUpdateTracer()
        {
            return false;
        }

        /**
         * Returns an appropriate response for an unknown query point.
         *
         * @param query The query.
         *
         * @return The response.
         */
        @Nonnull
        @CheckReturnValue
        protected final StoreValues unknownQueryPoint(
                @Nonnull final StoreValuesQuery query)
        {
            Object point = query.getPoint().orElse(null);

            if (point == null) {
                point = query.getPointUUID().get().undeleted();
            }

            getThisLogger().warn(StoreMessages.POINT_NOT_IN_STORE, point);

            return new StoreValues(new PointUnknownException(point));
        }

        /**
         * Unlocks.
         */
        protected final void unlock()
        {
            _lock.writeLock().unlock();
        }

        /**
         * Returns an appropriate response for unsupported pull query.
         *
         * @return The response.
         */
        @Nonnull
        @CheckReturnValue
        protected final StoreValues unsupportedPullQuery()
        {
            final Exception exception = new UnsupportedOperationException(
                BaseMessages.PULL_QUERIES_NOT_SUPPORTED.toString());

            getThisLogger().warn(BaseMessages.VERBATIM, exception.getMessage());

            return new StoreValues(exception);
        }

        /**
         * Returns a versioned value for an update.
         *
         * @param update The update.
         *
         * @return The versioned value.
         */
        @Nonnull
        @CheckReturnValue
        protected final VersionedValue versionedValue(
                @Nonnull final PointValue update)
        {
            final VersionedValue versionedValue;

            if (update instanceof ReplicatedValue) {
                versionedValue = (VersionedValue) update.copy();

                final Optional<Point> point = versionedValue.getPoint();

                if (point.isPresent()
                        && !versionedValue.isDeleted()
                        && (versionedValue.getValue() == null)
                        && point.get().isNullRemoves(_nullRemoves)) {
                    ((ReplicatedValue) versionedValue).setDeleted(true);
                }
            } else if (update.isDeleted()) {
                if (update instanceof VersionedValue.Purged) {
                    versionedValue = (VersionedValue) update.copy();
                } else {
                    versionedValue = new VersionedValue.Deleted(update);
                }
            } else {
                if (update.getValue() == null) {
                    final Optional<Point> point = update.getPoint();

                    if (point.isPresent()
                            && point.get().isNullRemoves(_nullRemoves)) {
                        versionedValue = new VersionedValue.Deleted(update);
                    } else {
                        versionedValue = new VersionedValue(update);
                    }
                } else {
                    versionedValue = new VersionedValue(update);
                }
            }

            return versionedValue;
        }

        private StoreServiceAppImpl _getStoreAppImpl()
        {
            return Require.notNull(_storeAppImpl);
        }

        /** Default pull sleep. */
        public static final ElapsedTime DEFAULT_PULL_SLEEP = ElapsedTime
            .fromMillis(60000);

        /** Traces subdirectory for deleted values. */
        public static final String DELETED_TRACES = "deleted";

        /** Name property. */
        public static final String NAME_PROPERTY = "name";

        /** Pull sleep property. */
        public static final String PULL_SLEEP_PROPERTY = "pull.sleep";

        /** States reference property. */
        public static final String STATES_PROPERTY = "states";

        /** State definition property. */
        public static final String STATE_PROPERTY = "state";

        /** Traces subdirectory for updated values. */
        public static final String UPDATED_TRACES = "updated";

        private final NavigableSet<PointBinding> _bindings =
            new TreeSet<PointBinding>();
        private final Map<String, PointBinding> _bindingsByName =
            new HashMap<String, PointBinding>();
        private final Map<UUID, PointBinding> _bindingsByUUID =
            new HashMap<UUID, PointBinding>();
        private final Traces _deletedTraces = new Traces();
        private final ReadWriteLock _lock = new ReentrantReadWriteLock(true);
        private final Logger _logger = Logger.getInstance(getClass());
        private boolean _nullRemoves;
        private long _pullSleep;
        private volatile boolean _started;
        private final Map<String, State.Group> _stateGroups = new HashMap<>();
        private volatile boolean _stopped;
        private StoreServiceAppImpl _storeAppImpl;
        private final Traces _updatedTraces = new Traces();
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
