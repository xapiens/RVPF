/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMNotifier.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.store.server.som;

import java.io.Serializable;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.som.SOMFactory;
import org.rvpf.service.som.SOMSender;
import org.rvpf.store.server.Notifier;
import org.rvpf.store.server.StoreMessages;
import org.rvpf.store.server.StoreServiceAppImpl;

/** SOM notifier.
 */
public final class SOMNotifier
    extends Notifier.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    public synchronized void close()
    {
        final SOMSender sender = _sender;

        if (sender != null) {
            sender.close();

            super.close();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized void commit()
        throws InterruptedException
    {
        if (!isClosed()) {
            final SOMSender sender = _sender;

            if (sender.isClosed()) {
                throw new InterruptedException();
            }

            if (getNoticeCount() > 0) {
                final long mark = System.nanoTime();

                if (!sender.commit()) {
                    throw new InterruptedException();
                }
                addTime(System.nanoTime() - mark);

                getThisLogger().debug(
                    StoreMessages.NOTICES_COMMITTED,
                    String.valueOf(getNoticeCount()));
            }

            super.commit();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final StoreServiceAppImpl storeAppImpl)
    {
        if (!super.setUp(storeAppImpl)) {
            return false;
        }

        final KeyedGroups queueProperties =
            storeAppImpl.getServerProperties()
            .getGroup(QUEUE_PROPERTIES);

        if (queueProperties.isMissing()) {
            getThisLogger().error(
                ServiceMessages.MISSING_PROPERTIES,
                QUEUE_PROPERTIES);
            return false;
        }

        final SOMFactory factory = new SOMFactory(storeAppImpl.getConfig());
        final SOMFactory.Queue factoryQueue =
            factory.createQueue(queueProperties);
        final SOMSender sender = factoryQueue.createSender(false);

        if (sender == null) {
            return false;
        }
        if (sender.isRemote()) {
            getThisLogger().warn(
                ServiceMessages.REMOTE_SERVICE_WARNING,
                sender.toString());
        }
        _sender = sender;

        getThisLogger().debug(ServiceMessages.SET_UP_COMPLETED);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
    {
        _sender.open();

        super.start();
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        final SOMSender sender = _sender;

        if (sender != null) {
            close();
            _sender = null;
            sender.tearDown();
        }

        super.tearDown();

        getThisLogger().debug(ServiceMessages.TEAR_DOWN_COMPLETED);
    }

    /** {@inheritDoc}
     */
    @Override
    protected synchronized boolean doNotify(final PointValue pointValue)
        throws InterruptedException
    {
        final SOMSender sender = _sender;

        if (sender.isClosed()) {
            throw new InterruptedException();
        }

        final long mark = System.nanoTime();

        if (!sender.send(new Serializable[] {pointValue}, false)) {
            throw new InterruptedException();
        }
        addTime(System.nanoTime() - mark);
        getThisLogger().trace(StoreMessages.NOTICE_QUEUED, pointValue);

        return true;
    }

    /** The specification of the store's notifier's queue. */
    public static final String QUEUE_PROPERTIES = "notifier.som.queue";

    private volatile SOMSender _sender;
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
