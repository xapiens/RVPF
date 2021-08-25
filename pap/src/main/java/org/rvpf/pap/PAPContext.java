/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PAPContext.java 4084 2019-06-15 18:32:47Z SFB $
 */

package org.rvpf.pap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.Attributes;
import org.rvpf.base.Origin;
import org.rvpf.base.Point;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.Traces;
import org.rvpf.base.util.container.IdentityHashSet;
import org.rvpf.base.value.filter.ValueFilter;
import org.rvpf.document.loader.MetadataDocumentLoader;
import org.rvpf.metadata.Metadata;

/**
 * PAP context.
 */
public abstract class PAPContext
{
    /**
     * Constructs an instance.
     *
     * @param support The protocol support.
     * @param metadata The optional metadata.
     * @param traces The traces (optional).
     */
    protected PAPContext(
            @Nonnull final PAPSupport support,
            @Nonnull final Optional<Metadata> metadata,
            @Nonnull final Optional<Traces> traces)
    {
        Require.notNull(support);

        _support = Require.notNull(support);
        _metadata = metadata.orElse(null);
        _traces = traces.orElse(new Traces());
        _logger = Logger.getInstance(getClass());

        new PAPVersion().logImplementationIdent(true);
    }

    /**
     * Fetches the metadata.
     *
     * @param metadataFilter The metadata filter.
     * @param from The optional location from which to fetch the metadata.
     * @param uuid The optional UUID for the metadata cache.
     *
     * @return The metadata (null on failure).
     */
    @Nullable
    @CheckReturnValue
    public static Metadata fetchMetadata(
            @Nonnull final PAPMetadataFilter metadataFilter,
            @Nonnull final Optional<String> from,
            @Nonnull final Optional<UUID> uuid)
    {
        final Metadata metadata = MetadataDocumentLoader
            .fetchMetadata(
                Require.notNull(metadataFilter),
                Optional.empty(),
                uuid,
                from);

        if (metadata != null) {
            if (!metadata.validatePointsRelationships()) {
                return null;
            }

            metadata.cleanUp();
        }

        return metadata;
    }

    /**
     * Adds a remote origin.
     *
     * @param remoteOrigin The remote origin.
     * @param originAttributes The origin attributes.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public abstract boolean addRemoteOrigin(
            @Nonnull Origin remoteOrigin,
            @Nonnull Attributes originAttributes);

    /**
     * Adds a remote point.
     *
     * @param remotePoint The remote point.
     * @param pointAttributes The point attributes.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public abstract boolean addRemotePoint(
            @Nonnull Point remotePoint,
            @Nonnull Attributes pointAttributes);

    /**
     * Gets the metadata.
     *
     * @return The metadata.
     */
    @Nonnull
    @CheckReturnValue
    public final Metadata getMetadata()
    {
        return _metadata;
    }

    /**
     * Gets the protocol name.
     *
     * @return The protocol name.
     */
    @Nonnull
    @CheckReturnValue
    public abstract String getProtocolName();

    /**
     * Gets a remote origin by its name (not case sensitive).
     *
     * @param remoteOriginName The remote origin's name.
     *
     * @return The remote origin (empty if unknown).
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<Origin> getRemoteOrigin(
            @Nonnull final Optional<String> remoteOriginName)
    {
        if (remoteOriginName.isPresent()) {
            return Optional
                .ofNullable(
                    _remoteOriginByName
                        .get(
                                remoteOriginName
                                        .get()
                                        .trim()
                                        .toUpperCase(Locale.ROOT)));
        }

        return Optional.empty();
    }

    /**
     * Gets the remote point for a UUID.
     *
     * @param uuid The UUID.
     *
     * @return The remote point (empty if unknown).
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<Point> getRemotePoint(@Nonnull final UUID uuid)
    {
        return Optional.ofNullable(_remotePointByUUID.get(uuid));
    }

    /**
     * Gets the remote points.
     *
     * @return The remote points.
     */
    @Nonnull
    @CheckReturnValue
    public final Collection<Point> getRemotePoints()
    {
        return _remotePointByUUID.values();
    }

