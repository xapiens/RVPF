/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: QueueServerImpl.java 4101 2019-06-30 14:56:50Z SFB $
 */

package org.rvpf.som.queue;

import java.io.File;

import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

import org.rvpf.base.UUID;
import org.rvpf.base.rmi.Session;
import org.rvpf.base.rmi.Session.ConnectionMode;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.som.QueueInfo;
import org.rvpf.base.som.QueueServer;
import org.rvpf.base.som.ReceiverActiveException;
import org.rvpf.base.som.ReceiverSession;
import org.rvpf.base.som.SenderSession;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.xml.streamer.Streamer;
import org.rvpf.config.Config;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.rmi.ExportedSessionImpl;
import org.rvpf.service.rmi.SessionImpl;
import org.rvpf.service.rmi.SessionSecurityContext;
import org.rvpf.som.SOMServerImpl;
import org.rvpf.som.SOMStatsHolder;

/**
 * Queue server implementation.
 */
public final class QueueServerImpl
    extends SOMServerImpl
    implements QueueServer
{
    /**
     * Constructs an instance.
     *
     * @param securityContext The optional security context.
     */
    public QueueServerImpl(
            @Nonnull final Optional<SessionSecurityContext> securityContext)
    {
        super(securityContext);
    }

    /** {@inheritDoc}
     */
    @Override
    public void close()
    {
        super.close();

        synchronized (_receiverMutex) {
            if (_receiverWrapper != null) {
                _receiverWrapper.close();
                _receiverWrapper = null;
            } else if (_receiverSession != null) {
                _receiverSession.close();
                _receiverSession = null;
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public ReceiverSession createReceiverSession(
            final UUID uuid,
            final String clientName)
        throws SessionException
    {
        return (ReceiverSession) createSession(
            uuid,
            new Descriptor(_RECEIVER, clientName));
    }

    /**
     * Creates a receiver wrapper.
     *
     * @return The receiver wrapper.
     *
     * @throws ReceiverActiveException When a receiver is already active.
     */
    @Nonnull
    @CheckReturnValue
    public ReceiverWrapper createReceiverWrapper()
        throws ReceiverActiveException
    {
        synchronized (_receiverMutex) {
            _dropReceiver();

            _receiverWrapper = new ReceiverWrapper(
                getQueue().newReceiver(),
                this);

            return _receiverWrapper;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public SenderSession createSenderSession(
            final UUID uuid,
            final String clientName)
        throws SessionException
    {
        return (SenderSession) createSession(
            uuid,
            new Descriptor(_SENDER, clientName));
    }

    /**
     * Creates a sender wrapper.
     *
     * @return The sender wrapper.
     */
    @Nonnull
    @CheckReturnValue
    public SenderWrapper createSenderWrapper()
    {
        return new SenderWrapper(getQueue().newSender(), this);
    }

    /** {@inheritDoc}
     */
    @Override
    public QueueInfo getInfo()
    {
        return getQueue().getInfo();
    }

    /**
     * Gets the queue.
     *
     * @return The queue.
     */
    @Nonnull
    @CheckReturnValue
    public Queue getQueue()
    {
        return Require.notNull(_queue.get());
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean removeSession(final SessionImpl session)
    {
        final boolean removed = super.removeSession(session);
        final QueueStats stats = _stats.get();

        if (session instanceof ReceiverSessionImpl) {
            synchronized (_receiverMutex) {
                if (session == _receiverSession) {
                    _receiverSession = null;
                }
            }

            if (stats != null) {
                stats.receiverSessionClosed();
            }
        } else if (session instanceof SenderSessionImpl) {
            if (stats != null) {
                stats.senderSessionClosed();
            }
        }

        return removed;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Config config, KeyedGroups queueProperties)
    {
        if (!super
            .setUp(config, queueProperties, DEFAULT_QUEUE_BINDING_PREFIX)) {
            return false;
        }

        if (!hasSecurityContext()) {
            getThisLogger().debug(ServiceMessages.QUEUE_IS_PRIVATE, getName());
        }

        final SOMStatsHolder statsOwner = new SOMStatsHolder(getName());
        final QueueStats stats = new QueueStats(statsOwner);

        statsOwner.setStats(stats);

        if (hasSecurityContext() && !stats.register(config)) {
            return false;
        }

        _stats.set(stats);

        final Queue queue;

        if (queueProperties.getBoolean(MEMORY_PROPERTY)) {
            queue = new MemoryQueue(getName(), stats);
        } else {
            final Streamer streamer = Streamer.newInstance();

            if ((streamer == null)
                    || !streamer.setUp(
                        Optional.of(config.getProperties()),
                        Optional.of(queueProperties))) {
                return false;
            }

            queue = new FilesQueue(getName(), stats, streamer);

            final File rootDirectory = Config
                .dataDir(
                    Optional.of(config.getDataDir()),
                    queueProperties,
                    FilesQueue.ROOT_PROPERTY,
                    config
                        .getStringValue(
                                ROOT_ALL_PROPERTY,
                                        Optional.of(DEFAULT_ROOT))
                        .orElse(null));

            queueProperties = queueProperties.copy();
            queueProperties
                .setValue(
                    FilesQueue.ROOT_PROPERTY,
                    rootDirectory.getAbsolutePath());

            if (!queueProperties.containsValueKey(FilesQueue.BACKUP_PROPERTY)) {
                _setPropertiesValue(
                    queueProperties,
                    FilesQueue.BACKUP_PROPERTY,
                    config.getStringValue(BACKUP_ALL_PROPERTY));
            }

            if (!queueProperties
                .containsValueKey(FilesQueue.MERGE_LIMIT_PROPERTY)) {
                _setPropertiesValue(
                    queueProperties,
                    FilesQueue.MERGE_LIMIT_PROPERTY,
                    config.getStringValue(MERGE_LIMIT_ALL_PROPERTY));
            }

            if (!queueProperties
                .containsValueKey(FilesQueue.MERGE_SPLIT_PROPERTY)) {
                _setPropertiesValue(
                    queueProperties,
                    FilesQueue.MERGE_SPLIT_PROPERTY,
                    config.getStringValue(MERGE_SPLIT_ALL_PROPERTY));
            }

            _setPropertiesValue(
                queueProperties,
                FilesQueue.LOCK_DISABLED_PROPERTY,
                config.getStringValue(LOCK_DISABLED_PROPERTY));

            queueProperties.freeze();
        }

        if (!queue.setUp(queueProperties)) {
            return false;
        }

        _queue.set(queue);

        return hasSecurityContext()? bind(): true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        final Queue queue = _queue.getAndSet(null);

        if (queue != null) {
            if (hasSecurityContext()) {
                super.tearDown();
            }

            queue.tearDown();
        }

        final QueueStats stats = _stats.getAndSet(null);

        if ((stats != null) && hasSecurityContext()) {
            stats.unregister();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    @GuardedBy("this")
    protected Session newSession(
            final ConnectionMode connectionMode,
            final Optional<RMIClientSocketFactory> clientSocketFactory,
            final Optional<RMIServerSocketFactory> serverSocketFactory,
            final Object reference)
        throws ReceiverActiveException
    {
        final Descriptor descriptor = (Descriptor) reference;
        final String modeName = descriptor.getModeName();
        final String clientName = descriptor.getClientName();
        final ExportedSessionImpl session;

        if (modeName == _SENDER) {
            session = new SenderSessionImpl(this, connectionMode, clientName);
            _stats.get().senderSessionOpened();
        } else if (modeName == _RECEIVER) {
            synchronized (_receiverMutex) {
                _dropReceiver();
                _receiverSession = new ReceiverSessionImpl(
                    this,
                    connectionMode,
                    clientName);
                session = _receiverSession;
            }

            _stats.get().receiverSessionOpened();
        } else {
            throw new AssertionError();
        }

        session
            .open(
                clientSocketFactory.orElse(null),
                serverSocketFactory.orElse(null));

        return session;
    }

    /**
     * Called when a receiver wrapper is closed.
     *
     * @param receiverWrapper The receiver wrapper.
     */
    void closed(@Nonnull final ReceiverWrapper receiverWrapper)
    {
        synchronized (_receiverMutex) {
            if (receiverWrapper == _receiverWrapper) {
                _receiverWrapper = null;
            }
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

    private void _dropReceiver()
        throws ReceiverActiveException
    {
        if (_receiverWrapper != null) {
            throw new ReceiverActiveException();
        } else if (_receiverSession != null) {
            getThisLogger().warn(ServiceMessages.DROPPING_RECEIVER, getName());
            _receiverSession.close();
            _receiverSession = null;
            getThisLogger().debug(ServiceMessages.DROPPED_RECEIVER, getName());
        }
    }

    /** True will backup messages on all queues. */
    public static final String BACKUP_ALL_PROPERTY = "service.som.queue.backup";

    /** Default queue root directory. */
    public static final String DEFAULT_ROOT = "queue";

    /** The lock disabled property. */
    public static final String LOCK_DISABLED_PROPERTY =
        "service.som.queue.lock.disabled";

    /** Memory property. */
    public static final String MEMORY_PROPERTY = "memory";

    /** Merge limit for all queues. */
    public static final String MERGE_LIMIT_ALL_PROPERTY =
        "service.som.queue.merge.limit";

    /** Merge split for all queues. */
    public static final String MERGE_SPLIT_ALL_PROPERTY =
        "service.som.queue.merge.split";

    /**
     * The root directory for the SOM queues. It can be specified as a relative
     * or absolute path.
     */
    public static final String ROOT_ALL_PROPERTY = "service.som.queue.root";
    private static final String _RECEIVER = "receiver";
    private static final String _SENDER = "sender";

    private final AtomicReference<Queue> _queue = new AtomicReference<>();
    private final Object _receiverMutex = new Object();
    private ReceiverSessionImpl _receiverSession;
    private ReceiverWrapper _receiverWrapper;
    private final AtomicReference<QueueStats> _stats = new AtomicReference<>();
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
