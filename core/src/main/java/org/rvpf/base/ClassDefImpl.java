/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ClassDefImpl.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.base;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;

/**
 * Provides a class definition implementation.
 */
@ThreadSafe
public final class ClassDefImpl
    implements ClassDef
{
    /**
     * Constructs an instance.
     *
     * <p>Used to create default class definitions.</p>
     *
     * @param aClass A class object.
     */
    public ClassDefImpl(@Nonnull final Class<?> aClass)
    {
        this(aClass.getName());

        _class = aClass;
    }

    /**
     * Constructs an instance.
     *
     * <p>Creates a class definition from a string.</p>
     *
     * @param className The complete class name.
     */
    public ClassDefImpl(@Nonnull final String className)
    {
        final int index = className.lastIndexOf('.');

        if (index >= 0) {
            _packageName = className.substring(0, index).trim();
            _member = className.substring(index + 1).trim();
        } else {
            _packageName = null;
            _member = className.trim();
        }
    }

    /**
     * Constructs an instance.
     *
     * @param packageName The name of the package containing the class.
     * @param member The class name without the package specification.
     */
    public ClassDefImpl(
            @Nonnull final String packageName,
            @Nonnull final String member)
    {
        _packageName = Require.notNull(packageName);
        _member = Require.notNull(member);
    }

    /** {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T createInstance(final Class<T> expectedClass)
    {
        T instance = null;

        if (getInstanceClass() != null) {
            if (expectedClass.isAssignableFrom(_class)) {
                try {
                    instance = (T) _class.getConstructor().newInstance();
                } catch (final Exception exception) {
                    final Throwable cause = exception.getCause();

                    _getLogger()
                        .error(
                            BaseMessages.INSTANTIATION_FAILED,
                            getClassName(),
                            (cause != null)? cause: exception);
                }
            } else {
                _getLogger()
                    .error(
                        BaseMessages.CLASS_NOT_COMPATIBLE,
                        _class.getName(),
                        expectedClass.getName());
            }
        }

        return instance;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getClassName()
    {
        return (_packageName != null)? (_packageName + '.' + _member): _member;
    }

    /** {@inheritDoc}
     */
    @Override
    public Class<?> getInstanceClass()
    {
        return getInstanceClass(
            Optional
                .ofNullable(Thread.currentThread().getContextClassLoader()));
    }

    /** {@inheritDoc}
     */
    @Override
    public Class<?> getInstanceClass(final Optional<ClassLoader> classLoader)
    {
        Class<?> instanceClass = _class;

        if (instanceClass == null) {
            try {
                instanceClass = Class
                    .forName(getClassName(), true, classLoader.orElse(null));
            } catch (final ClassNotFoundException exception) {
                _getLogger().warn(BaseMessages.CLASS_NOT_FOUND, getClassName());
            }

            _class = instanceClass;
        }

        return instanceClass;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getMember()
    {
        return _member;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getPackageName()
    {
        return _packageName;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isLoaded()
    {
        final ClassLoader classLoader = Thread
            .currentThread()
            .getContextClassLoader();

        if (classLoader instanceof Loader) {
            return ((Loader) classLoader).isLoaded(getClassName());
        }

        return false;
    }

    /**
     * Returns a String representation of itself.
     *
     * @return The name of its referenced Class.
     */
    @Override
    public String toString()
    {
        return getClassName();
    }

    private Logger _getLogger()
    {
        return Logger.getInstance(getClass());
    }

    private volatile Class<?> _class;
    private final String _member;
    private final String _packageName;
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
