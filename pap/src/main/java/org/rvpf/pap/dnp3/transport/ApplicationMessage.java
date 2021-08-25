/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ApplicationMessage.java 4085 2019-06-16 15:17:12Z SFB $
 */

package org.rvpf.pap.dnp3.transport;

import java.io.IOException;
import java.io.InterruptedIOException;

import java.util.LinkedList;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;
import org.rvpf.pap.dnp3.DNP3Messages;
import org.rvpf.pap.dnp3.DNP3ProtocolException;

/**
 * Application message.
 */
public class ApplicationMessage
{
    /**
     * Constructs an instance.
     *
     * @param association The association.
     * @param functionCode An optional function code.
     * @param fromMaster True if from master.
     */
    public ApplicationMessage(
            @Nonnull final Association association,
            @Nonnull final Optional<FunctionCode> functionCode,
            final boolean fromMaster)
    {
        _association = association;
        _header = Fragment.Header.newInstance(functionCode, fromMaster);
    }

    /**
     * Adds an item.
     *
     * @param item The item.
     */
    @CheckReturnValue
    public final void add(@Nonnull final Fragment.Item item)
    {
        _items.add(item);
    }

    /**
     * Receives this message from an association.
     *
     * @throws IOException On I/O exception.
     */
    public final void receive()
        throws IOException
    {
        final ApplicationLayer applicationLayer = _association
            .getApplicationLayer();
        boolean needsFirst = true;

        for (;;) {
            final Fragment fragment = applicationLayer.receive();

            if (fragment.isFirst() != needsFirst) {
                throw new DNP3ProtocolException(
                    DNP3Messages.UNEXPECTED_FRAGMENT,
                    _association);
            }

            needsFirst = false;

            Require
                .ignored(
                    _association
                        .getConnectionManager()
                        .onReceivedFragment(_association, fragment));

            if (fragment.isLast()) {
                break;
            }
        }
    }

    /**
     * Sends this message via an association.
     *
     * @return False for a request with unsent items.
     *
     * @throws IOException On I/O exception.
     */
    @CheckReturnValue
    public final boolean send()
        throws IOException
    {
        _header.reset();
        _header.setFirst();

        final ApplicationLayer applicationLayer = _association
            .getApplicationLayer();

        synchronized (_association) {
            Fragment fragment = new Fragment(_association, _header);

            for (;;) {
                if (_items.isEmpty()) {
                    _header.setLast();
                    fragment.send();

                    break;
                }

                final boolean added = fragment.add(_items.getFirst());

                if (added) {
                    _items.removeFirst();
                } else {
                    if (_items.isEmpty()) {
                        return false;
                    }

                    if (_header.isConfirmRequested()) {
                        _association.expectConfirm();
                    }

                    fragment.send();

                    if (_header.isInRequest()) {
                        return false;
                    }

                    if (_header.isConfirmRequested()) {
                        try {
                            _association.waitForConfirm();
                        } catch (final InterruptedException exception) {
                            throw new InterruptedIOException();
                        }
                    }

                    _header.reset();
                    _header
                        .setSequence(
                            _header.isUnsolicited()? applicationLayer
                                .nextUnsolicitedSequence(): applicationLayer
                                        .nextSolicitedSequence());
                    fragment = new Fragment(_association, _header);
                }
            }
        }

        return true;
    }

    /**
     * Requests a confirm message.
     */
    public void setConfirmRequested()
    {
        _header.setConfirmRequested();
    }

    /**
     * Sets the internal indications.
     *
     * @param internalIndications The internal indications.
     *
     * @throws ClassCastException When called on a request.
     */
    public void setInternalIndications(
            @Nonnull final InternalIndications internalIndications)
        throws ClassCastException
    {
        ((Fragment.Header.Response) _header)
            .setInternalIndications(internalIndications);
    }

    /**
     * Sets the first fragment sequence.
     *
     * @param sequence The sequence.
     */
    public void setSequence(final byte sequence)
    {
        _header.setSequence(sequence);
    }

    /**
     * Sets the unsolicited indicator.
     */
    public void setUnsolicited()
    {
        _header.setUnsolicited();
    }

    private final Association _association;
    private final Fragment.Header _header;
    private final LinkedList<Fragment.Item> _items = new LinkedList<>();
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
