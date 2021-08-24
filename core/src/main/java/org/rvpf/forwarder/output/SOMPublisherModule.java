/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMPublisherModule.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.forwarder.output;

import java.io.Serializable;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.som.SOMFactory;
import org.rvpf.service.som.SOMPublisher;

/** SOM publisher.
 */
public final class SOMPublisherModule
    extends OutputModule
{
    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp(final KeyedGroups moduleProperties)
    {
        setOutput(new _Publisher());

        return super.setUp(moduleProperties);
    }

    /** The topic properties. */
    public static final String TOPIC_PROPERTIES = "topic";

    /** Publisher.
     */
    private final class _Publisher
        extends AbstractOutput
    {
        /** Constructs an instance.
         */
        _Publisher() {}

        /** {@inheritDoc}
         */
        @Override
        public void close()
        {
            if (_publisher != null) {
                _publisher.close();
            }
            _open = false;
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
        public String getDestinationName()
        {
            return _publisher.getSOMName();
        }

        /** {@inheritDoc}
         */
        @Override
        public String getDisplayName()
        {
            return "SOM publisher ("
                + (_publisher.isServer()? "server": "client") + ")";
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isClosed()
        {
            return _publisher.isClosed();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isOpen()
        {
            return _open;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isReliable()
        {
            return !_publisher.isRemote();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean open()
            throws InterruptedException
        {
            _open = _publisher.open();

            return isOpen();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean output(final Serializable[] messages)
            throws InterruptedException
        {
            if (!_publisher.send(messages)) {
                getThisLogger().warn(ServiceMessages.SERVICE_CLOSED);
                return false;
            }

            if (getTraces().isEnabled()) {
                for (final Serializable message: messages) {
                    getTraces().add(message);
                }
            }

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean setUp(final KeyedGroups moduleProperties)
        {
            if (!super.setUp(moduleProperties)) {
                return false;
            }

            final KeyedGroups topicProperties =
                moduleProperties.getGroup(TOPIC_PROPERTIES);

            if (topicProperties.isMissing()) {
                getThisLogger().error(
                    ServiceMessages.MISSING_PROPERTIES,
                    TOPIC_PROPERTIES);
                return false;
            }

            final SOMFactory factory = new SOMFactory(getConfig());
            final SOMFactory.Topic factoryTopic =
                factory.createTopic(topicProperties);

            _publisher = factoryTopic.createPublisher(false);

            return _publisher != null;
        }

        /** {@inheritDoc}
         */
        @Override
        public void tearDown()
        {
            if (_publisher != null) {
                _publisher.tearDown();
                _publisher = null;
            }

            super.tearDown();
        }

        private boolean _open;
        private SOMPublisher _publisher;
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
