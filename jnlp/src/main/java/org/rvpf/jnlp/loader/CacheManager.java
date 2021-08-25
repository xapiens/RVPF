/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CacheManager.java 4053 2019-06-03 19:22:49Z SFB $
 */

package org.rvpf.jnlp.loader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;

import java.nio.charset.StandardCharsets;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.logger.Messages;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.IdentityHashSet;

/**
 * The cache manager is responsible for the resource files cache management.
 *
 * <p>When asked for a resource file from a URL, it will try to refresh the
 * cached version of that file from the specified location, then return the File
 * from the cache.</p>
 *
 * <p>When a connection to the resources server fails, no further connections
 * will be attempted unless the requested resource file is absent from the cache
 * or the {@value #INSIST_PROPERTY} property is true. When a connection retry is
 * necessary, it will be controlled by the {@value #RETRY_DELAY_PROPERTY} and
 * {@value #RETRIES_PROPERTY} properties (default to
 * {@value #DEFAULT_RETRY_DELAY} seconds and infinite retries).</p>
 *
 * <p>The cache directory is specified by the {@value #CACHE_DIR_PROPERTY}
 * property (defaults to {@value #DEFAULT_CACHE_DIR}). Applications can share
 * the same cache directory for concurrent access by supplying different values
 * for the {@value #CACHE_PREFIX_PROPERTY} property (defaults to
 * {@value #DEFAULT_CACHE_PREFIX}); when they also share the same prefix, the
 * access is serialized by the use of a lock file.</p>
 */
@NotThreadSafe
public final class CacheManager
{
    /**
     * Constructs an instance.
     *
     * @throws IOException When appropriate.
     */
    CacheManager()
        throws IOException
    {
        _prefix = JNLPProperties
            .getInstance()
            .getStringValue(
                CACHE_PREFIX_PROPERTY,
                Optional.of(DEFAULT_CACHE_PREFIX))
            .get();

        _cacheDir = new File(
            JNLPProperties.getInstance().getStringValue(
                CACHE_DIR_PROPERTY,
                Optional.of(DEFAULT_CACHE_DIR)).get());

        if (_cacheDir.mkdirs()) {
            _LOGGER
                .info(JNLPMessages.CACHE_CREATED, _cacheDir.getAbsolutePath());
        } else if (_cacheDir.isDirectory()) {
            _LOGGER
                .debug(
                    JNLPMessages.CACHE_DIRECTORY,
                    _cacheDir.getAbsolutePath());
        } else {
            throw new IOException(
                Message.format(
                    JNLPMessages.CACHE_FAILED,
                    _cacheDir.getAbsolutePath()));
        }

        final File lockFile = new File(_cacheDir, _prefix + LOCK_FILE_NAME);

        _lockFile = new RandomAccessFile(lockFile, "rw");
        _lockFile.getChannel().lock();
        _LOGGER.trace(JNLPMessages.CACHE_LOCKED);

        _purge();
        _loadIndex();
        _cleanUp();
    }

    /**
     * Closes.
     *
     * @throws IOException When appropriate.
     */
    void close()
        throws IOException
    {
        _lockFile.close();
        _LOGGER.trace(JNLPMessages.CACHE_UNLOCKED);
    }

