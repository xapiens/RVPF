/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: BDBBackEnd.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.store.server.the.bdb;

import java.io.File;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.security.Identity;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.service.ServiceMessages;
import org.rvpf.store.server.StoreCursor;
import org.rvpf.store.server.StoreMessages;
import org.rvpf.store.server.the.BackEnd;
import org.rvpf.store.server.the.TheStoreServiceAppImpl;

/**
 * Berkeley DB (Java Edition) back-end.
 */
public class BDBBackEnd
    implements BackEnd
{
    /** {@inheritDoc}
     */
    @Override
    public void beginUpdates()
    {
        _lock.writeLock().lock();
        Require.success(_transaction == null);

        try {
            _transaction = _wrapper.beginTransaction();
        } catch (final JEWrapperException exception) {
            throw new RuntimeException(exception.getCause());
        } finally {
            if (_transaction == null) {
                _lock.writeLock().unlock();
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void close()
    {
        if (_wrapper != null) {
            try {
                _wrapper.close();
            } catch (final JEWrapperException exception) {
                throw new RuntimeException(exception.getCause());
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void commit()
    {
        final Object transaction = _transaction;

        Require.notNull(transaction);
        _transaction = null;

        try {
            _wrapper.commit(transaction);
        } catch (final JEWrapperException exception) {
            throw new RuntimeException(exception.getCause());
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public StoreValues createResponse(
            final StoreValuesQuery query,
            final Optional<Identity> identity)
    {
        final Responder responder = new Responder(
            _wrapper,
            _theStoreAppImpl.getService().getMetadata(),
            _theStoreAppImpl.getBackEndLimit());
        final StoreCursor cursor = new StoreCursor(
            query,
            _theStoreAppImpl.getServer(),
            responder);
        final StoreValues response;

        _lock.readLock().lock();

        try {
            response = cursor.createResponse(identity);
        } finally {
            responder.close();
            _lock.readLock().unlock();
        }

        return response;
    }

    /** {@inheritDoc}
     */
    @Override
    public int delete(final VersionedValue versionedValue)
    {
        final VersionedValue pointValue = versionedValue
            .isDeleted()? new VersionedValue(versionedValue): versionedValue;
        final VersionedValue deletedValue = versionedValue
            .isDeleted()? versionedValue: new VersionedValue.Deleted(
                versionedValue);
        final boolean purged = versionedValue instanceof VersionedValue.Purged;
        int deleted;

        try {
            deleted = _wrapper.delete(_transaction, pointValue);

            if (purged) {
                deleted += _wrapper.delete(_transaction, deletedValue);
            }
        } catch (final JEWrapperException exception) {
            throw new RuntimeException(exception.getCause());
        }

        if (deleted > 0) {
            _LOGGER.trace(StoreMessages.ROW_DELETED, pointValue);

            if (!(purged || _theStoreAppImpl.isDropDeleted())) {
                update(deletedValue);
            }
        }

        return deleted;
    }

    /** {@inheritDoc}
     */
    @Override
    public void endUpdates()
    {
        Require.success(_transaction == null);

        _lock.writeLock().unlock();
    }

    /** {@inheritDoc}
     */
    @Override
    public void open()
        throws ServiceNotAvailableException
    {
        try {
            _wrapper.open();
        } catch (final JEWrapperException exception) {
            throw new ServiceNotAvailableException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void rollback()
    {
        final Object transaction = _transaction;

        Require.notNull(transaction);
        _transaction = null;

        try {
            _wrapper.abort(transaction);
        } catch (final JEWrapperException exception) {
            throw new RuntimeException(exception.getCause());
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final TheStoreServiceAppImpl theStoreAppImpl)
    {
        _theStoreAppImpl = theStoreAppImpl;

        _wrapper = JEWrapper.IMPL.createInstance(JEWrapper.class);

        if (_wrapper == null) {
            return false;
        }

        final KeyedGroups serverProperties = _theStoreAppImpl
            .getServerProperties();
        final Optional<String> bdbDir = serverProperties
            .getString(BDB_DIR_PROPERTY);
        final Path storeDataPath = theStoreAppImpl.getStoreDataDir().toPath();
        Path homePath = null;

        if (bdbDir.isPresent()) {
            try {
                homePath = storeDataPath.resolve(bdbDir.get());
            } catch (final InvalidPathException exception) {
                _LOGGER
                    .warn(
                        ServiceMessages.INVALID_PATH_IN,
                        bdbDir.get(),
                        BDB_DIR_PROPERTY);
            }
        }

        if (homePath == null) {
            final String defaultPath = DEFAULT_BDB_DIR + "/"
                +theStoreAppImpl.getEntityName().orElse(
                    null);

            try {
                homePath = storeDataPath.resolve(defaultPath);
            } catch (final InvalidPathException exception) {
                _LOGGER.error(ServiceMessages.INVALID_PATH, defaultPath);

                return false;
            }
        }

        final File home = homePath.toFile();

        _LOGGER.info(StoreMessages.BDB_DIR, home.getAbsolutePath());

        if (!home.isDirectory()) {
            if (!home.mkdirs()) {
                _LOGGER
                    .error(
                        StoreMessages.BDB_DIR_CREATION_FAILED,
                        home.getAbsolutePath());

                return false;
            }

            _LOGGER.info(StoreMessages.BDB_DIR_CREATED, home.getAbsolutePath());
        }

        try {
            return _wrapper
                .setUp(
                    home,
                    _theStoreAppImpl.isSnapshot(),
                    _theStoreAppImpl.isPullDisabled());
        } catch (final JEWrapperException exception) {
            throw new RuntimeException(exception.getCause());
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsCount()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        if (_wrapper != null) {
            try {
                _wrapper.tearDown();
            } catch (final JEWrapperException exception) {
                throw new RuntimeException(exception.getCause());
            }

            _wrapper = null;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void update(final VersionedValue versionedValue)
    {
        try {
            _wrapper.put(_transaction, versionedValue);
        } catch (final JEWrapperException exception) {
            throw new RuntimeException(exception.getCause());
        }
    }

    /** The BDB-JE environment directory property. */
    public static final String BDB_DIR_PROPERTY = "bdb.dir";

    /** The default BDB-JE environment directory. */
    public static final String DEFAULT_BDB_DIR = "bdb";
    private static final Logger _LOGGER = Logger.getInstance(BDBBackEnd.class);

    private final ReadWriteLock _lock = new ReentrantReadWriteLock(true);
    private TheStoreServiceAppImpl _theStoreAppImpl;
    private Object _transaction;
    private JEWrapper _wrapper;
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
