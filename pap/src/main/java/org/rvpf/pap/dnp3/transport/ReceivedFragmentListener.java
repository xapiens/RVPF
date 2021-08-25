/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.pap.dnp3.transport;

import java.io.IOException;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.pap.ListenerManager;

/**
 * Received fragment listener.
 */
public interface ReceivedFragmentListener
{
    /**
     * Called when a fragment is received.
     * @param receivedFragment The received fragment.
     *
     * @return True if this event has been handled.
     *
     * @throws IOException On I/O exception.
     */
    @CheckReturnValue
    boolean onReceivedFragment(
            @Nonnull Fragment receivedFragment)
        throws IOException;

    /**
     * Received fragment listener manager.
     */
    class Manager
        extends ListenerManager<ReceivedFragmentListener>
        implements ReceivedFragmentListener
    {
        /** {@inheritDoc}
         */
        @Override
        public boolean onReceivedFragment(
                final Fragment receivedFragment)
            throws IOException
        {
            for (final ReceivedFragmentListener listener: getListeners()) {
                if (listener.onReceivedFragment(receivedFragment)) {
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
