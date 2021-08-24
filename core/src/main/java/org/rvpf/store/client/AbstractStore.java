/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AbstractStore.java 4097 2019-06-25 15:35:48Z SFB $
 */

package org.rvpf.store.client;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Params;
import org.rvpf.base.Point;
import org.rvpf.base.TimeInterval;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.store.Store;
import org.rvpf.base.store.StoreAccessException;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Externalizer;
import org.rvpf.base.tool.Externalizer.ValueType;
import org.rvpf.base.value.PointValue;
import org.rvpf.config.Config;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.Proxied;
import org.rvpf.metadata.entity.ProxyEntity;
import org.rvpf.service.ServiceMessages;

/**
 * Abstract store.
 */
public abstract class AbstractStore
    extends Proxied.Abstract
    implements Store
{
    /** {@inheritDoc}
     */
    @Override
    public final boolean addQuery(final StoreValuesQuery query)
    {
        if (query.isCancelled()) {
            return false;
        }

        final StoreValuesQuery.Builder queryBuilder = StoreValuesQuery
            .newBuilder()
            .copyFrom(query);

        doAddQuery(
            queryBuilder.limit(_responseLimit)? queryBuilder.build(): query);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public final void addUpdate(final PointValue pointValue)
    {
        _updates.add(pointValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void bind(final Point point)
    {
        if (_bindPoints != null) {
            _bindPoints.add(point);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean canConfirm()
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public void close() {}

    /** {@inheritDoc}
     */
    @Override
    public final int compareTo(final Store other)
    {
        return getName().compareTo(other.getName());
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean confirm(
            final PointValue pointValue,
            final boolean confirmValue)
        throws InterruptedException, StoreAccessException
    {
        throw new UnsupportedOperationException(
            Message.format(
                ServiceMessages.STORE_CANT_CONFIRM,
                getName()).toString());
    }

    /** {@inheritDoc}
     */
    @Override
    public StoreValues deliver(
            final int limit,
            final long timeout)
        throws InterruptedException, StoreAccessException
    {
        return new StoreValues(
            new UnsupportedOperationException(
                Message.format(
                    ServiceMessages.STORE_CANT_DELIVER,
                    getName()).toString()));
    }

    /** {@inheritDoc}
     */
    @Override
    public final int getQueriesBatchLimit()
    {
        return _queriesBatchLimit;
    }

    /** {@inheritDoc}
     */
    @Override
    public final int getResponseLimit()
    {
        return _responseLimit;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<StoreValues[]> getSubscribedValues()
    {
        throw new UnsupportedOperationException(
            Message.format(
                ServiceMessages.STORE_CANT_SUBSCRIBE,
                getName()).toString());
    }

    /** {@inheritDoc}
     */
    @Override
    public UUID getUUID()
    {
        return getProxyEntity().getUUID().get();
    }

    /** {@inheritDoc}
     */
    @Override
    public final int getUpdateCount()
    {
        return _updates.size();
    }

    /** {@inheritDoc}
     */
    @Override
    public final Collection<PointValue> getUpdates()
    {
        return _updates;
    }

    /** {@inheritDoc}
     */
    @Override
    public void impersonate(
            final Optional<String> user)
        throws StoreAccessException
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isNullRemoves()
    {
        return _nullRemoves;
    }

    /** {@inheritDoc}
     */
    @Override
    public Iterable<PointValue> iterate(final StoreValuesQuery query)
    {
        throw new UnsupportedOperationException(
            Message.format(
                ServiceMessages.STORE_CANT_GET,
                getName()).toString());
    }

    /** {@inheritDoc}
     */
    @Override
    public StoreValues pull(
            final StoreValuesQuery query,
            final long timeout)
        throws InterruptedException, StoreAccessException
    {
        throw new UnsupportedOperationException(
            Message.format(
                ServiceMessages.STORE_CANT_PULL,
                getName()).toString());
    }

    /** {@inheritDoc}
     */
    @Override
    public int purge(
            final UUID[] pointUUIDs,
            final TimeInterval timeInterval)
        throws StoreAccessException
    {
        throw new UnsupportedOperationException(
            Message.format(
                ServiceMessages.STORE_CANT_PURGE,
                getName()).toString());
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean sendUpdates()
        throws StoreAccessException
    {
        return sendUpdates(getUpdates());
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Metadata metadata, final ProxyEntity proxyEntity)
    {
        if (!super.setUp(metadata, proxyEntity)) {
            return false;
        }

        if (proxyEntity.getParams().getBoolean(BIND_POINTS_PARAM)) {
            _bindPoints = new HashSet<>();
            getThisLogger().info(ServiceMessages.POINTS_BOUND);
        }

        _nullRemoves = proxyEntity.getParams().getBoolean(NULL_REMOVES_PARAM);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean subscribe(
            final Collection<UUID> points)
        throws StoreAccessException
    {
        return subscribe(points.toArray(new UUID[points.size()]));
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean subscribe(final UUID[] points)
        throws StoreAccessException
    {
        throw new UnsupportedOperationException(
            Message.format(
                ServiceMessages.STORE_CANT_SUBSCRIBE,
                getName()).toString());
    }

    /** {@inheritDoc}
     */
    @Override
    public EnumSet<ValueType> supportedValueTypes()
        throws StoreAccessException
    {
        return Externalizer.ValueType.stringToSet(supportedValueTypeCodes());
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsCount()
        throws StoreAccessException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsDelete()
        throws StoreAccessException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsDeliver()
        throws StoreAccessException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsPull()
        throws StoreAccessException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsPurge()
        throws StoreAccessException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsSubscribe()
        throws StoreAccessException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        close();

        _bindPoints = null;

        super.tearDown();

        getThisLogger().debug(ServiceMessages.TEAR_DOWN_COMPLETED);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean unsubscribe(final UUID[] points)
        throws StoreAccessException
    {
        throw new UnsupportedOperationException(
            Message.format(
                ServiceMessages.STORE_CANT_UNSUBSCRIBE,
                getName()).toString());
    }

    /**
     * Returns an access exception.
     *
     * @param cause The cause for this.
     *
     * @return An access exception.
     */
    protected final StoreAccessException accessException(
            @Nonnull final SessionException cause)
    {
        reset();

        return new StoreAccessException(getProxyEntity().getUUID(), cause);
    }

    /**
     * Binds points to their UUID.
     *
     * @throws InterruptedException When the service is stopped.
     * @throws StoreAccessException On store access problem.
     */
    protected final void bindPoints()
        throws InterruptedException, StoreAccessException
    {
        if ((_bindPoints != null) && !_bindPoints.isEmpty()) {
            bindPoints(_bindPoints);
            _bindPoints.clear();
        }
    }

    /**
     * Binds points to their UUID.
     *
     * @param points The points.
     *
     * @throws InterruptedException When the service is stopped.
     * @throws StoreAccessException On store access problem.
     */
    protected void bindPoints(
            @Nonnull final Set<Point> points)
        throws InterruptedException, StoreAccessException {}

    /**
     * Does add a store query for point values.
     *
     * <p>This provides the subclass specific implementation.</p>
     *
     * @param query The store query.
     */
    protected abstract void doAddQuery(@Nonnull StoreValuesQuery query);

    /**
     * Gets the confirm retries.
     *
     * @return The confirm retries.
     */
    @CheckReturnValue
    protected final int getConfirmRetries()
    {
        return _confirmRetries;
    }

    /**
     * Gets the confirm retry delay.
     *
     * @return The confirm retry delay.
     */
    @CheckReturnValue
    protected final long getConfirmRetryDelay()
    {
        return _confirmRetryDelay;
    }

    /** {@inheritDoc}
     */
    @Override
    protected Logger getThisLogger()
    {
        return _logger;
    }

    /**
     * Resets.
     */
    protected void reset()
    {
        _updates.clear();
    }

    /**
     * Sets the confirm retries.
     */
    protected final void setConfirmRetries()
    {
        final Params params = getParams();
        final Optional<ElapsedTime> confirmRetryDelay = params
            .getElapsed(
                CONFIRM_RETRY_DELAY_PARAM,
                Optional.empty(),
                Optional.empty());

        _confirmRetries = params
            .getInt(
                CONFIRM_RETRIES_PARAM,
                confirmRetryDelay.isPresent()? 1: 0);

        if (_confirmRetries > 0) {
            _confirmRetryDelay = confirmRetryDelay
                .isPresent()? confirmRetryDelay
                    .get()
                    .toMillis(): _DEFAULT_CONFIRM_RETRY_DELAY;

            if (_confirmRetryDelay <= 0) {
                _confirmRetries = 0;
            }
        }

        if ((_confirmRetries > 0) && getThisLogger().isDebugEnabled()) {
            getThisLogger()
                .debug(
                    ServiceMessages.CONFIRM_RETRIES,
                    String.valueOf(_confirmRetries));
            getThisLogger()
                .debug(
                    ServiceMessages.CONFIRM_RETRY_DELAY,
                    ElapsedTime.fromMillis(_confirmRetryDelay));
        }
    }

    /**
     * Sets the queries batch limit.
     *
     * @param config The configuration.
     */
    protected final void setQueriesBatchLimit(@Nonnull final Config config)
    {
        _queriesBatchLimit = config
            .getIntValue(
                QUERIES_BATCH_LIMIT_PROPERTY,
                _DEFAULT_QUERIES_BATCH_LIMIT);

        if (_queriesBatchLimit < 1) {
            _queriesBatchLimit = 1;
        }

        getThisLogger()
            .debug(
                ServiceMessages.QUERIES_BATCH_LIMIT,
                String.valueOf(_queriesBatchLimit));
    }

    /**
     * Sets the response limit.
     *
     * @param config The configuration.
     */
    protected final void setResponseLimit(@Nonnull final Config config)
    {
        _responseLimit = config.getIntValue(RESPONSE_LIMIT_PROPERTY, 0);

        if (_responseLimit <= 0) {
            _responseLimit = _DEFAULT_RESPONSE_LIMIT;
        }

        getThisLogger()
            .debug(
                ServiceMessages.RESPONSE_LIMIT,
                String.valueOf(_responseLimit));
    }

    /**
     * Returns a string of supported value type codes.
     *
     * @return The string of supported value type codes.
     *
     * @throws StoreAccessException On store access problem.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract String supportedValueTypeCodes()
        throws StoreAccessException;

    /** Limits the size of a batch of queries. */
    public static final String QUERIES_BATCH_LIMIT_PROPERTY =
        "store.client.queries.batch.limit";

    /**
     * Limits the maximum number of point values that may be grouped in a
     * single response.
     */
    public static final String RESPONSE_LIMIT_PROPERTY =
        "store.client.response.limit";
    private static final long _DEFAULT_CONFIRM_RETRY_DELAY = 15000;
    private static final int _DEFAULT_QUERIES_BATCH_LIMIT = 100;
    private static final int _DEFAULT_RESPONSE_LIMIT = 100;

    private Set<Point> _bindPoints;
    private int _confirmRetries;
    private long _confirmRetryDelay;
    private final Logger _logger = Logger.getInstance(getClass());
    private boolean _nullRemoves;
    private int _queriesBatchLimit;
    private int _responseLimit;
    private final List<PointValue> _updates = new LinkedList<>();
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