    /**
     * Gets all the remote proxies.
     *
     * @return The remote proxies.
     */
    @Nonnull
    @CheckReturnValue
    public Collection<? extends PAPProxy> getRemoteProxies()
    {
        return _remoteProxyByOrigin.values();
    }

    /**
     * Gets the remote proxy for a remote point.
     *
     * @param remotePoint The remote point.
     *
     * @return The remote proxy (empty if none).
     */
    @Nonnull
    @CheckReturnValue
    public Optional<? extends PAPProxy> getRemoteProxy(final Point remotePoint)
    {
        final Optional<? extends Origin> origin = remotePoint.getOrigin();

        if (!origin.isPresent()) {
            _logger.warn(PAPMessages.MISSING_ORIGIN, remotePoint);

            return Optional.empty();
        }

        final Optional<? extends PAPProxy> remoteProxy = getRemoteProxyByOrigin(
            origin.get());

        if (!remoteProxy.isPresent()) {
            _logger
                .warn(PAPMessages.UNKNOWN_ORIGIN, origin.get().getName().get());
        }

        return remoteProxy;
    }

    /**
     * Gets the remote proxy for the specified remote origin.
     *
     * <p>Used by tests.</p>
     *
     * @param remoteOrigin The remote origin.
     *
     * @return The remote proxy (empty if none).
     */
    @Nonnull
    @CheckReturnValue
    public Optional<? extends PAPProxy> getRemoteProxyByOrigin(
            final Origin remoteOrigin)
    {
        return Optional.ofNullable(_remoteProxyByOrigin.get(remoteOrigin));
    }

    /**
     * Gets the support.
     *
     * @return The support.
     */
    @Nonnull
    @CheckReturnValue
    public PAPSupport getSupport()
    {
        return _support;
    }

    /**
     * Gets the traces.
     *
     * @return The traces.
     */
    @Nonnull
    @CheckReturnValue
    public final Traces getTraces()
    {
        return _traces;
    }

    /**
     * Gets the step filters.
     *
     * @return The step filters.
     */
    @Nonnull
    @CheckReturnValue
    public final Map<Point, ValueFilter> getValueFilters()
    {
        return _valueFilters;
    }

    /**
     * Asks if the logger has logged at least at the specified level.
     *
     * @param logLevel A LogLevel.
     *
     * @return True if the logger has logged at least at the specified level.
     */
    @CheckReturnValue
    public final boolean hasLogged(@Nonnull final Logger.LogLevel logLevel)
    {
        return _logger.hasLogged(logLevel);
    }

    /**
     * Asks if this is a client context.
     *
     * @return True if this is a client context.
     */
    @CheckReturnValue
    public abstract boolean isClientContext();

    /**
     * Asks if a point is active.
     *
     * @param point The point.
     *
     * @return True if the point is active.
     */
    @CheckReturnValue
    public final boolean isPointActive(@Nonnull final Point point)
    {
        return !_inactivePoints.contains(point);
    }

    /**
     * Asks if this is a server context.
     *
     * @return True if this is a server context.
     */
    @CheckReturnValue
    public final boolean isServerContext()
    {
        return !isClientContext();
    }

    /**
     * Sets up this context.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public boolean setUp()
    {
        final String attributesUsage = _support.getAttributesUsage();

        for (final Origin origin: getMetadata().getOriginEntities()) {
            final Optional<Attributes> originAttributes = origin
                .getAttributes(attributesUsage);

            if (originAttributes.isPresent()
                    && isRemoteOriginNeeded(origin, originAttributes.get())) {
                if (!addRemoteOrigin(origin, originAttributes.get())) {
                    return false;
                }
            }
        }

        for (final Point point: getMetadata().getPointsCollection()) {
            final Optional<Attributes> pointAttributes = point
                .getAttributes(attributesUsage);

            if (pointAttributes.isPresent()) {
                if (pointAttributes.get().getBoolean(PAP.INACTIVE_ATTRIBUTE)) {
                    _inactivePoints.add(point);
                }

                final Optional<Attributes> originAttributes = point
                    .getOrigin()
                    .get()
                    .getAttributes(pointAttributes.get().getUsage());

                if ((originAttributes.isPresent())
                        && isRemotePointNeeded(point, pointAttributes.get())) {
                    if (!addRemotePoint(point, pointAttributes.get())
                            && hasLogged(Logger.LogLevel.ERROR)) {
                        return false;
                    }

                    if (originAttributes
                        .get()
                        .getBoolean(PAP.INACTIVE_ATTRIBUTE)) {
                        _inactivePoints.add(point);
                    }
                }
            }
        }

        return !hasLogged(Logger.LogLevel.WARN);
    }

    /**
     * Tears down what has been set up.
     */
    public void tearDown()
    {
        _valueFilters.clear();
        _remotePointByUUID.clear();
        _remoteOriginByName.clear();
        _remoteProxyByOrigin.clear();
        _traces.tearDown();
    }

