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
 * Association listener.
 */
public interface AssociationListener
{
    /**
     * Called on a new association.
     *
     * @param association The association.
     *
     * @return True if this event has been handled.
     *
     * @throws IOException On I/O exception.
     */
    @CheckReturnValue
    boolean onNewAssociation(
            @Nonnull Association association)
        throws IOException;

    /**
     * Association listener manager.
     */
    class Manager
        extends ListenerManager<AssociationListener>
        implements AssociationListener
    {
        /** {@inheritDoc}
     */
        @Override
        public boolean onNewAssociation(
                final Association association)
            throws IOException
        {
            for (final AssociationListener listener: getListeners()) {
                if (listener.onNewAssociation(association)) {
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
