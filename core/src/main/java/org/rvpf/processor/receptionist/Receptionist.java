/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Receptionist.java 4043 2019-06-02 15:05:37Z SFB $
 */

package org.rvpf.processor.receptionist;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.util.SnoozeAlarm;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.ProcessorServiceAppImpl;

/**
 * Receptionist.
 *
 * <p>The receptionist protocol is used by the processor to receive the point
 * value change event notices.</p>
 */
public interface Receptionist
{
    /**
     * Closes the input Queue.
     */
    void close();

    /**
     * Registers that the notices have been processed.
     *
     * @throws InterruptedException When the service is stopped.
     * @throws ServiceNotAvailableException When the service is not available.
     */
    void commit()
        throws InterruptedException, ServiceNotAvailableException;

    /**
     * Fetches notices.
     *
     * @param limit The limit for the number of notices.
     * @param wait Negative is forever, 0 is no wait, other is milliseconds.
     *
     * @return The notices.
     *
     * @throws InterruptedException When the service is stopped.
     * @throws ServiceNotAvailableException When the service is not available.
     */
    @Nonnull
    @CheckReturnValue
    List<PointValue> fetchNotices(
            int limit,
            long wait)
        throws InterruptedException, ServiceNotAvailableException;

    /**
     * Opens the input Queue.
     */
    void open();

    /**
     * Returns and resets the reception time.
     *
     * @return The reception time.
     */
    @CheckReturnValue
    long receptionTime();

    /**
     * Forgets that the notices have been received.
     *
     * @throws InterruptedException When the service is stopped.
     * @throws ServiceNotAvailableException When the service is not available.
     */
    void rollback()
        throws InterruptedException, ServiceNotAvailableException;

    /**
     * Sets up access to the messaging system.
     *
     * @param metadata The processor metadata.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUp(Metadata metadata);

    /**
     * Tears down what has been set up.
     */
    void tearDown();

    /**
     * Abstract receptionist.
     */
    @NotThreadSafe
    abstract class Abstract
        implements Receptionist
    {
        /** {@inheritDoc}
         */
        @Override
        public final synchronized void close()
        {
            doClose();
        }

        /** {@inheritDoc}
         */
        @Override
        public final void commit()
            throws InterruptedException, ServiceNotAvailableException
        {
            final long mark = System.nanoTime();

            doCommit();

            _receptionTime += System.nanoTime() - mark;
        }

        /** {@inheritDoc}
         */
        @Override
        public List<PointValue> fetchNotices(
                int limit,
                long wait)
            throws InterruptedException, ServiceNotAvailableException
        {
            final List<PointValue> notices = new LinkedList<>();
            final long mark = System.nanoTime();

            while (limit > 0) {
                final PointValue notice = doFetchNotice(limit--, wait);

                if (notice == null) {
                    break;
                }

                if (!notice.hasPointUUID()) {
                    getThisLogger()
                        .trace(ProcessorMessages.IMMEDIATE_PROCESSING);

                    break;
                }

                notices.add(notice.restore(_metadata));
                wait = _wait;
            }

            _receptionTime += System.nanoTime() - mark;

            return notices;
        }

        /** {@inheritDoc}
         */
        @Override
        public final synchronized void open()
        {
            doOpen();
        }

        /** {@inheritDoc}
         */
        @Override
        public final long receptionTime()
        {
            final long receptionTime = _receptionTime;

            _receptionTime = 0;

            return receptionTime;
        }

        /** {@inheritDoc}
         */
        @Override
        public final void rollback()
            throws InterruptedException, ServiceNotAvailableException
        {
            doRollback();

            _receptionTime = 0;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean setUp(final Metadata metadata)
        {
            _metadata = metadata;

            final KeyedGroups processorProperties = metadata
                .getPropertiesGroup(
                    ProcessorServiceAppImpl.PROCESSOR_PROPERTIES);
            final Optional<ElapsedTime> wait = processorProperties
                .getElapsed(
                    WAIT_PROPERTY,
                    Optional.of(DEFAULT_WAIT),
                    Optional.empty());

            if (wait.isPresent()) {
                if (!SnoozeAlarm
                    .validate(wait.get(), this, ProcessorMessages.WAIT_TEXT)) {
                    return false;
                }

                getThisLogger().debug(ProcessorMessages.WAIT, wait.get());
                _wait = wait.get().toMillis();
            } else {
                _wait = -1;
            }

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public void tearDown() {}

        /**
         * Closes the input Queue.
         *
         * <p>Called while synchronized on this. May be called while already
         * closed.</p>
         */
        protected abstract void doClose();

        /**
         * Registers that the notices have been processed.
         *
         * <p>Called while synchronized on this.</p>
         *
         * @throws InterruptedException When the Service is stopped.
         * @throws ServiceNotAvailableException When the service is not
         *                                      available.
         */
        protected abstract void doCommit()
            throws InterruptedException, ServiceNotAvailableException;

        /**
         * Fetches a notice.
         *
         * @param limit The read ahead limit.
         * @param wait Negative is forever, 0 is no wait, other is milliseconds.
         *
         * @return A point value (null on timeout).
         *
         * @throws InterruptedException When the Service is stopped.
         * @throws ServiceNotAvailableException When the service is not
         *                                      available.
         */
        @Nullable
        @CheckReturnValue
        protected abstract PointValue doFetchNotice(
                int limit,
                long wait)
            throws InterruptedException, ServiceNotAvailableException;

        /**
         * Opens the input Queue.
         *
         * <p>Called while synchronized on this.</p>
         */
        protected abstract void doOpen();

        /**
         * Forgets that the notices have been received.
         *
         * @throws InterruptedException When the Service is stopped.
         * @throws ServiceNotAvailableException When the service is not
         *                                      available.
         */
        protected abstract void doRollback()
            throws InterruptedException, ServiceNotAvailableException;

        /**
         * Gets the metadata.
         *
         * @return The metadata.
         */
        @Nonnull
        @CheckReturnValue
        protected Metadata getMetadata()
        {
            return _metadata;
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

        /** Default wait. */
        public static final ElapsedTime DEFAULT_WAIT = ElapsedTime
            .fromMillis(1000);

        /**
         * The maximum time in millis to wait for an additional notice when at
         * least one has been accepted for processing.
         */
        public static final String WAIT_PROPERTY = "receptionist.wait";

        private final Logger _logger = Logger.getInstance(getClass());
        private Metadata _metadata;
        private long _receptionTime;
        private long _wait;
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
