/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MessagesPortClientModule.java 4055 2019-06-04 13:05:05Z SFB $
 */

package org.rvpf.forwarder.output;

import java.io.Serializable;

import java.util.Optional;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.util.LoginInfo;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.xml.streamer.StreamedMessagesPortClient;
import org.rvpf.forwarder.ForwarderMessages;

/**
 * Messages port client module.
 */
public final class MessagesPortClientModule
    extends OutputModule
{
    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp(final KeyedGroups moduleProperties)
    {
        setOutput(
            new _MessagesPortClient(
                moduleProperties.getString(CLIENT_NAME_PROPERTY).get()));

        return super.setUp(moduleProperties);
    }

    /** The server port address. */
    public static final String ADDRESS_PROPERTY = "address";

    /** Client name property. */
    public static final String CLIENT_NAME_PROPERTY = "client.name";

    private final class _MessagesPortClient
        extends AbstractOutput
    {
        /**
         * Constructs an instance.
         *
         * @param clientName The client name.
         */
        _MessagesPortClient(final String clientName)
        {
            _portClient = new StreamedMessagesPortClient(clientName);
        }

        /** {@inheritDoc}
         */
        @Override
        public void close()
        {
            _portClient.close();
        }

        /** {@inheritDoc}
         */
        @Override
        public String getDestinationName()
        {
            return _addressString;
        }

        /** {@inheritDoc}
         */
        @Override
        public String getDisplayName()
        {
            return "Messages port client";
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isClosed()
        {
            return !_portClient.isOpen();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isOpen()
        {
            return _portClient.isOpen();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean open()
            throws InterruptedException
        {
            try {
                _portClient.open(_addressString, Optional.of(_securityContext));
            } catch (final ServiceNotAvailableException exception) {
                return false;
            }

            try {
                _portClient.login(_loginInfo);
            } catch (final ServiceNotAvailableException exception) {
                close();

                return false;
            }

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean output(
                final Serializable[] messages)
            throws InterruptedException
        {
            for (final Serializable message: messages) {
                _portClient.addMessage(message);
            }

            try {
                _portClient.sendMessages(true);
            } catch (final ServiceNotAvailableException exception) {
                return false;
            }

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean setUp(final KeyedGroups moduleProperties)
        {
            if (!super.setUp(moduleProperties)) {
                return false;
            }

            if (!_portClient
                .setUp(Optional.of(getConfigProperties()), Optional.empty())) {
                return false;
            }

            _addressString = moduleProperties
                .getString(ADDRESS_PROPERTY)
                .orElse(null);

            if (_addressString == null) {
                getThisLogger()
                    .error(BaseMessages.MISSING_PROPERTY, ADDRESS_PROPERTY);

                return false;
            }

            getThisLogger().info(ForwarderMessages.ADDRESS, _addressString);

            _loginInfo = new LoginInfo(
                moduleProperties.getString(USER_PROPERTY),
                moduleProperties.getPassword(PASSWORD_PROPERTY));

            _securityContext = new SecurityContext(getThisLogger());

            return _securityContext
                .setUp(
                    getConfigProperties(),
                    moduleProperties
                        .getGroup(SecurityContext.SECURITY_PROPERTIES));
        }

        /** {@inheritDoc}
         */
        @Override
        public void tearDown()
        {
            close();

            super.tearDown();
        }

        private String _addressString;
        private LoginInfo _loginInfo;
        private final StreamedMessagesPortClient _portClient;
        private SecurityContext _securityContext;
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
