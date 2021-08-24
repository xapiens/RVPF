/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreSessionImpl.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.store.server.rmi;

import java.rmi.RemoteException;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Point;
import org.rvpf.base.TimeInterval;
import org.rvpf.base.UUID;
import org.rvpf.base.exception.PointUnknownException;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.exception.ThrowableProxy;
import org.rvpf.base.logger.Message;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.rmi.UnauthorizedAccessException;
import org.rvpf.base.store.PointBinding;
import org.rvpf.base.store.StoreSession;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.SnoozeAlarm;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.State;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.rmi.ExportedSessionImpl;
import org.rvpf.service.rmi.ServiceRegistry;
import org.rvpf.service.rmi.SessionFactory;
import org.rvpf.store.server.NoticeListener;
import org.rvpf.store.server.StoreMessages;
import org.rvpf.store.server.StoreServer;
import org.rvpf.store.server.StoreStats;

/**
 * Store RMI session implementation.
 */
public final class StoreSessionImpl
    extends ExportedSessionImpl
    implements StoreSession, NoticeListener
{
    /**
     * Constructs an instance.
     *
     * @param sessionFactory The factory creating this.
     * @param connectionMode The connection mode.
     * @param server The server providing access to the Store.
     * @param clientName A descriptive name for the client.
     */
    StoreSessionImpl(
            @Nonnull final SessionFactory sessionFactory,
            @Nonnull final ConnectionMode connectionMode,
            @Nonnull final StoreServer server,
            @Nonnull final String clientName)
    {
        super(clientName, sessionFactory, connectionMode);

        _server = server;

        if (_server.getStats() != null) {
            _server.getStats().sessionOpened();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void close()
    {
        synchronized (_mutex) {
            if (!isClosed()) {
                interrupt();

                if (!_subscribed.isEmpty()) {
                    _subscribed.clear();
                    _subscribed.clear();
                    _notified.clear();
                    _committed.clear();
                    _server.removeNoticeListener(this);
                }

                _clientBindings.clear();
                _unbindServer();
                _impersonate(null);

                super.close();

                final StoreStats storeStats = _server.getStats();

                if (storeStats != null) {
                    storeStats.sessionClosed();
                }
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void commit()
        throws InterruptedException
    {
        synchronized (_mutex) {
            if (!_notified.isEmpty()) {
                _committed.addAll(_notified);
                _notified.clear();
                _mutex.notifyAll();
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public StoreValues deliver(
            int limit,
            final long timeout)
        throws SessionException
    {
        final int serverLimit = _server.getResponseLimit();

        if ((serverLimit > 0) && ((limit == 0) || (limit > serverLimit))) {
            limit = serverLimit;
        }

        synchronized (_mutex) {
            try {
                securityCheck(LISTEN_ROLE);

                if (_subscribed.isEmpty()) {
                    return new StoreValues(
                        new IllegalStateException(
                            Message.format(StoreMessages.NO_SUBSCRIPTIONS)));
                }

                final StoreValues storeValues;

                interrupted();    // Clears any previous interrupt condition.

                if (timeout != 0) {
                    final long startMillis = (timeout > 0)? System
                        .currentTimeMillis(): 0;

                    while (_committed.isEmpty()
                            && !isClosed()
                            && !_interrupted) {
                        final long wait;

                        if (timeout > 0) {
                            final long elapsedMillis = System
                                .currentTimeMillis() - startMillis;

                            if ((elapsedMillis < 0)
                                    || (elapsedMillis >= timeout)) {
                                break;
                            }

                            wait = timeout - elapsedMillis;
                        } else {
                            wait = 0;
                        }

                        try {
                            _mutex.wait(wait);
                        } catch (final InterruptedException exception) {
                            interrupt();

                            break;
                        }
                    }
                }

                if (isClosed()) {
                    return null;
                }

                storeValues = new StoreValues();

                while ((limit-- > 0) && !_committed.isEmpty()) {
                    final PointValue pointValue = _committed.removeFirst();

                    storeValues.add(pointValue);
                    getThisLogger().trace(StoreMessages.DELIVERED, pointValue);
                }

                return storeValues;
            } catch (final RuntimeException|Error throwable) {
                getThisLogger()
                    .warn(
                        throwable,
                        ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                        throwable);

                throw throwable;
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public PointBinding[] getPointBindings(
            final PointBinding.Request[] bindingRequests)
        throws SessionException
    {
        try {
            securityCheck(INFO_ROLE);

            try {
                _server.disableSuspend();
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();

                return null;
            }

            try {
                return _getPointBindings(bindingRequests);
            } catch (final Exception exception) {
                throw new SessionException(new ThrowableProxy(exception));
            } finally {
                _server.enableSuspend();
            }
        } catch (final RuntimeException|Error throwable) {
            getThisLogger()
                .warn(
                    throwable,
                    ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                    throwable);

            throw throwable;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public State.Group[] getStateGroups()
        throws SessionException
    {
        try {
            securityCheck(INFO_ROLE);

            try {
                _server.disableSuspend();
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();

                return null;
            }

            final Collection<State.Group> stateGroups;

            try {
                stateGroups = _server.getStateGroups();
            } finally {
                _server.enableSuspend();
            }

            return stateGroups.toArray(new State.Group[stateGroups.size()]);
        } catch (final RuntimeException|Error throwable) {
            getThisLogger()
                .warn(
                    throwable,
                    ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                    throwable);

            throw throwable;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void impersonate(final String user)
        throws SessionException
    {
        try {
            if (getUser() == null) {
                throw new UnauthorizedAccessException(
                    StoreMessages.IMPERSONATING_NOT_AUTHENTICATED);
            }

            securityCheck(IMPERSONATE_ROLE);

            _impersonate(user);
        } catch (final RuntimeException|Error throwable) {
            getThisLogger()
                .warn(
                    throwable,
                    ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                    throwable);

            throw throwable;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void interrupt()
    {
        synchronized (_mutex) {
            try {
                _interrupted = true;
                wakeUp();
                _mutex.notifyAll();
            } catch (final RuntimeException|Error throwable) {
                getThisLogger()
                    .warn(
                        throwable,
                        ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                        throwable);

                throw throwable;
            }
        }
    }

    /**
     * Asks if this session has been interrupted. Also clears the condition.
     *
     * @return True if interrupted.
     */
    public boolean interrupted()
    {
        synchronized (_mutex) {
            final boolean interrupted = _interrupted;

            _interrupted = false;

            return interrupted || isClosed();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void notify(PointValue pointValue)
        throws InterruptedException
    {
        final UUID serverUUID = pointValue.getPointUUID();

        synchronized (_mutex) {
            if (_subscribed.contains(serverUUID)) {
                final PointBinding binding = _serverBindings.get(serverUUID);

                if (binding != null) {
                    pointValue = pointValue
                        .morph(Optional.of(binding.getUUID()));
                }

                _notified.add(pointValue);
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean probe()
        throws RemoteException, SessionException
    {
        try {
            securityCheck(INFO_ROLE);

            return _server.probe();
        } catch (final RuntimeException|Error throwable) {
            getThisLogger()
                .warn(
                    throwable,
                    ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                    throwable);

            throw throwable;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public StoreValues pull(
            StoreValuesQuery query,
            final long timeout)
        throws ServiceNotAvailableException
    {
        try {
            securityCheck(QUERY_ROLE);

            if (!query.isPull()) {
                return new StoreValues(
                    new IllegalArgumentException(
                        Message.format(StoreMessages.QUERY_IS_NOT_PULL)));
            }

            if (ServiceRegistry.isPrivate()) {
                query = _unlink(query);
            }

            if (!_clientBindings.isEmpty()) {
                query = _mapQuery(query, _clientBindings, true);
            }

            interrupted();    // Clears any previous interrupt condition.

            try {
                _server.disableSuspend();
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();

                return null;
            }

            StoreValues pointValues = null;

            try {
                pointValues = _server
                    .pull(
                        query,
                        timeout,
                        this,
                        Optional.ofNullable(getIdentity(getUser())))
                    .orElse(null);

                synchronized (_mutex) {
                    _committed.clear();
                }
            } catch (final SessionException exception) {
                throw exception;
            } catch (final Exception exception) {
                throw new SessionException(new ThrowableProxy(exception));
            } finally {
                _server.enableSuspend();
            }

            if (pointValues != null) {
                if (!_subscribed.isEmpty()) {
                    for (final Iterator<PointValue> iterator =
                            pointValues.iterator();
                            iterator.hasNext(); ) {
                        if (!_subscribed
                            .contains(iterator.next().getPointUUID())) {
                            iterator.remove();
                        }
                    }
                }

                if (!_serverBindings.isEmpty()) {
                    pointValues = _mapResponseValues(
                        pointValues,
                        _serverBindings);
                }
            }

            return pointValues;
        } catch (final RuntimeException|Error throwable) {
            getThisLogger()
                .warn(
                    throwable,
                    ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                    throwable);

            throw throwable;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public int purge(
            final UUID[] pointUUIDs,
            final TimeInterval timeInterval)
        throws SessionException
    {
        try {
            securityCheck(PURGE_ROLE);

            try {
                _server.disableSuspend();
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();

                return -1;
            }

            try {
                return _server
                    .purge(
                        pointUUIDs,
                        timeInterval,
                        Optional.ofNullable(getIdentity(getUser())));
            } catch (final SessionException exception) {
                throw exception;
            } catch (final Exception exception) {
                throw new SessionException(new ThrowableProxy(exception));
            } finally {
                _server.enableSuspend();
            }
        } catch (final RuntimeException|Error throwable) {
            getThisLogger()
                .warn(
                    throwable,
                    ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                    throwable);
            _server.getMetadata().getService().fail();

            throw throwable;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public State resolve(
            final State state,
            UUID pointUUID)
        throws SessionException
    {
        try {
            securityCheck(INFO_ROLE);

            if ((pointUUID != null) && !_clientBindings.isEmpty()) {
                final PointBinding binding = _clientBindings.get(pointUUID);

                if (binding != null) {
                    pointUUID = binding.getServerUUID();
                }
            }

            try {
                _server.disableSuspend();
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();

                return null;
            }

            try {
                return _server
                    .resolveForPointUUID(state, Optional.ofNullable(pointUUID))
                    .orElse(null);
            } finally {
                _server.enableSuspend();
            }
        } catch (final RuntimeException|Error throwable) {
            getThisLogger()
                .warn(
                    throwable,
                    ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                    throwable);

            throw throwable;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public StoreValues[] select(
            final StoreValuesQuery[] queries)
        throws SessionException
    {
        try {
            return _select(queries);
        } catch (final RuntimeException|Error throwable) {
            getThisLogger()
                .warn(
                    throwable,
                    ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                    throwable);

            throw throwable;
        }
    }

    /**
     * Sleeps for the specified number of milliseconds.
     *
     * @param millis The number of milliseconds.
     *
     * @return True unless awaken or interrupted.
     */
    public boolean sleep(final long millis)
    {
        try {
            return _snoozeAlarm.snooze(
                (millis >= 0)? ElapsedTime
                    .fromMillis(millis): ElapsedTime.INFINITY)
                   && !_interrupted
                   && !isClosed();
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
        }

        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public StoreValues[] subscribe(
            final UUID[] pointUUIDs)
        throws SessionException
    {
        synchronized (_mutex) {
            try {
                securityCheck(LISTEN_ROLE);

                // Generates a query for each subscribed point.

                final StoreValuesQuery.Builder queryBuilder = StoreValuesQuery
                    .newBuilder();
                final StoreValuesQuery[] queries =
                    new StoreValuesQuery[pointUUIDs.length];
                final StoreValues[] responses;

                for (int i = 0; i < queries.length; ++i) {
                    final UUID pointUUID = pointUUIDs[i];

                    queries[i] = (pointUUID != null)? queryBuilder
                        .setPointUUID(pointUUID)
                        .build(): null;
                }

                _server.suspendUpdates();

                try {
                    // Gets the last value for each point.
                    responses = _select(queries);

                    if (responses == null) {
                        return null;
                    }

                    final Metadata metadata = _server.getMetadata();
                    final boolean wasEmpty = _subscribed.isEmpty();

                    // Adds a subscription for each point.
                    for (int i = 0; i < pointUUIDs.length; ++i) {
                        if ((responses[i] == null)
                                || responses[i].getException().isPresent()) {
                            continue;
                        }

                        if (pointUUIDs[i] != null) {
                            final PointBinding binding = _clientBindings
                                .get(pointUUIDs[i]);
                            final Optional<Point> point = metadata
                                .getPointByUUID(
                                    (binding != null)
                                    ? binding.getServerUUID(): pointUUIDs[i]);

                            if (point.isPresent()) {
                                if (!_subscribed.add(pointUUIDs[i])) {
                                    responses[i]
                                        .setException(
                                            new IllegalStateException(
                                                Message.format(
                                                        StoreMessages.POINT_ALREADY_SUBSCRIBED,
                                                                point.get())));
                                }
                            } else {
                                responses[i]
                                    .setException(
                                        new PointUnknownException(
                                            pointUUIDs[i]));
                            }
                        } else {
                            responses[i] = new StoreValues(
                                (new NullPointerException()));
                        }
                    }

                    if (wasEmpty && !_subscribed.isEmpty()) {
                        _server.addNoticeListener(this);
                    }
                } finally {
                    _server.resumeUpdates();
                }

                return responses;
            } catch (final RuntimeException|Error throwable) {
                getThisLogger()
                    .warn(
                        throwable,
                        ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                        throwable);

                throw throwable;
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public String supportedValueTypeCodes()
        throws ServiceNotAvailableException
    {
        try {
            securityCheck(INFO_ROLE);

            return _server.supportedValueTypeCodes();
        } catch (final RuntimeException|Error throwable) {
            getThisLogger()
                .warn(
                    throwable,
                    ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                    throwable);

            throw throwable;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsCount()
        throws SessionException
    {
        try {
            securityCheck(INFO_ROLE);

            return _server.supportsCount();
        } catch (final RuntimeException|Error throwable) {
            getThisLogger()
                .warn(
                    throwable,
                    ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                    throwable);

            throw throwable;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsDelete()
        throws SessionException
    {
        try {
            securityCheck(INFO_ROLE);

            return _server.supportsDelete();
        } catch (final RuntimeException|Error throwable) {
            getThisLogger()
                .warn(
                    throwable,
                    ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                    throwable);

            throw throwable;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsDeliver()
        throws SessionException
    {
        try {
            securityCheck(INFO_ROLE);

            return _server.supportsDeliver();
        } catch (final RuntimeException|Error throwable) {
            getThisLogger()
                .warn(
                    throwable,
                    ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                    throwable);

            throw throwable;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsPull()
        throws SessionException
    {
        try {
            securityCheck(INFO_ROLE);

            return _server.supportsPull();
        } catch (final RuntimeException|Error throwable) {
            getThisLogger()
                .warn(
                    throwable,
                    ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                    throwable);

            throw throwable;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsPurge()
        throws RemoteException, SessionException
    {
        try {
            securityCheck(INFO_ROLE);

            return _server.supportsPurge();
        } catch (final RuntimeException|Error throwable) {
            getThisLogger()
                .warn(
                    throwable,
                    ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                    throwable);

            throw throwable;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsSubscribe()
        throws SessionException
    {
        try {
            securityCheck(INFO_ROLE);

            return _server.supportsSubscribe();
        } catch (final RuntimeException|Error throwable) {
            getThisLogger()
                .warn(
                    throwable,
                    ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                    throwable);

            throw throwable;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public Exception[] unsubscribe(
            final UUID[] pointUUIDs)
        throws SessionException
    {
        synchronized (_mutex) {
            try {
                securityCheck(LISTEN_ROLE);

                final Exception[] exceptions = new Exception[pointUUIDs.length];
                final Metadata metadata = _server.getMetadata();
                final boolean wasEmpty = _subscribed.isEmpty();

                _server.suspendUpdates();

                try {
                    for (int i = 0; i < pointUUIDs.length; ++i) {
                        if (pointUUIDs[i] != null) {
                            final PointBinding binding = _clientBindings
                                .get(pointUUIDs[i]);
                            final Optional<Point> point = metadata
                                .getPointByUUID(
                                    (binding != null)
                                    ? binding.getServerUUID(): pointUUIDs[i]);

                            if (point.isPresent()) {
                                if (!_subscribed.remove(pointUUIDs[i])) {
                                    exceptions[i] = new IllegalStateException(
                                        Message.format(
                                            StoreMessages.POINT_WAS_NOT_SUBSCRIBED,
                                            point.get()));
                                }
                            } else {
                                exceptions[i] = new PointUnknownException(
                                    pointUUIDs[i]);
                            }
                        } else {
                            exceptions[i] = new NullPointerException();
                        }
                    }

                    if (_subscribed.isEmpty() && !wasEmpty) {
                        _server.removeNoticeListener(this);
                        _notified.clear();
                        _committed.clear();
                    }
                } finally {
                    _server.resumeUpdates();
                }

                return exceptions;
            } catch (final RuntimeException|Error throwable) {
                getThisLogger()
                    .warn(
                        throwable,
                        ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                        throwable);

                throw throwable;
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public Exception[] update(
            PointValue[] updates)
        throws ServiceNotAvailableException
    {
        boolean deleting = false;
        boolean purging = false;
        boolean updating = false;

        for (final PointValue update: updates) {
            if (!update.isDeleted()) {
                updating = true;
            } else if (update instanceof VersionedValue.Purged) {
                purging = true;
            } else {
                deleting = true;
            }
        }

        try {
            if (updating) {
                securityCheck(UPDATE_ROLE);
            }

            if (purging) {
                securityCheck(PURGE_ROLE);
            }

            if (deleting) {
                securityCheck(DELETE_ROLE);
            }

            if (ServiceRegistry.isPrivate()) {
                updates = updates.clone();

                for (int i = 0; i < updates.length; ++i) {
                    final PointValue update = updates[i];

                    if (update != null) {
                        updates[i] = update.reset();
                    }
                }
            }

            if (!_clientBindings.isEmpty()) {
                if (!ServiceRegistry.isPrivate()) {
                    updates = updates.clone();
                }

                for (int i = 0; i < updates.length; ++i) {
                    final UUID clientUUID = updates[i].getPointUUID();
                    final PointBinding binding = _clientBindings
                        .get(clientUUID);

                    if (binding != null) {
                        updates[i] = updates[i]
                            .morph(Optional.of(binding.getServerUUID()));
                    }
                }
            }

            Exception[] exceptions;

            try {
                _server.disableSuspend();
            } catch (final InterruptedException exception) {
                exceptions = new Exception[updates.length];
                Arrays.fill(exceptions, new InterruptedException());
                Thread.currentThread().interrupt();

                return exceptions;
            }

            try {
                exceptions = _server
                    .update(
                        updates,
                        Optional.ofNullable(getIdentity(getUser())));
            } catch (final ServiceNotAvailableException exception) {
                throw exception;
            } catch (final Exception exception) {
                throw new SessionException(new ThrowableProxy(exception));
            } finally {
                _server.enableSuspend();
            }

            return exceptions;
        } catch (final RuntimeException|Error throwable) {
            getThisLogger()
                .warn(
                    throwable,
                    ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                    throwable);
            _server.getMetadata().getService().fail();

            throw throwable;
        }
    }

    /**
     * Wakes up.
     */
    public void wakeUp()
    {
        _snoozeAlarm.wakeUp();
    }

    /** {@inheritDoc}
     */
    @Override
    protected void finalize()
        throws Throwable
    {
        _unbindServer();

        super.finalize();
    }

    /** {@inheritDoc}
     */
    @Override
    protected Optional<String> getUser()
    {
        final String impersonated = _impersonated;

        return (impersonated != null)? Optional
            .ofNullable(impersonated): super.getUser();
    }

    /** {@inheritDoc}
     */
    @Override
    protected void setUser(final Optional<String> user)
    {
        if (!user.isPresent()) {
            _impersonate(null);
        }

        super.setUser(user);
    }

    /**
     * Rebinds a UUID.
     *
     * @param oldUUID The old UUID.
     * @param newUUID The new UUID.
     */
    @GuardedBy("getSessionFactory()")
    void rebind(final UUID oldUUID, final UUID newUUID)
    {
        final PointBinding oldBinding = _serverBindings.remove(oldUUID);

        if (oldBinding != null) {
            final PointBinding newBinding = new PointBinding(
                oldBinding.getName(),
                oldBinding.getUUID(),
                Optional.of(newUUID));

            _clientBindings.put(newBinding.getUUID(), newBinding);
            _serverBindings.put(newUUID, newBinding);
        }
    }

    private static StoreValuesQuery _mapQuery(
            final StoreValuesQuery originalQuery,
            final Map<UUID, PointBinding> bindings,
            final boolean toServer)
    {
        StoreValuesQuery mappedQuery = originalQuery;

        if (mappedQuery != null) {
            final Optional<UUID> originalUUID = originalQuery.getPointUUID();
            final PointBinding binding = originalUUID
                .isPresent()? bindings.get(originalUUID.get()): null;

            if (binding != null) {
                mappedQuery = StoreValuesQuery
                    .newBuilder()
                    .copyFrom(originalQuery)
                    .setPointUUID(
                        toServer? binding.getServerUUID(): binding.getUUID())
                    .build();
            }
        }

        return mappedQuery;
    }

    private static StoreValues _mapResponseValues(
            final StoreValues originalValues,
            final Map<UUID, PointBinding> bindings)
    {
        final StoreValues mappedValues = new StoreValues(
            _mapQuery(originalValues.getQuery().orElse(null), bindings, false));
        final List<PointValue> pointValues = originalValues.getPointValues();

        for (PointValue pointValue: pointValues) {
            final PointBinding binding = pointValue
                .hasPointUUID()? bindings.get(pointValue.getPointUUID()): null;

            if (binding != null) {
                pointValue = pointValue.morph(Optional.of(binding.getUUID()));
            }

            mappedValues.add(pointValue);
        }

        final Optional<StoreValuesQuery.Mark> optionalMark = originalValues
            .getMark();

        if (optionalMark.isPresent()) {
            final StoreValuesQuery.Mark mark = optionalMark.get();
            final UUID originalUUID = mark.getQueryPointUUID().orElse(null);
            final PointBinding binding = (originalUUID != null)? bindings
                .get(originalUUID): null;

            mappedValues
                .mark(
                    Optional
                        .ofNullable(
                                (binding != null)
                                ? binding.getUUID(): originalUUID),
                    mark.getStamp(),
                    mark.getDone());
        }

        return mappedValues;
    }

    private static StoreValuesQuery _unlink(final StoreValuesQuery query)
    {
        return StoreValuesQuery.newBuilder().copyFrom(query).unlink().build();
    }

    private void _bind(final PointBinding binding)
    {
        _serverBindings.put(binding.getServerUUID(), binding);
        _clientBindings.put(binding.getUUID(), binding);
    }

    private PointBinding[] _getPointBindings(
            final PointBinding.Request[] bindingRequests)
    {
        final Collection<PointBinding.Request> patternQueries =
            new LinkedList<>();
        final Collection<PointBinding.Request> bindRequests =
            new LinkedList<>();
        final Collection<PointBinding> pointBindings = new LinkedList<>();
        final Metadata metadata = _server.getMetadata();

        for (final PointBinding.Request request: bindingRequests) {
            final Optional<Pattern> pattern = request.getSelectionPattern();

            if (pattern.isPresent()) {
                patternQueries.add(request);
            } else if (request.getClientUUID().isPresent()) {
                bindRequests.add(request);
            } else {
                final Point point;
                final Optional<String> name = request.getSelectionName();

                if (name.isPresent()) {
                    point = metadata.getPointByName(name.get()).orElse(null);
                } else {
                    final Optional<UUID> uuid = request.getSelectionUUID();

                    point = (uuid
                        .isPresent())? metadata
                            .getPointByUUID(uuid.get())
                            .orElse(null): null;
                }

                if (point != null) {
                    final String pointName = point.getName().get();
                    final UUID pointUUID = point.getUUID().get();

                    pointBindings
                        .add(
                            new PointBinding(
                                pointName,
                                pointUUID,
                                Optional.empty()));
                }
            }
        }

        if (!patternQueries.isEmpty()) {
            for (final Point point: metadata.getPointsCollection()) {
                final String name = point.getName().orElse("");

                for (final PointBinding.Request query: patternQueries) {
                    final Pattern selectionPattern = query
                        .getSelectionPattern()
                        .get();

                    if (selectionPattern.matcher(name).matches()) {
                        final String pointName = point.getName().get();
                        final UUID pointUUID = point.getUUID().get();

                        pointBindings
                            .add(
                                new PointBinding(
                                    pointName,
                                    pointUUID,
                                    Optional.empty()));
                    }
                }
            }
        }

        if (!bindRequests.isEmpty()) {
            for (final Iterator<PointBinding.Request> i =
                    bindRequests.iterator();
                    i.hasNext(); ) {
                final PointBinding.Request request = i.next();
                final String selectionName = request.getSelectionName().get();
                final UUID clientUUID = request.getClientUUID().get();
                final Point point = metadata
                    .getPointByName(selectionName)
                    .orElse(null);

                if (point != null) {
                    final PointBinding binding = new PointBinding(
                        selectionName,
                        clientUUID,
                        point.getUUID());

                    _bind(binding);
                    pointBindings.add(binding);
                    getThisLogger()
                        .trace(StoreMessages.BOUND_FROM_METADATA, binding);

                    i.remove();
                }
            }
        }

        if (!bindRequests.isEmpty()) {
            final PointBinding.Request[] requests = bindRequests
                .toArray(new PointBinding.Request[bindRequests.size()]);
            final PointBinding[] bindings = _server.bind(requests);

            if (bindings != null) {
                Require.success(bindings.length == requests.length);

                for (int i = 0; i < bindings.length; ++i) {
                    final PointBinding binding = bindings[i];

                    if (binding != null) {
                        _bind(binding);
                        pointBindings.add(binding);
                        getThisLogger()
                            .trace(StoreMessages.BOUND_BY_SERVER, binding);
                    } else {
                        getThisLogger()
                            .trace(
                                StoreMessages.UNKNOWN,
                                requests[i].getSelectionName());
                    }
                }
            }
        }

        return pointBindings.toArray(new PointBinding[pointBindings.size()]);
    }

    private void _impersonate(final String user)
    {
        if (getThisLogger().isDebugEnabled()) {
            final String impersonated = _impersonated;

            if ((impersonated != null) && !impersonated.equals(user)) {
                getThisLogger()
                    .trace(
                        StoreMessages.IMPERSONATE_ENDS,
                        super.getUser().orElse(null),
                        impersonated);
            }

            if (user != null) {
                if (user.equals(impersonated)) {
                    getThisLogger()
                        .trace(
                            StoreMessages.IMPERSONATE_CONTINUES,
                            super.getUser().orElse(null),
                            user);
                } else {
                    getThisLogger()
                        .trace(
                            StoreMessages.IMPERSONATE_BEGINS,
                            super.getUser().orElse(null),
                            user);
                }
            }
        }

        _impersonated = user;
    }

    private StoreValues[] _select(
            StoreValuesQuery[] queries)
        throws SessionException
    {
        securityCheck(QUERY_ROLE);

        if (ServiceRegistry.isPrivate()) {
            queries = queries.clone();

            for (int i = 0; i < queries.length; ++i) {
                final StoreValuesQuery query = queries[i];

                if (query != null) {
                    queries[i] = _unlink(query);
                }
            }
        }

        if (!_clientBindings.isEmpty()) {
            if (!ServiceRegistry.isPrivate()) {
                queries = queries.clone();
            }

            for (int i = 0; i < queries.length; ++i) {
                queries[i] = _mapQuery(queries[i], _clientBindings, true);
            }
        }

        try {
            _server.disableSuspend();
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();

            return null;
        }

        StoreValues[] responses;

        try {
            responses = _server
                .select(queries, Optional.ofNullable(getIdentity(getUser())));
        } catch (final SessionException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new SessionException(new ThrowableProxy(exception));
        } finally {
            _server.enableSuspend();
        }

        if (ServiceRegistry.isPrivate()) {
            responses = responses.clone();
        }

        if ((responses != null) && !_serverBindings.isEmpty()) {
            if (!ServiceRegistry.isPrivate()) {
                responses = responses.clone();
            }

            for (int i = 0; i < responses.length; ++i) {
                responses[i] = _mapResponseValues(
                    responses[i],
                    _serverBindings);
            }
        }

        if (ServiceRegistry.isPrivate()) {
            for (int i = 0; i < responses.length; ++i) {
                final ListIterator<PointValue> iterator = responses[0]
                    .iterator();

                while (iterator.hasNext()) {
                    iterator.set(iterator.next());
                }
            }
        }

        return responses;
    }

    private void _unbindServer()
    {
        _server
            .unbind(
                _serverBindings
                    .values()
                    .toArray(new PointBinding[_serverBindings.size()]));
        _serverBindings.clear();
    }

    /** Update role. */
    static final String DELETE_ROLE = "Delete";

    /** Impersonate role. */
    static final String IMPERSONATE_ROLE = "Impersonate";

    /** Info role. */
    static final String INFO_ROLE = "Info";

    /** Listen role. */
    static final String LISTEN_ROLE = "Listen";

    /** Purge role. */
    static final String PURGE_ROLE = "Purge";

    /** Query role. */
    static final String QUERY_ROLE = "Query";

    /** Update role. */
    static final String UPDATE_ROLE = "Update";

    private final Map<UUID, PointBinding> _clientBindings = new HashMap<>();
    private final LinkedList<PointValue> _committed = new LinkedList<>();
    private volatile String _impersonated;
    private volatile boolean _interrupted;
    private final Object _mutex = new Object();
    private final LinkedList<PointValue> _notified = new LinkedList<>();
    private final StoreServer _server;
    private final Map<UUID, PointBinding> _serverBindings = new HashMap<>();
    private final SnoozeAlarm _snoozeAlarm = new SnoozeAlarm();
    private final Set<UUID> _subscribed = new HashSet<>();
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
