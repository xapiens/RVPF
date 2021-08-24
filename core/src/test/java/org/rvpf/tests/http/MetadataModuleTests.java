/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MetadataModuleTests.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.tests.http;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import java.net.HttpURLConnection;

import java.nio.charset.StandardCharsets;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.tool.Inet;
import org.rvpf.base.tool.Require;
import org.rvpf.config.Config;
import org.rvpf.document.exporter.MetadataExporter;
import org.rvpf.document.loader.MetadataDocumentLoader;
import org.rvpf.document.loader.MetadataDocumentLoaderProxy;
import org.rvpf.http.metadata.MetadataServerModule;
import org.rvpf.metadata.Metadata;
import org.rvpf.tests.TestsMetadataFilter;
import org.rvpf.tests.core.CoreTestsMessages;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Metadata module tests.
 */
public final class MetadataModuleTests
    extends ModuleTests
{
    /**
     * Sets up this.
     *
     * @throws Exception On failure.
     */
    @BeforeClass
    public void setUp()
        throws Exception
    {
        setProperty(Config.SUBSTITUTION_DEFERRED_PROPERTY, "1");

        startServer();
        setProperty(
            _METADATA_SERVER_PROPERTY,
            Inet.LOCAL_HOST + ":" + getListenerPort());
    }

    /**
     * Tears down what has been set up.
     *
     * @throws Exception On failure.
     */
    @AfterClass(alwaysRun = true)
    public void tearDown()
        throws Exception
    {
        stopServer();

        checkAlerts();
        tearDownAlerter();
    }

    /**
     * Tests the metadata loader.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = "testMetadataServer")
    public void testFetchMetadata()
        throws Exception
    {
        final Metadata fetchedMetadata = MetadataDocumentLoader
            .fetchMetadata(
                new TestsMetadataFilter(true),
                Optional.empty(),
                Optional.of(getSourceUUID()),
                Optional.empty());

        Require.notNull(fetchedMetadata);

        final boolean matched = getMetadata().equals(fetchedMetadata);

        if (!matched || getThisLogger().isTraceEnabled()) {
            final Optional<Set<String>> emptyStringSet = Optional
                .of(Collections.<String>emptySet());

            getThisLogger()
                .log(
                    matched? Logger.LogLevel.TRACE: Logger.LogLevel.WARN,
                    CoreTestsMessages.EXPECTED,
                    MetadataExporter
                        .export(
                                getMetadata(),
                                        emptyStringSet,
                                        emptyStringSet,
                                        true)
                        .toString());
            getThisLogger()
                .log(
                    matched? Logger.LogLevel.TRACE: Logger.LogLevel.WARN,
                    CoreTestsMessages.RECEIVED,
                    MetadataExporter
                        .export(
                                fetchedMetadata,
                                        emptyStringSet,
                                        emptyStringSet,
                                        true)
                        .toString());
        }

        if (!matched) {
            Require.failure(CoreTestsMessages.METADATA_MISMATCH.toString());
        }
    }

    /**
     * Tests the metadata server module.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testMetadataServer()
        throws Exception
    {
        final String xml;
        final HttpURLConnection connection;
        final Writer writer;
        final InputStream inputStream;
        final String encoding;
        final BufferedReader reader;
        final StringBuilder stringBuilder = new StringBuilder();
        final Metadata receivedMetadata;
        final MetadataDocumentLoaderProxy documentLoader;

        loadMetadata(true);
        xml = getMetadata()
            .getFilter()
            .getXML(Optional.empty(), Optional.empty());

        connection = openConnection(_CONNECTION_PATH, false);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
        connection
            .setRequestProperty("Content-Length", String.valueOf(xml.length()));
        writer = new OutputStreamWriter(
            connection.getOutputStream(),
            StandardCharsets.UTF_8);
        writer.write(xml.toString());
        writer.close();

        getThisLogger()
            .trace(
                () -> new Message(CoreTestsMessages.REQUEST, xml.toString()));

        inputStream = connection.getInputStream();
        encoding = connection.getContentEncoding();
        reader = new BufferedReader(
            new InputStreamReader(
                inputStream,
                (encoding != null)? encoding: StandardCharsets.UTF_8.name()));

        for (;;) {
            final String line = reader.readLine();

            if (line == null) {
                break;
            }

            stringBuilder.append(line);
            stringBuilder.append('\n');
        }

        reader.close();
        connection.disconnect();

        getThisLogger()
            .trace(
                () -> new Message(
                    CoreTestsMessages.RESPONSE,
                    stringBuilder.toString()));

        if (!getConfig().getBooleanValue(_METADATA_CRYPTED_PROPERTY)) {
            receivedMetadata = new Metadata(new Config(""));
            receivedMetadata.setFilter(getMetadata().getFilter().clone());
            documentLoader = new MetadataDocumentLoaderProxy(receivedMetadata);
            Require.success(documentLoader.read(stringBuilder.toString()));

            final boolean matched = getMetadata().equals(receivedMetadata);

            if (!matched || getThisLogger().isTraceEnabled()) {
                getThisLogger()
                    .log(
                        matched? Logger.LogLevel.TRACE: Logger.LogLevel.WARN,
                        CoreTestsMessages.EXPECTED,
                        getMetadata());
                getThisLogger()
                    .log(
                        matched? Logger.LogLevel.TRACE: Logger.LogLevel.WARN,
                        CoreTestsMessages.RECEIVED,
                        receivedMetadata);
            }

            if (!matched) {
                Require.failure(CoreTestsMessages.METADATA_MISMATCH.toString());
            }
        }
    }

    private static final String _CONNECTION_PATH =
        MetadataServerModule.DEFAULT_PATH + MetadataServerModule.GET_PATH;
    private static final String _METADATA_CRYPTED_PROPERTY =
        "tests.metadata.crypted";
    private static final String _METADATA_SERVER_PROPERTY = "metadata.server";
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
