/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.pap.dnp3.object;

import java.io.IOException;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.pap.ListenerManager;

/**
 * Object event listener.
 */
public interface ObjectEventListener
{
    /**
     * Called when an object event is received.
     *
     * @param pointType The point type.
     * @param objectInstance The object instance.
     *
     * @return True if this event has been handled.
     *
     * @throws IOException On I/O exception.
     */
    @CheckReturnValue
    boolean onEventReceived(
            @Nonnull PointType pointType,
            @Nonnull ObjectInstance objectInstance)
        throws IOException;

    /**
     * Received message listener manager.
     */
    class Manager
        extends ListenerManager<ObjectEventListener>
        implements ObjectEventListener
    {
        /** {@inheritDoc}
         */
        @Override
        public boolean onEventReceived(
                @Nonnull final PointType pointType,
                @Nonnull final ObjectInstance objectInstance)
            throws IOException
        {
            for (final ObjectEventListener listener: getListeners()) {
                if (listener.onEventReceived(pointType, objectInstance)) {
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
