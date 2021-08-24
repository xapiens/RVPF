/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CStoreServer.java 4100 2019-06-29 17:22:34Z SFB $
 */

package org.rvpf.store.server.c;

import java.util.Optional;
import java.util.concurrent.Callable;

import org.rvpf.base.Point;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.security.Identity;
import org.rvpf.base.store.PointBinding;
import org.rvpf.base.store.PointBinding.Request;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.State;
import org.rvpf.service.ServiceMessages;
import org.rvpf.store.server.StoreCursor;
import org.rvpf.store.server.StoreMessages;
import org.rvpf.store.server.StoreServer;
import org.rvpf.store.server.archiver.Archiver;
import org.rvpf.store.server.archiver.ScheduledArchiver;

/**
 * C store server.
 */
final class CStoreServer
    extends StoreServer.Abstract
{
    /**
     * Constructs an instance.
     *
     * @param cStoreAppImpl The C store application instance.
     * @param cStore The C store instance.
     * @param notifyUpdates Notify updates.
     * @param notifyDeletes Notify deletes.
     */
    CStoreServer(
            final CStoreServiceAppImpl cStoreAppImpl,
            final CStore cStore,
            final boolean notifyUpdates,
            final boolean notifyDeletes)
    {
        _cStoreAppImpl = cStoreAppImpl;
        _cStore = cStore;
        _notifyUpdates = notifyUpdates;
        _notifyDeletes = notifyDeletes;

        _supportsThreads = _cStore.supportsThreads();
    }

    /** {@inheritDoc}
     */
    @Override
    public PointBinding[] bind(final Request[] bindRequests)
    {
        Logger.setLogID(Optional.ofNullable(_logID));

        final PointBinding[] bindings = new PointBinding[bindRequests.length];

        for (int i = 0; i < bindings.length; ++i) {
            final String name = bindRequests[i].getSelectionName().get();
            final UUID clientUUID = bindRequests[i].getClientUUID().get();
            final Optional<PointBinding> binding = getBinding(name);
            final UUID serverUUID = binding
                .isPresent()? binding
                    .get()
                    .getServerUUID(): UUID.generate().deleted();

            bindings[i] = new PointBinding(
                name,
                clientUUID,
                Optional.of(serverUUID));
        }

        final CStore cStore = _cStore;
        final CStore.Task<Boolean> task = new CStore.Task<Boolean>(
            new Callable<Boolean>()
            {
                @Override
                public Boolean call()
                {
                    return Boolean.valueOf(cStore.resolve(bindings));
                }
            });

        try {
            cStore.execute(task);

            if (task.get().booleanValue()) {
                return null;
            }
        } catch (final InterruptedException exception) {
            getThisLogger().debug(ServiceMessages.INTERRUPTED);
            Thread.currentThread().interrupt();

            return null;
        } catch (final RuntimeException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new RuntimeException(exception);
        }

        return bindings;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<Archiver> newArchiver()
    {
        return Optional.of(new ScheduledArchiver());
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<State> resolveForPointUUID(
            final State state,
            final Optional<UUID> pointUUID)
    {
        Optional<State> resolved = _resolve(state, pointUUID);

        if (resolved == null) {
            resolved = super
                .resolveForPoint(
                    state,
                    pointUUID.isPresent()? getMetadata()
                        .getPointByUUID(pointUUID.get()): Optional.empty());
        }

        return resolved;
    }

    /** {@inheritDoc}
     */
    @Override
    public StoreValues[] select(
            final StoreValuesQuery[] queries,
            final Optional<Identity> identity)
    {
        final StoreValues[] responses = new StoreValues[queries.length];

        for (int i = 0; i < queries.length; ++i) {
            StoreValuesQuery query = queries[i];

            if (query == null) {
                responses[i] = null;

                continue;
            }

            final long mark = System.nanoTime();

            final StoreValuesQuery.Builder queryBuilder = StoreValuesQuery
                .newBuilder()
                .copyFrom(query);

            if (!queryBuilder.restore(getMetadata())) {
                getThisLogger().trace(StoreMessages.QUERY_RECEIVED, query);
                responses[i] = unknownQueryPoint(query);

                continue;
            }

            query = queryBuilder.build();
            getThisLogger().trace(StoreMessages.QUERY_RECEIVED, query);
            queries[i] = query;

            if (query.isPull() && !_cStore.supportsPull()) {
                responses[i] = unsupportedPullQuery();

                continue;
            }

            final Responder responder = new Responder(
                _cStore,
                getBackEndLimit());
            final StoreCursor cursor = new StoreCursor(query, this, responder);
            final StoreValues response;

            if (_supportsThreads) {
                suspendUpdates();
            } else {
                lock();
            }

            try {
                response = cursor.createResponse(identity);
            } finally {
                if (_supportsThreads) {
                    resumeUpdates();
                } else {
                    unlock();
                }
            }

            if (getThisLogger().isTraceEnabled()) {
                if (query.isCount()) {
                    getThisLogger()
                        .trace(
                            StoreMessages.QUERY_ANSWER,
                            Long.valueOf(response.getCount()).toString());
                } else {
                    for (final PointValue pointValue: response) {
                        getThisLogger()
                            .trace(StoreMessages.QUERY_ANSWER, pointValue);
                    }
                }

                getThisLogger().trace(StoreMessages.QUERY_DONE);
            }

            getStats()
                .addQueryResponse(
                    Optional.of(response),
                    System.nanoTime() - mark);

            responses[i] = response;
        }

        getThisLogger().trace(StoreMessages.QUERIES_DONE);

        return responses;
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized void start()
    {
        _logID = Logger.currentLogID().orElse(null);

        super.start();
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        if (!isStopped()) {
            _cStoreAppImpl.onServerStop();
            _cStore.stop();

            super.stop();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public String supportedValueTypeCodes()
    {
        return _cStore.supportedValueTypeCodes();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsCount()
    {
        if (_supportsCount == null) {
            _supportsCount = Boolean.valueOf(_cStore.supportsCount());
        }

        return _supportsCount.booleanValue();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsDelete()
    {
        if (_supportsDelete == null) {
            _supportsDelete = Boolean.valueOf(_cStore.supportsDelete());
        }

        return _supportsDelete.booleanValue();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsDeliver()
    {
        if (_supportsDeliver == null) {
            _supportsDeliver = Boolean.valueOf(_cStore.supportsDeliver());
        }

        return _supportsDeliver.booleanValue();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsPull()
    {
        if (_supportsPull == null) {
            _supportsPull = Boolean
                .valueOf(
                    !_cStoreAppImpl.isPullDisabled() && _cStore.supportsPull());
        }

        return _supportsPull.booleanValue();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsSubscribe()
    {
        if (_supportsSubscribe == null) {
            _supportsSubscribe = Boolean.valueOf(_cStore.supportsSubscribe());
        }

        return _supportsSubscribe.booleanValue();
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        _cStore.dispose();

        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    public void unbind(final PointBinding[] bindings)
    {
        Logger.setLogID(Optional.ofNullable(_logID));

        final CStore cStore = _cStore;
        final CStore.Task<Boolean> task = new CStore.Task<Boolean>(
            new Callable<Boolean>()
            {
                @Override
                public Boolean call()
                {
                    return Boolean.valueOf(cStore.forget(bindings));
                }
            });

        try {
            cStore.execute(task);
            task.get();
        } catch (final InterruptedException exception) {
            getThisLogger().debug(ServiceMessages.INTERRUPTED);
            Thread.currentThread().interrupt();
        } catch (final RuntimeException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public Exception[] update(
            final PointValue[] updates,
            final Optional<Identity> identity)
    {
        final Exception[] exceptions = restoreUpdates(updates);
        final Updater updater = new Updater(_cStore);
        final long mark = System.nanoTime();

        enterUpdates();

        try {
            long ignored = 0;
            long updated = 0;
            long deleted = 0;

            for (int i = 0; i < updates.length; i++) {
                if (exceptions[i] != null) {
                    ++ignored;

                    continue;
                }

                final PointValue versionedValue = versionedValue(updates[i]);

                exceptions[i] = checkUpdate(versionedValue, identity)
                    .orElse(null);

                if (exceptions[i] != null) {
                    ++ignored;

                    continue;
                }

                updater.update(versionedValue);
            }

            try {
                updater.commit();
            } catch (final Status.FailedException exception) {
                throw new RuntimeException(exception);
            }

            synchronized (_cStore) {
                for (final PointValue update: updater.getDeleted()) {
                    if (update.isDeleted()) {
                        ++deleted;
                        getDeletedTraces().add(update);
                        getReplicator().replicate(update);

                        if (_notifyDeletes) {
                            addNotice(update);
                        }

                        getThisLogger()
                            .trace(StoreMessages.UPDATER_DELETED, update);
                    } else {
                        ++ignored;
                        getThisLogger()
                            .trace(StoreMessages.UPDATER_IGNORED, update);
                    }
                }

                getDeletedTraces().commit();

                for (final PointValue update: updater.getUpdated()) {
                    ++updated;
                    getUpdatedTraces().add(update);
                    getReplicator().replicate(update);

                    if (_notifyUpdates) {
                        addNotice(update);
                    }

                    getThisLogger()
                        .trace(StoreMessages.UPDATER_UPDATED, update);
                }

                getUpdatedTraces().commit();
                getReplicator().commit();

                if (_notifyUpdates || _notifyDeletes) {
                    sendNotices();
                }
            }

            _cStoreAppImpl.wakeUpNotifier();

            reportUpdates(updated, deleted, ignored, System.nanoTime() - mark);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new RuntimeException(exception);
        } finally {
            leaveUpdates();
        }

        return exceptions;
    }

    /** {@inheritDoc}
     */
    @Override
    protected Optional<State> resolveForPoint(
            final State state,
            final Optional<Point> point)
    {
        final Optional<State> resolved = _resolve(
            state,
            point.isPresent()? point.get().getUUID(): null);

        return resolved
            .isPresent()? resolved: super.resolveForPoint(state, point);
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean supportsDeleteTracer()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean supportsUpdateTracer()
    {
        return true;
    }

    /**
     * Gets the C store.
     *
     * @return The C store.
     */
    CStore getCStore()
    {
        return _cStore;
    }

    private Optional<State> _resolve(
            final State state,
            final Optional<UUID> pointUUID)
    {
        Optional<String> name = state.getName();
        Optional<Integer> code = state.getCode();

        if (code.isPresent()) {
            if (name.isPresent()) {
                return Optional.of(state);
            }

            if (pointUUID.isPresent()) {
                final int serverHandle = _cStore
                    .getServerHandle(pointUUID.get());

                if (serverHandle != 0) {
                    name = _cStore.getStateName(serverHandle, code.get());
                }
            } else {
                name = _cStore.getQualityName(code.get());
            }

            if (name.isPresent()) {
                return Optional.of(new State(code, name));
            }
        } else if (name.isPresent()) {
            if (pointUUID.isPresent()) {
                final int serverHandle = _cStore
                    .getServerHandle(pointUUID.get());

                if (serverHandle != 0) {
                    code = _cStore.getStateCode(serverHandle, name.get());
                }
            } else {
                code = Optional.ofNullable(_cStore.getQualityCode(name.get()));
            }

            if (code.isPresent()) {
                return Optional.of(new State(code, name));
            }
        }

        return Optional.empty();
    }

    private final CStore _cStore;
    private final CStoreServiceAppImpl _cStoreAppImpl;
    private String _logID;
    private final boolean _notifyDeletes;
    private final boolean _notifyUpdates;
    private volatile Boolean _supportsCount;
    private volatile Boolean _supportsDelete;
    private volatile Boolean _supportsDeliver;
    private volatile Boolean _supportsPull;
    private volatile Boolean _supportsSubscribe;
    private final boolean _supportsThreads;
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
