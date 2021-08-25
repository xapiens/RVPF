/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PAPConnectionListener.java 4084 2019-06-15 18:32:47Z SFB $
 */

package org.rvpf.pap;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * PAP connection listener.
 */
public interface PAPConnectionListener
{
    /**
     * Called on connection lost.
     *
     * @param remoteProxy The previously connected proxy.
     * @param cause The cause.
     *
     * @return True if this event has been handled.
     */
    @CheckReturnValue
    boolean onLostConnection(
            @Nonnull PAPProxy remoteProxy,
            @Nonnull Optional<Exception> cause);

    /**
     * Called on a new connection.
     *
     * @param remoteProxy The newly connected proxy.
     *
     * @return True if this event has been handled.
     */
    @CheckReturnValue
    boolean onNewConnection(@Nonnull PAPProxy remoteProxy);

    /**
     * Manager.
     */
    class Manager
        extends ListenerManager<PAPConnectionListener>
        implements PAPConnectionListener
    {
        /** {@inheritDoc}
         */
        @Override
        public boolean onLostConnection(
                final PAPProxy remoteProxy,
                final Optional<Exception> cause)
        {
            for (final PAPConnectionListener listener: getListeners()) {
                if (listener.onLostConnection(remoteProxy, cause)) {
                    return true;
                }
            }

            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean onNewConnection(final PAPProxy remoteProxy)
        {
            for (final PAPConnectionListener listener: getListeners()) {
                if (listener.onNewConnection(remoteProxy)) {
                    return true;
                }
            }

            return false;
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
