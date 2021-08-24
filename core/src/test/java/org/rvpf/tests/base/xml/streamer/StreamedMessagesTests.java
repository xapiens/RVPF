/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StreamedMessagesTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.base.xml.streamer;

import java.io.Serializable;

import java.util.Optional;

import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.DateTime;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Message;
import org.rvpf.base.security.Crypt;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedValues;
import org.rvpf.base.value.BigRational;
import org.rvpf.base.value.Complex;
import org.rvpf.base.value.Dict;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.Rational;
import org.rvpf.base.value.State;
import org.rvpf.base.value.Tuple;
import org.rvpf.base.xml.streamer.StreamedMessagesConverter;
import org.rvpf.base.xml.streamer.xstream.XStreamStreamer;
import org.rvpf.config.Config;
import org.rvpf.document.loader.ConfigDocumentLoader;
import org.rvpf.tests.Tests;
import org.rvpf.tests.core.CoreTestsMessages;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Streamed messages tests.
 */
public class StreamedMessagesTests
    extends Tests
{
    /**
     * Sets up this.
     */
    @BeforeClass
    public void setUp()
    {
        final Config config = ConfigDocumentLoader
            .loadConfig("", Optional.empty(), Optional.empty());

        Require.notNull(config);

        config.registerClassLoader();
    }

    /**
     * Tests the messages converters.
     */
    @Test
    public void testMessagesConverters()
    {
        _converter = new StreamedMessagesConverter();

        final KeyedValues moduleProperties = new KeyedValues();

        moduleProperties
            .add(
                XStreamStreamer.ANNOTATED_CLASS_PROPERTY,
                _MESSAGE_EXAMPLE_CLASS_NAME);
        Require
            .success(
                _converter
                    .setUp(
                            Optional.empty(),
                                    Optional.of(moduleProperties.freeze())));

        final UUID uuid = UUID.generate();

        _testMessage(uuid);

        final DateTime now = DateTime.now();

        _testMessage(now);

        final Double doubleValue = Double.valueOf(0.1);

        _testMessage(doubleValue);

        final Float floatValue = Float.valueOf(0.1F);

        _testMessage(floatValue);

        final State state = new State(
            Optional.of(Integer.valueOf(1)),
            Optional.of("On"));

        _testMessage(state);

        final Complex complex = Complex.cartesian(1.0, 2.0);

        _testMessage(complex);

        final Rational rational = Rational.valueOf(1, 2);

        _testMessage(rational);

        final BigRational bigRational = BigRational.valueOf(1, 2);

        _testMessage(bigRational);

        final Tuple tuple = new Tuple();

        _testMessage(tuple);
        tuple.add(uuid);
        tuple.add(now);
        tuple.add(doubleValue);
        tuple.add(floatValue);
        tuple.add(state);
        tuple.add(complex);
        tuple.add(rational);
        tuple.add(bigRational);
        _testMessage(tuple);

        final Dict dict = new Dict();

        _testMessage(dict);
        dict.put("UUID", uuid);
        dict.put("DateTime", now);
        dict.put("Tuple", tuple);
        _testMessage(dict);

        final Serializable example = new ClassDefImpl(
            _MESSAGE_EXAMPLE_CLASS_NAME)
            .createInstance(Serializable.class);

        _testMessage(example);

        PointValue pointValue = new PointValue(
            "Test",
            Optional.of(now),
            State.fromString("Set"),
            null);

        _testMessage(pointValue);
        pointValue = pointValue.copy();
        pointValue.setState(null);
        pointValue.setValue(doubleValue);
        _testMessage(pointValue);
        pointValue = pointValue.copy();
        pointValue.setValue(tuple);
        _testMessage(pointValue);
        pointValue = pointValue.copy();
        pointValue.setValue(dict);
        _testMessage(pointValue);

        final Serializable encrypted = Crypt.newEncrypted("ENCRYPTED");

        _testMessage(encrypted);

        final Serializable signed = Crypt.newSigned(encrypted, "SIGNATURE");

        _testMessage(signed);

        _converter.tearDown();
    }

    private void _testMessage(final Serializable message)
    {
        final String xml = _converter.toXMLString(message);

        Require.notNull(xml);
        getThisLogger()
            .trace(() -> new Message(CoreTestsMessages.XML, xml.trim()));

        final Serializable restoredMessage = _converter
            .fromXMLString(xml)
            .get();

        Require.equal(restoredMessage, message);
        Require.equal(restoredMessage.getClass(), message.getClass());

        if (message instanceof PointValue) {
            final Serializable originalValue = ((PointValue) message)
                .getValue();

            if (originalValue != null) {
                final Serializable restoredValue =
                    ((PointValue) restoredMessage)
                        .getValue();

                Require
                    .equal(restoredValue.getClass(), originalValue.getClass());
            }
        }
    }

    private static final String _MESSAGE_EXAMPLE_CLASS_NAME =
        "org.rvpf.tests.example.MessageExample";

    private StreamedMessagesConverter _converter;
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
