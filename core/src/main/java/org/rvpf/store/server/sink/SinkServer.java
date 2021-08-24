/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SinkServer.java 3970 2019-05-09 19:35:44Z SFB $
 */

package org.rvpf.store.server.sink;

import java.util.EnumSet;
import java.util.Optional;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Messages;
import org.rvpf.base.security.Identity;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Externalizer;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.store.server.StoreMessages;
import org.rvpf.store.server.StoreServer;
import org.rvpf.store.server.StoreServiceAppImpl;
import org.rvpf.store.server.archiver.Archiver;

/**
 * Sink server
 */
final class SinkServer
    extends StoreServer.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    public Optional<Archiver> newArchiver()
    {
        return Optional.empty();
    }

    /** {@inheritDoc}
     */
    @Override
    public StoreValues[] select(
            final StoreValuesQuery[] queries,
            final Optional<Identity> identity)
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized void stop()
    {
        if (_sink != null) {
            _sink.close();
            _sink = null;
        }

        super.stop();
    }

    /** {@inheritDoc}
     */
    @Override
    public String supportedValueTypeCodes()
    {
        return Externalizer.ValueType
            .setToString(EnumSet.allOf(Externalizer.ValueType.class));
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        stop();

        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized Exception[] update(
            final PointValue[] updates,
            final Optional<Identity> identity)
    {
        if (_sink == null) {
            return null;
        }

        final Exception[] exceptions = restoreUpdates(updates);
        final long mark = System.nanoTime();

        try {
            long ignored = 0;
            long updated = 0;
            long deleted = 0;

            for (int i = 0; i < updates.length; i++) {
                if (exceptions[i] != null) {
                    ++ignored;

                    continue;
                }

                final VersionedValue versionedValue = versionedValue(
                    updates[i]);
                final boolean notify;
                final Messages.Entry actionMessage;

                exceptions[i] = checkUpdate(versionedValue, identity)
                    .orElse(null);

                if (exceptions[i] != null) {
                    ++ignored;

                    continue;
                }

                if (versionedValue.isDeleted()) {
                    notify = _sink.delete(versionedValue);
                    ++deleted;
                    getDeletedTraces().add(versionedValue);
                    actionMessage = StoreMessages.UPDATER_DELETED;
                } else {
                    notify = _sink.update(versionedValue);
                    ++updated;
                    getUpdatedTraces().add(versionedValue);
                    actionMessage = StoreMessages.UPDATER_UPDATED;
                }

                getThisLogger().trace(actionMessage, versionedValue);

                getReplicator().replicate(versionedValue);

                if (notify) {
                    addNotice(versionedValue);
                }
            }

            getReplicator().commit();
            sendNotices();
            getUpdatedTraces().commit();
            getDeletedTraces().commit();
            reportUpdates(updated, deleted, ignored, System.nanoTime() - mark);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new RuntimeException(exception);
        } catch (final RuntimeException exception) {
            getThisLogger()
                .warn(exception, BaseMessages.VERBATIM, exception.getMessage());

            throw exception;
        } catch (final Exception exception) {
            getThisLogger()
                .trace(
                    exception,
                    BaseMessages.VERBATIM,
                    exception.getMessage());

            throw exception;
        }

        return exceptions;
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean setUpPullSleep()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean supportsDeleteTracer()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean supportsUpdateTracer()
    {
        return true;
    }

    /**
     * Sets up for processing.
     *
     * @param storeAppImpl The store application implementation.
     * @param sink The sink module.
     *
     * @return True on success.
     */
    boolean setUp(final StoreServiceAppImpl storeAppImpl, final SinkModule sink)
    {
        if (!setUp(storeAppImpl)) {
            return false;
        }

        synchronized (this) {
            _sink = sink;
        }

        return true;
    }

    private SinkModule _sink;
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
