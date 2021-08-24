/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreServiceAppImpl.java 4097 2019-06-25 15:35:48Z SFB $
 */

package org.rvpf.store.server;

import java.io.File;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.ClassDef;
import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.Point;
import org.rvpf.base.UUID;
import org.rvpf.base.alert.Signal;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.store.Store;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.util.container.Listeners;
import org.rvpf.base.value.PointValue;
import org.rvpf.config.Config;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.metadata.entity.StoreEntity;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;
import org.rvpf.service.metadata.MetadataService;
import org.rvpf.service.metadata.app.MetadataServiceAppImpl;
import org.rvpf.store.server.archiver.Archiver;
import org.rvpf.store.server.polator.LinearPolator;
import org.rvpf.store.server.polator.Polator;
import org.rvpf.store.server.rmi.StoreSessionFactoryImpl;
import org.rvpf.store.server.som.SOMNotifier;
import org.rvpf.store.server.som.SOMReplicator;
import org.rvpf.store.server.som.SOMUpdatesListener;

/**
 * Store service application implementation.
 */
public abstract class StoreServiceAppImpl
    extends MetadataServiceAppImpl
    implements StoreServiceApp, ServiceThread.Target
{
    /**
     * Makes a notices filter.
     *
     * @param metadata The metadata.
     *
     * @return A notices filter.
     */
    public static Collection<Point> makeNoticesFilter(final Metadata metadata)
    {
        final Collection<Point> noticesFilter = new LinkedList<Point>();

        for (final Point point: metadata.getPointsCollection()) {
            final PointEntity pointEntity = (PointEntity) point;

            if (pointEntity.getStoreEntity().isPresent()) {
                final boolean notify;

                if (point.getParams().containsValueKey(Point.NOTIFY_PARAM)) {
                    notify = point
                        .getParams()
                        .getBoolean(Point.NOTIFY_PARAM, true);
                } else {
                    notify = pointEntity.hasResultRelations();
                }

                if (notify) {
                    noticesFilter.add(point);
                }
            }
        }

        return noticesFilter;
    }

    /**
     * Adds a notice.
     *
     * @param notice A point value.
     *
     * @throws InterruptedException When the service is stopped.
     */
    public void addNotice(
            @Nonnull final PointValue notice)
        throws InterruptedException
    {
        for (final NoticeListener listener: _noticeListeners) {
            listener.notify(notice);
        }
    }

    /**
     * Adds a notice listener.
     *
     * @param noticeListener The notice listener.
     */
    public final void addNoticeListener(final NoticeListener noticeListener)
    {
        _noticeListeners.add(noticeListener);
    }

    /**
     * Asks if the notices are filtered.
     *
     * <p>Allows an implementation override.</p>
     *
     * @return True if the notices are filtered.
     */
    public boolean areNoticesFiltered()
    {
        if (_noticesFiltered == null) {
            _noticesFiltered = Boolean
                .valueOf(
                    getServerProperties()
                        .getBoolean(NOTICES_FILTERED_PROPERTY));

            if (_noticesFiltered.booleanValue()) {
                getThisLogger().debug(StoreMessages.NOTICES_FILTERED);
            }
        }

        return _noticesFiltered.booleanValue();
    }

    /**
     * Gets the archiver.
     *
     * @return The optional archiver.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Archiver> getArchiver()
    {
        return _archiver;
    }

    /**
     * Gets the back-end limit.
     *
     * @return The back-end limit.
     */
    public final int getBackEndLimit()
    {
        if (_backEndLimit == null) {
            _backEndLimit = Integer
                .valueOf(
                    getServerProperties()
                        .getInt(BACK_END_LIMIT_PROPERTY, getResponseLimit()));

            if (_backEndLimit.equals(_responseLimit)) {
                if (_backEndLimit.intValue() > 0) {
                    _backEndLimit = Integer
                        .valueOf(_backEndLimit.intValue() + 1);
                }
            } else {
                getThisLogger()
                    .debug(
                        StoreMessages.BACK_END_LIMIT,
                        _backEndLimit.toString());
            }
        }

        return _backEndLimit.intValue();
    }

    /** {@inheritDoc}
     */
    @Override
    public final Optional<String> getEntityName()
    {
        return Optional.of(_entityName);
    }

    /**
     * Gets the notices filter.
     *
     * @return The list of point entities needing notices.
     */
    public final Collection<Point> getNoticesFilter()
    {
        return Require.notNull(_noticesFilter);
    }

    /**
     * Gets the inter/extra-polator for a point.
     *
     * @param point The point.
     *
     * @return The inter/extra-polator for the point.
     */
    @Nonnull
    @CheckReturnValue
    public final Polator getPolator(@Nonnull final Point point)
    {
        Polator polator = _polators.get(point);

        if (polator == null) {
            polator = Require.notNull(_polator);
        }

        return polator;
    }

    /**
     * Gets the replicator.
     *
     * @return The replicator.
     */
    @Nonnull
    @CheckReturnValue
    public final Replicator getReplicator()
    {
        return Require.notNull(_replicator);
    }

    /**
     * Gets the response limit.
     *
     * @return The response limit.
     */
    public int getResponseLimit()
    {
        if (_responseLimit == null) {
            _responseLimit = Integer
                .valueOf(
                    getServerProperties()
                        .getInt(
                                RESPONSE_LIMIT_PROPERTY,
                                        DEFAULT_RESPONSE_LIMIT));
            getThisLogger()
                .debug(
                    ServiceMessages.RESPONSE_LIMIT,
                    _responseLimit.toString());
        }

        return _responseLimit.intValue();
    }

    /** {@inheritDoc}
     */
    @Override
    public final synchronized String getServerName()
    {
        if (_serverName == null) {
            throw new IllegalStateException();
        }

        return _serverName;
    }

    /**
     * Gets the server properties.
     *
     * @return The server properties.
     */
    @Nonnull
    @CheckReturnValue
    public KeyedGroups getServerProperties()
    {
        return Require.notNull(_serverProperties);
    }

    /**
     * Gets the store data directory.
     *
     * @return The store data directory.
     */
    @Nonnull
    @CheckReturnValue
    public File getStoreDataDir()
    {
        if (_storeDataDir == null) {
            _storeDataDir = Config
                .dataDir(
                    Optional.of(getService().getDataDir()),
                    getServerProperties(),
                    STORE_DATA_DIR_PROPERTY,
                    DEFAULT_STORE_DATA_DIR);
            getThisLogger()
                .debug(
                    StoreMessages.STORE_DATA_DIR,
                    _storeDataDir.getAbsolutePath());
        }

        return Require.notNull(_storeDataDir);
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<StoreEntity> getStoreEntity()
    {
        return Optional.ofNullable(_storeEntity);
    }

    /**
     * Gets the store statistics.
     *
     * @return The store statistics.
     */
    @Nonnull
    @CheckReturnValue
    public final StoreStats getStoreStats()
    {
        return (StoreStats) getService().getStats();
    }

    /**
     * Asks if a null value removes that value.
     *
     * @return True if a null value removes that value.
     */
    @CheckReturnValue
    public final boolean isNullRemoves()
    {
        if (_nullRemoves == null) {
            _nullRemoves = Boolean
                .valueOf(
                    (_storeEntity != null)
                    && _storeEntity.getParams().getBoolean(
                        Store.NULL_REMOVES_PARAM));

            if (_nullRemoves.booleanValue()) {
                getThisLogger().debug(StoreMessages.NULL_REMOVES);
            }
        }

        return _nullRemoves.booleanValue();
    }

    /**
     * Asks if pull requests support are disabled.
     *
     * @return True if pull requests support is disabled.
     */
    @CheckReturnValue
    public final boolean isPullDisabled()
    {
        return _pullDisabled;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onNewMetadata(final Metadata metadata)
    {
        metadata.cleanUp();

        final StoreMetadataFilter storeMetadataFilter =
            (StoreMetadataFilter) metadata
                .getFilter();

        if (storeMetadataFilter.getStoreName().isPresent()) {    // Not ProxyStore.
            _storeEntity = storeMetadataFilter.getStoreEntity().get();

            if (_storeEntity == null) {
                getThisLogger()
                    .error(
                        ServiceMessages.STORE_NOT_FOUND,
                        getEntityName().orElse(null));

                return false;
            }

            getService().setSourceUUID(_storeEntity.getUUID().get());
        }

        for (final Iterator<Point> iterator =
                metadata.getPointsCollection().iterator();
                iterator.hasNext(); ) {
            final PointEntity pointEntity = (PointEntity) iterator.next();

            if (!pointEntity.setUp(metadata)) {
                return false;
            }

            pointEntity.clearInputs();
            pointEntity.clearResults();

            if (!pointEntity.getStoreEntity().isPresent()
                    || ((_storeEntity != null)
                        && (pointEntity.getStoreEntity().get()
                            != _storeEntity))) {
                iterator.remove();    // Forgets foreign points.
            }
        }

        metadata.cleanUp();

        return super.onNewMetadata(metadata);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onSignal(final Signal signal)
    {
        if (MetadataService.REFRESH_METADATA_SIGNAL
            .equalsIgnoreCase(signal.getName())) {
            _refreshMetadata = true;
            _semaphore.release();

            return false;
        }

        return true;
    }

    /**
     * Rebinds a UUID.
     *
     * @param oldUUID The old UUID.
     * @param newUUID The UUID.
     */
    public final synchronized void rebind(
            @Nonnull final UUID oldUUID,
            @Nonnull final UUID newUUID)
    {
        _sessionFactory.rebind(oldUUID, newUUID);
    }

    /**
     * Removes a notice listener.
     *
     * @param noticeListener The notice listener.
     */
    public final void removeNoticeListener(
            @Nonnull final NoticeListener noticeListener)
    {
        _noticeListeners.remove(Require.notNull(noticeListener));
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
        throws InterruptedException
    {
        try {
            for (;;) {
                doPendingActions();
                _semaphore.acquire();
            }
        } catch (final ServiceNotAvailableException exception) {
            onServiceNotAvailableException(exception);
        }
    }

    /**
     * Sends the notices.
     *
     * @throws InterruptedException When the service is stopped.
     */
    public void sendNotices()
        throws InterruptedException
    {
        for (final NoticeListener listener: _noticeListeners) {
            listener.commit();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final MetadataService service)
    {
        return super.setUp(service) && setUp();
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
    {
        final Notifier notifier = _notifier;

        if (notifier != null) {
            notifier.start();
        }

        _server.start();

        _replicator.start();

        for (final UpdatesListener updatesListener: _updatesListeners) {
            updatesListener.start();
        }

        final Optional<Archiver> archiver = getArchiver();

        if (archiver.isPresent()) {
            archiver.get().start();
        }

        final ServiceThread thread = new ServiceThread(
            this,
            "Store service for [" + getService().getServiceName() + "]");

        if (_thread.compareAndSet(null, thread)) {
            getThisLogger()
                .debug(ServiceMessages.STARTING_THREAD, thread.getName());
            thread.start();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        final ServiceThread thread = _thread.getAndSet(null);

        if (thread != null) {
            getThisLogger()
                .debug(ServiceMessages.STOPPING_THREAD, thread.getName());
            Require
                .ignored(
                    thread.interruptAndJoin(getThisLogger(), getJoinTimeout()));
        }

        unregisterServer();

        final Optional<Archiver> archiver = getArchiver();

        if (archiver.isPresent()) {
            archiver.get().stop();
        }

        for (final UpdatesListener updatesListener: _updatesListeners) {
            updatesListener.stop();
        }

        if (_replicator != null) {
            _replicator.close();
            _replicator.join();
        }

        if (_server != null) {
            _server.stop();
            _server = null;
        }

        final Notifier notifier = _notifier;

        if (notifier != null) {
            notifier.close();
            notifier.join();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        if (_archiver.isPresent()) {
            _archiver.get().tearDown();
            _archiver = Optional.empty();
        }

        for (final UpdatesListener updatesListener: _updatesListeners) {
            updatesListener.tearDown();
        }

        _updatesListeners.clear();

        if (_notifier != null) {
            _notifier.tearDown();
            _notifier = null;
        }

        if (_replicator != null) {
            _replicator.tearDown();
            _replicator = null;
        }

        _storeEntity = null;
        _noticesFilter = null;

        super.tearDown();
    }

    /**
     * Does pending actions.
     *
     * <p>Caution: this is called while synchronized.</p>
     *
     * @throws InterruptedException When the service is stopped.
     * @throws ServiceNotAvailableException When the service is not available.
     */
    protected void doPendingActions()
        throws InterruptedException, ServiceNotAvailableException
    {
        if (_refreshMetadata) {
            getThisLogger().info(StoreMessages.REFRESHING_METADATA);
            _refreshMetadata = false;
            getService().suspend();

            try {
                if (!refreshMetadata()) {
                    throw new ServiceNotAvailableException();
                }

                for (final UpdatesListener updatesListener: _updatesListeners) {
                    updatesListener.onMetadataRefreshed(getMetadata());
                }

                final Optional<Archiver> archiver = getArchiver();

                if ((archiver.isPresent()) && !archiver.get().setUpPoints()) {
                    throw new ServiceNotAvailableException();
                }

                getThisLogger().debug(ServiceMessages.METADATA_REFRESHED);
            } finally {
                getService().resume();
            }
        }
    }

    /**
     * Gets the default store name.
     *
     * @return The default store name.
     */
    @Nonnull
    @CheckReturnValue
    protected String getDefaultStoreName()
    {
        return Store.DEFAULT_STORE_NAME;
    }

    /**
     * Refreshes the metadata.
     *
     * @return True on success.
     *
     * @throws InterruptedException When the service is stopped.
     * @throws ServiceNotAvailableException When the service is not available.
     */
    @CheckReturnValue
    protected boolean refreshMetadata()
        throws InterruptedException, ServiceNotAvailableException
    {
        if (!getService().reloadMetadata()) {
            return false;
        }

        refreshNoticesFilter();
        _replicator.useNewMetadata();

        return true;
    }

    /**
     * Refreshes the notices filter.
     */
    protected final void refreshNoticesFilter()
    {
        if (areNoticesFiltered()) {
            _noticesFilter = makeNoticesFilter(getMetadata());
            getThisLogger()
                .info(
                    StoreMessages.STORE_WILL_NOTIFY,
                    String.valueOf(_noticesFilter.size()),
                    Integer.valueOf(_noticesFilter.size()));
        } else {
            _noticesFilter = null;
        }
    }

    /**
     * Registers the server.
     *
     * @param server The server.
     *
     * @return True on success.
     */
    @CheckReturnValue
    protected final boolean registerServer(@Nonnull final StoreServer server)
    {
        return registerServer(
            _storeEntity.getParams().getString(Store.NAME_PARAM),
            server);
    }

    /**
     * Registers the server.
     *
     * @param serverPath The optional server path.
     * @param server The server.
     *
     * @return True on success.
     */
    @CheckReturnValue
    protected final synchronized boolean registerServer(
            @Nonnull final Optional<String> serverPath,
            @Nonnull final StoreServer server)
    {
        _sessionFactory = new StoreSessionFactoryImpl();

        if (!_sessionFactory
            .setUp(getService().getConfig(), getServerProperties(), server)) {
            return false;
        }

        _serverName = getService()
            .registerServer(
                _sessionFactory,
                serverPath.isPresent()
                ? serverPath.get(): getEntityName().get());

        if (_serverName == null) {
            return false;
        }

        _server = server;

        for (final UpdatesListener updatesListener: _updatesListeners) {
            updatesListener.setServer(_server);
        }

        return true;
    }

    /**
     * Sets up this store service.
     *
     * @return True on success.
     */
    @CheckReturnValue
    protected boolean setUp()
    {
        return setUp(DEFAULT_SERVER_PROPERTIES);
    }

    /**
     * Sets up using the specified server properties.
     *
     * @param serverPropertiesName The server properties name.
     *
     * @return True on success.
     */
    @CheckReturnValue
    protected final boolean setUp(final String serverPropertiesName)
    {
        final Config config = getConfig();
        final StoreMetadataFilter filter;

        _serverProperties = config.getPropertiesGroup(serverPropertiesName);

        _entityName = config
            .getStringValue(
                STORE_NAME_PROPERTY,
                Optional.of(getDefaultStoreName()))
            .orElse(null);

        // Creates the replicator.

        final ClassDef replicatorClassDef = getServerProperties()
            .getClassDef(
                StoreServiceAppImpl.REPLICATOR_CLASS_PROPERTY,
                StoreServiceAppImpl.DEFAULT_REPLICATOR);

        _replicator = replicatorClassDef.createInstance(Replicator.class);

        if (_replicator == null) {
            return false;
        }

        if (!_replicator.setUp(this)) {
            return false;
        }

        // Loads the metadata.

        filter = storeMetadataFilter(
            getEntityName().get(),
            _replicator.getPartnerNames());

        if (filter == null) {
            return false;
        }

        if (filter.getStoreName().isPresent()) {    // ProxyStore has no entity name.
            getThisLogger()
                .info(ServiceMessages.STORE_NAME, getEntityName().orElse(null));
        }

        final SecurityContext securityContext = new SecurityContext(
            getThisLogger());

        if (!securityContext
            .setUp(config.getProperties(), KeyedGroups.MISSING_KEYED_GROUP)) {
            return false;
        }

        filter
            .setPermissionsNeeded(
                !securityContext.getRealmProperties().isMissing());

        getService().saveConfigState();    // Allows Metadata reload.

        if (!loadMetadata(filter)) {
            return false;
        }

        refreshNoticesFilter();

        // Configure updates listeners.

        final KeyedGroups[] listenersProperties = getServerProperties()
            .getGroups(UPDATES_LISTENER_PROPERTIES);

        for (final KeyedGroups listenerProperties: listenersProperties) {
            final ClassDef listenerClassDef = listenerProperties
                .getClassDef(
                    UPDATES_LISTENER_CLASS_PROPERTY,
                    DEFAULT_UPDATES_LISTENER);

            if (listenerClassDef != null) {
                getThisLogger()
                    .info(StoreMessages.UPDATES_LISTENER, listenerClassDef);

                final UpdatesListener updatesListener = listenerClassDef
                    .createInstance(UpdatesListener.class);

                if ((updatesListener == null)
                        || !updatesListener.setUp(this, listenerProperties)) {
                    return false;
                }

                _updatesListeners.add(updatesListener);
            }
        }

        // Creates the polators.

        final Map<ClassDef, Polator> polators = new IdentityHashMap<>();
        ClassDef polatorClassDef = getServerProperties()
            .getClassDef(POLATOR_CLASS_PROPERTY, DEFAULT_POLATOR);

        if (polatorClassDef != DEFAULT_POLATOR) {
            getThisLogger().info(StoreMessages.POLATOR, polatorClassDef);
        }

        _polator = polatorClassDef.createInstance(Polator.class);

        if (_polator == null) {
            return false;
        }

        polators.put(polatorClassDef, _polator);

        for (final Point point: getMetadata().getPointsCollection()) {
            polatorClassDef = point
                .getParams()
                .getClassDef(Point.POLATOR_PARAM, Optional.empty())
                .orElse(null);

            if (polatorClassDef != null) {
                Polator polator = polators.get(polatorClassDef);

                if (polator == null) {
                    polator = polatorClassDef.createInstance(Polator.class);

                    if (polator == null) {
                        return false;
                    }

                    polators.put(polatorClassDef, polator);
                }

                _polators.put(point, polator);
            }
        }

        // Completes the replicator set up.

        if (!_replicator.setUpImpl()) {
            return false;
        }

        _replicator.useNewMetadata();

        // Checks if pull is disabled.

        _pullDisabled = getServerProperties()
            .getBoolean(PULL_DISABLED_PROPERTY);

        if (_pullDisabled) {
            getThisLogger().info(StoreMessages.PULL_DISABLED);
        }

        // Logs the point count.

        final Integer pointCount = Integer
            .valueOf(getMetadata().getPointsCollection().size());

        getThisLogger()
            .info(
                StoreMessages.STORE_SET_UP,
                pointCount.toString(),
                pointCount);

        return true;
    }

    /**
     * Sets up the archiver.
     *
     * @return True on success.
     */
    @CheckReturnValue
    protected final boolean setUpArchiver()
    {
        if (Archiver.supports(_server)) {
            final Optional<Archiver> archiver = _server.newArchiver();

            if (archiver.isPresent() && !archiver.get().setUp(this)) {
                return false;
            }

            _archiver = archiver;
        }

        return true;
    }

    /**
     * Sets up the notifier.
     *
     * @return True on success.
     */
    @CheckReturnValue
    protected final boolean setUpNotifier()
    {
        final ClassDef notifierClassDef = getServerProperties()
            .getClassDef(NOTIFIER_CLASS_PROPERTY, DEFAULT_NOTIFIER_CLASS);

        if (notifierClassDef != DEFAULT_NOTIFIER_CLASS) {
            getThisLogger()
                .info(StoreMessages.NOTIFIER_CLASS, notifierClassDef);
        }

        _notifier = notifierClassDef.createInstance(Notifier.class);

        if (_notifier == null) {
            return false;
        }

        return _notifier.setUp(this);
    }

    /**
     * Returns a store metadata filter.
     *
     * @param storeName The store's name.
     * @param partnerNames The name of the store partners (optional).
     *
     * @return A store metadata filter (null on failure).
     */
    @Nullable
    @CheckReturnValue
    protected StoreMetadataFilter storeMetadataFilter(
            final String storeName,
            final Collection<String> partnerNames)
    {
        return new StoreMetadataFilter(
            Optional.of(storeName),
            Optional.of(partnerNames));
    }

    /**
     * Unregisters the store server.
     */
    protected final synchronized void unregisterServer()
    {
        if (_serverName != null) {
            getService().unregisterServer(_serverName);
            _serverName = null;
        }

        if (_sessionFactory != null) {
            _sessionFactory.tearDown();
            _sessionFactory = null;
        }
    }

    /**
     * Specifies the maximum number of point values that may be requested from
     * the backend. Zero means no limit.
     */
    public static final String BACK_END_LIMIT_PROPERTY = "backend.limit";

    /** Default notifier. */
    public static final ClassDef DEFAULT_NOTIFIER_CLASS = new ClassDefImpl(
        SOMNotifier.class);

    /** Default inter/extra-polator. */
    public static final ClassDef DEFAULT_POLATOR = new ClassDefImpl(
        LinearPolator.class);

    /** Default replicator. */
    public static final ClassDef DEFAULT_REPLICATOR = new ClassDefImpl(
        SOMReplicator.class);

    /** Default response limit. */
    public static final int DEFAULT_RESPONSE_LIMIT = 1000;

    /** Default server properties. */
    public static final String DEFAULT_SERVER_PROPERTIES = "store.server";

    /** Default store data directory path. */
    public static final String DEFAULT_STORE_DATA_DIR = "store";

    /** Default updates listener. */
    public static final ClassDef DEFAULT_UPDATES_LISTENER = new ClassDefImpl(
        SOMUpdatesListener.class);

    /**
     * When enabled, the store will send notices only for points acting as
     * trigger for an other value. Otherwise, it will send notices updates on
     * any known point.
     */
    public static final String NOTICES_FILTERED_PROPERTY = "notices.filtered";

    /** Specifies an alternative to the SOM Notifier. */
    public static final String NOTIFIER_CLASS_PROPERTY = "notifier.class";

    /** Specifies an alternative to the linear inter/extra-polator. */
    public static final String POLATOR_CLASS_PROPERTY = "polator.class";

    /** Pull disabled property. */
    public static final String PULL_DISABLED_PROPERTY = "pull.disabled";

    /** Specifies an alternative to the SOM replicator. */
    public static final String REPLICATOR_CLASS_PROPERTY = "replicator.class";

    /**
     * Specifies the maximum number of point values that may be grouped in a
     * single response.
     */
    public static final String RESPONSE_LIMIT_PROPERTY = "response.limit";

    /** Specifies the store data directory path. */
    public static final String STORE_DATA_DIR_PROPERTY = "data.dir";

    /**
     * This property is provided to a store, usually in its 'service' element,
     * to identify in the metadata the 'Store' entity that it is instantiating.
     * Its value defaults to 'Store'.
     */
    public static final String STORE_NAME_PROPERTY = "store.name";

    /** This property may be used to specify a queued updates listener. */
    public static final String UPDATES_LISTENER_CLASS_PROPERTY =
        "listener.class";

    /** Updates listener properties. */
    public static final String UPDATES_LISTENER_PROPERTIES = "updates.listener";

    private Optional<Archiver> _archiver = Optional.empty();
    private Integer _backEndLimit;
    private String _entityName;
    private final Listeners<NoticeListener> _noticeListeners =
        new Listeners<>();
    private Collection<Point> _noticesFilter;
    private Boolean _noticesFiltered;
    private Notifier _notifier;
    private Boolean _nullRemoves;
    private Polator _polator;
    private final Map<Point, Polator> _polators = new IdentityHashMap<>();
    private boolean _pullDisabled;
    private volatile boolean _refreshMetadata;
    private Replicator _replicator;
    private Integer _responseLimit;
    private final Semaphore _semaphore = new Semaphore(0);
    private StoreServer _server;
    private String _serverName;
    private KeyedGroups _serverProperties;
    private StoreSessionFactoryImpl _sessionFactory;
    private volatile File _storeDataDir;
    private StoreEntity _storeEntity;
    private final AtomicReference<ServiceThread> _thread =
        new AtomicReference<>();
    private final Collection<UpdatesListener> _updatesListeners =
        new LinkedList<>();
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
