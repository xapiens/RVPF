/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Scanner.java 4087 2019-06-16 18:12:18Z SFB $
 */

package org.rvpf.service.pap.datalogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Origin;
import org.rvpf.base.Point;
import org.rvpf.base.PointRelation;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.sync.NullSync;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.Traces;
import org.rvpf.base.util.Schedule;
import org.rvpf.base.util.SnoozeAlarm;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.util.container.KeyedValues;
import org.rvpf.base.util.container.PointValueFilters;
import org.rvpf.base.value.PointValue;
import org.rvpf.config.entity.ClassDefEntity;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.OriginEntity;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.metadata.entity.SyncEntity;
import org.rvpf.pap.PAPClient;
import org.rvpf.pap.PAPConnectionListener;
import org.rvpf.pap.PAPContext;
import org.rvpf.pap.PAPMessages;
import org.rvpf.pap.PAPProxy;
import org.rvpf.pap.PAPSupport;
import org.rvpf.processor.engine.pap.PAPSplitter;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;

/**
 * Scanner.
 *
 * <p>A datalogger scanner collects data from configured sources.</p>
 *
 * <p>A different thread is used for each source.</p>
 */
public final class Scanner
    implements PAPConnectionListener
{
    /**
     * Constructs an instance.
     *
     * @param dataloggerApp The calling datalogger app.
     * @param name The name of this scanner.
     * @param origin The origin representing this scanner.
     * @param retryLimit The retry limit.
     * @param retryInterval The retry interval.
     * @param controlsBuilder The scan controls builder.
     * @param proxyThreadBuilders The proxy thread builders.
     * @param statePoints The state points.
     * @param traces The traces.
     */
    Scanner(
            @Nonnull final DataloggerAppImpl dataloggerApp,
            @Nonnull final String name,
            @Nonnull final Origin origin,
            final int retryLimit,
            @Nonnull final Optional<ElapsedTime> retryInterval,
            @Nonnull final ScanControls.Builder controlsBuilder,
            @Nonnull final Map<PAPProxy,
            _ProxyThread.Builder> proxyThreadBuilders,
            @Nonnull final Set<Point> statePoints,
            @Nonnull final Traces traces)
    {
        _dataloggerApp = dataloggerApp;
        _name = name;
        _retryLimit = retryLimit;
        _retryInterval = retryInterval;
        _controls = controlsBuilder.build();

        for (final Map.Entry<PAPProxy, _ProxyThread.Builder> proxyThreadBuilder:
                proxyThreadBuilders.entrySet()) {
            _proxyThreads
                .put(
                    proxyThreadBuilder.getKey(),
                    proxyThreadBuilder.getValue().setScanner(this).build());
        }

        _statePoints = statePoints;
        _traces = traces;
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

    /** {@inheritDoc}
     */
    @Override
    public boolean onLostConnection(
            final PAPProxy remoteProxy,
            final Optional<Exception> cause)
    {
        _updateState(
            remoteProxy,
            Boolean.FALSE,
            cause.map(Exception::getMessage));

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onNewConnection(final PAPProxy remoteProxy)
    {
        _updateState(remoteProxy, Boolean.TRUE, Optional.empty());

        return true;
    }

    /**
     * Gets the retry interval.
     *
     * @return The retry interval.
     */
    @Nonnull
    @CheckReturnValue
    ElapsedTime _getRetryInterval()
    {
        return _retryInterval.get();
    }

    /**
     * Gets the retry limit.
     *
     * @return The retry limit.
     */
    @CheckReturnValue
    int _getRetryLimit()
    {
        return _retryLimit;
    }

    /**
     * Gets the traces.
     *
     * @return The traces.
     */
    @Nonnull
    @CheckReturnValue
    Traces _getTraces()
    {
        return _traces;
    }

    /**
     * Gets the value filters.
     *
     * @return The value filters.
     */
    @Nonnull
    @CheckReturnValue
    PointValueFilters _getValueFilters()
    {
        return _valueFilters;
    }

    /**
     * Asks if the barrier is open.
     *
     * @return True if the barrier is open.
     */
    @CheckReturnValue
    boolean _isBarrierOpen()
    {
        return _controls.isBarrierOpen();
    }

    /**
     * Limits updates.
     *
     * @throws InterruptedException When interrupted.
     */
    void _limitUpdates()
        throws InterruptedException
    {
        _dataloggerApp.limitUpdates();
    }

    /**
     * Sends updates.
     *
     * @param pointValues The updates.
     */
    void _sendUpdates(@Nonnull final Collection<PointValue> pointValues)
    {
        _dataloggerApp.sendUpdates(pointValues);
    }

    /**
     * Storage monitor check.
     *
     * @return True unless on alert.
     */
    @CheckReturnValue
    boolean _storageMonitorCheck()
    {
        return _dataloggerApp.storageMonitorCheck();
    }

    /**
     * Gets the name.
     *
     * @return The name.
     */
    @Nonnull
    @CheckReturnValue
    final String getName()
    {
        return _name;
    }

    /**
     * Starts.
     */
    final void start()
    {
        // Sets the connection states as undefined.

        final DateTime startStamp = DateTime.now();
        final List<PointValue> connectionStates = new ArrayList<>(
            _statePoints.size());

        for (final Point statePoint: _statePoints) {
            connectionStates
                .add(
                    new PointValue(
                        statePoint,
                        Optional.of(startStamp),
                        null,
                        null));
        }

        _dataloggerApp.sendUpdates(connectionStates);

        if (!_proxyThreads.isEmpty()) {
            if (!_controls.start()) {
                _dataloggerApp.fail();
            }

            for (final _ProxyThread thread: _proxyThreads.values()) {
                thread.start();
            }
        }
    }

    /**
     * Stops.
     *
     * @param joinTimeout The join timeout.
     */
    final void stop(final long joinTimeout)
    {
        _controls.stop(joinTimeout);

        for (final _ProxyThread thread: _proxyThreads.values()) {
            thread.stop(joinTimeout);
        }

        _traces.tearDown();
    }

    private void _updateState(
            final PAPProxy remoteProxy,
            final Boolean connected,
            final Optional<String> cause)
    {
        final Optional<Point> proxyStatePoint = remoteProxy
            .getConnectionStatePoint();

        if (!proxyStatePoint.isPresent()) {
            return;
        }

        final DateTime now = DateTime.now();
        final Collection<PointValue> connectionStates = new ArrayList<>(
            _statePoints.size());

        for (final PointRelation resultRelation:
                proxyStatePoint.get().getResults()) {
            final Point resultPoint = resultRelation.getResultPoint();

            if (_statePoints.contains(resultPoint)) {
                connectionStates
                    .add(
                        new PointValue(
                            resultPoint,
                            Optional.of(now),
                            cause.orElse(null),
                            connected));
            }
        }

        _dataloggerApp.sendUpdates(connectionStates);
    }

    private final ScanControls _controls;
    private final DataloggerAppImpl _dataloggerApp;
    private final String _name;
    private final Map<PAPProxy, _ProxyThread> _proxyThreads =
        new IdentityHashMap<>();
    private final Optional<ElapsedTime> _retryInterval;
    private final int _retryLimit;
    private final Set<Point> _statePoints;
    private final Traces _traces;
    private final PointValueFilters _valueFilters = new PointValueFilters();

    /**
     * Scanner builder.
     */
    static final class Builder
    {
        /**
         * Constructs an instance.
         */
        Builder() {}

        /**
         * Applies properties.
         *
         * @param scannerProperties The scanner properties.
         *
         * @return This (null on failure).
         */
        @Nullable
        @CheckReturnValue
        Builder applyProperties(@Nonnull final KeyedGroups scannerProperties)
        {
            // Gets the origin associated with this scanner.

            _name = scannerProperties.getString(NAME_PROPERTY).orElse(null);

            if (_name == null) {
                _LOGGER
                    .error(
                        BaseMessages.MISSING_PROPERTY_IN,
                        NAME_PROPERTY,
                        scannerProperties.getName().orElse(null));

                return null;
            }

            _LOGGER.debug(PAPMessages.DATALOGGER_SCANNER_NAME, _name);

            final Metadata metadata = _dataloggerApp.getMetadata();
            final Optional<OriginEntity> origin = metadata
                .getOriginEntity(Optional.of(_name));

            if (!origin.isPresent()) {
                _LOGGER.error(ServiceMessages.PROCESSOR_NOT_FOUND, _name);

                return null;
            }

            _origin = origin.get();

            // Gets the retry properties.

            _retryLimit = scannerProperties
                .getInt(RETRY_LIMIT_PROPERTY, DEFAULT_RETRY_LIMIT);

            if (_retryLimit == 0) {
                _LOGGER.debug(PAPMessages.SCANNER_NO_RETRIES, _name);
            } else {
                _retryInterval = scannerProperties
                    .getElapsed(
                        RETRY_INTERVAL_PROPERTY,
                        Optional.of(DEFAULT_RETRY_INTERVAL),
                        Optional.of(DEFAULT_RETRY_INTERVAL));
                _LOGGER
                    .debug(
                        PAPMessages.SCANNER_RETRY_INTERVAL,
                        _name,
                        _retryInterval.get());

                if (!SnoozeAlarm
                    .validate(
                        _retryInterval.get(),
                        this,
                        PAPMessages.SCANNER_RETRY_INTERVAL_TEXT)) {
                    return null;
                }

                if (_retryLimit > 0) {
                    _LOGGER
                        .debug(
                            PAPMessages.SCANNER_RETRY_LIMIT,
                            Integer.valueOf(_retryLimit));
                }
            }

            // Gets the overriding sync entity.

            final Optional<String> scannerSyncName = scannerProperties
                .getString(SYNC_PROPERTY);
            final SyncEntity scannerSyncEntity;

            if (scannerSyncName.isPresent()) {
                final Optional<SyncEntity> optionalScannerSyncEntity = metadata
                    .getSyncEntity(Optional.of(scannerSyncName.get()));

                if (!optionalScannerSyncEntity.isPresent()) {
                    _LOGGER
                        .error(
                            ServiceMessages.ENTITY_UNKNOWN,
                            SyncEntity.ELEMENT_NAME,
                            scannerSyncName.get());

                    return null;
                }

                scannerSyncEntity = optionalScannerSyncEntity.get();

                if (!scannerSyncEntity.setUp()) {
                    return null;
                }

                _LOGGER
                    .debug(
                        PAPMessages.DATALOGGER_SCANNER_SYNC,
                        _name,
                        scannerSyncEntity.getSync());
            } else {
                scannerSyncEntity = null;
            }

            // Sets up the control points.

            if (!_setUpControlPoints(
                    scannerProperties.getGroup(CONTROLS_PROPERTIES))) {
                return null;
            }

            // Sets up the schedule.

            final Set<Point> inputPoints = new LinkedHashSet<>();
            final Set<Point> resultPoints = new HashSet<>();

            if (!_setUpSchedules(
                    scannerSyncEntity,
                    inputPoints,
                    resultPoints)) {
                return null;
            }

            // Logs a summary.

            if (_proxyThreadBuilders.isEmpty()) {
                _LOGGER.warn(PAPMessages.NO_POINTS_TO_SCAN, _name);
            } else {
                final int originated = resultPoints.size();
                final Collection<DateTime> firstStamps = new ArrayList<>(
                    _proxyThreadBuilders.size());
                int scheduled = 0;

                for (final _ProxyThread.Builder proxyThreadBuilder:
                        _proxyThreadBuilders.values()) {
                    final ScanSchedule schedule = proxyThreadBuilder
                        .getSchedule();

                    firstStamps.add(schedule.peek().get().getStamp());
                    scheduled += schedule.size();
                }

                _LOGGER
                    .debug(
                        PAPMessages.SCANNER_ORIGINATES,
                        _name,
                        String.valueOf(originated),
                        Integer.valueOf(originated));
                _LOGGER
                    .debug(
                        PAPMessages.POINTS_TO_SCAN,
                        _name,
                        String.valueOf(scheduled),
                        Integer.valueOf(scheduled));

                if (_scanControlsBuilder.isBarrierOpen()) {
                    _LOGGER
                        .debug(
                            PAPMessages.SCAN_STARTS_AT,
                            _name,
                            DateTime.min(firstStamps));
                }
            }

            // Locates the results of the connection state points.

            for (final PAPProxy proxy: _proxyThreadBuilders.keySet()) {
                final Optional<Point> proxyStatePoint = proxy
                    .getConnectionStatePoint();

                if (proxyStatePoint.isPresent()) {
                    for (final PointRelation resultRelation:
                            proxyStatePoint.get().getResults()) {
                        final Point resultPoint = resultRelation
                            .getResultPoint();

                        if (resultPoints.contains(resultPoint)) {
                            _statePoints.add(resultPoint);
                        }
                    }
                }
            }

            // Sets up the traces.

            if (!_traces
                .setUp(
                    _dataloggerApp.getDataDir(),
                    metadata.getProperties().getGroup(Traces.TRACES_PROPERTIES),
                    metadata.getService().getSourceUUID(),
                    scannerProperties.getString(TRACES_PROPERTY))) {
                return null;
            }

            return this;
        }

        /**
         * Builds a scanner.
         *
         * @return The scanner.
         */
        @Nonnull
        @CheckReturnValue
        Scanner build()
        {
            return new Scanner(
                _dataloggerApp,
                _name,
                _origin,
                _retryLimit,
                _retryInterval,
                _scanControlsBuilder,
                _proxyThreadBuilders,
                _statePoints,
                _traces);
        }

        /**
         * Sets the datalogger app.
         *
         * @param dataloggerApp The datalogger app.
         *
         * @return This.
         */
        @Nonnull
        Builder setDataloggerApp(@Nonnull final DataloggerAppImpl dataloggerApp)
        {
            _dataloggerApp = dataloggerApp;

            return this;
        }

        private boolean _setUpControlPoint(
                final Optional<String> controlPointName,
                final Consumer<Point> setControlPoint)
        {
            if (controlPointName.isPresent()) {
                final Optional<Point> optionalControlPoint = _dataloggerApp
                    .getMetadata()
                    .getPoint(controlPointName.get());

                if (!optionalControlPoint.isPresent()) {
                    _LOGGER
                        .warn(
                            PAPMessages.CONTROL_POINT_UNKNOWN,
                            controlPointName.get());

                    return false;
                }

                setControlPoint.accept(optionalControlPoint.get());
            }

            return true;
        }

        private boolean _setUpControlPoints(
                final KeyedValues controlsProperties)
        {
            _scanControlsBuilder = ScanControls
                .newBuilder()
                .setScannerName(_name);

            if (!_setUpControlPoint(
                    controlsProperties.getString(BARRIER_PROPERTY),
                    _scanControlsBuilder::setBarrierPoint)) {
                return false;
            }

            if (!_setUpControlPoint(
                    controlsProperties.getString(EACH_PROPERTY),
                    _scanControlsBuilder::setEachPoint)) {
                return false;
            }

            if (!_setUpControlPoint(
                    controlsProperties.getString(CRONTAB_PROPERTY),
                    _scanControlsBuilder::setCrontabPoint)) {
                return false;
            }

            if (!_setUpControlPoint(
                    controlsProperties.getString(ELAPSED_PROPERTY),
                    _scanControlsBuilder::setElapsedPoint)) {
                return false;
            }

            return true;
        }

        private boolean _setUpSchedules(
                final SyncEntity scannerSyncEntity,
                final Set<Point> inputPoints,
                final Set<Point> resultPoints)
        {
            final Metadata metadata = _dataloggerApp.getMetadata();
            final DateTime startStamp = DateTime.now();

            for (final Point resultPoint: metadata.getPointsCollection()) {
                if (resultPoint.getOrigin().orElse(null) != _origin) {
                    continue;    // Skips when not produced by this.
                }

                final List<? extends PointRelation> pointRelations = resultPoint
                    .getInputs();

                if (pointRelations.size() != 1) {
                    _LOGGER.warn(PAPMessages.POINT_EQ_1_INPUT, resultPoint);

                    continue;
                }

                final Point inputPoint = pointRelations.get(0).getInputPoint();
                final PAPContext protocolContext = _dataloggerApp
                    .getProtocolContext(inputPoint, Optional.of(_traces));

                if (protocolContext == null) {
                    return false;
                }

                if (protocolContext.isPointActive(resultPoint)
                        && protocolContext.isPointActive(inputPoint)
                        && inputPoints.add(inputPoint)) {
                    resultPoints.add(resultPoint);

                    final PAPProxy remoteProxy = protocolContext
                        .getRemoteProxy(inputPoint)
                        .get();

                    if (inputPoint
                            == remoteProxy.getConnectionStatePoint().orElse(
                                null)) {
                        continue;    // Not scheduled.
                    }

                    SyncEntity syncEntity = scannerSyncEntity;

                    if (syncEntity == null) {    // Uses the point sync.
                        syncEntity = ((PointEntity) inputPoint)
                            .getSyncEntity()
                            .orElse(null);
                    }

                    if (syncEntity == null) {    // Needs trigger by controls.
                        if (_scanControlsBuilder.mayTrigger()) {
                            syncEntity = SyncEntity
                                .newBuilder()
                                .setClassDef(
                                    ClassDefEntity
                                        .newBuilder()
                                        .setImpl(
                                                new ClassDefImpl(
                                                        NullSync.class))
                                        .build())
                                .build();

                            if (!syncEntity.setUp()) {
                                throw new InternalError();
                            }
                        } else {
                            _LOGGER
                                .warn(
                                    PAPMessages.NO_SCHEDULE_FOR_POINT,
                                    inputPoint);

                            continue;
                        }
                    }

                    _ProxyThread.Builder proxyThreadBuilder =
                        _proxyThreadBuilders
                            .get(remoteProxy);

                    if (proxyThreadBuilder == null) {
                        // Prepares a thread for the first point for the proxy.
                        proxyThreadBuilder = _ProxyThread
                            .newBuilder()
                            .setProtocolContext(protocolContext)
                            .setOrigin(inputPoint.getOrigin());
                        _proxyThreadBuilders
                            .put(remoteProxy, proxyThreadBuilder);
                        _scanControlsBuilder
                            .addSchedule(proxyThreadBuilder.getSchedule());
                    }

                    if (!proxyThreadBuilder.getSplitter().setUp(inputPoint)) {
                        return false;
                    }

                    // Schedules the next event for the point.
                    proxyThreadBuilder
                        .getSchedule()
                        .add(
                            new Schedule.PointEvent(
                                Optional.of(inputPoint),
                                startStamp,
                                syncEntity.getSync()));
                }
            }

            return true;
        }

        /** Barrier property. */
        public static final String BARRIER_PROPERTY = "barrier";

        /** Controls properties. */
        public static final String CONTROLS_PROPERTIES = "controls";

        /** Crontab property. */
        public static final String CRONTAB_PROPERTY = "crontab";

        /** Default retry interval. */
        public static final ElapsedTime DEFAULT_RETRY_INTERVAL =
            ElapsedTime.MINUTE;

        /** Default retry interval. */
        public static final int DEFAULT_RETRY_LIMIT = -1;

        /** Each property. */
        public static final String EACH_PROPERTY = "each";

        /** Elapsed property. */
        public static final String ELAPSED_PROPERTY = "elapsed";

        /** Name property. */
        public static final String NAME_PROPERTY = "name";

        /** Retry interval property. */
        public static final String RETRY_INTERVAL_PROPERTY = "retry.interval";

        /** Retry limit property. */
        public static final String RETRY_LIMIT_PROPERTY = "retry.limit";

        /** Sync property. */
        public static final String SYNC_PROPERTY = "sync";

        /** Traces subdirectory property. */
        public static final String TRACES_PROPERTY = "traces";

        /**  */

        private static final Logger _LOGGER = Logger.getInstance(Builder.class);

        private DataloggerAppImpl _dataloggerApp;
        private String _name;
        private Origin _origin;
        private final Map<PAPProxy, _ProxyThread.Builder> _proxyThreadBuilders =
            new IdentityHashMap<>();
        private Optional<ElapsedTime> _retryInterval = Optional.empty();
        private int _retryLimit;
        private ScanControls.Builder _scanControlsBuilder;
        private final Set<Point> _statePoints = new LinkedHashSet<>();
        private final Traces _traces = new Traces();
    }


    /**
     * Proxy thread.
     */
    private static final class _ProxyThread
        implements ServiceThread.Target
    {
        /**
         * Constructs an instance.
         *
         * @param scanner The scanner owning this.
         * @param protocolContext The protocol context.
         * @param schedule The scan schedule.
         * @param origin The optional origin.
         * @param splitter The splitter.
         */
        _ProxyThread(
                @Nonnull final Scanner scanner,
                @Nonnull final PAPContext protocolContext,
                @Nonnull final ScanSchedule schedule,
                @Nonnull final Optional<? extends Origin> origin,
                @Nonnull final PAPSplitter splitter)
        {
            _scanner = scanner;

            final PAPSupport protocolSupport = protocolContext.getSupport();

            _client = protocolSupport.newClient(protocolContext);
            _splitter = splitter;
            _schedule = schedule;
            _scanThread = new ServiceThread(
                this,
                "Datalogger scanner '" + _scanner.getName() + "' from '"
                + origin.orElse(
                    null) + "'");
        }

        /** {@inheritDoc}
         */
        @Override
        public void run()
            throws Exception
        {
            _client.open();

            try {
                final int retryLimit = _scanner._getRetryLimit();
                final SnoozeAlarm snoozeAlarm = (retryLimit != 0)
                    ? new SnoozeAlarm(): null;

                for (;;) {
                    _scanner._limitUpdates();

                    final Schedule.PointEvent[] eventsDue = _schedule
                        .allDue(true, true)
                        .toArray(new Schedule.PointEvent[0]);

                    if (eventsDue.length == 0) {
                        _LOGGER
                            .warn(
                                PAPMessages.NO_POINTS_TO_SCAN,
                                _scanner.getName());

                        break;
                    }

                    if (!(_scanner._isBarrierOpen()
                            && _scanner._storageMonitorCheck())) {
                        continue;
                    }

                    _LOGGER
                        .trace(
                            PAPMessages.SCANNING_NOW,
                            String.valueOf(eventsDue.length));

                    final Point[] pointsDue = new Point[eventsDue.length];

                    for (int i = 0; i < pointsDue.length; ++i) {
                        pointsDue[i] = eventsDue[i].getPoint().get();
                    }

                    final PointValue[] requestedValues;

                    try {
                        requestedValues = _client.fetchPointValues(pointsDue);
                    } catch (final ServiceNotAvailableException exception) {
                        if ((retryLimit >= 0) && (_retries >= retryLimit)) {
                            throw exception;
                        }

                        if (_retries++ == 0) {
                            _LOGGER
                                .trace(
                                    exception,
                                    BaseMessages.VERBATIM,
                                    exception.getMessage());
                            _LOGGER.info(PAPMessages.BEGINNING_SCANNER_RETRIES);
                        }

                        Require
                            .ignored(
                                snoozeAlarm
                                    .snooze(_scanner._getRetryInterval()));

                        continue;
                    }

                    if (_retries > 0) {
                        _LOGGER.info(PAPMessages.SCANNER_RETRY_SUCCEEDED);
                        _retries = 0;
                    }

                    final Collection<PointValue> inputValues = new ArrayList<>(
                        requestedValues.length);

                    for (int i = 0; i < requestedValues.length; ++i) {
                        if (requestedValues[i] != null) {
                            inputValues.add(requestedValues[i]);
                        } else {
                            final Schedule.PointEvent pointEvent = eventsDue[i];

                            _schedule.cancel(pointEvent);
                            _LOGGER.warn(PAPMessages.CANCELLED_, pointEvent);
                        }
                    }

                    _process(inputValues);
                }
            } finally {
                _client.close();
            }
        }

        /**
         * Starts.
         */
        public final void start()
        {
            _client.addConnectionListener(_scanner);

            _LOGGER
                .debug(ServiceMessages.STARTING_THREAD, _scanThread.getName());
            _scanThread.start();
        }

        /**
         * Stops.
         *
         * @param joinTimeout The join timeout.
         */
        public final void stop(final long joinTimeout)
        {
            _client.close();

            Require.ignored(_scanThread.interruptAndJoin(_LOGGER, joinTimeout));

            _client.removeConnectionListener(_scanner);
        }

        /**
         * Returns a new builder.
         *
         * @return The new builder.
         */
        @Nonnull
        @CheckReturnValue
        static Builder newBuilder()
        {
            return new Builder();
        }

        /**
         * Gets the schedule.
         *
         * @return The schedule.
         */
        @Nonnull
        @CheckReturnValue
        ScanSchedule getSchedule()
        {
            return _schedule;
        }

        /**
         * Gets the splitter.
         *
         * @return The splitter.
         */
        @Nonnull
        @CheckReturnValue
        PAPSplitter getSplitter()
        {
            return _splitter;
        }

        private Collection<PointValue> _filter(
                final Collection<PointValue> inputPointValues)
        {
            final Collection<PointValue> outputPointValues = new ArrayList<>(
                inputPointValues.size());
            final PointValueFilters valueFilters = _scanner._getValueFilters();

            for (final PointValue inputPointValue: inputPointValues) {
                for (final PointValue filteredPointValue:
                        valueFilters.filter(inputPointValue)) {
                    outputPointValues.add(filteredPointValue);
                }
            }

            return outputPointValues;
        }

        private void _process(final Collection<PointValue> inputValues)
        {
            final Traces traces = _scanner._getTraces();

            if (traces.isEnabled()) {
                for (final PointValue pointValue: inputValues) {
                    traces.add(pointValue);
                }

                traces.commit();
            }

            final Collection<PointValue> filteredValues = _filter(inputValues);
            final Collection<PointValue> outputValues = new ArrayList<>(
                filteredValues.size());

            for (final PointValue pointValue: filteredValues) {
                final Collection<PointValue> splittedValues = _split(
                    pointValue);

                if (splittedValues != null) {
                    outputValues.addAll(_filter(splittedValues));
                } else {
                    outputValues.add(pointValue);
                }
            }

            final Collection<PointValue> updates = new ArrayList<>(
                outputValues.size());

            for (final PointValue pointValue: outputValues) {
                if (_client.isPointActive(pointValue.getPoint().get())) {
                    updates.add(pointValue.encoded());
                }
            }

            _scanner._sendUpdates(updates);
        }

        private Collection<PointValue> _split(final PointValue inputPointValue)
        {
            final List<PointValue> outputPointValues = new LinkedList<>();
            final Optional<PAPSplitter.Splitted> splitted = _splitter
                .split(inputPointValue);

            if (!splitted.isPresent()) {
                return null;
            }

            final Point splittedPoint = inputPointValue.getPoint().get();

            for (final PointRelation result: splittedPoint.getResults()) {
                final Point resultPoint = result.getResultPoint();

                outputPointValues
                    .add(
                        new PointValue(
                            resultPoint,
                            Optional.of(inputPointValue.getStamp()),
                            null,
                            splitted.get().get(resultPoint).orElse(null)));
            }

            return outputPointValues;
        }

        private static final Logger _LOGGER = Logger.getInstance(Scanner.class);

        private final PAPClient _client;
        private int _retries;
        private final ServiceThread _scanThread;
        private final Scanner _scanner;
        private final ScanSchedule _schedule;
        private final PAPSplitter _splitter;

        /**
         * Proxy thread builder.
         */
        static final class Builder
        {
            /**
             * Constructs an instance.
             */
            Builder() {}

            /**
             * Builds a proxy thread.
             *
             * @return The proxy thread.
             */
            @Nonnull
            _ProxyThread build()
            {
                final _ProxyThread proxyThread = new _ProxyThread(
                    _scanner,
                    _protocolContext,
                    getSchedule(),
                    _origin,
                    getSplitter());

                return proxyThread;
            }

            /**
             * Gets the schedule.
             *
             * @return The schedule.
             */
            @Nonnull
            @CheckReturnValue
            ScanSchedule getSchedule()
            {
                return _schedule;
            }

            /**
             * Gets the splitter.
             *
             * @return The splitter.
             */
            @Nonnull
            @CheckReturnValue
            PAPSplitter getSplitter()
            {
                return Require.notNull(_splitter);
            }

            /**
             * Sets the origin.
             *
             * @param origin The optional origin.
             *
             * @return This.
             */
            @Nonnull
            Builder setOrigin(@Nonnull final Optional<? extends Origin> origin)
            {
                _origin = origin;

                return this;
            }

            /**
             * Sets the protocol context.
             *
             * @param protocolContext The protocol context.
             *
             * @return This.
             */
            @Nonnull
            Builder setProtocolContext(
                    @Nonnull final PAPContext protocolContext)
            {
                _protocolContext = protocolContext;

                final PAPSupport protocolSupport = _protocolContext
                    .getSupport();

                _splitter = protocolSupport.newSplitter();

                return this;
            }

            /**
             * Sets the scanner.
             *
             * @param scanner The scanner.
             *
             * @return this.
             */
            @Nonnull
            Builder setScanner(@Nonnull final Scanner scanner)
            {
                _scanner = scanner;

                return this;
            }

            private Optional<? extends Origin> _origin = Optional.empty();
            private PAPContext _protocolContext;
            private Scanner _scanner;
            private final ScanSchedule _schedule = new ScanSchedule();
            private PAPSplitter _splitter;
        }
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
