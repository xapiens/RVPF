/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMSubscriberModule.java 3948 2019-05-02 20:37:43Z SFB $
 */

package org.rvpf.forwarder.input;

import java.io.Serializable;

import java.util.Optional;

import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.forwarder.BatchControl;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.som.SOMFactory;
import org.rvpf.service.som.SOMSubscriber;

/**
 * SOM subscriber.
 */
public final class SOMSubscriberModule
    extends InputModule
{
    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp(final KeyedGroups moduleProperties)
    {
        setInput(new _Subscriber());

        return super.setUp(moduleProperties);
    }

    /** The topic properties. */
    public static final String TOPIC_PROPERTIES = "topic";

    private final class _Subscriber
        extends AbstractInput
    {
        /**
         * Constructs an instance.
         */
        _Subscriber() {}

        /** {@inheritDoc}
         */
        @Override
        public void close()
        {
            if (_subscriber != null) {
                _subscriber.close();
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean commit()
        {
            getTraces().commit();

            return super.commit();
        }

        /** {@inheritDoc}
         */
        @Override
        public String getDisplayName()
        {
            return "SOM subscriber ("
                   + (_subscriber.isServer()? "server": "client") + ")";
        }

        /** {@inheritDoc}
         */
        @Override
        public String getSourceName()
        {
            return _subscriber.getSOMName();
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<Serializable[]> input(
                final BatchControl batchControl)
            throws InterruptedException
        {
            final int limit = batchControl.getLimit();
            final Serializable[] messageArray = _subscriber.receive(limit, -1);

            if ((messageArray != null) && getTraces().isEnabled()) {
                for (final Serializable message: messageArray) {
                    getTraces().add(message);
                }
            }

            return Optional.ofNullable(messageArray);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isClosed()
        {
            return _subscriber.isClosed();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isReliable()
        {
            return !_subscriber.isRemote();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean open()
        {
            return _subscriber.open();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean setUp(final KeyedGroups moduleProperties)
        {
            final KeyedGroups topicProperties = moduleProperties
                .getGroup(TOPIC_PROPERTIES);

            if (topicProperties.isMissing()) {
                getThisLogger()
                    .error(
                        ServiceMessages.MISSING_PROPERTIES,
                        TOPIC_PROPERTIES);

                return false;
            }

            final SOMFactory factory = new SOMFactory(getConfig());
            final SOMFactory.Topic factoryTopic = factory
                .createTopic(topicProperties);

            _subscriber = factoryTopic.createSubscriber(false);

            return _subscriber != null;
        }

        /** {@inheritDoc}
         */
        @Override
        public void tearDown()
        {
            if (_subscriber != null) {
                _subscriber.tearDown();
                _subscriber = null;
            }
        }

        private SOMSubscriber _subscriber;
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
