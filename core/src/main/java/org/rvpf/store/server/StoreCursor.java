/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreCursor.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.store.server;

import java.security.GeneralSecurityException;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.TimeInterval;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.logger.Messages;
import org.rvpf.base.security.Identity;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.sync.Sync;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.metadata.Permissions;
import org.rvpf.metadata.entity.PointEntity;

/**
 * Store cursor.
 */
public final class StoreCursor
{
    /**
     * Constructs an instance.
     *
     * @param query The store values query.
     * @param server The store server.
     * @param responder The responder to use.
     */
    public StoreCursor(
            @Nonnull final StoreValuesQuery query,
            @Nonnull final StoreServer server,
            @Nonnull final Responder responder)
    {
        _query = query;
        _server = (StoreServer.Abstract) server;
        _responder = responder;
    }

    /**
     * Creates a store values response.
     *
     * @param identity The optional requesting identity.
     *
     * @return The store values.
     */
    @Nonnull
    @CheckReturnValue
    public StoreValues createResponse(
            @Nonnull final Optional<Identity> identity)
    {
        return createResponse(_query, identity);
    }

    /**
     * Creates a store values response to a store query.
     *
     * @param query The store query.
     * @param identity The optional requesting identity.
     *
     * @return The store values.
     */
    @Nonnull
    @CheckReturnValue
    public StoreValues createResponse(
            @Nonnull StoreValuesQuery query,
            @Nonnull final Optional<Identity> identity)
    {
        final StoreValues response = new StoreValues(query);

        _toDo = query.getRows();

        if (_toDo < 1) {
            return response;
        }

        _done = 0;

        if (!_check(query.getPoint(), identity)) {
            final String message = Message
                .format(StoreMessages.QUERY_UNAUTHORIZED, query.getPoint());

            _LOGGER.debug(BaseMessages.VERBATIM, message);
            response.setException(new GeneralSecurityException(message));

            return response;
        }

        final StoreValuesQuery.Builder queryBuilder = StoreValuesQuery
            .newBuilder()
            .copyFrom(query);

        if (queryBuilder.limit(_server.getResponseLimit())) {
            query = queryBuilder.build();
        }

        if (query.isPolated() && !query.getPoint().isPresent()) {
            final String message = Message
                .format(StoreMessages.NO_POINT_FOR_POLATE, query);

            _LOGGER.warn(BaseMessages.VERBATIM, message);
            response.setException(new IllegalArgumentException(message));

            return response;
        }

        if (query.isPolated() && !isCount()) {
            if (!query.getSync().isPresent()
                    && !query.getInterval().isInstant()) {
                final String message = Message
                    .format(StoreMessages.POLATED_SYNC);

                _LOGGER.warn(BaseMessages.VERBATIM, message);
                response.setException(new IllegalStateException(message));

                return response;
            }

            if ((((query.getRows() == Integer.MAX_VALUE)
                    && (query.getInterval().isFromBeginningOfTime()
                        || query.getInterval().isToEndOfTime()))
                    || (query.isReverse()? !query
                        .getInterval()
                        .getBefore()
                        .isPresent(): !query
                                .getInterval()
                                .getAfter()
                                .isPresent()))) {
                final String message = Message
                    .format(StoreMessages.POLATED_INTERVAL);

                _LOGGER.warn(BaseMessages.VERBATIM, message);
                response.setException(new IllegalArgumentException(message));

                return response;
            }

            return _server
                .getPolator(query.getPoint().get())
                .polate(query, this, identity.get());
        }

        _interval = query.getInterval();
        _query = query;
        _responder.reset(Optional.of(this));

        if (isCount()) {
            final int rows = query.getRows();
            final long count = _responder.count();

            if ((rows > 0) && (rows < count)) {
                response.setCount(rows);
            } else {
                response.setCount(count);
            }
        } else {
            _fillResponse(response, identity);
        }

        _interval = null;
        _query = null;
        _responder.reset(Optional.empty());

        return response;
    }