    /**
     * Gets the default port for a remote origin.
     *
     * @return The default port for a remote origin.
     */
    @CheckReturnValue
    protected int getDefaultPortForRemoteOrigin()
    {
        return 0;
    }

    /**
     * Gets the remote proxy by origin map.
     *
     * @return The remote proxy by origin map.
     */
    @Nonnull
    @CheckReturnValue
    protected final Map<Origin, PAPProxy> getRemoteProxyByOrigin()
    {
        return Collections.unmodifiableMap(_remoteProxyByOrigin);
    }

    /**
     * Gets the logger for this instance.
     *
     * @return The logger.
     */
    @Nonnull
    @CheckReturnValue
    protected final Logger getThisLogger()
    {
        return _logger;
    }

    /**
     * Asks if a remote origin is needed.
     *
     * @param remoteOrigin The remote origin.
     * @param originAttributes The origin attributes.
     *
     * @return True if the remote origin is needed.
     */
    @CheckReturnValue
    protected boolean isRemoteOriginNeeded(
            @Nonnull final Origin remoteOrigin,
            @Nonnull final Attributes originAttributes)
    {
        return true;
    }

    /**
     * Asks if a remote point is needed.
     *
     * @param remotePoint The remote point.
     * @param pointAttributes The point attributes.
     *
     * @return True if the remote point is needed.
     */
    @CheckReturnValue
    protected boolean isRemotePointNeeded(
            @Nonnull final Point remotePoint,
            @Nonnull final Attributes pointAttributes)
    {
        return true;
    }

    /**
     * Returns a new remote proxy.
     *
     * @param remoteOrigin The remote origin for the proxy.
     *
     * @return The new remote proxy.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract PAPProxy newRemoteProxy(@Nonnull Origin remoteOrigin);

    /**
     * Registers a remote point.
     *
     * @param remotePoint The remote point.
     */
    protected final void registerRemotePoint(@Nonnull final Point remotePoint)
    {
        final ValueFilter valueFilter = remotePoint.filter();

        if (!valueFilter.isDisabled()) {
            _valueFilters.put(remotePoint, valueFilter);
        }

        _remotePointByUUID.put(remotePoint.getUUID().get(), remotePoint);
    }

    /**
     * Registers a remote proxy.
     *
     * @param remoteProxy The remote proxy.
     */
    protected final void registerRemoteProxy(
            @Nonnull final PAPProxy remoteProxy)
    {
        final Origin remoteOrigin = remoteProxy.getOrigin();
        final String remoteOriginName = remoteOrigin.getName().get();

        _remoteOriginByName
            .put(remoteOriginName.toUpperCase(Locale.ROOT), remoteOrigin);
        _remoteProxyByOrigin.put(remoteOrigin, remoteProxy);
        _logger.debug(PAPMessages.CONFIGURED_PROXY, remoteOrigin);
    }

    private final Set<Point> _inactivePoints = new IdentityHashSet<>();
    private final Logger _logger;
    private final Metadata _metadata;
    private final Map<Origin, PAPProxy> _remoteProxyByOrigin =
        new IdentityHashMap<>();
    private final Map<UUID, Point> _remotePointByUUID = new HashMap<>();
    private final Map<String, Origin> _remoteOriginByName = new HashMap<>();
    private final PAPSupport _support;
    private final Traces _traces;
    private final Map<Point, ValueFilter> _valueFilters =
        new IdentityHashMap<>();
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
