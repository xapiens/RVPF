/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RMIStore.java 4057 2019-06-04 18:44:38Z SFB $
 */

package org.rvpf.store.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import org.rvpf.base.Params;
import org.rvpf.base.Point;
import org.rvpf.base.TimeInterval;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Message;
import org.rvpf.base.rmi.RegistryEntry;
import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.rmi.SessionProxy;
import org.rvpf.base.store.PointBinding;
import org.rvpf.base.store.Store;
import org.rvpf.base.store.StoreAccessException;
import org.rvpf.base.store.StoreSessionProxy;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.util.container.KeyedValues;
import org.rvpf.base.value.PointValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.ProxyEntity;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.rmi.ServiceRegistry;

/**
 * RMI store client.
 *
 * <p>This class provides a client side access to an RMI Store.</p>
 */
public final class RMIStore
    extends AbstractStore
    implements StoreSessionProxy.Listener
{
    /** {@inheritDoc}
     */
    @Override
    public boolean canConfirm()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized void close()
    {
        if (_sessionProxy != null) {
            _sessionProxy.disconnect();
            _sessionProxy = null;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean confirm(
            final PointValue pointValue,
            final boolean confirmValue)
        throws InterruptedException, StoreAccessException
    {
        final Optional<Point> point = pointValue.getPoint();
        int retries = 0;
        boolean confirmed;

        for (;;) {
            final StoreValuesQuery.Builder storeQueryBuilder = point
                .isPresent()? StoreValuesQuery
                    .newBuilder()
                    .setPoint(
                        point.get()): StoreValuesQuery
                                .newBuilder()
                                .setPointUUID(pointValue.getPointUUID());

            storeQueryBuilder.setAt(pointValue.getStamp());

            final StoreValues storeValues = select(storeQueryBuilder.build());

            if (storeValues == null) {
                throw accessException(new ServiceClosedException());
            }

            final Optional<PointValue> storeValue = storeValues.getPointValue();
            final boolean missing = !storeValue.isPresent()
                    || storeValue.get().isDeleted();

            if (missing) {
                confirmed = pointValue.isDeleted();
            } else if (pointValue.isDeleted()) {
                confirmed = false;
            } else if (confirmValue) {
                confirmed = storeValue.get().sameValueAs(pointValue);
            } else {
                confirmed = true;
            }

            if (confirmed || (retries >= getConfirmRetries())) {
                break;
            }

            if (retries == 0) {
                getThisLogger()
                    .debug(ServiceMessages.CONFIRM_RETRY, pointValue);
            }

            Thread.sleep(getConfirmRetryDelay());
            ++retries;
        }

        if (confirmed) {
            getThisLogger().trace(ServiceMessages.CONFIRMED, pointValue);
        } else {
            getThisLogger().debug(ServiceMessages.CONFIRM_FAILED, pointValue);
        }

        return confirmed;
    }

    /** {@inheritDoc}
     */
    @Override
    public void connect()
        throws StoreAccessException
    {
        final StoreSessionProxy sessionProxy = _sessionProxy;

        if (sessionProxy == null) {
            throw accessException(new ServiceClosedException());
        }

        try {
            sessionProxy.connect();
        } catch (final SessionException exception) {
            throw accessException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public StoreValues deliver(
            final int limit,
            final long timeout)
        throws InterruptedException, StoreAccessException
    {
        if (!supportsDeliver()) {
            return super.deliver(limit, timeout);
        }

        final StoreSessionProxy sessionProxy = _sessionProxy;
        final StoreValues response;

        if (sessionProxy != null) {
            try {
                response = sessionProxy.deliver(limit, timeout);
            } catch (final SessionException exception) {
                throw accessException(exception);
            }
        } else {
            response = null;
        }

        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        if (response == null) {
            throw accessException(new ServiceClosedException());
        }

        _acceptQueryResponse(Optional.empty(), response);

        return response;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<Exception[]> getExceptions()
    {
        return (_sessionProxy != null)? _sessionProxy
            .getExceptions(): Optional.empty();
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<StoreValues[]> getSubscribedValues()
    {
        return (_sessionProxy != null)? _sessionProxy
            .getResponses(): Optional.empty();
    }

    /** {@inheritDoc}
     */
    @Override
    public void impersonate(
            final Optional<String> user)
        throws StoreAccessException
    {
        final StoreSessionProxy sessionProxy = _sessionProxy;

        if (sessionProxy != null) {
            try {
                sessionProxy.impersonate(user.orElse(null));
            } catch (final SessionException exception) {
                throw accessException(exception);
            }
        } else {
            throw accessException(new ServiceClosedException());
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public Iterable<PointValue> iterate(final StoreValuesQuery query)
    {
        return _sessionProxy.iterate(query, Optional.of(getMetadata()));
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<StoreValues> nextValues()
        throws InterruptedException, StoreAccessException
    {
        if (!_queries.isEmpty()) {
            final List<StoreValuesQuery> queries =
                new LinkedList<StoreValuesQuery>();

            for (final StoreValuesQuery query: _queries) {
                getThisLogger().trace(ServiceMessages.SENDING_QUERY, query);
                queries.add(query);

                if (queries.size() >= getQueriesBatchLimit()) {
                    _sendQueries(
                        queries.toArray(new StoreValuesQuery[queries.size()]));
                    queries.clear();
                }
            }

            if (!queries.isEmpty()) {
                _sendQueries(
                    queries.toArray(new StoreValuesQuery[queries.size()]));
            }

            _queries.clear();
        }

        return Optional
            .ofNullable(
                !_queriesResponses.isEmpty()
                ? _queriesResponses.removeFirst(): null);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onSessionConnected(final SessionProxy sessionProxy)
    {
        try {
            bindPoints();
        } catch (final InterruptedException exception) {
            getThisLogger().debug(ServiceMessages.INTERRUPTED);
            Thread.currentThread().interrupt();

            return false;
        } catch (final StoreAccessException exception) {
            getThisLogger().warn(exception, ServiceMessages.BIND_POINTS_FAILED);

            return false;
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void onSessionDisconnected(final SessionProxy sessionProxy) {}

    /** {@inheritDoc}
     */
    @Override
    public boolean probe()
        throws StoreAccessException
    {
        final StoreSessionProxy sessionProxy = _sessionProxy;

        if (sessionProxy == null) {
            throw accessException(new ServiceClosedException());
        }

        try {
            return sessionProxy.probe();
        } catch (final SessionException exception) {
            throw accessException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public StoreValues pull(
            final StoreValuesQuery query,
            final long timeout)
        throws InterruptedException, StoreAccessException
    {
        if (!supportsPull()) {
            return super.pull(query, timeout);
        }

        final StoreSessionProxy sessionProxy = _sessionProxy;
        final StoreValues response;

        if (sessionProxy != null) {
            getThisLogger().trace(ServiceMessages.PULLING_VALUES);

            try {
                response = sessionProxy.pull(query, timeout);
            } catch (final SessionException exception) {
                throw accessException(exception);
            }
        } else {
            response = null;
        }

        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        if (response == null) {
            throw accessException(new ServiceClosedException());
        }

        _acceptQueryResponse(Optional.empty(), response);

        return response;
    }

    /** {@inheritDoc}
     */
    @Override
    public int purge(
            final UUID[] pointUUIDs,
            final TimeInterval timeInterval)
        throws StoreAccessException
    {
        if (!supportsPurge()) {
            return super.purge(pointUUIDs, timeInterval);
        }

        final StoreSessionProxy sessionProxy = _sessionProxy;
        final int purged;

        if (sessionProxy == null) {
            throw accessException(new ServiceClosedException());
        }

        try {
            purged = sessionProxy.purge(pointUUIDs, timeInterval);
        } catch (final SessionException exception) {
            throw accessException(exception);
        }

        return purged;
    }

    /** {@inheritDoc}
     */
    @Override
    public StoreValues select(
            final StoreValuesQuery query)
        throws InterruptedException, StoreAccessException
    {
        if (query.isCount() && !supportsCount()) {
            return new StoreValues(
                new UnsupportedOperationException(
                    Message.format(
                        ServiceMessages.STORE_CANT_COUNT,
                        getName()).toString()));
        }

        final StoreSessionProxy sessionProxy = _sessionProxy;
        final Optional<StoreValues> response;

        if (sessionProxy != null) {
            getThisLogger().trace(ServiceMessages.SENDING_QUERY, query);

            try {
                response = sessionProxy.select(query);
            } catch (final SessionException exception) {
                throw accessException(exception);
            }
        } else {
            response = Optional.empty();
        }

        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        if (!response.isPresent()) {
            throw accessException(new ServiceClosedException());
        }

        _acceptQueryResponse(Optional.of(query), response.get());

        return response.get();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean sendUpdates(
            final Collection<PointValue> updates)
        throws StoreAccessException
    {
        final boolean success;

        final StoreSessionProxy sessionProxy = _sessionProxy;

        if (sessionProxy != null) {
            try {
                success = sessionProxy.updateAndCheck(updates, getThisLogger());
            } catch (final SessionException exception) {
                throw accessException(exception);
            }

            updates.clear();
        } else {
            throw accessException(new ServiceClosedException());
        }

        return success;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Metadata metadata, final ProxyEntity proxyEntity)
    {
        if (!super.setUp(metadata, proxyEntity)) {
            return false;
        }

        final Params params = getParams();
        final Optional<String> securityParam = getParams()
            .getString(SECURITY_PARAM);
        final KeyedGroups securityProperties = securityParam
            .isPresent()? metadata
                .getPropertiesGroup(
                    securityParam.get()): KeyedGroups.MISSING_KEYED_GROUP;
        final RegistryEntry registryEntry = RegistryEntry
            .newBuilder()
            .setBinding(params.getString(BINDING_PARAM))
            .setName(params.getString(NAME_PARAM, proxyEntity.getName()))
            .setDefaultName(Store.DEFAULT_STORE_NAME)
            .setDefaultRegistryAddress(ServiceRegistry.getRegistryAddress())
            .setDefaultRegistryPort(ServiceRegistry.getRegistryPort())
            .build();

        _sessionProxy = (StoreSessionProxy) StoreSessionProxy
            .newBuilder()
            .setRegistryEntry(registryEntry)
            .setConfigProperties(metadata.getProperties())
            .setSecurityProperties(securityProperties)
            .setLoginUser(params.getString(USER_PARAM))
            .setLoginPassword(params.getPassword(PASSWORD_PARAM))
            .setAutoconnect(true)
            .setListener(this)
            .setClientName(metadata.getServiceName())
            .setClientLogger(getThisLogger())
            .build();

        if (_sessionProxy == null) {
            return false;
        }

        setResponseLimit(metadata);
        setQueriesBatchLimit(metadata);
        setConfirmRetries();

        getThisLogger().debug(ServiceMessages.SET_UP_COMPLETED);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean subscribe(final UUID[] points)
        throws StoreAccessException
    {
        if (!supportsSubscribe()) {
            return super.subscribe(points);
        }

        final StoreSessionProxy sessionProxy = _sessionProxy;

        if (sessionProxy != null) {
            try {
                return sessionProxy.subscribeAndCheck(points, getThisLogger());
            } catch (final SessionException exception) {
                throw accessException(exception);
            }
        }

        throw accessException(new ServiceClosedException());
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsCount()
        throws StoreAccessException
    {
        if (_supportsCount == null) {
            final StoreSessionProxy sessionProxy = _sessionProxy;

            if (sessionProxy == null) {
                throw accessException(new ServiceClosedException());
            }

            try {
                _supportsCount = Boolean.valueOf(sessionProxy.supportsCount());
            } catch (final SessionException exception) {
                throw accessException(exception);
            }
        }

        return _supportsCount.booleanValue();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsDelete()
        throws StoreAccessException
    {
        if (_supportsDelete == null) {
            final StoreSessionProxy sessionProxy = _sessionProxy;

            if (sessionProxy == null) {
                throw accessException(new ServiceClosedException());
            }

            try {
                _supportsDelete = Boolean
                    .valueOf(sessionProxy.supportsDelete());
            } catch (final SessionException exception) {
                throw accessException(exception);
            }
        }

        return _supportsDelete.booleanValue();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsDeliver()
        throws StoreAccessException
    {
        if (_supportsDeliver == null) {
            final StoreSessionProxy sessionProxy = _sessionProxy;

            if (sessionProxy == null) {
                throw accessException(new ServiceClosedException());
            }

            try {
                _supportsDeliver = Boolean
                    .valueOf(sessionProxy.supportsDeliver());
            } catch (final SessionException exception) {
                throw accessException(exception);
            }
        }

        return _supportsDeliver.booleanValue();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsPull()
        throws StoreAccessException
    {
        if (_supportsPull == null) {
            final StoreSessionProxy sessionProxy = _sessionProxy;

            if (sessionProxy == null) {
                throw accessException(new ServiceClosedException());
            }

            try {
                _supportsPull = Boolean.valueOf(sessionProxy.supportsPull());
            } catch (final SessionException exception) {
                throw accessException(exception);
            }
        }

        return _supportsPull.booleanValue();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsSubscribe()
        throws StoreAccessException
    {
        if (_supportsSubscribe == null) {
            final StoreSessionProxy sessionProxy = _sessionProxy;

            if (sessionProxy == null) {
                throw accessException(new ServiceClosedException());
            }

            try {
                _supportsSubscribe = Boolean
                    .valueOf(sessionProxy.supportsSubscribe());
            } catch (final SessionException exception) {
                throw accessException(exception);
            }
        }

        return _supportsSubscribe.booleanValue();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean unsubscribe(final UUID[] points)
        throws StoreAccessException
    {
        if (!supportsSubscribe()) {
            return super.unsubscribe(points);
        }

        final StoreSessionProxy sessionProxy = _sessionProxy;

        if (sessionProxy == null) {
            throw accessException(new ServiceClosedException());
        }

        try {
            return sessionProxy.unsubscribeAndCheck(points, getThisLogger());
        } catch (final SessionException exception) {
            throw accessException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected void bindPoints(
            final Set<Point> points)
        throws InterruptedException, StoreAccessException
    {
        final StoreSessionProxy sessionProxy = _sessionProxy;

        if (sessionProxy == null) {
            throw accessException(new ServiceClosedException());
        }

        final Map<String, PointBinding.Request> requests = new HashMap<>(
            KeyedValues.hashCapacity(points.size()));

        getThisLogger().debug(ServiceMessages.BINDING_POINTS);

        for (final Point point: points) {
            final UUID pointUUID = point.getUUID().get();
            final PointBinding.Request.Builder requestBuilder =
                PointBinding.Request
                    .newBuilder();
            final String tag = point
                .getParams()
                .getString(Point.TAG_PARAM, point.getName())
                .orElse(null);

            requestBuilder.selectName(tag);
            requestBuilder.bindTo(pointUUID);

            final PointBinding.Request request = requestBuilder.build();

            requests.put(request.getSelectionName().get(), request);
        }

        final PointBinding[] bindings;

        try {
            bindings = sessionProxy
                .getPointBindings(
                    requests
                        .values()
                        .toArray(new PointBinding.Request[requests.size()]));
        } catch (final SessionException exception) {
            throw accessException(exception);
        }

        if ((bindings == null) || Thread.interrupted()) {
            throw new InterruptedException();
        }

        for (final PointBinding binding: bindings) {
            if (requests.remove(binding.getName()) == null) {
                getThisLogger()
                    .warn(ServiceMessages.BIND_UNEXPECTED, binding.getName());
            }
        }

        for (final String name: requests.keySet()) {
            getThisLogger().warn(ServiceMessages.BIND_NAME_FAILED, name);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doAddQuery(final StoreValuesQuery query)
    {
        _queries.add(query);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void reset()
    {
        _queries.clear();
        _queriesResponses.clear();

        super.reset();
    }

    /** {@inheritDoc}
     */
    @Override
    protected String supportedValueTypeCodes()
        throws StoreAccessException
    {
        if (_supportedValueTypeCodes == null) {
            final StoreSessionProxy sessionProxy = _sessionProxy;

            if (sessionProxy == null) {
                throw accessException(new ServiceClosedException());
            }

            try {
                _supportedValueTypeCodes = sessionProxy
                    .supportedValueTypeCodes();
            } catch (final SessionException exception) {
                throw accessException(exception);
            }
        }

        return _supportedValueTypeCodes;
    }

    private void _acceptQueryResponse(
            final Optional<StoreValuesQuery> query,
            final StoreValues response)
    {
        if (response.isSuccess()) {
            final Point point = query
                .isPresent()? query.get().getPoint().orElse(null): null;

            for (final ListIterator<PointValue> iterator = response.iterator();
                    iterator.hasNext(); ) {
                PointValue pointValue = iterator.next();

                if (point != null) {
                    pointValue = pointValue.restore(point);
                } else {
                    pointValue = pointValue.restore(getMetadata());
                }

                iterator.set(pointValue);
                getThisLogger()
                    .trace(ServiceMessages.RECEIVED_VALUE, pointValue);
            }

            if (response.getCount() >= 0) {
                getThisLogger()
                    .trace(
                        ServiceMessages.RECEIVED_COUNT,
                        Long.valueOf(response.getCount()));
            }

            response.setQuery(query);
        } else {
            getThisLogger()
                .warn(
                    response.getException().get(),
                    ServiceMessages.STORE_REJECTED_QUERY,
                    response.getException().get().getMessage());
        }
    }

    private void _sendQueries(
            @Nonnull final StoreValuesQuery[] queries)
        throws InterruptedException, StoreAccessException
    {
        final StoreSessionProxy sessionProxy = _sessionProxy;
        final StoreValues[] responses;

        if (sessionProxy != null) {
            try {
                responses = sessionProxy.select(queries);
            } catch (final SessionException exception) {
                throw accessException(exception);
            }
        } else {
            responses = null;
        }

        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        if (responses == null) {
            throw accessException(new ServiceClosedException());
        }

        for (int i = 0; i < queries.length; ++i) {
            final StoreValues response = responses[i];

            if (response != null) {
                final StoreValuesQuery query = queries[i];

                _acceptQueryResponse(Optional.of(query), response);

                if (!response.isEmpty() || query.isCount()) {
                    _queriesResponses.add(response);
                }
            }
        }
    }

    private final List<StoreValuesQuery> _queries = new LinkedList<>();
    private final LinkedList<StoreValues> _queriesResponses =
        new LinkedList<>();
    private volatile StoreSessionProxy _sessionProxy;
    private volatile String _supportedValueTypeCodes;
    private volatile Boolean _supportsCount;
    private volatile Boolean _supportsDelete;
    private volatile Boolean _supportsDeliver;
    private volatile Boolean _supportsPull;
    private volatile Boolean _supportsSubscribe;
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
