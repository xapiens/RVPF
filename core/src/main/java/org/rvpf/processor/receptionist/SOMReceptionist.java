/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMReceptionist.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.processor.receptionist;

import java.io.Serializable;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.processor.ProcessorServiceAppImpl;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.som.SOMFactory;
import org.rvpf.service.som.SOMReceiver;

/** SOM receptionist.
 */
public final class SOMReceptionist
    extends Receptionist.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Metadata metadata)
    {
        if (!super.setUp(metadata)) {
            return false;
        }

        final KeyedGroups processorProperties =
            metadata.getPropertiesGroup(
                ProcessorServiceAppImpl.PROCESSOR_PROPERTIES);
        final KeyedGroups queueProperties =
            processorProperties.getGroup(QUEUE_PROPERTIES);

        if (queueProperties.isMissing()) {
            getThisLogger().error(
                ServiceMessages.MISSING_PROPERTIES,
                QUEUE_PROPERTIES);
            return false;
        }

        final SOMFactory factory = new SOMFactory(metadata);
        final SOMFactory.Queue factoryQueue =
            factory.createQueue(queueProperties);

        _receiver = factoryQueue.createReceiver(false);
        if (_receiver == null) {
            return false;
        }
        if (_receiver.isRemote()) {
            getThisLogger().warn(
                ServiceMessages.REMOTE_SERVICE_WARNING,
                _receiver.toString());
        }

        getThisLogger().debug(ServiceMessages.SET_UP_COMPLETED);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        _notices = null;
        _index = 0;

        _receiver.tearDown();

        super.tearDown();

        getThisLogger().debug(ServiceMessages.TEAR_DOWN_COMPLETED);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doClose()
    {
        if (_receiver != null) {
            _receiver.close();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doCommit()
        throws InterruptedException, ServiceNotAvailableException
    {
        if (_receiver.isClosed()) {
            throw new InterruptedException();
        }

        try {
            if (!_receiver.commit()) {
                throw new ServiceNotAvailableException();
            }
        } catch (final Exception exception) {
            throw new ServiceNotAvailableException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doOpen()
    {
        _receiver.open();
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doRollback()
        throws InterruptedException, ServiceNotAvailableException
    {
        if (_receiver.isClosed()) {
            throw new InterruptedException();
        }

        try {
            _receiver.rollback();
        } catch (final Exception exception) {
            throw new ServiceNotAvailableException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected PointValue doFetchNotice(final int limit, final long wait)
        throws ServiceNotAvailableException
    {
        final PointValue notice;

        if (_notices == null) {
            _notices = _receiver.receive(limit, wait);
            if (_notices == null) {
                throw new ServiceNotAvailableException();
            }
            if (_notices.length == 0) {
                _notices = null;
                return null;
            }
        }

        notice = (PointValue) _notices[_index++];
        if (_index >= _notices.length) {
            _notices = null;
            _index = 0;
        }

        return notice;
    }

    /** The reception queue properties. */
    public static final String QUEUE_PROPERTIES = "receptionist.som.queue";

    private int _index;
    private Serializable[] _notices;
    private SOMReceiver _receiver;
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
