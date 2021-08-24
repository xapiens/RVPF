/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ClockServiceAppImpl.java 4080 2019-06-12 20:21:38Z SFB $
 */

package org.rvpf.clock;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Point;
import org.rvpf.base.alert.Event;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.sync.CrontabSync;
import org.rvpf.base.sync.Sync;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.Traces;
import org.rvpf.base.util.Schedule;
import org.rvpf.base.value.NormalizedValue;
import org.rvpf.base.value.PointValue;
import org.rvpf.config.Config;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.OriginEntity;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.service.Service;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;
import org.rvpf.service.metadata.MetadataService;
import org.rvpf.service.metadata.app.MetadataServiceAppImpl;

/**
 * Clock service application implementation.
 */
public final class ClockServiceAppImpl
    extends MetadataServiceAppImpl
    implements ServiceThread.Target
{
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
    public boolean onEvent(final Event event)
    {
        if (Service.WATCHDOG_EVENT.equalsIgnoreCase(event.getName())) {
            synchronized (_schedule) {
                _schedule.notifyAll();
            }
        }

        return super.onEvent(event);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onNewMetadata(final Metadata metadata)
    {
        int count = 0;

        for (final Point point: metadata.getPointsCollection()) {
            if (!((PointEntity) point).setUp(metadata)) {
                return false;
            }

            if (!(point.getContent().orElse(null) instanceof ClockContent)) {
                getThisLogger()
                    .error(ServiceMessages.CLOCK_CONTENT_CLASS, point);

                return false;
            }

            if (!point.getSync().isPresent()) {
                getThisLogger()
                    .error(ServiceMessages.CLOCK_POINT_NEEDS_SYNC, point);

                return false;
            }

            ++count;
        }

        getThisLogger()
            .info(
                ServiceMessages.CLOCK_SET_UP,
                String.valueOf(count),
                Integer.valueOf(count));

        metadata.cleanUp();

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void onServicesNotReady()
    {
        synchronized (_schedule) {
            _schedule.clear();
            _reconnectNeeded = true;
            _schedule.notifyAll();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
        throws InterruptedException
    {
        try {
            synchronized (_schedule) {
                for (;;) {
                    if (_schedule.isEmpty()) {
                        if (_reconnectNeeded) {
                            getService().resetPointsStore();
                            _reconnectNeeded = false;
                        }

                        _setUpEvents();
                    }

                    final long delay = _processEvents();

                    _traces.commit();
                    _schedule.wait(delay);
                }
            }
        } catch (final ServiceClosedException exception) {
            // Ends the thread.
        } catch (final ServiceNotAvailableException exception) {
            onServiceNotAvailableException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final MetadataService service)
    {
        if (!super.setUp(service)) {
            return false;
        }

        final Config config = service.getConfig();

        _entityName = config
            .getStringValue(NAME_PROPERTY, Optional.of(DEFAULT_NAME))
            .orElse(null);
        getThisLogger().info(ServiceMessages.ORIGIN, _entityName);

        final ClockMetadataFilter filter = new ClockMetadataFilter(_entityName);

        if (!loadMetadata(filter)) {
            return false;
        }

        final Optional<OriginEntity> origin = filter.getOriginEntity();

        if (!origin.isPresent()) {
            getThisLogger()
                .error(ServiceMessages.ORIGIN_NOT_FOUND, _entityName);

            return false;
        }

        _origin = origin.get();

        service.setSourceUUID(_origin.getUUID().get());

        service.monitorStores();

        _midnightEnabled = config.getBooleanValue(MIDNIGHT_ENABLED_PROPERTY);

        if (_midnightEnabled) {
            getThisLogger().info(ServiceMessages.MIDNIGHT_EVENT_ENABLED);
        }

        return _traces
            .setUp(
                getDataDir(),
                getConfigProperties().getGroup(Traces.TRACES_PROPERTIES),
                getSourceUUID(),
                Optional.of(GENERATED_TRACES));
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
    {
        final ServiceThread thread = new ServiceThread(this, "Clock service");

        if (_thread.compareAndSet(null, thread)) {
            getThisLogger()
                .debug(ServiceMessages.STARTING_THREAD, thread.getName());
            thread.start();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        final ServiceThread thread = _thread.getAndSet(null);

        if (thread != null) {
            getThisLogger()
                .debug(ServiceMessages.STOPPING_THREAD, thread.getName());
            Require
                .ignored(
                    thread.interruptAndJoin(getThisLogger(), getJoinTimeout()));

            for (final Point point: getMetadata().getPointsCollection()) {
                ((PointEntity) point).close();
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        _traces.tearDown();
        _schedule.clear();

        super.tearDown();
    }

    private long _processEvents()
        throws InterruptedException, ServiceNotAvailableException
    {
        long delay;

        for (;;) {
            delay = _schedule.getDelay();

            if (delay > 0) {
                break;
            }

            final Schedule.PointEvent event = _schedule.next();
            final Optional<Point> point = event.getPoint();
            final DateTime stamp = event.getStamp();

            if (point.isPresent()) {
                final PointValue pointValue = new NormalizedValue(
                    point.get(),
                    Optional.of(stamp),
                    null,
                    Long.valueOf(stamp.toMillis()))
                    .encoded();

                if (pointValue.updateStore()) {
                    _traces.add(pointValue);
                }
            } else {
                getService()
                    .sendEvent(Service.MIDNIGHT_EVENT, Optional.empty());
            }

            _schedule.advance();
        }

        return delay;
    }

    private void _setUpEvent(
            final Point point)
        throws InterruptedException, ServiceNotAvailableException
    {
        final StoreValuesQuery.Builder queryBuilder = StoreValuesQuery
            .newBuilder()
            .setPoint(point);
        final StoreValues response;

        response = queryBuilder.build().getResponse();

        if (response.isSuccess()) {
            final Sync pointSync = point.getSync().get();
            final Optional<PointValue> pointValue = response.getPointValue();
            final Schedule.PointEvent event;
            final DateTime stamp;

            if (!pointValue.isPresent()
                    || (pointValue.get().getValue() == null)) {
                stamp = DateTime.now().floored(ElapsedTime.MINUTE);
                event = new Schedule.PointEvent(
                    Optional.of(point),
                    stamp,
                    pointSync);
                getThisLogger()
                    .info(
                        ServiceMessages.CLOCK_POINT_NEW,
                        point,
                        event.getStamp());
            } else {
                stamp = DateTime
                    .fromMillis(
                        ((Long) pointValue.get().normalized().getValue())
                            .longValue());
                event = new Schedule.PointEvent(
                    Optional.of(point),
                    pointSync.getNextStamp(stamp).get(),
                    pointSync);
            }

            _schedule.add(event);
        } else {
            getThisLogger()
                .warn(
                    ServiceMessages.STORE_REJECTED_QUERY_ON,
                    point,
                    response.getException().get().getMessage());
        }
    }

    private void _setUpEvents()
        throws InterruptedException, ServiceNotAvailableException
    {
        if (_midnightEnabled) {
            _schedule
                .add(
                    new Schedule.PointEvent(
                        Optional.empty(),
                        DateTime.now(),
                        new CrontabSync("0 0 * * *")));
        }

        for (final Point point: getMetadata().getPointsCollection()) {
            _setUpEvent(point);
        }

        if (_schedule.isEmpty()) {
            _schedule
                .add(
                    new Schedule.PointEvent(
                        Optional.empty(),
                        DateTime.END_OF_TIME,
                        new CrontabSync("0 0 * * *")));
        }
    }

    /** Name of the default 'Origin'. */
    public static final String DEFAULT_NAME = "Clock";

    /** Traces subdirectory. */
    public static final String GENERATED_TRACES = "generated";

    /** Enables the generation of 'Midnight' events. */
    public static final String MIDNIGHT_ENABLED_PROPERTY =
        "clock.midnight.enabled";

    /**
     * Identifies the 'Origin' entry in the Metadata associated with the
     * service instance.
     */
    public static final String NAME_PROPERTY = "clock.name";

    private String _entityName;
    private boolean _midnightEnabled;
    private OriginEntity _origin;
    private boolean _reconnectNeeded;
    private final Schedule<Schedule.PointEvent> _schedule = new Schedule<>(
        false);
    private final AtomicReference<ServiceThread> _thread =
        new AtomicReference<>();
    private final Traces _traces = new Traces();
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
