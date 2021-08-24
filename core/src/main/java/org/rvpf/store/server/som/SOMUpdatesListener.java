/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMUpdatesListener.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.store.server.som;

import java.io.Serializable;

import java.util.Optional;

import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.som.SOMFactory;
import org.rvpf.service.som.SOMReceiver;
import org.rvpf.store.server.StoreMessages;
import org.rvpf.store.server.StoreServiceAppImpl;
import org.rvpf.store.server.UpdatesListener;

/**
 * SOM updates listener.
 */
public final class SOMUpdatesListener
    extends UpdatesListener.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    public void doStop()
    {
        _receiver.close();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(
            final StoreServiceAppImpl storeAppImpl,
            final KeyedGroups listenerProperties)
    {
        if (!super.setUp(storeAppImpl, listenerProperties)) {
            return false;
        }

        final KeyedGroups queueProperties = listenerProperties
            .getGroup(QUEUE_PROPERTIES);

        if (queueProperties.isMissing()) {
            getThisLogger()
                .error(ServiceMessages.MISSING_PROPERTIES, QUEUE_PROPERTIES);

            return false;
        }

        final SOMFactory factory = new SOMFactory(storeAppImpl.getConfig());
        final SOMFactory.Queue factoryQueue = factory
            .createQueue(queueProperties);

        _receiver = factoryQueue.createReceiver(false);

        if (_receiver == null) {
            return false;
        }

        getThisLogger().debug(ServiceMessages.SET_UP_COMPLETED);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        _updates = null;
        _index = 0;

        if (_receiver != null) {
            _receiver.tearDown();
        }

        super.tearDown();

        getThisLogger().debug(ServiceMessages.TEAR_DOWN_COMPLETED);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doCommit()
        throws InterruptedException
    {
        if (_receiver.isClosed()) {
            throw new InterruptedException();
        }

        if (!_receiver.commit()) {
            throw new InterruptedException();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doStart()
        throws InterruptedException
    {
        super.doStart();

        _receiver.open();

        getThisLogger()
            .info(StoreMessages.LISTENING_UPDATES, _receiver.getSOMName());
    }

    /** {@inheritDoc}
     */
    @Override
    protected Optional<PointValue> nextUpdate(
            final int limit,
            final boolean wait)
        throws InterruptedException
    {
        final PointValue update;

        if (_updates == null) {
            _updates = _receiver
                .receive(
                    limit,
                    wait? -1: 0);

            if (_updates == null) {
                throw new InterruptedException();
            }

            if (_updates.length == 0) {
                _updates = null;

                return Optional.empty();
            }
        }

        update = (PointValue) _updates[_index++];

        if (_index >= _updates.length) {
            _updates = null;
            _index = 0;
        }

        return Optional.of(update);
    }

    /** The specification of the store's updates listener queue. */
    public static final String QUEUE_PROPERTIES = "som.queue";

    private int _index;
    private SOMReceiver _receiver;
    private Serializable[] _updates;
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