    /**
     * Gets after time.
     *
     * @return The optional after time.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<DateTime> getAfter()
    {
        return _interval.getAfter();
    }

    /**
     * Gets before time.
     *
     * @return The optional before time.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<DateTime> getBefore()
    {
        return _interval.getBefore();
    }

    /**
     * Gets a limit on the number of requested values.
     *
     * @return The limit.
     */
    @CheckReturnValue
    public int getLimit()
    {
        int limit;

        limit = _responder.limit();

        if (limit > 0) {
            if ((_toDo > 0) && ((_toDo - _done) < limit)) {
                limit = _toDo - _done;
                Require.success(limit > 0);
            }
        } else if (_query.isFixed()) {
            limit = _toDo - _done;
            Require.success(limit > 0);
        }

        return limit;
    }

    /**
     * Gets the point's definition.
     *
     * @return The optional point's definition.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Point> getPoint()
    {
        return _query.getPoint();
    }

    /**
     * Gets the point's UUID.
     *
     * @return The optional point's UUID.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<UUID> getPointUUID()
    {
        return _query.getPointUUID();
    }

    /**
     * Gets the store query's type.
     *
     * <p>The type is a bit mask representing characteristics of the store
     * query.</p>
     *
     * @return The type.
     */
    @CheckReturnValue
    public int getType()
    {
        return _query.getType();
    }

    /**
     * Asks if the responses should be counts.
     *
     * @return True if the responses should be counts.
     */
    @CheckReturnValue
    public boolean isCount()
    {
        return (_query != null) && _query.isCount();
    }

    /**
     * Asks if deleted values are included.
     *
     * @return True when deleted values are included.
     */
    @CheckReturnValue
    public boolean isIncludeDeleted()
    {
        return _query.isIncludeDeleted();
    }

    /**
     * Asks if this query is for an instant.
     *
     * @return True when it is for an instant.
     */
    @CheckReturnValue
    public boolean isInstant()
    {
        return _interval.isInstant();
    }

    /**
     * Asks if null values are ignored.
     *
     * @return True when null values are ignored.
     */
    @CheckReturnValue
    public boolean isNullIgnored()
    {
        return _query.isNotNull();
    }

    /**
     * Asks if the store query is a pull query.
     *
     * @return True when it answers to a pull query.
     */
    @CheckReturnValue
    public boolean isPull()
    {
        return _query.isPull();
    }

    /**
     * Asks if the store query asks for values in reverse order.
     *
     * @return True when values are queried in reverse order.
     */
    @CheckReturnValue
    public boolean isReverse()
    {
        return _query.isReverse();
    }

    /**
     * Returns a fresh value of the store query's type.
     *
     * <p>The type is a bit mask representing characteristics of the store
     * query.</p>
     *
     * @return The type.
     */
    @CheckReturnValue
    public int refreshType()
    {
        _query = StoreValuesQuery
            .newBuilder()
            .copyFrom(_query)
            .setInterval(_interval)
            .build();

        return _query.getType();
    }

    /**
     * Asks if the server supports count.
     *
     * @return True if count is supported.
     */
    @CheckReturnValue
    public boolean supportsCount()
    {
        return _server.supportsCount();
    }

    private static boolean _check(
            final Optional<Point> point,
            final Optional<Identity> identity)
    {
        if (point.isPresent() && identity.isPresent()) {
            final Optional<? extends Permissions> permissions =
                ((PointEntity) point
                    .get())
                    .getPermissions();

            return !permissions.isPresent()
                   || permissions.get().check(
                       Permissions.Action.READ,
                       identity);
        }

        return true;
    }

