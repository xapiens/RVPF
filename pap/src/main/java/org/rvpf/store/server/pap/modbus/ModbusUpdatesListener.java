/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ModbusUpdatesListener.java 4088 2019-06-18 13:07:16Z SFB $
 */

package org.rvpf.store.server.pap.modbus;

import java.util.Optional;

import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.pap.PAP;
import org.rvpf.pap.modbus.Modbus;
import org.rvpf.pap.modbus.ModbusServer;
import org.rvpf.pap.modbus.ModbusServerContext;
import org.rvpf.pap.modbus.ModbusSupport;
import org.rvpf.store.server.StoreServiceAppImpl;
import org.rvpf.store.server.pap.PAPUpdatesListener;

/**
 * Modbus updates listener.
 */
public final class ModbusUpdatesListener
    extends PAPUpdatesListener
{
    /** {@inheritDoc}
     */
    @Override
    public void onMetadataRefreshed(final Metadata metadata)
    {
        super.onMetadataRefreshed(metadata);

        final ModbusSupport support = new ModbusSupport();
        final ModbusServerContext serverContext = support
            .newServerContext(metadata, _originNames, Optional.empty());
        final ModbusServer server = (serverContext != null)? support
            .newServer(serverContext): null;

        if (server != null) {
            _server = server;
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

        final KeyedGroups modbusProperties = updatesListenerProperties
            .getGroup(Modbus.PROPERTIES);

        _originNames = modbusProperties.getStrings(PAP.ORIGIN_PROPERTY);

        final ModbusSupport support = new ModbusSupport();
        final ModbusServerContext serverContext = support
            .newServerContext(
                storeAppImpl.getMetadata(),
                _originNames,
                Optional.empty());

        _server = (serverContext != null)? support
            .newServer(serverContext): null;

        if ((_server == null) || !_server.setUp(modbusProperties)) {
            return false;
        }

        _server.setResponder(this);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doCommit()
        throws InterruptedException
    {
        _server.onUpdatesCommit();
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doStart()
        throws InterruptedException
    {
        super.doStart();

        _server.start();
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doStop()
    {
        final ModbusServer server = _server;

        if (server != null) {
            server.stop();
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
        final Optional<PointValue> pointValue = _server.nextUpdate(wait? -1: 0);

        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        return pointValue;
    }

    private String[] _originNames;
    private volatile ModbusServer _server;
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
