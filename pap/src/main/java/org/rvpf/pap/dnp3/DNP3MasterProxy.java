/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DNP3MasterProxy.java 4082 2019-06-14 18:38:55Z SFB $
 */

package org.rvpf.pap.dnp3;

import java.io.Serializable;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.Origin;
import org.rvpf.base.Point;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.Tuple;
import org.rvpf.pap.PAPMessages;
import org.rvpf.pap.PAPProxy;
import org.rvpf.pap.dnp3.object.ObjectHeader;
import org.rvpf.pap.dnp3.object.ObjectInstance;
import org.rvpf.pap.dnp3.object.ObjectRange;
import org.rvpf.pap.dnp3.object.ObjectVariation;
import org.rvpf.pap.dnp3.object.PointType;
import org.rvpf.pap.dnp3.object.content.InternalIndication;
import org.rvpf.pap.dnp3.object.groupCategory.devices
    .InternalIndicationsVariation;
import org.rvpf.pap.dnp3.transport.ApplicationMessage;
import org.rvpf.pap.dnp3.transport.Fragment;
import org.rvpf.pap.dnp3.transport.FunctionCode;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;

/**
 * DNP3 master proxy.
 */
public final class DNP3MasterProxy
    extends DNP3Proxy
    implements ServiceThread.Target
{
    /**
     * Constructs an instance.
     *
     * @param context The context.
     * @param origin The origin representing the proxy.
     */
    public DNP3MasterProxy(
            @Nonnull final DNP3Context context,
            @Nonnull final Origin origin)
    {
        super(context, origin);
    }

    /**
     * Constructs an instance.
     *
     * @param context The context.
     * @param name The name for the synthesized origin.
     */
    public DNP3MasterProxy(
            @Nonnull final DNP3Context context,
            @Nonnull final String name)
    {
        super(context, name);
    }

    private DNP3MasterProxy(@Nonnull final DNP3MasterProxy other)
    {
        super(other);
    }

    /** {@inheritDoc}
     */
    @Override
    public PAPProxy copy()
    {
        return new DNP3MasterProxy(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isMaster()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
        throws Exception
    {
        getThisLogger()
            .debug(PAPMessages.STARTED_SERVICES, getOrigin().getName().get());

        try {
            for (;;) {
                final Fragment request = _outstation.nextRequest();

                switch (request.getFunctionCode()) {
                    case READ: {
                        _read(request);

                        break;
                    }
                    case WRITE: {
                        _write(request);

                        break;
                    }
                    case DIRECT_OPERATE: {
                        _directOperate(request);

                        break;
                    }
                    case CONFIRM: {
                        _confirm(request);

                        break;
                    }
                    case DISABLE_UNSOLICITED: {
                        _sendNullResponse(request);

                        break;
                    }
                    case RECORD_CURRENT_TIME: {
                        _outstation.recordTime();
                        _sendNullResponse(request);

                        break;
                    }
                    default: {
                        getThisLogger()
                            .warn(
                                DNP3Messages.UNSUPPORTED_REQUEST,
                                request.getFunctionCode());

                        break;
                    }
                }
            }
        } catch (final InterruptedException exception) {
            getThisLogger()
                .debug(
                    PAPMessages.STOPPED_SERVICES,
                    getOrigin().getName().get());
        }
    }

    /**
     * Starts.
     *
     * @param outstation The DNP3 outstation.
     * @param responseSender A response sender.
     */
    public void start(
            @Nonnull final DNP3Outstation outstation,
            @Nonnull final Consumer<ApplicationMessage> responseSender)
    {
        final ServiceThread thread = new ServiceThread(
            this,
            "DNP3 outstation (proxy " + getOrigin() + ")");

        if (_thread.compareAndSet(null, thread)) {
            _responder = responseSender;
            _outstation = outstation;

            getThisLogger()
                .debug(ServiceMessages.STARTING_THREAD, thread.getName());
            thread.start();
        }
    }

    /**
     * Stops.
     */
    public void stop()
    {
        final ServiceThread thread = _thread.getAndSet(null);

        if (thread != null) {
            getThisLogger()
                .debug(ServiceMessages.STOPPING_THREAD, thread.getName());
            Require.ignored(thread.interruptAndJoin(getThisLogger(), 0));

            _outstation = null;
            _responder = null;
        }
    }

    private static void _confirm(@Nonnull final Fragment request)
    {
        request.getAssociation().onConfirm();
    }

    private void _directOperate(@Nonnull final Fragment receivedRequest)
    {
        _write(receivedRequest);
    }

    private void _read(@Nonnull final Fragment request)
    {
        final ApplicationMessage response = new ApplicationMessage(
            request.getAssociation(),
            Optional.of(FunctionCode.RESPONSE),
            false);

        response.setSequence(request.getSequence());

        for (final Fragment.Item item: request.getItems()) {
            final Optional<PointType> pointType = item.getPointType();

            if (!pointType.isPresent()) {
                continue;
            }

            final Fragment.Item.Indexes indexes = item.getIndexes();

            if (!indexes.isStartStop()) {
                continue;
            }

            final ObjectRange objectRange = ObjectRange
                .newIndexInstance(indexes.getStart(), indexes.getStop());
            final Optional<Point> point = _outstation
                .getPoint(pointType.get(), objectRange);

            if (!point.isPresent()) {
                continue;
            }

            final Optional<PointValue> pointValue = _outstation
                .getPointValue(point.get());

            if (!pointValue.isPresent()) {
                continue;
            }

            final ObjectVariation requestObjectVariation = item
                .getObjectVariation();
            final Optional<ObjectVariation> optionalObjectVariation;

            if (requestObjectVariation.isAny()) {
                final DNP3StationPoint stationPoint = _outstation
                    .getStationPoint(point.get());

                optionalObjectVariation = stationPoint
                    .getInputVariation(stationPoint.getDataType());
            } else {
                optionalObjectVariation = Optional.of(requestObjectVariation);
            }

            if (!optionalObjectVariation.isPresent()) {
                continue;
            }

            final ObjectVariation objectVariation = optionalObjectVariation
                .get();

            if (!ObjectInstance.WithValue.class
                .isAssignableFrom(objectVariation.getObjectClass())) {
                continue;
            }

            final Serializable value = pointValue.get().getValue();

            if (value == null) {
                continue;
            }

            final ObjectInstance[] objectInstances =
                new ObjectInstance[indexes.getLength()];

            if (value instanceof Tuple) {
                final Tuple tuple = (Tuple) value;

                if (tuple.size() != objectInstances.length) {
                    continue;
                }

                for (int i = 0; i < objectInstances.length; ++i) {
                    final ObjectInstance.WithValue objectInstanceWithValue =
                        (ObjectInstance.WithValue) objectVariation
                            .newObjectInstance();
                    final int index = indexes.next().intValue();

                    objectInstanceWithValue.setUp(index, index);
                    objectInstanceWithValue.setValue(tuple.get(i));
                    objectInstances[i] = objectInstanceWithValue;
                }
            } else {
                if (objectInstances.length != 1) {
                    continue;
                }

                final ObjectInstance.WithValue objectInstanceWithValue =
                    (ObjectInstance.WithValue) objectVariation
                        .newObjectInstance();
                final int index = indexes.getStart();

                objectInstanceWithValue.setUp(index, index);
                objectInstanceWithValue.setValue(value);
                objectInstances[0] = objectInstanceWithValue;
            }

            final ObjectHeader objectHeader = ObjectHeader
                .newInstance(objectVariation, objectRange);

            response
                .add(
                    new Fragment.Item(
                        objectHeader,
                        Optional.of(objectInstances)));

            getThisLogger()
                .trace(DNP3Messages.SENT_POINT_VALUE, pointValue.get());

        }

        _responder.accept(response);
    }

    private void _sendNullResponse(@Nonnull final Fragment request)
    {
        final ApplicationMessage response = new ApplicationMessage(
            request.getAssociation(),
            Optional.of(FunctionCode.RESPONSE),
            false);

        response.setSequence(request.getSequence());
        _responder.accept(response);
    }

    private void _write(@Nonnull final Fragment request)
    {
        for (final Fragment.Item item: request.getItems()) {
            final Class<?> objectVariationClass = item
                .getObjectVariation()
                .getObjectClass();

            if (item.getObjectVariation()
                    == InternalIndicationsVariation.PACKED_FORMAT) {
                final ObjectInstance.Packed iinObject =
                    (ObjectInstance.Packed) item
                        .getObjectInstances()
                        .get()[0];
                final Fragment.Item.Indexes indexes = item.getIndexes();

                for (int i = 0; i < indexes.getLength(); ++i) {
                    final int index = indexes.next().intValue();
                    final boolean set = (iinObject.get(i) & (1 << i)) != 0;

                    _outstation
                        .setInternalIndication(
                            InternalIndication.instance(index),
                            set);
                }

                continue;
            }

            if (!ObjectInstance.WithValue.class
                .isAssignableFrom(objectVariationClass)) {
                if (ObjectInstance.WithTime.class
                    .isAssignableFrom(objectVariationClass)) {
                    final ObjectInstance[] objectInstances = item
                        .getObjectInstances()
                        .get();

                    if (objectInstances.length == 1) {
                        _outstation
                            .setTime(
                                ((ObjectInstance.WithTime) objectInstances[0])
                                    .getTime());
                    }
                }

                continue;
            }

            final Optional<PointType> pointType = item.getPointType();

            if (!pointType.isPresent()) {
                continue;
            }

            final Fragment.Item.Indexes indexes = item.getIndexes();

            if (!indexes.isStartStop()) {
                continue;
            }

            final ObjectRange objectRange = ObjectRange
                .newIndexInstance(indexes.getStart(), indexes.getStop());
            final Optional<Point> point = _outstation
                .getPoint(pointType.get(), objectRange);

            if (!point.isPresent()) {
                continue;
            }

            final ObjectInstance[] objectInstances = item
                .getObjectInstances()
                .get();

            if (objectInstances.length != indexes.getLength()) {
                continue;
            }

            final Serializable value;

            if (objectRange.isMultiple()) {
                final Tuple tuple = new Tuple(objectInstances.length);

                for (int i = 0; i < objectInstances.length; ++i) {
                    tuple
                        .add(
                            ((ObjectInstance.WithValue) objectInstances[i])
                                .getValue());
                }

                value = tuple;
            } else {
                value = ((ObjectInstance.WithValue) objectInstances[0])
                    .getValue();
            }

            final PointValue pointValue = new PointValue(
                point.get(),
                Optional.of(DateTime.now()),
                null,
                value);

            getThisLogger()
                .trace(DNP3Messages.RECEIVED_POINT_VALUE, pointValue);

            _outstation.putPointValue(pointValue);
        }

        _sendNullResponse(request);
    }

    private DNP3Outstation _outstation;
    private Consumer<ApplicationMessage> _responder;
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
