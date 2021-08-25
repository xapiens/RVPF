/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ValveTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.valve;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.ValueConverter;
import org.rvpf.service.ServiceMessages;
import org.rvpf.tests.TestsMessages;
import org.rvpf.tests.service.ServiceTests;
import org.rvpf.valve.Connection;
import org.rvpf.valve.Controller;
import org.rvpf.valve.ValveServiceActivator;
import org.rvpf.valve.ValveServiceImpl;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Valve Tests.
 */
public final class ValveTests
    extends ServiceTests
{
    /**
     * Sets up the class.
     *
     * @throws Exception On failure.
     */
    @BeforeClass
    public void classSetUp()
        throws Exception
    {
        final String controlAddress = "127.0.0.1:" + allocateTCPPort();

        setProperty(_VALVE_CONTROL_PORT_PROPERTY, controlAddress);

        loadConfig();

        _timeout = super.getTimeout();

        _listenSocket = new ServerSocket();
        _listenSocket.setSoTimeout(getTimeout());
        _listenSocket.bind(new InetSocketAddress("127.0.0.1", 0));

        _valve = new ValveServiceActivator();
        _valve.create();
        _valve
            .setProperty(
                ValveServiceImpl.SERVER_ADDRESS_PROPERTY,
                "127.0.0.1:" + _listenSocket.getLocalPort());
        _valve
            .setProperty(
                ValveServiceImpl.CONTROL_ADDRESS_PROPERTY,
                controlAddress);

        _valve.start();
        Require.success(_valve.isStarted());
        Require.success(_valve.isPaused());

        // Gets the other addresses.

        Collection<SocketAddress> addresses;

        addresses = _valve.getControlledAddresses();
        Require.success(addresses.size() == 1);
        _controlledAddress = addresses.iterator().next();

        addresses = _valve.getDirectAddresses();
        Require.success(addresses.size() == 1);
        _directAddress = addresses.iterator().next();

        // Prepares the SSL context.

        final Properties env = _valve.getEnv();
        final Optional<SecurityContext> securityContext = ValveServiceImpl
            .createSecurityContext(
                env,
                false,
                CLIENT_CERTIFIED_PROPERTY,
                CLIENT_KEYSTORE_PROPERTY,
                CLIENT_KEYSTORE_TYPE_PROPERTY,
                CLIENT_KEYSTORE_PROVIDER_PROPERTY,
                CLIENT_KEYSTORE_PASSWORD_PROPERTY,
                CLIENT_KEY_PASSWORD_PROPERTY,
                CLIENT_TRUSTSTORE_PROPERTY,
                CLIENT_TRUSTSTORE_TYPE_PROPERTY,
                CLIENT_TRUSTSTORE_PROVIDER_PROPERTY,
                getThisLogger());

        _sslContext = securityContext
            .isPresent()? securityContext.get().createSSLContext(): null;
    }

    /**
     * Tears down what has been set up for the class.
     *
     * @throws Exception On failure.
     */
    @AfterClass(alwaysRun = true)
    public void classTearDown()
        throws Exception
    {
        _valve.stop();
        Require.success(_valve.isStopped());

        _valve.destroy();
        _valve = null;

        _listenSocket.close();
        _listenSocket = null;
    }

    /**
     * Simple controlled connection test.
     *
     * @throws Exception On failure.
     */
    @Test
    public void simpleControlledConnectionTest()
        throws Exception
    {
        _run(
            new _WrappedTest()
            {
                /** {@inheritDoc}
                 */
                @Override
                void test()
                    throws Exception
                {
                    // A connection to the controlled port should be
                    // refused.

                    connectToServer(getControlledAddress(), Optional.empty());
                    Require
                        .success(
                            getServerSocket().getInputStream().read() == -1);
                    closeSockets();

                    // Opens the valve.

                    final Controller controller = new Controller(getConfig());

                    Require.success(controller.open(_VALVE_PROPERTIES));
                    getValve().waitForControl(true);

                    // A connection to the controlled port should be
                    // accepted.

                    Future<?> sender;

                    connectToServer(getControlledAddress(), Optional.empty());
                    acceptClient();
                    sender = send(
                        getServerOutputStream(),
                        "REQUEST\r\n",
                        getServerSocket());
                    receive(
                        getClientInputStream(),
                        "REQUEST\r\n",
                        getClientSocket());
                    waitForRunnable(sender);
                    sender = send(
                        getClientOutputStream(),
                        "RESPONSE\r\n",
                        getClientSocket());
                    receive(
                        getServerInputStream(),
                        "RESPONSE\r\n",
                        getServerSocket());
                    waitForRunnable(sender);
                    sender = send(
                        getServerOutputStream(),
                        "LIST\r\n",
                        getServerSocket());
                    receive(
                        getClientInputStream(),
                        "LSUB\r\n",
                        getClientSocket());
                    waitForRunnable(sender);
                    sender = send(
                        getClientOutputStream(),
                        "LSUB\r\n",
                        getClientSocket());
                    receive(
                        getServerInputStream(),
                        "LIST\r\n",
                        getServerSocket());
                    waitForRunnable(sender);
                    closeSockets();

                    // Closes the valve.

                    controller.close();
                    getValve().waitForControl(false);

                    // A connection to the controlled port should be
                    // refused.

                    connectToServer(getControlledAddress(), Optional.empty());
                    Require
                        .success(
                            getServerSocket().getInputStream().read() == -1);
                    closeSockets();
                }
            },
            TESTS_CONTROLLED_INSTANCES_PROPERTY,
            TESTS_CONTROLLED_INSTANCES_DEFAULT,
            TESTS_CONTROLLED_REPEAT_PROPERTY,
            TESTS_CONTROLLED_REPEAT_DEFAULT);
    }

    /**
     * Simple direct connection test.
     *
     * @throws Exception On failure.
     */
    @Test
    public void simpleDirectConnectionTest()
        throws Exception
    {
        _run(
            new _WrappedTest()
            {
                /** {@inheritDoc}
                 */
                @Override
                void test()
                    throws Exception
                {
                    Future<?> sender;

                    connectToServer(getDirectAddress(), getSSLContext());
                    acceptClient();
                    sender = send(
                        getServerOutputStream(),
                        "LIST\r\n",
                        getServerSocket());
                    receive(
                        getClientInputStream(),
                        "LIST\r\n",
                        getClientSocket());
                    waitForRunnable(sender);
                    sender = send(
                        getClientOutputStream(),
                        "LSUB\r\n",
                        getClientSocket());
                    receive(
                        getServerInputStream(),
                        "LSUB\r\n",
                        getServerSocket());
                    waitForRunnable(sender);
                    closeSockets();
                }
            },
            TESTS_DIRECT_INSTANCES_PROPERTY,
            TESTS_DIRECT_INSTANCES_DEFAULT,
            TESTS_DIRECT_REPEAT_PROPERTY,
            TESTS_DIRECT_REPEAT_DEFAULT);
    }

    /**
     * Volume direct connection test.
     *
     * @throws Exception On failure.
     */
    @Test
    public void volumeDirectConnectionTest()
        throws Exception
    {
        _run(
            new _WrappedTest()
            {
                /** {@inheritDoc}
                 */
                @Override
                void test()
                    throws Exception
                {
                    connectToServer(getDirectAddress(), getSSLContext());
                    acceptClient();

                    generateVolume(100, 1, 2);
                    generateVolume(100, 1, 10);
                    generateVolume(100, 1, 100);
                    generateVolume(100, 1, 1000);
                    generateVolume(10, 1001, 100000);
                    generateVolume(1, 100001, 1000000);

                    closeSockets();
                }

                private void generateVolume(
                        final int count,
                        final int minimum,
                        final int maximum)
                    throws Exception
                {
                    Future<?> sender;

                    for (int i = 1; i <= count; ++i) {
                        byte[] bytes;

                        bytes = generateBytes(minimum, maximum);
                        sender = send(
                            getServerOutputStream(),
                            bytes,
                            getServerSocket());
                        receive(
                            getClientInputStream(),
                            bytes,
                            getClientSocket());
                        waitForRunnable(sender);

                        bytes = generateBytes(minimum, maximum);
                        sender = send(
                            getClientOutputStream(),
                            bytes,
                            getClientSocket());
                        receive(
                            getServerInputStream(),
                            bytes,
                            getServerSocket());
                        waitForRunnable(sender);
                    }
                }

                private byte[] generateBytes(
                        final int minimum,
                        final int maximum)
                {
                    final int length = minimum + _random.nextInt(
                        maximum + 1 - minimum);
                    final byte[] bytes = new byte[length];

                    _random.nextBytes(bytes);

                    return bytes;
                }

                private final Random _random = new Random();
            },
            TESTS_VOLUME_INSTANCES_PROPERTY,
            TESTS_VOLUME_INSTANCES_DEFAULT,
            TESTS_VOLUME_REPEAT_PROPERTY,
            TESTS_VOLUME_REPEAT_DEFAULT);
    }

    /** {@inheritDoc}
     */
    @Override
    protected int getTimeout()
    {
        return _timeout;
    }

    /**
     * Accepts the client socket for a server connection.
     *
     * @param serverOutputStream The server output stream.
     *
     * @return The client socket.
     *
     * @throws Exception On failure.
     */
    @Nonnull
    @CheckReturnValue
    Socket acceptClient(final OutputStream serverOutputStream)
        throws Exception
    {
        final Integer serverID = Integer
            .valueOf(_nextConnectionID.getAndIncrement());
        final DataOutputStream outputStream = new DataOutputStream(
            serverOutputStream);

        outputStream.writeInt(serverID.intValue());
        outputStream.flush();

        Socket clientSocket;

        for (;;) {
            clientSocket = _acceptedSockets.remove(serverID);

            if (clientSocket != null) {
                return clientSocket;
            }

            synchronized (_listenSocket) {
                clientSocket = _acceptedSockets.remove(serverID);

                if (clientSocket != null) {
                    return clientSocket;
                }

                clientSocket = _listenSocket.accept();
                clientSocket.setSoTimeout(getTimeout());

                final DataInputStream inputStream = new DataInputStream(
                    new BufferedInputStream(clientSocket.getInputStream()));
                final Integer clientID = Integer.valueOf(inputStream.readInt());

                _acceptedSockets.put(clientID, clientSocket);
            }
        }
    }

    /**
     * Gets the controlled address.
     *
     * @return The controlled address.
     */
    @Nonnull
    @CheckReturnValue
    SocketAddress getControlledAddress()
    {
        return _controlledAddress;
    }

    /**
     * Gets the direct address.
     *
     * @return The direct address.
     */
    @Nonnull
    @CheckReturnValue
    SocketAddress getDirectAddress()
    {
        return _directAddress;
    }

    /**
     * Gets the SSL context.
     *
     * @return The optional SSL context.
     */
    @Nonnull
    @CheckReturnValue
    Optional<SSLContext> getSSLContext()
    {
        return Optional.ofNullable(_sslContext);
    }

    /**
     * Gets the valve.
     *
     * @return The valve.
     */
    @Nonnull
    @CheckReturnValue
    ValveServiceActivator getValve()
    {
        final ValveServiceActivator valve = _valve;

        Require.notNull(valve);

        return valve;
    }

    private void _run(
            final _WrappedTest test,
            final String instancesProperty,
            final int defaultInstances,
            final String repeatProperty,
            final int defaultRepeat)
        throws Exception
    {
        final Properties env = _valve.getEnv();
        final int instances = ValueConverter
            .convertToInteger(
                ServiceMessages.PROPERTY_TYPE.toString(),
                instancesProperty,
                Optional.ofNullable(env.getProperty(instancesProperty)),
                Optional.of(Integer.valueOf(defaultInstances)))
            .get()
            .intValue();
        final int repeat = ValueConverter
            .convertToInteger(
                ServiceMessages.PROPERTY_TYPE.toString(),
                repeatProperty,
                Optional.ofNullable(env.getProperty(repeatProperty)),
                Optional.of(Integer.valueOf(defaultRepeat)))
            .get()
            .intValue();

        test.setRepeat(repeat);

        if (instances <= 1) {
            test.call();
        } else {
            final ExecutorService executor = Executors
                .newFixedThreadPool(instances);
            final Future<?>[] futures = new Future<?>[instances];
            boolean failed = false;

            try {
                for (int i = 0; i < instances; ++i) {
                    futures[i] = executor.submit(test.clone());
                }

                for (int i = 0; i < instances; ++i) {
                    try {
                        futures[i].get();
                    } catch (final ExecutionException exception) {
                        Throwable cause = exception.getCause();

                        if ((cause != null) && (cause.getCause() != null)) {
                            cause = cause.getCause();
                        }

                        getThisLogger()
                            .warn(
                                (cause != null)? cause: exception,
                                TestsMessages.TEST_FAILED);
                        failed = true;
                    }
                }

                Require.failure(failed);
            } finally {
                executor.shutdown();
            }
        }
    }

    /** Client certified property. */
    public static final String CLIENT_CERTIFIED_PROPERTY = "client.certified";

    /** Client keystore password property. */
    public static final String CLIENT_KEYSTORE_PASSWORD_PROPERTY =
        "client.keystore.password";

    /** Client keystore property. */
    public static final String CLIENT_KEYSTORE_PROPERTY = "client.keystore";

    /** Client keystore provider property. */
    public static final String CLIENT_KEYSTORE_PROVIDER_PROPERTY =
        "client.keystore.provider";

    /** Client keystore type property. */
    public static final String CLIENT_KEYSTORE_TYPE_PROPERTY =
        "client.keystore.type";

    /** Client key password property. */
    public static final String CLIENT_KEY_PASSWORD_PROPERTY =
        "client.key.password";

    /** Client truststore property. */
    public static final String CLIENT_TRUSTSTORE_PROPERTY = "client.truststore";

    /** Client truststore provider property. */
    public static final String CLIENT_TRUSTSTORE_PROVIDER_PROPERTY =
        "client.truststore.provider";

    /** Client truststore type. */
    public static final String CLIENT_TRUSTSTORE_TYPE_PROPERTY =
        "client.truststore.type";

    /** Tests controlled instances default. */
    public static final int TESTS_CONTROLLED_INSTANCES_DEFAULT = 1;

    /** Tests controlled instances property. */
    public static final String TESTS_CONTROLLED_INSTANCES_PROPERTY =
        "tests.controlled.instances";

    /** Tests controlled repeat default. */
    public static final int TESTS_CONTROLLED_REPEAT_DEFAULT = 100;

    /** Tests controlled repeat property. */
    public static final String TESTS_CONTROLLED_REPEAT_PROPERTY =
        "tests.controlled.repeat";

    /** Tests direct instances default. */
    public static final int TESTS_DIRECT_INSTANCES_DEFAULT = 50;

    /** Tests direct instances property. */
    public static final String TESTS_DIRECT_INSTANCES_PROPERTY =
        "tests.direct.instances";

    /** Tests direct repeat default. */
    public static final int TESTS_DIRECT_REPEAT_DEFAULT = 10;

    /** Tests direct repeat property. */
    public static final String TESTS_DIRECT_REPEAT_PROPERTY =
        "tests.direct.repeat";

    /** Tests volume instances default. */
    public static final int TESTS_VOLUME_INSTANCES_DEFAULT = 10;

    /** Tests volume instances property. */
    public static final String TESTS_VOLUME_INSTANCES_PROPERTY =
        "tests.volume.instances";

    /** Tests volume repeat default. */
    public static final int TESTS_VOLUME_REPEAT_DEFAULT = 1;

    /** Tests volume repeat property. */
    public static final String TESTS_VOLUME_REPEAT_PROPERTY =
        "tests.volume.repeat";
    private static final int _BYTES_BUFFER_SIZE = 1024;
    private static final String _VALVE_CONTROL_PORT_PROPERTY =
        "tests.valve.control.port";
    private static final String _VALVE_PROPERTIES = "tests.valve";

    private final Map<Integer, Socket> _acceptedSockets =
        new ConcurrentHashMap<Integer, Socket>();
    private SocketAddress _controlledAddress;
    private SocketAddress _directAddress;
    private ServerSocket _listenSocket;
    private final AtomicInteger _nextConnectionID = new AtomicInteger(1);
    private SSLContext _sslContext;
    private int _timeout;
    private ValveServiceActivator _valve;

    /**
     * Filter.
     *
     * <p>This class is referenced in "tests/config/rvpf-valve.properties".</p>
     */
    public static final class Filter
        implements Connection.Filter
    {
        /** {@inheritDoc}
         */
        @Override
        public void onClientData(final Connection.Direction direction)
        {
            if (direction.isControlled()) {
                final ByteBuffer buffer = direction.getOriginalData();
                final byte[] bytes =
                    new byte[buffer.limit() - buffer.position()];
                final String string;

                buffer.get(bytes);
                string = new String(bytes, StandardCharsets.UTF_8);

                if (string.contains("LIST")) {
                    direction
                        .getModifiedData()
                        .put(
                            string
                                .replace("LIST", "LSUB")
                                .getBytes(StandardCharsets.UTF_8));
                }
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public void onServerData(final Connection.Direction direction)
        {
            if (direction.isControlled()) {
                final ByteBuffer originalData = direction.getOriginalData();
                final byte[] originalBytes =
                    new byte[originalData.limit() - originalData.position()];
                final String string;

                originalData.get(originalBytes);
                string = new String(originalBytes, StandardCharsets.UTF_8);

                if (string.contains("LSUB")) {
                    direction
                        .getModifiedData()
                        .put(
                            string
                                .replace("LSUB", "LIST")
                                .getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }


    private final class _TextSender
        implements Runnable
    {
        _TextSender(
                final OutputStream writer,
                final byte[] bytes,
                final Socket socket)
        {
            _output = writer;
            _bytes = bytes;
            _socket = socket;
        }

        /** {@inheritDoc}
         */
        @Override
        public void run()
        {
            try {
                getThisLogger()
                    .trace(
                        ValveTestsMessages.SENDING_BYTES,
                        String.valueOf(_bytes.length),
                        _socket);
                _output.write(_bytes);
                _output.flush();
            } catch (final IOException exception) {
                Require.failure(exception);
            }
        }

        private final byte[] _bytes;
        private final OutputStream _output;
        private final Socket _socket;
    }


    private abstract class _WrappedTest
        implements Callable<Void>, Cloneable
    {
        /**
         * Constructs an instance.
         */
        _WrappedTest() {}

        /** {@inheritDoc}
         */
        @Override
        public Void call()
            throws Exception
        {
            int i = 0;

            _senderExecutor = Executors.newSingleThreadExecutor();

            try {
                for (i = 1; i <= _repeat; ++i) {
                    test();
                }
            } catch (final Exception exception) {
                getThisLogger()
                    .warn(
                        exception,
                        ValveTestsMessages.EXCEPTION_ON_SOCKET,
                        _serverSocket,
                        _clientSocket);

                throw exception;
            } finally {
                closeSockets();
                _senderExecutor.shutdown();
                Require
                    .success(
                        _senderExecutor
                            .awaitTermination(
                                    getTimeout(),
                                            TimeUnit.MILLISECONDS));
            }

            return null;
        }

        /**
         * Accepts a client.
         *
         * @throws Exception On failure.
         */
        protected void acceptClient()
            throws Exception
        {
            Require.notNull(_serverSocket);
            Require.success(_clientSocket == null);
            _clientSocket = ValveTests.this
                .acceptClient(getServerOutputStream());
        }

        /** {@inheritDoc}
         */
        @Override
        protected _WrappedTest clone()
        {
            try {
                return (_WrappedTest) super.clone();
            } catch (final CloneNotSupportedException exception) {
                throw new InternalError(exception);
            }
        }

        /**
         * Closes sockets.
         *
         * @throws Exception On failure.
         */
        protected void closeSockets()
            throws Exception
        {
            if (_serverSocket != null) {
                _serverInputStream = null;
                _serverOutputStream = null;
                _serverSocket.close();
                _serverSocket = null;
            }

            if (_clientSocket != null) {
                _clientInputStream = null;
                _clientOutputStream = null;
                _clientSocket.close();
                _clientSocket = null;
            }
        }

        /**
         * Connects to server.
         *
         * @param address The server address.
         * @param sslContext The optional SSL context.
         *
         * @throws Exception On failure.
         */
        protected void connectToServer(
                final SocketAddress address,
                final Optional<SSLContext> sslContext)
            throws Exception
        {
            Require.success(_serverSocket == null);
            _serverSocket = sslContext
                .isPresent()? sslContext
                    .get()
                    .getSocketFactory()
                    .createSocket(): new Socket();
            _serverSocket.connect(address, getTimeout());
            _serverSocket.setSoTimeout(getTimeout());

            if (sslContext.isPresent()) {
                ((SSLSocket) _serverSocket).startHandshake();
            }
        }

        /**
         * Gets the client input stream.
         *
         * @return The client input stream.
         *
         * @throws Exception On failure.
         */
        protected InputStream getClientInputStream()
            throws Exception
        {
            if (_clientInputStream == null) {
                _clientInputStream = new BufferedInputStream(
                    _clientSocket.getInputStream());
            }

            return _clientInputStream;
        }

        /**
         * Gets the client output stream.
         *
         * @return The client output stream.
         *
         * @throws Exception On failure.
         */
        protected OutputStream getClientOutputStream()
            throws Exception
        {
            if (_clientOutputStream == null) {
                _clientOutputStream = new BufferedOutputStream(
                    _clientSocket.getOutputStream());
            }

            return _clientOutputStream;
        }

        /**
         * Gets the socket from the client.
         *
         * @return The socket from the client.
         */
        protected Socket getClientSocket()
        {
            return _clientSocket;
        }

        /**
         * Gets the server input stream.
         *
         * @return The server input stream.
         *
         * @throws Exception On failure.
         */
        protected InputStream getServerInputStream()
            throws Exception
        {
            if (_serverInputStream == null) {
                _serverInputStream = new BufferedInputStream(
                    _serverSocket.getInputStream());
            }

            return _serverInputStream;
        }

        /**
         * Gets the server output stream.
         *
         * @return The server output stream.
         *
         * @throws Exception On failure.
         */
        protected OutputStream getServerOutputStream()
            throws Exception
        {
            if (_serverOutputStream == null) {
                _serverOutputStream = new BufferedOutputStream(
                    _serverSocket.getOutputStream());
            }

            return _serverOutputStream;
        }

        /**
         * Gets the socket to the server.
         *
         * @return The socket to the server.
         */
        protected Socket getServerSocket()
        {
            return _serverSocket;
        }

        /**
         * Receives bytes from an input stream.
         *
         * @param inputStream The input stream.
         * @param expected The expected bytes.
         * @param socket The socket of the input stream.
         *
         * @throws Exception On failure.
         */
        protected void receive(
                final InputStream inputStream,
                final byte[] expected,
                final Socket socket)
            throws Exception
        {
            final byte[] bytes = new byte[expected.length];
            int read = 0;

            for (int offset = 0; offset < bytes.length; offset += read) {
                read = inputStream.read(bytes, offset, bytes.length - offset);

                Require.success(read >= 0);
            }

            getThisLogger()
                .trace(
                    ValveTestsMessages.RECEIVED_BYTES,
                    String.valueOf(bytes.length),
                    socket);

            Require.success(Arrays.equals(bytes, expected));
        }

        /**
         * Receives a string from an input stream.
         *
         * @param inputStream The input stream.
         * @param expected The expected string.
         * @param socket The socket of the input stream.
         *
         * @throws Exception On failure.
         */
        protected void receive(
                final InputStream inputStream,
                final String expected,
                final Socket socket)
            throws Exception
        {
            final StringBuilder builder = new StringBuilder();
            final byte[] bytes = new byte[_BYTES_BUFFER_SIZE];

            while (builder.length() < expected.length()) {
                final int read = inputStream.read(bytes);

                Require.success(read >= 0);

                builder
                    .append(new String(bytes, 0, read, StandardCharsets.UTF_8));
            }

            getThisLogger()
                .trace(
                    ValveTestsMessages.RECEIVED_BYTES,
                    String.valueOf(builder.length()),
                    socket);

            Require.equal(builder.toString(), expected);
        }

        /**
         * Sends bytes on an output stream.
         *
         * @param outputStream The output stream.
         * @param bytes The bytes.
         * @param socket The socket of the output stream.
         *
         * @return A future representatikon of the sender.
         */
        protected Future<?> send(
                final OutputStream outputStream,
                final byte[] bytes,
                final Socket socket)
        {
            return _senderExecutor
                .submit(new _TextSender(outputStream, bytes, socket));
        }

        /**
         * Sends text on an output stream.
         *
         * @param outputStream The output stream.
         * @param text The text.
         * @param socket The socket of the output stream.
         *
         * @return A future representatikon of the sender.
         */
        protected Future<?> send(
                final OutputStream outputStream,
                final String text,
                final Socket socket)
        {
            return _senderExecutor
                .submit(
                    new _TextSender(
                        outputStream,
                        text.getBytes(StandardCharsets.UTF_8),
                        socket));
        }

        /**
         * Sets the repeat count.
         *
         * @param repeat The repeat count.
         */
        void setRepeat(final int repeat)
        {
            _repeat = repeat;
        }

        /**
         * Tests.
         *
         * @throws Exception On failure.
         */
        abstract void test()
            throws Exception;

        private InputStream _clientInputStream;
        private OutputStream _clientOutputStream;
        private Socket _clientSocket;
        private int _repeat;
        private ExecutorService _senderExecutor;
        private InputStream _serverInputStream;
        private OutputStream _serverOutputStream;
        private Socket _serverSocket;
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
