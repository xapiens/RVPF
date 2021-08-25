/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: GroupCategory.java 4085 2019-06-16 15:17:12Z SFB $
 */

package org.rvpf.pap.dnp3.object;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.pap.dnp3.object.groupCategory.analogInputs.AnalogInputsGroup;
import org.rvpf.pap.dnp3.object.groupCategory.analogOutputs.AnalogOutputsGroup;
import org.rvpf.pap.dnp3.object.groupCategory.binaryInputs.BinaryInputsGroup;
import org.rvpf.pap.dnp3.object.groupCategory.binaryOutputs.BinaryOutputsGroup;
import org.rvpf.pap.dnp3.object.groupCategory.classes.ClassesGroup;
import org.rvpf.pap.dnp3.object.groupCategory.counters.CountersGroup;
import org.rvpf.pap.dnp3.object.groupCategory.devices.DevicesGroup;
import org.rvpf.pap.dnp3.object.groupCategory.times.TimesGroup;

/**
 * Group category enumeration.
 */
public enum GroupCategory
{
    DEVICE_ATTRIBUTES(0, 0, "Device Attributes", Optional.empty(), Optional
        .empty()),
    BINARY_INPUTS(1, 9, "Binary Inputs", Optional
        .of(BinaryInputsGroup.class), Optional
            .of(BinaryInputsGroup.BINARY_INPUT)),
    BINARY_OUTPUTS(10, 19, "Binary Outputs", Optional
        .of(BinaryOutputsGroup.class), Optional
            .of(BinaryOutputsGroup.BINARY_OUTPUT)),
    COUNTERS(20, 29, "Counters", Optional
        .of(CountersGroup.class), Optional.of(CountersGroup.COUNTER)),
    ANALOG_INPUTS(30, 39, "Analog Inputs", Optional
        .of(AnalogInputsGroup.class), Optional
            .of(AnalogInputsGroup.ANALOG_INPUT)),
    ANALOG_OUTPUTS(40, 49, "Analog Outputs", Optional
        .of(AnalogOutputsGroup.class), Optional
            .of(AnalogOutputsGroup.ANALOG_OUTPUT_COMMAND)),
    TIMES(50, 59, "Times", Optional
        .of(TimesGroup.class), Optional.of(TimesGroup.TIME_DATE)),
    CLASSES(60, 69, "Classes", Optional
        .of(ClassesGroup.class), Optional.of(ClassesGroup.CLASS_OBJECTS)),
    FILES(70, 79, "Files", Optional.empty(), Optional.empty()),
    DEVICES(80, 82, "Devices", Optional
        .of(DevicesGroup.class), Optional
            .of(DevicesGroup.INTERNAL_INDICATIONS)),
    DATA_SETS(83, 89, "Data Sets", Optional.empty(), Optional.empty()),
    APPLICATIONS(90, 99, "Applications", Optional.empty(), Optional.empty()),
    ALTERNATE_NUMERICS(100, 109, "Alternate Numerics", Optional.empty(),
            Optional
                .empty()),
    OTHER(110, 119, "Other", Optional.empty(), Optional.empty()),
    SECURITY(120, 129, "Security", Optional.empty(), Optional.empty());

    /**
     * Constructs an instance.
     *
     * @param fromGroupCode From group code.
     * @param toGroupCode To group code.
     * @param title The point type title.
     * @param objectGroupClass The optional object group class.
     * @param baseGroup The optional object base group.
     */
    GroupCategory(
            final int fromGroupCode,
            final int toGroupCode,
            @Nonnull final String title,
            @Nonnull final Optional<Class<? extends ObjectGroup>> objectGroupClass,
            @Nonnull final Optional<ObjectGroup> baseGroup)
    {
        _fromGroupCode = fromGroupCode;
        _toGroupCode = toGroupCode;
        _title = title;
        _objectGroupClass = objectGroupClass;
        _baseGroup = baseGroup;
    }

