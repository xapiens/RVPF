/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PAPServer.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;

/**
 * PAP server.
 */
public interface PAPServer
{
    /**
     * Adds a point value.
     *
     * @param pointValue The point value.
     */
    void addPointValue(@Nonnull PointValue pointValue);

    /**
     * Gets the responder.
     *
     * @return The optional responder.
     */
    @Nonnull
    @CheckReturnValue
    Optional<PAPProxy.Responder> getResponder();

    /**
     * Gets the logger.
     *
     * @return The logger.
     */
    @Nonnull
    @CheckReturnValue
    Logger getThisLogger();

    /**
     * Asks if we are currently updating.
     *
     * @return True if updating.
     */
    @CheckReturnValue
    boolean isUpdating();

    /**
     * Returns the next update.
     *
     * @param timeout A time limit in millis to wait for a point value (negative
     *                for infinite).
     *
     * @return The next update (empty if none).
     *
     * @throws InterruptedException When interrupted.
     */
    @Nonnull
    @CheckReturnValue
    Optional<PointValue> nextUpdate(long timeout)
        throws InterruptedException;

    /**
     * Sets the responder.
     *
     * @param responder The responder.
     */
    void setResponder(@Nonnull final PAPProxy.Responder responder);

    /**
     * Sets up.
     *
     * @param serverProperties The protocol specific server properties.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUp(@Nonnull KeyedGroups serverProperties);

    /**
     * Sets up a listener.
     *
     * @param listenerProperties The listener properties.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUpListener(@Nonnull KeyedGroups listenerProperties);

    /**
     * Starts.
     */
    void start();

    /**
     * Stops.
     */
    void stop();

    /**
     * Tears down what has been set up.
     */
    void tearDown();

    /**
     * Abstract.
     */
    abstract class Abstract
        implements PAPServer
    {
        /**
         * Constructs an instance.
         */
        protected Abstract() {}

        /** {@inheritDoc}
         */
        @Override
        public Optional<PAPProxy.Responder> getResponder()
        {
            return Optional.ofNullable(_responder);
        }

        /** {@inheritDoc}
         */
        @Override
        public Logger getThisLogger()
        {
            return _logger;
        }

        /** {@inheritDoc}
         */
        @Override
        public void setResponder(final PAPProxy.Responder responder)
        {
            _responder = Require.notNull(responder);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean setUp(final KeyedGroups serverProperties)
        {
            final KeyedGroups[] listenersProperties = serverProperties
                .getGroups(PAP.LISTENER_PROPERTIES);
            boolean success = listenersProperties.length > 0;

            if (!success) {
                getThisLogger().warn(PAPMessages.NO_LISTENERS);
            }

            for (final KeyedGroups listenerProperties: listenersProperties) {
                success &= setUpListener(listenerProperties);
            }

            return success;
        }

        /**
         * Gets the context.
         *
         * @return The context.
         */
        @Nonnull
        @CheckReturnValue
        protected abstract PAPContext getContext();

        private final Logger _logger = Logger.getInstance(getClass());
        private PAPProxy.Responder _responder;
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
