/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: InputModule.java 4096 2019-06-24 23:07:39Z SFB $
 */

package org.rvpf.forwarder.input;

import java.io.Serializable;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Message;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.forwarder.BatchControl;
import org.rvpf.forwarder.ForwarderMessages;
import org.rvpf.forwarder.ForwarderModule;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;

/**
 * Input module.
 */
public abstract class InputModule
    extends ForwarderModule.Abstract
    implements ServiceThread.Target
{
    /** {@inheritDoc}
     */
    @Override
    public final void run()
        throws InterruptedException
    {
        final ModuleInput input = _input;
        boolean failed = false;

        for (;;) {
            boolean loggedFailed = false;

            for (;;) {
                boolean opened = false;

                if (isReliable()) {
                    opened = input.open();
                }

                ServiceThread.ready();

                if (!isReliable()) {
                    opened = input.open();
                }

                if (opened) {
                    if (!isReliable()) {
                        final Message message = new Message(
                            ForwarderMessages.CONNECTION_COMPLETED,
                            input.getSourceName());

                        if (failed) {
                            getThisLogger().info(message);
                            failed = false;
                        } else {
                            getThisLogger().debug(message);
                        }
                    }

                    break;
                }

                if (!loggedFailed) {
                    if (isReliable()) {
                        getThisLogger()
                            .warn(
                                ForwarderMessages.CONNECTION_FAILED,
                                input.getSourceName());
                        getService().fail();

                        return;
                    }

                    getThisLogger()
                        .warn(
                            ForwarderMessages.CONNECTION_FAILED_SLEEPING,
                            input.getSourceName());
                    loggedFailed = true;
                    failed = true;
                }

                getService().snooze(getConnectionRetryDelay());
            }

            for (;;) {
                final Optional<Serializable[]> messages = input
                    .input(getBatchControl());

                synchronized (_mutex) {
                    if (_thread.get() == null) {
                        throw new InterruptedException();
                    }

                    if (!messages.isPresent()) {
                        break;
                    }

                    if (getThisLogger().isDebugEnabled()) {
                        final String from = input.getSourceName();

                        for (final Serializable message: messages.get()) {
                            getThisLogger()
                                .trace(
                                    ForwarderMessages.MESSAGE_FROM,
                                    from,
                                    message);
                        }
                    }

                    try {
                        if (output(filter(messages.get()))) {
                            if (!input.commit()) {
                                if (isReliable() && !input.isClosed()) {
                                    throw new ServiceNotAvailableException();
                                }

                                break;
                            }
                        } else {
                            if (!input.rollback()) {
                                if (isReliable() && !input.isClosed()) {
                                    throw new ServiceNotAvailableException();
                                }
                            }
                        }
                    } catch (final ServiceNotAvailableException exception) {
                        getThisLogger()
                            .error(exception, ServiceMessages.RESTART_NEEDED);
                        getService().restart(true);
                    }
                }

                if (_thread.get() == null) {
                    throw new InterruptedException();
                }
            }

            synchronized (_mutex) {
                if (_thread.get() == null) {
                    throw new InterruptedException();
                }

                input.close();
            }

            getThisLogger().warn(ForwarderMessages.CONNECTION_LOST);
            failed = true;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
    {
        final ModuleInput input = _input;

        if (input != null) {
            final ServiceThread thread = new ServiceThread(
                this,
                input.getDisplayName());

            if (_thread.compareAndSet(null, thread)) {
                getThisLogger()
                    .debug(ServiceMessages.STARTING_THREAD, thread.getName());
                Require.ignored(thread.start(true));
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        final ServiceThread thread = _thread.getAndSet(null);

        if (thread != null) {
            getThisLogger()
                .debug(ServiceMessages.STOPPING_THREAD, thread.getName());
            thread.interrupt();

            final ModuleInput input = _input;

            if (input != null) {
                input.close();
            }

            Require
                .ignored(
                    thread
                        .join(getThisLogger(), getService().getJoinTimeout()));
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        final ModuleInput input = _input;

        if (input != null) {
            _input = null;
            input.tearDown();
        }

        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    protected int getDefaultBatchLimit()
    {
        return _DEFAULT_BATCH_LIMIT;
    }

    /**
     * Gets the input.
     *
     * @return Returns the input.
     */
    @Nonnull
    @CheckReturnValue
    protected final ModuleInput getInput()
    {
        return Require.notNull(_input);
    }

    /**
     * Outputs messages.
     *
     * @param messages The messages.
     *
     * @return True on success.
     *
     * @throws InterruptedException When the module input is stopped.
     * @throws ServiceNotAvailableException When the service is not available.
     */
    @CheckReturnValue
    protected final boolean output(
            @Nonnull final Serializable[] messages)
        throws InterruptedException, ServiceNotAvailableException
    {
        return getForwarderAppImpl().output(messages);
    }

    /**
     * Sets the input.
     *
     * @param input The input.
     */
    protected final void setInput(@Nonnull final ModuleInput input)
    {
        _input = input;
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp(final KeyedGroups moduleProperties)
    {
        final ModuleInput input = _input;

        if (input != null) {
            if (!input.setUp(moduleProperties)) {
                return false;
            }

            if (!getReliable().isPresent()) {
                setReliable(Boolean.valueOf(input.isReliable()));
            }
        }

        return true;
    }

    private static final int _DEFAULT_BATCH_LIMIT = 100;

    private volatile ModuleInput _input;
    private final Object _mutex = new Object();
    private final AtomicReference<ServiceThread> _thread =
        new AtomicReference<>();

    /**
     * Module input.
     */
    protected interface ModuleInput
        extends ModuleInputOutput
    {
        /**
         * Gets the source's name.
         *
         * @return The source's name.
         */
        @Nonnull
        @CheckReturnValue
        String getSourceName();

        /**
         * Inputs messages.
         *
         * @param batchControl The batch control.
         *
         * @return The optional messages.
         *
         * @throws InterruptedException When the service is stopped.
         */
        @Nonnull
        @CheckReturnValue
        Optional<Serializable[]> input(
                @Nonnull BatchControl batchControl)
            throws InterruptedException;

        /**
         * Rolls back.
         *
         * @return True on success.
         */
        @CheckReturnValue
        boolean rollback();
    }


    /**
     * Abstract input.
     */
    protected abstract static class AbstractInput
        extends AbstractInputOutput
        implements ModuleInput
    {
        /** {@inheritDoc}
         */
        @Override
        public boolean rollback()
        {
            return true;
        }
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
