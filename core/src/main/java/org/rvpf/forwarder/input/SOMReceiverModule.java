/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMReceiverModule.java 3948 2019-05-02 20:37:43Z SFB $
 */

package org.rvpf.forwarder.input;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.forwarder.BatchControl;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.som.SOMFactory;
import org.rvpf.service.som.SOMReceiver;

/**
 * SOM receiver.
 */
public final class SOMReceiverModule
    extends InputModule
{
    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp(final KeyedGroups moduleProperties)
    {
        setInput(new _Receiver());

        return super.setUp(moduleProperties);
    }

    /** The queue properties. */
    public static final String QUEUE_PROPERTIES = "queue";

    private final class _Receiver
        extends AbstractInput
    {
        /**
         * Constructs an instance.
         */
        _Receiver() {}

        /** {@inheritDoc}
         */
        @Override
        public void close()
        {
            if (_receiver != null) {
                _receiver.close();
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean commit()
        {
            final boolean success = _receiver.commit();

            getTraces().commit();

            return success && super.commit();
        }

        /** {@inheritDoc}
         */
        @Override
        public String getDisplayName()
        {
            return "SOM receiver ("
                   + (_receiver.isServer()? "server": "client") + ")";
        }

        /** {@inheritDoc}
         */
        @Override
        public String getSourceName()
        {
            return _receiver.getSOMName();
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<Serializable[]> input(final BatchControl batchControl)
        {
            final int limit = batchControl.getLimit();
            final Optional<ElapsedTime> wait = batchControl.getWait();
            final List<Serializable[]> messageArrayList =
                new LinkedList<Serializable[]>();
            int count = 0;
            long timeout = -1;
            Serializable[] messageArray;

            for (;;) {
                messageArray = _receiver.receive(limit - count, timeout);

                if (messageArray == null) {
                    return Optional.empty();
                }

                if (messageArray.length == 0) {
                    break;
                }

                messageArrayList.add(messageArray);
                count += messageArray.length;

                if (!wait.isPresent() || (count >= limit)) {
                    break;
                }

                timeout = wait.get().toMillis();
            }

            if (messageArrayList.size() > 1) {
                final Iterator<Serializable[]> iterator = messageArrayList
                    .iterator();
                final List<Serializable> messageList =
                    new ArrayList<Serializable>(
                        count);

                do {
                    messageList.addAll(Arrays.asList(iterator.next()));
                } while (iterator.hasNext());

                messageArray = messageList
                    .toArray(new Serializable[messageList.size()]);
            } else {
                messageArray = messageArrayList.get(0);
            }

            if (getTraces().isEnabled()) {
                for (final Serializable message: messageArray) {
                    getTraces().add(message);
                }
            }

            return Optional.of(messageArray);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isClosed()
        {
            return _receiver.isClosed();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isReliable()
        {
            return !_receiver.isRemote();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean open()
        {
            return _receiver.open();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean rollback()
        {
            return _receiver.rollback();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean setUp(final KeyedGroups moduleProperties)
        {
            final KeyedGroups queueProperties = moduleProperties
                .getGroup(QUEUE_PROPERTIES);

            if (queueProperties.isMissing()) {
                getThisLogger()
                    .error(
                        ServiceMessages.MISSING_PROPERTIES,
                        QUEUE_PROPERTIES);

                return false;
            }

            final SOMFactory factory = new SOMFactory(getConfig());
            final SOMFactory.Queue factoryQueue = factory
                .createQueue(queueProperties);

            _receiver = factoryQueue.createReceiver(false);

            return _receiver != null;
        }

        /** {@inheritDoc}
         */
        @Override
        public void tearDown()
        {
            if (_receiver != null) {
                _receiver.tearDown();
                _receiver = null;
            }
        }

        private SOMReceiver _receiver;
    }
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
