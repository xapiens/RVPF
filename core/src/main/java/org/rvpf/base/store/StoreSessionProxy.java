/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreSessionProxy.java 4079 2019-06-12 13:40:21Z SFB $
 */

package org.rvpf.base.store;

import java.rmi.RemoteException;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.Point;
import org.rvpf.base.Points;
import org.rvpf.base.TimeInterval;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.rmi.Session;
import org.rvpf.base.rmi.SessionClientContext;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.rmi.SessionProxy;
import org.rvpf.base.tool.Externalizer;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.LoginInfo;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.State;

/**
 * Store server session proxy.
 *
 * <p>Simplifies the access to a store session.</p>
 */
@ThreadSafe
public final class StoreSessionProxy
    extends SessionProxy
    implements StoreSession
{
    /**
     * Constructs an instance.
     *
     * @param clientName The client name.
     * @param loginInfo The optional login informations.
     * @param context The session client context.
     * @param listener The optional listener.
     * @param autoconnect The autoconnect indicator.
     */
    StoreSessionProxy(
            @Nonnull final String clientName,
            @Nonnull final Optional<LoginInfo> loginInfo,
            @Nonnull final SessionClientContext context,
            @Nonnull final Optional<Listener> listener,
            final boolean autoconnect)
    {
        super(clientName, loginInfo, context, listener, autoconnect);
    }

    /**
     * Returns a new builder.
     *
     * @return The new builder.
     */
    @Nonnull
    @CheckReturnValue
    public static Builder newBuilder()
    {
        return new Builder();
    }

    /** {@inheritDoc}
     */
    @Override
    public StoreValues deliver(
            final int limit,
            final long timeout)
        throws SessionException
    {
        try {
            _setResponse(_getStoreSession().deliver(limit, timeout));
        } catch (final Exception exception) {
            throw sessionException(exception);
        }

        return getResponse().orElse(null);
    }

    /**
     * Get the exception.
     *
     * @return The optional exception.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Exception> getException()
    {
        final Optional<Exception[]> exceptions = getExceptions();

        return (exceptions.isPresent()
                && (exceptions.get().length == 1))? Optional
                    .ofNullable(
                            exceptions.get()[0]): Optional
                                    .of(new ServiceClosedException());
    }

    /**
     * Gets the exceptions.
     *
     * @return The exceptions (each null on success).
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Exception[]> getExceptions()
    {
        if (_exceptions == null) {
            final Optional<StoreValues[]> responses = getResponses();

            if (responses.isPresent()) {
                final Exception[] exceptions =
                    new Exception[responses.get().length];

                for (int i = 0; i < exceptions.length; ++i) {
                    exceptions[i] = responses
                        .get()[i]
                        .getException()
                        .orElse(null);
                }

                _exceptions = exceptions;
            }
        }

        return Optional.of(_exceptions);
    }

    /**
     * Gets point binding informations on selected points.
     *
     * @param bindingRequests Selection specifications.
     *
     * @return The point bindings informations (empty on service closed).
     *
     * @throws SessionException From session security check.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<List<PointBinding>> getPointBindings(
            @Nonnull final Collection<PointBinding.Request> bindingRequests)
        throws SessionException
    {
        final PointBinding[] pointBindings = getPointBindings(
            bindingRequests
                .toArray(new PointBinding.Request[bindingRequests.size()]));

        return (pointBindings != null)? Optional
            .of(Arrays.asList(pointBindings)): Optional.empty();
    }

    /**
     * Gets point binding informations on selected points.
     *
     * @param bindingRequest Selection specifications.
     *
     * @return The point bindings informations (empty on service closed).
     *
     * @throws SessionException From session security check.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<PointBinding[]> getPointBindings(
            @Nonnull final PointBinding.Request bindingRequest)
        throws SessionException
    {
        return Optional
            .ofNullable(
                getPointBindings(
                    new PointBinding.Request[] {bindingRequest, }));
    }

    /** {@inheritDoc}
     */
    @Override
    public PointBinding[] getPointBindings(
            final PointBinding.Request[] bindingRequests)
        throws SessionException
    {
        try {
            return _getStoreSession().getPointBindings(bindingRequests);
        } catch (final Exception exception) {
            throw sessionException(exception);
        }
    }

    /**
     * Gets the response.
     *
     * @return The response (may be empty).
     */
    @Nonnull
    @CheckReturnValue
    public Optional<StoreValues> getResponse()
    {
        final StoreValues[] responses = _responses;

        return ((responses != null)
                && (responses.length == 1))? Optional
                    .ofNullable(responses[0]): Optional.empty();
    }

    /**
     * Gets the responses.
     *
     * @return The responses (may be empty).
     */
    @Nonnull
    @CheckReturnValue
    public Optional<StoreValues[]> getResponses()
    {
        return Optional.ofNullable(_responses);
    }

    /**
     * Gets a state group by its name.
     *
     * @param name The group name (empty for anonymous).
     *
     * @return The state group (empty if absent).
     *
     * @throws SessionException From session security check.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<State.Group> getStateGroup(
            @Nonnull final Optional<String> name)
        throws SessionException
    {
        if (_stateGroupsMap == null) {
            final Map<String, State.Group> stateGroupMap = new HashMap<String,
                State.Group>();
            final State.Group[] stateGroups = getStateGroups();

            if (stateGroups == null) {
                throw new ServiceClosedException();
            }

            for (final State.Group stateGroup: stateGroups) {
                stateGroupMap.put(stateGroup.getName(), stateGroup);
            }

            _stateGroupsMap = stateGroupMap;
        }

        return Optional
            .ofNullable(
                _stateGroupsMap.get(name.isPresent()? name.get().trim(): ""));
    }

    /** {@inheritDoc}
     */
    @Override
    public State.Group[] getStateGroups()
        throws SessionException
    {
        if (_stateGroups == null) {
            try {
                _stateGroups = _getStoreSession().getStateGroups();
            } catch (final Exception exception) {
                throw sessionException(exception);
            }
        }

        return _stateGroups;
    }

    /** {@inheritDoc}
     */
    @Override
    public void impersonate(final String user)
        throws SessionException
    {
        if (hasLoginInfo()) {
            try {
                _getStoreSession().impersonate(user);
            } catch (final Exception exception) {
                throw sessionException(exception);
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void interrupt()
        throws SessionException
    {
        try {
            _getStoreSession().interrupt();
        } catch (final Exception exception) {
            throw sessionException(exception);
        }
    }

    /**
     * Iterates on the point values returned for a store query.
     *
     * <p>The iteration can extend beyond the store query limit.</p>
     *
     * <p>Note: the returned iterable and its iterator are not thread safe.</p>
     *
     * @param storeQuery The store query.
     * @param points Points metadata.
     *
     * @return An iterable on store values.
     */
    @Nonnull
    @CheckReturnValue
    public Iterable<PointValue> iterate(
            @Nonnull final StoreValuesQuery storeQuery,
            @Nonnull final Optional<Points> points)
    {
        return new ValuesIterator(storeQuery, points);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean probe()
        throws SessionException
    {
        try {
            return _getStoreSession().probe();
        } catch (final Exception exception) {
            throw sessionException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public StoreValues pull(
            final StoreValuesQuery query,
            final long timeout)
        throws SessionException
    {
        try {
            _setResponse(_getStoreSession().pull(query, timeout));
        } catch (final Exception exception) {
            throw sessionException(exception);
        }

        return getResponse().orElse(null);
    }

    /**
     * Purges point values.
     *
     * @param pointUUID The point to purge.
     * @param timeInterval A time interval.
     *
     * @return The number of point values purged.
     *
     * @throws SessionException On session exception.
     *
     * @see Store#purge(UUID[], TimeInterval)
     */
    @CheckReturnValue
    public int purge(
            @Nonnull final UUID pointUUID,
            @Nonnull final TimeInterval timeInterval)
        throws SessionException
    {
        return purge(new UUID[] {pointUUID, }, timeInterval);
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
            return _getStoreSession().purge(pointUUIDs, timeInterval);
        } catch (final Exception exception) {
            throw sessionException(exception);
        }
    }

    /**
     * Resolves a global state.
     *
     * @param state The state.
     *
     * @return The resolved state.
     *
     * @throws SessionException From session security check.
     */
    @Nullable
    @CheckReturnValue
    public State resolve(@Nonnull final State state)
        throws SessionException
    {
        return resolve(state, (UUID) null);
    }

    /**
     * Resolves a state for a point.
     *
     * @param state The state.
     * @param point The optional point.
     *
     * @return The resolved state.
     *
     * @throws SessionException From session security check.
     */
    @Nullable
    @CheckReturnValue
    public State resolve(
            @Nonnull final State state,
            @Nullable final Point point)
        throws SessionException
    {
        return resolve(
            state,
            (point != null)? point.getUUID().get(): null);
    }

    /** {@inheritDoc}
     */
    @Override
    public State resolve(
            final State state,
            final UUID pointUUID)
        throws SessionException
    {
        try {
            return _getStoreSession().resolve(state, pointUUID);
        } catch (final Exception exception) {
            throw sessionException(exception);
        }
    }

    /**
     * Selects point values.
     *
     * @param queries The store queries.
     *
     * @return A list of store values (null on service closed).
     *
     * @throws SessionException On session exception.
     */
    @Nullable
    @CheckReturnValue
    public List<StoreValues> select(
            @Nonnull final Collection<StoreValuesQuery> queries)
        throws SessionException
    {
        final StoreValues[] responses = select(
            queries.toArray(new StoreValuesQuery[queries.size()]));

        return (responses != null)? Arrays.asList(responses): null;
    }

    /**
     * Selects point values.
     *
     * @param storeQuery A store query.
     *
     * @return Store values (empty on service closed).
     *
     * @throws SessionException From session security check.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<StoreValues> select(
            @Nonnull final StoreValuesQuery storeQuery)
        throws SessionException
    {
        select(new StoreValuesQuery[] {storeQuery, });

        return getResponse();
    }

    /** {@inheritDoc}
     */
    @Override
    public StoreValues[] select(
            final StoreValuesQuery[] queries)
        throws SessionException
    {
        try {
            _setResponses(_getStoreSession().select(queries));
        } catch (final Exception exception) {
            throw sessionException(exception);
        }

        return getResponses().orElse(null);
    }

    /**
     * Subscribes to point values events.
     *
     * @param pointUUIDs The point UUIDs.
     *
     * @return The last store value for each point (null on service closed).
     *
     * @throws SessionException From session security check.
     */
    @Nullable
    @CheckReturnValue
    public List<StoreValues> subscribe(
            @Nonnull final Collection<UUID> pointUUIDs)
        throws SessionException
    {
        final StoreValues[] storeValues = subscribe(
            pointUUIDs.toArray(new UUID[pointUUIDs.size()]));

        return (storeValues != null)? Arrays.asList(storeValues): null;
    }

    /**
     * Subscribes to a point values events.
     *
     * @param pointUUID The point UUID.
     *
     * @return The last store value for the point (empty on service closed).
     *
     * @throws SessionException From session security check.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<StoreValues> subscribe(
            @Nullable final UUID pointUUID)
        throws SessionException
    {
        subscribe(new UUID[] {pointUUID, });

        return getResponse();
    }

    /** {@inheritDoc}
     */
    @Override
    public StoreValues[] subscribe(
            final UUID[] pointUUIDs)
        throws SessionException
    {
        try {
            _setResponses(_getStoreSession().subscribe(pointUUIDs));
        } catch (final Exception exception) {
            throw sessionException(exception);
        }

        return getResponses().orElse(null);
    }

    /**
     * Subscribes to point values events and check the responses.
     *
     * @param pointUUIDs The point UUIDs.
     * @param logger A logger.
     *
     * @return False when a warning has been logged.
     *
     * @throws SessionException From session security check.
     */
    @CheckReturnValue
    public boolean subscribeAndCheck(
            @Nonnull final Collection<UUID> pointUUIDs,
            @Nonnull final Logger logger)
        throws SessionException
    {
        return subscribeAndCheck(
            pointUUIDs.toArray(new UUID[pointUUIDs.size()]),
            logger);
    }

    /**
     * Subscribes to point value events and check the response.
     *
     * @param pointUUID The point UUID.
     * @param logger A logger.
     *
     * @return False when a warning has been logged.
     *
     * @throws SessionException On session exception.
     */
    @CheckReturnValue
    public boolean subscribeAndCheck(
            @Nonnull final UUID pointUUID,
            @Nonnull final Logger logger)
        throws SessionException
    {
        return subscribeAndCheck(new UUID[] {pointUUID, }, logger);
    }

    /**
     * Subscribes to point values events and check the responses.
     *
     * @param pointUUIDs The point UUIDs.
     * @param logger A logger.
     *
     * @return False when a warning has been logged.
     *
     * @throws SessionException From session security check.
     */
    @CheckReturnValue
    public boolean subscribeAndCheck(
            @Nonnull final UUID[] pointUUIDs,
            @Nonnull final Logger logger)
        throws SessionException
    {
        boolean success = true;

        if (pointUUIDs.length > 0) {
            if (logger.isTraceEnabled()) {
                Arrays
                    .stream(pointUUIDs)
                    .forEach(
                        pointUUID -> logger
                            .trace(
                                    BaseMessages.SENDING_POINT_SUBSCRIBE,
                                            pointUUID));
            }

            final StoreValues[] responses = subscribe(pointUUIDs);

            if (responses == null) {
                return false;
            }

            for (int i = 0; i < responses.length; ++i) {
                final Optional<Exception> exception = responses[i]
                    .getException();

                if (exception.isPresent()) {
                    logger
                        .warn(
                            BaseMessages.POINT_SUBSCRIBE_FAILED,
                            pointUUIDs[i],
                            exception.get());
                    success = false;
                }
            }

            if (success) {
                logger.trace(BaseMessages.POINT_SUBSCRIBES_SUCCEEDED);
            }
        } else {
            _setResponses(_EMPTY_RESPONSES);
        }

        return success;
    }

    /** {@inheritDoc}
     */
    @Override
    public String supportedValueTypeCodes()
        throws SessionException
    {
        try {
            return _getStoreSession().supportedValueTypeCodes();
        } catch (final Exception exception) {
            throw sessionException(exception);
        }
    }

    /**
     * Returns the set of supported value types.
     *
     * @return The set of supported value types.
     *
     * @throws SessionException From session security check.
     */
    @Nonnull
    @CheckReturnValue
    public EnumSet<Externalizer.ValueType> supportedValueTypes()
        throws SessionException
    {
        if (_supportedValueTypes == null) {
            final String valueTypeCodes = supportedValueTypeCodes();

            _supportedValueTypes = Externalizer.ValueType
                .stringToSet(valueTypeCodes);
        }

        return _supportedValueTypes;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsCount()
        throws SessionException
    {
        try {
            return _getStoreSession().supportsCount();
        } catch (final Exception exception) {
            throw sessionException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsDelete()
        throws SessionException
    {
        try {
            return _getStoreSession().supportsDelete();
        } catch (final Exception exception) {
            throw sessionException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsDeliver()
        throws SessionException
    {
        try {
            return _getStoreSession().supportsDeliver();
        } catch (final Exception exception) {
            throw sessionException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsPull()
        throws SessionException
    {
        try {
            return _getStoreSession().supportsPull();
        } catch (final Exception exception) {
            throw sessionException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsPurge()
        throws SessionException
    {
        try {
            return _getStoreSession().supportsPurge();
        } catch (final Exception exception) {
            throw sessionException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsSubscribe()
        throws SessionException
    {
        try {
            return _getStoreSession().supportsSubscribe();
        } catch (final Exception exception) {
            throw sessionException(exception);
        }
    }

    /**
     * Unsubscribes from point values events.
     *
     * @param pointUUIDs The point UUIDs.
     *
     * @return An exception list (null on service closed, each null on
     *         success).
     *
     * @throws SessionException From session security check.
     */
    @Nullable
    @CheckReturnValue
    public List<Exception> unsubscribe(
            @Nonnull final Collection<UUID> pointUUIDs)
        throws SessionException
    {
        final Exception[] exceptions = unsubscribe(
            pointUUIDs.toArray(new UUID[pointUUIDs.size()]));

        return (exceptions != null)? Arrays.asList(exceptions): null;
    }

    /**
     * Usubscribes from a point values events.
     *
     * @param pointUUID The point.
     *
     * @return An exception (null on success).
     *
     * @throws SessionException From session security check.
     */
    @Nullable
    @CheckReturnValue
    public Exception unsubscribe(
            @Nonnull final UUID pointUUID)
        throws SessionException
    {
        final Exception[] exceptions = unsubscribe(new UUID[] {pointUUID, });

        if (exceptions == null) {
            throw new ServiceClosedException();
        }

        return exceptions[0];
    }

    /** {@inheritDoc}
     */
    @Override
    public Exception[] unsubscribe(
            final UUID[] pointUUIDs)
        throws SessionException
    {
        try {
            _setExceptions(_getStoreSession().unsubscribe(pointUUIDs));
        } catch (final Exception exception) {
            throw sessionException(exception);
        }

        return getExceptions().orElse(null);
    }

    /**
     * Subscribes to point values events and check the responses.
     *
     * @param pointUUIDs The point UUIDs.
     * @param logger A logger.
     *
     * @return False when a warning has been logged.
     *
     * @throws SessionException From session security check.
     */
    @CheckReturnValue
    public boolean unsubscribeAndCheck(
            @Nonnull final Collection<UUID> pointUUIDs,
            @Nonnull final Logger logger)
        throws SessionException
    {
        return unsubscribeAndCheck(
            pointUUIDs.toArray(new UUID[pointUUIDs.size()]),
            logger);
    }

    /**
     * Subscribes to point value events and check the response.
     *
     * @param pointUUID The point UUID.
     * @param logger A logger.
     *
     * @return False when a warning has been logged.
     *
     * @throws SessionException On session exception.
     */
    @CheckReturnValue
    public boolean unsubscribeAndCheck(
            @Nonnull final UUID pointUUID,
            @Nonnull final Logger logger)
        throws SessionException
    {
        return unsubscribeAndCheck(new UUID[] {pointUUID, }, logger);
    }

    /**
     * Subscribes to point values events and check the responses.
     *
     * @param pointUUIDs The point UUIDs.
     * @param logger A logger.
     *
     * @return False when a warning has been logged.
     *
     * @throws SessionException From session security check.
     */
    @CheckReturnValue
    public boolean unsubscribeAndCheck(
            @Nonnull final UUID[] pointUUIDs,
            @Nonnull final Logger logger)
        throws SessionException
    {
        boolean success = true;

        if (pointUUIDs.length > 0) {
            if (logger.isTraceEnabled()) {
                Arrays
                    .stream(pointUUIDs)
                    .forEach(
                        pointUUID -> logger
                            .trace(
                                    BaseMessages.SENDING_POINT_UNSUBSCRIBE,
                                            pointUUID));
            }

            final Exception[] exceptions = unsubscribe(pointUUIDs);

            if (exceptions == null) {
                return false;
            }

            for (int i = 0; i < pointUUIDs.length; ++i) {
                if (exceptions[i] != null) {
                    logger
                        .warn(
                            BaseMessages.POINT_UNSUBSCRIBE_FAILED,
                            pointUUIDs[i],
                            exceptions[i]);
                    success = false;
                }
            }

            if (success) {
                logger.trace(BaseMessages.POINT_UNSUBSCRIBES_SUCCEEDED);
            }
        } else {
            _setExceptions(_EMPTY_EXCEPTIONS);
        }

        return success;
    }

    /**
     * Updates point values.
     *
     * @param pointValues The point values.
     *
     * @return An exception list (null on service closed, each null on
     *         success).
     *
     * @throws SessionException On session exception.
     */
    @Nullable
    @CheckReturnValue
    public List<Exception> update(
            @Nonnull final Collection<PointValue> pointValues)
        throws SessionException
    {
        final Exception[] exceptions = update(
            pointValues.toArray(new PointValue[pointValues.size()]));

        return (exceptions != null)? Arrays.asList(exceptions): null;
    }

    /**
     * Updates a point value.
     *
     * @param pointValue The point value.
     *
     * @return An optional exception.
     *
     * @throws SessionException On session exception.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Exception> update(
            @Nonnull final PointValue pointValue)
        throws SessionException
    {
        update(new PointValue[] {pointValue, });

        return getException();
    }

    /** {@inheritDoc}
     */
    @Override
    public Exception[] update(
            final PointValue[] pointValues)
        throws SessionException
    {
        _setExceptions(null);

        try {
            _setExceptions(_getStoreSession().update(pointValues));
        } catch (final Exception exception) {
            throw sessionException(exception);
        }

        return getExceptions().orElse(null);
    }

    /**
     * Updates point values and check the responses.
     *
     * @param pointValues The point values.
     * @param logger A logger.
     *
     * @return False when a warning has been logged.
     *
     * @throws SessionException On session exception.
     */
    @CheckReturnValue
    public boolean updateAndCheck(
            @Nonnull final Collection<PointValue> pointValues,
            @Nonnull final Logger logger)
        throws SessionException
    {
        return updateAndCheck(
            pointValues.toArray(new PointValue[pointValues.size()]),
            logger);
    }

    /**
     * Updates a point value and check the response.
     *
     * @param pointValue The point value.
     * @param logger A logger.
     *
     * @return False when a warning has been logged.
     *
     * @throws SessionException On session exception.
     */
    @CheckReturnValue
    public boolean updateAndCheck(
            final PointValue pointValue,
            final Logger logger)
        throws SessionException
    {
        return updateAndCheck(new PointValue[] {pointValue}, logger);
    }

    /**
     * Updates point values and check the responses.
     *
     * @param pointValues The point values.
     * @param logger A logger.
     *
     * @return False when a warning has been logged.
     *
     * @throws SessionException On session exception.
     */
    @CheckReturnValue
    public boolean updateAndCheck(
            @Nonnull final PointValue[] pointValues,
            @Nonnull final Logger logger)
        throws SessionException
    {
        boolean warned = false;

        if (pointValues.length > 0) {
            if (logger.isTraceEnabled()) {
                Arrays
                    .stream(pointValues)
                    .forEach(
                        update -> logger
                            .trace(BaseMessages.SENDING_POINT_UPDATE, update));
            }

            update(pointValues);

            final Optional<Exception[]> optionalExceptions = getExceptions();

            if (!optionalExceptions.isPresent()) {
                return false;
            }

            final Exception[] exceptions = optionalExceptions.get();

            for (int i = 0; i < pointValues.length; ++i) {
                if (exceptions[i] != null) {
                    logger
                        .warn(
                            BaseMessages.POINT_UPDATE_FAILED,
                            pointValues[i].pointString(),
                            exceptions[i]);
                    warned = true;
                }
            }

            if (!warned) {
                logger.trace(BaseMessages.POINT_UPDATES_SUCCEEDED);
            }
        } else {
            _setExceptions(_EMPTY_EXCEPTIONS);
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected Session createSession()
        throws RemoteException, SessionException
    {
        return ((StoreSessionFactory) getFactory())
            .createStoreSession(getContextUUID(), getClientName());
    }

    private StoreSession _getStoreSession()
        throws SessionException
    {
        return (StoreSession) getSession();
    }

    private void _setExceptions(final Exception[] exceptions)
    {
        _exceptions = exceptions;
        _responses = null;
    }

    private void _setResponse(final StoreValues response)
    {
        _setResponses(new StoreValues[] {response});
    }

    private void _setResponses(final StoreValues[] responses)
    {
        _responses = responses;
        _exceptions = null;
    }

    private static final Exception[] _EMPTY_EXCEPTIONS = new Exception[0];
    private static final StoreValues[] _EMPTY_RESPONSES = new StoreValues[0];

    private volatile Exception[] _exceptions;
    private volatile StoreValues[] _responses;
    private volatile State.Group[] _stateGroups;
    private volatile Map<String, State.Group> _stateGroupsMap;
    private volatile EnumSet<Externalizer.ValueType> _supportedValueTypes;

    /**
     * Builder.
     */
    public static final class Builder
        extends SessionProxy.Builder
    {
        /** {@inheritDoc}
         */
        @Override
        public StoreSessionProxy build()
        {
            if (!setUp()) {
                return null;
            }

            return new StoreSessionProxy(
                getClientName(),
                getLoginInfo(),
                getContext(),
                getListener(),
                isAutoconnect());
        }
    }


    /**
     * Values iterator.
     */
    public final class ValuesIterator
        implements Iterable<PointValue>, Iterator<PointValue>
    {
        /**
         * Constructs an instance.
         *
         * @param storeQuery The store query.
         * @param points The point definitions (optional).
         */
        ValuesIterator(
                @Nonnull final StoreValuesQuery storeQuery,
                @Nonnull final Optional<Points> points)
        {
            _storeQuery = storeQuery;
            _points = points;
        }

        /**
         * Gets the store values.
         *
         * @return The store values.
         */
        @Nonnull
        @CheckReturnValue
        public StoreValues getStoreValues()
        {
            return Require.notNull(_storeValues);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean hasNext()
        {
            boolean hasNext = (_storeValues != null)
                    && _storeValuesIterator.hasNext();

            while (!hasNext) {
                final StoreValuesQuery storeQuery;

                if (_storeValues == null) {
                    storeQuery = _storeQuery;
                } else if (_storeValues.isComplete()) {
                    break;
                } else {
                    storeQuery = _storeValues.createQuery();
                }

                try {
                    _storeValues = select(storeQuery).orElse(null);
                } catch (final SessionException exception) {
                    throw new StoreValuesQuery.IterationException(exception);
                }

                if ((_storeValues == null)
                        || Thread.currentThread().isInterrupted()) {
                    throw new StoreValuesQuery.IterationException(
                        new InterruptedException());
                }

                _storeValuesIterator = _storeValues.iterator();
                hasNext = _storeValuesIterator.hasNext();
            }

            return hasNext;
        }

        /** {@inheritDoc}
         */
        @Override
        public Iterator<PointValue> iterator()
        {
            return this;
        }

        /** {@inheritDoc}
         */
        @Override
        public PointValue next()
        {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            final Optional<Point> point = _storeQuery.getPoint();
            PointValue pointValue = _storeValuesIterator.next();

            if (point.isPresent()) {
                pointValue = pointValue.restore(point.get());
            } else if (_points.isPresent()) {
                pointValue = pointValue.restore(_points.get());
            }

            return pointValue;
        }

        /** {@inheritDoc}
         */
        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        private final Optional<Points> _points;
        private final StoreValuesQuery _storeQuery;
        private StoreValues _storeValues;
        private Iterator<PointValue> _storeValuesIterator;
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
