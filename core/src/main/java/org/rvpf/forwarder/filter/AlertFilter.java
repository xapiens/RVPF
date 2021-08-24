/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AlertFilter.java 3947 2019-05-02 17:29:03Z SFB $
 */

package org.rvpf.forwarder.filter;

import java.io.Serializable;

import java.util.Optional;

import org.rvpf.base.UUID;
import org.rvpf.base.alert.Alert;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.forwarder.ForwarderMessages;
import org.rvpf.forwarder.ForwarderModule;

/**
 * Alert filter.
 */
public class AlertFilter
    extends ForwarderFilter.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    public Serializable[] filter(Serializable message)
    {
        if (message instanceof Alert) {
            if (!((Alert) message).addVisit(_mark)) {
                getThisLogger().trace(ForwarderMessages.ALERT_DROPPED, message);
                message = null;
            }
        } else {
            logDropped(
                ForwarderMessages.MESSAGE_CLASS_DROPPED,
                message.getClass().getName());
            message = null;
        }

        return (message != null)? new Serializable[] {message, }: NO_MESSAGES;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(
            final ForwarderModule forwarderModule,
            final KeyedGroups filterProperties)
    {
        if (!super.setUp(forwarderModule, filterProperties)) {
            return false;
        }

        final Optional<UUID> mark = getUUID();

        _mark = mark
            .isPresent()? mark
                .get(): forwarderModule
                    .getConfig()
                    .getService()
                    .getSourceUUID();

        return true;
    }

    private UUID _mark;
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
