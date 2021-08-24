/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: FromAlerterModule.java 4080 2019-06-12 20:21:38Z SFB $
 */

package org.rvpf.forwarder.input;

import java.io.Serializable;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.alert.Alert;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.forwarder.BatchControl;
import org.rvpf.forwarder.ForwarderMessages;
import org.rvpf.service.Alerter;

/**
 * From alerter module.
 */
public final class FromAlerterModule
    extends InputModule
{
    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp(final KeyedGroups moduleProperties)
    {
        setInput(new _AlertReceiver());

        return super.setUp(moduleProperties);
    }

    /**
     * Alert receiver.
     */
    private final class _AlertReceiver
        extends AbstractInput
        implements Alerter.Listener
    {
        /**
         * Constructs an instance.
         */
        _AlertReceiver() {}

        /** {@inheritDoc}
         */
        @Override
        public void close()
        {
            if (_closed.compareAndSet(false, true)) {
                Require
                    .ignored(
                        getConfig().getService().removeAlertListener(this));
                _alerts.clear();
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public String getDisplayName()
        {
            return "From Alerter";
        }

        /** {@inheritDoc}
         */
        @Override
        public String getSourceName()
        {
            return "Alerter";
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<Serializable[]> input(
                final BatchControl batchControl)
            throws InterruptedException
        {
            final int limit = batchControl.getLimit();
            final Optional<ElapsedTime> wait = batchControl.getWait();
            final List<Alert> alertList = new LinkedList<>();
            int count = 0;
            long timeout = -1;

            for (;;) {
                final Alert alert;

                if (timeout < 0) {
                    alert = _alerts.take();
                } else {
                    alert = _alerts.poll(timeout, TimeUnit.MILLISECONDS);

                    if (alert == null) {
                        break;
                    }
                }

                alertList.add(alert);
                ++count;

                if (!wait.isPresent() || (count >= limit)) {
                    break;
                }

                timeout = wait.get().toMillis();
            }

            if (getTraces().isEnabled()) {
                for (final Alert alert: alertList) {
                    getTraces().add(alert);
                }
            }

            return Optional.of(alertList.toArray(new Alert[alertList.size()]));
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isClosed()
        {
            return _closed.get();
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
        public boolean onAlert(final Optional<Alert> alert)
        {
            return _alerts.add(alert.get());
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean open()
        {
            _closed.set(false);

            if (!getConfig().getService().addAlertListener(this)) {
                getThisLogger()
                    .debug(ForwarderMessages.ADD_ALERT_LISTENER_FAILED);

                return false;
            }

            return true;
        }

        private final BlockingQueue<Alert> _alerts =
            new LinkedBlockingQueue<>();
        private final AtomicBoolean _closed = new AtomicBoolean();
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
