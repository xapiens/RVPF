/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.pap;

import java.io.IOException;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.logger.Logger;

/**
 * PAP connection.
 */
public interface PAPConnection
{
    /**
     * Closes.
     */
    void close();

    /**
     * Asks if this connection is closed.
     *
     * @return True if it is closed.
     */
    @CheckReturnValue
    boolean isClosed();

    /**
     * Abstract connection.
     */
    abstract class Abstract
        implements PAPConnection
    {
        /** {@inheritDoc}
         */
        @Override
        public final void close()
        {
            if (_closed.compareAndSet(false, true)) {
                try {
                    doClose();
                } catch (final IOException exception) {
                    getThisLogger()
                        .debug(PAPMessages.CLOSE_FAILED, getName(), exception);
                }
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public final boolean isClosed()
        {
            return _closed.get();
        }

        /**
         * Does close.
         *
         * @throws IOException To be logged.
         */
        protected abstract void doClose()
            throws IOException;

        /**
         * Gets a name for this connection.
         *
         * @return The name.
         */
        @Nonnull
        @CheckReturnValue
        protected abstract String getName();

        /**
         * Gets the logger for this instance.
         *
         * @return The logger for this instance.
         */
        @Nonnull
        @CheckReturnValue
        protected final Logger getThisLogger()
        {
            return Logger.getInstance(getClass());
        }

        private final AtomicBoolean _closed = new AtomicBoolean();
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
