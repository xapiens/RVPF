/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ProcessorServiceAppImpl.java 4080 2019-06-12 20:21:38Z SFB $
 */

package org.rvpf.processor;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import org.rvpf.base.ClassDef;
import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.PointRelation;
import org.rvpf.base.StatsOwner;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.MemoryLogger;
import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.store.Store;
import org.rvpf.base.store.StoreAccessException;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.sync.Sync;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.NormalizedValue;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.RecalcTrigger;
import org.rvpf.base.value.ResultValue;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.config.Config;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.EngineEntity;
import org.rvpf.metadata.entity.OriginEntity;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.metadata.entity.PointInput;
import org.rvpf.metadata.entity.TransformEntity;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.metadata.processor.BatchValuesQuery;
import org.rvpf.metadata.processor.Behavior;
import org.rvpf.metadata.processor.Engine;
import org.rvpf.metadata.processor.MemoryLimitException;
import org.rvpf.metadata.processor.Processor;
import org.rvpf.metadata.processor.Transform;
import org.rvpf.processor.behavior.PrimaryBehavior;
import org.rvpf.processor.behavior.RecalcTriggerBehavior;
import org.rvpf.processor.engine.control.ControlEngine;
import org.rvpf.processor.engine.control.ControlTransform;
import org.rvpf.processor.receptionist.Receptionist;
import org.rvpf.processor.receptionist.SOMReceptionist;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceStats;
import org.rvpf.service.ServiceThread;
import org.rvpf.service.metadata.MetadataService;
import org.rvpf.service.metadata.app.MetadataServiceAppImpl;

/**
 * Processor service application implementation.
 */
