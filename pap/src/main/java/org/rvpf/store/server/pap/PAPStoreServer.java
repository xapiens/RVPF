/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PAPStoreServer.java 4057 2019-06-04 18:44:38Z SFB $
 */

package org.rvpf.store.server.pap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.Point;
import org.rvpf.base.exception.ConnectFailedException;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.security.Identity;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Traces;
import org.rvpf.base.value.PointValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.pap.PAPClient;
import org.rvpf.pap.PAPContext;
import org.rvpf.pap.PAPSupport;
import org.rvpf.store.server.StoreMessages;
import org.rvpf.store.server.StoreServer;
import org.rvpf.store.server.StoreServiceAppImpl;
import org.rvpf.store.server.archiver.Archiver;

/**
 * PAP store server.
 */
public final class PAPStoreServer
    extends StoreServer.Abstract
{
    /**
     * Constructs an instance.
     *
     * @param protocolSupport A protocol support instance.
     */
    public PAPStoreServer(@Nonnull final PAPSupport protocolSupport)
    {
        _protocolSupport = protocolSupport;
    }

    /**
     * Accepts new metadata.
     *
     * @param metadata The new metadata.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public boolean acceptMetadata(@Nonnull final Metadata metadata)
    {
        final PAPContext context = _protocolSupport
            .newClientContext(metadata, Optional.empty());

        if (context == null) {
            return false;
        }

        final PAPClient client = _protocolSupport.newClient(context);

        lock();

        try {
            if (_client != null) {
                _client.close();
            }

            _client = client;
            _client.open();
        } finally {
            unlock();
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<Archiver> newArchiver()
    {
        return Optional.empty();
    }

    /** {@inheritDoc}
     */
    @Override
    public StoreValues[] select(
            final StoreValuesQuery[] queries,
            final Optional<Identity> identity)
        throws SessionException
    {
        activateServiceClassLoader();

        final StoreValues[] responses = new StoreValues[queries.length];
        final PAPClient client = _client;

        lock();

        try {
            if (client == null) {
                throw new ServiceClosedException();
            }

            final long mark = System.nanoTime();
            final Collection<Point> points = new ArrayList<>(queries.length);

            for (int i = 0; i < queries.length; ++i) {
                StoreValuesQuery query = queries[i];

                if (query == null) {
                    responses[i] = null;

                    continue;
                }

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

                if (query.isPull()) {
                    responses[i] = unsupportedPullQuery();

                    continue;
                }

                points.add(query.getPoint().get());
                responses[i] = _RESPONSE_MARKER;
            }

            final Collection<PointValue> pointValues = Arrays
                .asList(
                    client
                        .fetchPointValues(
                                points.toArray(new Point[points.size()])));
            final Iterator<PointValue> pointValuesIterator = pointValues
                .iterator();

            for (int i = 0; i < queries.length; ++i) {
                if (responses[i] == _RESPONSE_MARKER) {
                    final PointValue pointValue = pointValuesIterator.next();
                    final StoreValuesQuery query = queries[i];
                    final StoreValues storeValues = new StoreValues(query);

                    if (pointValue != null) {
                        storeValues.add(pointValue);
                        getThisLogger()
                            .trace(
                                StoreMessages.QUERY_ANSWER,
                                query.isCount()? "1": pointValue);
                    } else if (query.isCount()) {
                        getThisLogger().trace(StoreMessages.QUERY_ANSWER, "0");
                    }

                    responses[i] = storeValues;

                    getThisLogger().trace(StoreMessages.QUERY_DONE);
                    getStats().addQueryResponse(Optional.of(responses[i]), 0);
                }
            }

            getStats()
                .addQueryResponse(Optional.empty(), System.nanoTime() - mark);
        } catch (final RuntimeException exception) {
            getThisLogger()
                .warn(exception, BaseMessages.VERBATIM, exception.getMessage());

            throw exception;
        } catch (final InterruptedException exception) {
            throw new SessionException(exception);
        } catch (final ServiceNotAvailableException exception) {
            throw new SessionException(exception);
        } catch (final Exception exception) {
            getThisLogger()
                .trace(
                    exception,
                    BaseMessages.VERBATIM,
                    exception.getMessage());

            throw exception;
        } finally {
            unlock();
        }

        getThisLogger().trace(StoreMessages.QUERIES_DONE);

        return responses;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final StoreServiceAppImpl storeAppImpl)
    {
        if (!super.setUp(storeAppImpl)) {
            return false;
        }

        if (!_traces
            .setUp(
                storeAppImpl.getDataDir(),
                storeAppImpl
                    .getConfigProperties()
                    .getGroup(Traces.TRACES_PROPERTIES),
                storeAppImpl.getSourceUUID(),
                storeAppImpl
                    .getServerProperties()
                    .getString(TRACES_PROPERTY))) {
            return false;
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        lock();

        try {
            if (_client != null) {
                _client.close();
                _client = null;
            }

            _traces.tearDown();
        } finally {
            unlock();
        }

        super.stop();
    }

    /** {@inheritDoc}
     */
    @Override
    public String supportedValueTypeCodes()
        throws SessionException
    {
        return _protocolSupport.supportedValueTypeCodes();
    }

    /** {@inheritDoc}
     */
    @Override
    public Exception[] update(
            final PointValue[] pointValues,
            final Optional<Identity> identity)
        throws SessionException
    {
        activateServiceClassLoader();

        final Exception[] exceptions = restoreUpdates(pointValues);
        final PAPClient client = _client;

        lock();

        try {
            if (client == null) {
                throw new ServiceClosedException();
            }

            final long mark = System.nanoTime();

            final Collection<PointValue> updates = new ArrayList<>(
                pointValues.length);
            long ignored = 0;
            long updated = 0;

            for (int i = 0; i < pointValues.length; i++) {
                if (exceptions[i] != null) {
                    ++ignored;

                    continue;
                }

                final PointValue pointValue = pointValues[i];

                exceptions[i] = pointValue
                    .isDeleted()? new UnsupportedOperationException()
                        : checkUpdate(
                            pointValue,
                            identity)
                            .orElse(null);

                if (exceptions[i] != null) {
                    ++ignored;

                    continue;
                }

                updates.add(pointValue);
            }

            try {
                final Exception[] responses = _client
                    .updatePointValues(
                        updates.toArray(new PointValue[updates.size()]));
                final Iterator<Exception> responsesIterator = Arrays
                    .asList(responses)
                    .iterator();

                for (int i = 0; i < pointValues.length; i++) {
                    if (exceptions[i] != null) {
                        continue;
                    }

                    exceptions[i] = responsesIterator.next();

                    if (exceptions[i] != null) {
                        continue;
                    }

                    final PointValue pointValue = pointValues[i];

                    ++updated;
                    getUpdatedTraces().add(pointValue);
                    getThisLogger()
                        .trace(StoreMessages.UPDATER_UPDATED, pointValue);
                    getReplicator().replicate(pointValue);
                    addNotice(pointValue);
                }

                getReplicator().commit();
                sendNotices();
            } catch (final InterruptedException exception) {
                throw new SessionException(exception);
            } catch (final ConnectFailedException exception) {
                throw new SessionException(exception);
            } catch (final RuntimeException exception) {
                getThisLogger()
                    .warn(
                        exception,
                        BaseMessages.VERBATIM,
                        exception.getMessage());

                throw exception;
            } catch (final Exception exception) {
                getThisLogger()
                    .trace(
                        exception,
                        BaseMessages.VERBATIM,
                        exception.getMessage());

                throw new RuntimeException(exception);
            }

            reportUpdates(updated, 0, ignored, System.nanoTime() - mark);
        } finally {
            unlock();
        }

        return exceptions;
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean supportsUpdateTracer()
    {
        return true;
    }

    /** Traces subdirectory property. */
    public static final String TRACES_PROPERTY = "traces";

    /**  */

    private static final StoreValues _RESPONSE_MARKER = new StoreValues();

    private PAPClient _client;
    private final PAPSupport _protocolSupport;
    private final Traces _traces = new Traces();
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
