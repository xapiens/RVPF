/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Profiler.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.base.tool;

import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;

/**
 * Profiler. Provides rudimentary profiling capabilities.
 *
 * <p>When configured to do so, will take periodic samples of all or some thread
 * execution point, and / or take periodic snapshots of these threads. Sampling
 * will conclude by a tally of the execution points; snapshots will provide
 * tracebacks in the log.</p>
 *
 * <p>All the configuration is done with System properties.</p>
 *
 * <h1>Configuration properties</h1>
 *
 * <dl>
 *   <dt>{@value #START_MILLIS_PROPERTY}</dt>
 *   <dd>The number of milliseconds to wait after being started before doing
 *     anything. Defaults to no wait.</dd>
 *
 *   <dt>{@value #SAMPLE_MILLIS_PROPERTY}</dt>
 *   <dd>The number of milliseconds to wait between taking samples. Defaults to
 *     not taking samples.</dd>
 *
 *   <dt>{@value #SAMPLE_COUNT_PROPERTY}</dt>
 *   <dd>The number of samples to take. Defaults to unlimited.</dd>
 *
 *   <dt>{@value #SAMPLE_PRIORITY_PROPERTY}</dt>
 *   <dd>The priority of the thread taking samples. Defaults to 9.</dd>
 *
 *   <dt>{@value #SNAPSHOT_MILLIS_PROPERTY}</dt>
 *   <dd>The number of milliseconds to wait between taking snapshots. Defaults
 *     to not taking snapshots, except when a start wait is specified, where a
 *     single snapshot is taken.</dd>
 *
 *   <dt>{@value #SNAPSHOT_COUNT_PROPERTY}</dt>
 *   <dd>The number of snapshots to take. Defaults to unlimited.</dd>
 *
 *   <dt>{@value #SNAPSHOT_DEPTH_PROPERTY}</dt>
 *   <dd>The stack depth of each snapshot. Defaults to unlimited.</dd>
 *
 *   <dt>{@value #SNAPSHOT_PRIORITY_PROPERTY}</dt>
 *   <dd>The priority of the thread taking snapshots. Defaults to 8.</dd>
 *
 *   <dt>{@value #THREAD_GROUP_PROPERTY}</dt>
 *   <dd>The thread group on which samples and snapshots will be taken. Defaults
 *     to any.</dd>
 *
 *   <dt>{@value #THREAD_STATE_PROPERTY}</dt>
 *   <dd>The state of threads on which samples and snapshots will be taken.
 *     Defaults to any.</dd>
 *
 *   <dt>{@value #STOP_IGNORED_PROPERTY}</dt>
 *   <dd>When true, calls to the {@link #stop} method will be ignored. This is
 *     useful when looking for activities occuring after application stop
 *     actions, like stuck threads. Defaults to false.</dd>
 *
 *   <dt>{@value #MARGIN_PROPERTY}</dt>
 *   <dd>The number of spaces beginning each trace element line. Defaults to
 *     {@value #DEFAULT_MARGIN} spaces.</dd>
 * </dl>
 *
 * <h1>Usage</h1>
 *
 * <pre>
 *      Profiler.start();
 *      try {
 *          // Section of code to be profiled . . .
 *      } finally {
 *          Profiler.stop();
 *      }
 * </pre>
 */
