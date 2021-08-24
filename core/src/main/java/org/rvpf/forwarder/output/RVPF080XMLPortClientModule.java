/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RVPF080XMLPortClientModule.java 4059 2019-06-05 20:44:44Z SFB $
 */

package org.rvpf.forwarder.output;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

import java.nio.charset.StandardCharsets;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.UUID;
import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.tool.Inet;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.forwarder.ForwarderMessages;
import org.rvpf.service.ServiceMessages;

/**
 * RVPF 0.8.0 XML port client.
 */
public final class RVPF080XMLPortClientModule
    extends OutputModule
{
    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp(final KeyedGroups moduleProperties)
    {
        setOutput(
            new _XMLPortClient(
                moduleProperties.getString(CLIENT_NAME_PROPERTY).orElse(null)));

        return super.setUp(moduleProperties);
    }

    /**
     * Escapes element entities.
     *
     * @param text The source text.
     * @param quote The quote character ('"', '\'' or '\0').
     *
     * @return The encoded text.
     */
    static String _escapeEntities(final CharSequence text, final char quote)
    {
        if (text == null) {
            return null;
        }

        final StringBuilder stringBuilder = new StringBuilder(text.length());

        for (int i = 0; i < text.length(); ++i) {
            final char next = text.charAt(i);
            final String entity;

            switch (next) {
                case '<': {
                    entity = "&lt;";

                    break;
                }
                case '>': {
                    entity = "&gt;";

                    break;
                }
                case '&': {
                    entity = "&amp;";

                    break;
                }
                case '"': {
                    entity = (quote == next)? "&quot;": null;

                    break;
                }
                case '\'': {
                    entity = (quote == next)? "&apos;": null;

                    break;
                }
                case '\t':
                case '\n':
                case '\r': {
                    entity = null;

                    break;
                }
                default: {
                    entity = (next < ' ')? ("&#" + (int) next + ";"): null;

                    break;
                }
            }

            if (entity != null) {
                stringBuilder.append(entity);
            } else {
                stringBuilder.append(next);
            }
        }

        return stringBuilder.toString();
    }

    /** The server port address. */
    public static final String ADDRESS_PROPERTY = "address";

    /** Client name property. */
    public static final String CLIENT_NAME_PROPERTY = "client.name";
    static final char[] EMPTY_PASSWORD = new char[0];
    static final Pattern RESPONSE_PATTERN = Pattern
        .compile("<done ref='([0-9]++)'/>");

    /**
     * XML port client.
     */
    private final class _XMLPortClient
        extends AbstractOutput
    {
        /**
         * Constructs an instance.
         *
         * @param clientName The client name.
         */
        _XMLPortClient(final String clientName)
        {
            _clientName = clientName;
        }

        /**
         * Closes the connection.
         */
        @Override
        public void close()
        {
            if (isOpen()) {
                try {
                    if (_transacted) {
                        _endTransaction();
                    }

                    if (_output != null) {
                        _output.close();
                    }

                    if (_input != null) {
                        _input.close();
                    }
                } catch (final SocketException exception) {
                    // Ignored.
                } catch (final IOException exception) {
                    throw new RuntimeException(exception);
                } finally {
                    try {
                        _socket.close();
                    } catch (final IOException exception) {
                        // Ignored.
                    }
                }

                _socket = null;
                _output = null;
                _input = null;
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean commit()
        {
            if (_transacted) {
                try {
                    _endTransaction();
                } catch (final SocketException exception) {
                    return false;
                } catch (final IOException exception) {
                    throw new RuntimeException(exception);
                }
            }

            return super.commit();
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
            return "RVPF 0.8.0 XML port client";
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isClosed()
        {
            return !isOpen();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isOpen()
        {
            return _socket != null;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean open()
            throws InterruptedException
        {
            final Optional<InetSocketAddress> socketAddress = Inet
                .socketAddress(_addressString);

            if (!socketAddress.isPresent()) {
                throw new IllegalArgumentException(
                    "Bad address value: " + _addressString);
            }

            final boolean secure = _securityContext.isCertified()
                    || _securityContext.isSecure()
                    || !Inet.isOnLocalHost(socketAddress.get().getAddress());

            try {
                if (secure) {    // Uses a SSL connection.
                    _securityContext.checkForSecureOperation();

                    final SSLContext sslContext = _securityContext
                        .createSSLContext();

                    _socket = sslContext.getSocketFactory().createSocket();
                    _socket.connect(socketAddress.get());
                    ((SSLSocket) _socket).startHandshake();
                } else {    // Uses a normal connection.
                    _socket = new Socket();
                    _socket.connect(socketAddress.get());
                }

                _output = new BufferedOutputStream(_socket.getOutputStream());
                _input = new BufferedInputStream(_socket.getInputStream());

                if (_user != null) {
                    _login();
                }
            } catch (final SocketException exception) {
                getThisLogger()
                    .trace(
                        ServiceMessages.CONNECTION_FAILED,
                        socketAddress.get(),
                        exception.getMessage());
                _socket = null;

                return false;
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
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
            try {
                for (final Serializable message: messages) {
                    if (message instanceof PointValue) {
                        _sendValue((PointValue) message);
                    }
                }
            } catch (final SocketException exception) {
                return false;
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
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

            _addressString = moduleProperties
                .getString(ADDRESS_PROPERTY)
                .orElse(null);

            if (_addressString == null) {
                getThisLogger()
                    .error(BaseMessages.MISSING_PROPERTY, ADDRESS_PROPERTY);

                return false;
            }

            getThisLogger().info(ForwarderMessages.ADDRESS, _addressString);

            _user = moduleProperties.getString(USER_PROPERTY).orElse(null);

            if (_user != null) {
                _password = moduleProperties
                    .getPassword(PASSWORD_PROPERTY)
                    .orElse(null);

                if (_password == null) {
                    _password = EMPTY_PASSWORD;
                }
            }

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

        private void _beginTransaction()
            throws IOException
        {
            final StringBuilder text = new StringBuilder();

            text.append('<');
            text.append(_DATA_ELEMENT);
            text.append(' ');
            text.append(_ID_ATTRIBUTE);
            text.append("='");
            text.append(++_id);
            text.append("'>\n");

            _sendText(text);

            _transacted = true;
        }

        private void _endTransaction()
            throws IOException
        {
            _sendText("</" + _DATA_ELEMENT + ">\n");

            _verifyResponse(_id);

            _transacted = false;
        }

        private void _login()
            throws IOException
        {
            final StringBuilder text = new StringBuilder();

            text.append('<');
            text.append(_LOGIN_ELEMENT);
            text.append(' ');

            if (_clientName != null) {
                text.append(_CLIENT_ATTRIBUTE);
                text.append("='");
                text.append(_escapeEntities(_clientName, '\''));
                text.append("' ");
            }

            text.append(_ID_ATTRIBUTE);
            text.append("='");
            text.append(++_id);
            text.append("' ");
            text.append(_USER_ATTRIBUTE);
            text.append("='");
            text.append(_escapeEntities(_user, '\''));
            text.append("' ");
            text.append(_PASSWORD_ATTRIBUTE);
            text.append("='");
            text.append(_escapeEntities(new String(_password), '\''));
            text.append("'/>\n");

            _sendText(text);
            _verifyResponse(_id);
        }

        private void _sendText(final CharSequence text)
            throws IOException
        {
            final byte[] bytes = text
                .toString()
                .getBytes(StandardCharsets.UTF_8);

            _output.write(bytes);
        }

        private void _sendValue(final PointValue pointValue)
            throws IOException
        {
            final StringBuilder text = new StringBuilder();
            final UUID uuid = pointValue
                .hasPointUUID()? pointValue.getPointUUID(): null;
            final Serializable state = pointValue.getState();

            if (!_transacted) {
                _beginTransaction();
            }

            text.append('<');
            text.append(_ENTRY_ELEMENT);
            text.append(' ');

            text.append(_POINT_ATTRIBUTE);
            text.append("='");
            text
                .append(
                    (uuid != null)? uuid.toString(): _escapeEntities(
                        pointValue.getPointName().orElse(null),
                        '\''));
            text.append("' ");

            text.append(_STAMP_ATTRIBUTE);
            text.append("='");
            text.append(pointValue.getStamp().toHexString());
            text.append('\'');

            if (state == _DELETED_STATE) {
                text.append(' ');
                text.append(_DELETED_ATTRIBUTE);
                text.append("='yes'/>");
            } else {
                if (state != null) {
                    text.append(' ');
                    text.append(_STATE_ATTRIBUTE);
                    text.append("='");
                    text.append(_escapeEntities(state.toString().trim(), '\''));
                    text.append('\'');
                }

                if (pointValue.getValue() != null) {
                    final String value = pointValue
                        .getValue()
                        .toString()
                        .trim();

                    if (value.isEmpty()) {
                        text.append(' ');
                        text.append(_VALUE_ATTRIBUTE);
                        text.append("=''/>");
                    } else {
                        text.append('>');
                        text.append(_escapeEntities(value, '\0'));
                        text.append("</");
                        text.append(_ENTRY_ELEMENT);
                        text.append('>');
                    }
                } else {
                    text.append("/>");
                }
            }

            text.append('\n');

            _sendText(text);
        }

        private void _verifyResponse(final long id)
            throws IOException
        {
            _output.flush();

            final StringBuilder stringBuilder = new StringBuilder();

            for (;;) {
                final int next = _input.read();

                if (next < 0) {
                    throw new EOFException();
                }

                if (next == '\n') {
                    break;
                }

                stringBuilder.append((char) next);
            }

            final Matcher matcher = RESPONSE_PATTERN.matcher(stringBuilder);

            if (!matcher.matches()) {
                throw new IOException("Unexpected response: " + stringBuilder);
            }

            if (Long.parseLong(matcher.group(1)) != id) {
                throw new IOException(
                    "Mismatched transaction id: expected '" + id
                    + "' but received '" + matcher.group(
                        1) + "'");
            }
        }

        private static final String _CLIENT_ATTRIBUTE = "client";
        private static final String _DATA_ELEMENT = "data";
        private static final String _DELETED_ATTRIBUTE = "deleted";
        private static final String _DELETED_STATE = "data";
        private static final String _ENTRY_ELEMENT = "entry";
        private static final String _ID_ATTRIBUTE = "id";
        private static final String _LOGIN_ELEMENT = "login";
        private static final String _PASSWORD_ATTRIBUTE = "password";
        private static final String _POINT_ATTRIBUTE = "point";
        private static final String _STAMP_ATTRIBUTE = "stamp";
        private static final String _STATE_ATTRIBUTE = "state";
        private static final String _USER_ATTRIBUTE = "user";
        private static final String _VALUE_ATTRIBUTE = "value";

        private String _addressString;
        private final String _clientName;
        private long _id;
        private InputStream _input;
        private OutputStream _output;
        private char[] _password;
        private SecurityContext _securityContext;
        private Socket _socket;
        private boolean _transacted;
        private String _user;
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
