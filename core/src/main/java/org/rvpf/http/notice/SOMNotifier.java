/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMNotifier.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.http.notice;

import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.som.QueueProxy;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.service.ServiceMessages;

/**
 * SOM notifier.
 */
public final class SOMNotifier
    extends NoticeContext
{
    /** {@inheritDoc}
     */
    @Override
    public void notify(final PointValue[] notices)
        throws InterruptedException
    {
        try {
            _sender.send(notices, true);
        } catch (final ServiceClosedException exception) {
            final InterruptedException interruptedException =
                new InterruptedException();

            interruptedException.initCause(exception);

            throw interruptedException;
        } catch (final SessionException exception) {
            throw new RuntimeException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(
            final Metadata metadata,
            final KeyedGroups contextProperties)
    {
        if (!super.setUp(metadata, contextProperties)) {
            return false;
        }

        final KeyedGroups queueProperties = contextProperties
            .getGroup(QUEUE_PROPERTIES);

        if (queueProperties.isMissing()) {
            getThisLogger()
                .error(ServiceMessages.MISSING_PROPERTIES, QUEUE_PROPERTIES);
        }

        final QueueProxy.Sender sender = QueueProxy.Sender
            .newBuilder()
            .prepare(
                metadata.getProperties(),
                queueProperties,
                metadata.getServiceName(),
                getThisLogger())
            .setAutoconnect(true)
            .build();

        if (sender == null) {
            return false;
        }

        _sender = sender;

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        final QueueProxy.Sender sender = _sender;

        if (sender != null) {
            sender.disconnect();
            _sender = null;
        }

        super.tearDown();
    }

    /** The queue properties. */
    public static final String QUEUE_PROPERTIES = "som.queue";

    private volatile QueueProxy.Sender _sender;
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
