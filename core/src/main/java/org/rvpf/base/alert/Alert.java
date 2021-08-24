/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Alert.java 3981 2019-05-13 15:00:56Z SFB $
 */

package org.rvpf.base.alert;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.DateTime;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.tool.Externalizer;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.Mappable;

/**
 * Alert.
 *
 * <p>Maintains a set of the forwarders visited for loop detection.</p>
 */
@ThreadSafe
public abstract class Alert
    implements Externalizable, Mappable
{
    /**
     * Constructs an instance.
     *
     * <p>This is needed for an externalizable implementation.</p>
     */
    protected Alert() {}

    /**
     * Constructs an instance.
     *
     * @param name The name of this alert.
     * @param sourceServiceName The optional service generating this alert.
     * @param sourceEntityName The optional name of the entity associated with the
     *                         source.
     * @param sourceUUID The optional UUID of the source of this alert.
     * @param info Additional optional informations.
     */
    protected Alert(
            @Nonnull final String name,
            @Nonnull final Optional<String> sourceServiceName,
            @Nonnull final Optional<String> sourceEntityName,
            @Nonnull final Optional<UUID> sourceUUID,
            @Nonnull final Optional<? extends Object> info)
    {
        _uuid = UUID.generate();
        _name = Require.notNull(name);
        _sourceServiceName = sourceServiceName;
        _sourceEntityName = sourceEntityName;
        _sourceUUID = sourceUUID;
        _info = info
            .isPresent()? Optional
                .of(info.get().toString().trim()): Optional.empty();
        _stamp = Optional.of(DateTime.now());
    }

    /**
     * Adds a forwarder as visited by this alert.
     *
     * @param uuid The UUID of the forwarder.
     *
     * @return True if the forwarder is a new visit.
     */
    public final boolean addVisit(@Nonnull final UUID uuid)
    {
        if (_visits == null) {
            _visits = new LinkedHashSet<UUID>();
        }

        return _visits.add(Require.notNull(uuid));
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (this == other) {
            return true;
        }

        if ((other == null) || (getClass() != other.getClass())) {
            return false;
        }

        final Alert otherAlert = (Alert) other;

        return Objects.equals(_name, otherAlert._name)
               && Objects.equals(
                   _sourceServiceName,
                   otherAlert._sourceServiceName)
               && Objects.equals(
                   _sourceEntityName,
                   otherAlert._sourceEntityName)
               && Objects.equals(_sourceUUID, otherAlert._sourceUUID)
               && Objects.equals(_info, otherAlert._info)
               && Objects.equals(_stamp, otherAlert._stamp)
               && Objects.equals(_visits, otherAlert._visits);
    }

    /**
     * Gets the additonal informations.
     *
     * @return The additonal informations.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<String> getInfo()
    {
        return _info;
    }

    /**
     * Gets this alert's name.
     *
     * @return The alert's name.
     */
    @Nonnull
    @CheckReturnValue
    public final String getName()
    {
        return _name;
    }

    /**
     * Gets the name of the source entity generating this alert.
     *
     * @return The optional name.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<String> getSourceEntityName()
    {
        return _sourceEntityName;
    }

    /**
     * Gets the service generating this alert.
     *
     * @return The optional service.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<String> getSourceServiceName()
    {
        return _sourceServiceName;
    }

    /**
     * Gets the source's UUID.
     *
     * @return The optional source's UUID.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<UUID> getSourceUUID()
    {
        return _sourceUUID;
    }

    /**
     * Gets this alert's time stamp.
     *
     * @return The alert's time stamp.
     */
    @Nonnull
    @CheckReturnValue
    public final DateTime getStamp()
    {
        return _stamp.get();
    }

    /**
     * Gets this alert's UUID.
     *
     * @return The alert's UUID.
     */
    @Nonnull
    @CheckReturnValue
    public final UUID getUUID()
    {
        return _uuid;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public void readExternal(final ObjectInput input)
        throws IOException
    {
        _uuid = UUID.readExternal(input).get();
        _name = Externalizer.readString(input);
        _sourceServiceName = Optional
            .ofNullable(Externalizer.readString(input));
        _sourceEntityName = Optional.ofNullable(Externalizer.readString(input));
        _sourceUUID = UUID.readExternal(input);
        _info = Optional.ofNullable(Externalizer.readString(input));
        _stamp = DateTime.readExternal(input);

        int visits = input.readInt();

        while (--visits >= 0) {
            addVisit(UUID.readExternal(input).get());
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void readMap(final Map<String, Serializable> map)
    {
        _uuid = UUID.fromString((String) map.get(UUID_FIELD)).get();
        _name = (String) map.get(NAME_FIELD);
        _stamp = DateTime
            .fromString(Optional.ofNullable((String) map.get(STAMP_FIELD)));
        _sourceServiceName = Optional
            .ofNullable((String) map.get(SERVICE_FIELD));
        _sourceEntityName = Optional.ofNullable((String) map.get(ENTITY_FIELD));

        final String sourceUUID = (String) map.get(SOURCE_FIELD);

        _sourceUUID = (sourceUUID != null)? UUID
            .fromString(sourceUUID): Optional.empty();

        _info = Optional.ofNullable((String) map.get(INFO_FIELD));

        final Collection<?> visits = (Collection<?>) map.get(VISITS_FIELD);

        if (visits != null) {
            for (final Object visit: visits) {
                addVisit((UUID) visit);
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final String toString()
    {
        final String entity = (_sourceEntityName
            .isPresent())? ("'" + _sourceEntityName.get() + "' "): "";
        final String service = (_sourceServiceName
            .isPresent())? ("[" + _sourceServiceName.get() + "] "): "";
        final String info;

        if (!_info.isPresent()) {
            info = "";
        } else if (_info.get().startsWith("(") && _info.get().endsWith(")")) {
            info = " " + _info.get();
        } else {
            info = " \"" + _info.get() + "\"";
        }

        return Message
            .format(
                BaseMessages.ALERT,
                getTypeString(),
                getName(),
                info,
                entity,
                service,
                _sourceUUID.orElse(null));
    }

    /** {@inheritDoc}
     */
    @Override
    public void writeExternal(final ObjectOutput output)
        throws IOException
    {
        UUID.writeExternal(Optional.of(_uuid), output);
        Externalizer.writeString(_name, output);
        Externalizer.writeString(_sourceServiceName.orElse(null), output);
        Externalizer.writeString(_sourceEntityName.orElse(null), output);
        UUID.writeExternal(_sourceUUID, output);
        Externalizer.writeString(_info.orElse(null), output);
        DateTime.writeExternal(_stamp, output);

        final int visits = (_visits != null)? _visits.size(): 0;

        output.writeInt(visits);

        if (visits > 0) {
            for (final UUID visit: _visits) {
                UUID.writeExternal(Optional.of(visit), output);
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void writeMap(final Map<String, Serializable> map)
    {
        map.put(SIMPLE_STRING_MODE, null);
        map.put(UUID_FIELD, _uuid.toString());
        map.put(NAME_FIELD, _name);
        map.put(STAMP_FIELD, _stamp.get().toString());
        map.put(INFO_FIELD, _info.orElse(null));
        map.put(SERVICE_FIELD, _sourceServiceName.orElse(null));
        map.put(ENTITY_FIELD, _sourceEntityName.orElse(null));

        if (_sourceUUID.isPresent()) {
            map.put(SOURCE_FIELD, _sourceUUID.get().toString());
        }

        map.put(SERIALIZABLE_MODE, null);
        map.put(VISITS_FIELD, _visits);
    }

    /**
     * Gets a text string for this alert's type.
     *
     * @return The text string for this alert's type.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract String getTypeString();

    /** Entity field key. */
    public static final String ENTITY_FIELD = "entity";

    /** Info field key. */
    public static final String INFO_FIELD = "info";

    /** Name field key. */
    public static final String NAME_FIELD = "name";

    /** Service field key. */
    public static final String SERVICE_FIELD = "service";

    /** Source field key. */
    public static final String SOURCE_FIELD = "source";

    /** Stamp field key. */
    public static final String STAMP_FIELD = "stamp";

    /** UUID field key. */
    public static final String UUID_FIELD = "uuid";

    /** Visits field key. */
    public static final String VISITS_FIELD = "visits";

    /**  */

    private static final long serialVersionUID = 1L;

    /**
     * Additional optional informations.
     *
     * @serial
     */
    private Optional<String> _info;

    /**
     * The name of this alert.
     *
     * @serial
     */
    private String _name;

    /**
     * The optional name of the entity associated with the source.
     *
     * @serial
     */
    private Optional<String> _sourceEntityName;

    /**
     * The optional name of the service generating this alert.
     *
     * @serial
     */
    private Optional<String> _sourceServiceName;

    /**
     * The optional UUID of the source of this alert.
     *
     * @serial
     */
    private Optional<UUID> _sourceUUID;

    /**
     * The optional time stamp of this alert.
     *
     * @serial
     */
    private Optional<DateTime> _stamp;

    /**
     * The UUID of this alert.
     *
     * @serial
     */
    private UUID _uuid;

    /**
     * The UUIDs of forwarders visited by this alert.
     *
     * @serial
     */
    private LinkedHashSet<UUID> _visits;

    /**
     * Alert dispatcher.
     */
    public interface Dispatcher
    {
        /**
         * Dispatches an alert.
         *
         * @param alertLevel The alert level.
         * @param alertName The alert name.
         * @param alertInfo The alert info.
         */
        void dispatchAlert(
                @Nonnull Logger.LogLevel alertLevel,
                @Nonnull String alertName,
                @Nonnull String alertInfo);
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
