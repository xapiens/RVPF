/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMSenderModule.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.forwarder.output;

import java.io.Serializable;

import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.som.SOMFactory;
import org.rvpf.service.som.SOMSender;

/**
 * SOM sender.
 */
public final class SOMSenderModule
    extends OutputModule
{
    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp(final KeyedGroups moduleProperties)
    {
        setOutput(new _Sender());

        return super.setUp(moduleProperties);
    }

    /** The queue properties. */
    public static final String QUEUE_PROPERTIES = "queue";

    /**
     * Sender.
     */
    private final class _Sender
        extends AbstractOutput
    {
        /**
         * Constructs an instance.
         */
        _Sender() {}

        /** {@inheritDoc}
         */
        @Override
        public void close()
        {
            if (_sender != null) {
                _sender.close();
            }

            _open = false;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean commit()
        {
            if (!isAlone()) {
                if (!_sender.commit()) {
                    getThisLogger().warn(ServiceMessages.SERVICE_CLOSED);

                    return false;
                }
            }

            getTraces().commit();

            return super.commit();
        }

        /** {@inheritDoc}
         */
        @Override
        public String getDestinationName()
        {
            return _sender.getSOMName();
        }

        /** {@inheritDoc}
         */
        @Override
        public String getDisplayName()
        {
            return "SOM sender ("
                   + (_sender.isServer()? "server": "client") + ")";
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isClosed()
        {
            return _sender.isClosed();
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
            return !_sender.isRemote();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean open()
        {
            _open = _sender.open();

            return isOpen();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean output(final Serializable[] messages)
        {
            if (!_sender.send(messages, isAlone())) {
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

            final KeyedGroups queueProperties = moduleProperties
                .getGroup(QUEUE_PROPERTIES);

            if (queueProperties.isMissing()) {
                getThisLogger()
                    .error(
                        ServiceMessages.MISSING_PROPERTIES_IN,
                        QUEUE_PROPERTIES,
                        moduleProperties.getName().orElse(null));

                return false;
            }

            final SOMFactory factory = new SOMFactory(getConfig());
            final SOMFactory.Queue factoryQueue = factory
                .createQueue(queueProperties);

            _sender = factoryQueue.createSender(false);

            return _sender != null;
        }

        /** {@inheritDoc}
         */
        @Override
        public void tearDown()
        {
            if (_sender != null) {
                _sender.tearDown();
                _sender = null;
            }

            super.tearDown();
        }

        private boolean _open;
        private SOMSender _sender;
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
