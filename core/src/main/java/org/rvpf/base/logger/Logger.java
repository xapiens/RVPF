/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Logger.java 4109 2019-07-22 23:31:37Z SFB $
 */

package org.rvpf.base.logger;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.lang.reflect.Field;

import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.spi.ExtendedLogger;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.DateTime;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.Listeners;

/**
 * The Logger class provides an implementation independent interface to the
 * logging facilities.
 *
 * <p>Support is provided for the messages resources and formatting classes. To
 * minimize overhead, the resource access and text formatting is done just in
 * time.</p>
 *
 * <p>This class is serializable, but the serialization process only propagates
 * the logger identification (name).</p>
 *
 * <p>The current implementation is a thin wrapper over Log4J.</p>
 */
@ThreadSafe
public class Logger
    implements Externalizable, Thread.UncaughtExceptionHandler
{
    /**
     * Constructs an instance.
     *
     * <p>This is needed for an Externalizable implementation.</p>
     */
    public Logger()
    {
        this(Logger.class);
    }

    /**
     * Constructs an instance.
     *
     * @param ownerClass The owner class.
     */
    protected Logger(@Nonnull final Class<?> ownerClass)
    {
        this(LogManager.getLogger(ownerClass.getName()));
    }

    /**
     * Constructs an instance.
     *
     * @param log4jLogger The 'org.apache.logging.log4j' Logger.
     */
    protected Logger(@Nonnull final org.apache.logging.log4j.Logger log4jLogger)
    {
        _log4jLogger = Require.notNull((ExtendedLogger) log4jLogger);
    }

    /**
     * Adds a listener for this logger.
     *
     * @param listener The listener.
     *
     * @return True unless already added.
     */
    @CheckReturnValue
    public static boolean addListener(@Nonnull final LogListener listener)
    {
        return _listeners.add(Require.notNull(listener));
    }

    /**
     * Returns the current log ID.
     *
     * @return The log ID (may be empty).
     */
    @Nonnull
    @CheckReturnValue
    public static synchronized Optional<String> currentLogID()
    {
        return Optional.ofNullable(ThreadContext.get(ID_KEY));
    }

    /**
     * Gets a logger instance.
     *
     * @param ownerClass The class of the logger's owner.
     *
     * @return The logger instance.
     */
    @Nonnull
    @CheckReturnValue
    public static Logger getInstance(@Nonnull final Class<?> ownerClass)
    {
        return getInstance(ownerClass.getName());
    }

    /**
     * Gets a logger instance.
     *
     * <p>This is used by scripts to supply their Log4j logger in calls to RVPF.
     * </p>
     *
     * @param log4jLogger A log4j logger.
     *
     * @return The logger instance.
     */
    @Nonnull
    @CheckReturnValue
    public static synchronized Logger getInstance(
            @Nonnull final org.apache.logging.log4j.Logger log4jLogger)
    {
        Logger logger = _loggers.get(Require.notNull(log4jLogger));

        if ((logger == null) || logger.hasLogged(LogLevel.WARN)) {
            logger = new Logger(log4jLogger);
            _loggers.put(log4jLogger, logger);
        } else {
            logger.reset();
        }

        return logger;
    }

    /**
     * Gets a logger instance.
     *
     * @param name The logger's name.
     *
     * @return The logger instance.
     */
    @Nonnull
    @CheckReturnValue
    public static Logger getInstance(@Nonnull final String name)
    {
        return getInstance(LogManager.getLogger(Require.notNull(name)));
    }

    /**
     * Gets the MIDNIGHT logger.
     *
     * @return The MIDNIGHT logger.
     */
    @Nonnull
    @CheckReturnValue
    public static synchronized Logger getMidnightLogger()
    {
        Logger midnightLogger = _midnightLogger;

        if (midnightLogger == null) {
            midnightLogger = getInstance(MIDNIGHT_LOGGER_NAME);
            _midnightLogger = midnightLogger;
        }

        return midnightLogger;
    }

    /**
     * Asks if logging is shut down.
     *
     * @return True if logging is shut down.
     */
    @CheckReturnValue
    public static synchronized boolean isShutDown()
    {
        return _shutDown;
    }

    /**
     * Logs back end informations.
     */
    public static void logBackEnd()
    {
        final Package backEndPackage = LoggerContext.class.getPackage();

        getInstance(Logger.class)
            .debug(
                BaseMessages.LOGGER_BACK_END,
                backEndPackage.getImplementationTitle(),
                backEndPackage.getImplementationVersion());
    }

    /**
     * Removes a listener for this logger.
     *
     * @param listener The listener.
     *
     * @return True if it was present.
     */
    @CheckReturnValue
    public static boolean removeListener(@Nonnull final LogListener listener)
    {
        return _listeners.remove(Require.notNull(listener));
    }

    /**
     * Restores the log ID.
     *
     * <p>If the log ID is empty, any previous value is removed.</p>
     *
     * @param logID The log ID.
     */
    public static synchronized void restoreLogID(
            @Nonnull final Optional<String> logID)
    {
        if (logID.isPresent()) {
            ThreadContext.put(ID_KEY, logID.get());
        } else {
            ThreadContext.remove(ID_KEY);
        }
    }

    /**
     * Sets the log ID to its default value.
     */
    public static void setLogID()
    {
        setLogID(Optional.ofNullable(System.getProperty(LOG_ID_PROPERTY)));
    }

    /**
     * Sets the log ID.
     *
     * <p>If the log ID is empty, a new value is generated.</p>
     *
     * @param logID The log ID.
     */
    public static synchronized void setLogID(
            @Nonnull final Optional<String> logID)
    {
        ThreadContext
            .put(
                ID_KEY,
                logID.isPresent()? logID.get().trim(): Integer
                    .toString(_generated_id.incrementAndGet()));
    }

    /**
     * Shuts down logging.
     */
    public static synchronized void shutDown()
    {
        if (!_shutDown) {
            _shutDown = true;

            if (_manualShutdown) {
                Configurator.shutdown((LoggerContext) LogManager.getContext());
            }
        }
    }

    /**
     * Starts up logging.
     *
     * @param manualShutdown True if {@link #shutDown} will be called.
     */
    public static synchronized void startUp(final boolean manualShutdown)
    {
        System
            .setProperty(
                "java.util.logging.manager",
                "org.apache.logging.log4j.jul.LogManager");
        System.setProperty("log4j2.disable.jmx", "true");

        if (manualShutdown) {
            System
                .setProperty(
                    ShutdownCallbackRegistry.SHUTDOWN_HOOK_ENABLED,
                    "false");
        }

        _manualShutdown = manualShutdown;
    }

    /**
     * Logs a debug message.
     *
     * @param message The message.
     */
    public final void debug(@Nonnull final Message message)
    {
        log(LogLevel.DEBUG, message);
    }

    /**
     * Logs a debug message.
     *
     * @param messageSupplier A message supplier.
     */
    public final void debug(@Nonnull final Supplier<Message> messageSupplier)
    {
        log(LogLevel.DEBUG, Require.notNull(messageSupplier));
    }

    /**
     * Logs a debug message.
     *
     * @param entry The messages entry.
     * @param params The message parameters.
     */
    public final void debug(
            @Nonnull final Messages.Entry entry,
            @Nonnull final Object... params)
    {
        log(LogLevel.DEBUG, Require.notNull(entry), params);
    }

    /**
     * Logs a debug message.
     *
     * @param format The message format.
     * @param params The message parameters.
     *
     * @deprecated For temporary use.
     */
    @Deprecated
    public final void debug(
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        log(LogLevel.DEBUG, Require.notNull(format), params);
    }

    /**
     * Logs a debug message.
     *
     * @param cause The message cause.
     * @param entry The messages entry.
     * @param params The message parameters.
     */
    public final void debug(
            @Nonnull final Throwable cause,
            @Nonnull final Messages.Entry entry,
            @Nonnull final Object... params)
    {
        log(LogLevel.DEBUG, cause, Require.notNull(entry), params);
    }

    /**
     * Logs a debug message.
     *
     * @param cause The message cause.
     * @param format The message format.
     * @param params The message parameters.
     *
     * @deprecated For temporary use.
     */
    @Deprecated
    public final void debug(
            @Nonnull final Throwable cause,
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        log(LogLevel.DEBUG, cause, Require.notNull(format), params);
    }

    /**
     * Logs an error message.
     *
     * @param message The message.
     */
    public final void error(@Nonnull final Message message)
    {
        log(LogLevel.ERROR, message);
    }

    /**
     * Logs an error message.
     *
     * @param entry The messages entry.
     * @param params The message parameters.
     */
    public final void error(
            @Nonnull final Messages.Entry entry,
            @Nonnull final Object... params)
    {
        log(LogLevel.ERROR, Require.notNull(entry), params);
    }

    /**
     * Logs an error message.
     *
     * @param format The message format.
     * @param params The message parameters.
     *
     * @deprecated For temporary use.
     */
    @Deprecated
    public final void error(
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        log(LogLevel.ERROR, Require.notNull(format), params);
    }

    /**
     * Logs an error message.
     *
     * @param cause The message cause.
     * @param entry The messages entry.
     * @param params The message parameters.
     */
    public final void error(
            @Nonnull final Throwable cause,
            @Nonnull final Messages.Entry entry,
            @Nonnull final Object... params)
    {
        log(LogLevel.ERROR, cause, Require.notNull(entry), params);
    }

    /**
     * Logs an error message.
     *
     * @param cause The message cause.
     * @param format The message format.
     * @param params The message parameters.
     *
     * @deprecated For temporary use.
     */
    @Deprecated
    public final void error(
            @Nonnull final Throwable cause,
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        log(LogLevel.ERROR, cause, Require.notNull(format), params);
    }

    /**
     * Logs a fatal message.
     *
     * @param message The message.
     */
    public final void fatal(@Nonnull final Message message)
    {
        log(LogLevel.FATAL, message);
    }

    /**
     * Logs a fatal message.
     *
     * @param entry The messages entry.
     * @param params The message parameters.
     */
    public final void fatal(
            @Nonnull final Messages.Entry entry,
            @Nonnull final Object... params)
    {
        log(LogLevel.FATAL, Require.notNull(entry), params);
    }

    /**
     * Logs a fatal message.
     *
     * @param format The message format.
     * @param params The message parameters.
     *
     * @deprecated For temporary use.
     */
    @Deprecated
    public final void fatal(
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        log(LogLevel.FATAL, Require.notNull(format), params);
    }

    /**
     * Logs a fatal message.
     *
     * @param cause The message cause.
     * @param entry The messages entry.
     * @param params The message parameters.
     */
    public final void fatal(
            @Nonnull final Throwable cause,
            @Nonnull final Messages.Entry entry,
            @Nonnull final Object... params)
    {
        log(LogLevel.FATAL, cause, Require.notNull(entry), params);
    }

    /**
     * Logs a fatal message.
     *
     * @param cause The message cause.
     * @param format The message format.
     * @param params The message parameters.
     *
     * @deprecated For temporary use.
     */
    @Deprecated
    public final void fatal(
            @Nonnull final Throwable cause,
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        log(LogLevel.FATAL, cause, Require.notNull(format), params);
    }

    /**
     * Gets the log level.
     *
     * @return The log level.
     */
    @Nonnull
    @CheckReturnValue
    public final LogLevel getLogLevel()
    {
        return LogLevel.get(_log4jLogger.getLevel());
    }

    /**
     * Gets this logger's name.
     *
     * @return This logger's name.
     */
    @Nonnull
    @CheckReturnValue
    public final String getName()
    {
        return _log4jLogger.getName();
    }

    /**
     * Gets a PrintWriter
     *
     * <p>All messages sent to the PrintWriter will be processed according to
     * the log level supplied here.</p>
     *
     * @param logLevel The log level.
     *
     * @return A PrintWriter.
     */
    @Nonnull
    @CheckReturnValue
    public final PrintWriter getPrintWriter(@Nonnull final LogLevel logLevel)
    {
        final StringWriter logWriter = new StringWriter()
        {
            /** {@inheritDoc}
             */
            @Override
            public void flush()
            {
                super.flush();

                if (getLog4jLevel().isLessSpecificThan(logLevel.getLevel())) {
                    String text = toString();

                    while (text.endsWith("\n") || text.endsWith("\r")) {
                        text = text.substring(0, text.length() - 1);
                    }

                    if (text.length() > 0) {
                        log(logLevel, new Message(BaseMessages.VERBATIM, text));
                    }

                    getBuffer().setLength(0);
                }
            }
        };

        return new PrintWriter(logWriter, true);
    }

    /**
     * Asks if this has logged at least at the specified level.
     *
     * @param logLevel A LogLevel.
     *
     * @return True if it has logged at least at the specified level.
     *
     * @see #reset
     */
    @CheckReturnValue
    public final boolean hasLogged(@Nonnull final LogLevel logLevel)
    {
        final LogLevel loggedLevel = _loggedLevel.get();

        return (loggedLevel != null) && (loggedLevel.compareTo(logLevel) <= 0);
    }

    /**
     * Logs an info message.
     *
     * @param message The message.
     */
    public final void info(@Nonnull final Message message)
    {
        log(LogLevel.INFO, message);
    }

    /**
     * Logs an info message.
     *
     * @param messageSupplier A message supplier.
     */
    public final void info(@Nonnull final Supplier<Message> messageSupplier)
    {
        log(LogLevel.INFO, messageSupplier);
    }

    /**
     * Logs an info message.
     *
     * @param entry The messages entry.
     * @param params The message parameters.
     */
    public final void info(
            @Nonnull final Messages.Entry entry,
            @Nonnull final Object... params)
    {
        log(LogLevel.INFO, entry, params);
    }

    /**
     * Logs an info message.
     *
     * @param format The message format.
     * @param params The message parameters.
     *
     * @deprecated For temporary use.
     */
    @Deprecated
    public final void info(
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        log(LogLevel.INFO, format, params);
    }

    /**
     * Logs an info message.
     *
     * @param cause The message cause.
     * @param entry The messages entry.
     * @param params The message parameters.
     */
    public final void info(
            @Nonnull final Throwable cause,
            @Nonnull final Messages.Entry entry,
            @Nonnull final Object... params)
    {
        log(LogLevel.INFO, cause, entry, params);
    }

    /**
     * Logs an info message.
     *
     * @param cause The message cause.
     * @param format The message format.
     * @param params The message parameters.
     *
     * @deprecated For temporary use.
     */
    @Deprecated
    public final void info(
            @Nonnull final Throwable cause,
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        log(LogLevel.INFO, cause, format, params);
    }

    /**
     * Asks if the debug level is enabled.
     *
     * @return True if enabled.
     */
    @CheckReturnValue
    public final boolean isDebugEnabled()
    {
        return getLog4jLevel().isLessSpecificThan(Level.DEBUG);
    }

    /**
     * Asks if a log level is enabled.
     *
     * @param logLevel The log level.
     *
     * @return True if enabled.
     */
    @CheckReturnValue
    public final boolean isEnabledFor(final LogLevel logLevel)
    {
        return getLog4jLevel().isLessSpecificThan(logLevel.getLevel());
    }

    /**
     * Asks if the info level is enabled.
     *
     * @return True if enabled.
     */
    @CheckReturnValue
    public final boolean isInfoEnabled()
    {
        return getLog4jLevel().isLessSpecificThan(Level.INFO);
    }

    /**
     * Asks if the trace level is enabled.
     *
     * @return True if enabled.
     */
    @CheckReturnValue
    public final boolean isTraceEnabled()
    {
        return getLog4jLevel().isLessSpecificThan(Level.TRACE);
    }

    /**
     * Logs a message.
     *
     * @param logLevel The log level.
     * @param message The message.
     */
    public final void log(
            @Nonnull final LogLevel logLevel,
            @Nonnull final Message message)
    {
        if (getLog4jLevel().isLessSpecificThan(logLevel.getLevel())) {
            doLog(logLevel, Require.notNull(message));
        }
    }

    /**
     * Logs a debug message.
     *
     * @param logLevel The log level.
     * @param messageSupplier A message supplier.
     */
    public final void log(
            @Nonnull final LogLevel logLevel,
            @Nonnull final Supplier<Message> messageSupplier)
    {
        if (getLog4jLevel().isLessSpecificThan(logLevel.getLevel())) {
            doLog(logLevel, messageSupplier.get());
        }
    }

    /**
     * Logs a message.
     *
     * @param logLevel The log level.
     * @param entry The messages entry.
     * @param params The message parameters.
     */
    public final void log(
            @Nonnull final LogLevel logLevel,
            @Nonnull final Messages.Entry entry,
            @Nonnull final Object... params)
    {
        if (getLog4jLevel().isLessSpecificThan(logLevel.getLevel())) {
            doLog(logLevel, new Message(entry, params));
        }
    }

    /**
     * Logs a message.
     *
     * @param logLevel The log level.
     * @param format The message format.
     * @param params The message parameters.
     */
    public final void log(
            @Nonnull final LogLevel logLevel,
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        if (getLog4jLevel().isLessSpecificThan(logLevel.getLevel())) {
            doLog(logLevel, new Message(format, params));
        }
    }

    /**
     * Logs a message.
     *
     * @param logLevel The log level.
     * @param cause The message cause.
     * @param entry The messages entry.
     * @param params The message parameters.
     */
    public final void log(
            @Nonnull final LogLevel logLevel,
            @Nonnull final Throwable cause,
            @Nonnull final Messages.Entry entry,
            @Nonnull final Object... params)
    {
        if (getLog4jLevel().isLessSpecificThan(logLevel.getLevel())) {
            doLog(logLevel, new Message(cause, entry, params));
        }
    }

    /**
     * Logs a message.
     *
     * @param logLevel The log level.
     * @param cause The message cause.
     * @param format The message format.
     * @param params The message parameters.
     */
    public final void log(
            @Nonnull final LogLevel logLevel,
            @Nonnull final Throwable cause,
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        if (getLog4jLevel().isLessSpecificThan(logLevel.getLevel())) {
            doLog(logLevel, new Message(cause, format, params));
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final void readExternal(
            @Nonnull final ObjectInput input)
        throws IOException
    {
        synchronized (_log4jLoggerField) {
            _log4jLoggerField.setAccessible(true);

            try {
                _log4jLoggerField
                    .set(this, LogManager.getLogger(input.readUTF()));
            } catch (final IllegalArgumentException
                     |IllegalAccessException exception) {
                throw new InternalError(exception);
            }

            _log4jLoggerField.setAccessible(false);
        }
    }

    /**
     * Resets the maximum level actually logged with this logger.
     *
     * @see #hasLogged
     */
    public final void reset()
    {
        _loggedLevel.set(null);
    }

    /**
     * Logs a trace message.
     *
     * @param message The message.
     */
    public final void trace(@Nonnull final Message message)
    {
        log(LogLevel.TRACE, message);
    }

    /**
     * Logs a trace message.
     *
     * @param messageSupplier A message supplier.
     */
    public final void trace(@Nonnull final Supplier<Message> messageSupplier)
    {
        log(LogLevel.TRACE, messageSupplier);
    }

    /**
     * Logs a trace message.
     *
     * @param entry The messages entry.
     * @param params The message parameters.
     */
    public final void trace(
            @Nonnull final Messages.Entry entry,
            @Nonnull final Object... params)
    {
        log(LogLevel.TRACE, entry, params);
    }

    /**
     * Logs a trace message.
     *
     * @param format The message format.
     * @param params The message parameters.
     *
     * @deprecated For temporary use.
     */
    @Deprecated
    public final void trace(
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        log(LogLevel.TRACE, format, params);
    }

    /**
     * Logs a trace message.
     *
     * @param cause The message cause.
     * @param entry The messages entry.
     * @param params The message parameters.
     */
    public final void trace(
            @Nonnull final Throwable cause,
            @Nonnull final Messages.Entry entry,
            @Nonnull final Object... params)
    {
        log(LogLevel.TRACE, cause, entry, params);
    }

    /**
     * Logs a trace message.
     *
     * @param cause The message cause.
     * @param format The message format.
     * @param params The message parameters.
     *
     * @deprecated For temporary use.
     */
    @Deprecated
    public final void trace(
            @Nonnull final Throwable cause,
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        log(LogLevel.TRACE, cause, format, params);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void uncaughtException(
            @Nonnull final Thread thread,
            @Nonnull final Throwable throwable)
    {
        if (!_shutDown) {
            getInstance(Logger.class)
                .fatal(
                    throwable,
                    BaseMessages.THREAD_EXCEPTION,
                    thread.getName(),
                    throwable);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param message The message.
     */
    public final void warn(@Nonnull final Message message)
    {
        log(LogLevel.WARN, message);
    }

    /**
     * Logs a warning message.
     *
     * @param entry The messages entry.
     * @param params The message parameters.
     */
    public final void warn(
            @Nonnull final Messages.Entry entry,
            @Nonnull final Object... params)
    {
        log(LogLevel.WARN, entry, params);
    }

    /**
     * Logs a warning message.
     *
     * @param format The message format.
     * @param params The message parameters.
     *
     * @deprecated For temporary use.
     */
    @Deprecated
    public final void warn(
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        log(LogLevel.WARN, format, params);
    }

    /**
     * Logs a warning message.
     *
     * @param cause The message cause.
     * @param entry The messages entry.
     * @param params The message parameters.
     */
    public final void warn(
            @Nonnull final Throwable cause,
            @Nonnull final Messages.Entry entry,
            @Nonnull final Object... params)
    {
        log(LogLevel.WARN, cause, entry, params);
    }

    /**
     * Logs a warning message.
     *
     * @param cause The message cause.
     * @param format The message format.
     * @param params The message parameters.
     *
     * @deprecated For temporary use.
     */
    @Deprecated
    public final void warn(
            @Nonnull final Throwable cause,
            @Nonnull final String format,
            @Nonnull final Object... params)
    {
        log(LogLevel.WARN, cause, format, params);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void writeExternal(
            @Nonnull final ObjectOutput output)
        throws IOException
    {
        output.writeUTF(getName());
    }

    /**
     * Logs a message.
     *
     * @param logLevel The log level.
     * @param message The message.
     */
    protected void doLog(
            @Nonnull LogLevel logLevel,
            @Nonnull final Message message)
    {
        if (!_listeners.isEmpty()) {
            for (final LogListener listener: _listeners) {
                final LogLevel newLevel = listener
                    .onLog(this, logLevel, message)
                    .orElse(null);

                if (newLevel != logLevel) {
                    if ((newLevel == null)
                            || getLog4jLevel().isMoreSpecificThan(
                                newLevel.getLevel())) {
                        return;
                    }

                    logLevel = newLevel;
                }
            }
        }

        if (_shutDown) {
            final StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append(DateTime.now().toFullString());
            stringBuilder.append(' ');
            stringBuilder.append(logLevel.name());

            if (logLevel.name().length() < 5) {
                stringBuilder.append(' ');
            }

            stringBuilder.append(" [");
            stringBuilder.append(_log4jLogger.getName());
            stringBuilder.append("] ");
            stringBuilder.append(message.toString());
            System.err.println(stringBuilder);

            final Optional<Throwable> messageCause = message.getCause();

            if (messageCause.isPresent()) {
                messageCause.get().printStackTrace(System.err);
            }

            System.err.flush();
        } else {
            message.saveFormatted();
            _log4jLogger
                .logMessage(
                    _FQCN,
                    logLevel.getLevel(),
                    null,
                    message,
                    message.getCause().orElse(null));

            for (;;) {
                final LogLevel loggedLevel = _loggedLevel.get();

                if ((loggedLevel == null)
                        || (logLevel.compareTo(loggedLevel) < 0)) {
                    if (!_loggedLevel.compareAndSet(loggedLevel, logLevel)) {
                        continue;
                    }
                }

                break;
            }
        }
    }

    /**
     * Gets the log4j Level.
     *
     * @return The log4j Level.
     */
    @Nonnull
    @CheckReturnValue
    protected final Level getLog4jLevel()
    {
        return _log4jLogger.getLevel();
    }

    /**
     * Gets the log4j Logger.
     *
     * @return The log4j Logger.
     */
    @Nonnull
    @CheckReturnValue
    protected final org.apache.logging.log4j.Logger getLog4jLogger()
    {
        return _log4jLogger;
    }

    private Object readResolve()
    {
        synchronized (Logger.class) {
            return _loggers.computeIfAbsent(_log4jLogger, logger -> this);
        }
    }

    /** Diagnostic Context ID key. */
    public static final String ID_KEY = "ID";

    /** Log ID property. */
    public static final String LOG_ID_PROPERTY = "rvpf.log.id";

    /** Midnight logger name. */
    public static final String MIDNIGHT_LOGGER_NAME = "MIDNIGHT";

    /**  */

    private static final String _FQCN = Logger.class.getName();
    private static final Field _log4jLoggerField;
    private static boolean _manualShutdown;
    private static Logger _midnightLogger;
    private static boolean _shutDown;
    private static final long serialVersionUID = 1L;
    private static final Map<org.apache.logging.log4j.Logger, Logger> _loggers =
        new IdentityHashMap<>();
    private static final Listeners<LogListener> _listeners = new Listeners<>();
    private static final AtomicInteger _generated_id = new AtomicInteger();

    static {
        try {
            _log4jLoggerField = Logger.class.getDeclaredField("_log4jLogger");
        } catch (final NoSuchFieldException|SecurityException exception) {
            throw new InternalError(exception);
        }
    }

    private final ExtendedLogger _log4jLogger;
    private final AtomicReference<LogLevel> _loggedLevel =
        new AtomicReference<>();

    /**
     * Log Level.
     */
    public enum LogLevel
    {
        NONE(Level.OFF),
        FATAL(Level.FATAL),
        ERROR(Level.ERROR),
        WARN(Level.WARN),
        INFO(Level.INFO),
        DEBUG(Level.DEBUG),
        TRACE(Level.TRACE),
        ALL(Level.ALL);

        LogLevel(final Level level)
        {
            _level = level;
        }

        /**
         * Gets the log level from the ordinal.
         *
         * @param ordinal The ordinal.
         *
         * @return The log level.
         */
        public static LogLevel get(final int ordinal)
        {
            switch (ordinal) {
                case 0: {
                    return NONE;
                }
                case 1: {
                    return FATAL;
                }
                case 2: {
                    return ERROR;
                }
                case 3: {
                    return WARN;
                }
                case 4: {
                    return INFO;
                }
                case 5: {
                    return DEBUG;
                }
                case 6: {
                    return TRACE;
                }
                case 7: {
                    return ALL;
                }
                default: {
                    return DEBUG;
                }
            }
        }

        /**
         * Gets the log level from its name.
         *
         * @param name The name.
         *
         * @return The log level.
         */
        @Nonnull
        @CheckReturnValue
        public static LogLevel get(@Nonnull final String name)
        {
            return valueOf(LogLevel.class, name.toUpperCase(Locale.ROOT));
        }

        /**
         * Gets the log level from a log4j level.
         *
         * @param level The log4j level.
         *
         * @return The log level.
         */
        @Nonnull
        @CheckReturnValue
        static LogLevel get(@Nonnull final Level level)
        {
            return get(level.toString());
        }

        /**
         * Gets the log4j level.
         *
         * @return The log4j level.
         */
        @Nonnull
        @CheckReturnValue
        Level getLevel()
        {
            return _level;
        }

        private final Level _level;
    }

    /**
     * Log listener.
     */
    public interface LogListener
    {
        /**
         * Called before logging occurs.
         *
         * @param logger The logger.
         * @param logLevel The log level.
         * @param message The message.
         *
         * @return A log level (empty to discard).
         */
        @Nonnull
        @CheckReturnValue
        Optional<LogLevel> onLog(
                @Nonnull Logger logger,
                @Nonnull LogLevel logLevel,
                @Nonnull Message message);
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
