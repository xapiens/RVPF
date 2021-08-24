/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PointValue.java 4097 2019-06-25 15:35:48Z SFB $
 */

package org.rvpf.base.value;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.Content;
import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.Points;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.store.Store;
import org.rvpf.base.store.StoreAccessException;
import org.rvpf.base.tool.Externalizer;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.Mappable;
import org.rvpf.base.xml.streamer.Streamer;

/**
 * Point value.
 *
 * <p>Instances of this class and subclasses represent point values inside and
 * between the services. They hold a reference to a point definition, a time
 * stamp, a state and a value. The point definition may be referenced by its
 * UUID or by a point object holding at least the name of the point. The state
 * and value may be null.</p>
 *
 * <p>The Externalizable interface is implemented for transit thru RMI calls;
 * transit as XML text is supported by the Mappable interface.</p>
 *
 * <p>Convenience methods are supplied for encoding / decoding, normalization
 * denormalization and for store updates.</p>
 */
@NotThreadSafe
public class PointValue
    implements Externalizable, Mappable, Streamer.Validated
{
    /**
     * Constructs an instance.
     *
     * <p>This is needed for an Externalizable implementation. It is also used
     * as a never matching reference.</p>
     */
    public PointValue() {}

    /**
     * Constructs an instance from an other.
     *
     * <p>The copy will be a shallow clone of the original.</p>
     *
     * @param other The other point value.
     */
    public PointValue(@Nonnull final PointValue other)
    {
        _point = other._point;
        _pointUUID = other._pointUUID;
        Require.success((_point != null) || (_pointUUID != null));
        _stamp = other._stamp;
        _state = other._state;
        _value = other._value;
    }

    /**
     * Constructs an instance.
     *
     * @param point The point definition.
     * @param stamp The optional time stamp of the value.
     * @param state The state.
     * @param value The value.
     */
    public PointValue(
            @Nonnull final Point point,
            @Nonnull final Optional<DateTime> stamp,
            @Nullable final Serializable state,
            @Nullable final Serializable value)
    {
        Require.notNull(point);

        _point = point;
        _pointUUID = point.getUUID().get();

        Require
            .success(
                (_pointUUID != null) || (point instanceof Point.Named),
                "Bad point identity");

        _stamp = stamp.orElse(null);
        _state = state;
        _value = value;
    }

    /**
     * Constructs an instance.
     *
     * @param point A string identifying the point.
     * @param stamp The optional time stamp of the value.
     * @param state The state.
     * @param value The value.
     */
    public PointValue(
            @Nonnull String point,
            @Nonnull final Optional<DateTime> stamp,
            @Nullable final Serializable state,
            @Nullable final Serializable value)
    {
        Require.notNull(point);

        point = point.trim();

        if (!point.isEmpty()) {
            if (UUID.isUUID(point)) {
                _pointUUID = UUID.fromString(point).get();
            } else {
                _point = new Point.Named(point);
            }
        }

        _stamp = stamp.orElse(null);
        _state = state;
        _value = value;
    }

    /**
     * Constructs an instance from raw data.
     *
     * @param uuid The UUID of the point definition.
     * @param stamp The optional time stamp of the value.
     * @param state The state.
     * @param value The value.
     */
    public PointValue(
            @Nonnull final UUID uuid,
            @Nonnull final Optional<DateTime> stamp,
            @Nullable final Serializable state,
            @Nullable final Serializable value)
    {
        Require.notNull(uuid);

        _pointUUID = uuid;
        _stamp = stamp.orElse(null);
        _state = state;
        _value = value;
    }

    /**
     * Clears this value's time stamp.
     */
    public final void clearStamp()
    {
        checkNotFrozen();

        _stamp = null;
    }

    /**
     * Confirms this.
     *
     * <p>Note: the point reference must be set and its store must be able to
     * confirm.</p>
     *
     * @param confirmValue When true, {@link #sameValueAs} should be called.
     *
     * @return True when confirmed.
     *
     * @throws InterruptedException When interrupted.
     * @throws StoreAccessException When store access fails.
     */
    @CheckReturnValue
    public final boolean confirm(
            final boolean confirmValue)
        throws InterruptedException, StoreAccessException
    {
        final Point point = getPoint().get();
        final Store store = point.getStore().get();

        return store.confirm(this, confirmValue);
    }

    /**
     * Creates a copy of this point value.
     *
     * @return The copy.
     */
    @Nonnull
    @CheckReturnValue
    public PointValue copy()
    {
        return new PointValue(this);
    }

    /**
     * Copies the value from an other point value.
     *
     * <p>A subclass supporting additional value informations should override
     * this method.</p>
     *
     * <p>Used by the processor's batch.</p>
     *
     * @param pointValue The source point value.
     */
    public void copyValueFrom(@Nonnull final PointValue pointValue)
    {
        setState(pointValue.getState());
        setValue(pointValue.getValue());
    }

    /**
     * Returns this or a decoded clone of this.
     *
     * @return A decoded point value.
     */
    @Nonnull
    @CheckReturnValue
    public final PointValue decoded()
    {
        final Optional<Content> content = getPoint().get().getContent();

        if (!content.isPresent()) {
            return this;
        }

        final Serializable value = content.get().decode(this);
        final PointValue decoded;

        if (value != getValue()) {
            decoded = copy();
            decoded.setValue(value);
        } else {
            decoded = this;
        }

        return decoded;
    }

    /**
     * Denormalizes this.
     *
     * <p>This method is overriden by {@link NormalizedValue}. Other point value
     * classes, including this one, assume that their value is not
     * normalized.</p>
     *
     * @return The denormalized value.
     */
    @Nonnull
    @CheckReturnValue
    public PointValue denormalized()
    {
        return this;
    }

    /**
     * Returns this or an encoded clone of this.
     *
     * @return A encoded point value.
     */
    @Nonnull
    @CheckReturnValue
    public final PointValue encoded()
    {
        final PointValue denormalized = denormalized();
        final Optional<Content> content = getPoint().get().getContent();

        if (!content.isPresent()) {
            return this;
        }

        final Serializable value = content.get().encode(denormalized);
        final PointValue encoded;

        if (value != denormalized.getValue()) {
            encoded = denormalized.copy();
            encoded.setValue(value);
        } else {
            encoded = denormalized;
        }

        return encoded;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object)
    {
        if (this == object) {
            return true;
        }

        if (!(object instanceof PointValue)) {
            return false;
        }

        final PointValue other = (PointValue) object;

        if (_pointUUID != null) {
            if (!_pointUUID.equals(other._pointUUID)) {
                return false;
            }
        } else {
            Require
                .success(
                    (_point != null) && (_point.getName().isPresent()),
                    "Point not identified");

            if (other._point == null) {
                return false;
            }

            if (!_point.getName().equals(other._point.getName())) {
                return false;
            }
        }

        Require.notNull(_stamp, "Point not stamped");

        return _stamp.equals(other._stamp);
    }

    /**
     * Freezes this point value.
     */
    public final void freeze()
    {
        if (!_frozen) {
            Require
                .success((_point != null) || (_pointUUID != null), "No point");
            Require.notNull(_stamp, "No stamp");

            if (_state instanceof Container) {
                ((Container) _state).freeze();
            }

            if (_value instanceof Container) {
                ((Container) _value).freeze();
            }

            _frozen = true;
        }
    }

    /**
     * Returns a frozen instance of this.
     *
     * @return This if frozen, or a frozen clone.
     */
    @Nonnull
    @CheckReturnValue
    public PointValue frozen()
    {
        final PointValue frozen;

        if (isFrozen()) {
            frozen = this;
        } else {
            frozen = copy();
            frozen.freeze();
        }

        return frozen;
    }

    /**
     * Gets the point definition.
     *
     * <p>Needed by a comparator.</p>
     *
     * @return The point definition (null if missing).
     */
    @Nullable
    @CheckReturnValue
    public final Point getNullablePoint()
    {
        return (_pointUUID != null)? _point: null;
    }

    /**
     * Gets the point definition.
     *
     * @return The optional point definition.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<Point> getPoint()
    {
        return (_pointUUID != null)? Optional
            .ofNullable(_point): Optional.empty();
    }

    /**
     * Gets the point's name.
     *
     * @return The optional point's name.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<String> getPointName()
    {
        return (_point != null)? _point.getName(): Optional.empty();
    }

    /**
     * Gets the point's UUID.
     *
     * @return The point's UUID.
     */
    @Nonnull
    @CheckReturnValue
    public final UUID getPointUUID()
    {
        return Require.notNull(_pointUUID);
    }

    /**
     * Gets this value's time stamp.
     *
     * @return The time stamp.
     */
    @Nonnull
    @CheckReturnValue
    public final DateTime getStamp()
    {
        return Require.notNull(_stamp);
    }

    /**
     * Gets this point value's state.
     *
     * @return The state (may have a null value).
     */
    @Nullable
    @CheckReturnValue
    public final Serializable getState()
    {
        return _state;
    }

    /**
     * Gets the actual value.
     *
     * @return The value (may have a null value).
     */
    @Nullable
    @CheckReturnValue
    public final Serializable getValue()
    {
        return _value;
    }

    /**
     * Asks if this has a point UUID.
     *
     * @return True if this has a point UUID.
     */
    @CheckReturnValue
    public boolean hasPointUUID()
    {
        return _pointUUID != null;
    }

    /**
     * Asks if this has a stamp.
     *
     * @return True if this has a stamp.
     */
    @CheckReturnValue
    public boolean hasStamp()
    {
        return _stamp != null;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        int hash = 0;

        if (_pointUUID != null) {
            hash ^= _pointUUID.hashCode();
        } else {
            final String pointName = (_point != null)? _point
                .getName()
                .orElse(null): null;

            hash ^= Require
                .notNull(pointName, "Point not identified")
                .hashCode();
        }

        if (_stamp != null) {
            hash ^= _stamp.hashCode();
        } else if (!(this instanceof Null)) {
            Require.failure("Point not stamped");
        }

        return hash;
    }

    /**
     * Asks if this point value is absent.
     *
     * @return True if absent.
     */
    @CheckReturnValue
    public boolean isAbsent()
    {
        return _stamp == null;
    }

    /**
     * Asks if this point value may be cached.
     *
     * @return True if it may be cached.
     */
    @CheckReturnValue
    public final boolean isCacheable()
    {
        return !(isSynthesized() || _point.isVolatile());
    }

    /**
     * Asks if this point value is (going to be) deleted.
     *
     * <p>A subclass may override this method to supply this functionality.</p>
     *
     * @return True when this is (going to be) deleted.
     *
     * @see org.rvpf.base.value.VersionedValue.Deleted#isDeleted
     */
    @CheckReturnValue
    public boolean isDeleted()
    {
        return false;
    }

    /**
     * Asks if this point value is extrapolated.
     *
     * @return True if this value is extrapolated.
     */
    @CheckReturnValue
    public boolean isExtrapolated()
    {
        return false;
    }

    /**
     * Gets the frozen indicator.
     *
     * @return The frozen indicator.
     */
    @CheckReturnValue
    public final boolean isFrozen()
    {
        return _frozen;
    }

    /**
     * Asks if this point value is interpolated.
     *
     * @return True if this value is interpolated.
     */
    @CheckReturnValue
    public boolean isInterpolated()
    {
        return false;
    }

    /**
     * Asks if this point value is normalized.
     *
     * @return True if this value is normalized.
     */
    @CheckReturnValue
    public boolean isNormalized()
    {
        return false;
    }

    /**
     * Asks if this point value is present.
     *
     * @return True if present.
     */
    @CheckReturnValue
    public final boolean isPresent()
    {
        return !isAbsent();
    }

    /**
     * Asks if this point value is synthesized.
     *
     * @return True if this value is synthesized.
     */
    @CheckReturnValue
    public boolean isSynthesized()
    {
        return false;
    }

    /**
     * Morphs this into a point value for an other point.
     *
     * @param uuid The other point's optional UUID.
     *
     * @return The morphed point value.
     */
    @Nonnull
    @CheckReturnValue
    public PointValue morph(@Nonnull final Optional<UUID> uuid)
    {
        if (Objects.equals(_pointUUID, uuid.orElse(null))) {
            return this;
        }

        final PointValue clone = copy();

        clone._pointUUID = uuid.orElse(null);
        clone._point = null;

        return clone;
    }

    /**
     * Morphs this into a point value for an other point and/or an other time.
     *
     * @param point The other point (may be empty).
     * @param stamp The other time (may be empty).
     *
     * @return The morphed point value.
     */
    @Nonnull
    @CheckReturnValue
    public final PointValue morph(
            @Nonnull final Optional<Point> point,
            @Nonnull final Optional<DateTime> stamp)
    {
        PointValue clone = null;

        if (point.isPresent()) {
            clone = copy();
            clone._point = point.get();
            clone._pointUUID = point.get().getUUID().get();
        }

        if (stamp.isPresent() && !stamp.get().equals(_stamp)) {
            if ((_stamp == null) && !_frozen) {
                _stamp = stamp.get();
            } else {
                if (clone == null) {
                    clone = copy();
                }

                clone._stamp = stamp.get();
            }
        }

        return (clone != null)? clone: this;
    }

    /**
     * Normalizes this point value.
     *
     * <p>The point's content instance is called to perform the actual
     * normalization.</p>
     *
     * @return The normalized value.
     */
    @Nonnull
    @CheckReturnValue
    public NormalizedValue normalized()
    {
        final Point point = getPoint().get();
        final Optional<Content> content = point.getContent();

        if (!content.isPresent()) {
            return new NormalizedValue(this);
        }

        final Serializable value = content.get().decode(this);
        final NormalizedValue normalized = new NormalizedValue(
            point,
            Optional.of(getStamp()),
            getState(),
            value);

        if (value != null) {
            normalized.setValue(content.get().normalize(normalized));
        }

        return normalized;
    }

    /**
     * Returns a string identification of the point.
     *
     * @return The string identification.
     */
    @Nonnull
    @CheckReturnValue
    public final String pointString()
    {
        if (_point != null) {
            return _point.toString();
        } else if (_pointUUID != null) {
            return _pointUUID.toString();
        }

        return "";
    }

    /** {@inheritDoc}
     */
    @Override
    public void readExternal(final ObjectInput input)
        throws IOException
    {
        _pointUUID = UUID.readExternal(input).orElse(null);

        if (_pointUUID == null) {
            final String pointName = Externalizer.readString(input);

            if (pointName != null) {
                _point = new Point.Named(pointName);
            }
        }

        _stamp = DateTime.readExternal(input).orElse(null);

        _state = Externalizer.readSerializable(input);

        _value = Externalizer.readSerializable(input);

        setFrozen();
    }

    /** {@inheritDoc}
     */
    @Override
    public void readMap(final Map<String, Serializable> map)
    {
        final String uuidString = (String) map.get(UUID_FIELD);

        if (uuidString != null) {
            _pointUUID = UUID.fromString(uuidString).get();
        } else {
            final String pointString = (String) map.get(POINT_FIELD);

            if (pointString != null) {
                if (UUID.isUUID(pointString)) {
                    _pointUUID = UUID.fromString(pointString).get();
                } else {
                    _point = new Point.Named(pointString);
                }
            }
        }

        _stamp = DateTime
            .fromString(Optional.ofNullable((String) map.get(STAMP_FIELD)))
            .orElse(null);

        _state = map.get(STATE_FIELD);

        _value = map.get(VALUE_FIELD);

        setFrozen();
    }

    /**
     * Substitutes serialization/deserialization.
     *
     * @return The reset clone.
     */
    @Nonnull
    @CheckReturnValue
    public final PointValue reset()
    {
        final PointValue clone = copy();

        if ((getClass() != Null.class) && !(_point instanceof Point.Named)) {
            if (_pointUUID == null) {
                if (_point != null) {
                    clone._pointUUID = _point.getUUID().get();
                }

                Require
                    .notNull(
                        clone._pointUUID,
                        "Can't reset: point UUID not specified");
            }

            clone._point = null;
        }

        clone.freeze();

        return clone;
    }

    /**
     * Restores the reference to the point definition.
     *
     * <p>This is used in the deserialization process.</p>
     *
     * @param point The point's definition.
     *
     * @return The point value.
     */
    @Nonnull
    @CheckReturnValue
    public final PointValue restore(@Nullable final Point point)
    {
        if ((point == null) || point.isDropped()) {
            return this;
        }

        final Optional<UUID> pointUUID = point.getUUID();
        final DateTime version = getVersion();

        if (_pointUUID == null) {
            if (_point == null) {
                _pointUUID = pointUUID.orElse(null);
                _point = point;

                return this;
            }

            if (point instanceof Point.Named) {
                final Optional<String> pointName = point.getName();

                Require
                    .success(
                        pointName
                            .get()
                            .equalsIgnoreCase(_point.getName().orElse(null)),
                        "'" + pointName + "' != '" + _point.getName() + "'");

                return this;
            }

            Require.present(pointUUID, "Point UUID is missing");

            final PointValue pointValue = copy();

            pointValue._pointUUID = pointUUID.orElse(null);
            pointValue._point = point;
            pointValue.setVersion(version);

            return pointValue;
        }

        Require
            .success(
                !pointUUID.isPresent() || pointUUID.get().equals(_pointUUID),
                "'" + pointUUID + "' != '" + _pointUUID + "'");

        final PointValue pointValue = (_point == null)? this: copy();

        pointValue._point = point;
        pointValue.setVersion(version);

        return pointValue;
    }

    /**
     * Restores the reference to the point definition.
     *
     * @param points The points from which to get the point's definition.
     *
     * @return The point value.
     */
    @Nonnull
    @CheckReturnValue
    public final PointValue restore(@Nonnull final Points points)
    {
        final PointValue pointValue;

        if (_pointUUID == null) {
            pointValue = (_point != null)? restore(
                points
                    .getPointByName(_point.getName().orElse(null))
                    .orElse(null)): this;
        } else {
            pointValue = restore(
                points.getPointByUUID(_pointUUID).orElse(null));
        }

        return pointValue;
    }

    /**
     * Asks if an other point value has the same value.
     *
     * <p>A subclass supporting additional value informations should override
     * this method.</p>
     *
     * @param pointValue The other point value.
     *
     * @return True if the contained values are equal.
     */
    @CheckReturnValue
    public boolean sameValueAs(@Nonnull final PointValue pointValue)
    {
        boolean same;

        if (_value != null) {
            same = _value.equals(pointValue._value);
        } else {
            same = pointValue._value == null;
        }

        if (same) {
            if (_state != null) {
                same = _state.equals(pointValue._state);
            } else {
                same = pointValue._state == null;
            }
        }

        return same;
    }

    /**
     * Sets the point's name.
     *
     * @param name The point's name.
     */
    public final void setPointName(@Nonnull final String name)
    {
        Require.success(_point == null, "Point is already specified");

        _point = new Point.Named(name);
    }

    /**
     * Sets the point's UUID.
     *
     * @param uuid The point's UUID.
     */
    public final void setPointUUID(@Nonnull final UUID uuid)
    {
        checkNotFrozen();

        Require.success(_pointUUID == null, "Point UUID already set");
        Require.success(_point == null, "Point name already set");

        _pointUUID = Require.notNull(uuid);
    }

    /**
     * Sets this value's time stamp.
     *
     * @param stamp The time stamp.
     */
    public final void setStamp(@Nonnull final DateTime stamp)
    {
        checkNotFrozen();

        Require.success(_stamp == null, "Stamp already set");

        _stamp = Require.notNull(stamp);
    }

    /**
     * Sets this value's state.
     *
     * @param state The state.
     */
    public final void setState(@Nullable final Serializable state)
    {
        checkNotFrozen();

        _state = state;
    }

    /**
     * Sets the actual value.
     *
     * @param value The value.
     */
    public final void setValue(@Nullable final Serializable value)
    {
        checkNotFrozen();

        _value = value;
    }

    /**
     * Returns a thawed instance of this.
     *
     * @return A clone if frozen, or this.
     */
    @Nonnull
    @CheckReturnValue
    public PointValue thawed()
    {
        return isFrozen()? copy(): this;
    }

    /**
     * Returns the value as a double floating-point value.
     *
     * @return The value (may be null).
     */
    @Nullable
    @CheckReturnValue
    public Double toDouble()
    {
        if (_value instanceof Double) {
            return (Double) _value;
        }

        if (_value instanceof Number) {
            return Double.valueOf(((Number) _value).doubleValue());
        }

        if (_value instanceof String) {
            try {
                return Double.valueOf((String) _value);
            } catch (final NumberFormatException exception) {
                getThisLogger().warn(BaseMessages.BAD_VALUE_NULLED, _value);

                return null;
            }
        }

        if (_value instanceof Boolean) {
            return Double
                .valueOf(((Boolean) _value).booleanValue()? 1.0d: 0.0d);
        }

        return null;
    }

    /**
     * Returns the value as a long value.
     *
     * @return The value (null on failure).
     */
    @Nullable
    @CheckReturnValue
    public Long toLong()
    {
        if (_value instanceof Long) {
            return (Long) _value;
        }

        if (_value instanceof Number) {
            return Long.valueOf(((Number) _value).longValue());
        }

        if (_value instanceof String) {
            try {
                return Long.decode((String) _value);
            } catch (final NumberFormatException exception) {
                getThisLogger().warn(BaseMessages.BAD_VALUE_NULLED, _value);

                return null;
            }
        }

        if (_value instanceof Boolean) {
            return Long.valueOf(((Boolean) _value).booleanValue()? 1: 0);
        }

        return null;
    }

    /** {@inheritDoc}
     */
    @Override
    public final String toString()
    {
        return toString(DateTime.defaultContext());
    }

    /**
     * Returns a string representation of itself.
     *
     * @param dateTimeContext The date-time context.
     *
     * @return The string representation of itsself.
     */
    @Nonnull
    @CheckReturnValue
    public final String toString(
            @Nonnull final DateTime.Context dateTimeContext)
    {
        final StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("'");
        stringBuilder.append(pointString());
        stringBuilder.append("'");

        stringBuilder.append(' ');
        stringBuilder.append(getStampString(dateTimeContext));

        if (isDeleted()) {
            stringBuilder.append(" (deleted)");
        } else {
            if (isSynthesized()) {
                stringBuilder.append(" (");
                stringBuilder.append(getSynthesizedString());
                stringBuilder.append(')');
            }

            if (_state != null) {
                stringBuilder.append(" [");
                stringBuilder.append(_valueString(_state));
                stringBuilder.append(']');
            }

            stringBuilder.append(' ');
            stringBuilder.append(valueString());
        }

        return stringBuilder.toString();
    }

    /**
     * Updates the store with this.
     *
     * @return True on success.
     *
     * @throws InterruptedException When the service is stopped.
     * @throws StoreAccessException When store access fails.
     */
    @CheckReturnValue
    public final boolean updateStore()
        throws InterruptedException, StoreAccessException
    {
        final Store store = getPoint().get().getStore().get();

        store.addUpdate(this);

        return store.sendUpdates();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean validate()
    {
        boolean valid = true;

        if (_stamp != null) {
            if ((_pointUUID == null)
                    && ((_point == null) || (!_point.getName().isPresent())
                        || _point.getName().get().trim().isEmpty())) {
                getThisLogger().error(BaseMessages.NO_POINT);
                valid = false;
            }
        } else if ((_pointUUID != null)
                   || (_point != null)
                   || (_state != null)
                   || (_value != null)) {
            getThisLogger().error(BaseMessages.NO_STAMP);
            valid = false;
        }

        return valid;
    }

    /**
     * Returns a simple representation of the value.
     *
     * @return A simple representation of the value.
     */
    @Nonnull
    @CheckReturnValue
    public String valueString()
    {
        return _valueString(_value);
    }

    /** {@inheritDoc}
     */
    @Override
    public void writeExternal(final ObjectOutput output)
        throws IOException
    {
        freeze();

        UUID.writeExternal(Optional.ofNullable(_pointUUID), output);

        if (_pointUUID == null) {
            Externalizer
                .writeString(
                    (_point != null)? _point.getName().get(): null,
                    output);
        }

        DateTime.writeExternal(Optional.ofNullable(_stamp), output);

        Externalizer.writeSerializable(_state, output);

        Externalizer.writeSerializable(_value, output);
    }

    /** {@inheritDoc}
     */
    @Override
    public void writeMap(final Map<String, Serializable> map)
    {
        map.put(SIMPLE_STRING_MODE, null);

        final UUID uuid = (_pointUUID != null)
            ? _pointUUID: ((_point != null)? _point
                .getUUID()
                .orElse(null): null);
        final String point = (_point != null)? _point.getName().get(): null;

        if (point != null) {
            map.put(POINT_FIELD, point);
        } else if (uuid != null) {
            map.put(POINT_FIELD, uuid.toString());
        }

        if ((point != null) && (uuid != null)) {
            map.put(UUID_FIELD, uuid.toString());
        }

        if (_stamp != null) {
            map.put(STAMP_FIELD, _stamp.toString());
        }

        map.put(SERIALIZABLE_MODE, null);
        map.put(STATE_FIELD, _state);
        map.put(VALUE_FIELD, _value);

        freeze();
    }

    /**
     * Checks that the point value is not frozen.
     */
    protected final void checkNotFrozen()
    {
        Require.success(!_frozen, BaseMessages.FROZEN);
    }

    /**
     * Gets the time stamp string.
     *
     * @param dateTimeContext The date-time context.
     *
     * @return The time stamp string.
     */
    @Nonnull
    @CheckReturnValue
    protected String getStampString(
            @Nonnull final DateTime.Context dateTimeContext)
    {
        return (_stamp != null)? dateTimeContext.toString(_stamp): "null";
    }

    /**
     * Gets the 'synthesized' string.
     *
     * @return The 'synthesized' string.
     */
    @Nonnull
    @CheckReturnValue
    protected String getSynthesizedString()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the logger for this instance.
     *
     * @return The logger for this instance.
     */
    @Nonnull
    @CheckReturnValue
    protected final Logger getThisLogger()
    {
        return Logger.getInstance(getClass());
    }

    /**
     * Gets this value's version.
     *
     * @return The version (null when not available).
     */
    @CheckReturnValue
    protected DateTime getVersion()
    {
        return null;
    }

    /**
     * Sets the frozen indicator.
     */
    protected final void setFrozen()
    {
        _frozen = true;
    }

    /**
     * Sets this value's version.
     *
     * @param version The version.
     */
    protected void setVersion(final DateTime version) {}

    private static String _valueString(final Serializable value)
    {
        final String valueString;

        if (value == null) {
            valueString = "null";
        } else {
            if (value instanceof State) {
                valueString = value.toString();
            } else if ((value instanceof Number)
                       || (value instanceof String)
                       || (value instanceof Boolean)) {
                valueString = "\"" + value.toString() + "\"";
            } else if (value instanceof Tuple) {
                final StringBuilder stringBuilder = new StringBuilder();
                boolean first = true;

                stringBuilder.append('(');

                for (final Serializable childValue: (Tuple) value) {
                    if (!first) {
                        stringBuilder.append(',');
                    }

                    stringBuilder.append(_valueString(childValue));
                    first = false;
                }

                stringBuilder.append(')');
                valueString = stringBuilder.toString();
            } else {
                valueString = value.getClass().getName();
            }
        }

        return valueString;
    }

    /** The null point value. */
    public static final PointValue NULL = new Null();

    /** Point field key. */
    public static final String POINT_FIELD = "point";

    /** Stamp field key. */
    public static final String STAMP_FIELD = "stamp";

    /** State field key. */
    public static final String STATE_FIELD = "state";

    /** UUID field key. */
    public static final String UUID_FIELD = "uuid";

    /** Value field key. */
    public static final String VALUE_FIELD = "value";

    /**  */

    private static final long serialVersionUID = 1L;

    private transient boolean _frozen;
    private transient Point _point;
    private UUID _pointUUID;
    private DateTime _stamp;
    private Serializable _state;
    private Serializable _value;

    /**
     * Null.
     */
    public static class Null
        extends PointValue
    {
        /**
         * Constructs an instance.
         */
        public Null()
        {
            setFrozen();
        }

        /**
         * Constructs an instance.
         *
         * @param pointValue The source point value.
         */
        public Null(@Nonnull final PointValue pointValue)
        {
            super(pointValue);

            setFrozen();
        }

        /** {@inheritDoc}
         */
        @Override
        public Null copy()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean equals(final Object object)
        {
            if (this == object) {
                return true;
            }

            if ((object == null) || (getClass() != object.getClass())) {
                return false;
            }

            return Objects
                .equals(getPointUUID(), ((Null) object).getPointUUID());
        }

        /** {@inheritDoc}
         */
        @Override
        public int hashCode()
        {
            return Objects.hashCode(getPointUUID());
        }

        /** {@inheritDoc}
         */
        @Override
        public NormalizedValue normalized()
        {
            return new NormalizedValue.Null(this);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean sameValueAs(final PointValue pointValue)
        {
            return false;
        }

        private Object readResolve()
        {
            return hasPointUUID()? this: NULL;
        }
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