    /**
     * Returns the instance for a group code.
     *
     * @param groupCode The group code.
     *
     * @return The optional instance (empty if unknown).
     */
    @Nonnull
    @CheckReturnValue
    public static Optional<GroupCategory> instance(final int groupCode)
    {
        return Optional
            .ofNullable(_POINT_TYPE_MAP.get(Integer.valueOf(groupCode)));
    }

    /**
     * Returns a new object instance for an object variation.
     *
     * @param objectVariation The object variation.
     *
     * @return The new object instance.
     */
    @Nonnull
    @CheckReturnValue
    public static ObjectInstance newObjectInstance(
            @Nonnull final ObjectVariation objectVariation)
    {
        final ObjectInstance objectInstance;

        try {
            objectInstance = objectVariation.getObjectClass().newInstance();
        } catch (final ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }

        return objectInstance;
    }

    /**
     * Returns the object group for a group code.
     *
     * @param groupCode The group code.
     *
     * @return The optional object group (empty if unknown).
     */
    @Nonnull
    @CheckReturnValue
    public static Optional<ObjectGroup> objectGroup(final int groupCode)
    {
        final Optional<GroupCategory> groupCategory = instance(groupCode);

        if (!groupCategory.isPresent()) {
            return Optional.empty();
        }

        final Optional<Class<? extends ObjectGroup>> objectGroupClass =
            groupCategory
                .get()
                .getObjectGroupClass();

        if (!objectGroupClass.isPresent()) {
            return Optional.empty();
        }

        final ObjectGroup objectGroup;

        try {
            final Method instanceMethod = objectGroupClass
                .get()
                .getMethod(_INSTANCE_METHOD_NAME, int.class);

            objectGroup = (ObjectGroup) instanceMethod
                .invoke(null, Integer.valueOf(groupCode));
        } catch (final InvocationTargetException exception) {
            return Optional.empty();
        } catch (final ReflectiveOperationException exception) {
            throw new InternalError(exception);
        }

        return Optional.of(objectGroup);
    }

    /**
     * Returns the object variation for an object group and a variation code.
     *
     * @param objectGroup The object group.
     * @param variationCode The variation code.
     *
     * @return The object variation (empty if unknown).
     */
    @Nonnull
    @CheckReturnValue
    public static Optional<ObjectVariation> objectVariation(
            @Nonnull final ObjectGroup objectGroup,
            final int variationCode)
    {
        final ObjectVariation objectVariation;

        try {
            final Method instanceMethod = objectGroup
                .getObjectVariationClass()
                .getMethod(_INSTANCE_METHOD_NAME, int.class);

            objectVariation = (ObjectVariation) instanceMethod
                .invoke(null, Integer.valueOf(variationCode));
        } catch (final ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }

        return Optional.ofNullable(objectVariation);
    }

    /**
     * Returns the object variation for an object group and a variation key.
     *
     * @param objectGroup The object group.
     * @param variationKey The variation key.
     *
     * @return The object variation (empty if unknown).
     */
    @Nonnull
    @CheckReturnValue
    public static Optional<ObjectVariation> objectVariation(
            @Nonnull final ObjectGroup objectGroup,
            @Nonnull final String variationKey)
    {
        final ObjectVariation objectVariation;

        try {
            final Method instanceMethod = objectGroup
                .getObjectVariationClass()
                .getMethod(_VALUE_OF_METHOD_NAME, String.class);

            objectVariation = (ObjectVariation) instanceMethod
                .invoke(null, variationKey);
        } catch (final InvocationTargetException exception) {
            return Optional.empty();
        } catch (final ReflectiveOperationException exception) {
            throw new InternalError(exception);
        }

        return Optional.ofNullable(objectVariation);
    }

