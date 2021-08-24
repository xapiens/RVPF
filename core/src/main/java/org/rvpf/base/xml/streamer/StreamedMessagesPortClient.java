/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StreamedMessagesPortClient.java 4096 2019-06-24 23:07:39Z SFB $
 */

package org.rvpf.base.xml.streamer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

import java.nio.charset.StandardCharsets;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.tool.Inet;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.LoginInfo;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.base.xml.XMLDocument.ParseException;
import org.rvpf.base.xml.XMLElement;

/**
 * Streamed messages port client.
 */
@NotThreadSafe
public class StreamedMessagesPortClient
    extends StreamedMessagesAccess
{
    /**
     * Constructs an instance.
     *
     * @param client The client identification.
     */
    public StreamedMessagesPortClient(@Nonnull final String client)
    {
        _client = client.trim();
    }

    /**
     * Adds a message.
     *
     * @param message The message.
     */
    public void addMessage(@Nonnull final Serializable message)
    {
        _messages.add(Require.notNull(message));
        ++_pending;
    }

    /**
     * Closes the connection.
     */
    public void close()
    {
        if (isOpen()) {
            try {
                flush();

                if (_output != null) {
                    _output.close();
                }

                if (_input != null) {
                    _input.close();
                }
            } catch (final ServiceNotAvailableException exception) {
                // Ignored.
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
            _messages.clear();
            _pending = 0;
            _id = 0;
        }
    }

    /**
     * Flushes pending entries.
     *
     * @throws ServiceNotAvailableException When the service is not available.
     */
    public void flush()
        throws ServiceNotAvailableException
    {
        if (_pending > 0) {
            if (_messages.isEmpty()) {
                _sendXML(new XMLElement(FLUSH_ELEMENT));
            } else {
                sendMessages(true);
            }

            _pending = 0;
        }
    }

    /**
     * Asks if this is open.
     *
     * @return True if this is open.
     */
    @CheckReturnValue
    public boolean isOpen()
    {
        return _socket != null;
    }

    /**
     * Logs in.
     *
     * @param loginInfo The login info.
     *
     * @throws ServiceNotAvailableException When the service is not available.
     */
    public void login(
            @Nonnull final LoginInfo loginInfo)
        throws ServiceNotAvailableException
    {
        Require.success(_pending == 0, BaseMessages.PENDING_MESSAGES);

        if (!loginInfo.getUser().isPresent()) {
            return;
        }

        final XMLElement loginElement = new XMLElement(LOGIN_ELEMENT);
        final char[] password = loginInfo.getPassword().get();

        loginElement
            .setAttribute(CLIENT_ATTRIBUTE, XMLElement.escape(_client, '\''));
        loginElement
            .setAttribute(
                USER_ATTRIBUTE,
                XMLElement.escape(loginInfo.getUser().get(), '\''));
        loginElement
            .setAttribute(
                PASSWORD_ATTRIBUTE,
                XMLElement.escape(new String(password), '\''));

        _sendXML(loginElement);
    }

    /**
     * Logs in.
     *
     * @param user The optional user.
     * @param password The optional password.
     *
     * @throws ServiceNotAvailableException When the service is not available.
     */
    public void login(
            @Nonnull final Optional<String> user,
            @Nonnull final Optional<char[]> password)
        throws ServiceNotAvailableException
    {
        login(new LoginInfo(user, password));
    }

    /**
     * Opens a connection to the XML Port.
     *
     * @param addressString The address for the port (host:port).
     * @param securityContext An optional security context.
     *
     * @throws ServiceNotAvailableException When the service is not available.
     */
    public void open(
            @Nonnull final String addressString,
            @Nonnull final Optional<SecurityContext> securityContext)
        throws ServiceNotAvailableException
    {
        final Optional<InetSocketAddress> socketAddress = Inet
            .socketAddress(addressString);

        if (!socketAddress.isPresent()) {
            throw new IllegalArgumentException(
                Message.format(BaseMessages.BAD_ADDRESS, addressString));
        }

        final boolean secure = (securityContext.isPresent())
                && (securityContext.get().isCertified()
                    || securityContext.get().isSecure() || !Inet.isOnLocalHost(
                            socketAddress.get().getAddress()));

        try {
            if (secure) {    // Uses a SSL connection.
                securityContext.get().checkForSecureOperation();

                final SSLContext sslContext = securityContext
                    .get()
                    .createSSLContext();

                _socket = sslContext.getSocketFactory().createSocket();
                _socket.connect(socketAddress.get());
                ((SSLSocket) _socket).startHandshake();
            } else {    // Uses a normal connection.
                _socket = SocketFactory.getDefault().createSocket();
                _socket.connect(socketAddress.get());
            }

            _output = new BufferedOutputStream(_socket.getOutputStream());
            _input = new BufferedInputStream(_socket.getInputStream());

            final XMLDocument.ElementReader reader =
                new XMLDocument.ElementReader(
                    _input);

            _document.setRootHandler(Optional.of(reader));
            _reader = reader;
        } catch (final SocketException exception) {
            throw new ServiceNotAvailableException(exception);
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Sends messages.
     *
     * @param flush True asks for a flush.
     *
     * @throws ServiceNotAvailableException When the service is not available.
     */
    public void sendMessages(
            final boolean flush)
        throws ServiceNotAvailableException
    {
        if (!_messages.isEmpty()) {
            final XMLElement messagesElement = new XMLElement(MESSAGES_ELEMENT);
            final Streamer.Output output = getStreamer()
                .newOutput(messagesElement);

            for (final Serializable message: _messages) {
                if (!output.add(message)) {
                    Logger
                        .getInstance(getClass())
                        .warn(BaseMessages.BAD_MESSAGE, message);
                }
            }

            output.close();
            _messages.clear();

            if (flush) {
                messagesElement
                    .setAttribute(FLUSH_ATTRIBUTE, String.valueOf(flush));
            }

            _sendXML(messagesElement);
        }
    }

    private void _sendXML(
            final XMLElement element)
        throws ServiceNotAvailableException
    {
        element.setAttribute(ID_ATTRIBUTE, String.valueOf(++_id));

        final byte[] bytes = element
            .toString()
            .getBytes(StandardCharsets.UTF_8);

        try {
            _output.write(bytes);
            _output.flush();

            final XMLElement response = _document.parse(_reader);
            final String ref = response
                .getAttributeValue(REF_ATTRIBUTE, Optional.of("0"))
                .orElse(null);

            if (Long.parseLong(ref) != _id) {
                throw new IOException(
                    Message.format(
                        BaseMessages.MISMATCHED_REF,
                        String.valueOf(_id),
                        ref));
            }

            if (DONE_ELEMENT.equals(response.getName())) {
                _reader.reset();
            } else if (FAILED_ELEMENT.equals(response.getName())) {
                throw new IOException(
                    Message.format(
                        BaseMessages.FAILED,
                        response.getAttributeValue(
                            EXCEPTION_ATTRIBUTE,
                            Optional.empty()).orElse(null),
                        response.getText()));
            } else {
                throw new IOException(
                    Message.format(BaseMessages.UNEXPECTED_RESPONSE, response));
            }
        } catch (final SocketException exception) {
            throw new ServiceNotAvailableException(exception);
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        } catch (final ParseException exception) {
            throw new ServiceNotAvailableException(exception);
        }
    }

    /** Client attribute. */
    public static final String CLIENT_ATTRIBUTE = "client";

    /** Done element name. */
    public static final String DONE_ELEMENT = "done";

    /** Exception attribute name. */
    public static final String EXCEPTION_ATTRIBUTE = "exception";

    /** Failed element name. */
    public static final String FAILED_ELEMENT = "failed";

    /** Flush attribute name. */
    public static final String FLUSH_ATTRIBUTE = "flush";

    /** Flush element name. */
    public static final String FLUSH_ELEMENT = "flush";

    /** ID attribute. */
    public static final String ID_ATTRIBUTE = "id";

    /** Login element name. */
    public static final String LOGIN_ELEMENT = "login";

    /** Messages element name. */
    public static final String MESSAGES_ELEMENT = "messages";

    /** Password attribute. */
    public static final String PASSWORD_ATTRIBUTE = "password";

    /** REF attribute. */
    public static final String REF_ATTRIBUTE = "ref";

    /** User attribute. */
    public static final String USER_ATTRIBUTE = "user";

    private final String _client;
    private final XMLDocument _document = new XMLDocument();
    private long _id;
    private InputStream _input;
    private final List<Serializable> _messages = new LinkedList<>();
    private OutputStream _output;
    private int _pending;
    private Reader _reader;
    private Socket _socket;
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
