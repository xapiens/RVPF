/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DNP3UpdatesListener.java 4088 2019-06-18 13:07:16Z SFB $
 */

package org.rvpf.store.server.pap.dnp3;

import java.util.Optional;

import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.pap.PAP;
import org.rvpf.pap.dnp3.DNP3;
import org.rvpf.pap.dnp3.DNP3Outstation;
import org.rvpf.pap.dnp3.DNP3OutstationContext;
import org.rvpf.pap.dnp3.DNP3Support;
import org.rvpf.store.server.StoreServiceAppImpl;
import org.rvpf.store.server.pap.PAPUpdatesListener;

/**
 * DNP3 updates listener.
 */
public final class DNP3UpdatesListener
    extends PAPUpdatesListener
{
    /** {@inheritDoc}
     */
    @Override
    public void onMetadataRefreshed(final Metadata metadata)
    {
        super.onMetadataRefreshed(metadata);

        final DNP3Support support = new DNP3Support();
        final DNP3OutstationContext outstationContext = support
            .newOutstationContext(metadata, _originNames, Optional.empty());
        final DNP3Outstation outstation = (outstationContext != null)? support
            .newOutstation(outstationContext): null;

        if (outstation != null) {
            _outstation = outstation;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(
            final StoreServiceAppImpl storeAppImpl,
            final KeyedGroups updatesListenerProperties)
    {
        if (!super.setUp(storeAppImpl, updatesListenerProperties)) {
            return false;
        }

        final KeyedGroups dnp3Properties = updatesListenerProperties
            .getGroup(DNP3.PROPERTIES);

        _originNames = dnp3Properties.getStrings(PAP.ORIGIN_PROPERTY);

        final DNP3Support support = new DNP3Support();
        final DNP3OutstationContext outstationContext = support
            .newOutstationContext(
                storeAppImpl.getMetadata(),
                _originNames,
                Optional.empty());

        _outstation = (outstationContext != null)? support
            .newOutstation(outstationContext): null;

        if ((_outstation == null) || !_outstation.setUp(dnp3Properties)) {
            return false;
        }

        _outstation.setResponder(this);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doCommit()
        throws InterruptedException
    {
        _outstation.onUpdatesCommit();
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doStart()
        throws InterruptedException
    {
        final DNP3Outstation outstation = _outstation;

        if (outstation != null) {
            super.doStart();

            _outstation.start();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doStop()
    {
        final DNP3Outstation outstation = _outstation;

        if (outstation != null) {
            outstation.stop();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected Optional<PointValue> nextUpdate(
            final int limit,
            final boolean wait)
        throws InterruptedException
    {
        final DNP3Outstation outstation = _outstation;

        if (outstation == null) {
            throw new InterruptedException();
        }

        final Optional<PointValue> pointValue = _outstation
            .nextUpdate(wait? -1: 0);

        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        return pointValue;
    }

    private String[] _originNames;
    private volatile DNP3Outstation _outstation;
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
