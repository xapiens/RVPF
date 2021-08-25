/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DNP3Master.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.pap.dnp3;

import java.io.IOException;
import java.io.Serializable;

import java.net.ConnectException;

import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.DateTime;
import org.rvpf.base.Origin;
import org.rvpf.base.Point;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.IdentityHashSet;
import org.rvpf.base.util.container.KeyedValues;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.Tuple;
import org.rvpf.pap.ListenerManager;
import org.rvpf.pap.PAPClient;
import org.rvpf.pap.PAPMessages;
import org.rvpf.pap.PAPProxy;
import org.rvpf.pap.dnp3.object.ObjectEventListener;
import org.rvpf.pap.dnp3.object.ObjectHeader;
import org.rvpf.pap.dnp3.object.ObjectInstance;
import org.rvpf.pap.dnp3.object.ObjectRange;
import org.rvpf.pap.dnp3.object.ObjectVariation;
import org.rvpf.pap.dnp3.object.PointType;
import org.rvpf.pap.dnp3.object.content.InternalIndication;
import org.rvpf.pap.dnp3.object.groupCategory.classes.ClassObjectsVariation;
import org.rvpf.pap.dnp3.object.groupCategory.devices
    .InternalIndicationsVariation;
import org.rvpf.pap.dnp3.object.groupCategory.times.TimeDateVariation;
import org.rvpf.pap.dnp3.transport.Association;
import org.rvpf.pap.dnp3.transport.ConnectionManager;
import org.rvpf.pap.dnp3.transport.Fragment;
import org.rvpf.pap.dnp3.transport.FunctionCode;
import org.rvpf.pap.dnp3.transport.MasterOutstationAssociation;
import org.rvpf.pap.dnp3.transport.ReadTransaction;
import org.rvpf.pap.dnp3.transport.ReceivedFragmentListener;
import org.rvpf.pap.dnp3.transport.RemoteEndPoint;
import org.rvpf.pap.dnp3.transport.WriteTransaction;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;
import org.rvpf.service.ServiceThread.Target;

/**
 * DNP3 master.
 */