    /**
     * Gets the File from the cache.
     *
     * @param url The origin URL.
     *
     * @return The File from the cache.
     *
     * @throws Exception When appropriate.
     */
    @Nonnull
    @CheckReturnValue
    File getFile(@Nonnull final URL url)
        throws Exception
    {
        _LOGGER.trace(JNLPMessages.LOOKING_FOR, url);

        final File cachedFile = _cacheIndex.get(url.toExternalForm());

        if (_connectionFailed && (cachedFile != null)) {
            return cachedFile;
        }

        final long after = (cachedFile != null)? cachedFile.lastModified(): 0;

        _retried = 0;

        for (;;) {
            final URLConnection connection = _openConnection(url, after);

            if (connection == null) {
                if (((cachedFile == null) || _insist()) && _retry()) {
                    continue;
                }

                break;
            }

            final long lastModified = connection.getLastModified();

            if (lastModified > after) {
                final File newCachedFile;

                try {
                    newCachedFile = _download(connection);
                } catch (final IOException exception) {
                    if (((cachedFile == null) || _insist()) && _retry()) {
                        continue;
                    }

                    break;
                }

                if (!newCachedFile
                    .setLastModified(((lastModified + 999) / 1000) * 1000)) {
                    throw new JNLPException(
                        Message.format(
                            JNLPMessages.SET_FILE_MODIFIED_FAILED,
                            newCachedFile));
                }

                _cacheIndex.put(url.toExternalForm(), newCachedFile);
                _saveIndex();
                _newEntries.add(newCachedFile);

                _deleteObsoleteFile(cachedFile, url);

                return newCachedFile;
            } else if (after == 0) {
                throw new JNLPException(
                    Message.format(JNLPMessages.LAST_MODIFIED_MISSING));
            } else {
                Require.notNull(cachedFile);
                _LOGGER
                    .trace(JNLPMessages.FILE_UP_TO_DATE, cachedFile.getName());
            }

            break;
        }

        if (cachedFile == null) {
            throw new JNLPException(
                Message.format(JNLPMessages.GET_FILE_FAILED));
        }

        return cachedFile;
    }

    /**
     * Asks if a cached file is new.
     *
     * @param cachedFile The cached file.
     *
     * @return True if new.
     */
    @CheckReturnValue
    boolean isNew(@Nonnull final File cachedFile)
    {
        return _newEntries.contains(cachedFile);
    }

    private static void _deleteObsoleteFile(
            final File cachedFile,
            final URL url)
    {
        if (cachedFile != null) {
            if (cachedFile.delete()) {
                _LOGGER
                    .trace(
                        JNLPMessages.OBSOLETE_REMOVED,
                        cachedFile.getName(),
                        url);
            } else {
                _LOGGER
                    .warn(
                        JNLPMessages.OBSOLETE_REMOVE_FAILED,
                        cachedFile.getName(),
                        url);
            }
        }
    }

    private void _cleanUp()
    {
        final File[] cacheDirFiles = Require.notNull(_cacheDir.listFiles());
        final Set<File> cachedFiles = new HashSet<File>(_cacheIndex.values());

        for (final File file: cacheDirFiles) {
            if (file.isFile()
                    && file.getName().startsWith(_prefix)
                    && !cachedFiles.contains(file)
                    && !file.getName().endsWith(LOCK_FILE_NAME)
                    && !file.getName().endsWith(INDEX_FILE_NAME)) {
                if (file.delete()) {
                    _LOGGER.trace(JNLPMessages.UNREF_REMOVED, file.getName());
                } else {
                    _LOGGER
                        .warn(JNLPMessages.UNREF_REMOVE_FAILED, file.getName());
                }
            }
        }
    }

    private URLConnection _connectionFailed(
            final Messages.Entry entry,
            final Object... params)
    {
        _connectionFailed = true;

        if (_retried == 0) {
            _LOGGER.warn(entry, params);
        }

        return null;
    }

    private File _download(final URLConnection connection)
        throws IOException
    {
        DateTime nameStamp = DateTime.now();

        if (nameStamp.isNotAfter(_nameStamp)) {
            nameStamp = _nameStamp.after();
        }

        _nameStamp = nameStamp;

        final File cachedFile = new File(
            _cacheDir,
            _prefix + _nameStamp.toFileName());

        _LOGGER
            .debug(
                JNLPMessages.DOWNLOADING_FROM,
                cachedFile.getName(),
                connection.getURL());

        final InputStream connectionStream = connection.getInputStream();
        final OutputStream fileStream = new FileOutputStream(cachedFile);
        final byte[] buffer = new byte[_STREAM_BUFFER_SIZE];

        try {
            for (;;) {
                final int length = connectionStream.read(buffer);

                if (length < 0) {
                    break;
                }

                fileStream.write(buffer, 0, length);
            }
        } catch (final IOException exception) {
            _LOGGER.warn(JNLPMessages.DOWNLOAD_FAILED, exception.getMessage());
            fileStream.close();

            if (!cachedFile.delete()) {
                _LOGGER
                    .warn(
                        JNLPMessages.DOWNLOAD_DELETE_FAILED,
                        cachedFile.getName());
            }

            throw exception;
        } finally {
            connectionStream.close();
            fileStream.close();
        }

        return cachedFile;
    }

