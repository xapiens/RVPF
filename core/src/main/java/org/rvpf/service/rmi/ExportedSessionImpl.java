/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ExportedSessionImpl.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.service.rmi;

import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.service.ServiceClassLoader;
import org.rvpf.service.ServiceMessages;

/**
 * RMI exported session implementation.
 */
@ThreadSafe
public abstract class ExportedSessionImpl
    extends SessionImpl
{
    /**
     * Constructs an instance.
     *
     * @param clientName A descriptive name for the client.
     * @param sessionFactory The factory creating this.
     * @param connectionMode The connection mode.
     */
    protected ExportedSessionImpl(
            @Nonnull final String clientName,
            @Nonnull final SessionFactory sessionFactory,
            @Nonnull final ConnectionMode connectionMode)
    {
        super(clientName, sessionFactory, connectionMode);
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized void close()
    {
        if (!isClosed()) {
            super.close();

            if (_stub != null) {
                if (!ServiceRegistry.isPrivate()) {
                    final Optional<ServiceClassLoader> savedClassLoader =
                        ServiceClassLoader
                            .hideInstance();

                    try {
                        UnicastRemoteObject.unexportObject(this, true);
                    } catch (final NoSuchObjectException exception) {
                        throw new RuntimeException(exception);
                    } finally {
                        ServiceClassLoader.restoreInstance(savedClassLoader);
                    }

                    getThisLogger()
                        .trace(ServiceMessages.UNEXPORTED_RMI, this, _stub);
                }

                _stub = null;
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean equals(final Object object)
    {
        if (object == null) {
            return false;
        }

        return (object == this) || ((this != _stub) && object.equals(_stub));
    }

    /** {@inheritDoc}
     */
    @Override
    public final int hashCode()
    {
        final Remote stub = _stub;

        return (stub != null)? stub.hashCode(): super.hashCode();
    }

    /**
     * Opens this.
     *
     * @param clientSocketFactory The RMI client socket factory instance.
     * @param serverSocketFactory The RMI server socket factory instance.
     */
    public synchronized void open(
            @Nullable final RMIClientSocketFactory clientSocketFactory,
            @Nullable final RMIServerSocketFactory serverSocketFactory)
    {
        Require.success(_stub == null);

        open();

        if (ServiceRegistry.isPrivate()) {
            _stub = this;
        } else {
            final Optional<String> savedLogID = Logger.currentLogID();
            final Optional<ServiceClassLoader> savedClassLoader =
                ServiceClassLoader
                    .hideInstance();

            try {
                Logger.restoreLogID(Optional.empty());
                _stub = UnicastRemoteObject
                    .exportObject(
                        this,
                        0,
                        clientSocketFactory,
                        serverSocketFactory);
            } catch (final RemoteException exception) {
                throw new RuntimeException(exception);
            } finally {
                ServiceClassLoader.restoreInstance(savedClassLoader);
                Logger.restoreLogID(savedLogID);
            }

            Require.notNull(_stub);
            getThisLogger().trace(ServiceMessages.EXPORTED_RMI, this, _stub);
        }
    }

    private volatile Remote _stub;
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
