/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DNP3Outstation.java 4085 2019-06-16 15:17:12Z SFB $
 */

package org.rvpf.pap.dnp3;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.UUID;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.util.container.KeyedValues;
import org.rvpf.base.value.PointValue;
import org.rvpf.pap.PAPConnectionListener;
import org.rvpf.pap.PAPContext;
import org.rvpf.pap.PAPProxy;
import org.rvpf.pap.PAPServer;
import org.rvpf.pap.dnp3.object.ObjectRange;
import org.rvpf.pap.dnp3.object.PointType;
import org.rvpf.pap.dnp3.object.content.InternalIndication;
import org.rvpf.pap.dnp3.transport.ApplicationMessage;
import org.rvpf.pap.dnp3.transport.Association;
import org.rvpf.pap.dnp3.transport.AssociationListener;
import org.rvpf.pap.dnp3.transport.ConnectionManager;
import org.rvpf.pap.dnp3.transport.Fragment;
import org.rvpf.pap.dnp3.transport.FunctionCode;
import org.rvpf.pap.dnp3.transport.InternalIndications;
import org.rvpf.pap.dnp3.transport.LogicalDevice;
import org.rvpf.pap.dnp3.transport.ReceivedFragmentListener;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;

/**
 * DNP3 outstation.
 */