    private boolean _insist()
    {
        if (_insist == null) {
            _insist = Boolean
                .valueOf(
                    JNLPProperties
                        .getInstance()
                        .getBooleanValue(INSIST_PROPERTY));
        }

        return _insist.booleanValue();
    }

    private void _loadIndex()
        throws IOException
    {
        final File indexFile = new File(_cacheDir, _prefix + INDEX_FILE_NAME);

        if (indexFile.exists()) {
            _LOGGER.trace(JNLPMessages.CACHE_INDEX_FOUND, indexFile.getName());

            final Properties properties = new Properties();

            try (final InputStream indexStream = new BufferedInputStream(
                    new FileInputStream(indexFile))) {
                properties.load(indexStream);
            }

            for (final Map.Entry<?, ?> entry: properties.entrySet()) {
                final File cachedFile = new File(
                    _cacheDir,
                    _prefix + entry.getKey());
                final DateTime nameStamp = DateTime
                    .fromString((String) entry.getKey());
                final URL url;

                url = new URL((String) entry.getValue());

                if (nameStamp.isAfter(_nameStamp)) {
                    _nameStamp = nameStamp;
                }

                if (cachedFile.isFile()) {
                    _LOGGER
                        .trace(
                            JNLPMessages.CACHE_FILE_FOUND,
                            cachedFile.getName(),
                            url);
                    _cacheIndex.put(url.toExternalForm(), cachedFile);
                } else {
                    _LOGGER
                        .warn(
                            JNLPMessages.CACHE_FILE_LOST,
                            cachedFile.getName());
                }
            }
        }
    }

    private URLConnection _openConnection(
            final URL url,
            final long after)
        throws Exception
    {
        final URLConnection connection = url.openConnection();

        connection.setUseCaches(false);

        if (after > 0) {
            connection.setIfModifiedSince(after);
        }

        try {
            connection.connect();
        } catch (final IOException exception) {
            return _connectionFailed(
                JNLPMessages.CONNECT_FAILED,
                url,
                exception.getMessage());
        }

        if (connection instanceof HttpURLConnection) {
            final int responseCode;

            try {
                responseCode = ((HttpURLConnection) connection)
                    .getResponseCode();
            } catch (final IOException exception) {
                return _connectionFailed(
                    JNLPMessages.RESPONSE_FAILED,
                    url,
                    exception.getMessage());
            }

            if ((responseCode != HttpURLConnection.HTTP_OK)
                    && (responseCode != HttpURLConnection.HTTP_NOT_MODIFIED)) {
                return _connectionFailed(
                    JNLPMessages.RESPONSE_UNEXPECTED,
                    url,
                    String.valueOf(responseCode),
                    URLDecoder
                        .decode(
                            ((HttpURLConnection) connection)
                                    .getResponseMessage(),
                            StandardCharsets.UTF_8.name()));
            }

            if (_retried > 0) {
                _LOGGER.info(JNLPMessages.CONNECT_SUCCEEDED, url);
                _connectionFailed = false;
            }
        }

        return connection;
    }

    private void _purge()
    {
        if (JNLPProperties.getInstance().getBooleanValue(PURGE_PROPERTY)) {
            final File[] cacheDirFiles = Require.notNull(_cacheDir.listFiles());

            for (final File cacheDirFile: cacheDirFiles) {
                if (cacheDirFile.isFile()
                        && cacheDirFile.getName().startsWith(_prefix)
                        && !cacheDirFile.getName().endsWith(LOCK_FILE_NAME)) {
                    if (cacheDirFile.delete()) {
                        _LOGGER
                            .trace(JNLPMessages.PURGED, cacheDirFile.getName());
                    } else {
                        _LOGGER
                            .warn(
                                JNLPMessages.PURGE_FAILED,
                                cacheDirFile.getName());
                    }
                }
            }
        }
    }

