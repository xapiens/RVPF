/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Session.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.base.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Messages;

/**
 * Remote object session.
 *
 * <p>A remote object implements a session when it has security requirements. A
 * session allows a client to provide identification and authentification. The
 * mode of connection to the remote object may have an effect on the security
 * constraints.</p>
 *
 * <p>A session object is created by a remote session factory object with an
 * appropriate connection mode.</p>
 */
public interface Session
    extends Remote
{
    /**
     * Gets the connection mode.
     *
     * <p>This method may be called before or after {@link #login}.</p>
     *
     * @return The connection mode.
     *
     * @throws RemoteException From RMI RunTime.
     */
    @Nullable
    @CheckReturnValue
    ConnectionMode getConnectionMode()
        throws RemoteException;

    /**
     * Identifies and authenticates the identity associated with the Session.
     *
     * @param identifier The identifier for the identity.
     * @param password The password authenticating the identity.
     *
     * @throws RemoteException From RMI RunTime.
     * @throws SessionException From the session implementation.
     */
    void login(
            @Nullable String identifier,
            @Nullable char[] password)
        throws RemoteException, SessionException;

    /**
     * Logs out.
     *
     * @throws RemoteException From RMI RunTime.
     * @throws SessionException When the session logout fails.
     */
    void logout()
        throws RemoteException, SessionException;

    /**
     * Connection mode.
     *
     * <p>The connection mode indicates if the socket listening for the session
     * is secure (SSL) and certified (client certificate).</p>
     *
     * <p>The string representation may be used to identify the connection mode
     * in a text message.</p>
     */
    enum ConnectionMode
    {
        /** A private connection. */
        PRIVATE(BaseMessages.CONNECTION_PRIVATE),

        /** A direct connection thru a local interface. */
        LOCAL(BaseMessages.CONNECTION_LOCAL),

        /** A SSL connection thru any interface. */
        SECURE(BaseMessages.CONNECTION_SECURE),

        /** A SSL connection certified by a verified client certificate. */
        CERTIFIED(BaseMessages.CONNECTION_CERTIFIED);

        /**
         * Constructs an instance.
         *
         * @param messagesEntry The messages entry.
         */
        ConnectionMode(@Nonnull final Messages.Entry messagesEntry)
        {
            _messagesEntry = messagesEntry;
        }

        /**
         * Asks if the connection is certified.
         *
         * @return True if the connection is certified.
         */
        @CheckReturnValue
        public boolean isCertified()
        {
            return this == CERTIFIED;
        }

        /**
         * Asks if the connection is local.
         *
         * @return True if the connection is local.
         */
        @CheckReturnValue
        public boolean isLocal()
        {
            return this == LOCAL;
        }

        /**
         * Asks if the connection is private.
         *
         * @return True if the connection is private.
         */
        @CheckReturnValue
        public boolean isPrivate()
        {
            return this == PRIVATE;
        }

        /**
         * Asks if the connection is secure.
         *
         * @return True if the connection is secure.
         */
        @CheckReturnValue
        public boolean isSecure()
        {
            return !isLocal();
        }

        /** {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return _messagesEntry.toString();
        }

        private final Messages.Entry _messagesEntry;
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
