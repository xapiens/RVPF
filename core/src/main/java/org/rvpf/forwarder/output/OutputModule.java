/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: OutputModule.java 3948 2019-05-02 20:37:43Z SFB $
 */

package org.rvpf.forwarder.output;

import java.io.Serializable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Message;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.forwarder.ForwarderMessages;
import org.rvpf.forwarder.ForwarderModule;

/**
 * Output module.
 */
public abstract class OutputModule
    extends ForwarderModule.Abstract
{
    /**
     * Commits the messages transmission.
     *
     * @return True on success.
     *
     * @throws InterruptedException When the service is stopped.
     * @throws ServiceNotAvailableException When the service is not available.
     */
    @CheckReturnValue
    public boolean commit()
        throws InterruptedException, ServiceNotAvailableException
    {
        if (!_output.commit()) {
            return _failed();
        }

        return true;
    }

    /**
     * Outputs messages.
     *
     * @param messages The messages.
     *
     * @return True on success.
     *
     * @throws InterruptedException When the module output is stopped.
     * @throws ServiceNotAvailableException When the service is not available.
     */
    @CheckReturnValue
    public boolean output(
            @Nonnull Serializable[] messages)
        throws InterruptedException, ServiceNotAvailableException
    {
        messages = filter(messages);

        if (messages.length > 0) {
            if (!_output.isOpen()) {
                open(_output);
            }

            if (!_output.output(messages)) {
                return _failed();
            }

            if (getThisLogger().isDebugEnabled()) {
                final String to = _output.getDestinationName();

                for (final Serializable message: messages) {
                    getThisLogger()
                        .trace(ForwarderMessages.MESSAGE_TO, to, message);
                }
            }
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
        throws InterruptedException, ServiceNotAvailableException
    {
        if (_output != null) {
            if (isReliable()) {
                open(_output);
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        if (_output != null) {
            _output.close();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        if (_output != null) {
            _output.tearDown();
            _output = null;
        }

        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    protected int getDefaultBatchLimit()
    {
        return DEFAULT_BATCH_LIMIT;
    }

    /**
     * Asks if this module is alone.
     *
     * @return True if this module is alone.
     */
    protected final boolean isAlone()
    {
        return getForwarderAppImpl().getOutputsSize() == 1;
    }

    /**
     * Opens the output.
     *
     * @param output The output.
     *
     * @throws InterruptedException When appropriate.
     * @throws ServiceNotAvailableException When the service is not available.
     */
    protected final void open(
            @Nonnull final ModuleOutput output)
        throws InterruptedException, ServiceNotAvailableException
    {
        boolean loggedFailed = false;

        for (;;) {
            if (output.open()) {
                if (!isReliable()) {
                    final Message message = new Message(
                        ForwarderMessages.CONNECTION_COMPLETED,
                        output.getDestinationName());

                    if (_failed) {
                        getThisLogger().info(message);
                        _failed = false;
                    } else {
                        getThisLogger().debug(message);
                    }
                }

                break;
            }

            if (isReliable()) {
                getThisLogger()
                    .warn(
                        ForwarderMessages.CONNECTION_FAILED,
                        output.getDestinationName());

                throw new ServiceNotAvailableException();
            }

            if (!loggedFailed) {
                getThisLogger()
                    .warn(
                        ForwarderMessages.CONNECTION_FAILED_SLEEPING,
                        output.getDestinationName());
                loggedFailed = true;
                _failed = true;
            }

            getService().snooze(getConnectionRetryDelay());
        }
    }

    /**
     * Sets the output.
     *
     * @param output The output.
     */
    protected final void setOutput(@Nonnull final ModuleOutput output)
    {
        _output = output;
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp(final KeyedGroups moduleProperties)
    {
        final ModuleOutput output = _output;

        if (output != null) {
            if (!output.setUp(moduleProperties)) {
                return false;
            }

            if (!getReliable().isPresent()) {
                setReliable(Boolean.valueOf(output.isReliable()));
            }
        }

        return true;
    }

    private boolean _failed()
        throws ServiceNotAvailableException
    {
        getThisLogger().warn(ForwarderMessages.CONNECTION_LOST);
        _output.close();

        if (isReliable() && !_output.isClosed()) {
            throw new ServiceNotAvailableException();
        }

        _failed = true;

        return false;
    }

    /** Default batch limit. */
    public static final int DEFAULT_BATCH_LIMIT = Integer.MAX_VALUE;

    private boolean _failed;
    private ModuleOutput _output;

    /**
     * Module output.
     */
    protected interface ModuleOutput
        extends ModuleInputOutput
    {
        /**
         * Gets the destination's name.
         *
         * @return The destination's name.
         */
        @Nonnull
        @CheckReturnValue
        String getDestinationName();

        /**
         * Asks if this module has been opened.
         *
         * @return True if it has been opened.
         */
        @CheckReturnValue
        boolean isOpen();

        /**
         * Outputs messages.
         *
         * @param messages The messages.
         *
         * @return True on success.
         *
         * @throws InterruptedException When the service is stopped.
         */
        @CheckReturnValue
        boolean output(
                @Nonnull Serializable[] messages)
            throws InterruptedException;
    }


    /**
     * Abstract output.
     */
    protected abstract static class AbstractOutput
        extends AbstractInputOutput
        implements ModuleOutput {}
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
