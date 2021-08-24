/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: UUIDTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.base;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Arrays;
import java.util.Optional;

import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.tests.core.CoreTestsMessages;

import org.testng.annotations.Test;

/**
 * UUID tests.
 */
public final class UUIDTests
{
    private UUIDTests() {}

    /**
     * Should detect a flipped bit in 'equals'.
     */
    @Test(priority = 30)
    public static void shouldDetectFlippedBitInEquals()
    {
        final UUID uuid;
        final UUID clone;

        // Given a generated UUID,
        uuid = UUID.generate();

        // when generating a clone with a bit flipped,
        clone = UUID
            .fromLongs(
                uuid.getMostSignificantBits() ^ 1,
                uuid.getLeastSignificantBits());

        // then the UUID should not see itself equal to the clone.
        Require.failure(uuid.equals(clone), "equal to bad clone");
    }

    /**
     * Should generate correct URN.
     */
    @Test(priority = 50)
    public static void shouldGenerateCorrectURN()
    {
        final UUID sut;
        final String urn;

        // Given a generated UUID,
        sut = UUID.generate();

        // when generating a URN,
        urn = sut.toURN();

        // then it must conform to the expected pattern.
        Require.equal(urn, "urn:uuid:" + sut.toString(), "generated URN");
    }

    /**
     * Should recognize UUID strings.
     */
    @Test(priority = 10)
    public static void shouldRecognizeStrings()
    {
        final UUID uuid;
        final String rawString;
        final String string;

        // Given a generated UUID providing a raw string
        uuid = UUID.generate();
        rawString = uuid.toRawString();

        // and a normal (decorated) string,
        string = uuid.toString();

        // then these strings should be recognized as UUID.
        Require.success(UUID.isUUID(rawString), "raw string recognized");
        Require.success(UUID.isUUID(string), "string recognized");
    }

    /**
     * Should serialize thru bytes.
     */
    @Test(priority = 20)
    public static void shouldSerializeThruBytes()
    {
        final UUID uuid;
        final byte[] uuidBytes;
        final UUID clone;

        // Given a generated UUID and its byte array,
        uuid = UUID.generate();
        uuidBytes = uuid.toBytes();

        // when creating a clone thru the UUID bytes,
        clone = UUID.fromBytes(uuidBytes);

        // then the UUID byte array should have the correct length
        Require
            .success(uuidBytes.length == UUID.BYTES_LENGTH, "bytes length");

        // and both byte arrays should be equal
        Require
            .success(
                Arrays.equals(uuidBytes, clone.toBytes()),
                "byte arrays equal");

        // and the original and the clone should be equal.
        Require.equal(clone, uuid, "clone from bytes");
    }

    /**
     * Should serialize thru longs.
     */
    @Test(priority = 20)
    public static void shouldSerializeThruLongs()
    {
        final UUID original;
        final UUID clone;

        // Given a generated UUID,
        original = UUID.generate();

        // when creating a clone thru the UUID longs,
        clone = UUID
            .fromLongs(
                original.getMostSignificantBits(),
                original.getLeastSignificantBits());

        // then the original and the clone should be equal.
        Require.equal(clone, original, "clone from longs");
    }

    /**
     * Should serialize thru name.
     */
    @Test(priority = 20)
    public static void shouldSerializeThruName()
    {
        final UUID original;
        final String name;
        final UUID clone;

        // Given a generated UUID,
        original = UUID.generate();

        // when producing a name with the UUID
        name = original.toName();

        // and creating a clone thru that name,
        clone = UUID.fromName(name);

        // then the length of the name should be correct
        Require.success(name.length() == UUID.NAME_LENGTH);

        // and the original and the clone should be equal.
        Require.equal(clone, original);
    }

    /**
     * Should serialize thru string.
     */
    @Test(priority = 20)
    public static void shouldSerializeThruString()
    {
        final UUID original;
        final String string;
        final UUID clone;

        // Given a generated UUID,
        original = UUID.generate();

        // when producing a string representing the UUID
        string = original.toString();

        // and using it to create a clone of the UUID,
        clone = Require.notNull(UUID.fromString(string)).get();

        // then the length of the string should be correct
        Require
            .success(
                clone.toString().length() == UUID.STRING_LENGTH,
                "string length");

        // and the original and the clone should be equal.
        Require.equal(clone, original, "clone from string");
    }

    /**
     * Should synthesize.
     */
    @Test(priority = 40)
    public static void shouldSynthesize()
    {
        final long high = 0x112233445566L;
        final int midHigh = 0xF2;
        final int midLow = 0xF3;
        final long low = 0xAABBCCDDEEL;
        final UUID synthesized;

        // Given a synthesized UUID,
        synthesized = UUID.synthesize(high, midHigh, midLow, low);

        // then its whole and its part should correspond to its inputs.
        Require
            .equal(
                synthesized.toString(),
                "11223344-5566-40f2-80f3-01aabbccddee",
                "synthesized string");
        Require.success(synthesized.getHigh() == high, "synthesized high");
        Require
            .success(
                synthesized.getMidHigh() == midHigh,
                "synthesized mid high");
        Require
            .success(
                synthesized.getMidLow() == midLow,
                "synthesized mid low");
        Require.success(synthesized.getLow() == low, "synthesized low");
    }

    /**
     * Should use the host MAC address consistently.
     */
    @Test(priority = 60)
    public static void shouldUseMACAddressConsistently()
    {
        final Optional<byte[]> nodeBytes;
        final boolean usesMACAddress;
        final UUID original;

        // Given the MAC address bytes (if available)
        nodeBytes = UUID.getNode();
        usesMACAddress = nodeBytes.isPresent();
        Logger
            .getInstance(UUIDTests.class)
            .debug(CoreTestsMessages.USES_MAC, Boolean.valueOf(usesMACAddress));

        // and a generated UUID,
        original = UUID.generate();

        // then the MAC address bytes should be the node part of the UUID
        if (nodeBytes.isPresent()) {
            final byte[] uuidBytes = original.toBytes();

            for (int i = 0; i < nodeBytes.get().length; ++i) {
                Require
                    .equal(
                        Byte.valueOf(uuidBytes[10 + i]),
                        Byte.valueOf(nodeBytes.get()[i]),
                        "a MAC address byte");
            }

            final UUID clone = UUID
                .generate(original.toRawString().substring(0, 20));

            Require.equal(clone, original, "clone");
        }

        // and the random node bit should reflect its availability.
        Require
            .success(
                usesMACAddress != original.isRandomNode(),
                "uses MAC address consistent");
    }

    /**
     * Tests the serialization.
     *
     * @throws Exception On failure.
     */
    @Test
    public static void testSerialization()
        throws Exception
    {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final ObjectOutputStream objectOutputStream = new ObjectOutputStream(
            outputStream);
        final UUID localUUID = UUID.generate();

        objectOutputStream.writeObject(localUUID);
        objectOutputStream.close();

        final InputStream inputStream = new ByteArrayInputStream(
            outputStream.toByteArray());
        final ObjectInputStream objectInputStream = new ObjectInputStream(
            inputStream);
        final UUID remoteUUID = (UUID) objectInputStream.readObject();

        Require.equal(remoteUUID, localUUID);
        objectInputStream.close();
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
