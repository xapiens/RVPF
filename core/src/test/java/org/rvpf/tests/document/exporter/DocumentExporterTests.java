/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DocumentExporterTests.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.tests.document.exporter;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.Config;
import org.rvpf.document.exporter.MetadataExporter;
import org.rvpf.document.loader.ConfigDocumentLoader;
import org.rvpf.document.loader.MetadataDocumentLoader;
import org.rvpf.document.loader.MetadataDocumentLoaderProxy;
import org.rvpf.document.loader.MetadataFilter;
import org.rvpf.metadata.Metadata;
import org.rvpf.tests.TestsMetadataFilter;
import org.rvpf.tests.core.CoreTestsMessages;

import org.testng.annotations.Test;

/**
 * Document exporter tests.
 */
public final class DocumentExporterTests
{
    /**
     * Tests the metadata exporter.
     */
    @Test
    public static void testExporter()
    {
        final Config config = ConfigDocumentLoader
            .loadConfig("", Optional.empty(), Optional.empty());

        Require.notNull(config);

        final MetadataFilter filter = new TestsMetadataFilter(true);
        final KeyedGroups metadataProperties = config
            .getPropertiesGroup(MetadataDocumentLoader.METADATA_PROPERTIES);
        final Optional<String> from = metadataProperties
            .getString(
                MetadataDocumentLoader.PATH_PROPERTY,
                Optional.of(MetadataDocumentLoader.DEFAULT_PATH));
        final Metadata oldMetadata;
        final Metadata newMetadata;
        final MetadataDocumentLoaderProxy documentLoader;

        oldMetadata = MetadataDocumentLoader
            .fetchMetadata(filter, Optional.of(config), Optional.empty(), from);
        Require.notNull(oldMetadata);
        Require.success(oldMetadata.adjustPointsLevel());

        final String oldMetadataString = oldMetadata.toString();

        _LOGGER.trace(CoreTestsMessages.EXPORTED, oldMetadataString);

        newMetadata = new Metadata(new Config(""));
        newMetadata.setFilter(filter);
        documentLoader = new MetadataDocumentLoaderProxy(newMetadata);
        Require.success(documentLoader.read(oldMetadataString));
        Require.success(newMetadata.adjustPointsLevel());

        if (_LOGGER.isTraceEnabled()) {
            final Optional<Set<String>> emptyStringSet = Optional
                .of(Collections.<String>emptySet());

            _LOGGER
                .trace(
                    CoreTestsMessages.READ,
                    MetadataExporter
                        .export(
                                newMetadata,
                                        emptyStringSet,
                                        emptyStringSet,
                                        true)
                        .toString());
        }

        if (!oldMetadata.equals(newMetadata)) {
            if (!_LOGGER.isTraceEnabled()) {
                _LOGGER
                    .info(
                        CoreTestsMessages.EXPORTED,
                        oldMetadataString.toString());

                final Optional<Set<String>> emptyStringSet = Optional
                    .of(Collections.<String>emptySet());

                _LOGGER
                    .info(
                        CoreTestsMessages.READ,
                        MetadataExporter
                            .export(
                                    newMetadata,
                                            emptyStringSet,
                                            emptyStringSet,
                                            true)
                            .toString());
            }

            Require.failure("Exported metadata does not match expectations");
        }
    }

    /**
     * Returns self.
     *
     * <p>Avoids 'utility class' warning.</p>
     *
     * @return Self.
     */
    public Object self()
    {
        return this;
    }

    private static final Logger _LOGGER = Logger
        .getInstance(DocumentExporterTests.class);
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
