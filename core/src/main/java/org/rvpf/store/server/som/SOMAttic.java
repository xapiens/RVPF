/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMAttic.java 4101 2019-06-30 14:56:50Z SFB $
 */

package org.rvpf.store.server.som;

import java.io.File;

import java.util.Collection;
import java.util.Optional;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.config.Config;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.som.SOMFactory;
import org.rvpf.service.som.SOMSender;
import org.rvpf.som.queue.FilesQueue;
import org.rvpf.som.queue.QueueServerImpl;
import org.rvpf.store.server.StoreMessages;
import org.rvpf.store.server.archiver.Archiver;

/**
 * SOM attic.
 */
public class SOMAttic
    implements Archiver.Attic
{
    /** {@inheritDoc}
     */
    @Override
    public void commit()
    {
        Require.success(_sender.commit());
    }

    /** {@inheritDoc}
     */
    @Override
    public void put(final Collection<PointValue> archivedValues)
    {
        Require
            .success(
                _sender
                    .send(
                            archivedValues
                                    .toArray(new PointValue[archivedValues.size()]),
                                    false));
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(
            final KeyedGroups atticProperties,
            final Config config,
            final File dataDir)
    {
        KeyedGroups queueProperties = atticProperties
            .getGroup(QUEUE_PROPERTIES);

        if (queueProperties.isMissing()) {
            queueProperties = queueProperties.copy();
            queueProperties
                .setValue(
                    QueueServerImpl.NAME_PROPERTY,
                    atticProperties
                        .getString(
                                NAME_PROPERTY,
                                        Optional.of(DEFAULT_ATTIC_NAME))
                        .get());
            queueProperties.setValue(SOMFactory.PRIVATE_PROPERTY, Boolean.TRUE);
            queueProperties
                .setValue(FilesQueue.ROOT_PROPERTY, dataDir.getAbsolutePath());
            _setPropertiesValue(
                queueProperties,
                FilesQueue.DIRECTORY_PROPERTY,
                atticProperties.getString(DIRECTORY_PROPERTY));
            _setPropertiesValue(
                queueProperties,
                FilesQueue.COMPRESSED_PROPERTY,
                atticProperties.getString(COMPRESSED_PROPERTY));
            queueProperties.freeze();
        }

        final SOMFactory factory = new SOMFactory(config);
        final SOMFactory.Queue factoryQueue = factory
            .createQueue(queueProperties);

        _sender = factoryQueue.createSender(false);

        if (_sender == null) {
            return false;
        }

        _LOGGER.info(StoreMessages.ARCHIVER_QUEUE, _sender.getSOMName());

        if (_sender.isRemote()) {
            _LOGGER
                .warn(
                    ServiceMessages.REMOTE_SERVICE_WARNING,
                    _sender.toString());
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        if (_sender != null) {
            _sender.tearDown();
            _sender = null;
        }
    }

    private static void _setPropertiesValue(
            final KeyedGroups properties,
            final String propertyName,
            final Optional<String> value)
    {
        if (value.isPresent()) {
            properties.setValue(propertyName, value.get());
        } else {
            properties.removeValue(propertyName);
        }
    }

    /** Compresses the attic. */
    public static final String COMPRESSED_PROPERTY = "compressed";

    /** Default queue name. */
    public static final String DEFAULT_ATTIC_NAME = "attic";

    /** Directory property. */
    public static final String DIRECTORY_PROPERTY = "directory";

    /** Name property. */
    public static final String NAME_PROPERTY = "name";

    /** Queue properties. */
    public static final String QUEUE_PROPERTIES = "queue";
    private static final Logger _LOGGER = Logger.getInstance(SOMAttic.class);

    private SOMSender _sender;
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