public final class DNP3Master
    extends PAPClient.Abstract
    implements ReceivedFragmentListener
{
    /**
     * Constructs an instance.
     *
     * @param masterContext The DNP3 master context.
     * @param localDeviceAddress The local device address.
     */
    public DNP3Master(
            @Nonnull final DNP3MasterContext masterContext,
            final short localDeviceAddress)
    {
        super(masterContext);

        _localDeviceAddress = localDeviceAddress;
        _connectionManager = new ConnectionManager(masterContext);
    }

    /**
     * Adds an object event listener.
     *
     * @param objectEventListener The object event listener.
     *
     * @return True if added, false if already added.
     */
    @CheckReturnValue
    public boolean addObjectEventListener(
            @Nonnull final ObjectEventListener objectEventListener)
    {
        return _objectEventListenerManager.addListener(objectEventListener);
    }

    /**
     * Adds a received response listener.
     *
     * @param receivedResponseListener The received response listener.
     *
     * @return True if added, false if already added.
     */
    @CheckReturnValue
    public boolean addReceivedResponseListener(
            @Nonnull final ReceivedFragmentListener receivedResponseListener)
    {
        return _connectionManager
            .addReceivedFragmentListener(receivedResponseListener);
    }

    /**
     * Adds an unsolicited item listener.
     *
     * @param unsolicitedItemListener The unsolicited item listener.
     *
     * @return True if added, false if already added.
     */
    @CheckReturnValue
    public boolean addUnsolicitedItemListener(
            @Nonnull final UnsolicitedItemListener unsolicitedItemListener)
    {
        return _unsolicitedItemListenerManager
            .addListener(unsolicitedItemListener);
    }

    /** {@inheritDoc}
     */
    @Override
    public void close()
    {
        final ServiceThread thread = _requestsSenderThread.getAndSet(null);

        if (thread != null) {
            getThisLogger()
                .debug(ServiceMessages.STOPPING_THREAD, thread.getName());
            Require.ignored(thread.interruptAndJoin(getThisLogger(), 0));

            disconnect();

            try {
                _connectionManager.stopListening();
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
            }

            _objectEventListenerManager.clear();
            _unsolicitedItemListenerManager.clear();
        }
    }

    /**
     * Commits point update requests.
     *
     * @return The responses.
     *
     * @throws ServiceNotAvailableException On connection failure.
     */
    @Nonnull
    @CheckReturnValue
    public Collection<WriteTransaction.Response> commitPointUpdateRequests()
        throws ServiceNotAvailableException
    {
        final Collection<WriteTransaction.Response> responses =
            new LinkedList<>();

        for (final MasterOutstationAssociation association:
                _outputAssociations) {
            try {
                for (final WriteTransaction.Response response:
                        association.commitWriteRequests()) {
                    responses.add(response);
                }
            } catch (final ServiceNotAvailableException exception) {
                getThisLogger()
                    .warn(
                        DNP3Messages.WRITE_COMMIT_FAILED,
                        association
                            .getRemoteEndPoint()
                            .getRemoteProxyName()
                            .orElse(null));

                throw exception;
            }
        }

        _outputAssociations.clear();

        return responses;
    }

    /**
     * Commits point value requests.
     *
     * @return The responses.
     *
     * @throws ServiceNotAvailableException On connection failure.
     */
    @Nonnull
    @CheckReturnValue
    public Collection<ReadTransaction.Response> commitPointValueRequests()
        throws ServiceNotAvailableException
    {
        final Collection<ReadTransaction.Response> responses =
            new LinkedList<>();

        for (final MasterOutstationAssociation association:
                _inputAssociations) {
            try {
                for (final ReadTransaction.Response response:
                        association.commitReadRequests()) {
                    responses.add(response);
                }
            } catch (final ServiceNotAvailableException exception) {
                getThisLogger()
                    .warn(
                        DNP3Messages.READ_COMMIT_FAILED,
                        association
                            .getRemoteEndPoint()
                            .getRemoteProxyName()
                            .orElse(null));

                throw exception;
            }
        }

        _inputAssociations.clear();

        return responses;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean connect(final Origin origin)
        throws InterruptedException
    {
        return connect(origin, (short) 0);
    }

    /**
     * Connects to an outstation.
     *
     * @param outstationOrigin The outstation origin.
     * @param remoteAddress The remote DNP3 address.
     *
     * @return True on success.
     *
     * @throws InterruptedException When interrupted.
     */
    @CheckReturnValue
    public boolean connect(
            @Nonnull final Origin outstationOrigin,
            final short remoteAddress)
        throws InterruptedException
    {
        final Optional<? extends PAPProxy> outstationProxy =
            _getOutstationProxy(
                outstationOrigin);

        if (!outstationProxy.isPresent()) {
            return false;
        }

        final Association association = _connectionManager
            .connect(
                (DNP3Proxy) outstationProxy.get(),
                _localDeviceAddress,
                remoteAddress);

        if (association == null) {
            return false;
        }

        final Fragment request = _newRequest(
            association,
            FunctionCode.DISABLE_UNSOLICITED);

        Require
            .ignored(
                request
                    .add(
                            new Fragment.Item(
                                    ObjectHeader.newInstance(
                                            ClassObjectsVariation.CLASS_1_DATA,
                                                    ObjectRange.NONE))));
        Require
            .ignored(
                request
                    .add(
                            new Fragment.Item(
                                    ObjectHeader.newInstance(
                                            ClassObjectsVariation.CLASS_2_DATA,
                                                    ObjectRange.NONE))));
        Require
            .ignored(
                request
                    .add(
                            new Fragment.Item(
                                    ObjectHeader.newInstance(
                                            ClassObjectsVariation.CLASS_3_DATA,
                                                    ObjectRange.NONE))));

        _executeRequest(request);

        final Fragment response;

        try {
            response = _receiveNullResponse(request);
        } catch (final DNP3ProtocolException exception) {
            getThisLogger()
                .trace(
                    exception,
                    BaseMessages.VERBATIM,
                    exception.getMessage());

            return false;
        }

        ((MasterOutstationAssociation) response.getAssociation())
            .setUnsolicitedSupported(
                !response.getInternalIndications().hasNoFuncCodeSupport());

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void disconnect(final Origin origin)
    {
        final Optional<PAPProxy> outstationProxy = forgetServerProxy(origin);

        if (outstationProxy.isPresent()) {
            _connectionManager.disconnect((DNP3Proxy) outstationProxy.get());
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public PointValue[] fetchPointValues(
            final Point[] points)
        throws InterruptedException, ServiceNotAvailableException
    {
        final PointValue[] pointValues = new PointValue[points.length];

        try {
            for (int i = 0; i < pointValues.length; ++i) {
                pointValues[i] = read(points[i]);
            }
        } catch (final IOException exception) {
            throw new ServiceNotAvailableException(exception);
        }

        return pointValues;
    }

    /**
     * Gets the local device address.
     *
     * @return The local device address.
     */
    @CheckReturnValue
    public short getLocalDeviceAddress()
    {
        return _localDeviceAddress;
    }

    /**
     * Gets the remote end point for a proxy.
     *
     * @param proxy The proxy.
     *
     * @return The remote end point (empty if unknown).
     */
    @Nonnull
    @CheckReturnValue
    public Optional<RemoteEndPoint> getRemoteEndPoint(
            @Nonnull final DNP3Proxy proxy)
    {
        return _connectionManager.getRemoteEndPoint(proxy);
    }

    /**
     * Gets a station point.
     *
     * @param point The metadata point.
     *
     * @return The station point.
     *
     * @throws IllegalArgumentException When the station point is unknown.
     */
    @Nonnull
    @CheckReturnValue
    public DNP3StationPoint getStationPoint(@Nonnull final Point point)
    {
        return ((DNP3MasterContext) getContext()).getRemoteStationPoint(point);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onReceivedFragment(
            final Fragment response)
        throws IOException
    {
        if (response.isUnsolicited()) {
            if (response.isConfirmRequested()) {
                _executeTarget(() -> _sendConfirmUnsolicited(response));
            }

            response
                .getItems()
                .forEach(
                    item -> _unsolicitedItemListenerManager
                        .onUnsolicitedItem(item));
        } else {
            if (response.getInternalIndications().hasDeviceRestart()) {
                _executeTarget(
                    () -> _sendClearDeviceRestart(response.getAssociation()));
            }

            if (response.getInternalIndications().hasNeedTime()) {
                if (_sendingTime.compareAndSet(false, true)) {
                    _executeTarget(() -> _sendTime(response.getAssociation()));
                }
            }

            _responses.add(response);
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void open()
    {
        final Thread thread = _requestsSenderThread.get();

        if (thread != null) {
            Require.ignored(addReceivedResponseListener(this));

            try {
                _connectionManager.startListening(this);
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
            }

            thread.start();
        }
    }

    /**
     * Reads an input point value.
     *
     * @param point The point.
     *
     * @return The point value.
     *
     * @throws IOException On I/O exception.
     * @throws InterruptedException When interrupted.
     */
    @Nonnull
    @CheckReturnValue
    public PointValue read(
            @Nonnull final Point point)
        throws IOException, InterruptedException
    {
        final DNP3StationPoint stationPoint = getStationPoint(point);
        final Fragment request = _newRequest(
            _getAssociation(stationPoint),
            FunctionCode.READ,
            stationPoint.getInputVariation(),
            stationPoint.getObjectRange());
        final Fragment response;

        _executeRequest(request);
        response = _responses.take();

        final Deque<Fragment.Item> responseItems = response.getItems();
        final Serializable responseValue;

        if (!responseItems.isEmpty()) {
            final Fragment.Item responseItem = responseItems.getFirst();
            final ObjectInstance[] objectinstances = responseItem
                .getObjectInstances()
                .orElse(new ObjectInstance[0]);

            if (objectinstances.length == 1) {
                final ObjectInstance objectInstance = objectinstances[0];

                responseValue = ((ObjectInstance.WithValue) objectInstance)
                    .getValue();
            } else if (objectinstances.length > 0) {
                final Tuple tuple = new Tuple(objectinstances.length);

                for (final ObjectInstance objectInstance: objectinstances) {
                    tuple
                        .add(
                            ((ObjectInstance.WithValue) objectInstance)
                                .getValue());
                }

                responseValue = tuple;
            } else {
                responseValue = null;
            }
        } else {
            responseValue = null;
        }

        final PointValue pointValue = new PointValue(
            point,
            Optional.of(DateTime.now()),
            null,
            responseValue);

        getThisLogger().trace(DNP3Messages.RECEIVED_POINT_VALUE, pointValue);

        return pointValue;
    }

    /**
     * Removes an object event listener.
     *
     * @param objectEventListener The object event listener.
     *
     * @return True if removed, false if already removed.
     */
    @CheckReturnValue
    public boolean removeObjectEventListener(
            @Nonnull final ObjectEventListener objectEventListener)
    {
        return _objectEventListenerManager.removeListener(objectEventListener);
    }

    /**
     * Removes an unsolicited item listener.
     *
     * @param unsolicitedItemListener The unsolicited item listener.
     *
     * @return True if removed, false if already removed.
     */
    @CheckReturnValue
    public boolean removeUnsolicitedItemListener(
            @Nonnull final UnsolicitedItemListener unsolicitedItemListener)
    {
        return _unsolicitedItemListenerManager
            .removeListener(unsolicitedItemListener);
    }

    /**
     * Requests a point update.
     *
     * @param pointValue The point value.
     *
     * @return The new request.
     *
     * @throws ConnectException When connect fails.
     * @throws InterruptedException When interrupted.
     */
    @Nonnull
    @CheckReturnValue
    public WriteTransaction.Request requestPointUpdate(
            @Nonnull final PointValue pointValue)
        throws ConnectException, InterruptedException
    {
        final Point point = pointValue.getPoint().get();
        final MasterOutstationAssociation association = _getAssociation(
            getStationPoint(point));

        _outputAssociations.add(association);

        return association.addWriteRequest(pointValue);
    }

    /**
     * Requests a point value.
     *
     * @param point The point.
     *
     * @return The new request.
     *
     * @throws ConnectException When connect fails.
     * @throws InterruptedException When interrupted.
     */
    @Nonnull
    @CheckReturnValue
    public ReadTransaction.Request requestPointValue(
            @Nonnull final Point point)
        throws ConnectException, InterruptedException
    {
        final MasterOutstationAssociation association = _getAssociation(
            getStationPoint(point));

        _inputAssociations.add(association);

        return association.addReadRequest(point);
    }

    /**
     * Sets up this.
     *
     * @param listenProperties The listen properties.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public boolean setUp(@Nonnull final KeyedValues listenProperties)
    {
        return _connectionManager.setUp(listenProperties);
    }

    /**
     * Tears down what has been set up.
     */
    public void tearDown()
    {
        close();

        _connectionManager.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    public Exception[] updatePointValues(
            final PointValue[] pointValues)
        throws ServiceNotAvailableException, InterruptedException
    {
        final Exception[] exceptions = new Exception[pointValues.length];

        for (int i = 0; i < exceptions.length; ++i) {
            try {
                write(pointValues[i]);
            } catch (final IOException exception) {
                exceptions[i] = exception;
            }
        }

        return exceptions;
    }

    /**
     * Writes a point value.
     *
     * @param pointValue The point value.
     *
     * @throws IOException On I/O exception.
     * @throws InterruptedException When interrupted.
     */
    public void write(
            @Nonnull final PointValue pointValue)
        throws IOException, InterruptedException
    {
        final Optional<Point> point = pointValue.getPoint();

        if (!point.isPresent()) {
            throw new IllegalArgumentException(
                "Point entity reference missing: " + pointValue);
        }

        final DNP3StationPoint stationPoint = getStationPoint(point.get());
        final PointType.Support pointSupport = stationPoint.getSupport();

        if (pointSupport.isReadOnly()) {
            throw new IllegalArgumentException(
                "Point cannot be written: " + point.get());
        }

        final Optional<ObjectVariation> objectVariation = stationPoint
            .getOutputVariation(stationPoint.getDataType());
        final ObjectRange objectRange = stationPoint.getObjectRange();
        final Serializable value = pointValue.getValue();

        if (value == null) {
            throw new IllegalArgumentException(
                "Null in point value: " + pointValue);
        }

        final ObjectInstance.WithValue valueObject =
            (ObjectInstance.WithValue) objectVariation
                .get()
                .newObjectInstance();

        valueObject.setValue(value);

        final Fragment request = _newRequest(
            _getAssociation(stationPoint),
            FunctionCode.DIRECT_OPERATE,
            valueObject,
            objectRange);

        _executeRequest(request);
        _receiveNullResponse(request);

        getThisLogger().trace(DNP3Messages.SENT_POINT_VALUE, pointValue);
    }

    private static Fragment _newRequest(
            final Association association,
            final FunctionCode functionCode)
    {
        final Fragment request = new Fragment(
            association,
            Fragment.Header.newInstance(Optional.of(functionCode), true));

        return request;
    }

    private static Fragment _newRequest(
            final Association association,
            final FunctionCode functionCode,
            final ObjectInstance objectInstance,
            final ObjectRange objectRange)
    {
        final ObjectVariation objectVariation = objectInstance
            .getObjectVariation();
        final Fragment request = _newRequest(association, functionCode);
        final ObjectHeader objectHeader = ObjectHeader
            .newInstance(objectVariation, objectRange);

        Require
            .ignored(
                request
                    .add(
                            new Fragment.Item(
                                    objectHeader,
                                            Optional.of(
                                                    new ObjectInstance[] {
                                                    objectInstance, }))));

        return request;
    }

    private static Fragment _newRequest(
            final Association association,
            final FunctionCode functionCode,
            final ObjectVariation objectVariation,
            final ObjectRange objectRange)
    {
        final Fragment request = new Fragment(
            association,
            Fragment.Header.newInstance(Optional.of(functionCode), true));

        Require
            .ignored(
                request
                    .add(
                            new Fragment.Item(
                                    ObjectHeader.newInstance(
                                            objectVariation,
                                                    objectRange))));

        return request;
    }

    private static void _sendConfirmUnsolicited(
            final Fragment response)
        throws IOException
    {
        final Fragment request = _newRequest(
            response.getAssociation(),
            FunctionCode.CONFIRM);

        request.setUnsolicited();
        request.setSequence(response.getSequence());

        request.send();
    }

    private static void _sendRequest(
            final Fragment request,
            final CountDownLatch latch)
        throws IOException
    {
        request
            .setSequence(
                request
                    .getAssociation()
                    .getApplicationLayer()
                    .nextSolicitedSequence());
        request.send();

        latch.countDown();
    }

    private void _executeRequest(
            final Fragment request)
        throws InterruptedException
    {
        final CountDownLatch latch = new CountDownLatch(1);

        _executeTarget(() -> _sendRequest(request, latch));
        latch.await();
    }

    private void _executeTarget(final Target target)
    {
        _requestsSenderThread.get().execute(target);
    }

    private MasterOutstationAssociation _getAssociation(
            final DNP3StationPoint stationPoint)
        throws ConnectException, InterruptedException
    {
        final DNP3Proxy proxy = (DNP3Proxy) getPointProxy(
            stationPoint.getPoint())
            .get();
        final short remoteAddress = stationPoint
            .getLogicalDevice()
            .getAddress()
            .shortValue();
        Optional<RemoteEndPoint> remoteEndPoint = getRemoteEndPoint(proxy);

        if (!remoteEndPoint.isPresent()) {
            if (!connect(proxy.getOrigin(), remoteAddress)) {
                throw new ConnectException();
            }

            remoteEndPoint = getRemoteEndPoint(proxy);
        }

        return (MasterOutstationAssociation) remoteEndPoint
            .get()
            .getAssociation(_localDeviceAddress, remoteAddress);
    }

    private Optional<? extends PAPProxy> _getOutstationProxy(
            final Origin origin)
    {
        final Optional<? extends PAPProxy> outstationProxy = getContext()
            .getRemoteProxyByOrigin(origin);

        if (!outstationProxy.isPresent()) {
            Logger
                .getInstance(getClass())
                .warn(PAPMessages.UNKNOWN_ORIGIN, origin);
        }

        return outstationProxy;
    }

    private Fragment _receiveNullResponse(
            final Fragment request)
        throws DNP3ProtocolException, InterruptedException
    {
        final Fragment response = _responses.take();

        if (!response.getItems().isEmpty()) {
            throw new DNP3ProtocolException(
                DNP3Messages.UNEXPECTED_RESPONSE_ITEMS);
        }

        return response;
    }

    private void _sendClearDeviceRestart(
            final Association association)
        throws IOException, InterruptedException
    {
        final ObjectInstance.Packed objectInstance =
            (ObjectInstance.Packed) InternalIndicationsVariation.PACKED_FORMAT
                .newObjectInstance();
        final Fragment request = _newRequest(
            association,
            FunctionCode.WRITE,
            objectInstance,
            ObjectRange
                .newIndexInstance(InternalIndication.DEVICE_RESTART.getCode()));

        objectInstance.put(0, 0);

        request
            .setSequence(
                association.getApplicationLayer().nextSolicitedSequence());
        request.send();
        _receiveNullResponse(request);
    }

    private void _sendTime(
            final Association association)
        throws IOException, InterruptedException
    {
        Fragment request = _newRequest(
            association,
            FunctionCode.RECORD_CURRENT_TIME);
        final DateTime recordedTime;

        request
            .setSequence(
                association.getApplicationLayer().nextSolicitedSequence());
        request.send();
        recordedTime = DateTime.now();
        _receiveNullResponse(request);

        final ObjectInstance timeObject =
            TimeDateVariation.ABSOLUTE_TIME_LAST_RECORDED
                .newObjectInstance();

        request = _newRequest(
            association,
            FunctionCode.WRITE,
            timeObject,
            ObjectRange.newCountInstance(1));
        request
            .setSequence(
                association.getApplicationLayer().nextSolicitedSequence());

        ((ObjectInstance.WithTime) timeObject).setTime(recordedTime);

        request.send();
        _receiveNullResponse(request);

        _sendingTime.set(false);
    }

    private final ConnectionManager _connectionManager;
    private final Set<MasterOutstationAssociation> _inputAssociations =
        new IdentityHashSet<>();
    private final short _localDeviceAddress;
    private final ObjectEventListener.Manager _objectEventListenerManager =
        new ObjectEventListener.Manager();
    private final Set<MasterOutstationAssociation> _outputAssociations =
        new IdentityHashSet<>();
    private final AtomicReference<ServiceThread> _requestsSenderThread =
        new AtomicReference<>(
            new ServiceThread("DNP3 Master requests sender"));
    private final BlockingQueue<Fragment> _responses =
        new LinkedBlockingQueue<>();
    private final AtomicBoolean _sendingTime = new AtomicBoolean();
    private final UnsolicitedItemListener.Manager _unsolicitedItemListenerManager =
        new UnsolicitedItemListener.Manager();

    /**
     * Unsolicited item listener.
     */
    public interface UnsolicitedItemListener
    {
        /**
         * Called when an unsolicited item is received.
         *
         * @param responseItem The unsolicited item.
         *
         * @return True if the item has been handled.
         */
        @CheckReturnValue
        boolean onUnsolicitedItem(@Nonnull Fragment.Item responseItem);

        /**
         * Received message listener manager.
         */
        class Manager
            extends ListenerManager<UnsolicitedItemListener>
            implements UnsolicitedItemListener
        {
            /** {@inheritDoc}
             */
            @Override
            public boolean onUnsolicitedItem(final Fragment.Item responseItem)
            {
                for (final UnsolicitedItemListener listener: getListeners()) {
                    if (listener.onUnsolicitedItem(responseItem)) {
                        return true;
                    }
                }

                return false;
            }
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
