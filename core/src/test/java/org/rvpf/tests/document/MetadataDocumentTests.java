/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MetadataDocumentTests.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.tests.document;

import java.util.Optional;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.tool.Require;
import org.rvpf.config.Config;
import org.rvpf.document.loader.ConfigDocumentLoader;
import org.rvpf.document.loader.MetadataDocumentLoader;
import org.rvpf.document.loader.MetadataDocumentLoaderProxy;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.OriginEntity;
import org.rvpf.tests.Tests;
import org.rvpf.tests.TestsMetadataFilter;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Metadata Document Tests.
 */
public final class MetadataDocumentTests
    extends Tests
{
    /**
     * Sets up the tests.
     */
    @BeforeMethod
    public void setUp()
    {
        final Config config = ConfigDocumentLoader
            .loadConfig("", Optional.empty(), Optional.empty());

        Require.notNull(config);

        _metadata = new Metadata(config);
        _metadata.setFilter(new TestsMetadataFilter(true));
        _fileSpec = config
            .getPropertiesGroup(MetadataDocumentLoader.METADATA_PROPERTIES)
            .getString(
                MetadataDocumentLoader.PATH_PROPERTY,
                Optional.of(MetadataDocumentLoader.DEFAULT_PATH))
            .get();
    }

    /**
     * Tests load invalid metadata.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = {"testLoadValidMetadata"})
    public void testLoadInvalidMetadata()
        throws Exception
    {
        final MetadataDocumentLoaderProxy documentLoader =
            new MetadataDocumentLoaderProxy(
                _metadata);

        documentLoader.setValidating(true);

        expectLogs(BaseMessages.VERBATIM);
        quash(BaseMessages.VERBATIM);

        Require
            .success(
                documentLoader.loadFrom(_TESTS_BAD_METADATA, Optional.empty()));

        requireLogs(BaseMessages.VERBATIM);
    }

    /**
     * Tests load valid metadata.
     */
    @Test
    public void testLoadValidMetadata()
    {
        final MetadataDocumentLoaderProxy documentLoader =
            new MetadataDocumentLoaderProxy(
                _metadata);

        documentLoader.setValidating(true);
        Require.notPresent(_metadata.getStringValue(_TESTS_METADATA_PROPERTY));

        Require.success(documentLoader.loadFrom(_fileSpec, Optional.empty()));

        Require.present(_metadata.getStringValue(_TESTS_METADATA_PROPERTY));

        final Optional<OriginEntity> origin = _metadata
            .getOriginEntity(Optional.of(_TESTS_ORIGIN));

        origin.get().getAttributes(_TESTS_USAGE).get();
    }

    private static final String _TESTS_BAD_METADATA = "rvpf-metadata-bad.xml";
    private static final String _TESTS_METADATA_PROPERTY =
        "tests.metadata.property";
    private static final String _TESTS_ORIGIN = "TestsClock";
    private static final String _TESTS_USAGE = "TESTS-ATTRIBUTES";

    private String _fileSpec;
    private Metadata _metadata;
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