@ThreadSafe
public final class Profiler
    implements Runnable
{
    /**
     * Constructs an instance.
     */
    private Profiler()
    {
        _startMillis = _getProperty(START_MILLIS_PROPERTY, -1);

        if (_startMillis > 0) {
            _LOGGER
                .info(BaseMessages.START_MILLIS, String.valueOf(_startMillis));
        }

        _sampleMillis = _getProperty(SAMPLE_MILLIS_PROPERTY, -1);

        if (_sampleMillis > 0) {
            _LOGGER
                .info(
                    BaseMessages.SAMPLE_MILLIS,
                    String.valueOf(_sampleMillis));
        }

        _sampleCount = _getProperty(SAMPLE_COUNT_PROPERTY, Integer.MAX_VALUE);

        if (_sampleCount < Integer.MAX_VALUE) {
            _LOGGER
                .info(BaseMessages.SAMPLE_COUNT, String.valueOf(_sampleCount));
        }

        _snapshotMillis = _getProperty(SNAPSHOT_MILLIS_PROPERTY, -1);

        if (_snapshotMillis > 0) {
            _LOGGER
                .info(
                    BaseMessages.SNAPSHOT_MILLIS,
                    String.valueOf(_snapshotMillis));
        }

        _snapshotCount = _getProperty(
            SNAPSHOT_COUNT_PROPERTY,
            Integer.MAX_VALUE);

        if (_snapshotCount < Integer.MAX_VALUE) {
            _LOGGER
                .info(
                    BaseMessages.SNAPSHOT_COUNT,
                    String.valueOf(_snapshotCount));
        }

        final int snapshotDepth = _getProperty(
            SNAPSHOT_DEPTH_PROPERTY,
            Integer.MAX_VALUE);

        _snapshotDepth = (snapshotDepth > 0)? snapshotDepth: Integer.MAX_VALUE;

        if (_snapshotDepth < Integer.MAX_VALUE) {
            _LOGGER
                .info(
                    BaseMessages.SNAPSHOT_DEPTH,
                    String.valueOf(_snapshotDepth));
        }

        _threadGroup = _getProperty(THREAD_GROUP_PROPERTY);

        if (_threadGroup != null) {
            _LOGGER.info(BaseMessages.THREAD_GROUP, _threadGroup);
        }

        _threadState = _getProperty(THREAD_STATE_PROPERTY);

        if (_threadState != null) {
            _LOGGER.info(BaseMessages.THREAD_STATE, _threadState);
        }

        _stopIgnored = _getProperty(STOP_IGNORED_PROPERTY, false);

        if (_stopIgnored) {
            _LOGGER.info(BaseMessages.STOP_IGNORED);
        }
    }

    /**
     * Returns the profile margin.
     *
     * @return The profile margin.
     */
    @Nonnull
    @CheckReturnValue
    public static String margin()
    {
        String margin = _margin;

        if (margin == null) {
            final StringBuilder stringBuilder = new StringBuilder("\n");
            int spacesCount = _getProperty(MARGIN_PROPERTY, DEFAULT_MARGIN);

            while (spacesCount-- > 0) {
                stringBuilder.append(" ");
            }

            margin = stringBuilder.toString();
            _margin = margin;
        }

        return margin;
    }

    /**
     * Prints a stack trace.
     *
     * @param thread The traced thread.
     * @param stackTrace The stack trace.
     * @param skippedName The optional skipped name for the current thread.
     * @param stackDepth The stack depth.
     * @param printWriter The print writer.
     */
    public static synchronized void printStackTrace(
            @Nonnull final Thread thread,
            @Nonnull final StackTraceElement[] stackTrace,
            @Nonnull final Optional<String> skippedName,
            final int stackDepth,
            @Nonnull final PrintWriter printWriter)
    {
        final ThreadGroup group = thread.getThreadGroup();
        final StringBuilder stringBuilder = new StringBuilder();

        if (_traceThreadMessage == null) {
            _traceThreadMessage = new Message(BaseMessages.TRACE_THREAD);
            _traceElementMessage = new Message(BaseMessages.TRACE_ELEMENT);
        }

        stringBuilder
            .append(
                _traceThreadMessage
                    .format(
                            (Object) String.valueOf(thread.getId()),
                                    thread.getName(),
                                    (group != null)? group.getName(): null,
                                    thread.getState().name(),
                                    String.valueOf(thread.getPriority())));

        final int startIndex = (thread == Thread.currentThread())? Math
            .max(stackTraceStartIndex(stackTrace, skippedName), 0): 0;

        for (int i = startIndex; i < stackTrace.length; ++i) {
            stringBuilder.append(margin());

            if ((i - startIndex) >= stackDepth) {
                stringBuilder.append(BaseMessages.ELLIPSIS);

                break;
            }

            stringBuilder.append(_traceElementMessage.format(stackTrace[i]));
        }

        printWriter.println(stringBuilder);
    }

    /**
     * Starts the profiler
     */
    public static synchronized void start()
    {
        Require.success(_instance == null);

        if (_LOGGER.isDebugEnabled()) {
            _instance = new Profiler();

            if (_instance._areSamplesEnabled()) {
                final Thread samplesThread = new Thread(
                    _instance.new _Sampler(),
                    "Sampler");

                samplesThread
                    .setPriority(
                        _getProperty(
                            SAMPLE_PRIORITY_PROPERTY,
                            Thread.MAX_PRIORITY - 1));
                _instance._setSamplesThread(samplesThread);
            }

            if (_instance._areSnapshotsEnabled()
                    || _instance._areSamplesEnabled()) {
                final Thread thread = new Thread(_instance, "Profiler");

                thread
                    .setPriority(
                        _getProperty(
                            SNAPSHOT_PRIORITY_PROPERTY,
                            Thread.MAX_PRIORITY - 2));
                _instance._setSnapshotsThread(thread);
                _instance._getSnapshotsThread().start();
            }
        }
    }

    /**
     * Stops the profiler.
     */
    public static synchronized void stop()
    {
        if ((_instance != null) && !_instance._isStopIgnored()) {
            try {
                final Thread thread = _instance._getSnapshotsThread();

                if (thread != null) {
                    thread.interrupt();
                    thread.join();
                }

                final Thread samplesThread = _instance._getSamplesThread();

                if (samplesThread != null) {
                    samplesThread.interrupt();
                    samplesThread.join();
                }
            } catch (final InterruptedException exception) {
                throw new RuntimeException(exception);
            } finally {
                _instance._setSnapshotsThread(null);
                _instance._setSamplesThread(null);
                _instance = null;
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
    {
        try {
            if (_startMillis > 0) {
                Thread.sleep(_startMillis);

                if ((_snapshotCount > 0) && (_snapshotMillis <= 0)) {
                    _LOGGER.info(BaseMessages.SINGLE_SNAPSHOT);
                    _printStackTraces(
                        _LOGGER_PRINT_WRITER,
                        _getGroup(),
                        _getThreadState(),
                        _snapshotDepth,
                        this);
                }
            }

            if (_areSamplesEnabled()) {
                _samplesThread.start();
            }

            if ((_snapshotMillis > 0) && (_snapshotCount > 0)) {
                int snapshotCount = _snapshotCount;

                _LOGGER.info(BaseMessages.STARTING_SNAPSHOTS);

                try {
                    for (;;) {
                        _printStackTraces(
                            _LOGGER_PRINT_WRITER,
                            _getGroup(),
                            _getThreadState(),
                            _snapshotDepth,
                            this);

                        if (--snapshotCount <= 0) {
                            break;
                        }

                        Thread.sleep(_snapshotMillis);
                    }
                } finally {
                    _LOGGER.info(BaseMessages.STOPPED_SNAPSHOTS);
                }
            }
        } catch (final InterruptedException exception) {
            // Stops.
        }
    }

    /**
     * Gets the logger.
     *
     * @return The logger.
     */
    @Nonnull
    @CheckReturnValue
    static Logger _getLogger()
    {
        return _LOGGER;
    }

    /**
     * Returns the stack traces start index.
     *
     * @param stackTrace The stack trace.
     * @param skippedName The optional name to skip.
     *
     * @return The entry index following the skipped name (-1 if not found).
     */
    @CheckReturnValue
    static int stackTraceStartIndex(
            @Nonnull final StackTraceElement[] stackTrace,
            @Nonnull final Optional<String> skippedName)
    {
        if (skippedName.isPresent()) {
            boolean nameSeen = false;

            for (int i = 0; i < stackTrace.length; ++i) {
                if (skippedName.get().equals(stackTrace[i].getClassName())) {
                    nameSeen = true;
                } else if (nameSeen) {
                    return i;
                }
            }
        }

        return -1;
    }

    /**
     * Gets the thread group.
     *
     * @return The thread group.
     */
    String _getGroup()
    {
        return _threadGroup;
    }

    /**
     * Gets the sample count.
     *
     * @return The sample count.
     */
    int _getSampleCount()
    {
        return _sampleCount;
    }

    /**
     * Gets the sample millis.
     *
     * @return The sample millis.
     */
    int _getSampleMillis()
    {
        return _sampleMillis;
    }

    /**
     * Gets the samples thread.
     *
     * @return The samples thread.
     */
    Thread _getSamplesThread()
    {
        return _samplesThread;
    }

    /**
     * Gets the snapshots thread.
     *
     * @return The snapshots thread.
     */
    Thread _getSnapshotsThread()
    {
        return _snapshotsThread;
    }

    /**
     * Gets the thread state.
     *
     * @return The thread state.
     */
    String _getThreadState()
    {
        return _threadState;
    }

    private static String _getProperty(final String key)
    {
        return System.getProperty(key);
    }

    private static boolean _getProperty(
            final String key,
            final boolean defaultValue)
    {
        String value = _getProperty(key);

        if (value != null) {
            value = value.trim();

            if (value.isEmpty()) {
                return true;
            }

            if (ValueConverter.isTrue(value)) {
                return true;
            }

            if (ValueConverter.isFalse(value)) {
                return false;
            }
        }

        return defaultValue;
    }

    private static int _getProperty(final String key, final int defaultValue)
    {
        final String value = _getProperty(key);

        return (value != null)? Integer.parseInt(value): defaultValue;
    }

    private static void _printStackTraces(
            final PrintWriter printWriter,
            final String threadGroup,
            final String threadState,
            final int stackDepth,
            final Profiler profiler)
    {
        final Map<Thread, StackTraceElement[]> stackTraces;

        synchronized (Profiler.class) {
            stackTraces = Thread.getAllStackTraces();
        }

        for (final Map.Entry<Thread, StackTraceElement[]> entry:
                stackTraces.entrySet()) {
            final Thread thread = entry.getKey();

            if ((profiler == null)
                    || ((thread != profiler._getSnapshotsThread())
                        && (thread != profiler._getSamplesThread()))) {
                final ThreadGroup group = thread.getThreadGroup();

                if ((group != null)
                        && ((threadGroup == null)
                            || threadGroup.equalsIgnoreCase(
                                    group.getName()))
                        && ((threadState == null)
                        || threadState.equalsIgnoreCase(
                            thread.getState().name()))) {
                    printStackTrace(
                        thread,
                        entry.getValue(),
                        Optional.of(_CLASS_NAME),
                        stackDepth,
                        printWriter);
                }
            }
        }
    }

    private boolean _areSamplesEnabled()
    {
        return (_sampleCount > 0) && (_sampleMillis > 0);
    }

    private boolean _areSnapshotsEnabled()
    {
        return (_snapshotCount > 0)
               && ((_snapshotMillis > 0) || (_startMillis > 0));
    }

    private boolean _isStopIgnored()
    {
        return _stopIgnored;
    }

    private void _setSamplesThread(final Thread thread)
    {
        _samplesThread = thread;
    }

    private void _setSnapshotsThread(final Thread thread)
    {
        _snapshotsThread = thread;
    }

    /** Default margin. */
    public static final int DEFAULT_MARGIN = 13;

    /** Margin property. */
    public static final String MARGIN_PROPERTY = "rvpf.profile.margin";

    /** Sample count property. */
    public static final String SAMPLE_COUNT_PROPERTY =
        "rvpf.profile.sample.count";

    /** Sample millis property. */
    public static final String SAMPLE_MILLIS_PROPERTY =
        "rvpf.profile.sample.millis";

    /** Sample priority property. */
    public static final String SAMPLE_PRIORITY_PROPERTY =
        "rvpf.profile.sample.priority";

    /** Snapshot count property. */
    public static final String SNAPSHOT_COUNT_PROPERTY =
        "rvpf.profile.snapshot.count";

    /** Snapshot depth property. */
    public static final String SNAPSHOT_DEPTH_PROPERTY =
        "rvpf.profile.snapshot.depth";

    /** Snapshot millis property. */
    public static final String SNAPSHOT_MILLIS_PROPERTY =
        "rvpf.profile.snapshot.millis";

    /** Snapshot priority property. */
    public static final String SNAPSHOT_PRIORITY_PROPERTY =
        "rvpf.profile.snapshot.priority";

    /** Start millis property. */
    public static final String START_MILLIS_PROPERTY =
        "rvpf.profile.start.millis";

    /** Stop ignored property. */
    public static final String STOP_IGNORED_PROPERTY =
        "rvpf.profile.stop.ignored";

    /** Thread group property. */
    public static final String THREAD_GROUP_PROPERTY =
        "rvpf.profile.thread.group";

    /** Thread state property. */
    public static final String THREAD_STATE_PROPERTY =
        "rvpf.profile.thread.state";
    private static final String _CLASS_NAME = Profiler.class.getName();
    private static final Logger _LOGGER = Logger.getInstance(Profiler.class);
    private static final PrintWriter _LOGGER_PRINT_WRITER = _LOGGER
        .getPrintWriter(Logger.LogLevel.DEBUG);
    private static Profiler _instance;
    private static volatile String _margin;
    private static Message _traceElementMessage;
    private static Message _traceThreadMessage;

    private final int _sampleCount;
    private final int _sampleMillis;
    private volatile Thread _samplesThread;
    private final int _snapshotCount;
    private final int _snapshotDepth;
    private final int _snapshotMillis;
    private volatile Thread _snapshotsThread;
    private final int _startMillis;
    private final boolean _stopIgnored;
    private final String _threadGroup;
    private final String _threadState;

    /**
     * Counter.
     */
    private static class _Counter
    {
        /**
         * Constructs an instance.
         *
         * @param position The position in the sample.
         */
        _Counter(final int position)
        {
            _order = position;
        }

        /**
         * Gets the count.
         *
         * @return The count.
         */
        int getCount()
        {
            return _count;
        }

        /**
         * Gets the order.
         *
         * @return The order.
         */
        int getOrder()
        {
            return _order;
        }

        /**
         * Increments the count.
         */
        void increment()
        {
            ++_count;
        }

        private int _count = 1;
        private final int _order;
    }


    /**
     * Sampler.
     */
    private class _Sampler
        implements Runnable
    {
        /**
         * Constructs an instance.
         */
        _Sampler() {}

        /** {@inheritDoc}
         */
        @Override
        public void run()
        {
            _getLogger().info(BaseMessages.STARTING_SAMPLING);

            try {
                int sampleCount = _getSampleCount();

                do {
                    final Map<Thread, StackTraceElement[]> stackTraces;

                    synchronized (Profiler.class) {
                        stackTraces = Thread.getAllStackTraces();
                    }

                    for (final Map.Entry<Thread, StackTraceElement[]> entry:
                            stackTraces.entrySet()) {
                        final Thread thread = entry.getKey();

                        if ((thread == _getSamplesThread())
                                || (thread == _getSnapshotsThread())) {
                            continue;
                        }

                        if ((_getThreadState() != null)
                                && _getThreadState().equalsIgnoreCase(
                                    thread.getState().name())) {
                            continue;
                        }

                        final ThreadGroup group = thread.getThreadGroup();

                        if ((group == null)
                                || ((_getGroup() != null)
                                    && !_getGroup().equalsIgnoreCase(
                                            group.getName()))) {
                            continue;
                        }

                        Map<StackTraceElement, _Counter> traces = _traces
                            .get(thread);

                        if (traces == null) {
                            traces = new HashMap<StackTraceElement, _Counter>();
                            _traces.put(thread, traces);
                        }

                        for (final StackTraceElement traceElement:
                                entry.getValue()) {
                            final _Counter counter = traces.get(traceElement);

                            if (counter != null) {
                                counter.increment();
                            } else {
                                traces
                                    .put(
                                        traceElement,
                                        new _Counter(traces.size()));
                            }
                        }
                    }

                    Thread.sleep(_getSampleMillis());
                } while (--sampleCount > 0);
            } catch (final InterruptedException exception) {
                // Ends.
            }

            _getLogger().info(BaseMessages.STOPPED_SAMPLING);

            _dump();
        }

        private void _dump()
        {
            final Message threadMessage = new Message(
                BaseMessages.THREAD_SAMPLES);
            final Message traceMessage = new Message(BaseMessages.SAMPLE_AT);
            final Comparator<Map.Entry<StackTraceElement, _Counter>> traceElementComparator =
                new Comparator<Map.Entry<StackTraceElement, _Counter>>()
            {
                @Override
                public int compare(
                        final Map.Entry<StackTraceElement, _Counter> left,
                        final Map.Entry<StackTraceElement, _Counter> right)
                {
                    int comparison = right
                        .getValue()
                        .getCount() - left.getValue().getCount();

                    if (comparison == 0) {
                        comparison = left
                            .getValue()
                            .getOrder() - right.getValue().getOrder();
                    }

                    return comparison;
                }
            };

            for (final Map.Entry<Thread,
                    Map<StackTraceElement, _Counter>> threadTraces:
                    _traces.entrySet()) {
                final Thread thread = threadTraces.getKey();
                final String threadMessageText = threadMessage
                    .format(
                        (Object) String.valueOf(thread.getId()),
                        thread.getName());
                final List<Map.Entry<StackTraceElement, _Counter>> traces =
                    new ArrayList<Map.Entry<StackTraceElement, _Counter>>(
                        threadTraces.getValue().entrySet());
                final StringBuilder stringBuilder = new StringBuilder();

                stringBuilder.append(threadMessageText);

                traces.sort(traceElementComparator);

                for (final Map.Entry<StackTraceElement, _Counter> trace:
                        traces) {
                    final String traceMessageText = traceMessage
                        .format(
                            trace.getKey(),
                            String.valueOf(trace.getValue().getCount()));

                    stringBuilder.append(margin());
                    stringBuilder.append(traceMessageText);
                }

                _getLogger().debug(BaseMessages.VERBATIM, stringBuilder);
            }
        }

        private final Map<Thread, Map<StackTraceElement, _Counter>> _traces =
            new LinkedHashMap<Thread, Map<StackTraceElement, _Counter>>();
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
