/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StatsHolder.java 4097 2019-06-25 15:35:48Z SFB $
 */

package org.rvpf.service;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

import org.rvpf.base.Stats;
import org.rvpf.base.StatsOwner;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;

/**
 * Stats holder.
 *
 * <p>Base class for MBeans holding {@link Stats}.</p>
 */
@ThreadSafe
public abstract class StatsHolder
    extends NotificationBroadcasterSupport
    implements StatsHolderMBean, StatsOwner
{
    /**
     * Gets the default JMX domain.
     *
     * @return The default JMX domain.
     */
    @Nonnull
    @CheckReturnValue
    public static String getDefaultDomain()
    {
        return _DEFAULT_DOMAIN;
    }

    /**
     * Makes an object name.
     *
     * @param domain The object's domain.
     * @param typeValue The value for the 'type' key.
     * @param nameValue The value for the 'name' key (optional).
     *
     * @return The object name.
     */
    @Nonnull
    @CheckReturnValue
    public static ObjectName makeObjectName(
            @Nonnull final String domain,
            @Nonnull final String typeValue,
            @Nonnull final Optional<String> nameValue)
    {
        try {
            return ObjectName
                .getInstance(
                    domain + ":type=" + typeValue
                    + (nameValue.isPresent()
                       ? (",name=" + nameValue.get()): ""));
        } catch (final MalformedObjectNameException exception) {
            throw new RuntimeException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public ObjectName getObjectName()
    {
        return Require.notNull(_objectName.get());
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<String> getObjectVersion()
    {
        return Optional.empty();
    }

    /** {@inheritDoc}
     */
    @Override
    public final String[] getStatsStrings()
    {
        final Optional<? extends Stats> stats = getStats();
        final String string = stats.isPresent()? stats.get().toString(): "";

        return string.split("\\n");
    }

    /**
     * Makes an object name.
     *
     * @param nameValue The value for the 'name' key (optional).
     *
     * @return The object name.
     */
    @Nonnull
    @CheckReturnValue
    public ObjectName makeObjectName(@Nonnull final Optional<String> nameValue)
    {
        if (nameValue.isPresent() && (nameValue.get().contains(":type="))) {
            try {
                return ObjectName.getInstance(nameValue.get());
            } catch (final MalformedObjectNameException exception) {
                throw new RuntimeException(exception);
            }
        }

        // Makes a type from the class name without its package name.
        String typeValue = getClass().getSimpleName();

        // Removes the "Activator" suffix if present.
        if (typeValue.endsWith(ACTIVATOR_CLASS_NAME_SUFFIX)) {
            typeValue = typeValue
                .substring(
                    0,
                    typeValue.length() - ACTIVATOR_CLASS_NAME_SUFFIX.length());
        }

        return makeObjectName(getDefaultDomain(), typeValue, nameValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void onStatsUpdated()
    {
        sendNotification(
            new UpdateNotification(
                getObjectName(),
                _sequenceNumber.incrementAndGet(),
                getStats().get()));
    }

    /**
     * Sets the object name.
     *
     * @param objectName The object name.
     */
    public final void setObjectName(@Nonnull final ObjectName objectName)
    {
        if (!_objectName.compareAndSet(null, objectName)
                && !Objects.equals(_objectName.get(), objectName)) {
            getThisLogger()
                .warn(
                    ServiceMessages.OBJECT_NAME_CHANGE_IGNORED,
                    _objectName.get(),
                    objectName);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public abstract void updateStats();

    /**
     * Gets the stats.
     *
     * @return The optional stats.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract Optional<? extends Stats> getStats();

    /**
     * Gets the logger.
     *
     * @return The logger.
     */
    @Nonnull
    @CheckReturnValue
    protected final Logger getThisLogger()
    {
        if (_logger == null) {
            _logger = Logger.getInstance(getClass());
        }

        return _logger;
    }

    /**
     * Returns the object name.
     *
     * @return The object name (empty if undefined).
     */
    @Nonnull
    @CheckReturnValue
    protected final Optional<ObjectName> objectName()
    {
        return Optional.ofNullable(_objectName.get());
    }

    /** Activator class name suffix. */
    public static final String ACTIVATOR_CLASS_NAME_SUFFIX = "Activator";

    /** Default domain. */
    public static final String DEFAULT_DOMAIN = "org.rvpf";

    /** Domain property. */
    public static final String DOMAIN_PROPERTY = "rvpf.jmx.domain";

    /** Stats update type. */
    public static final String STATS_UPDATE_TYPE = "rvpf.stats.update";

    /** JMX name key. */
    protected static final String NAME_KEY = "name";

    /** JMX type key. */
    protected static final String TYPE_KEY = "type";

    /**  */

    private static final String _DEFAULT_DOMAIN = System
        .getProperty(DOMAIN_PROPERTY, DEFAULT_DOMAIN);

    private volatile Logger _logger;
    private final AtomicReference<ObjectName> _objectName =
        new AtomicReference<>();
    @GuardedBy("this")
    private final AtomicLong _sequenceNumber = new AtomicLong();

    /**
     * Update notification.
     */
    public static final class UpdateNotification
        extends Notification
    {
        /**
         * Constructs an instance.
         *
         * @param notificationSource The notification source.
         * @param sequenceNumber The sequence number.
         * @param stats The stats.
         */
        UpdateNotification(
                @Nonnull final ObjectName notificationSource,
                final long sequenceNumber,
                @Nonnull final Stats stats)
        {
            super(STATS_UPDATE_TYPE, notificationSource, sequenceNumber);

            _stats = stats;
        }

        /**
         * Gets the stats.
         *
         * @return The stats.
         */
        @Nonnull
        @CheckReturnValue
        public Stats getStats()
        {
            return _stats;
        }

        private static final long serialVersionUID = 1L;

        private final Stats _stats;
    }
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