    /**
     * Returns the object variations for an object group.
     *
     * @param objectGroup The object group.
     *
     * @return The object variations.
     */
    @Nonnull
    @CheckReturnValue
    public static ObjectVariation[] objectVariations(
            @Nonnull final Optional<ObjectGroup> objectGroup)
    {
        if (!objectGroup.isPresent()) {
            return _NO_VARIATIONS;
        }

        final ObjectVariation[] objectVariations;

        try {
            final Method valuesMethod = objectGroup
                .get()
                .getObjectVariationClass()
                .getMethod(_VALUES_METHOD_NAME);

            objectVariations = (ObjectVariation[]) valuesMethod.invoke(null);
        } catch (final ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }

        return objectVariations;
    }

    /**
     * Gets the baseGroup.
     *
     * @return The optional base group (empty if unspecified).
     */
    @Nonnull
    @CheckReturnValue
    public Optional<ObjectGroup> getBaseGroup()
    {
        return _baseGroup;
    }

    /**
     * Gets the from group code.
     *
     * @return The from group code.
     */
    @CheckReturnValue
    public int getFromGroupCode()
    {
        return _fromGroupCode;
    }

    /**
     * Gets the object group with the specified key.
     *
     * @param groupKey The group key.
     *
     * @return The object group (null if unknown).
     */
    @Nonnull
    @CheckReturnValue
    public Optional<ObjectGroup> getObjectGroup(@Nonnull final String groupKey)
    {
        if (!_objectGroupClass.isPresent()) {
            return Optional.empty();
        }

        final ObjectGroup objectGroup;
        final Class<? extends ObjectGroup> objectGroupClass = _objectGroupClass
            .get();

        try {
            final Method valuesMethod = objectGroupClass
                .getMethod(_VALUE_OF_METHOD_NAME, String.class);

            objectGroup = (ObjectGroup) valuesMethod.invoke(null, groupKey);
        } catch (final InvocationTargetException exception) {
            return Optional.empty();
        } catch (final ReflectiveOperationException exception) {
            throw new InternalError(exception);
        }

        return Optional.of(objectGroup);
    }

    /**
     * Gets the object group class.
     *
     * @return The optional object group class.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Class<? extends ObjectGroup>> getObjectGroupClass()
    {
        return _objectGroupClass;
    }

    /**
     * Gets the object groups for this category.
     *
     * @return The object groups.
     */
    @Nonnull
    @CheckReturnValue
    public ObjectGroup[] getObjectGroups()
    {
        if (!_objectGroupClass.isPresent()) {
            return _NO_GROUPS;
        }

        final ObjectGroup[] objectGroups;
        final Class<? extends ObjectGroup> objectGroupClass = _objectGroupClass
            .get();

        try {
            final Method valuesMethod = objectGroupClass
                .getMethod(_VALUES_METHOD_NAME);

            objectGroups = (ObjectGroup[]) valuesMethod.invoke(null);
        } catch (final ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }

        return objectGroups;
    }

    /**
     * Gets the group category title.
     *
     * @return The group category title.
     */
    @Nonnull
    @CheckReturnValue
    public String getTitle()
    {
        return _title;
    }

    /**
     * Gets the to group code.
     *
     * @return The to group code.
     */
    @CheckReturnValue
    public int getToGroupCode()
    {
        return _toGroupCode;
    }

    private static final String _INSTANCE_METHOD_NAME = "instance";
    private static final Map<Integer, GroupCategory> _POINT_TYPE_MAP =
        new HashMap<>();
    private static final ObjectVariation[] _NO_VARIATIONS =
        new ObjectVariation[0];
    private static final ObjectGroup[] _NO_GROUPS = new ObjectGroup[0];
    private static final String _VALUES_METHOD_NAME = "values";
    private static final String _VALUE_OF_METHOD_NAME = "valueOf";

    static {
        for (final GroupCategory value: values()) {
            for (int i = value.getFromGroupCode(); i <= value.getToGroupCode();
                    ++i) {
                _POINT_TYPE_MAP.put(Integer.valueOf(i), value);
            }
        }
    }

    private final Optional<ObjectGroup> _baseGroup;
    private final int _fromGroupCode;
    private final Optional<Class<? extends ObjectGroup>> _objectGroupClass;
    private final String _title;
    private final int _toGroupCode;
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
