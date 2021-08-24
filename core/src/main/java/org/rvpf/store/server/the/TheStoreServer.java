/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TheStoreServer.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.store.server.the;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.Point;
import org.rvpf.base.TimeInterval;
import org.rvpf.base.UUID;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Messages;
import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.security.Identity;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Externalizer;
import org.rvpf.base.util.container.ConcurrentIdentityHashSet;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.store.server.StoreMessages;
import org.rvpf.store.server.StoreServer;
import org.rvpf.store.server.archiver.Archiver;
import org.rvpf.store.server.rmi.StoreSessionImpl;

/**
 * TheStore server implementation.
 */
final class TheStoreServer
    extends StoreServer.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    public Optional<Archiver> newArchiver()
    {
        return Optional.of(_backEnd.newArchiver());
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<StoreValues> pull(
            final StoreValuesQuery query,
            final long timeout,
            final StoreSessionImpl storeSession,
            final Optional<Identity> identity)
        throws SessionException
    {
        Optional<StoreValues> storeValues;

        _pullingSessions.add(storeSession);

        try {
            do {
                storeValues = super.pull(query, 0, storeSession, identity);

                if ((timeout == 0)
                        || (storeValues.isPresent()
                            && !storeValues.get().isEmpty())
                        || storeSession.interrupted()) {
                    break;
                }

                storeSession.sleep(-1);
            } while (!storeSession.interrupted());
        } finally {
            _pullingSessions.remove(storeSession);
        }

        return storeValues;
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
        return checkPurge(
            pointUUIDs,
            identity)? _backEnd.purge(pointUUIDs, timeInterval): 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public StoreValues[] select(
            final StoreValuesQuery[] queries,
            final Optional<Identity> identity)
        throws SessionException
    {
        final StoreValues[] responses = new StoreValues[queries.length];

        suspendUpdates();

        try {
            if (_backEnd == null) {
                throw new ServiceClosedException();
            }

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

                final StoreValues response = _backEnd
                    .createResponse(query, identity);

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
        } catch (final RuntimeException exception) {
            getThisLogger()
                .warn(exception, BaseMessages.VERBATIM, exception.getMessage());

            throw exception;
        } catch (final ServiceClosedException exception) {
            getThisLogger()
                .trace(
                    exception,
                    BaseMessages.VERBATIM,
                    exception.getMessage());

            throw exception;
        } finally {
            resumeUpdates();
        }

        getThisLogger().trace(StoreMessages.QUERIES_DONE);

        return responses;
    }

    /** {@inheritDoc}
     */
    @Override
    public void sendNotices()
        throws InterruptedException
    {
        super.sendNotices();

        for (final StoreSessionImpl storeSession: _pullingSessions) {
            storeSession.wakeUp();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
    {
        try {
            _backEnd.open();
        } catch (final ServiceNotAvailableException exception) {
            getThisLogger()
                .error(
                    exception.getCause(),
                    BaseMessages.VERBATIM,
                    exception.getMessage());
            _theStoreAppImpl.fail();
        }

        super.start();
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        if (_backEnd != null) {
            enterUpdates();

            try {
                _backEnd.close();
            } finally {
                leaveUpdates();
            }

            _backEnd = null;
        }

        super.stop();
    }

    /** {@inheritDoc}
     */
    @Override
    public String supportedValueTypeCodes()
    {
        return Externalizer.ValueType
            .setToString(EnumSet.allOf(Externalizer.ValueType.class));
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsCount()
    {
        return _backEnd.supportsCount();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsPull()
    {
        return !(_snapshot || _theStoreAppImpl.isPullDisabled());
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsPurge()
    {
        return _backEnd.supportsPurge();
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        stop();

        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    public Exception[] update(
            final PointValue[] updates,
            final Optional<Identity> identity)
        throws ServiceNotAvailableException
    {
        final long mark = System.nanoTime();
        final Exception[] exceptions = restoreUpdates(updates);
        final Optional<Archiver> archiver = getArchiver();

        enterUpdates();

        try {
            if (_backEnd == null) {
                return null;
            }

            boolean done = false;
            long ignored = 0;
            long updated = 0;
            long deleted = 0;

            _backEnd.beginUpdates();

            try {
                for (int i = 0; i < updates.length; i++) {
                    if (exceptions[i] != null) {
                        ++ignored;

                        continue;
                    }

                    final VersionedValue versionedValue = versionedValue(
                        updates[i]);
                    final Messages.Entry actionMessage;
                    boolean notify = true;

                    exceptions[i] = checkUpdate(versionedValue, identity)
                        .orElse(null);

                    if (exceptions[i] != null) {
                        ++ignored;

                        continue;
                    }

                    if (versionedValue.isDeleted()) {
                        if (_backEnd.delete(versionedValue) > 0) {
                            ++deleted;
                            getDeletedTraces().add(versionedValue);

                            if (versionedValue
                                    instanceof VersionedValue.Purged) {
                                actionMessage = StoreMessages.UPDATER_DELETED;
                                notify = false;
                            } else {
                                actionMessage = StoreMessages.UPDATER_HIDDEN;
                            }
                        } else {
                            actionMessage = StoreMessages.UPDATER_IGNORED;
                            ++ignored;
                            notify = false;
                        }
                    } else {
                        _backEnd.update(versionedValue);
                        ++updated;
                        getUpdatedTraces().add(versionedValue);
                        actionMessage = StoreMessages.UPDATER_UPDATED;
                    }

                    getThisLogger().trace(actionMessage, versionedValue);

                    if (notify) {
                        getReplicator().replicate(versionedValue);
                        addNotice(versionedValue);
                    }

                    if (archiver.isPresent()) {
                        final Optional<Point> point = versionedValue.getPoint();

                        if (point.isPresent()) {
                            archiver.get().archive(point.get());
                        }
                    }
                }

                done = true;

                _backEnd.commit();
                getReplicator().commit();
                sendNotices();
                getUpdatedTraces().commit();
                getDeletedTraces().commit();

                if (archiver.isPresent()) {
                    archiver.get().commit();
                }
            } finally {
                if (!done) {
                    _backEnd.rollback();
                }

                _backEnd.endUpdates();
            }

            reportUpdates(updated, deleted, ignored, System.nanoTime() - mark);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new RuntimeException(exception);
        } catch (final RuntimeException exception) {
            getThisLogger()
                .warn(exception, BaseMessages.VERBATIM, exception.getMessage());

            throw exception;
        } catch (final Exception exception) {
            getThisLogger()
                .trace(
                    exception,
                    BaseMessages.VERBATIM,
                    exception.getMessage());

            throw exception;
        } finally {
            leaveUpdates();
        }

        return exceptions;
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean setUpPullSleep()
    {
        return true;
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
     * Sets up for processing.
     *
     * @param theStoreAppImpl The store service application.
     * @param backEnd The back-end.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUp(
            @Nonnull final TheStoreServiceAppImpl theStoreAppImpl,
            @Nonnull final BackEnd backEnd)
    {
        if (!setUp(theStoreAppImpl)) {
            return false;
        }

        _theStoreAppImpl = theStoreAppImpl;
        _backEnd = backEnd;
        _snapshot = theStoreAppImpl.isSnapshot();

        return true;
    }

    private BackEnd _backEnd;
    private TheStoreServiceAppImpl _theStoreAppImpl;
    private final Set<StoreSessionImpl> _pullingSessions =
        new ConcurrentIdentityHashSet<>();
    private boolean _snapshot;
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