public final class ProcessorServiceAppImpl
    extends MetadataServiceAppImpl
    implements ServiceThread.Target, Processor
{
    /** {@inheritDoc}
     */
    @Override
    public ServiceStats createStats(final StatsOwner statsOwner)
    {
        return new ProcessorStats(statsOwner);
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<String> getEntityName()
    {
        return Optional.of(_entityName);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onNewMetadata(final Metadata metadata)
    {
        if (!metadata.validatePointsRelationships()) {
            return false;
        }

        return super.onNewMetadata(metadata);
    }

    /** {@inheritDoc}
     */
    @Override
    public void onServicesNotReady()
    {
        synchronized (_mutex) {
            _storesResetNeeded = true;
            _refreshControls = true;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<Collection<PointValue>> process(
            final Collection<PointValue> pointValues)
        throws InterruptedException, ServiceNotAvailableException
    {
        final BatchImpl batch = _batchControl.newBatch();

        try {
            batch.acceptNotices(pointValues);
        } catch (final MemoryLimitException exception) {
            if (_retrySmaller(batch.getNoticeValueCount() + 1, batch)) {
                return Optional.empty();
            }

            throw new ServiceNotAvailableException(exception);
        }

        if (getThisLogger().isDebugEnabled()) {
            getThisLogger()
                .debug(
                    ProcessorMessages.NOTICES_RECEIVED,
                    String.valueOf(batch.getNoticeValueCount()));
            getThisLogger()
                .debug(
                    ProcessorMessages.NOTICES_KEPT,
                    String.valueOf(batch.getNoticeValues().size()));
        }

        _doPendingActions();

        MemoryLogger.logMemoryInfo();

        try {
            final long processingStart = System.nanoTime();

            _setUpResults(batch);
            _prepareInputs(batch);
            _computeResults(batch);
            _stats
                .addBatch(
                    batch,
                    _receptionist.receptionTime(),
                    System.nanoTime() - processingStart);
        } catch (final MemoryLimitException exception) {
            final Collection<PointValue> notices = batch.getNoticeValues();

            if (_retrySmaller(notices.size(), batch)) {
                return Optional.empty();
            }

            for (final PointValue notice: notices) {
                getThisLogger().warn(ProcessorMessages.DROPPED_NOTICE, notice);
            }
        }

        final List<PointValue> updates = batch.getUpdates();

        batch.sendQueuedSignals();

        _batchControl.updateMemoryLimits(batch.getNoticeValueCount());

        return Optional.of(updates);
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
        throws InterruptedException
    {
        try {
            _receptionist.open();
            _doPendingActions();

            ServiceThread.ready();

            for (;;) {
                _doPendingActions();

                final Collection<PointValue> dueUpdates = _batchControl
                    .getDueUpdates();

                if (!dueUpdates.isEmpty()) {
                    _sendUpdates(dueUpdates);
                }

                final Collection<PointValue> notices = _receptionist
                    .fetchNotices(
                        _batchControl.getBatchLimit(),
                        _batchControl.getDueUpdateWait());

                if (notices.isEmpty()) {
                    continue;
                }

                getService().disableSuspend();

                try {
                    final Optional<Collection<PointValue>> updates = process(
                        notices);

                    if (updates.isPresent()) {
                        getService().setRestartEnabled(false);

                        try {
                            final long updateStart = System.nanoTime();

                            _sendUpdates(updates.get());
                            _receptionist.commit();
                            _batchControl.commit();
                            _stats
                                .addUpdates(
                                    updates.get().size(),
                                    System.nanoTime() - updateStart);
                        } finally {
                            getService().setRestartEnabled(true);
                        }
                    } else {
                        _batchControl.rollback();
                        _receptionist.rollback();
                    }
                } catch (final StoreAccessException exception) {
                    getThisLogger()
                        .warn(
                            exception,
                            ServiceMessages.STORE_ACCESS_FAILED,
                            exception.getStoreUUID().orElse(null),
                            exception.getMessage());

                    throw exception;
                } finally {
                    getService().enableSuspend();
                }

                System.gc();
                MemoryLogger.logMemoryInfo();
            }
        } catch (final ServiceNotAvailableException exception) {
            if (!_stopping) {
                if (exception.getCause() != null) {
                    getThisLogger()
                        .warn(
                            ServiceMessages.RESTART_NEEDED_,
                            exception.getCause());
                }

                getService().restart(true);
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final MetadataService service)
    {
        boolean success = super.setUp(service);

        // Reads the Metadata Document.

        final Config config = service.getConfig();
        final KeyedGroups processorProperties;
        final ProcessorMetadataFilter filter;

        if (success) {
            processorProperties = config
                .getPropertiesGroup(PROCESSOR_PROPERTIES);
            _entityName = processorProperties
                .getString(NAME_PROPERTY, Optional.of(DEFAULT_NAME))
                .get();
            getThisLogger().info(ServiceMessages.PROCESSOR_NAME, _entityName);
            filter = new ProcessorMetadataFilter(_entityName);
            success = loadMetadata(filter);
        } else {
            processorProperties = null;
            filter = null;
        }

        if (success) {
            Require.notNull(filter);

            _processor = filter.getProcessor().orElse(null);

            success = _processor != null;

            if (success) {
                service.setSourceUUID(_processor.getUUID().get());
            } else {
                getThisLogger()
                    .error(ServiceMessages.PROCESSOR_NOT_FOUND, _entityName);
            }
        }

        // Validates Relationships.

        Point cutoffControlPoint = null;
        Point filterControlPoint = null;
        int count = 0;

        if (success) {
            for (final Point point: getMetadata().getPointsCollection()) {
                final PointEntity pointEntity = (PointEntity) point;
                final Optional<TransformEntity> transformEntity = pointEntity
                    .getTransformEntity();

                if (transformEntity.isPresent()) {
                    final Optional<EngineEntity> engineEntity = transformEntity
                        .get()
                        .getEngineEntity();
                    final Optional<? extends Engine> engine = engineEntity
                        .get()
                        .getEngine();

                    if (engine.isPresent()
                            && (engine.get() instanceof ControlEngine)) {
                        final String action =
                            ((ControlTransform) transformEntity
                                .get()
                                .getTransform())
                                .getAction();

                        if (action == ControlTransform.UPDATE_CUTOFF_CONTROL_ACTION) {
                            if (cutoffControlPoint != null) {
                                getThisLogger()
                                    .warn(
                                        ProcessorMessages.CUTOFF_CONTROL_OVERRIDE,
                                        pointEntity,
                                        cutoffControlPoint);
                            }

                            cutoffControlPoint = pointEntity;
                        } else if (action
                                == ControlTransform.UPDATE_FILTER_CONTROL_ACTION) {
                            if (filterControlPoint != null) {
                                getThisLogger()
                                    .warn(
                                        ProcessorMessages.FILTER_CONTROL_OVERRIDE,
                                        pointEntity,
                                        filterControlPoint);
                            }

                            filterControlPoint = pointEntity;
                        }
                    }

                    ++count;
                } else if (pointEntity.getOriginEntity().isPresent()) {
                    getThisLogger()
                        .warn(
                            ProcessorMessages.NO_TRANSFORM_OR_INPUT,
                            pointEntity);
                }
            }
        }

        // Logs a Summary.

        if (success) {
            getThisLogger()
                .info(
                    ProcessorMessages.SET_UP_FOR_POINTS,
                    String.valueOf(count),
                    Integer.valueOf(count));
        }

        // Hires a Receptionist.

        if (success) {
            Require.notNull(processorProperties);

            final ClassDef classDef = processorProperties
                .getClassDef(
                    RECEPTIONIST_CLASS_PROPERTY,
                    DEFAULT_RECEPTIONIST_CLASS_DEF);

            _receptionist = classDef.createInstance(Receptionist.class);

            if (_receptionist != null) {
                success = _receptionist.setUp(getMetadata());
            } else {
                success = false;
            }
        }

        // Sets up cutoff Control.

        if (success) {
            if (cutoffControlPoint != null) {
                _cutoffControl = new CutoffControl(cutoffControlPoint);
                getThisLogger()
                    .info(
                        ProcessorMessages.CUTOFF_CONTROL_POINT,
                        cutoffControlPoint);
            }
        }

        // Cleans up the metadata and sets up the cache manager.

        CacheManager cacheManager = null;

        if (success) {
            getMetadata().cleanUp();
            cacheManager = new CacheManager();
            _stats = (ProcessorStats) service.getStats();
            success = cacheManager
                .setUp(
                    getMetadata(),
                    Optional.ofNullable(filterControlPoint),
                    _stats);
        }

        // Monitors stores.

        if (success) {
            service.monitorStores();
        }

        // Sets up the batch control.

        if (success) {
            _batchControl = new BatchControl();
            success = _batchControl
                .setUp(
                    getMetadata(),
                    Optional.ofNullable(_cutoffControl),
                    cacheManager,
                    config.getProperties());
        }

        return success;
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
    {
        final ServiceThread thread = new ServiceThread(this, "Processor");

        if (_thread.compareAndSet(null, thread)) {
            _refreshControls = true;

            getThisLogger()
                .debug(ServiceMessages.STARTING_THREAD, thread.getName());
            Require.ignored(thread.start(true));
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        final ServiceThread thread = _thread.getAndSet(null);

        if (thread != null) {
            final Receptionist receptionist = _receptionist;

            getThisLogger()
                .debug(ServiceMessages.STOPPING_THREAD, thread.getName());

            synchronized (receptionist) {
                _stopping = true;
                thread.interrupt();
                receptionist.close();
            }

            for (final EngineEntity engineEntity:
                    getService().getMetadata().getEngineEntities()) {
                final Optional<? extends Engine> engine = engineEntity
                    .getEngine();

                if (engine.isPresent()) {
                    engine.get().close();
                }
            }

            Require
                .ignored(
                    thread
                        .join(getThisLogger(), getService().getJoinTimeout()));
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        _cutoffControl = null;

        if (_receptionist != null) {
            _receptionist.tearDown();
        }

        if (_batchControl != null) {
            _batchControl.tearDown();
        }

        _processor = null;
    }

    private void _computeResults(
            final BatchImpl batch)
        throws InterruptedException, ServiceNotAvailableException
    {
        int droppedCount = 0;

        for (final ResultValue resultValue: batch.getResultValues()) {
            final Point resultPoint = resultValue.getPoint().get();
            final Optional<Transform> transform = ((PointEntity) resultPoint)
                .getTransform();
            boolean dropped = true;

            if (transform.isPresent()) {
                getThisLogger()
                    .trace(
                        ProcessorMessages.COMPUTING_VALUE,
                        resultValue.getPoint().get(),
                        resultValue.getStamp());

                final Optional<PointValue> optionalPointValue = transform
                    .get()
                    .applyTo(resultValue, batch);

                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                if (optionalPointValue.isPresent()) {
                    PointValue pointValue = optionalPointValue.get();

                    if (pointValue != Batch.DISABLED_UPDATE) {
                        if ((pointValue.getValue() == null)
                                && transform.get().isNullRemoves(
                                    pointValue.getPoint().get())) {
                            pointValue = new VersionedValue.Deleted(pointValue);
                        } else if (pointValue instanceof ResultValue) {
                            pointValue = new PointValue(pointValue);
                        }

                        batch.addUpdate(pointValue);
                    }

                    dropped = false;
                }
            }

            if (dropped) {
                ++droppedCount;
                getThisLogger()
                    .trace(
                        ProcessorMessages.UPDATE_CANCELLED,
                        resultValue.getPoint().orElse(null));
            }
        }

        if (droppedCount > 0) {
            getThisLogger()
                .debug(
                    ProcessorMessages.UPDATES_DROPPED,
                    String.valueOf(droppedCount));
        }

        batch.setDroppedResultValueCount(droppedCount);
    }

    private void _doPendingActions()
        throws InterruptedException, ServiceNotAvailableException
    {
        synchronized (_mutex) {
            if (_storesResetNeeded) {
                _receptionist.close();
                getService().resetPointsStore();
                _receptionist.open();
                _storesResetNeeded = false;
            }

            if (_refreshControls) {
                final CacheManager cacheManager = _batchControl
                    .getCacheManager();
                final CutoffControl cutoffControl = _cutoffControl;

                if (cutoffControl != null) {
                    final Point cutoffControlPoint = cutoffControl.getPoint();
                    final NormalizedValue controlValue = _getControlValue(
                        cutoffControlPoint);

                    cutoffControl.use(Optional.ofNullable(controlValue));
                }

                final Optional<Point> filterControlPoint = cacheManager
                    .getFilterControlPoint();

                if (filterControlPoint.isPresent()) {
                    final NormalizedValue controlValue = _getControlValue(
                        filterControlPoint.get());
                    final boolean updatesFiltered;

                    if ((controlValue != null)
                            && (controlValue.getValue() != null)) {
                        updatesFiltered = ((Boolean) controlValue.getValue())
                            .booleanValue();
                    } else {
                        updatesFiltered = false;
                    }

                    cacheManager.setUpdatesFiltered(updatesFiltered);
                }

                _refreshControls = false;
            }
        }
    }

    private NormalizedValue _getControlValue(
            final Point point)
        throws InterruptedException, ServiceNotAvailableException
    {
        final StoreValuesQuery.Builder storeQueryBuilder = StoreValuesQuery
            .newBuilder()
            .setPoint(point);
        final StoreValues response;
        final Optional<PointValue> pointValue;

        storeQueryBuilder.setNotNull(true);

        try {
            response = storeQueryBuilder.build().getResponse();
        } catch (final StoreAccessException exception) {
            throw new ServiceNotAvailableException(exception);
        }

        if (response.isSuccess()) {
            pointValue = response.getPointValue();
        } else {
            getThisLogger()
                .warn(
                    ServiceMessages.STORE_REJECTED_QUERY_ON,
                    point,
                    response.getException().get().getMessage());
            pointValue = Optional.empty();
        }

        return pointValue.isPresent()? pointValue.get().normalized(): null;
    }

    private void _prepareInputs(
            final BatchImpl batch)
        throws MemoryLimitException, InterruptedException, StoreAccessException
    {
        final Collection<ResultValue> resultValues = batch.getResultValues();
        boolean lookUpDone = false;

        // Lets the behaviors query the appropriate input values.

        batch.resetLookUpPass();

        while (!lookUpDone) {
            lookUpDone = true;

            for (final ResultValue resultValue: resultValues) {
                final Point resultPoint = resultValue.getPoint().get();
                final List<? extends PointRelation> inputs = resultPoint
                    .getInputs();

                for (final PointRelation input: inputs) {
                    final Behavior behavior = ((PointInput) input)
                        .getPrimaryBehavior()
                        .get();

                    lookUpDone &= behavior.prepareSelect(resultValue, batch);
                }
            }

            _processQueries(batch);
        }

        // Lets the behaviors set up the inputs.

        int droppedCount = 0;
        int preparedCount = 0;

        resultSetUp: for (final Iterator<ResultValue> i =
                resultValues.iterator();
                i.hasNext(); ) {
            final ResultValue resultValue = i.next();
            final Point resultPoint = resultValue.getPoint().get();
            final List<? extends PointRelation> inputs = resultPoint
                .getInputs();

            for (final PointRelation input: inputs) {
                final Behavior behavior = ((PointInput) input)
                    .getPrimaryBehavior()
                    .get();

                if (!behavior.select(resultValue, batch)) {
                    i.remove();
                    ++droppedCount;

                    continue resultSetUp;
                }
            }

            preparedCount += resultValue.getInputValues().size();
        }

        if (droppedCount > 0) {
            getThisLogger()
                .debug(
                    ProcessorMessages.COMPUTATIONS_DROPPED,
                    String.valueOf(droppedCount));
        }

        if (preparedCount > 0) {
            getThisLogger()
                .debug(
                    ProcessorMessages.INPUTS_PREPARED,
                    String.valueOf(preparedCount));
        }

        // Fetches unfetched results.

        for (final ResultValue resultValue: resultValues) {
            if (resultValue.isFetched()) {
                continue;
            }

            final Point resultPoint = resultValue.getPoint().get();
            final Transform transform = ((PointEntity) resultPoint)
                .getTransform()
                .get();

            if (transform.usesFetchedResult()) {
                final BatchValuesQuery.Builder batchValuesQueryBuilder =
                    BatchValuesQuery
                        .newBuilder()
                        .setPoint(resultValue.getPoint());

                batchValuesQueryBuilder.setAt(resultValue.getStamp());

                if (batch
                    .getPointValue(batchValuesQueryBuilder.build())
                    .isAbsent()) {
                    final StoreValuesQuery.Builder storeQueryBuilder =
                        StoreValuesQuery
                            .newBuilder()
                            .setPoint(resultValue.getPoint().get());

                    storeQueryBuilder.setAt(resultValue.getStamp());
                    batch.addStoreValuesQuery(storeQueryBuilder.build());
                }
            }
        }

        _processQueries(batch);

        // Logs a summary.

        getThisLogger()
            .debug(
                ProcessorMessages.RESULTS_READY,
                Integer.valueOf(batch.getResultValueCount()));
    }

    private void _processQueries(
            final BatchImpl batch)
        throws MemoryLimitException, InterruptedException, StoreAccessException
    {
        final int storeCount = batch.processStoreQueries();

        if (getThisLogger().isDebugEnabled()) {
            final int count = batch.getStoreQueryCount();
            final int sent = batch.getStoreQuerySentCount();

            if (sent < count) {
                getThisLogger()
                    .debug(
                        ProcessorMessages.DUPLICATE_QUERIES_IGNORED,
                        String.valueOf(count - sent));
            }

            if (sent > 0) {
                getThisLogger()
                    .debug(
                        ProcessorMessages.QUERIES_SENT,
                        Integer.valueOf(sent),
                        Integer.valueOf(storeCount));
                getThisLogger()
                    .debug(
                        ProcessorMessages.GOT_VALUES,
                        Integer.valueOf(batch.getReceivedValueCount()));
            }
        }
    }

    private boolean _retrySmaller(final int than, final BatchImpl batch)
    {
        MemoryLogger.logMemoryInfo();
        getThisLogger().info(ProcessorMessages.INSUFFICIENT_MEMORY);

        batch.clear();

        if (than <= 1) {
            return false;
        }

        final int batchLimit = than / 2;

        getThisLogger()
            .info(
                ProcessorMessages.WILL_RETRY_SMALLER,
                Integer.valueOf(batchLimit));

        _batchControl.setBatchLimit(batchLimit);

        System.gc();
        MemoryLogger.logMemoryInfo();

        return true;
    }

    private void _sendUpdates(
            final Collection<PointValue> updates)
        throws StoreAccessException
    {
        final Collection<Store> stores = new TreeSet<>();

        for (final PointValue update: updates) {
            final Point point = update.getPoint().get();
            final Store store = point.getStore().get();

            store.addUpdate(update);
            stores.add(store);
        }

        for (final Store store: stores) {
            final PointValue[] storeUpdates = store
                .getUpdates()
                .toArray(new PointValue[store.getUpdateCount()]);

            if (!store.sendUpdates() && !store.getExceptions().isPresent()) {
                throw new StoreAccessException(
                    Optional.of(store.getUUID()),
                    new ServiceClosedException());
            }

            final Optional<Exception[]> optionalExceptions = store
                .getExceptions();

            if (!optionalExceptions.isPresent()) {
                throw new StoreAccessException(
                    Optional.of(store.getUUID()),
                    new ServiceClosedException());
            }

            final Exception[] exceptions = optionalExceptions.get();

            for (int i = 0; i < exceptions.length; ++i) {
                if (exceptions[i] == null) {
                    _batchControl.traceUpdate(storeUpdates[i]);
                }
            }
        }

        if (getThisLogger().isDebugEnabled() && (updates.size() > 0)) {
            getThisLogger()
                .debug(
                    ProcessorMessages.UPDATES_SENT,
                    String.valueOf(updates.size()),
                    Integer.valueOf(updates.size()),
                    String.valueOf(stores.size()),
                    Integer.valueOf(stores.size()));
        }
    }

    private void _setUpResults(
            final BatchImpl batch)
        throws MemoryLimitException, InterruptedException, StoreAccessException
    {
        boolean lookUpDone = false;
        int protectedCount = 0;

        // Allows the behaviors to query values.

        batch.resetLookUpPass();

        while (!lookUpDone) {
            lookUpDone = true;

            for (final PointValue noticeValue: batch.getNoticeValues()) {
                if (!(noticeValue instanceof RecalcTrigger)) {
                    final Point noticePoint = noticeValue.getPoint().get();

                    for (final PointRelation result: noticePoint.getResults()) {
                        if (!((PointInput) result)
                            .getPrimaryBehavior()
                            .get()
                            .prepareTrigger(noticeValue, batch)) {
                            lookUpDone = false;
                        }
                    }
                }
            }

            _processQueries(batch);
        }

        // Asks the behaviors to set up the results.

        for (final PointValue noticeValue: batch.getNoticeValues()) {
            if (noticeValue instanceof RecalcTrigger) {
                batch
                    .setUpResultValue(
                        noticeValue.getStamp(),
                        new RecalcTriggerBehavior((RecalcTrigger) noticeValue));
            } else {
                final Point noticePoint = noticeValue.getPoint().get();

                for (final PointRelation result: noticePoint.getResults()) {
                    final Optional<Behavior> behavior = ((PointInput) result)
                        .getPrimaryBehavior();
                    final Sync sync;

                    if (behavior.orElse(null) instanceof PrimaryBehavior) {
                        sync = ((PrimaryBehavior) behavior.get())
                            .getSync()
                            .orElse(null);
                    } else {
                        sync = null;
                    }

                    if ((sync == null)
                            || sync.isInSync(noticeValue.getStamp())) {
                        behavior.get().trigger(noticeValue, batch);
                    }
                }
            }
        }

        // Respects the 'RecalcLatest' param.

        if (!batch.getRecalcLatestResults().isEmpty()) {
            for (final ResultValue result: batch.getRecalcLatestResults()) {
                final PointEntity point = (PointEntity) result.getPoint().get();
                final int recalcLatest = point.getRecalcLatest();
                final StoreValuesQuery.Builder storeQueryBuilder =
                    StoreValuesQuery
                        .newBuilder()
                        .setPoint(point);

                if (recalcLatest == 0) {
                    storeQueryBuilder.setNotBefore(result.getStamp());
                } else {
                    storeQueryBuilder.setAfter(result.getStamp());

                    if (recalcLatest > 1) {
                        storeQueryBuilder.setRows(recalcLatest);
                    }
                }

                batch.addStoreValuesQuery(storeQueryBuilder.build());
            }

            _processQueries(batch);

            for (final ResultValue result: batch.getRecalcLatestResults()) {
                final PointEntity point = (PointEntity) result.getPoint().get();
                final boolean drop;
                int recalcLatest = point.getRecalcLatest();
                DateTime stamp = result.getStamp();
                final BatchValuesQuery.Builder batchValuesQueryBuilder =
                    BatchValuesQuery
                        .newBuilder()
                        .setPoint(Optional.of(point));

                if (recalcLatest == 0) {
                    batchValuesQueryBuilder.setAt(stamp);
                    drop = batch
                        .getPointValue(batchValuesQueryBuilder.build())
                        .isPresent();
                } else {
                    do {
                        batchValuesQueryBuilder.setAfter(stamp);

                        final PointValue value = batch
                            .getPointValue(batchValuesQueryBuilder.build());

                        if (value.isAbsent()) {
                            break;
                        }

                        stamp = value.getStamp();
                    } while (--recalcLatest > 0);

                    drop = recalcLatest == 0;
                }

                if (drop) {
                    batch.dropResultValue(result);
                    ++protectedCount;
                }
            }

            batch.getRecalcLatestResults().clear();
        }

        // Logs a summary.

        if (getThisLogger().isDebugEnabled()) {
            if (batch.getCutoffResultCount() > 0) {
                getThisLogger()
                    .debug(
                        ProcessorMessages.CUTOFF_DROP,
                        String.valueOf(batch.getCutoffResultCount()));
            }

            if (protectedCount > 0) {
                batch.setDroppedResultValueCount(protectedCount);
                getThisLogger()
                    .debug(
                        ProcessorMessages.PROTECTED_DROP,
                        String.valueOf(protectedCount));
            }

            getThisLogger()
                .debug(
                    ProcessorMessages.RESULTS_SET_UP,
                    String.valueOf(batch.getResultValueCount()));
        }

        batch.freezeResultsMap();
    }

    /** Default processor name. */
    public static final String DEFAULT_NAME = "Processor";

    /** Name property. */
    public static final String NAME_PROPERTY = "name";

    /** Processor properties. */
    public static final String PROCESSOR_PROPERTIES = "processor";

    /** Default receptionist class. */
    public static final ClassDef DEFAULT_RECEPTIONIST_CLASS_DEF =
        new ClassDefImpl(
            SOMReceptionist.class);

    /** Used to specify an alternative to the SOM receptionist. */
    public static final String RECEPTIONIST_CLASS_PROPERTY =
        "receptionist.class";

    private BatchControl _batchControl;
    private volatile CutoffControl _cutoffControl;
    private String _entityName;
    private final Object _mutex = new Object();
    private OriginEntity _processor;
    private Receptionist _receptionist;
    private volatile boolean _refreshControls;
    private ProcessorStats _stats;
    private volatile boolean _stopping;
    private volatile boolean _storesResetNeeded;
    private final AtomicReference<ServiceThread> _thread =
        new AtomicReference<>();
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
