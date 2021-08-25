/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PAPStoreServiceAppImpl.java 4098 2019-06-25 16:46:46Z SFB $
 */

package org.rvpf.store.server.pap;

import java.util.Optional;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.tool.Require;
import org.rvpf.metadata.Metadata;
import org.rvpf.pap.PAPMessages;
import org.rvpf.pap.PAPSupport;
import org.rvpf.store.server.StoreServiceAppImpl;

/**
 * PAP store service application implementation.
 */
public final class PAPStoreServiceAppImpl
    extends StoreServiceAppImpl
{
    /** {@inheritDoc}
     */
    @Override
    public PAPStoreServer getServer()
    {
        return Require.notNull(_server);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onNewMetadata(final Metadata metadata)
    {
        if (!super.onNewMetadata(metadata)) {
            return false;
        }

        return (_server == null) || _server.acceptMetadata(metadata);
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        stop();

        if (_server != null) {
            _server.tearDown();
            _server = null;
        }

        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean refreshMetadata()
    {
        if (!getService().reloadMetadata()) {
            return false;
        }

        refreshNoticesFilter();

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp()
    {
        if (!super.setUp(SERVER_PROPERTIES)) {
            return false;
        }

        if (isNullRemoves()) {
            getThisLogger().error(PAPMessages.REMOVES_NOT_ALLOWED);

            return false;
        }

        final Optional<String> protocol = getServerProperties()
            .getString(PROTOCOL_PROPERTY);

        if (!protocol.isPresent()) {
            getThisLogger()
                .error(
                    BaseMessages.MISSING_PROPERTY_IN,
                    PROTOCOL_PROPERTY,
                    SERVER_PROPERTIES);
        }

        PAPSupport protocolSupport = null;

        for (final PAPSupport support:
                PAPSupport.getProtocolSupports(getConfigProperties())) {
            if (protocol.get().equalsIgnoreCase(support.getProtocolName())) {
                protocolSupport = support;
            }
        }

        if (protocolSupport == null) {
            getThisLogger().error(PAPMessages.UNKNOWN_PROTOCOL, protocol.get());

            return false;
        }

        _server = new PAPStoreServer(protocolSupport);

        if (!setUpNotifier()) {
            return false;
        }

        if (!_server.setUp(this)) {
            return false;
        }

        if (!_server.acceptMetadata(getMetadata())) {
            return false;
        }

        return registerServer(_server);
    }

    /** The protocol to support. */
    public static final String PROTOCOL_PROPERTY = "protocol";

    /** Server properties. */
    public static final String SERVER_PROPERTIES = "store.server.pap";

    private PAPStoreServer _server;
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
