/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.service.pap.datalogger;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.document.loader.MetadataFilter;
import org.rvpf.pap.PAPMetadataFilter;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.metadata.MetadataService;
import org.rvpf.service.pap.PAPServiceAppImpl;

/**
 * Datalogger application implementation.
 */
public final class DataloggerAppImpl
    extends PAPServiceAppImpl
{
    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final MetadataService service)
    {
        if (!super.setUp(service)) {
            return false;
        }

        final KeyedGroups dataloggerProperties = getConfigProperties()
            .getGroup(DATALOGGER_PROPERTIES);

        if (dataloggerProperties.isMissing()) {
            getThisLogger()
                .error(
                    ServiceMessages.MISSING_PROPERTIES,
                    DATALOGGER_PROPERTIES);

            return false;
        }

        if (!_setUpScanners(dataloggerProperties)) {
            return false;
        }

        if (!_setUpOutput(dataloggerProperties)) {
            return false;
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
    {
        for (final Scanner scanner: _scanners) {
            scanner.start();
        }

        _output.start();
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        final long joinTimeout = getJoinTimeout();

        for (final Scanner scanner: _scanners) {
            scanner.stop(joinTimeout);
        }

        _output.stop(joinTimeout);
    }

    /** {@inheritDoc}
     */
    @Override
    protected MetadataFilter getMetadataFilter()
    {
        if (_metadataFilter == null) {
            _metadataFilter = new PAPMetadataFilter(
                getProtocolAttributesUsages())
            {
                /** {@inheritDoc}
                 */
                @Override
                public boolean areContentsRequired()
                {
                    return false;
                }

                /** {@inheritDoc}
                 */
                @Override
                public boolean arePointInputsNeeded()
                {
                    return true;
                }

                /** {@inheritDoc}
                 */
                @Override
                public boolean areStoresNeeded()
                {
                    return true;
                }

                /** {@inheritDoc}
                 */
                @Override
                public boolean areStoresRequired()
                {
                    return false;
                }
            };
        }

        return _metadataFilter;
    }

    /**
     * Gets the logger.
     *
     * @return The logger.
     */
    @Nonnull
    @CheckReturnValue
    Logger getLogger()
    {
        return getThisLogger();
    }

    /**
     * Limits updates.
     *
     * @throws InterruptedException When interrupted.
     */
    void limitUpdates()
        throws InterruptedException
    {
        _output.limitUpdates();
    }

    /**
     * Sends updates.
     *
     * @param pointValues The updates.
     */
    void sendUpdates(@Nonnull final Collection<PointValue> pointValues)
    {
        _output.sendUpdates(pointValues);
    }

    /**
     * Storage monitor check.
     *
     * @return True unless on alert.
     */
    @CheckReturnValue
    boolean storageMonitorCheck()
    {
        return _output.storageMonitorCheck();
    }

    private boolean _setUpOutput(final KeyedGroups dataloggerProperties)
    {
        final KeyedGroups outputProperties = dataloggerProperties
            .getGroup(OUTPUT_PROPERTIES);

        if (outputProperties.isMissing()) {
            getThisLogger()
                .error(
                    ServiceMessages.MISSING_PROPERTIES_IN,
                    OUTPUT_PROPERTIES,
                    dataloggerProperties.getName().orElse(null));

            return false;
        }

        final Output.Builder outputBuilder = Output
            .newBuilder()
            .setDataloggerApp(this)
            .applyProperties(outputProperties);

        _output = (outputBuilder != null)? outputBuilder.build(): null;

        return _output != null;
    }

    private boolean _setUpScanners(final KeyedGroups dataloggerProperties)
    {
        final KeyedGroups[] scannersProperties = dataloggerProperties
            .getGroups(SCANNER_PROPERTIES);

        if (scannersProperties.length == 0) {
            getThisLogger()
                .error(
                    ServiceMessages.MISSING_PROPERTIES_IN,
                    SCANNER_PROPERTIES,
                    dataloggerProperties.getName().orElse(null));

            return false;
        }

        for (final KeyedGroups scannerProperties: scannersProperties) {
            final Scanner.Builder scannerBuilder = Scanner
                .newBuilder()
                .setDataloggerApp(this);

            if (scannerBuilder.applyProperties(scannerProperties) == null) {
                return false;
            }

            _scanners.add(scannerBuilder.build());
        }

        return true;
    }

    /** Datalogger properties. */
    public static final String DATALOGGER_PROPERTIES = "datalogger";

    /** Output properties. */
    public static final String OUTPUT_PROPERTIES = "output";

    /** Scanner properties. */
    public static final String SCANNER_PROPERTIES = "scanner";

    private volatile MetadataFilter _metadataFilter;
    private Output _output;
    private final List<Scanner> _scanners = new LinkedList<>();
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
