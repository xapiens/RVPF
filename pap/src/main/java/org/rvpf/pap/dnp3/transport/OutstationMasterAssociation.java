/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: OutstationMasterAssociation.java 4085 2019-06-16 15:17:12Z SFB $
 */

package org.rvpf.pap.dnp3.transport;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * Outstation-master association.
 */
public final class OutstationMasterAssociation
    extends Association
{
    /**
     * Constructs an instance.
     *
     * @param localDeviceAddress The local device address.
     * @param remoteDeviceAddress The remote device address.
     * @param remoteEndPoint The remote end point.
     */
    public OutstationMasterAssociation(
            final short localDeviceAddress,
            final short remoteDeviceAddress,
            @Nonnull final RemoteEndPoint remoteEndPoint)
    {
        super(localDeviceAddress, remoteDeviceAddress, remoteEndPoint);
    }

    /**
     * Gets the latest accepted request sequence.
     *
     * @return The latest accepted request sequence.
     */
    @CheckReturnValue
    public byte getLatestAcceptedRequestSequence()
    {
        return _latestAcceptedRequestSequence;
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
     * @return The latest unsolicited response fragment (may be empty).
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Fragment> getLatestUnsolicitedResponseFragment()
    {
        return Optional.ofNullable(_latestUnsolicitedResponseFragment);
    }

    /**
     * Gets the first valid request accepted indicator.
     *
     * @return The first valid request accepted indicator.
     */
    @CheckReturnValue
    public boolean isFirstValidRequestAccepted()
    {
        return _firstValidRequestAccepted;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isWithOutstation()
    {
        return false;
    }

    /**
     * Sets the first valid request accepted indicator.
     *
     * @param firstValidRequestAccepted The first valid request accepted
     *                                  indicator.
     */
    public void setFirstValidRequestAccepted(
            final boolean firstValidRequestAccepted)
    {
        _firstValidRequestAccepted = firstValidRequestAccepted;
    }

    /**
     * Sets the latest accepted request sequence.
     *
     * @param latestAcceptedRequestSequence The latest accepted request
     *                                      sequence.
     */
    public void setLatestAcceptedRequestSequence(
            final byte latestAcceptedRequestSequence)
    {
        _latestAcceptedRequestSequence = latestAcceptedRequestSequence;
    }

    /**
     * Sets the latest solicited response fragment.
     *
     * @param latestSolicitedResponseFragment The latest solicited response
     *                                        fragment.
     */
    public void setLatestSolicitedResponseFragment(
            @Nonnull final Fragment latestSolicitedResponseFragment)
    {
        _latestSolicitedResponseFragment = latestSolicitedResponseFragment;
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
        _latestUnsolicitedResponseFragment = latestUnsolicitedResponseFragment;
    }

    private boolean _firstValidRequestAccepted;
    private byte _latestAcceptedRequestSequence;
    private Fragment _latestSolicitedResponseFragment;
    private Fragment _latestUnsolicitedResponseFragment;
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
