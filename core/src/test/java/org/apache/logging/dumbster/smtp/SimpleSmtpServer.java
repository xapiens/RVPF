/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.dumbster.smtp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Dummy SMTP server for testing purposes.
 *
 * @todo constructor allowing user to pass preinitialized ServerSocket
 */
public class SimpleSmtpServer implements Runnable {
    /**
     * Stores all of the email received.
     */
    private final List<SmtpMessage> _receivedEmail;

    /**
     * Default SMTP port is 25.
     */
    public static final int DEFAULT_SMTP_PORT = 25;

    /**
     * Indicates whether this server is stopped or not.
     */
    private volatile boolean stopped = true;

    /**
     * Handle to the server socket this server listens to.
     */
    private ServerSocket serverSocket;

    /**
     * Port the server listens on - set to the default SMTP port initially.
     */
    private int port = DEFAULT_SMTP_PORT;

    /**
     * Timeout listening on server socket.
     */
    private static final int TIMEOUT = 500;

    /**
     * Constructor.
     *
     * @param portNumber port number
     */
    public SimpleSmtpServer(final int portNumber) {
        _receivedEmail = new ArrayList<SmtpMessage>();
        this.port = portNumber;
    }

    /**
     * Main loop of the SMTP server.
     */
    @Override
    public void run() {
        stopped = false;
        try {
            try {
                serverSocket = new ServerSocket(port);
                serverSocket.setSoTimeout(TIMEOUT); // Block for maximum of 1.5 seconds
            } finally {
                synchronized (this) {
                    // Notify when server socket has been created
                    notifyAll();
                }
            }

            // Server: loop until stopped
            while (!isStopped()) {
                // Start server socket and listen for client connections
                Socket socket = null;
                try {
                    socket = serverSocket.accept();
                } catch (final Exception e) {
                    continue; // Non-blocking socket timeout occurred: try accept() again
                }

                // Get the input and output streams
                final BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                final PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

                synchronized (this) {
          /*
           * We synchronize over the handle method and the list update because the client call completes inside
           * the handle method and we have to prevent the client from reading the list until we've updated it.
           * For higher concurrency, we could just change handle to return void and update the list inside the method
           * to limit the duration that we hold the lock.
           */
                    final List<SmtpMessage> msgs = handleTransaction(out, input);
                    _receivedEmail.addAll(msgs);
                    notifyAll();
                }
                socket.close();
            }
        } catch (final Exception e) {
            /** @todo Should throw an appropriate exception here. */
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Check if the server has been placed in a stopped state. Allows another thread to
     * stop the server safely.
     *
     * @return true if the server has been sent a stop signal, false otherwise
     */
    public synchronized boolean isStopped() {
        return stopped;
    }

    /**
     * Stops the server. Server is shutdown after processing of the current request is complete.
     */
    public synchronized void stop() {
        // Mark us closed
        stopped = true;
        try {
            // Kick the server accept loop
            serverSocket.close();
        } catch (final IOException e) {
            // Ignore
        }
    }

    /**
     * Handle an SMTP transaction, i.e. all activity between initial connect and QUIT command.
     *
     * @param out   output stream
     * @param input input stream
     * @return List of SmtpMessage
     * @throws IOException
     */
    private static List<SmtpMessage> handleTransaction(final PrintWriter out, final BufferedReader input) throws IOException {
        // Initialize the state machine
        SmtpState smtpState = SmtpState.CONNECT;
        final SmtpRequest smtpRequest = new SmtpRequest(SmtpActionType.CONNECT, "", smtpState);

        // Execute the connection request
        final SmtpResponse smtpResponse = smtpRequest.execute();

        // Send initial response
        sendResponse(out, smtpResponse);
        smtpState = smtpResponse.getNextState();

        final List<SmtpMessage> msgList = new ArrayList<SmtpMessage>();
        SmtpMessage msg = new SmtpMessage();

        while (smtpState != SmtpState.CONNECT) {
            final String line = input.readLine();

            if (line == null) {
                break;
            }

            // Create request from client input and current state
            final SmtpRequest request = SmtpRequest.createRequest(line, smtpState);
            // Execute request and create response object
            final SmtpResponse response = request.execute();
            // Move to next internal state
            smtpState = response.getNextState();
            // Send response to client
            sendResponse(out, response);

            // Store input in message
            final String params = request.getParams();
            msg.store(response, params);

            // If message reception is complete save it
            if (smtpState == SmtpState.QUIT) {
                msgList.add(msg);
                msg = new SmtpMessage();
            }
        }

        return msgList;
    }

    /**
     * Send response to client.
     *
     * @param out          socket output stream
     * @param smtpResponse response object
     */
    private static void sendResponse(final PrintWriter out, final SmtpResponse smtpResponse) {
        if (smtpResponse.getCode() > 0) {
            final int code = smtpResponse.getCode();
            final String message = smtpResponse.getMessage();
            out.print(code + " " + message + "\r\n");
            out.flush();
        }
    }

    /**
     * Get email received.
     *
     * @return List of String
     */
    public synchronized List<SmtpMessage> getReceivedEmail() {
        return _receivedEmail;
    }

    /**
     * Creates an instance of SimpleSmtpServer and starts it. Will listen on the default port.
     *
     * @return a reference to the SMTP server
     */
    public static SimpleSmtpServer start() {
        return start(DEFAULT_SMTP_PORT);
    }

    /**
     * Creates an instance of SimpleSmtpServer and starts it.
     *
     * @param port port number the server should listen to
     * @return a reference to the SMTP server
     */
    public static SimpleSmtpServer start(final int port) {
        final SimpleSmtpServer server = new SimpleSmtpServer(port);
        final Thread thread = new Thread(server);

        // Block until the server socket is created
        synchronized (server) {
            thread.start();
            if (thread.isAlive()) {
                while (server.serverSocket == null) {
                    try {
                        server.wait();
                    } catch (final InterruptedException exception) {
                        break;
                    }
                }
            }
        }

        return server;
    }

}