    private void _fillResponse(
            final StoreValues response,
            final Optional<Identity> identity)
    {
        final Optional<Sync> sync = _query.getSync();
        final int responseLimit = _query.getLimit();
        int received;
        int limit;

        received = 0;
        limit = getLimit();

        for (;;) {
            final PointValue pointValue;
            final boolean ignored;
            final Messages.Entry reason;

            pointValue = _responder.next().orElse(null);

            if (pointValue == null) {
                break;
            }

            ++received;

            if (_query.isNotNull() && (pointValue.getValue() == null)) {
                ignored = true;
                reason = StoreMessages.REASON_NULL;
            } else if (sync.isPresent()
                       && !sync.get().isInSync(pointValue.getStamp())) {
                ignored = true;
                reason = StoreMessages.REASON_SYNC;
            } else if (pointValue.isDeleted() && !_query.isIncludeDeleted()) {
                ignored = true;
                reason = StoreMessages.REASON_DELETED;
            } else if (!_query.getPoint().isPresent()
                       && !_check(pointValue.getPoint(), identity)) {
                ignored = true;
                reason = StoreMessages.REASON_UNAUTHORIZED;
            } else {
                ignored = false;
                reason = null;
            }

            if (ignored) {
                _LOGGER.trace(StoreMessages.IGNORED_VALUE, reason, pointValue);
            } else {
                if ((responseLimit > 0) && (_done >= responseLimit)) {
                    response.mark(pointValue, _done);
                    _LOGGER.trace(StoreMessages.MARKED_VALUE, pointValue);

                    break;
                }

                response
                    .add(_query.isNormalized()
                         ? pointValue.normalized(): pointValue);

                if (++_done >= _toDo) {
                    break;
                }
            }

            if ((limit > 0) && (received >= limit)) {
                if (!_resetResponder(pointValue)) {
                    break;
                }

                received = 0;
                limit = getLimit();
            }
        }
    }

    private boolean _resetResponder(final PointValue pointValue)
    {
        final TimeInterval.Builder intervalBuilder = TimeInterval.newBuilder();

        if (isPull()) {
            final VersionedValue versionedValue = (VersionedValue) pointValue;

            if (isReverse()) {
                intervalBuilder.setBefore(versionedValue.getVersion());
            } else {
                intervalBuilder.setAfter(versionedValue.getVersion());
            }
        } else if (isReverse()) {
            intervalBuilder.setBefore(pointValue.getStamp());
        } else {
            intervalBuilder.setAfter(pointValue.getStamp());
        }

        try {
            _interval = intervalBuilder.build();
        } catch (final TimeInterval.InvalidIntervalException exception) {
            _interval = null;

            return false;    // No more values inside interval.
        }

        _responder.reset(Optional.of(this));

        return true;
    }

    private static final Logger _LOGGER = Logger.getInstance(StoreCursor.class);

    private int _done;
    private TimeInterval _interval;
    private StoreValuesQuery _query;
    private final Responder _responder;
    private final StoreServer.Abstract _server;
    private int _toDo;

    /**
     * Cursor responder.
     *
     * <p>A cursor acts as a 'middle-man' between a store server query
     * processing loop and a store server supplied class implementing this
     * interface.</p>
     */
    public interface Responder
    {
        /**
         * Returns the count of values that would satisfy the query.
         *
         * @return The count.
         */
        @CheckReturnValue
        long count();

        /**
         * Returns a limit on the number of values allowed by the backend.
         *
         * @return The limit.
         */
        @CheckReturnValue
        int limit();

        /**
         * Returns the next point value.
         *
         * @return The next point value (empty when done).
         */
        @Nonnull
        @CheckReturnValue
        Optional<? extends PointValue> next();

        /**
         * Resets this responder.
         *
         * <p>The responder should free resources reserved by a previous store
         * query. If the store cursor parameter is not empty, it should prepare
         * for calls to its {@link #next} method.</p>
         *
         * @param storeCursor The controlling store cursor (may be empty).
         */
        void reset(@Nonnull Optional<StoreCursor> storeCursor);
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
