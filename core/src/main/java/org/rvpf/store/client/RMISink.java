/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RMISink.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.store.client;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.rvpf.base.Point;
import org.rvpf.base.store.StoreAccessException;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.value.PointValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.ProxyEntity;

/**
 * RMI Sink Client.
 *
 * <p>This class provides a client side access to a RMI Sink.</p>
 */
public final class RMISink
    extends AbstractSink
{
    /** {@inheritDoc}
     */
    @Override
    public void connect()
        throws StoreAccessException
    {
        _rmiStoreClient.connect();
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<Exception[]> getExceptions()
    {
        return _rmiStoreClient.getExceptions();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean probe()
        throws StoreAccessException
    {
        return _rmiStoreClient.probe();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean sendUpdates(
            final Collection<PointValue> updates)
        throws StoreAccessException
    {
        return _rmiStoreClient.sendUpdates(updates);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Metadata metadata, final ProxyEntity proxyEntity)
    {
        if (!super.setUp(metadata, proxyEntity)) {
            return false;
        }

        _rmiStoreClient = new RMIStore();

        return _rmiStoreClient.setUp(metadata, proxyEntity);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsDelete()
        throws StoreAccessException
    {
        return _rmiStoreClient.supportsDelete();
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        if (_rmiStoreClient != null) {
            _rmiStoreClient.tearDown();
            _rmiStoreClient = null;
        }

        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    protected void bindPoints(
            final Set<Point> points)
        throws InterruptedException, StoreAccessException
    {
        _rmiStoreClient.bindPoints(points);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doAddQuery(final StoreValuesQuery query) {}

    /** {@inheritDoc}
     */
    @Override
    protected String supportedValueTypeCodes()
        throws StoreAccessException
    {
        return _rmiStoreClient.supportedValueTypeCodes();
    }

    private RMIStore _rmiStoreClient;
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