public final class DNP3Outstation
    extends PAPServer.Abstract
    implements PAPConnectionListener, AssociationListener,
        ReceivedFragmentListener
{
    /**
     * Constructs an instance.
     *
     * @param outstationContext The context.
     */
    DNP3Outstation(@Nonnull final DNP3OutstationContext outstationContext)
    {
        _context = outstationContext;
        _connectionManager = new ConnectionManager(outstationContext);
        _responsesSenderThread
            .set(
                new ServiceThread(
                    this::_sendResponses,
                    "DNP3 Outstation responses sender"));
    }

    /**
     * Adds an association listener.
     *
     * @param associationListener The association listener.
     *
     * @return True if added, false if already added.
     */
    @CheckReturnValue
    public boolean addAssociationListener(
            @Nonnull final AssociationListener associationListener)
    {
        return _connectionManager.addAssociationListener(associationListener);
    }

    /** {@inheritDoc}
     */
    @Override
    public void addPointValue(@Nonnull final PointValue pointValue)
    {
        synchronized (_mutex) {
            _updating = true;
            _updates.add(pointValue);
        }
    }

    /**
     * Adds a received request listener.
     *
     * @param receivedRequestListener The received request listener.
     *
     * @return True if added, false if already added.
     */
    @CheckReturnValue
    public boolean addReceivedRequestListener(
            @Nonnull final ReceivedFragmentListener receivedRequestListener)
    {
        return _connectionManager
            .addReceivedFragmentListener(receivedRequestListener);
    }

    /**
     * Gets the point for a point type and object range.
     *
     * @param pointType The point type.
     * @param objectRange The object range for the point.
     *
     * @return The point (empty if unconfigured).
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Point> getPoint(
            @Nonnull final PointType pointType,
            @Nonnull final ObjectRange objectRange)
    {
        return ((DNP3Context) getContext()).getPoint(pointType, objectRange);
    }

    /**
     * Gets a point value for a point.
     *
     * @param point The point.
     *
     * @return The point value (empty if none).
     */
    @Nonnull
    @CheckReturnValue
    public Optional<PointValue> getPointValue(@Nonnull final Point point)
    {
        PointValue pointValue = _pointValues.get(point.getUUID().get());

        if (pointValue == null) {
            final Optional<PAPProxy.Responder> responder = getResponder();

            if (responder.isPresent()) {
                final PointValue[] pointValues;

                try {
                    pointValues = responder.get().select(new Point[] {point, });
                } catch (final InterruptedException
                         |ServiceNotAvailableException exception) {
                    throw new RuntimeException(exception);
                }

                if (pointValues.length > 0) {
                    pointValue = pointValues[0];
                }
            }
        }

        return Optional.ofNullable(pointValue);
    }

    /**
     * Gets a station point.
     *
     * @param point A metadata point.
     *
     * @return The station point.
     *
     * @throws IllegalArgumentException When the station point is unknown.
     */
    @Nonnull
    @CheckReturnValue
    public DNP3StationPoint getStationPoint(@Nonnull final Point point)
    {
        return ((DNP3Context) getContext()).getRemoteStationPoint(point);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isUpdating()
    {
        synchronized (_mutex) {
            return _updating;
        }
    }

    /**
     * Returns the next message.
     *
     * @return The request.
     *
     * @throws InterruptedException When interrupted.
     */
    @Nonnull
    @CheckReturnValue
    public Fragment nextRequest()
        throws InterruptedException
    {
        return _requests.take();
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<PointValue> nextUpdate(
            final long timeout)
        throws InterruptedException
    {
        return (timeout < 0)? Optional
            .of(_updates.take()): Optional
                .ofNullable(_updates.poll(timeout, TimeUnit.MILLISECONDS));
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onLostConnection(
            final PAPProxy remoteProxy,
            final Optional<Exception> cause)
    {
        ((DNP3MasterProxy) remoteProxy).stop();

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onNewAssociation(
            final Association association)
        throws IOException
    {
        if (_internalIndications.hasDeviceRestart()) {
            final ApplicationMessage nullUnsolicitedResponse =
                new ApplicationMessage(
                    association,
                    Optional.of(FunctionCode.UNSOLICITED_RESPONSE),
                    false);

            nullUnsolicitedResponse.setUnsolicited();
            nullUnsolicitedResponse
                .setSequence(
                    association.getApplicationLayer().nextUnsolicitedSequence());
            nullUnsolicitedResponse.setConfirmRequested();
            _responses.add(nullUnsolicitedResponse);
        }

        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onNewConnection(final PAPProxy remoteProxy)
    {
        if (_respond) {
            ((DNP3MasterProxy) remoteProxy)
                .start(this, response -> _responses.add(response));
        }

        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onReceivedFragment(final Fragment request)
        throws IOException
    {
        _requests.add(request);

        return true;
    }

    /**
     * Called on updates commit.
     */
    public void onUpdatesCommit()
    {
        synchronized (_mutex) {
            if (_updates.isEmpty()) {
                _updating = false;
                _mutex.notifyAll();
            }
        }
    }

    /**
     * Puts a point value.
     *
     * @param pointValue The point value.
     */
    public void putPointValue(@Nonnull final PointValue pointValue)
    {
        final Optional<Point> point = pointValue.getPoint();

        if (point.isPresent()) {
            _pointValues.put(pointValue.getPointUUID(), pointValue);
            addPointValue(pointValue);
        }
    }

    /**
     * Records time.
     */
    public void recordTime() {}

    /**
     * Removes an association listener.
     *
     * @param associationListener The association listener.
     *
     * @return True if removed, false if already removed.
     */
    @CheckReturnValue
    public boolean removeAssociationListener(
            @Nonnull final AssociationListener associationListener)
    {
        return _connectionManager
            .removeAssociationListener(associationListener);
    }

    /**
     * Sets the value of an internal indication.
     *
     * @param internalIndication The internal indication.
     * @param set True to set, false to clear.
     */
    public void setInternalIndication(
            @Nonnull final InternalIndication internalIndication,
            final boolean set)
    {
        _internalIndications.set(internalIndication, set);
    }

    /**
     * Sets the time needed indicator.
     */
    public void setNeedTime()
    {
        _internalIndications.setNeedTime(true);
    }

    /**
     * Sets the time.
     *
     * @param dateTime The time.
     */
    public void setTime(@Nonnull final DateTime dateTime)
    {
        _internalIndications.setNeedTime(false);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUpListener(@Nonnull final KeyedGroups listenerProperties)
    {
        // Registers logical devices.

        final DNP3Context context = (DNP3Context) getContext();
        final LogicalDevice[] logicalDevices = context
            .logicalDevices(
                listenerProperties.getStrings(DNP3.LOGICAL_DEVICE_PROPERTY),
                "");

        if (logicalDevices == null) {
            return false;
        }

        final Map<Short, LogicalDevice> logicalDeviceByAddress = new HashMap<>(
            KeyedValues.hashCapacity(logicalDevices.length),
            KeyedValues.HASH_LOAD_FACTOR);

        for (final LogicalDevice logicalDevice: logicalDevices) {
            logicalDeviceByAddress
                .put(logicalDevice.getAddress(), logicalDevice);
        }

        if (logicalDeviceByAddress.isEmpty()) {
            final Short address = Short.valueOf((short) 0);

            logicalDeviceByAddress.put(address, new LogicalDevice("", address));
        }

        _connectionManager.registerLogicalDevices(logicalDeviceByAddress);

        return _connectionManager.setUp(listenerProperties);
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
    {
        _respond = true;

        final Thread thread = _responsesSenderThread.get();

        if (thread != null) {
            Require.ignored(addAssociationListener(this));
            Require.ignored(addReceivedRequestListener(this));

            try {
                _connectionManager.startListening(this);
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
            }

            thread.start();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        final ServiceThread thread = _responsesSenderThread.getAndSet(null);

        if (thread != null) {
            getThisLogger()
                .debug(ServiceMessages.STOPPING_THREAD, thread.getName());
            Require.ignored(thread.interruptAndJoin(getThisLogger(), 0));

            try {
                _connectionManager.stopListening();
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        stop();

        _connectionManager.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    protected PAPContext getContext()
    {
        return _context;
    }

    private void _sendResponses()
        throws InterruptedException, IOException
    {
        for (;;) {
            final ApplicationMessage response = _responses.take();

            response.setInternalIndications(_internalIndications);
            Require.ignored(response.send());
        }
    }

    private final ConnectionManager _connectionManager;
    private final DNP3OutstationContext _context;
    private InternalIndications _internalIndications = new InternalIndications(
        InternalIndication.DEVICE_RESTART);
    private final Object _mutex = new Object();
    private final Map<UUID, PointValue> _pointValues =
        new ConcurrentHashMap<>();
    private final BlockingQueue<Fragment> _requests =
        new LinkedBlockingQueue<>();
    private volatile boolean _respond;
    private final BlockingQueue<ApplicationMessage> _responses =
        new LinkedBlockingQueue<>();
    private final AtomicReference<ServiceThread> _responsesSenderThread =
        new AtomicReference<>();
    private final BlockingQueue<PointValue> _updates =
        new LinkedBlockingQueue<>();
    private boolean _updating;
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