    private boolean _retry()
        throws InterruptedException
    {
        if (_retryDelay == null) {
            final ElapsedTime defaultRetryDelay = ElapsedTime
                .fromMillis(DEFAULT_RETRY_DELAY * 1000);

            _retries = JNLPProperties
                .getInstance()
                .getIntValue(RETRIES_PROPERTY, -1);
            _retryDelay = JNLPProperties
                .getInstance()
                .getElapsedValue(
                    RETRY_DELAY_PROPERTY,
                    Optional.of(defaultRetryDelay),
                    Optional.of(defaultRetryDelay))
                .get();

            if (_retryDelay.compareTo(MINIMUM_RETRY_DELAY) < 0) {
                _retryDelay = defaultRetryDelay;
            }
        }

        if ((_retries >= 0) && (_retried >= _retries)) {
            return false;
        }

        if (_retried == 0) {
            _LOGGER
                .info(
                    JNLPMessages.WILL_RETRY,
                    Integer.valueOf(_retries),
                    _retryDelay);
        }

        Thread.sleep(_retryDelay.toMillis());
        ++_retried;

        return true;
    }

    private void _saveIndex()
        throws IOException
    {
        final Properties properties = new Properties();

        for (final Map.Entry<String, File> entry: _cacheIndex.entrySet()) {
            properties
                .setProperty(
                    entry.getValue().getName().substring(_prefix.length()),
                    entry.getKey());
        }

        final File indexFile = new File(_cacheDir, _prefix + INDEX_FILE_NAME);

        try (final OutputStream indexStream = new BufferedOutputStream(
                new FileOutputStream(indexFile))) {
            properties.store(indexStream, "Cache index.");
        }
    }

    /** Cache directory property. */
    public static final String CACHE_DIR_PROPERTY = "cache.dir";

    /** Cache prefix property. */
    public static final String CACHE_PREFIX_PROPERTY = "cache.prefix";

    /** Default cache directory. */
    public static final String DEFAULT_CACHE_DIR = "cache";

    /** Default cache prefix. */
    public static final String DEFAULT_CACHE_PREFIX = "jnlp-";

    /** Default retry delay in seconds. */
    public static final int DEFAULT_RETRY_DELAY = 60;

    /** Name of the index file. */
    public static final String INDEX_FILE_NAME = "index.properties";

    /** Insist property. */
    public static final String INSIST_PROPERTY = "download.insist";

    /** Name of the lock file. */
    public static final String LOCK_FILE_NAME = "cache.lock";

    /** Minimum retry delay. */
    public static final ElapsedTime MINIMUM_RETRY_DELAY = ElapsedTime
        .fromMillis(1000);

    /** Purge property (needed by test classes). */
    public static final String PURGE_PROPERTY = "cache.purge";

    /** Retries property. */
    public static final String RETRIES_PROPERTY = "download.retries";

    /** Retry interval property. */
    public static final String RETRY_DELAY_PROPERTY = "download.retry.delay";

    /**  */

    private static final Logger _LOGGER = Logger
        .getInstance(CacheManager.class);
    private static final int _STREAM_BUFFER_SIZE = 8192;

    private final File _cacheDir;
    private final Map<String, File> _cacheIndex = new LinkedHashMap<>();
    private boolean _connectionFailed;
    private Boolean _insist;
    private final RandomAccessFile _lockFile;
    private DateTime _nameStamp = DateTime.fromRaw(0);
    private final Set<File> _newEntries = new IdentityHashSet<>();
    private final String _prefix;
    private int _retried;
    private int _retries;
    private ElapsedTime _retryDelay;
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
