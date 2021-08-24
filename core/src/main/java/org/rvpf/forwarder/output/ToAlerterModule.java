/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ToAlerterModule.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.forwarder.output;

import java.io.Serializable;
import org.rvpf.base.alert.Alert;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.service.Service;

/** To alerter module.
 */
public final class ToAlerterModule
    extends OutputModule
{
    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp(final KeyedGroups moduleProperties)
    {
        setOutput(new _AlertSender());

        return super.setUp(moduleProperties);
    }

    /** Alert sender.
     */
    private final class _AlertSender
        extends AbstractOutput
    {
        /** Constructs an instance.
         */
        _AlertSender() {}

        /** {@inheritDoc}
         */
        @Override
        public void close()
        {
            _open = false;
        }

        /** {@inheritDoc}
         */
        @Override
        public String getDestinationName()
        {
            return "Alerter";
        }

        /** {@inheritDoc}
         */
        @Override
        public String getDisplayName()
        {
            return "To Alerter";
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isClosed()
        {
            return !_open;
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
            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean open()
        {
            _open = true;

            return isOpen();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean output(final Serializable[] messages)
        {
            final Service service = getConfig().getService();

            for (final Serializable message: messages) {
                if (message instanceof Alert) {
                    service.sendAlert((Alert) message);
                }
            }

            return true;
        }

        private boolean _open;
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
