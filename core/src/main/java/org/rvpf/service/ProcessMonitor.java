/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ProcessMonitor.java 4112 2019-08-02 20:00:26Z SFB $
 */

package org.rvpf.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.nio.charset.Charset;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.ValueConverter;

/**
 * Process monitor.
 *
 * <p>Monitors and controls the execution of a subprocess.</p>
 */
public final class ProcessMonitor
    implements ServiceThread.Target
{
    /**
     * Constructs an instance.
     *
     * @param ownerName The name of the owner for Log identification.
     * @param logger A logger.
     * @param killDelay The kill delay in millis.
     * @param service The service (may be null).
     * @param charset The charset for communication with the process (may be
     *                null).
     * @param processBuilder The process builder.
     * @param keepProcess The optional elapsed time to keep an idle process.
     */
    ProcessMonitor(
            @Nonnull final String ownerName,
            @Nonnull final Logger logger,
            @Nonnull final ElapsedTime killDelay,
            @Nonnull final Optional<Service> service,
            @Nonnull final Charset charset,
            @Nonnull final ProcessBuilder processBuilder,
            @Nonnull final Optional<ElapsedTime> keepProcess)
    {
        _ownerName = ownerName;
        _logger = logger;
        _killDelay = killDelay;
        _service = service;
        _processBuilder = processBuilder;
        _charset = charset;
        _keepProcess = keepProcess;
    }

    /**
     * Returns a new builder.
     *
     * @return The new builder.
     */
    @Nonnull
    @CheckReturnValue
    public static Builder newBuilder()
    {
        return new Builder();
    }

    /**
     * Returns a new request ID.
     *
     * <p>This utility method generates a new value each time it is called. The
     * value will be unique for the class and will always be increasing.</p>
     *
     * <p>It is safe for use by concurrent threads.</p>
     *
     * @return A new request ID.
     */
    @CheckReturnValue
    public static synchronized long newRequestID()
    {
        final long millis = System.currentTimeMillis();

        if (millis > _lastRequestID) {
            _lastRequestID = millis;
        } else {
            ++_lastRequestID;
        }

        return _lastRequestID;
    }

    /**
     * Activates the process object.
     *
     * <p>Unless it is already running, the process will be created and a new
     * thread will be started to monitor its execution. Any sheduled interrupt
     * will be cancelled.</p>
     *
     * @return The Process object (null on failure).
     */
    @CheckReturnValue
    public boolean activateProcess()
    {
        synchronized (_mutex) {
            if (_ready) {
                _busy = true;
                _cancelExpirator();
            } else {
                while (_thread.get() != null) {    // Waits while process
                    // terminates.
                    try {
                        _mutex.wait();
                    } catch (final InterruptedException exception) {
                        _logger.debug(ServiceMessages.INTERRUPTED);
                        Thread.currentThread().interrupt();

                        return false;
                    }
                }

                final ServiceThread thread = new ServiceThread(
                    this,
                    "Process monitor (" + _ownerName + ")");

                if (_thread.compareAndSet(null, thread)) {
                    _logger
                        .debug(
                            ServiceMessages.STARTING_THREAD,
                            thread.getName());
                    thread.start();
                }

                while (!_ready) {    // Waits while process starts.
                    if (_thread.get() == null) {    // Thread committed suicide.
                        return false;
                    }

                    try {
                        _mutex.wait();
                    } catch (final InterruptedException exception) {
                        _logger.debug(ServiceMessages.INTERRUPTED);
                        Thread.currentThread().interrupt();

                        return false;
                    }
                }

                final String requestID = String.valueOf(newRequestID());

                if (!writeLine(requestID)) {    // Sends synchronization request.
                    cancelProcess(Optional.empty());

                    return false;
                }

                for (;;) {
                    final String line = readLine();

                    if (line == null) {
                        cancelProcess(Optional.empty());

                        return false;
                    }

                    if (line.equals(requestID)) {
                        break;    // Got synchronization (echo).
                    }
                }

                _busy = true;
            }
        }

        return true;
    }

    /**
     * Closes this.
     */
    public void close()
    {
        final Thread thread;

        synchronized (_mutex) {
            if (_closing) {
                return;
            }

            _closing = true;
            _cancelLimiter();
            _cancelExpirator();
            thread = _thread.get();
            _busy = false;
        }

        if (thread != null) {
            _logger.trace(ServiceMessages.CLOSING_PROCESS);
            _stopProcess(true);
        }
    }

    /**
     * Frees the process.
     *
     * <p>If there is a grace time and a timer object is available, an interrupt
     * will be scheduled; otherwise, unless the process is to be kept
     * indefinitely, it is interrupted.</p>
     */
    public void freeProcess()
    {
        final Thread thread;

        synchronized (_mutex) {
            final long graceMillis = _keepProcess
                .isPresent()? _keepProcess.get().toMillis(): -1;
            final Optional<Timer> timer = _getTimer();

            _busy = false;
            _cancelLimiter();

            if ((graceMillis > 0) && (timer.isPresent())) {
                _expirator = new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        expireProcess();
                    }
                };
                timer.get().schedule(_expirator, graceMillis);
                thread = null;
            } else if (graceMillis >= 0) {
                thread = _thread.get();
            } else {
                thread = null;
            }
        }

        if (thread != null) {
            _logger.trace(ServiceMessages.FREEING_PROCESS);
            _stopProcess(true);
        }
    }

    /**
     * Limits processing time.
     *
     * @param timeLimit The time limit in millis.
     */
    public void limit(final long timeLimit)
    {
        synchronized (_mutex) {
            final Optional<Timer> timer = _getTimer();

            if (timer.isPresent()) {
                _timeLimit = timeLimit;

                if (_timeLimit > 0) {
                    _logger
                        .trace(
                            ServiceMessages.TIME_LIMIT_SET,
                            ElapsedTime.fromMillis(_timeLimit));
                    _limiter = new TimerTask()
                    {
                        @Override
                        public void run()
                        {
                            cancelProcess(
                                Optional
                                    .of(ServiceMessages.TIME_LIMIT_EXCEEDED));
                        }
                    };
                    timer.get().schedule(_limiter, _timeLimit);
                }
            }
        }
    }

    /**
     * Reads a line from the process standard output.
     *
     * <p>Ignores empty lines or lines containing only spaces.</p>
     *
     * @return A trimmed line (null on failure).
     */
    @Nullable
    @CheckReturnValue
    public String readLine()
    {
        final BufferedReader stdout = _stdout;
        String line;

        if (stdout == null) {
            _logger.warn(ServiceMessages.PROCESS_PREMATURE_EXIT, _ownerName);

            return null;
        }

        for (;;) {
            try {
                line = stdout.readLine();
            } catch (final IOException exception) {
                _logger
                    .warn(
                        exception,
                        ServiceMessages.EXCEPTION_ON_RESPONSE,
                        _ownerName);

                return null;
            }

            if (line == null) {
                _logger
                    .warn(ServiceMessages.PROCESS_UNEXPECTED_END, _ownerName);

                break;
            }

            _logger.trace(ServiceMessages.RECEIVED_FROM_PROCESS, line);

            line = line.trim();

            if (line.length() > 0) {
                break;
            }
        }

        return line;
    }

    /**
     * Runs within the {@link java.lang.Thread} to monitor the execution of the
     * process.
     *
     * <p>Waits until the Thread is interrupted or the process dies by itself.
     * It will then perform a cleanup of the process context; this will close
     * stdin for the monitored process which, if still alive, is expected to
     * stop.</p>
     */
    @Override
    public void run()
    {
        final Charset charset = _charset;
        final Process process;
        final ProcessLogWriter logWriter;

        synchronized (_mutex) {
            _logger.trace(ServiceMessages.STARTING_PROCESS);

            try {
                process = _processBuilder.start();
            } catch (final IOException exception) {
                _logger.error(exception, ServiceMessages.PROCESS_START_FAILED);
                _thread.set(null);
                _mutex.notifyAll();

                return;
            }

            _logger.debug(ServiceMessages.PROCESS_STARTED);

            _stdin = new PrintWriter(
                new BufferedWriter(
                    new OutputStreamWriter(
                        process.getOutputStream(),
                        charset)));
            _stdout = new BufferedReader(
                new InputStreamReader(process.getInputStream(), charset));

            logWriter = new ProcessLogWriter(
                _ownerName,
                new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), charset)));
            logWriter.start();

            _ready = true;
            _mutex.notifyAll();
            _logger
                .debug(
                    ServiceMessages.THREAD_READY,
                    Thread.currentThread().getName());
        }

        try {
            _logger.trace(ServiceMessages.MONITORING_PROCESS);

            try {
                process.waitFor();

                final int exitValue = process.exitValue();

                if (exitValue != 0) {
                    _logger
                        .warn(
                            ServiceMessages.PROCESS_EXIT_CODE,
                            String.valueOf(exitValue));
                } else {
                    _logger
                        .debug(_ready
                               ? ServiceMessages.PROCESS_STOPPED_ITSELF
                               : ServiceMessages.PROCESS_STOPPED);
                }
            } catch (final InterruptedException exception1) {
                _logger.trace(ServiceMessages.PROCESS_WAIT_INTERRUPTED);
                _logger.debug(ServiceMessages.INTERRUPTING_PROCESS);
                process.destroy();
                _logger.trace(ServiceMessages.PROCESS_WAIT);

                try {
                    process.waitFor();
                } catch (final InterruptedException exception2) {
                    _logger
                        .debug(ServiceMessages.PROCESS_WAIT_INTERRUPTED_AGAIN);
                }
            }
        } finally {
            synchronized (_mutex) {
                _cancelLimiter();
                _cancelExpirator();
                _closeStdin();

                try {
                    _stdout.close();
                    _logger.trace(ServiceMessages.PROCESS_OUTPUT_CLOSED);
                } catch (final IOException exception) {
                    _logger
                        .debug(
                            ServiceMessages.PROCESS_OUTPUT_CLOSE_FAILED,
                            exception.getMessage());
                } finally {
                    _stdout = null;
                    _ready = false;
                }
            }

            if (_closing) {
                _logger.trace(ServiceMessages.CLOSING_LOG_WRITER);
                logWriter.close();
            } else {
                try {
                    if (logWriter.isAlive()) {
                        _logger.trace(ServiceMessages.LOG_WRITER_WAIT);
                        logWriter.join(_timeLimit);
                    }

                    _logger.trace(ServiceMessages.PROCESS_LOG_CLOSED);
                } catch (final InterruptedException exception) {
                    _logger.debug(ServiceMessages.LOG_WRITER_WAIT_INTERRUPTED);
                    logWriter.close();
                }
            }

            synchronized (_mutex) {
                _thread.set(null);
                _mutex.notifyAll();
            }
        }
    }

    /**
     * Writes a line to the process standard input.
     *
     * @param line The line.
     *
     * @return A true value on success.
     */
    @CheckReturnValue
    public boolean writeLine(@Nonnull final String line)
    {
        final PrintWriter processStdin = _stdin;

        if (processStdin == null) {
            _logger.warn(ServiceMessages.PROCESS_LOST, _ownerName);

            return false;
        }

        processStdin.println(line);

        if (processStdin.checkError()) {
            _logger.warn(ServiceMessages.PROCESS_REQUEST_FAILED, _ownerName);

            return false;
        }

        _logger.trace(ServiceMessages.SENT_TO_PROCESS, line);

        return true;
    }

    /**
     * Cancels the process.
     *
     * @param warning Optional warning message.
     */
    void cancelProcess(@Nonnull final Optional<Object> warning)
    {
        final Thread thread = _thread.get();

        synchronized (_mutex) {
            if ((thread == null) || !_busy || thread.isInterrupted()) {
                return;
            }

            _busy = false;
        }

        if (warning.isPresent()) {
            _logger
                .warn(ServiceMessages.CANCELLING_PROCESS_WARN, warning.get());
        } else {
            _logger.debug(ServiceMessages.CANCELLING_PROCESS);
        }

        _stopProcess(false);
    }

    /**
     * Expires the process.
     */
    void expireProcess()
    {
        final Thread thread = _thread.get();

        synchronized (_mutex) {
            if ((thread == null) || _busy || thread.isInterrupted()) {
                return;
            }
        }

        _logger.trace(ServiceMessages.PROCESS_INACTIVE);
        _stopProcess(true);
    }

    private void _cancelExpirator()
    {
        if (_expirator != null) {
            _expirator.cancel();
            _expirator = null;
            _logger.trace(ServiceMessages.PROCESS_EXPIRATOR_CANCELLED);
        }
    }

    private void _cancelLimiter()
    {
        if (_limiter != null) {
            _limiter.cancel();
            _limiter = null;
            _logger.trace(ServiceMessages.PROCESS_LIMITER_CANCELLED);
        }
    }

    private void _closeStdin()
    {
        synchronized (_mutex) {
            final PrintWriter stdin = _stdin;

            if (stdin != null) {
                final boolean hadErrors = stdin.checkError();

                stdin.close();

                if (stdin.checkError() && !hadErrors) {
                    _logger.warn(ServiceMessages.PROCESS_INPUT_CLOSE_FAILED);
                } else {
                    _logger.trace(ServiceMessages.PROCESS_INPUT_CLOSED);
                }

                _stdin = null;
            }
        }
    }

    private Optional<Timer> _getTimer()
    {
        final Optional<Service> service = _service;

        return service.isPresent()? service.get().getTimer(): Optional.empty();
    }

    private void _stopProcess(final boolean wait)
    {
        synchronized (_mutex) {
            if (_busy) {
                _logger.trace(ServiceMessages.PROCESS_STOP_CANCELLED);

                return;
            }

            _ready = false;
        }

        if (wait) {
            synchronized (_mutex) {
                final PrintWriter stdin = _stdin;

                if (stdin != null) {
                    final boolean hadErrors = stdin.checkError();

                    stdin.println(_END_MESSAGE);

                    if (stdin.checkError() && !hadErrors) {
                        _logger
                            .warn(
                                ServiceMessages.PROCESS_END_FAILED,
                                _END_MESSAGE);
                    } else {
                        _logger
                            .trace(
                                ServiceMessages.SENT_TO_PROCESS,
                                _END_MESSAGE);
                    }
                }
            }
        }

        _closeStdin();

        final ServiceThread thread = _thread.getAndSet(null);

        if (thread != null) {
            _logger.debug(ServiceMessages.STOPPING_THREAD, thread.getName());

            if (wait) {
                Require.ignored(thread.join(_logger, _killDelay.toMillis()));
            }

            if (thread.isAlive()) {
                Require
                    .ignored(
                        thread
                            .interruptAndJoin(
                                    _logger,
                                            _service.get().getJoinTimeout()));
            }
        }
    }

    private static final String _END_MESSAGE = "0";
    private static long _lastRequestID;

    private boolean _busy;
    private final Charset _charset;
    private volatile boolean _closing;
    private TimerTask _expirator;
    private final Optional<ElapsedTime> _keepProcess;
    private final ElapsedTime _killDelay;
    private TimerTask _limiter;
    private final Logger _logger;
    private final Object _mutex = new Object();
    private final String _ownerName;
    private final ProcessBuilder _processBuilder;
    private volatile boolean _ready;
    private final Optional<Service> _service;
    private volatile PrintWriter _stdin;
    private volatile BufferedReader _stdout;
    private final AtomicReference<ServiceThread> _thread =
        new AtomicReference<>();
    private volatile long _timeLimit;

    /**
     * Builder.
     */
    public static final class Builder
    {
        /**
         * Constructs an instance.
         */
        Builder() {}

        /**
         * Builds a process monitor.
         *
         * @return The process monitor (null on failure).
         */
        @Nullable
        @CheckReturnValue
        public ProcessMonitor build()
        {
            final String ownerName = Require.notNull(_ownerName);
            final Optional<ElapsedTime> killDelay = _killDelay;
            final Logger logger = Logger
                .getInstance(getClass().getName() + ':' + ownerName);
            Charset charset = _charset.orElse(null);

            if (charset == null) {
                charset = Charset.defaultCharset();
            }

            final List<String> list = new LinkedList<>();
            String program = _program.orElse(null);

            if (program != null) {
                program = program.trim();

                if (program.length() > 0) {
                    list.add(program);
                }
            }

            final Optional<String> command = _command;

            if (command.isPresent()) {
                list
                    .addAll(
                        Arrays
                            .asList(
                                    _WHITESPACE_PATTERN
                                            .split(command.get().trim())));
            }

            final String[] args = _args;

            if (args != null) {
                for (final String arg: args) {
                    list.add(arg.trim());
                }
            }

            if (list.isEmpty()) {
                logger.error(ServiceMessages.NOTHING_TO_EXECUTE);

                return null;
            }

            if (logger.isDebugEnabled()) {
                final StringBuilder stringBuilder = new StringBuilder();

                for (final String arg: list) {
                    stringBuilder.append(' ');
                    stringBuilder.append(arg);
                }

                logger.debug(ServiceMessages.COMMAND, stringBuilder);
            }

            final ProcessBuilder processBuilder = new ProcessBuilder(list);

            if (_directory.isPresent()) {
                processBuilder.directory(_directory.get());
                logger
                    .debug(
                        () -> new Message(
                            ServiceMessages.DIRECTORY,
                            _directory.get().getAbsolutePath()));
            }

            final Map<String, String> env = processBuilder.environment();

            if (_sets != null) {
                String pathKey = null;

                for (final String set: _sets) {
                    final int equalPos = set.indexOf('=');
                    final int operPos = ((equalPos > 0)
                            && (set.charAt(
                                equalPos - 1) == '+'))? (equalPos - 1)
                                    : equalPos;
                    String key = (operPos < 0)? set: set.substring(0, operPos);

                    if (key.equalsIgnoreCase(_PATH_KEY)) {
                        if (pathKey == null) {
                            pathKey = key;

                            for (final String envKey: env.keySet()) {
                                if (envKey.equalsIgnoreCase(_PATH_KEY)) {
                                    pathKey = envKey;

                                    break;
                                }
                            }
                        }

                        key = pathKey;
                    }

                    if (operPos >= 0) {
                        String text = set.substring(equalPos + 1);

                        if (key.equalsIgnoreCase(_PATH_KEY)
                                || key.equals(_CLASSPATH_KEY)) {
                            try {
                                text = ValueConverter.canonicalizePath(text);
                            } catch (final IOException exception) {
                                throw new RuntimeException(exception);
                            }
                        }

                        if (operPos != equalPos) {
                            final String prefix = env.get(key);

                            if ((prefix != null)
                                    && (prefix.trim().length() > 0)) {
                                if (key.equalsIgnoreCase(_PATH_KEY)
                                        || key.equals(_CLASSPATH_KEY)) {
                                    text = prefix + _PATH_SEPARATOR + text;
                                } else {
                                    text = prefix + text;
                                }
                            }
                        }

                        env.put(key, text);
                        logger
                            .debug(ServiceMessages.ENVIRONMENT_SET, key, text);
                    } else {
                        env.remove(key);
                    }
                }
            }

            return new ProcessMonitor(
                ownerName,
                Logger.getInstance(getClass().getName() + ':' + ownerName),
                (killDelay.isPresent() && (killDelay.get().toMillis() > 0))
                ? killDelay
                    .get(): _DEFAULT_KILL_DELAY,
                _service,
                charset,
                processBuilder,
                _keepProcess);
        }

        /**
         * Sets the args.
         *
         * @param args The args.
         *
         * @return This.
         */
        @Nonnull
        public Builder setArgs(@Nonnull final String[] args)
        {
            _args = args;

            return this;
        }

        /**
         * Sets the character set.
         *
         * @param charset The character set.
         *
         * @return This.
         */
        @Nonnull
        public Builder setCharset(@Nonnull final Optional<Charset> charset)
        {
            _charset = charset;

            return this;
        }

        /**
         * Sets the command.
         *
         * @param command The optional command.
         *
         * @return This.
         */
        @Nonnull
        public Builder setCommand(@Nonnull final Optional<String> command)
        {
            _command = command;

            return this;
        }

        /**
         * Sets the directory.
         *
         * @param directory The optional directory.
         *
         * @return This.
         */
        @Nonnull
        public Builder setDirectory(@Nonnull final Optional<File> directory)
        {
            _directory = directory;

            return this;
        }

        /**
         * Sets the keepProcess.
         *
         * @param keepProcess The optional keepProcess.
         *
         * @return This.
         */
        @Nonnull
        public Builder setKeepProcess(
                @Nonnull final Optional<ElapsedTime> keepProcess)
        {
            _keepProcess = keepProcess;

            return this;
        }

        /**
         * Sets the killDelay.
         *
         * @param killDelay The optional killDelay.
         *
         * @return This.
         */
        @Nonnull
        public Builder setKillDelay(
                @Nonnull final Optional<ElapsedTime> killDelay)
        {
            _killDelay = killDelay;

            return this;
        }

        /**
         * Sets the ownerName.
         *
         * @param ownerName The ownerName.
         *
         * @return This.
         */
        @Nonnull
        public Builder setOwnerName(@Nonnull final String ownerName)
        {
            _ownerName = ownerName;

            return this;
        }

        /**
         * Sets the program.
         *
         * @param program The optional program.
         *
         * @return This.
         */
        @Nonnull
        public Builder setProgram(@Nonnull final Optional<String> program)
        {
            _program = program;

            return this;
        }

        /**
         * Sets the service.
         *
         * @param service The optional service.
         *
         * @return This.
         */
        @Nonnull
        public Builder setService(@Nonnull final Optional<Service> service)
        {
            _service = service;

            return this;
        }

        /**
         * Sets the sets.
         *
         * @param sets The sets.
         *
         * @return This.
         */
        @Nonnull
        public Builder setSets(@Nonnull final String[] sets)
        {
            _sets = sets;

            return this;
        }

        private static final String _CLASSPATH_KEY = "CLASSPATH";
        private static final ElapsedTime _DEFAULT_KILL_DELAY = ElapsedTime
            .fromMillis(60000);
        private static final String _PATH_KEY = "PATH";
        private static final String _PATH_SEPARATOR = File.pathSeparator;
        private static final Pattern _WHITESPACE_PATTERN = Pattern
            .compile("\\s");

        private String[] _args;
        private Optional<Charset> _charset = Optional.empty();
        private Optional<String> _command = Optional.empty();
        private Optional<File> _directory = Optional.empty();
        private Optional<ElapsedTime> _keepProcess = Optional.empty();
        private Optional<ElapsedTime> _killDelay = Optional.empty();
        private String _ownerName;
        private Optional<String> _program = Optional.empty();
        private Optional<Service> _service = Optional.empty();
        private String[] _sets;
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
