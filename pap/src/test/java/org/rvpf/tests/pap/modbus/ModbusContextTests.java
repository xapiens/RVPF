/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ModbusContextTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.pap.modbus;

import java.util.Optional;

import org.rvpf.base.Attributes;
import org.rvpf.base.Point;
import org.rvpf.base.tool.Require;
import org.rvpf.config.Config;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.OriginEntity;
import org.rvpf.pap.PAPMessages;
import org.rvpf.pap.modbus.Modbus;
import org.rvpf.pap.modbus.ModbusMessages;
import org.rvpf.pap.modbus.ModbusServerContext;
import org.rvpf.pap.modbus.ModbusSupport;
import org.rvpf.tests.pap.PAPContextTests;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Modbus context tests.
 */
public final class ModbusContextTests
    extends PAPContextTests
{
    /**
     * Constructs an instance.
     */
    public ModbusContextTests()
    {
        super(Modbus.ATTRIBUTES_USAGE);
    }

    /**
     * Sets up the server context.
     */
    @BeforeMethod
    public void setUpServerContext()
    {
        final ModbusSupport support = new ModbusSupport();

        _serverContext = support
            .newServerContext(
                new Metadata(new Config("")),
                new String[0],
                Optional.empty());
        Require.notNull(_serverContext);
    }

    /**
     * Should accept multiple addresses.
     */
    @Test(priority = 40)
    public void shouldAcceptMultipleAddresses()
    {
        final OriginEntity originEntity = newOriginEntity();
        final Attributes originAttributes = getAttributes(originEntity).get();

        // Given an origin with more than one address,
        originAttributes.add(Modbus.SOCKET_ADDRESS_ATTRIBUTE, "127.0.0.1");
        originAttributes.add(Modbus.SOCKET_ADDRESS_ATTRIBUTE, "127.0.0.2");

        // when adding the origin,
        final boolean originAccepted = getServerContext()
            .addRemoteOrigin(originEntity, originAttributes);

        // then it should be accepted.
        Require.success(originAccepted, "origin accepted");
    }

    /**
     * Should accept origins with different addresses.
     */
    @Test(priority = 40)
    public void shouldAcceptOriginsWithDifferentAddresses()
    {
        final OriginEntity firstOriginEntity;
        final Attributes firstOriginAttributes;
        final boolean firstOriginAccepted;
        final OriginEntity secondOriginEntity;
        final Attributes secondOriginAttributes;
        final boolean secondOriginAccepted;

        // Given that two origins have different addresses,
        firstOriginEntity = newOriginEntity();
        firstOriginAttributes = getAttributes(firstOriginEntity).get();
        firstOriginAttributes.add(Modbus.SOCKET_ADDRESS_ATTRIBUTE, "127.0.0.1");
        secondOriginEntity = newOriginEntity();
        secondOriginAttributes = getAttributes(secondOriginEntity).get();
        secondOriginAttributes
            .add(Modbus.SOCKET_ADDRESS_ATTRIBUTE, "127.0.0.2");

        // when adding both origins,
        firstOriginAccepted = getServerContext()
            .addRemoteOrigin(firstOriginEntity, firstOriginAttributes);
        secondOriginAccepted = getServerContext()
            .addRemoteOrigin(secondOriginEntity, secondOriginAttributes);

        // then both should be accepted.
        Require.success(firstOriginAccepted, "first origin accepted");
        Require.success(secondOriginAccepted, "second origin accepted");
    }

    /**
     * Should accept wild node address.
     */
    @Test(priority = 20)
    public void shouldAcceptWildNodeAddress()
    {
        final OriginEntity originEntity;
        final Attributes originAttributes;
        final boolean originAccepted;

        // Given a single origin with a wild address,
        originEntity = newOriginEntity();
        originAttributes = getAttributes(originEntity).get();
        originAttributes.add(Modbus.SOCKET_ADDRESS_ATTRIBUTE, "*");

        // when adding the origin,
        originAccepted = getServerContext()
            .addRemoteOrigin(originEntity, originAttributes);

        // then it should be accepted.
        Require.success(originAccepted, "origin accepted");
    }

    /**
     * Should reject any other address after a wild address.
     */
    @Test(priority = 30)
    public void shouldRejectNodeAddressAfterWild()
    {
        final OriginEntity originEntity;
        final Attributes originAttributes;
        final boolean originAccepted;

        // Given an origin with a wild address followed by any other address,
        originEntity = newOriginEntity();
        originAttributes = getAttributes(originEntity).get();
        originAttributes.add(Modbus.SOCKET_ADDRESS_ATTRIBUTE, "*");
        originAttributes.add(Modbus.SOCKET_ADDRESS_ATTRIBUTE, "127.0.0.1");

        // when adding the origin,
        expectLogs(PAPMessages.WILDCARD_ADDRESS_RESTRICTS);
        originAccepted = getServerContext()
            .addRemoteOrigin(originEntity, originAttributes);

        // then it should be rejected.
        Require.failure(originAccepted, "origin accepted");
        requireLogs(PAPMessages.WILDCARD_ADDRESS_RESTRICTS);
    }

    /**
     * Should reject origins with same address.
     */
    @Test(priority = 30)
    public void shouldRejectOriginsWithSameAddress()
    {
        final OriginEntity firstOriginEntity;
        final Attributes firstOriginAttributes;
        final boolean firstOriginAccepted;
        final OriginEntity otherOriginEntity;
        final Attributes otherOriginAttributes;
        final boolean otherOriginAccepted;

        // Given more than one origin with the same address,
        firstOriginEntity = newOriginEntity();
        firstOriginAttributes = getAttributes(firstOriginEntity).get();
        firstOriginAttributes.add(Modbus.SOCKET_ADDRESS_ATTRIBUTE, "127.0.0.1");
        otherOriginEntity = newOriginEntity();
        otherOriginAttributes = getAttributes(otherOriginEntity).get();
        otherOriginAttributes.add(Modbus.SOCKET_ADDRESS_ATTRIBUTE, "127.0.0.1");

        // when adding the origins,
        firstOriginAccepted = getServerContext()
            .addRemoteOrigin(firstOriginEntity, firstOriginAttributes);
        expectLogs(PAPMessages.AMBIGUOUS_ORIGIN_ADDRESS);
        otherOriginAccepted = getServerContext()
            .addRemoteOrigin(otherOriginEntity, otherOriginAttributes);

        // then the first origin should be accepted
        // but the other one should be rejected.
        Require.success(firstOriginAccepted, "first origin accepted");
        requireLogs(PAPMessages.AMBIGUOUS_ORIGIN_ADDRESS);
        Require.failure(otherOriginAccepted, "other origin accepted");
    }

    /**
     * Should reject any other after a wild origin.
     */
    @Test(priority = 30)
    public void shouldRejectOtherAfterWildOrigin()
    {
        final OriginEntity firstOriginEntity;
        final Attributes firstOriginAttributes;
        final boolean firstOriginAccepted;
        final OriginEntity otherOriginEntity;
        final Attributes otherOriginAttributes;
        final boolean otherOriginAccepted;

        // Given a first origin with a wild address
        firstOriginEntity = newOriginEntity();
        firstOriginAttributes = getAttributes(firstOriginEntity).get();
        firstOriginAttributes.add(Modbus.SOCKET_ADDRESS_ATTRIBUTE, "*");

        // and any other origin,
        otherOriginEntity = newOriginEntity();
        otherOriginAttributes = getAttributes(otherOriginEntity).get();

        // when adding the origins,
        firstOriginAccepted = getServerContext()
            .addRemoteOrigin(firstOriginEntity, firstOriginAttributes);
        expectLogs(PAPMessages.WILDCARD_ADDRESS_RESTRICTS);
        otherOriginAccepted = getServerContext()
            .addRemoteOrigin(otherOriginEntity, otherOriginAttributes);

        // then the first origin should be accepted
        // but the other one should be rejected.
        Require.success(firstOriginAccepted, "first origin accepted");
        Require.failure(otherOriginAccepted, "other origin accepted");
        requireLogs(PAPMessages.WILDCARD_ADDRESS_RESTRICTS);
    }

    /**
     * Should reject a wild address after the first origin.
     */
    @Test(priority = 30)
    public void shouldRejectWildAfterFirstOrigin()
    {
        final OriginEntity firstOriginEntity;
        final Attributes firstOriginAttributes;
        final boolean firstOriginAccepted;
        final OriginEntity otherOriginEntity;
        final Attributes otherOriginAttributes;
        final boolean otherOriginAccepted;

        // Given an origin with any first address
        // and an other origin with a wild address,
        firstOriginEntity = newOriginEntity();
        firstOriginAttributes = getAttributes(firstOriginEntity).get();
        firstOriginAttributes.add(Modbus.SOCKET_ADDRESS_ATTRIBUTE, "127.0.0.1");
        otherOriginEntity = newOriginEntity();
        otherOriginAttributes = getAttributes(otherOriginEntity).get();
        otherOriginAttributes.add(Modbus.SOCKET_ADDRESS_ATTRIBUTE, "*");

        // when adding the origins,
        firstOriginAccepted = getServerContext()
            .addRemoteOrigin(firstOriginEntity, firstOriginAttributes);
        expectLogs(PAPMessages.WILDCARD_ADDRESS_RESTRICTS);
        otherOriginAccepted = getServerContext()
            .addRemoteOrigin(otherOriginEntity, otherOriginAttributes);

        // then the first origin should be accepted
        // but the other one should be rejected.
        Require.success(firstOriginAccepted, "first origin accepted");
        Require.failure(otherOriginAccepted, "other origin accepted");
        requireLogs(PAPMessages.WILDCARD_ADDRESS_RESTRICTS);
    }

    /**
     * Should reject a wild address after any other.
     */
    @Test(priority = 30)
    public void shouldRejectWildAfterNodeAddress()
    {
        final OriginEntity originEntity;
        final Attributes originAttributes;
        final boolean originAccepted;

        // Given an origin with any first address
        // and an other address which is wild,
        originEntity = newOriginEntity();
        originAttributes = getAttributes(originEntity).get();
        originAttributes.add(Modbus.SOCKET_ADDRESS_ATTRIBUTE, "127.0.0.1");
        originAttributes.add(Modbus.SOCKET_ADDRESS_ATTRIBUTE, "*");

        // when adding the origin,
        expectLogs(PAPMessages.WILDCARD_ADDRESS_RESTRICTS);
        originAccepted = getServerContext()
            .addRemoteOrigin(originEntity, originAttributes);

        // then it should be rejected.
        Require.failure(originAccepted, "origin accepted");
        requireLogs(PAPMessages.WILDCARD_ADDRESS_RESTRICTS);
    }

    /**
     * Should require an address for a point.
     */
    @Test(priority = 130)
    public void shouldRequirePointAddress()
    {
        final OriginEntity originEntity;
        final Attributes originAttributes;
        final Point point;
        final boolean pointAccepted;

        // Given a point with a known origin
        // and without an address,
        originEntity = newOriginEntity();
        originAttributes = getAttributes(originEntity).get();
        originAttributes.add(Modbus.SOCKET_ADDRESS_ATTRIBUTE, "127.0.0.1");
        Require
            .success(
                getServerContext()
                    .addRemoteOrigin(originEntity, originAttributes));
        point = newPoint(Optional.of(originEntity));

        // when adding the point,
        expectLogs(ModbusMessages.NO_ADDRESS);
        pointAccepted = getServerContext()
            .addRemotePoint(point, getAttributes(point).get());

        // then it should be rejected.
        Require.failure(pointAccepted, "point accepted");
        requireLogs(ModbusMessages.NO_ADDRESS);
    }

    /** {@inheritDoc}
     */
    @Override
    protected ModbusServerContext getServerContext()
    {
        return _serverContext;
    }

    private ModbusServerContext _serverContext;
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
