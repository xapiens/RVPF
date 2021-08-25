/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MasterOutstationAssociation.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3.transport;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Point;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.value.PointValue;
import org.rvpf.pap.dnp3.DNP3OutstationProxy;

/**
 * Master-outstation association.
 */
public final class MasterOutstationAssociation
    extends Association
{
    /**
     * Constructs an instance.
     *
     * @param localDeviceAddress The local device address.
     * @param remoteDeviceAddress The remote device address.
     * @param remoteEndPoint The remote end point.
     */
    public MasterOutstationAssociation(
            final short localDeviceAddress,
            final short remoteDeviceAddress,
            @Nonnull final RemoteEndPoint remoteEndPoint)
    {
        super(localDeviceAddress, remoteDeviceAddress, remoteEndPoint);

        final DNP3OutstationProxy outstationProxy =
            (DNP3OutstationProxy) remoteEndPoint
                .getRemoteProxy();

        _readTransaction = new ReadTransaction(outstationProxy);
        _writeTransaction = new WriteTransaction(outstationProxy);
    }

    /**
     * Adds a read request for a point.
     *
     * @param point The point.
     *
     * @return The new read request.
     */
    @Nonnull
    @CheckReturnValue
    public ReadTransaction.Request addReadRequest(@Nonnull final Point point)
    {
        return _readTransaction.addRequest(point);
    }

    /**
     * Adds write request for a point value.
     *
     * @param pointValue The point value.
     *
     * @return The new write request.
     */
    @Nonnull
    @CheckReturnValue
    public WriteTransaction.Request addWriteRequest(
            @Nonnull final PointValue pointValue)
    {
        return _writeTransaction.addRequest(pointValue);
    }

    /**
     * Commits read requests.
     *
     * @return The responses.
     *
     * @throws ServiceNotAvailableException On connection failure.
     */
    @Nonnull
    @CheckReturnValue
    public ReadTransaction.Response[] commitReadRequests()
        throws ServiceNotAvailableException
    {
        try {
            return _readTransaction.commit();
        } catch (final ServiceNotAvailableException exception) {
            disconnect();

            throw exception;
        }
    }

    /**
     * Commits write requests.
     *
     * @return The responses.
     *
     * @throws ServiceNotAvailableException On connection failure.
     */
    @Nonnull
    @CheckReturnValue
    public WriteTransaction.Response[] commitWriteRequests()
        throws ServiceNotAvailableException
    {
        try {
            return _writeTransaction.commit();
        } catch (final ServiceNotAvailableException exception) {
            disconnect();

            throw exception;
        }
    }

    /**
     * Disconnects.
     */
    public void disconnect()
    {
        getRemoteEndPoint().close();
    }

    /**
     * Gets the latest request sequenceSent.
     *
     * @return The latest request sequence sent.
     */
    @CheckReturnValue
    public byte getLatestRequestSequenceSent()
    {
        return _latestRequestSequenceSent;
    }

    /**
     * Gets the latest solicited response fragment.
     *
     * @return The latest solicited response fragment (may be empty).
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Fragment> getLatestSolicitedResponseFragment()
    {
        return Optional.ofNullable(_latestSolicitedResponseFragment);
    }

    /**
     * Gets the latest unsolicited response fragment.
     *
     * @return The latest unsolicited response fragment (may be emppty).
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Fragment> getLatestUnsolicitedResponseFragment()
    {
        return Optional.ofNullable(_latestUnsolicitedResponseFragment);
    }

    /**
     * Gets the unsolicited supported indicator.
     *
     * @return The unsolicited supported indicator.
     */
    @CheckReturnValue
    public boolean isUnsolicitedSupported()
    {
        return _unsolicitedSupported;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isWithOutstation()
    {
        return true;
    }

    /**
     * Rolls back read requests.
     */
    public void rollbackReadRequests()
    {
        _readTransaction.rollback();
    }

    /**
     * Rolls back write requests.
     */
    public void rollbackWriteRequests()
    {
        _writeTransaction.rollback();
    }

    /**
     * Sets the latest request sequence sent.
     *
     * @param latestRequestSequenceSent The latest request sequence sent.
     */
    public void setLatestRequestSequenceSent(
            final byte latestRequestSequenceSent)
    {
        _latestRequestSequenceSent = latestRequestSequenceSent;
    }

    /**
     * Sets the latest solicited response rragment.
     *
     * @param latestSolicitedResponseFragment The latest solicited response
     *                                        fragment.
     */
    public void setLatestSolicitedResponseFragment(
            @Nonnull final Fragment latestSolicitedResponseFragment)
    {
        _latestSolicitedResponseFragment = Objects
            .requireNonNull(latestSolicitedResponseFragment);
    }

    /**
     * Sets the latest unsolicited response fragment.
     *
     * @param latestUnsolicitedResponseFragment The latest unsolicited response
     *                                          fragment.
     */
    public void setLatestUnsolicitedResponseFragment(
            @Nonnull final Fragment latestUnsolicitedResponseFragment)
    {
        _latestUnsolicitedResponseFragment = Objects
            .requireNonNull(latestUnsolicitedResponseFragment);
    }

    /**
     * Sets the unsolicited supported indicator.
     *
     * @param unsolicitedSupported The unsolicited supported indicator.
     */
    public void setUnsolicitedSupported(final boolean unsolicitedSupported)
    {
        _unsolicitedSupported = unsolicitedSupported;
    }

    private byte _latestRequestSequenceSent;
    private Fragment _latestSolicitedResponseFragment;
    private Fragment _latestUnsolicitedResponseFragment;
    private final ReadTransaction _readTransaction;
    private boolean _unsolicitedSupported;
    private final WriteTransaction _writeTransaction;
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
