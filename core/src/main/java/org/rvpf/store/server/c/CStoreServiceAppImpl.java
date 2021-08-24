/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CStoreServiceAppImpl.java 4097 2019-06-25 15:35:48Z SFB $
 */

package org.rvpf.store.server.c;

import java.io.File;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Callable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Point;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.SnoozeAlarm;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.service.ServiceMessages;
import org.rvpf.store.server.StoreMessages;
import org.rvpf.store.server.StoreServiceAppImpl;

/**
 * C store service application implementation..
 */
public class CStoreServiceAppImpl
    extends StoreServiceAppImpl
{
    /** {@inheritDoc}
     */
    @Override
    public CStoreServer getServer()
    {
        return Require.notNull(_server);
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        if (_notifierThread != null) {
            _notifierThread.tearDown();
            _notifierThread = null;
        }

        if (_server != null) {
            _server.stop();
            _server.tearDown();
            _server = null;
        }

        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp()
    {
        if (!super.setUp(SERVER_PROPERTIES)) {
            return false;
        }

        // Identifies the shared object library file.

        final String implSo = getServerProperties()
            .getString(IMPL_SO_PROPERTY)
            .orElse(null);

        if ((implSo == null) || implSo.trim().isEmpty()) {
            getThisLogger()
                .error(BaseMessages.MISSING_PROPERTY, IMPL_SO_PROPERTY);

            return false;
        }

        getThisLogger().info(StoreMessages.IMPLEMENTATION_LIBRARY, implSo);

        File implFile = new File(System.mapLibraryName(implSo));

        if (implFile.getName().endsWith(_JNILIB_TYPE)
                && !implSo.endsWith(_JNILIB_TYPE)
                && !implSo.equals(CStore.LIBRARY)) {
            final String fileName = implFile.getName();

            implFile = new File(
                implFile.getParent(),
                fileName.substring(
                    0,
                    fileName.length() - _JNILIB_TYPE.length()) + _DYLIB_TYPE);
        }

        // Identifies the charset.

        final String charsetName = getServerProperties()
            .getString(CHARSET_PROPERTY)
            .orElse(null);
        final Charset charset;

        if ((charsetName != null) && (charsetName.trim().length() > 0)) {
            try {
                charset = Charset.forName(charsetName);
            } catch (final IllegalArgumentException exception) {
                getThisLogger()
                    .error(ServiceMessages.CHARSET_UNKNOWN, charsetName);

                return false;
            }
        } else {
            charset = StandardCharsets.UTF_8;
        }

        getThisLogger().info(ServiceMessages.CHARSET, charset.name());

        // Identifies the connection retry delay.

        final ElapsedTime connectionRetryDelay = getServerProperties()
            .getElapsed(
                CONNECTION_RETRY_DELAY_PROPERTY,
                Optional.of(DEFAULT_CONNECTION_RETRY_DELAY),
                Optional.of(DEFAULT_CONNECTION_RETRY_DELAY))
            .get();

        if (!SnoozeAlarm
            .validate(
                connectionRetryDelay,
                this,
                ServiceMessages.CONNECTION_RETRY_DELAY_TEXT)) {
            return false;
        }

        // Creates the C-store instance.

        final KeyedGroups envProperties = getServerProperties()
            .getGroup(ENV_PROPERTIES);
        final CStore cStore = CStore
            .getInstance(
                this,
                implFile,
                charset,
                connectionRetryDelay,
                envProperties);

        if (cStore == null) {
            return false;
        }

        // Sets up notification.

        _notifierThread = cStore.supportsDeliver()? new NotifierThread(): null;

        final boolean notifyUpdates = getServerProperties()
            .getBoolean(NOTIFY_UPDATES_PROPERTY, _notifierThread == null);
        final boolean notifyDeletes = getServerProperties()
            .getBoolean(NOTIFY_DELETES_PROPERTY, notifyUpdates);

        if (notifyUpdates) {
            getThisLogger().info(StoreMessages.UPDATE_NOTICES);
        }

        if (notifyDeletes) {
            getThisLogger().info(StoreMessages.DELETE_NOTICES);
        }

        if ((_notifierThread != null) && !_notifierThread.setUp(this, cStore)) {
            return false;
        }

        if (((_notifierThread != null) || notifyUpdates || notifyDeletes)
                && !setUpNotifier()) {
            return false;
        }

        // Creates the store server.

        _server = new CStoreServer(this, cStore, notifyUpdates, notifyDeletes);

        if (!_server.setUp(this)) {
            return false;
        }

        // Resolves points and subscriptions.

        _unsubscribe = Collections.emptyList();
        _forget = Collections.emptyList();
        _resolve = getMetadata().getPointsCollection();
        _subscribe = areNoticesFiltered()? getNoticesFilter(): _resolve;

        if (!_resolve()) {
            return false;
        }

        // Completes set up.

        if (_notifierThread != null) {
            _notifierThread.start();
        }

        if (!registerServer(_server)) {
            return false;
        }

        if (!setUpArchiver()) {
            return false;
        }

        return true;
    }

    /**
     * Gets the log level for the instance.
     *
     * @return The optional log level for the instance (null on failure).
     */
    @Nullable
    @CheckReturnValue
    Optional<Logger.LogLevel> getLogLevel()
    {
        final Optional<String> levelName = getServerProperties()
            .getString(LOG_LEVEL_PROPERTY);
        final Logger.LogLevel level;

        if (!levelName.isPresent()) {
            return Optional.empty();
        }

        try {
            level = Logger.LogLevel.get(levelName.get());
        } catch (final IllegalArgumentException exception) {
            getThisLogger().warn(StoreMessages.LOG_LEVEL_UNKNOWN, levelName);

            return null;
        }

        getThisLogger().info(StoreMessages.INSTANCE_LOG_LEVEL, level);

        return Optional.of(level);
    }

    /**
     * Gets the log name for the store instance.
     *
     * @return The log name for the store instance (may be empty).
     */
    @Nonnull
    @CheckReturnValue
    String getLogName()
    {
        return getServerProperties()
            .getString(LOG_NAME_PROPERTY, Optional.of(""))
            .get();
    }

    /**
     * Called when the C store server is stopping.
     */
    void onServerStop()
    {
        if (_notifierThread != null) {
            _notifierThread.stop();
            _notifierThread = null;
        }

        _forget = getMetadata().getPointsCollection();
        _unsubscribe = areNoticesFiltered()? getNoticesFilter(): _forget;
        _resolve = Collections.emptyList();
        _subscribe = Collections.emptyList();

        _resolve();
    }

    /**
     * Wakes up the notifier.
     */
    void wakeUpNotifier()
    {
        if (_notifierThread != null) {
            _notifierThread.wakeUp();
        }
    }

    private boolean _resolve()
    {
        final CStore cStore = _server.getCStore();
        final Collection<Point> unsubscribe = _unsubscribe;
        final Collection<Point> forget = _forget;
        final Collection<Point> resolve = _resolve;
        final Collection<Point> subscribe = _subscribe;
        final CStore.Task<Boolean> task = new CStore.Task<Boolean>(
            new Callable<Boolean>()
            {
                @Override
                public Boolean call()
                {
                    final boolean supportsSubscribe = cStore
                        .supportsSubscribe();

                    if (supportsSubscribe) {
                        if (!cStore.unsubscribe(unsubscribe)) {
                            return Boolean.FALSE;
                        }
                    }

                    if (!cStore.forget(forget)) {
                        return Boolean.FALSE;
                    }

                    if (!cStore.resolve(resolve)) {
                        return Boolean.FALSE;
                    }

                    if (supportsSubscribe) {
                        if (!cStore.subscribe(subscribe)) {
                            return Boolean.FALSE;
                        }
                    }

                    return Boolean.TRUE;
                }
            });

        try {
            cStore.execute(task);

            return task.get().booleanValue();
        } catch (final InterruptedException exception) {
            getThisLogger().debug(ServiceMessages.INTERRUPTED);
            Thread.currentThread().interrupt();

            return false;
        } catch (final RuntimeException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new RuntimeException(exception);
        } finally {
            _unsubscribe = null;
            _forget = null;
            _resolve = null;
            _subscribe = null;
        }
    }

    /** Names the Charset used by the implementation. */
    public static final String CHARSET_PROPERTY = "charset";

    /** The elapsed time before retrying to connect. */
    public static final String CONNECTION_RETRY_DELAY_PROPERTY =
        "connection.retry.delay";

    /** Default connect retry delay. */
    public static final ElapsedTime DEFAULT_CONNECTION_RETRY_DELAY = ElapsedTime
        .fromMillis(60000);

    /** Default notifier poll interval. */
    public static final ElapsedTime DEFAULT_NOTIFIER_POLL_INTERVAL = ElapsedTime
        .fromMillis(60000);

    /** Properties group for the implementation environment. */
    public static final String ENV_PROPERTIES = "env";

    /** Names the shareable object for the implementation. */
    public static final String IMPL_SO_PROPERTY = "impl.so";

    /** The log level for the implementation. */
    public static final String LOG_LEVEL_PROPERTY = "log.level";

    /** The log name for the store instance. */
    public static final String LOG_NAME_PROPERTY = "log.name";

    /** Specifies the elapsed time between notices poll. */
    public static final String NOTIFIER_POLL_INTERVAL_PROPERTY =
        "notifier.poll.interval";

    /** Enables the generation of notices for deleted values. */
    public static final String NOTIFY_DELETES_PROPERTY = "notify.deletes";

    /** Enables the generation of notices for updated values. */
    public static final String NOTIFY_UPDATES_PROPERTY = "notify.updates";

    /** Server properties. */
    public static final String SERVER_PROPERTIES = "store.server.c";
    private static final String _DYLIB_TYPE = ".dylib";
    private static final String _JNILIB_TYPE = ".jnilib";

    private Collection<Point> _forget;
    private NotifierThread _notifierThread;
    private Collection<Point> _resolve;
    private CStoreServer _server;
    private Collection<Point> _subscribe;
    private Collection<Point> _unsubscribe;
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
