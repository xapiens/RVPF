/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMSessionImpl.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.som;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.rvpf.base.logger.Message;
import org.rvpf.base.som.SOMSession;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.rmi.ExportedSessionImpl;
import org.rvpf.service.rmi.SessionFactory;

/** SOM Session.
 */
@ThreadSafe
public abstract class SOMSessionImpl
    extends ExportedSessionImpl
    implements SOMSession
{
    /** Constructs an instance.
     *
     * @param sessionFactory The factory creating this.
     * @param connectionMode The connection mode.
     * @param clientName A descriptive name for the client.
     */
    protected SOMSessionImpl(
            @Nonnull final SOMServerImpl sessionFactory,
            @Nonnull final ConnectionMode connectionMode,
            @Nonnull final String clientName)
    {
        super(clientName, sessionFactory, connectionMode);

        _keepAlive = sessionFactory.getKeepAlive()
            .toMillis();
    }

    /** {@inheritDoc}
     */
    @Override
    public final String toString()
    {
        final SessionFactory sessionFactory = getSessionFactory();
        final String name = ((SOMServerImpl) sessionFactory).getName();

        return Message.format(
                ServiceMessages.SOM_SESSION,
                getType(),
                name,
                super.toString());
    }

    /** Adjusts the timeout with the keep-alive interval.
     *
     * @param timeout The specified timeout.
     *
     * @return The adjusted timeout.
     */
    @CheckReturnValue
    protected long adjustTimeout(long timeout)
    {
        if (_keepAlive > 0) {
            if ((timeout < 0) || (timeout > _keepAlive)) {
                timeout = _keepAlive;
            }
        }

        return timeout;
    }

    /** Gets the session type.
     *
     * @return The session type.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract String getType();

    private final long _keepAlive;
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
