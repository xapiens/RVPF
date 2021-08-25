/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CIPClientTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.pap.cip;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

import org.rvpf.base.Attributes;
import org.rvpf.base.Origin;
import org.rvpf.base.Point;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.Tuple;
import org.rvpf.config.Config;
import org.rvpf.metadata.Metadata;
import org.rvpf.pap.PAPConnectionListener;
import org.rvpf.pap.PAPProxy;
import org.rvpf.pap.cip.CIP;
import org.rvpf.pap.cip.CIPClient;
import org.rvpf.pap.cip.CIPSupport;
import org.rvpf.pap.cip.transport.ReadTransaction;
import org.rvpf.tests.service.MetadataServiceTests;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * CIP client tests.
 */
public class CIPClientTests
    extends MetadataServiceTests
    implements PAPConnectionListener
{
    /** {@inheritDoc}
     */
    @Override
    public boolean onLostConnection(
            final PAPProxy remoteProxy,
            final Optional<Exception> cause)
    {
        Require.failure(_disconnected);

        _disconnected = true;

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onNewConnection(final PAPProxy remoteProxy)
    {
        Require.failure(_connected);

        _connected = true;

        return true;
    }

    /**
     * Sets up this.
     */
    @BeforeClass
    public void setUp()
    {
        setSystemProperty(Config.RVPF_PROPERTIES, _TESTS_PROPERTIES);

        final CIPSupport support = new CIPSupport();

        _client = support
            .newClient(
                support.newClientContext(getMetadata(true), Optional.empty()));
        _origin = _client.getOrigin(Optional.of(_TESTS_ORIGIN_NAME)).get();
    }

    /**
     * Tests.
     *
     * @throws Exception On failure.
     */
    @Test
    public void test()
        throws Exception
    {
        _client.open();

        Require.success(_client.addConnectionListener(this));
        Require.failure(_connected);
        Require.success(_client.connect(_origin));
        Require.success(_connected);

        final Metadata metadata = getMetadata();
        final Point pointBOOL1 = metadata.getPoint(_TESTS_BOOL_1_POINT).get();
        final Point pointDINT1 = metadata.getPoint(_TESTS_DINT_1_POINT).get();
        final Point pointBOOLArray1 = metadata
            .getPoint(_TESTS_BOOL_ARRAY_1_POINT)
            .get();
        final Point pointDINTArray1 = metadata
            .getPoint(_TESTS_DINT_ARRAY_1_POINT)
            .get();
        final Point pointREALArray1 = metadata
            .getPoint(_TESTS_REAL_ARRAY_1_POINT)
            .get();
        final Point pointINT1 = metadata.getPoint(_TESTS_INT_1_POINT).get();
        final Point pointREAL1 = metadata.getPoint(_TESTS_REAL_1_POINT).get();
        final Point pointSINT1 = metadata.getPoint(_TESTS_SINT_1_POINT).get();
        final Tuple boolTuple = new Tuple();
        final Tuple dintTuple = new Tuple();
        final Tuple realTuple = new Tuple();

        Require.notNull(pointBOOL1);
        Require.notNull(pointDINT1);
        Require.notNull(pointBOOLArray1);
        Require.notNull(pointDINTArray1);
        Require.notNull(pointREALArray1);
        Require.notNull(pointINT1);
        Require.notNull(pointREAL1);
        Require.notNull(pointSINT1);

        final Attributes attributes = pointDINTArray1
            .getAttributes(CIP.ATTRIBUTES_USAGE)
            .get();
        final int elements = attributes.getInt(CIP.ELEMENTS_ATTRIBUTE, 0);

        for (int i = 0; i < elements; ++i) {
            boolTuple.add(Boolean.valueOf((i & 1) != 0));
        }

        for (int i = 0; i < elements; ++i) {
            dintTuple.add(Integer.valueOf(i));
        }

        for (int i = 0; i < elements; ++i) {
            realTuple.add(Float.valueOf((float) (i / 10.0)));
        }

        Require
            .present(
                _client
                    .requestPointUpdate(
                            new PointValue(
                                    pointBOOL1,
                                            Optional.empty(),
                                            null,
                                            Boolean.valueOf(true))));
        Require
            .present(
                _client
                    .requestPointUpdate(
                            new PointValue(
                                    pointDINT1,
                                            Optional.empty(),
                                            null,
                                            Integer.valueOf(1234))));
        Require
            .present(
                _client
                    .requestPointUpdate(
                            new PointValue(
                                    pointBOOLArray1,
                                            Optional.empty(),
                                            null,
                                            boolTuple)));
        Require
            .present(
                _client
                    .requestPointUpdate(
                            new PointValue(
                                    pointDINTArray1,
                                            Optional.empty(),
                                            null,
                                            dintTuple)));
        Require
            .present(
                _client
                    .requestPointUpdate(
                            new PointValue(
                                    pointREALArray1,
                                            Optional.empty(),
                                            null,
                                            realTuple)));
        Require
            .present(
                _client
                    .requestPointUpdate(
                            new PointValue(
                                    pointINT1,
                                            Optional.empty(),
                                            null,
                                            Short.valueOf((short) 123))));
        Require
            .present(
                _client
                    .requestPointUpdate(
                            new PointValue(
                                    pointREAL1,
                                            Optional.empty(),
                                            null,
                                            Float.valueOf((float) 12.34))));
        Require
            .present(
                _client
                    .requestPointUpdate(
                            new PointValue(
                                    pointSINT1,
                                            Optional.empty(),
                                            null,
                                            Byte.valueOf((byte) 12))));
        Require.ignored(_client.commitPointUpdateRequests());

        Require.notNull(_client.requestPointValue(pointBOOL1));
        Require.notNull(_client.requestPointValue(pointDINT1));
        Require.notNull(_client.requestPointValue(pointBOOLArray1));
        Require.notNull(_client.requestPointValue(pointDINTArray1));
        Require.notNull(_client.requestPointValue(pointREALArray1));
        Require.notNull(_client.requestPointValue(pointINT1));
        Require.notNull(_client.requestPointValue(pointREAL1));
        Require.notNull(_client.requestPointValue(pointSINT1));

        final Collection<ReadTransaction.Response> responses = Require
            .notNull(_client.commitPointValueRequests());

        Require.success(responses.size() == 8);

        final Iterator<ReadTransaction.Response> iterator = responses
            .iterator();
        final PointValue pointBOOL1Value = iterator
            .next()
            .getPointValue()
            .get();
        final PointValue pointDINT1Value = iterator
            .next()
            .getPointValue()
            .get();
        final PointValue pointBOOLArray1Value = iterator
            .next()
            .getPointValue()
            .get();
        final PointValue pointDINTArray1Value = iterator
            .next()
            .getPointValue()
            .get();
        final PointValue pointREALArray1Value = iterator
            .next()
            .getPointValue()
            .get();
        final PointValue pointINT1Value = iterator.next().getPointValue().get();
        final PointValue pointREAL1Value = iterator
            .next()
            .getPointValue()
            .get();
        final PointValue pointSINT1Value = iterator
            .next()
            .getPointValue()
            .get();

        Require.equal(pointBOOL1Value.getValue(), Boolean.valueOf(true));
        Require.equal(pointDINT1Value.getValue(), Integer.valueOf(1234));
        Require.equal(pointBOOLArray1Value.getValue(), boolTuple);
        Require.equal(pointDINTArray1Value.getValue(), dintTuple);
        Require.equal(pointREALArray1Value.getValue(), realTuple);
        Require.equal(pointINT1Value.getValue(), Short.valueOf((short) 123));
        Require.equal(pointREAL1Value.getValue(), Float.valueOf((float) 12.34));
        Require.equal(pointSINT1Value.getValue(), Byte.valueOf((byte) 12));

        Require.failure(_disconnected);
        _client.disconnect();
        Require.success(_disconnected);
        Require.success(_client.removeConnectionListener(this));

        _client.close();
    }

    private static final String _TESTS_BOOL_1_POINT = "TESTS-CIP.BOOL.1";
    private static final String _TESTS_BOOL_ARRAY_1_POINT =
        "TESTS-CIP.BOOL.ARRAY.1";
    private static final String _TESTS_DINT_1_POINT = "TESTS-CIP.DINT.1";
    private static final String _TESTS_DINT_ARRAY_1_POINT =
        "TESTS-CIP.DINT.ARRAY.1";
    private static final String _TESTS_INT_1_POINT = "TESTS-CIP.INT.1";
    private static final String _TESTS_ORIGIN_NAME = "TestsCIP";
    private static final String _TESTS_PROPERTIES = "rvpf-cip.properties";
    private static final String _TESTS_REAL_1_POINT = "TESTS-CIP.REAL.1";
    private static final String _TESTS_REAL_ARRAY_1_POINT =
        "TESTS-CIP.REAL.ARRAY.1";
    private static final String _TESTS_SINT_1_POINT = "TESTS-CIP.SINT.1";

    private CIPClient _client;
    private boolean _connected;
    private boolean _disconnected;
    private Origin _origin;
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
