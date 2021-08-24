/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StreamedMessagesClient.java 4096 2019-06-24 23:07:39Z SFB $
 */

package org.rvpf.base.xml.streamer;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import java.nio.charset.Charset;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.DateTime;
import org.rvpf.base.logger.Message;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.util.container.KeyedValues;

/**
 * Streamed messages client.
 */
@NotThreadSafe
public final class StreamedMessagesClient
    extends StreamedMessagesWriter
{
    /** {@inheritDoc}
     */
    @Override
    public synchronized boolean add(final Serializable message)
    {
        if (_transaction == null) {
            DateTime nameStamp = DateTime.now();

            if (nameStamp.isNotAfter(_nameStamp)) {
                nameStamp = _nameStamp.after();
            }

            _nameStamp = nameStamp;

            final String transaction = _prefix + _nameStamp.toFileName()
                + _nameSuffix;
            final File file = new File(_directory, transaction + _transSuffix);

            try {
                super.open(file, Optional.empty());
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
            }

            _transFile = file;
            _transaction = transaction;
        }

        return super.add(message);
    }

    /** {@inheritDoc}
     */
    @Override
    public void close() {}

    /**
     * Commits the transaction.
     */
    public synchronized void commit()
    {
        if (_transaction != null) {
            super.close();

            try {
                final File dataFile = new File(
                    _directory,
                    _transaction + _dataSuffix);

                if (!_transFile.renameTo(dataFile)) {
                    throw new RuntimeException(
                        Message.format(
                            BaseMessages.FILE_RENAME_FAILED,
                            _transFile,
                            dataFile));
                }
            } finally {
                _transFile = null;
                _transaction = null;
            }
        }
    }

    /**
     * Flushes the output.
     */
    @Override
    public synchronized void flush()
    {
        if (_transaction != null) {
            super.flush();
        }
    }

    /**
     * Gets the name stamp.
     *
     * @return The name stamp.
     */
    @Nonnull
    @CheckReturnValue
    public synchronized DateTime getNameStamp()
    {
        return _nameStamp;
    }

    /** {@inheritDoc}
     */
    @Override
    public void open(final File file, final Optional<Charset> charset)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Rolls back the transaction.
     */
    public synchronized void rollback()
    {
        if (_transaction != null) {
            super.close();

            try {
                if (!_transFile.delete()) {
                    throw new RuntimeException(
                        Message.format(
                            BaseMessages.FILE_DELETE_FAILED,
                            _transFile));
                }
            } finally {
                _transFile = null;
                _transaction = null;
            }
        }
    }

    /**
     * Sets a (file) name suffix.
     *
     * @param nameSuffix The optional (file) name suffix.
     *
     * @throws Require.FailureException When a transaction is active.
     */
    public void setNameSuffix(
            @Nonnull final Optional<String> nameSuffix)
        throws Require.FailureException
    {
        Require
            .success(_transaction == null, BaseMessages.FILE_ALREADY_CREATED);

        _nameSuffix = nameSuffix.orElse("");
    }

    /**
     * Sets up this.
     *
     * @param directory The transactions directory.
     * @param configProperties Configuration properties (may be null).
     * @param moduleProperties Module properties (may be null).
     *
     * @return True on success.
     */
    @CheckReturnValue
    public synchronized boolean setUp(
            @Nonnull final File directory,
            @Nonnull final KeyedGroups configProperties,
            @Nonnull final KeyedValues moduleProperties)
    {
        if (!setUp(
                Optional.of(configProperties),
                Optional.of(moduleProperties))) {
            return false;
        }

        return _setUp(directory, configProperties);
    }

    /**
     * Tears down what has been set up.
     */
    @Override
    public synchronized void tearDown()
    {
        if (_directory != null) {
            rollback();
            _directory = null;
        }

        super.tearDown();
    }

    private synchronized boolean _setUp(
            final File directory,
            final KeyedGroups configProperties)
    {
        _prefix = configProperties
            .getString(PREFIX_PROPERTY, Optional.of(""))
            .get();
        _transSuffix = configProperties
            .getString(TRANS_SUFFIX_PROPERTY, Optional.of(DEFAULT_TRANS_SUFFIX))
            .get();
        _dataSuffix = configProperties
            .getString(DATA_SUFFIX_PROPERTY, Optional.of(DEFAULT_DATA_SUFFIX))
            .get();

        final File[] files = Require.notNull(directory.listFiles());

        for (final File file: files) {
            if (file.isFile()) {
                String name = file.getName();
                final int dotIndex = name.indexOf('.');
                final DateTime nameStamp;

                if (dotIndex >= 0) {
                    name = name.substring(0, dotIndex);
                }

                if (name.length() < DateTime.FILE_NAME_LENGTH) {
                    continue;
                }

                name = name
                    .substring(name.length() - DateTime.FILE_NAME_LENGTH);

                if (name.isEmpty()) {
                    continue;
                }

                try {
                    nameStamp = DateTime.fromString(name);
                } catch (final IllegalArgumentException exception) {
                    continue;
                }

                Require.notNull(nameStamp);

                if (nameStamp.isAfter(_nameStamp)) {
                    _nameStamp = nameStamp;
                }
            }
        }

        _directory = directory;

        return true;
    }

    /** The file suffix for data entries. */
    public static final String DATA_SUFFIX_PROPERTY = "suffix.data";

    /** Default file suffix for data entries. */
    public static final String DEFAULT_DATA_SUFFIX = ".data";

    /** Default file suffix for transaction entries. */
    public static final String DEFAULT_TRANS_SUFFIX = ".trans";

    /** The file name prefix. */
    public static final String PREFIX_PROPERTY = "name.prefix";

    /** The file suffix for transaction entries. */
    public static final String TRANS_SUFFIX_PROPERTY = "suffix.trans";

    private String _dataSuffix;
    private File _directory;
    private DateTime _nameStamp = DateTime.fromRaw(0);
    private String _nameSuffix = "";
    private String _prefix;
    private File _transFile;
    private String _transSuffix;
    private String _transaction;
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
