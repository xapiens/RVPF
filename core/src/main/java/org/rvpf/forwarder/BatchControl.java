/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: BatchControl.java 4043 2019-06-02 15:05:37Z SFB $
 */

package org.rvpf.forwarder;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.util.container.KeyedGroups;

/**
 * Batch control.
 */
public final class BatchControl
{
    /**
     * Constructs an instance.
     *
     * @param limit The limit.
     * @param timeout The timeout.
     * @param wait The wait.
     */
    BatchControl(
            final int limit,
            @Nonnull final Optional<ElapsedTime> timeout,
            @Nonnull final Optional<ElapsedTime> wait)
    {
        _limit = limit;
        _timeout = timeout;
        _wait = wait;
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

    /**
     * Gets the batch limit.
     *
     * @return The batch limit.
     */
    @CheckReturnValue
    public int getLimit()
    {
        return _limit;
    }

    /**
     * Gets the batch wait.
     *
     * @return The optional batch wait.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<ElapsedTime> getWait()
    {
        final ElapsedTime wait;

        if (_stamp.isPresent()) {
            final ElapsedTime elapsed = DateTime.now().sub(_stamp.get());
            final ElapsedTime left = (elapsed
                .compareTo(
                    _timeout.get()) < 0)? _timeout
                        .get()
                        .sub(elapsed): ElapsedTime.EMPTY;

            wait = left.min(_wait);
        } else {
            wait = _wait
                .isPresent()? _wait.get().min(_timeout): _timeout.orElse(null);

            if (_reset) {
                if (_timeout.isPresent()) {
                    _stamp = Optional.of(DateTime.now());
                }

                _reset = false;
            }
        }

        return Optional.ofNullable(wait);
    }

    /**
     * Resets.
     */
    public void reset()
    {
        _stamp = Optional.empty();
        _reset = true;
    }

    private final int _limit;
    private boolean _reset;
    private Optional<DateTime> _stamp = Optional.empty();
    private final Optional<ElapsedTime> _timeout;
    private final Optional<ElapsedTime> _wait;

    /**
     * Batch control builder.
     */
    public static final class Builder
    {
        /**
         * Applies properties.
         *
         * @param moduleProperties The module properties.
         *
         * @return This.
         */
        @Nonnull
        public Builder applyProperties(
                @Nonnull final KeyedGroups moduleProperties)
        {
            _limit = moduleProperties
                .getInt(BATCH_LIMIT_PROPERTY, _defaultLimit);

            _timeout = moduleProperties
                .getElapsed(
                    BATCH_TIMEOUT_PROPERTY,
                    Optional.empty(),
                    Optional.of(ElapsedTime.EMPTY));

            _wait = moduleProperties
                .getElapsed(
                    BATCH_WAIT_PROPERTY,
                    !_timeout.isPresent()? _defaultWait: Optional.empty(),
                    Optional.empty());

            return this;
        }

        /**
         * Builds a batch control.
         *
         * @return The new batch control.
         */
        public BatchControl build()
        {
            final int limit = _limit;

            if (limit < Integer.MAX_VALUE) {
                _logger
                    .debug(
                        ForwarderMessages.BATCH_SIZE_LIMIT,
                        String.valueOf(limit));
            } else {
                _logger.debug(ForwarderMessages.NO_BATCH_LIMIT);
            }

            final Optional<ElapsedTime> timeout = _timeout;

            if (timeout.isPresent()) {
                _logger.debug(ForwarderMessages.BATCH_TIMEOUT, timeout.get());
            }

            final Optional<ElapsedTime> wait = _wait;

            if (wait.isPresent()) {
                _logger.debug(ForwarderMessages.BATCH_WAIT, wait.get());
            }

            return new BatchControl(limit, timeout, wait);
        }

        /**
         * Sets the default limit.
         *
         * @param defaultLimit The default limit.
         *
         * @return This.
         */
        @Nonnull
        public Builder setDefaultLimit(final int defaultLimit)
        {
            _defaultLimit = defaultLimit;

            return this;
        }

        /**
         * Sets the default wait.
         *
         * @param defaultWait The default wait.
         *
         * @return This.
         */
        @Nonnull
        public Builder setDefaultWait(
                @Nonnull final Optional<ElapsedTime> defaultWait)
        {
            _defaultWait = defaultWait;

            return this;
        }

        /**
         * Sets the logger.
         *
         * @param logger The logger.
         *
         * @return This.
         */
        @Nonnull
        public Builder setLogger(@Nonnull final Logger logger)
        {
            _logger = logger;

            return this;
        }

        /** Specifies the maximum number of messages in a transaction. */
        public static final String BATCH_LIMIT_PROPERTY = "batch.limit";

        /**
         * The maximum elapsed time to complete the batch when at least one message
         * has been received in the current transaction.
         */
        public static final String BATCH_TIMEOUT_PROPERTY = "batch.timeout";

        /**
         * The maximum elapsed time to wait for an additional message when at least
         * one has been received in the current transaction.
         */
        public static final String BATCH_WAIT_PROPERTY = "batch.wait";

        private int _defaultLimit;
        private Optional<ElapsedTime> _defaultWait;
        private int _limit;
        private Logger _logger;
        private Optional<ElapsedTime> _timeout;
        private Optional<ElapsedTime> _wait;
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
