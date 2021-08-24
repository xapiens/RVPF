/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ClassDefEntity.java 4096 2019-06-24 23:07:39Z SFB $
 */

package org.rvpf.config.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.ClassDef;
import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.Entity;
import org.rvpf.base.UUID;
import org.rvpf.base.tool.Require;
import org.rvpf.config.TopologicalErrorException;
import org.rvpf.config.UndefinedEntityException;
import org.rvpf.service.ServiceClassLoader;
import org.rvpf.service.ServiceMessages;

/**
 * Provides a class definition entity.
 *
 * <p>Extends its reponsibilities as a class definition by implementing runtime
 * handling of class libraries and implementation chains.</p>
 *
 * <p>To protect against a loop in the implementation chain and to avoid forward
 * references in exported metadata, each instance is assigned a 'level' (see
 * {@link #adjustLevel()}).</p>
 */
public class ClassDefEntity
    extends AbstractEntity
    implements ClassDef
{
    /**
     * Constructs an instance.
     *
     * @param name The instance name.
     * @param impl The implementation.
     * @param implemented The implemented class definition entities.
     * @param classLib The optional class library.
     * @param initialized The optional initialized indicator.
     * @param level The level.
     */
    ClassDefEntity(
            @Nonnull final String name,
            @Nonnull final ClassDefImpl impl,
            @Nonnull final List<ClassDefEntity> implemented,
            @Nonnull final Optional<ClassLibEntity> classLib,
            @Nonnull final Optional<Boolean> initialized,
            final int level)
    {
        super(Optional.of(name), Optional.empty());

        _impl = Require.notNull(impl);
        _implemented.addAll(implemented);
        _classLib = classLib;
        _initialized = initialized;
        _level = level;
    }

    /**
     * Returns a new builder.
     *
     * @return The new builder.
     */
    @Nonnull
    @CheckReturnValue
    public static Builder newBuilder()
    {
        return new Builder();
    }

    /**
     * Adds an other class definition entity as an implemented interface.
     *
     * @param classDef The class definition entity to add.
     */
    public final void addImplemented(@Nonnull final ClassDefEntity classDef)
    {
        _implemented.add(classDef);
    }

    /**
     * Adjust the level of this instance's dependencies.
     *
     * <p>This implementation uses a reverse level order: an instance without
     * any implementors is at level 0; when an instance is implemented, its
     * level is made negative relative to the implementor. A strictly positive
     * level value indicates that the level has not been adjusted yet.</p>
     *
     * @throws TopologicalErrorException When a recursive reference is found.
     */
    public final void adjustLevel()
        throws TopologicalErrorException
    {
        _adjustLevel(0);
    }

    /** {@inheritDoc}
     */
    @Override
    public final int compareTo(final Entity other)
    {
        final ClassDefEntity otherClassDef = (ClassDefEntity) other;
        int comparison;

        comparison = getLevel() - otherClassDef.getLevel();

        if (comparison == 0) {
            comparison = getClassName().compareTo(otherClassDef.getClassName());
        }

        return comparison;
    }

    /** {@inheritDoc}
     */
    @Override
    public ClassDefEntity copy()
    {
        return newBuilder().copyFrom(this).build();
    }

    /** {@inheritDoc}
     */
    @Override
    public <T> T createInstance(final Class<T> expectedClass)
    {
        if (!_updateClassLoader()) {
            return null;
        }

        return _impl.createInstance(expectedClass);
    }

    /** {@inheritDoc}
     *
     * <p>For two class definition eneity to be equal, they must reference the
     * same class library, have the same class name and implement the same class
     * definitions.</p>
     */
    @Override
    public final boolean equals(final Object other)
    {
        if (this == other) {
            return true;
        }

        if (super.equals(other)) {
            final ClassDefEntity otherEntity = (ClassDefEntity) other;

            if (getClassName().equals(otherEntity.getClassName())) {
                if (Objects.equals(_classLib, otherEntity._classLib)) {
                    final List<ClassDefEntity> implemented = new ArrayList<>(
                        getImplemented());
                    final List<ClassDefEntity> otherImplemented =
                        new ArrayList<>(
                            otherEntity.getImplemented());

                    implemented.sort(null);
                    otherImplemented.sort(null);

                    return implemented.equals(otherImplemented);
                }
            }
        }

        return false;
    }

    /**
     * Gets the class library.
     *
     * @return The optional class library.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<ClassLibEntity> getClassLib()
    {
        return _classLib;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getClassName()
    {
        return _impl.getClassName();
    }

    /** {@inheritDoc}
     */
    @Override
    public final String getElementName()
    {
        return ELEMENT_NAME;
    }

    /**
     * Gets the List of implemented class definitions.
     *
     * @return The List of implemented class definitions.
     */
    @Nonnull
    @CheckReturnValue
    public final List<ClassDefEntity> getImplemented()
    {
        return _implemented;
    }

    /** {@inheritDoc}
     *
     * <p>Before triggering the class loading, makes sure that the class library
     * is known to the context class loader.</p>
     */
    @Override
    public Class<?> getInstanceClass()
    {
        if (!_updateClassLoader()) {
            return null;
        }

        return _impl.getInstanceClass();
    }

    /** {@inheritDoc}
     */
    @Override
    public final Class<?> getInstanceClass(
            final Optional<ClassLoader> classLoader)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets this instance's level.
     *
     * <p>The level helps control the declaration order to avoid forward
     * references.</p>
     *
     * @return An int where 0 means that there is no 'implements' reference to
     *         this instance. Lower (negative) values represent a higher stack
     *         of references.
     */
    @CheckReturnValue
    public final int getLevel()
    {
        return _level;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getMember()
    {
        return _impl.getMember();
    }

    /** {@inheritDoc}
     */
    @Override
    public String getPackageName()
    {
        return _impl.getPackageName();
    }

    /** {@inheritDoc}
     */
    @Override
    public final String getPrefix()
    {
        return ENTITY_PREFIX;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getReferenceName()
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public final int hashCode()
    {
        return super.hashCode();
    }

    /**
     * Asks if this instance implements an interface.
     *
     * @param classObject A class object.
     *
     * @return True if the interface is implemented.
     *
     * @throws UndefinedEntityException When the interface is undefined.
     */
    @CheckReturnValue
    public final boolean is(
            @Nonnull final Class<?> classObject)
        throws UndefinedEntityException
    {
        return is(classObject.getName());
    }

    /**
     * Asks if this instance implements an interface.
     *
     * @param className The name of the class or interface.
     *
     * @return True if the interface is implemented.
     *
     * @throws UndefinedEntityException When the interface is undefined.
     */
    @CheckReturnValue
    public final boolean is(
            @Nonnull final String className)
        throws UndefinedEntityException
    {
        if (className.equals(getClassName())) {
            return true;
        }

        for (final ClassDefEntity classDef: _implemented) {
            if (classDef.is(className)) {
                if (!classDef.isDefined()) {
                    throw new UndefinedEntityException(classDef);
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Asks if this instance is defined.
     *
     * @return True if defined.
     */
    @CheckReturnValue
    public boolean isDefined()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isLoaded()
    {
        if (!_updateClassLoader()) {
            return false;
        }

        return _impl.isLoaded();
    }

    /**
     * Returns a string representation of this.
     *
     * @return The name of its referenced Class.
     */
    @Override
    public final String toString()
    {
        return getClassName();
    }

    /**
     * Gets the impl.
     *
     * @return The impl.
     */
    @Nonnull
    @CheckReturnValue
    ClassDefImpl getImpl()
    {
        return _impl;
    }

    /**
     * Gets the initialized indicator.
     *
     * @return The initialized indicator.
     */
    @Nonnull
    @CheckReturnValue
    Boolean getInitialized()
    {
        return _initialized.get();
    }

    private void _adjustLevel(final int level)
        throws TopologicalErrorException
    {
        if (_busy) {
            throw new TopologicalErrorException(this);
        }

        if (level < _level) {
            _level = level;
            _busy = true;

            for (final ClassDefEntity classDef: _implemented) {
                classDef._adjustLevel(_level - 1);
            }

            _busy = false;
        }
    }

    private synchronized boolean _updateClassLoader()
    {
        if (_initialized.isPresent()) {
            return _initialized.get().booleanValue();
        }

        try {
            if (!(Thread.currentThread().getContextClassLoader()
                    instanceof ServiceClassLoader)) {
                ServiceClassLoader.getInstance().activate();
            }

            if (_classLib.isPresent()) {
                final ServiceClassLoader classLoader =
                    (ServiceClassLoader) Thread
                        .currentThread()
                        .getContextClassLoader();

                classLoader.addFromClassLib(_classLib.get());
                _classLib = null;
            }

            for (final ClassDefEntity implemented: _implemented) {
                if (!implemented.isDefined()) {
                    throw new UndefinedEntityException(implemented);
                }

                if (!implemented._updateClassLoader()) {
                    _initialized = Optional.of(Boolean.FALSE);
                }
            }

            if (!_initialized.isPresent()) {
                _initialized = Optional.of(Boolean.TRUE);
            }
        } catch (final UndefinedEntityException exception) {
            getThisLogger().warn(BaseMessages.VERBATIM, exception.getMessage());
            _initialized = Optional.of(Boolean.FALSE);

            return false;
        }

        if (_initialized.get().booleanValue()) {
            final Class<?> instanceClass = _impl.getInstanceClass();

            if (instanceClass == null) {
                _initialized = Optional.of(Boolean.FALSE);
            } else {
                for (final ClassDefEntity implemented: _implemented) {
                    final Class<?> implementedClass = implemented
                        .getInstanceClass();

                    if (implementedClass == null) {
                        _initialized = Optional.of(Boolean.FALSE);
                    } else if (!implementedClass
                        .isAssignableFrom(instanceClass)) {
                        getThisLogger()
                            .warn(
                                ServiceMessages.DOES_NOT_IMPLEMENT,
                                instanceClass.getName(),
                                implementedClass.getName());
                        _initialized = Optional.of(Boolean.FALSE);
                    }
                }
            }
        }

        return _initialized.get().booleanValue();
    }

    /** ClassDef element name. */
    public static final String ELEMENT_NAME = "ClassDef";

    /** ClassDef entity prefix. */
    public static final String ENTITY_PREFIX = "K";

    private boolean _busy;
    private Optional<ClassLibEntity> _classLib;
    private final ClassDefImpl _impl;
    private final List<ClassDefEntity> _implemented = new LinkedList<>();
    private Optional<Boolean> _initialized;
    private int _level = 1;

    /**
     * Builder.
     */
    public static class Builder
        extends AbstractEntity.Builder
    {
        /**
         * Constructs an instance.
         */
        Builder() {}

        /**
         * Adds an other class definition entity as an implemented interface.
         *
         * @param classDef The class definition entity to add.
         *
         * @return This.
         */
        @Nonnull
        public Builder addImplemented(@Nonnull final ClassDefEntity classDef)
        {
            _implemented.add(classDef);

            return this;
        }

        /** {@inheritDoc}
         */
        @Override
        public ClassDefEntity build()
        {
            return new ClassDefEntity(
                getName().get(),
                _impl,
                _implemented,
                _classLib,
                _initialized,
                _level);
        }

        /**
         * Copies the values from an other ClassDefEntity.
         *
         * @param classDef The other ClassDefEntity.
         *
         * @return This.
         */
        @Nonnull
        public final Builder copyFrom(@Nonnull final ClassDefEntity classDef)
        {
            super.copyFrom(classDef);

            _impl = classDef.getImpl();

            _implemented.clear();
            _implemented.addAll(classDef.getImplemented());

            _classLib = classDef.getClassLib();
            _initialized = Optional.of(classDef.getInitialized());
            _level = classDef.getLevel();

            return this;
        }

        /**
         * Sets the class library.
         *
         * @param classLib The class library.
         *
         * @return This.
         */
        @Nonnull
        public Builder setClassLib(@Nonnull final ClassLibEntity classLib)
        {
            _classLib = Optional.of(classLib);

            return this;
        }

        /**
         * Sets the impl.
         *
         * @param impl The impl.
         *
         * @return This.
         */
        @Nonnull
        public Builder setImpl(@Nonnull final ClassDefImpl impl)
        {
            _impl = Require.notNull(impl);

            return this;
        }

        /** {@inheritDoc}
         */
        @Override
        public Builder setUUID(final UUID uuid)
        {
            throw new UnsupportedOperationException();
        }

        private Optional<ClassLibEntity> _classLib = Optional.empty();
        private ClassDefImpl _impl;
        private final List<ClassDefEntity> _implemented = new LinkedList<>();
        private Optional<Boolean> _initialized = Optional.empty();
        private int _level = 1;
    }


    /**
     * Undefined.
     */
    public static final class Undefined
        extends ClassDefEntity
    {
        /**
         * Constructs an instance.
         *
         * @param name The entity name.
         */
        public Undefined(@Nonnull final String name)
        {
            super(
                name,
                _IMPL,
                Collections.emptyList(),
                Optional.empty(),
                Optional.empty(),
                0);
        }

        /**
         * Returns a new builder.
         *
         * @return The new builder.
         */
        public static Builder newBuilder()
        {
            return new Builder();
        }

        /** {@inheritDoc}
         */
        @Override
        public Undefined copy()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public <T> T createInstance(final Class<T> expectedClass)
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public String getClassName()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public Class<?> getInstanceClass()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public String getMember()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public String getPackageName()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isDefined()
        {
            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isLoaded()
        {
            return false;
        }

        private static final ClassDefImpl _IMPL = new ClassDefImpl("");

        /**
         * Builder.
         */
        public static final class Builder
            extends ClassDefEntity.Builder
        {
            /**
             * Constructs an instance.
             */
            Builder() {}

            /** {@inheritDoc}
             */
            @Override
            public Undefined build()
            {
                return new Undefined(Require.notNull(_key));
            }

            /**
             * Sets the key.
             *
             * @param key The key.
             *
             * @return This.
             */
            @Nonnull
            public Builder setKey(@Nonnull final String key)
            {
                _key = key;

                return this;
            }

            private String _key;
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
