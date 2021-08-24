/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ConfigProperties.java 4103 2019-07-01 13:31:25Z SFB $
 */

package org.rvpf.config;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListenerProxy;
import java.beans.PropertyChangeSupport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Message;
import org.rvpf.base.logger.Messages;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.util.container.KeyedValues;
import org.rvpf.service.ServiceContext;
import org.rvpf.service.ServiceMessages;

/**
 * Dictionary of values.
 *
 * <p>This class extends {@link org.rvpf.base.util.container.KeyedGroups} by
 * providing property change listener and dictionary hierarchy support.</p>
 */
@NotThreadSafe
public class ConfigProperties
    extends KeyedGroups
{
    /**
     * Constructs an instance.
     */
    public ConfigProperties()
    {
        this(BaseMessages.VALUE_TYPE, Optional.empty());
    }

    /**
     * Constructs an instance.
     *
     * @param type The type of values (for logging purpose).
     */
    public ConfigProperties(@Nonnull final Messages.Entry type)
    {
        this(type, Optional.empty());
    }

    /**
     * Constructs an instance.
     *
     * @param name The optional name for the properties as a group.
     */
    public ConfigProperties(@Nonnull final Optional<String> name)
    {
        this(BaseMessages.VALUE_TYPE, name);
    }

    /**
     * Constructs an instance.
     *
     * @param type The type of values (for logging purpose).
     * @param name The optional name for the properties as a group.
     */
    public ConfigProperties(
            @Nonnull final Messages.Entry type,
            @Nonnull final Optional<String> name)
    {
        super(type.toString(), name);
    }

    private ConfigProperties(final ConfigProperties other)
    {
        super(other);

        if (other._overrider != null) {
            _overrider = (!(other._overrider instanceof ServiceContext))
                    ? other._overrider
                        .copy(): null;
        }

        if (other._overriden != null) {
            _overriden = (!(other._overriden instanceof ServiceContext))
                    ? other._overriden
                        .copy(): null;
        }
    }

    /**
     * Adds all the properties from an other instance.
     *
     * <p>The new values and groups are appended to those already present.</p>
     *
     * @param otherInstance The other instance.
     */
    public void add(@Nonnull final KeyedGroups otherInstance)
    {
        super.addAll(otherInstance);
    }

    /**
     * Adds properties.
     *
     * <p>The new properties are appended to those already present.</p>
     *
     * @param properties The new properties.
     */
    public void add(@Nonnull final Properties properties)
    {
        for (final Map.Entry<?, ?> entry: properties.entrySet()) {
            add((String) entry.getKey(), entry.getValue());
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void add(final String key, final Object value)
    {
        if ((_changeSupport != null) && _changeSupport.hasListeners(key)) {
            setValue(key, value);
        } else {
            super.add(key, value);
        }
    }

    /**
     * Adds a property change listener.
     *
     * @param key The key of the property to be monitored.
     * @param listener The listener.
     */
    public final void addPropertyChangeListener(
            @Nonnull final String key,
            @Nonnull final PropertyChangeListener listener)
    {
        if (_changeSupport == null) {
            _changeSupport = new PropertyChangeSupport(this);
        }

        _changeSupport
            .addPropertyChangeListener(
                Require.notNull(key),
                Require.notNull(listener));
    }

    /**
     * Clears the overriden properties.
     */
    public void clearOverriden()
    {
        _overriden = null;
    }

    /**
     * Asks if the key is associated with a group.
     *
     * @param key The key to look for.
     *
     * @return True if the key is associated with a group.
     */
    @Override
    public final boolean containsGroupKey(final String key)
    {
        if ((_overrider != null) && _overrider.containsGroupKey(key)) {
            return true;
        }

        if (super.containsGroupKey(key)) {
            return true;
        }

        return (_overriden != null) && _overriden.containsGroupKey(key);
    }

    /**
     * Asks if the key is associated with a value.
     *
     * @param key The key to look for.
     *
     * @return True if the key is associated with a value.
     */
    @Override
    public final boolean containsValueKey(final String key)
    {
        if ((_overrider != null) && _overrider.containsValueKey(key)) {
            return true;
        }

        if (super.containsValueKey(key)) {
            return true;
        }

        return (_overriden != null) && _overriden.containsValueKey(key);
    }

    /** {@inheritDoc}
     */
    @Override
    public ConfigProperties copy()
    {
        return new ConfigProperties(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean equals(final Object other)
    {
        if (super.equals(other)) {
            final List<Map.Entry<String, List<Object>>> groups =
                new ArrayList<>(
                    getGroupsEntries());
            final List<Map.Entry<String, List<Object>>> otherGroups =
                new ArrayList<>(
                    ((ConfigProperties) other).getGroupsEntries());

            groups.sort(MAP_ENTRY_COMPARATOR);
            otherGroups.sort(MAP_ENTRY_COMPARATOR);

            return groups.equals(otherGroups);
        }

        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    @Nonnull
    public ConfigProperties freeze()
    {
        if (_overrider != null) {
            _overrider.freeze();
        }

        super.freeze();

        if (_overriden != null) {
            _overriden.freeze();
        }

        return this;
    }

    /** {@inheritDoc}
     */
    @Override
    public final ConfigProperties getGroup(final String key)
    {
        ConfigProperties group;

        if (_overrider == null) {
            group = getMissingGroup();
        } else {
            group = _overrider.getGroup(key);
        }

        if (group.isMissing()) {
            group = (ConfigProperties) super.getGroup(key);

            if (group.isMissing() && (_overriden != null)) {
                group = _overriden.getGroup(key);
            }
        }

        return group;
    }

    /** {@inheritDoc}
     */
    @Override
    public final KeyedGroups[] getGroups(final String key)
    {
        KeyedGroups[] groups;

        if (_overrider == null) {
            groups = getNoGroups();
        } else {
            groups = _overrider.getGroups(key);
        }

        if (groups.length == 0) {
            groups = super.getGroups(key);

            if ((groups.length == 0) && (_overriden != null)) {
                groups = _overriden.getGroups(key);
            }
        }

        return groups;
    }

    /** {@inheritDoc}
     */
    @Override
    public final List<Object> getObjects(final String key)
    {
        List<Object> objects;

        if (_overrider != null) {
            objects = _overrider.getObjects(key);
        } else {
            objects = Collections.emptyList();
        }

        if (objects.isEmpty()) {
            objects = super.getObjects(key);

            if (objects.isEmpty() && (_overriden != null)) {
                objects = _overriden.getObjects(key);
            }
        }

        return objects;
    }

    /**
     * Gets the overriden properties.
     *
     * @return The optional overriden properties.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<ConfigProperties> getOverriden()
    {
        return Optional.ofNullable(_overriden);
    }

    /**
     * Gets the overrider properties.
     *
     * @return The optional overrider properties.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<ConfigProperties> getOverrider()
    {
        return Optional.ofNullable(_overrider);
    }

    /** {@inheritDoc}
     */
    @Override
    public final int hashCode()
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isEmpty()
    {
        return super.isEmpty()
               && ((_overrider == null) || _overrider.isEmpty())
               && ((_overriden == null) || _overriden.isEmpty());
    }

    /**
     * Removes a property change Listener.
     *
     * @param listener The Listener.
     */
    public final void removePropertyChangeListener(
            @Nonnull final PropertyChangeListener listener)
    {
        final PropertyChangeListener[] listeners = _changeSupport
            .getPropertyChangeListeners();

        for (final PropertyChangeListener changeListener: listeners) {
            final PropertyChangeListenerProxy listenerProxy =
                (PropertyChangeListenerProxy) changeListener;

            if (listenerProxy.getListener() == listener) {
                _changeSupport.removePropertyChangeListener(listenerProxy);
            }
        }
    }

    /**
     * Restores monitored values.
     *
     * <p>This is used by {@link org.rvpf.document.loader.DocumentLoader} to
     * restore dynamic properties at the end of an 'include' PI.</p>
     *
     * @param values The opaque object containing the monitored values.
     */
    public final void restoreMonitoredValues(@Nonnull final Object values)
    {
        if (_changeSupport != null) {
            final _MonitoredValues monitoredValues = (_MonitoredValues) values;

            for (final String key: monitoredValues.getValuesKeys()) {
                setObjects(key, monitoredValues.getObjects(key));
            }
        }
    }

    /**
     * Saves monitored values.
     *
     * <p>This is used by {@link org.rvpf.document.loader.DocumentLoader} to
     * save dynamic properties at the beginning of an 'include' PI.</p>
     *
     * @return An opaque object containing the monitored values.
     */
    @Nonnull
    @CheckReturnValue
    public final Object saveMonitoredValues()
    {
        final _MonitoredValues monitoredValues = new _MonitoredValues(
            getType());

        if (_changeSupport != null) {
            final PropertyChangeListener[] listeners = _changeSupport
                .getPropertyChangeListeners();

            for (final PropertyChangeListener listener: listeners) {
                final PropertyChangeListenerProxy proxy =
                    (PropertyChangeListenerProxy) listener;
                final String key = proxy.getPropertyName();

                monitoredValues.setObjects(key, super.getObjects(key));
            }
        }

        return monitoredValues.freeze();
    }

    /**
     * Sets the overriden properties.
     *
     * @param overriden The overriden properties.
     */
    public void setOverriden(@Nonnull final ConfigProperties overriden)
    {
        _overriden = Require.notNull(overriden);
    }

    /**
     * Sets the overrider properties.
     *
     * @param overrider The overrider properties.
     */
    public void setOverrider(@Nonnull final ConfigProperties overrider)
    {
        _overrider = Require.notNull(overrider);
    }

    /** {@inheritDoc}
     */
    @Override
    public void setValue(final String key, final Object value)
    {
        if (_changeSupport != null) {
            final Object previousValue = getObject(key);

            super.setValue(key, value);
            _changeSupport.firePropertyChange(key, previousValue, value);
        } else {
            super.setValue(key, value);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected final ConfigProperties getMissingGroup()
    {
        return MISSING_CONFIG_PROPERTIES;
    }

    /** {@inheritDoc}
     */
    @Override
    protected final ConfigProperties[] getNoGroups()
    {
        return NO_CONFIG_PROPERTIES;
    }

    /** Missing configuration properties. */
    public static final ConfigProperties MISSING_CONFIG_PROPERTIES;

    /** No configuration properties. */
    public static final ConfigProperties[] NO_CONFIG_PROPERTIES =
        new ConfigProperties[0];

    /**  */

    private static final long serialVersionUID = 1L;

    static {
        MISSING_CONFIG_PROPERTIES = new ConfigProperties()
        {
            /** {@inheritDoc}
             */
            @Override
            public boolean isMissing()
            {
                return true;
            }

            private static final long serialVersionUID = 1L;
        };
        MISSING_CONFIG_PROPERTIES.freeze();
    }

    private transient PropertyChangeSupport _changeSupport;
    private transient ConfigProperties _overriden;
    private transient ConfigProperties _overrider;

    @NotThreadSafe
    private static final class _MonitoredValues
        extends KeyedValues
    {
        /**
         * Constructs an instance.
         *
         * @param superType The super type.
         */
        _MonitoredValues(@Nonnull final String superType)
        {
            super(Message.format(ServiceMessages.MONITORED_TYPE, superType));
        }

        /** {@inheritDoc}
         */
        @Override
        public _MonitoredValues freeze()
        {
            super.freeze();

            return this;
        }

        /** {@inheritDoc}
         */
        @Override
        public void setObjects(final String key, final List<Object> objects)
        {
            super.setObjects(key, objects);
        }

        private static final long serialVersionUID = 1L;
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
